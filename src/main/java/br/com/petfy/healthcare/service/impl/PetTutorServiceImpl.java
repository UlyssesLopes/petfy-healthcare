package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.PetTutorActivityNotifier;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.PetTutorService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetTutorServiceImpl implements PetTutorService {

    private final PetTutorRepository petTutorRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final PersonRepository personRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final AnimalAccessGuard animalAccessGuard;
    private final OpaqueTokenService opaqueTokenService;
    private final PetTutorActivityNotifier petTutorActivityNotifier;

    @Value("${petfy.pet-tutor-invite.default-expiration-days:7}")
    private int defaultExpirationDays;

    /**
     * Convidar e do titular. Um EDITOR que pudesse convidar contornaria a regra:
     * bastaria convidar um comparsa como HOLDER para tomar o animal de quem o
     * cadastrou.
     *
     * O convite existe em vez de vinculo direto porque o caso comum e o conjuge
     * que <b>ainda nao tem conta</b> - exigir cadastro previo mataria o fluxo onde
     * ele comeca. E vincular alguem sem que aceite faria a pessoa passar a receber
     * lembrete que nao pediu.
     */
    @Override
    @Transactional
    public PetTutorInviteResponseDTO invite(UUID animalId, PetTutorInviteRequestDTO request) {
        Animal animal = animalAccessGuard.requireTitular(animalId);
        Person emissor = currentPersonProvider.require();

        recusarSeJaETutor(animalId, request.getEmail());

        int validade = request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        PetTutorInvite invite = petTutorInviteRepository.save(PetTutorInvite.builder()
                .animal(animal)
                .createdBy(emissor)
                .tokenHash(opaqueTokenService.hash(token))
                .email(request.getEmail().trim())
                .role(request.getRole())
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(invite, token);
    }

    /**
     * Aceitar exige estar autenticado, e o e-mail da conta tem de ser o do
     * convite. Sem isso o link viraria portador: quem o recebesse encaminhado
     * entraria no historico de saude de um animal que nao e dele.
     *
     * <b>Token invalido, expirado, revogado, ja usado e destinado a outra pessoa
     * respondem igual.</b> Distinguir diria a quem tenta adivinhar qual parte
     * errou - e um convite de animal e o que separa um estranho da carteira inteira.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO accept(String token) {
        Person aceitante = currentPersonProvider.require();

        PetTutorInvite invite = petTutorInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(i -> i.isUsable(LocalDateTime.now()))
                .filter(i -> i.getEmail().equalsIgnoreCase(aceitante.getEmail()))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        UUID animalId = invite.getAnimal().getAnimalId();

        if (petTutorRepository.existsByAnimalAnimalIdAndPersonPersonId(animalId, aceitante.getPersonId())) {
            throw jaETutor();
        }

        // A transferencia rebaixa o titular atual ANTES de o novo vinculo entrar.
        // O indice unico parcial da V15 admite um HOLDER por animal: inserir primeiro
        // deixaria dois na tabela e o Postgres recusaria o insert.
        Person titularAnterior = invite.transfereTitularidade() ? rebaixarTitular(animalId) : null;

        PetTutor vinculo = petTutorRepository.save(PetTutor.builder()
                .animal(invite.getAnimal())
                .person(aceitante)
                .role(invite.getRole())
                .invitedBy(invite.getCreatedBy())
                .creationDate(LocalDateTime.now())
                .build());

        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(aceitante);
        petTutorInviteRepository.save(invite);

        // Um aviso so, e nao dois: aceitar convite de HOLDER e entrar no animal e virar
        // titular ao mesmo tempo, e a mudanca de titularidade e a informacao que
        // importa - ela ja diz que ha gente nova cuidando do animal.
        if (titularAnterior != null) {
            petTutorActivityNotifier.titularidadeMudou(invite.getAnimal(), tutoresDoAnimal(animalId),
                    titularAnterior, aceitante, aceitante);
        } else {
            petTutorActivityNotifier.tutorEntrou(invite.getAnimal(), tutoresDoAnimal(animalId),
                    aceitante, invite.getRole());
        }

        return toResponse(vinculo);
    }

    /**
     * Qualquer tutor ve a lista. Saber quem alcanca o historico de saude do seu
     * animal e parte da privacidade, e nao o contrario - inclusive para quem so
     * acompanha.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PetTutorResponseDTO> listTutors(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return petTutorRepository.findByAnimalAnimalIdOrderByRoleAscCreationDateAsc(animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Convite pendente e informacao de gestao: so o titular, que os emitiu. */
    @Override
    @Transactional(readOnly = true)
    public List<PetTutorInviteResponseDTO> listInvites(UUID animalId) {
        animalAccessGuard.requireTitular(animalId);

        return petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(animalId)
                .stream()
                .map(invite -> toResponse(invite, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeInvite(UUID animalId, UUID petTutorInviteId) {
        animalAccessGuard.requireTitular(animalId);

        PetTutorInvite invite = petTutorInviteRepository.findById(petTutorInviteId)
                .filter(i -> i.getAnimal().getAnimalId().equals(animalId))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (invite.getRevokedAt() == null) {
            invite.setRevokedAt(LocalDateTime.now());
            petTutorInviteRepository.save(invite);
        }
    }

    /**
     * Duas saidas pela mesma porta, com regras diferentes:
     *
     * <ul>
     *   <li><b>O titular remove um co-tutor</b> - tirar acesso ao animal e decisao de
     *       quem responde por ele.</li>
     *   <li><b>Um co-tutor remove a si mesmo</b> - quem nao quer mais acompanhar
     *       sai sozinho, sem depender do titular. Exigir autorizacao para sair
     *       prenderia a pessoa a notificacoes de um animal que nao e dela.</li>
     * </ul>
     *
     * O titular nunca sai por aqui, nem por vontade propria: o indice do banco
     * exige exatamente um HOLDER, e um animal sem titular ficaria sem ninguem que
     * pudesse convidar ou apaga-lo. Transfere primeiro, ou apaga o animal.
     */
    @Override
    @Transactional
    public void removeTutor(UUID animalId, UUID personId) {
        Person quemAgiu = currentPersonProvider.require();
        boolean saindoSozinho = quemAgiu.getPersonId().equals(personId);

        Animal animal = saindoSozinho
                // provar que e tutor basta: sair nao exige nivel nenhum
                ? animalAccessGuard.requireLeitura(animalId)
                : animalAccessGuard.requireTitular(animalId);

        PetTutor vinculo = buscarVinculo(animalId, personId);

        if (vinculo.isHolder()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getMessage(),
                    ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getCode(),
                    HttpStatus.CONFLICT);
        }

        Person queSaiu = vinculo.getPerson();

        petTutorRepository.delete(vinculo);
        // o flush e o que faz tutoresDoAnimal devolver a lista sem quem saiu: sem ele o
        // delete fica pendente e o aviso iria tambem para quem acabou de perder o
        // acesso, contando-lhe que ele mesmo saiu
        petTutorRepository.flush();

        petTutorActivityNotifier.tutorSaiu(animal, tutoresDoAnimal(animalId), queSaiu, quemAgiu);
    }

    /**
     * Troca entre EDITOR e VIEWER. HOLDER nao entra: promover alguem a titular
     * rebaixa o titular atual, entao nao e mudar o papel de um tutor - e a
     * transferencia, que pede confirmacao propria.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO changeRole(UUID animalId, UUID personId, PetTutorRoleUpdateRequestDTO request) {
        animalAccessGuard.requireTitular(animalId);

        if (request.getRole() == PetTutorRole.HOLDER) {
            throw transferenciaExigida();
        }

        PetTutor vinculo = buscarVinculo(animalId, personId);

        // rebaixar o titular por aqui deixaria o animal sem nenhum, e o indice do
        // banco recusaria - mas a resposta certa nao e 500: e apontar a porta
        if (vinculo.isHolder()) {
            throw transferenciaExigida();
        }

        vinculo.setRole(request.getRole());
        vinculo.setUpdateDate(LocalDateTime.now());

        return toResponse(petTutorRepository.save(vinculo));
    }

    /**
     * Adocao, venda, separacao: o animal troca de titular sem trocar de historico.
     *
     * Quem recebe precisa <b>ja ser tutor</b>. Transferir para quem esta fora do
     * animal e o convite com papel HOLDER - la a pessoa aceita, e aqui nao haveria
     * como pedir consentimento de quem passaria a responder pelo animal.
     *
     * O titular antigo vira EDITOR em vez de sair: quem cuidou do animal ate
     * ontem continua enxergando a carteira, e o novo titular decide se remove.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO transferHolder(UUID animalId, UUID toPersonId) {
        Animal animal = animalAccessGuard.requireTitular(animalId);
        Person quemAgiu = currentPersonProvider.require();

        PetTutor destino = buscarVinculo(animalId, toPersonId);

        // transferir para quem ja e o titular e no-op, e nao erro: o estado final
        // pedido e o estado atual. Nao avisa ninguem, porque nada mudou
        if (destino.isHolder()) {
            return toResponse(destino);
        }

        // mesma ordem da aceitacao: rebaixa antes de promover, senao os dois
        // HOLDER coexistem e o indice unico parcial recusa
        Person titularAnterior = rebaixarTitular(animalId);

        destino.setRole(PetTutorRole.HOLDER);
        destino.setUpdateDate(LocalDateTime.now());

        PetTutorResponseDTO resposta = toResponse(petTutorRepository.save(destino));

        petTutorActivityNotifier.titularidadeMudou(animal, tutoresDoAnimal(animalId),
                titularAnterior, destino.getPerson(), quemAgiu);

        return resposta;
    }

    /**
     * Rebaixa o titular atual a EDITOR e descarrega no banco.
     *
     * O {@code flush} nao e zelo: sem ele o Hibernate pode emitir o insert ou o
     * update do novo titular antes deste update, e o indice unico parcial veria
     * dois HOLDER no mesmo animal. Foi exatamente assim que a exclusao de conta
     * quebrou - ver PersonServiceImpl.
     */
    private Person rebaixarTitular(UUID animalId) {
        Person anterior = petTutorRepository.findByAnimalAnimalIdAndRole(animalId, PetTutorRole.HOLDER)
                .map(titular -> {
                    titular.setRole(PetTutorRole.EDITOR);
                    titular.setUpdateDate(LocalDateTime.now());
                    petTutorRepository.save(titular);
                    return titular.getPerson();
                })
                .orElse(null);

        petTutorRepository.flush();

        return anterior;
    }

    /**
     * Quem cuida do animal agora, por consulta e nao pela colecao de {@code Animal}.
     *
     * A colecao e lazy e acabou de ser mexida - vinculo inserido, papel alterado -,
     * entao ler dela daria uma lista que pode nao refletir o que foi gravado. O
     * aviso iria para o conjunto errado de pessoas, que e o unico jeito de este
     * recurso piorar a privacidade em vez de melhorar.
     */
    private List<Person> tutoresDoAnimal(UUID animalId) {
        return petTutorRepository.findByAnimalAnimalIdOrderByRoleAscCreationDateAsc(animalId)
                .stream()
                .map(PetTutor::getPerson)
                .collect(Collectors.toList());
    }

    /**
     * Convidar quem ja cuida do animal nao e engano de digitacao a ser silenciado: a
     * chave unica recusaria o vinculo no aceite, e a pessoa levaria o erro no
     * lugar de quem convidou.
     */
    private void recusarSeJaETutor(UUID animalId, String email) {
        personRepository.findByEmail(email.trim())
                .filter(person -> petTutorRepository.existsByAnimalAnimalIdAndPersonPersonId(animalId, person.getPersonId()))
                .ifPresent(person -> {
                    throw jaETutor();
                });
    }

    private PetTutor buscarVinculo(UUID animalId, UUID personId) {
        return petTutorRepository.findByAnimalAnimalIdAndPersonPersonId(animalId, personId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.TUTOR_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.TUTOR_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private static PetfyHealthcareException conviteInvalido() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private static PetfyHealthcareException jaETutor() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ALREADY_A_TUTOR.getMessage(),
                ErrorMessageEnum.ALREADY_A_TUTOR.getCode(),
                HttpStatus.CONFLICT);
    }

    private static PetfyHealthcareException transferenciaExigida() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getMessage(),
                ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getCode(),
                HttpStatus.CONFLICT);
    }

    private PetTutorResponseDTO toResponse(PetTutor vinculo) {
        return PetTutorResponseDTO.builder()
                .petTutorId(vinculo.getPetTutorId())
                .animalId(vinculo.getAnimal().getAnimalId())
                .personId(vinculo.getPerson().getPersonId())
                .personName(vinculo.getPerson().getName())
                .personEmail(vinculo.getPerson().getEmail())
                .role(vinculo.getRole())
                .holder(vinculo.isHolder())
                .invitedByPersonName(vinculo.getInvitedBy() != null ? vinculo.getInvitedBy().getName() : null)
                .creationDate(vinculo.getCreationDate())
                .build();
    }

    private PetTutorInviteResponseDTO toResponse(PetTutorInvite invite, String token) {
        return PetTutorInviteResponseDTO.builder()
                .petTutorInviteId(invite.getPetTutorInviteId())
                .animalId(invite.getAnimal().getAnimalId())
                .animalName(invite.getAnimal().getName())
                .token(token)
                .email(invite.getEmail())
                .role(invite.getRole())
                .createdByPersonName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .acceptedAt(invite.getAcceptedAt())
                .revokedAt(invite.getRevokedAt())
                .usable(invite.isUsable(LocalDateTime.now()))
                .transfersHolder(invite.transfereTitularidade())
                .build();
    }

}
