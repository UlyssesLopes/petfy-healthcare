package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PersonRequestDTO;
import br.com.petfy.healthcare.domain.dto.PersonResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.ClinicInvite;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.ProfessionalCredential;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.ConsentRecordRepository;
import br.com.petfy.healthcare.domain.repository.EmailVerificationTokenRepository;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PasswordResetTokenRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.ClinicInviteService;
import br.com.petfy.healthcare.service.ClinicService;
import br.com.petfy.healthcare.service.ConsentService;
import br.com.petfy.healthcare.service.EmailVerificationService;
import br.com.petfy.healthcare.service.PersonService;
import br.com.petfy.healthcare.service.AnimalPurger;
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
public class PersonServiceImpl implements PersonService {

    private static final String CONSELHO_CRMV = "CRMV";

    private final PersonRepository personRepository;
    private final ProfessionalCredentialRepository credentialRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicService clinicService;
    private final ClinicInviteService clinicInviteService;
    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentPersonProvider currentPersonProvider;
    private final EmailVerificationService emailVerificationService;
    private final AnimalPurger animalPurger;
    private final ConsentService consentService;
    private final ConsentRecordRepository consentRecordRepository;
    private final AttachmentRepository attachmentRepository;

    @Override
    public PersonResponseDTO createPerson(PersonRequestDTO request) {

        garantirEmailLivre(request.getEmail());

        // valida o convite antes de criar a pessoa: nao faz sentido gravar a conta
        // para depois descobrir que o convite nao servia
        ClinicInvite invite = temTexto(request.getInviteToken())
                ? clinicInviteService.validate(request.getInviteToken(), request.getEmail())
                : null;

        Clinic clinic = resolverClinica(request, invite);

        Person person = Person.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .clinic(clinic)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        Person salvo = personRepository.save(person);

        registrarCredencial(salvo, request);

        // consome o convite: e de uso unico, senao o mesmo link serviria a
        // qualquer numero de pessoas
        if (invite != null) {
            clinicInviteService.markAccepted(invite, salvo.getPersonId());
        }

        // O consentimento e gravado na mesma transacao do cadastro, e antes do
        // envio de e-mail: conta que existisse sem aceite registrado seria
        // exatamente a lacuna que a V16 fecha, e um erro no envio nao pode ser o
        // que decide se a base legal ficou registrada. O @AssertTrue no request
        // garante que houve aceite; aqui ele ganha data, versao e evidencia.
        consentService.registrarAceiteNoCadastro(salvo);

        // e-mail comeca sem verificacao: a conta funciona, mas nao recebe aviso
        // ate o tutor confirmar o endereco. Falha de envio aqui nao desfaz o
        // cadastro - quem nao receber pede o reenvio
        emailVerificationService.sendVerification(salvo);

        return toResponseDTO(salvo);
    }

    @Override
    public PersonResponseDTO getCurrentPerson() {
        return toResponseDTO(currentPersonProvider.require());
    }

    @Override
    public PersonResponseDTO updateCurrentPerson(PersonRequestDTO request) {
        Person existingPerson = currentPersonProvider.require();

        if (request.getName() != null) {
            existingPerson.setName(request.getName());
        }

        if (request.getEmail() != null) {
            existingPerson.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            existingPerson.setPhone(request.getPhone());
        }

        if (request.getAddress() != null) {
            existingPerson.setAddress(request.getAddress());
        }

        // password fica de fora de proposito: troca de senha pede endpoint
        // proprio, com confirmacao da senha atual
        existingPerson.setUpdateDate(LocalDateTime.now());

        return toResponseDTO(personRepository.save(existingPerson));
    }

