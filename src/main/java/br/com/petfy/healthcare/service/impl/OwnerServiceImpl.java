package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.EmailVerificationTokenRepository;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PasswordResetTokenRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.EmailVerificationService;
import br.com.petfy.healthcare.service.OwnerService;
import br.com.petfy.healthcare.service.PetPurger;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final VetRepository vetRepository;
    private final PetTutorRepository petTutorRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final EmailVerificationService emailVerificationService;
    private final PetPurger petPurger;

    @Override
    public OwnerResponseDTO createOwner(OwnerRequestDTO request) {

        garantirEmailLivre(request.getEmail());

        Owner owner = Owner.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        Owner salvo = ownerRepository.save(owner);

        // e-mail comeca sem verificacao: a conta funciona, mas nao recebe aviso
        // ate o tutor confirmar o endereco. Falha de envio aqui nao desfaz o
        // cadastro - quem nao receber pede o reenvio
        emailVerificationService.sendVerification(salvo);

        return toResponseDTO(salvo);
    }

    @Override
    public OwnerResponseDTO getCurrentOwner() {
        return toResponseDTO(currentOwnerProvider.require());
    }

    @Override
    public OwnerResponseDTO updateCurrentOwner(OwnerRequestDTO request) {
        Owner existingOwner = currentOwnerProvider.require();

        if (request.getName() != null) {
            existingOwner.setName(request.getName());
        }

        if (request.getEmail() != null) {
            existingOwner.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            existingOwner.setPhone(request.getPhone());
        }

        if (request.getAddress() != null) {
            existingOwner.setAddress(request.getAddress());
        }

        // password fica de fora de proposito: troca de senha pede endpoint
        // proprio, com confirmacao da senha atual
        existingOwner.setUpdateDate(LocalDateTime.now());

        return toResponseDTO(ownerRepository.save(existingOwner));
    }

    @Override
    public void changePassword(PasswordChangeRequestDTO request) {
        Owner owner = currentOwnerProvider.require();

        // exigir a senha atual e o que impede que um token roubado, sozinho,
        // troque a senha e tome a conta em definitivo
        if (!passwordEncoder.matches(request.getCurrentPassword(), owner.getPassword())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getMessage(),
                    ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        // sem isso, quem troca a senha depois de um vazamento acha que rodou a
        // credencial quando na pratica nao mudou nada
        if (passwordEncoder.matches(request.getNewPassword(), owner.getPassword())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getMessage(),
                    ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        owner.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // e isto que derruba as sessoes abertas: o filtro recusa token emitido
        // antes deste instante. Sem o carimbo, trocar a senha nao expulsaria
        // quem ja estava dentro, que e justamente o motivo de trocar
        owner.setPasswordChangedAt(LocalDateTime.now());
        owner.setUpdateDate(LocalDateTime.now());
        ownerRepository.save(owner);
    }

    /**
     * Apaga a conta e todo o rastro do tutor no sistema: pets, vacinas, historico,
     * shares, acessos por clinica, correcoes e tokens. E o exercicio do direito de
     * exclusao pela LGPD.
     *
     * Ordem obrigatoria: filhas antes das pais. As correcoes apontam para vacina e
     * historico; vacina/historico apontam para pet; pet aponta para owner. Sem
     * essa ordem o banco recusa cada delete com violacao de chave estrangeira - o
     * DELETE /owners/me estava quebrado desde a V12 exatamente por isso, so
     * apagava a conta sem pet.
     *
     * <b>Com multi-tutor, a cascata deixou de ser cega.</b> Antes da V15 todo pet
     * tinha um dono so, entao sair do sistema e levar os pets junto eram a mesma
     * coisa. Agora nao sao: um pet que outra pessoa tambem cuida nao pode morrer
     * porque um dos tutores fechou a conta - seria apagar dado de saude de um
     * animal que continua tendo quem responda por ele, e o pedido de exclusao de
     * um titular nao autoriza destruir o historico do outro.
     *
     * Entao os pets se dividem em dois grupos:
     * <ul>
     *   <li><b>Pet sem outro tutor</b> - morre junto, com vacinas, historico,
     *       correcoes, shares e acessos de clinica. E a cascata de antes.</li>
     *   <li><b>Pet com outro tutor</b> - sobrevive e so perde este vinculo. Se
     *       quem sai era o titular, a titularidade passa ao tutor mais antigo,
     *       porque o indice do banco exige exatamente um HOLDER por pet e um pet
     *       sem titular ficaria sem ninguem que pudesse convidar ou apagar.</li>
     * </ul>
     *
     * A limpeza fica em codigo, nao em ON DELETE CASCADE no schema, para manter a
     * decisao visivel e testavel - mesmo padrao ja adotado para os tokens.
     */
    @Override
    @Transactional
    public void deleteCurrentOwner() {
        Owner owner = currentOwnerProvider.require();
        UUID ownerId = owner.getOwnerId();

        List<PetTutor> vinculos = petTutorRepository.findByOwnerOwnerId(ownerId);

        List<UUID> petsQueMorrem = new ArrayList<>();
        List<UUID> petsQuePrecisamDeSucessor = new ArrayList<>();

        for (PetTutor vinculo : vinculos) {
            UUID petId = vinculo.getPet().getPetId();

            if (petTutorRepository.countByPetPetId(petId) > 1) {
                if (vinculo.isHolder()) {
                    petsQuePrecisamDeSucessor.add(petId);
                }
            } else {
                petsQueMorrem.add(petId);
            }
        }

        // Os convites saem antes dos vinculos e dos pets: cada linha aponta para o
        // pet, para quem convidou e para quem aceitou, entao seguraria os tres
        // deletes seguintes. Convite e credencial de uso unico com validade curta,
        // nao historico de saude - apagar segue a mesma politica que o passo 10
        // escolheu para o resto da conta.
        petTutorInviteRepository.deleteByCreatedByOwnerId(ownerId);
        petTutorInviteRepository.deleteByAcceptedByOwnerId(ownerId);

        // Os vinculos saem primeiro: sao filhos de pet e de owner ao mesmo tempo,
        // entao segurariam os dois deletes seguintes
        petTutorRepository.deleteByOwnerOwnerId(ownerId);

        // E saem tambem antes de promover o sucessor, nao depois. O indice unico
        // parcial da V15 exige exatamente um HOLDER por pet: promover com o
        // vinculo de quem sai ainda na tabela deixa dois, e o Postgres recusa o
        // update - derrubando a exclusao de conta inteira. Nao aparecia em teste
        // de mock, que nao tem indice.
        petTutorRepository.flush();
        petsQuePrecisamDeSucessor.forEach(this::promoverSucessor);

        // Os pets que morrem, com tudo que pende deles. A sequencia mora no
        // PetPurger, compartilhada com o DELETE /pets/{id}: eram duas listas
        // separadas e elas divergiram - quando o passo 9 trouxe peso e
        // antiparasitario, nenhuma das duas foi atualizada, e os dois caminhos
        // passaram a responder 500 em casos diferentes.
        petPurger.purge(petsQueMorrem);

        // Tokens da conta
        passwordResetTokenRepository.deleteByOwnerOwnerId(ownerId);
        emailVerificationTokenRepository.deleteByOwnerOwnerId(ownerId);

        // Finalmente o owner
        ownerRepository.delete(owner);
    }

    /**
     * Quem sai era o titular de um pet que sobrevive: alguem precisa herdar.
     * O criterio e o vinculo mais antigo entre os que ficam - quem acompanha o
     * pet ha mais tempo. Nao ha escolha do usuario aqui de proposito: apagar a
     * conta nao pode ficar bloqueado esperando uma decisao.
     *
     * Chamado <b>depois</b> de o vinculo de quem sai ter sido apagado e descarregado
     * no banco, entao os candidatos aqui sao apenas quem fica - nao ha mais o que
     * filtrar, e nao ha um segundo HOLDER competindo pelo indice unico.
     */
    private void promoverSucessor(UUID petId) {
        petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(petId).stream()
                .min(Comparator.comparing(PetTutor::getCreationDate))
                .ifPresent(sucessor -> {
                    sucessor.setRole(PetTutorRole.HOLDER);
                    sucessor.setUpdateDate(LocalDateTime.now());
                    petTutorRepository.save(sucessor);
                });
    }

    private void garantirEmailLivre(String email) {
        boolean jaUsado = ownerRepository.existsByEmail(email) || vetRepository.existsByEmail(email);

        if (jaUsado) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage(),
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private OwnerResponseDTO toResponseDTO(Owner owner) {
        return OwnerResponseDTO.builder()
                .ownerId(owner.getOwnerId())
                .name(owner.getName())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .address(owner.getAddress())
                .creationDate(owner.getCreationDate())
                .updateDate(owner.getUpdateDate())
                .build();
    }

}
