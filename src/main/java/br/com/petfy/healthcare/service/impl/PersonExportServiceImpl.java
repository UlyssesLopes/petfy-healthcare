package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PersonExportDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.PersonExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonExportServiceImpl implements PersonExportService {

    /**
     * Muda quando o formato do documento mudar de forma - campo removido ou renomeado,
     * nao campo novo. Um arquivo guardado precisa dizer por qual leitor foi escrito.
     */
    private static final String FORMATO = "1";

    /**
     * Teto de acessos por animal no documento.
     *
     * O log de acesso cresce sem limite: uma clinica que acompanha um animal cronico gera
     * entrada toda semana, por anos. Sem teto, o export de um unico animal antigo viraria o
     * maior objeto que esta API ja serializou. O corte e declarado nas limitacoes - o que
     * nao pode acontecer e o documento parecer completo tendo truncado em silencio.
     */
    private static final int MAX_ACESSOS_POR_PET = 500;

    private final CurrentPersonProvider currentPersonProvider;
    private final ConsentRecordRepository consentRecordRepository;
    private final PetTutorRepository petTutorRepository;
    private final VaccineRepository vaccineRepository;
    private final VaccineCorrectionRepository vaccineCorrectionRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final AnimalWeightHistoryRepository animalWeightHistoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final GrantRepository grantRepository;
    private final SensitiveAccessLogRepository sensitiveAccessLogRepository;
    private final AnimalHealthConditionRepository animalHealthConditionRepository;

    /**
     * Monta o documento a partir dos vinculos de tutor, e nao de uma consulta por dono.
     *
     * A diferenca importa desde a V15: um animal pode ter varios tutores, e o que define o
     * que este titular leva embora e o <b>vinculo</b> - inclusive o papel, que pode ser
     * apenas de leitor.
     *
     * Sao muitas consultas, uma por colecao por animal. E deliberado: um tutor tem poucos
     * animals, o endpoint e chamado raramente, e a alternativa - uma consulta grande com
     * varios joins - devolveria produto cartesiano que teria de ser desmontado em memoria
     * de qualquer forma. Legibilidade vence aqui.
     */
    @Override
    @Transactional(readOnly = true)
    public PersonExportDTO exportarDoAutenticado() {
        Person person = currentPersonProvider.require();

        // Depois do P2b sao duas origens, e o export tem de cobrir as duas: os animais
        // por que a pessoa responde, e os que ela alcanca por concessao. Antes vinham
        // da mesma tabela porque custodia e acesso eram a mesma coisa.
        LocalDateTime agora = LocalDateTime.now();

        List<Relacao> vinculos = new ArrayList<>();

        custodyRepository.findEmCursoDaPessoa(person.getPersonId()).forEach(c ->
                vinculos.add(new Relacao(c.getAnimal(), "CUSTODIA", c.getStartedAt())));

        grantRepository.findVigentesDaPessoa(person.getPersonId(), agora).forEach(g ->
                vinculos.add(new Relacao(g.getAnimal(), g.getLevel().name(), g.getGrantedAt())));

        return PersonExportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .formatVersion(FORMATO)
                .tutor(PersonExportDTO.TutorDTO.builder()
                        .personId(person.getPersonId())
                        .name(person.getName())
                        .email(person.getEmail())
                        .phone(person.getPhone())
                        .address(person.getAddress())
                        .emailVerifiedAt(person.getEmailVerifiedAt())
                        .creationDate(person.getCreationDate())
                        .updateDate(person.getUpdateDate())
                        .build())
                .consentimentos(consentRecordRepository
                        .findByPersonPersonIdOrderByAcceptedAtDesc(person.getPersonId())
                        .stream()
                        .map(c -> PersonExportDTO.ConsentimentoDTO.builder()
                                .document(c.getDocument())
                                .documentVersion(c.getDocumentVersion())
                                .acceptedAt(c.getAcceptedAt())
                                .build())
                        .toList())
                .animals(vinculos.stream().map(this::exportarAnimal).toList())
                .limitacoes(limitacoes())
                .build();
    }

    /**
     * Como esta pessoa alcanca este animal.
     *
     * Existe porque as duas origens - custodia e concessao - nao tem uma classe em
     * comum, e nem deveriam ter: sao conceitos opostos. Achata-las aqui, no limite do
     * documento, e melhor que inventar uma superclasse no dominio so para o export.
     */
    private record Relacao(Animal animal, String relacao, LocalDateTime desde) {
    }

    private PersonExportDTO.AnimalExportDTO exportarAnimal(Relacao meuVinculo) {
        Animal animal = meuVinculo.animal();
        UUID animalId = animal.getAnimalId();

        return PersonExportDTO.AnimalExportDTO.builder()
                .animalId(animalId)
                .name(animal.getName())
                .type(animal.getType())
                .breed(animal.getBreed())
                .species(animal.getSpecies())
                .bornDate(animal.getBornDate())
                .gender(animal.getGender())
                .color(animal.getColor())
                .microchip(animal.getMicrochip())
                .microchipNumber(animal.getMicrochipNumber())
                .castrated(animal.getCastrated())
                .castratedAt(animal.getCastratedAt())
                .generalRegistry(animal.getGeneralRegistry())
                .weight(animal.getWeight())
                .creationDate(animal.getCreationDate())
                .meuPapel(meuVinculo.getRole())
                .condicoes(condicoes(animalId))
                .coTutores(coTutores(animalId, meuVinculo))
                .vacinas(vacinas(animalId))
                .antiparasitarios(antiparasitarios(animalId))
                .pesagens(pesagens(animalId))
                .atendimentos(atendimentos(animalId))
                .anexos(anexos(animalId))
                .linksCompartilhados(links(animal))
                .acessosDeClinica(acessosDeClinica(animalId))
                .acessosDeTerceiros(acessosDeTerceiros(animalId))
                .build();
    }

    /**
     * As outras pessoas que alcancam o animal, por nome e relacao.
     *
     * Sem e-mail e sem id: saber com quem se divide o animal e informacao do titular, o
     * endereco de contato da outra pessoa nao. O proprio vinculo sai da lista - ele ja
     * esta em {@code minhaRelacao}, e repetir daria a impressao de haver uma pessoa a
     * mais.
     *
     * Clinica e link ficam de fora daqui e aparecem em {@code acessosDeClinica} e
     * {@code linksCompartilhados}: sao concessoes, mas nao sao pessoas com quem se
     * divide o cuidado, e junta-las numa lista so apagaria a diferenca.
     */
    private List<PersonExportDTO.CoTutorDTO> coTutores(UUID animalId, UUID euMesmo) {
        LocalDateTime agora = LocalDateTime.now();
        List<PersonExportDTO.CoTutorDTO> outros = new ArrayList<>();

        custodyRepository.findEmCurso(animalId)
                .filter(c -> c.getHolderPerson() != null)
                .filter(c -> !c.getHolderPerson().getPersonId().equals(euMesmo))
                .ifPresent(c -> outros.add(PersonExportDTO.CoTutorDTO.builder()
                        .name(c.getHolderPerson().getName())
                        .relacao("CUSTODIA")
                        .desde(c.getStartedAt())
                        .build()));

        grantRepository.findVigentesDePessoasNoAnimal(animalId, agora).stream()
                .filter(g -> !g.getGranteePerson().getPersonId().equals(euMesmo))
                .forEach(g -> outros.add(PersonExportDTO.CoTutorDTO.builder()
                        .name(g.getGranteePerson().getName())
                        .relacao(g.getLevel().name())
                        .desde(g.getGrantedAt())
                        .build()));

        return outros;
    }

    /**
     * Alergias e condicoes cronicas, ativas primeiro.
     *
     * Entram no export porque sao o que outro sistema precisa ler <b>antes</b> do
     * historico: quem importa o prontuario deste animal tem de saber a que ele e alergico
     * antes de ler o que ja aconteceu com ele.
     */
    private List<PersonExportDTO.CondicaoDTO> condicoes(UUID animalId) {
        return animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(animalId)
                .stream()
                .map(c -> PersonExportDTO.CondicaoDTO.builder()
                        .kind(c.getKind())
                        .description(c.getDescription())
                        .severity(c.getSeverity())
                        .notes(c.getNotes())
                        .since(c.getSince())
                        .resolvedAt(c.getResolvedAt())
                        .ativa(c.isAtiva())
                        .build())
                .toList();
    }

    private List<PersonExportDTO.VacinaDTO> vacinas(UUID animalId) {
        return vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId)
                .stream()
                .map(v -> PersonExportDTO.VacinaDTO.builder()
                        .vaccineId(v.getVaccineId())
                        .vaccineName(v.getVaccineName())
                        .applicationDate(v.getApplicationDate())
                        .nextDoseDate(v.getNextDoseDate())
                        .description(v.getDescription())
                        .clinicName(v.getClinic() != null ? v.getClinic().getName() : null)
                        .creationDate(v.getCreationDate())
                        .correcoes(correcoesDeVacina(v.getVaccineId()))
                        .build())
                .toList();
    }

    /**
     * O rastro de correcao vai junto.
     *
     * Nao e detalhe interno: o historico que o tutor leva embora inclui o que foi
     * alterado, por quem e quando. Um prontuario portado sem isso conta a versao final
     * como se sempre tivesse sido aquela.
     */
    private List<PersonExportDTO.CorrecaoDTO> correcoesDeVacina(UUID vaccineId) {
        return vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(vaccineId)
                .stream()
                .map(c -> PersonExportDTO.CorrecaoDTO.builder()
                        .corrigidoPor(quemCorrigiu(c.getCorrectedBy()))
                        .valorAnterior(c.getPreviousVaccineName())
                        .corrigidoEm(c.getCorrectedAt())
                        .build())
                .toList();
    }

    private List<PersonExportDTO.AtendimentoDTO> atendimentos(UUID animalId) {
        return healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(animalId)
                .stream()
                .map(r -> PersonExportDTO.AtendimentoDTO.builder()
                        .healthRecordId(r.getHealthRecordId())
                        .category(r.getCategory())
                        .eventType(r.getEventType())
                        .diagnosis(r.getDiagnosis())
                        .eventDate(r.getEventDate())
                        .description(r.getDescription())
                        .clinicName(r.getClinic() != null ? r.getClinic().getName() : null)
                        .creationDate(r.getCreationDate())
                        .correcoes(correcoesDeAtendimento(r.getHealthRecordId()))
                        .build())
                .toList();
    }

    private List<PersonExportDTO.CorrecaoDTO> correcoesDeAtendimento(UUID healthRecordId) {
        return healthRecordCorrectionRepository
                .findByHealthRecordHealthRecordIdOrderByCorrectedAtDesc(healthRecordId)
                .stream()
                .map(c -> PersonExportDTO.CorrecaoDTO.builder()
                        .corrigidoPor(quemCorrigiu(c.getCorrectedBy()))
                        .valorAnterior(c.getPreviousEventType())
                        .corrigidoEm(c.getCorrectedAt())
                        .build())
                .toList();
    }

    /** Nome de quem corrigiu, seja tutor ou veterinario. Nulo se a conta ja saiu. */
    /**
     * Deixou de escolher entre duas colunas: com pessoa unica ha um autor so.
     *
     * A clinica em nome de quem a pessoa agiu nao entra aqui de proposito. Este e
     * o export do titular, e a clinica ja aparece no proprio registro corrigido -
     * repeti-la na correcao so acrescentaria dado de terceiro a um arquivo que
     * circula e fica guardado.
     */
    private String quemCorrigiu(Person autor) {
        return autor != null ? autor.getName() : null;
    }

    private List<PersonExportDTO.AntiparasiticoDTO> antiparasitarios(UUID animalId) {
        return antiparasiticRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId)
                .stream()
                .map(a -> PersonExportDTO.AntiparasiticoDTO.builder()
                        .antiparasiticId(a.getAntiparasiticId())
                        .name(a.getName())
                        .kind(a.getKind())
                        .applicationDate(a.getApplicationDate())
                        .nextDoseDate(a.getNextDoseDate())
                        .description(a.getDescription())
                        .build())
                .toList();
    }

    private List<PersonExportDTO.PesagemDTO> pesagens(UUID animalId) {
        return animalWeightHistoryRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(animalId)
                .stream()
                .map(p -> PersonExportDTO.PesagemDTO.builder()
                        .weight(p.getWeight())
                        .measuredAt(p.getMeasuredAt())
                        .note(p.getNote())
                        .build())
                .toList();
    }

    /**
     * Metadado e caminho de download, nao bytes.
     *
     * JSON carrega binario so em base64, o que infla um terco e transforma um export com
     * tres laudos em algo que nenhum editor abre. O caminho continua autenticado, e a
     * limitacao esta declarada no documento - o que nao pode acontecer e o titular
     * concluir que levou os arquivos quando levou a lista deles.
     */
    private List<PersonExportDTO.AnexoDTO> anexos(UUID animalId) {
        return attachmentRepository.findByAnimalAnimalIdOrderByCreationDateDesc(animalId)
                .stream()
                .map(a -> PersonExportDTO.AnexoDTO.builder()
                        .attachmentId(a.getAttachmentId())
                        .originalFilename(a.getOriginalFilename())
                        .contentType(a.getContentType())
                        .sizeBytes(a.getSizeBytes())
                        .checksumSha256(a.getChecksumSha256())
                        .description(a.getDescription())
                        .downloadPath("/attachments/" + a.getAttachmentId() + "/content")
                        .creationDate(a.getCreationDate())
                        .build())
                .toList();
    }

    /**
     * Os dois metodos abaixo leem a mesma tabela desde o P2 - links e acessos de
     * clinica viraram concessoes -, e continuam separados no documento de
     * proposito: para quem le o export, "quem tem um link do meu animal" e "que
     * clinica enxerga meu animal" sao perguntas diferentes.
     *
     * O escopo entra nos dois. Sem ele o documento diria que houve acesso sem dizer
     * a quanto, que e justamente o que o P2 passou a permitir limitar.
     */
    private List<PersonExportDTO.LinkCompartilhadoDTO> links(Animal animal) {
        LocalDateTime agora = LocalDateTime.now();

        return grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(animal.getAnimalId())
                .stream()
                .filter(g -> g.getTokenHash() != null)
                .map(g -> PersonExportDTO.LinkCompartilhadoDTO.builder()
                        .grantId(g.getGrantId())
                        .scopes(g.getScopes())
                        .expiresAt(g.getExpiresAt())
                        .revokedAt(g.getRevokedAt())
                        .active(g.estaVigente(agora))
                        .creationDate(g.getGrantedAt())
                        .build())
                .toList();
    }

    private List<PersonExportDTO.AcessoDeClinicaDTO> acessosDeClinica(UUID animalId) {
        LocalDateTime agora = LocalDateTime.now();

        return grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(animalId)
                .stream()
                .filter(g -> g.getGranteeClinic() != null)
                .map(g -> PersonExportDTO.AcessoDeClinicaDTO.builder()
                        .clinicName(g.getGranteeClinic().getName())
                        .scopes(g.getScopes())
                        .grantedAt(g.getGrantedAt())
                        .revokedAt(g.getRevokedAt())
                        .active(g.estaVigente(agora))
                        .build())
                .toList();
    }

    private List<PersonExportDTO.AcessoRegistradoDTO> acessosDeTerceiros(UUID animalId) {
        return sensitiveAccessLogRepository
                .findByAnimalAnimalIdOrderByAccessedAtDesc(animalId, PageRequest.of(0, MAX_ACESSOS_POR_PET))
                .getContent()
                .stream()
                .map(l -> PersonExportDTO.AcessoRegistradoDTO.builder()
                        .actorType(l.getActorType())
                        .actorName(l.getActorName())
                        .clinicName(l.getClinicName())
                        .resource(l.getResource())
                        .accessedAt(l.getAccessedAt())
                        .ipAddress(l.getIpAddress())
                        .build())
                .toList();
    }

    /**
     * O que este documento nao carrega, dito nele mesmo.
     *
     * Vai dentro da resposta, e nao apenas na documentacao da rota: quem abre o arquivo
     * meses depois nao tem o Swagger ao lado, e um export que parece completo sem ser e
     * pior que um export declaradamente parcial.
     */
    private List<String> limitacoes() {
        return List.of(
                "Os anexos entram como metadado e caminho de download, nao como conteudo. "
                        + "Cada arquivo e baixado por downloadPath, que exige autenticacao.",
                "O log de acesso de terceiros vem truncado nos " + MAX_ACESSOS_POR_PET
                        + " registros mais recentes por animal.",
                "Dado pessoal de terceiro vem reduzido: co-tutor aparece por nome e papel, "
                        + "sem e-mail. A portabilidade e dos dados do titular.",
                "Animal compartilhado com outros tutores esta incluido, com o papel do titular "
                        + "indicado em meuPapel - ele pode nao ser o titular do animal.",
                "A senha nao aparece, nem como hash.",
                "As alergias e condicoes cronicas vem com as ativas primeiro; condicao "
                        + "encerrada aparece com resolvedAt preenchido, porque faz parte do "
                        + "historico do animal.");
    }

}