    @Override
    public void changePassword(PasswordChangeRequestDTO request) {
        Person person = currentPersonProvider.require();

        // exigir a senha atual e o que impede que um token roubado, sozinho,
        // troque a senha e tome a conta em definitivo
        if (!passwordEncoder.matches(request.getCurrentPassword(), person.getPassword())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getMessage(),
                    ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        // sem isso, quem troca a senha depois de um vazamento acha que rodou a
        // credencial quando na pratica nao mudou nada
        if (passwordEncoder.matches(request.getNewPassword(), person.getPassword())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getMessage(),
                    ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        person.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // e isto que derruba as sessoes abertas: o filtro recusa token emitido
        // antes deste instante. Sem o carimbo, trocar a senha nao expulsaria
        // quem ja estava dentro, que e justamente o motivo de trocar
        person.setPasswordChangedAt(LocalDateTime.now());
        person.setUpdateDate(LocalDateTime.now());
        personRepository.save(person);
    }

    /**
     * Apaga a conta e todo o rastro do tutor no sistema: animals, vacinas, historico,
     * shares, acessos por clinica, correcoes e tokens. E o exercicio do direito de
     * exclusao pela LGPD.
     *
     * Ordem obrigatoria: filhas antes das pais. As correcoes apontam para vacina e
     * historico; vacina/historico apontam para animal; animal aponta para person. Sem
     * essa ordem o banco recusa cada delete com violacao de chave estrangeira - o
     * DELETE /persons/me estava quebrado desde a V12 exatamente por isso, so
     * apagava a conta sem animal.
     *
     * <b>Com multi-tutor, a cascata deixou de ser cega.</b> Antes da V15 todo animal
     * tinha um dono so, entao sair do sistema e levar os animals junto eram a mesma
     * coisa. Agora nao sao: um animal que outra pessoa tambem cuida nao pode morrer
     * porque um dos tutores fechou a conta - seria apagar dado de saude de um
     * animal que continua tendo quem responda por ele, e o pedido de exclusao de
     * um titular nao autoriza destruir o historico do outro.
     *
     * Entao os animals se dividem em dois grupos:
     * <ul>
     *   <li><b>Animal sem outro tutor</b> - morre junto, com vacinas, historico,
     *       correcoes, shares e acessos de clinica. E a cascata de antes.</li>
     *   <li><b>Animal com outro tutor</b> - sobrevive e so perde este vinculo. Se
     *       quem sai era o titular, a titularidade passa ao tutor mais antigo,
     *       porque o indice do banco exige exatamente um HOLDER por animal e um animal
     *       sem titular ficaria sem ninguem que pudesse convidar ou apagar.</li>
     * </ul>
     *
     * A limpeza fica em codigo, nao em ON DELETE CASCADE no schema, para manter a
     * decisao visivel e testavel - mesmo padrao ja adotado para os tokens.
     */
    @Override
    @Transactional
    public void deleteCurrentPerson() {
        Person person = currentPersonProvider.require();
        UUID personId = person.getPersonId();

        // As custodias em curso de quem sai. Depois do P2b so elas dizem por quais
        // animais esta pessoa responde - concessao a terceiros nao entra, porque quem
        // tem acesso concedido nunca respondeu pelo animal.
        List<Custody> custodias = custodyRepository.findEmCursoDaPessoa(personId);

        List<UUID> animalsQueMorrem = new ArrayList<>();
        List<UUID> animalsQuePrecisamDeSucessor = new ArrayList<>();

        LocalDateTime agora = LocalDateTime.now();

        for (Custody custodia : custodias) {
            UUID animalId = custodia.getAnimal().getAnimalId();

            // sobrevive se alguem mais alcanca o animal: nesse caso a custodia passa
            // para o mais antigo desses. Sem ninguem, o animal morre com a conta.
            //
            // A regra que o PRODUTO.md 3.4 decidiu e outra - recusar a exclusao ate o
            // titular dar destino ao animal -, e ela nao entra aqui de proposito: e
            // mudanca de contrato de um endpoint de LGPD, e vai numa fatia propria.
            // Este passo preserva o comportamento que ja existia.
            if (!grantRepository.findVigentesDePessoasNoAnimal(animalId, agora).isEmpty()) {
                animalsQuePrecisamDeSucessor.add(animalId);
            } else {
                animalsQueMorrem.add(animalId);
            }
        }

        // Os convites saem antes dos vinculos e dos animals: cada linha aponta para o
        // animal, para quem convidou e para quem aceitou, entao seguraria os tres
        // deletes seguintes. Convite e credencial de uso unico com validade curta,
        // nao historico de saude - apagar segue a mesma politica que o passo 10
        // escolheu para o resto da conta.
        petTutorInviteRepository.deleteByCreatedByPersonId(personId);
        petTutorInviteRepository.deleteByAcceptedByPersonId(personId);

        // As custodias de quem sai saem primeiro: sao filhas de animal e de person ao
        // mesmo tempo, entao segurariam os dois deletes seguintes.
        //
        // E saem antes de abrir a custodia do sucessor, nao depois. O indice unico
        // parcial exige no maximo UMA custodia em curso por animal: criar a do sucessor
        // com a de quem sai ainda aberta deixa duas, e o Postgres recusa - derrubando a
        // exclusao de conta inteira. Nao aparece em teste de mock, que nao tem indice.
        // E a mesma armadilha que o 8b encontrou na troca de titularidade.
        custodias.forEach(c -> c.setSuccessor(null));
        custodyRepository.saveAll(custodias);
        custodyRepository.flush();
        custodyRepository.deleteAll(custodias);
        custodyRepository.flush();

        animalsQuePrecisamDeSucessor.forEach(this::abrirCustodiaDoSucessor);

        // Anexo enviado por quem sai, num animal que SOBREVIVE porque tem outro tutor: o
        // arquivo pertence ao animal, nao a quem fez o upload. Apagar o laudo porque quem o
        // subiu fechou a conta destruiria dado de saude de um animal que continua tendo
        // quem responda por ele - a mesma regra que a V15 aplicou ao animal inteiro. Perde-se
        // so a autoria.
        //
        // Nos animals que morrem, o purge apaga o anexo e o arquivo de qualquer forma; este
        // update passa por eles antes sem prejuizo.
        attachmentRepository.desassociarUploader(personId);

        // Os animals que morrem, com tudo que pende deles. A sequencia mora no
        // AnimalPurger, compartilhada com o DELETE /animals/{id}: eram duas listas
        // separadas e elas divergiram - quando o passo 9 trouxe peso e
        // antiparasitario, nenhuma das duas foi atualizada, e os dois caminhos
        // passaram a responder 500 em casos diferentes.
        animalPurger.purge(animalsQueMorrem);

        // Tokens da conta
        passwordResetTokenRepository.deleteByPersonPersonId(personId);
        emailVerificationTokenRepository.deleteByPersonPersonId(personId);

        // O registro de consentimento sai junto. A evidencia do aceite - IP, user
        // agent, data - e dado pessoal do titular, e guardar prova de consentimento
        // de quem pediu para ser esquecido inverteria o proposito da prova. Nao ha o
        // que demonstrar sobre um titular que nao existe mais.
        consentRecordRepository.deleteByPersonPersonId(personId);

        // A credencial profissional sai junto: numero de registro em conselho e
        // dado pessoal do titular, e a tabela aponta para persons, entao seguraria
        // o delete abaixo. E a mesma familia dos seis bugs da Fase 4 - tabela nova
        // que aponta para a conta e nao entra na lista de exclusao.
        credentialRepository.deleteByPersonPersonId(personId);

        // Finalmente o person
        personRepository.delete(person);
    }

    /**
     * Quem sai era o titular de um animal que sobrevive: alguem precisa herdar.
     * O criterio e o vinculo mais antigo entre os que ficam - quem acompanha o
     * animal ha mais tempo. Nao ha escolha do usuario aqui de proposito: apagar a
     * conta nao pode ficar bloqueado esperando uma decisao.
     *
     * Chamado <b>depois</b> de o vinculo de quem sai ter sido apagado e descarregado
     * no banco, entao os candidatos aqui sao apenas quem fica - nao ha mais o que
     * filtrar, e nao ha um segundo HOLDER competindo pelo indice unico.
     */
    private void abrirCustodiaDoSucessor(UUID animalId) {
        grantRepository.findVigentesDePessoasNoAnimal(animalId, LocalDateTime.now()).stream()
                .min(Comparator.comparing(Grant::getGrantedAt))
                .ifPresent(maisAntigo -> {
                    // a pessoa com acesso mais antigo passa a responder pelo animal, e a
                    // concessao dela e revogada: quem responde nao precisa de concessao, e
                    // manter as duas deixaria a mesma pessoa alcancando o animal por dois
                    // caminhos - o dia em que divergissem seria um vazamento
                    custodyRepository.save(Custody.builder()
                            .animal(maisAntigo.getAnimal())
                            .holderPerson(maisAntigo.getGranteePerson())
                            .nature(CustodyNature.DEFINITIVA)
                            .startedAt(LocalDateTime.now())
                            .build());

                    maisAntigo.setRevokedAt(LocalDateTime.now());
                    grantRepository.save(maisAntigo);
                });
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Uma clinica, nenhuma, ou erro se pedirem as duas coisas.
     *
     * <b>Nenhuma passou a ser valido no P1,</b> e essa e a entrada do veterinario
     * autonomo: atendimento domiciliar e enorme no Brasil e nao tem clinica, e
     * antes o cadastro obrigava a inventar uma. Quem atende e o profissional; a
     * clinica e onde ele atende.
     */
    private Clinic resolverClinica(PersonRequestDTO request, ClinicInvite invite) {
        if (invite != null && request.getClinic() != null) {
            throw new PetfyHealthcareException(
                    "informe inviteToken para entrar numa clinica existente, ou clinic para cadastrar uma nova",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        if (invite != null) {
            return invite.getClinic();
        }

        if (request.getClinic() == null) {
            return null;
        }

        UUID clinicId = clinicService.createClinic(request.getClinic()).getClinicId();

        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    /**
     * A credencial nasce INFORMADA, nunca VERIFICADA: nao ha integracao com
     * conselho, e um estado que mentisse sobre isso seria pior que nao ter estado.
     */
    private void registrarCredencial(Person person, PersonRequestDTO request) {
        if (!temTexto(request.getCrmv())) {
            return;
        }

        String uf = temTexto(request.getCrmvUf()) ? request.getCrmvUf().toUpperCase() : null;

        if (uf == null) {
            throw new PetfyHealthcareException(
                    "crmvUf e obrigatorio quando crmv e informado",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        if (credentialRepository.existsByCouncilAndUfAndNumber(CONSELHO_CRMV, uf, request.getCrmv())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CREDENTIAL_ALREADY_REGISTERED.getMessage(),
                    ErrorMessageEnum.CREDENTIAL_ALREADY_REGISTERED.getCode(),
                    HttpStatus.CONFLICT);
        }

        credentialRepository.save(ProfessionalCredential.builder()
                .person(person)
                .council(CONSELHO_CRMV)
                .uf(uf)
                .number(request.getCrmv())
                .status(CredentialStatus.INFORMADO)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build());
    }

    /**
     * O e-mail agora e unico no banco, e nao mais conferido em dois lados pelo
     * servico. Esta checagem sobrevive para o cliente receber 409 em vez de um
     * erro de integridade como 500.
     */
    private void garantirEmailLivre(String email) {
        boolean jaUsado = personRepository.existsByEmail(email);

        if (jaUsado) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage(),
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private PersonResponseDTO toResponseDTO(Person person) {
        return PersonResponseDTO.builder()
                .personId(person.getPersonId())
                .name(person.getName())
                .email(person.getEmail())
                .phone(person.getPhone())
                .address(person.getAddress())
                .creationDate(person.getCreationDate())
                .updateDate(person.getUpdateDate())
                .build();
    }

}
