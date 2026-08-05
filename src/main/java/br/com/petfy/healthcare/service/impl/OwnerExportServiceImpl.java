package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerExportDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.OwnerExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerExportServiceImpl implements OwnerExportService {

    /**
     * Muda quando o formato do documento mudar de forma - campo removido ou renomeado,
     * nao campo novo. Um arquivo guardado precisa dizer por qual leitor foi escrito.
     */
    private static final String FORMATO = "1";

    /**
     * Teto de acessos por pet no documento.
     *
     * O log de acesso cresce sem limite: uma clinica que acompanha um pet cronico gera
     * entrada toda semana, por anos. Sem teto, o export de um unico pet antigo viraria o
     * maior objeto que esta API ja serializou. O corte e declarado nas limitacoes - o que
     * nao pode acontecer e o documento parecer completo tendo truncado em silencio.
     */
    private static final int MAX_ACESSOS_POR_PET = 500;

    private final CurrentOwnerProvider currentOwnerProvider;
    private final ConsentRecordRepository consentRecordRepository;
    private final PetTutorRepository petTutorRepository;
    private final VaccineRepository vaccineRepository;
    private final VaccineCorrectionRepository vaccineCorrectionRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final PetWeightHistoryRepository petWeightHistoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final PetShareRepository petShareRepository;
    private final PetClinicAccessRepository petClinicAccessRepository;
    private final SensitiveAccessLogRepository sensitiveAccessLogRepository;
    private final PetHealthConditionRepository petHealthConditionRepository;

    /**
     * Monta o documento a partir dos vinculos de tutor, e nao de uma consulta por dono.
     *
     * A diferenca importa desde a V15: um pet pode ter varios tutores, e o que define o
     * que este titular leva embora e o <b>vinculo</b> - inclusive o papel, que pode ser
     * apenas de leitor.
     *
     * Sao muitas consultas, uma por colecao por pet. E deliberado: um tutor tem poucos
     * pets, o endpoint e chamado raramente, e a alternativa - uma consulta grande com
     * varios joins - devolveria produto cartesiano que teria de ser desmontado em memoria
     * de qualquer forma. Legibilidade vence aqui.
     */
    @Override
    @Transactional(readOnly = true)
    public OwnerExportDTO exportarDoAutenticado() {
        Owner owner = currentOwnerProvider.require();

        List<PetTutor> vinculos = petTutorRepository.findByOwnerOwnerId(owner.getOwnerId());

        return OwnerExportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .formatVersion(FORMATO)
                .tutor(OwnerExportDTO.TutorDTO.builder()
                        .ownerId(owner.getOwnerId())
                        .name(owner.getName())
                        .email(owner.getEmail())
                        .phone(owner.getPhone())
                        .address(owner.getAddress())
                        .emailVerifiedAt(owner.getEmailVerifiedAt())
                        .creationDate(owner.getCreationDate())
                        .updateDate(owner.getUpdateDate())
                        .build())
                .consentimentos(consentRecordRepository
                        .findByOwnerOwnerIdOrderByAcceptedAtDesc(owner.getOwnerId())
                        .stream()
                        .map(c -> OwnerExportDTO.ConsentimentoDTO.builder()
                                .document(c.getDocument())
                                .documentVersion(c.getDocumentVersion())
                                .acceptedAt(c.getAcceptedAt())
                                .build())
                        .toList())
                .pets(vinculos.stream().map(this::exportarPet).toList())
                .limitacoes(limitacoes())
                .build();
    }

    private OwnerExportDTO.PetExportDTO exportarPet(PetTutor meuVinculo) {
        Pet pet = meuVinculo.getPet();
        UUID petId = pet.getPetId();

        return OwnerExportDTO.PetExportDTO.builder()
                .petId(petId)
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .species(pet.getSpecies())
                .bornDate(pet.getBornDate())
                .gender(pet.getGender())
                .color(pet.getColor())
                .microchip(pet.getMicrochip())
                .microchipNumber(pet.getMicrochipNumber())
                .castrated(pet.getCastrated())
                .castratedAt(pet.getCastratedAt())
                .generalRegistry(pet.getGeneralRegistry())
                .weight(pet.getWeight())
                .creationDate(pet.getCreationDate())
                .meuPapel(meuVinculo.getRole())
                .condicoes(condicoes(petId))
                .coTutores(coTutores(petId, meuVinculo))
                .vacinas(vacinas(petId))
                .antiparasitarios(antiparasitarios(petId))
                .pesagens(pesagens(petId))
                .atendimentos(atendimentos(petId))
                .anexos(anexos(petId))
                .linksCompartilhados(links(pet))
                .acessosDeClinica(acessosDeClinica(petId))
                .acessosDeTerceiros(acessosDeTerceiros(petId))
                .build();
    }

    /**
     * Os outros tutores, por nome e papel.
     *
     * Sem e-mail e sem id: saber com quem se divide o pet e informacao do titular, o
     * endereco de contato da outra pessoa nao. O proprio vinculo sai da lista - ele ja
     * esta em {@code meuPapel}, e repetir daria a impressao de haver um tutor a mais.
     */
    private List<OwnerExportDTO.CoTutorDTO> coTutores(UUID petId, PetTutor meuVinculo) {
        return petTutorRepository.findByPetPetIdOrderByRoleAscCreationDateAsc(petId)
                .stream()
                .filter(t -> !t.getPetTutorId().equals(meuVinculo.getPetTutorId()))
                .map(t -> OwnerExportDTO.CoTutorDTO.builder()
                        .name(t.getOwner().getName())
                        .role(t.getRole())
                        .desde(t.getCreationDate())
                        .build())
                .toList();
    }

    /**
     * Alergias e condicoes cronicas, ativas primeiro.
     *
     * Entram no export porque sao o que outro sistema precisa ler <b>antes</b> do
     * historico: quem importa o prontuario deste animal tem de saber a que ele e alergico
     * antes de ler o que ja aconteceu com ele.
     */
    private List<OwnerExportDTO.CondicaoDTO> condicoes(UUID petId) {
        return petHealthConditionRepository.findByPetOrdenadasPorRelevancia(petId)
                .stream()
                .map(c -> OwnerExportDTO.CondicaoDTO.builder()
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

    private List<OwnerExportDTO.VacinaDTO> vacinas(UUID petId) {
        return vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(petId)
                .stream()
                .map(v -> OwnerExportDTO.VacinaDTO.builder()
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
    private List<OwnerExportDTO.CorrecaoDTO> correcoesDeVacina(UUID vaccineId) {
        return vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(vaccineId)
                .stream()
                .map(c -> OwnerExportDTO.CorrecaoDTO.builder()
                        .corrigidoPor(quemCorrigiu(c.getCorrectedByOwner(), c.getCorrectedByVet()))
                        .valorAnterior(c.getPreviousVaccineName())
                        .corrigidoEm(c.getCorrectedAt())
                        .build())
                .toList();
    }

    private List<OwnerExportDTO.AtendimentoDTO> atendimentos(UUID petId) {
        return healthRecordRepository.findByPetPetIdOrderByEventDateDesc(petId)
                .stream()
                .map(r -> OwnerExportDTO.AtendimentoDTO.builder()
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

    private List<OwnerExportDTO.CorrecaoDTO> correcoesDeAtendimento(UUID healthRecordId) {
        return healthRecordCorrectionRepository
                .findByHealthRecordHealthRecordIdOrderByCorrectedAtDesc(healthRecordId)
                .stream()
                .map(c -> OwnerExportDTO.CorrecaoDTO.builder()
                        .corrigidoPor(quemCorrigiu(c.getCorrectedByOwner(), c.getCorrectedByVet()))
                        .valorAnterior(c.getPreviousEventType())
                        .corrigidoEm(c.getCorrectedAt())
                        .build())
                .toList();
    }

    /** Nome de quem corrigiu, seja tutor ou veterinario. Nulo se a conta ja saiu. */
    private String quemCorrigiu(Owner porTutor, Vet porVet) {
        if (porTutor != null) {
            return porTutor.getName();
        }

        return porVet != null ? porVet.getName() : null;
    }

    private List<OwnerExportDTO.AntiparasiticoDTO> antiparasitarios(UUID petId) {
        return antiparasiticRepository.findByPetPetIdOrderByApplicationDateDesc(petId)
                .stream()
                .map(a -> OwnerExportDTO.AntiparasiticoDTO.builder()
                        .antiparasiticId(a.getAntiparasiticId())
                        .name(a.getName())
                        .kind(a.getKind())
                        .applicationDate(a.getApplicationDate())
                        .nextDoseDate(a.getNextDoseDate())
                        .description(a.getDescription())
                        .build())
                .toList();
    }

    private List<OwnerExportDTO.PesagemDTO> pesagens(UUID petId) {
        return petWeightHistoryRepository.findByPetPetIdOrderByMeasuredAtDesc(petId)
                .stream()
                .map(p -> OwnerExportDTO.PesagemDTO.builder()
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
    private List<OwnerExportDTO.AnexoDTO> anexos(UUID petId) {
        return attachmentRepository.findByPetPetIdOrderByCreationDateDesc(petId)
                .stream()
                .map(a -> OwnerExportDTO.AnexoDTO.builder()
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

    private List<OwnerExportDTO.LinkCompartilhadoDTO> links(Pet pet) {
        LocalDateTime agora = LocalDateTime.now();

        return petShareRepository.findByPetOrderByCreationDateDesc(pet)
                .stream()
                .map(s -> OwnerExportDTO.LinkCompartilhadoDTO.builder()
                        .petShareId(s.getPetShareId())
                        .expiresAt(s.getExpiresAt())
                        .revokedAt(s.getRevokedAt())
                        .active(s.isActive(agora))
                        .creationDate(s.getCreationDate())
                        .build())
                .toList();
    }

    private List<OwnerExportDTO.AcessoDeClinicaDTO> acessosDeClinica(UUID petId) {
        return petClinicAccessRepository.findByPetPetIdOrderByGrantedAtDesc(petId)
                .stream()
                .map(a -> OwnerExportDTO.AcessoDeClinicaDTO.builder()
                        .clinicName(a.getClinic().getName())
                        .grantedAt(a.getGrantedAt())
                        .revokedAt(a.getRevokedAt())
                        .active(a.isActive())
                        .build())
                .toList();
    }

    private List<OwnerExportDTO.AcessoRegistradoDTO> acessosDeTerceiros(UUID petId) {
        return sensitiveAccessLogRepository
                .findByPetPetIdOrderByAccessedAtDesc(petId, PageRequest.of(0, MAX_ACESSOS_POR_PET))
                .getContent()
                .stream()
                .map(l -> OwnerExportDTO.AcessoRegistradoDTO.builder()
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
                        + " registros mais recentes por pet.",
                "Dado pessoal de terceiro vem reduzido: co-tutor aparece por nome e papel, "
                        + "sem e-mail. A portabilidade e dos dados do titular.",
                "Pet compartilhado com outros tutores esta incluido, com o papel do titular "
                        + "indicado em meuPapel - ele pode nao ser o titular do pet.",
                "A senha nao aparece, nem como hash.",
                "As alergias e condicoes cronicas vem com as ativas primeiro; condicao "
                        + "encerrada aparece com resolvedAt preenchido, porque faz parte do "
                        + "historico do animal.");
    }

}
