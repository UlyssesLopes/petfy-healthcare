package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.Custodias;
import br.com.petfy.healthcare.domain.dto.PersonExportDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * O que o export carrega, e principalmente o que ele <b>nao</b> carrega.
 *
 * A parte facil de um export e incluir tudo. A parte que precisa de teste e a exclusao
 * deliberada: senha, hash de token de compartilhamento e e-mail de co-tutor. Um arquivo
 * de export circula por e-mail e fica guardado em pasta compartilhada - o destino normal
 * dele -, e cada campo a mais vaza para onde ninguem controla.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonExportServiceImplTest {

    @Mock private CurrentPersonProvider currentPersonProvider;
    @Mock private ConsentRecordRepository consentRecordRepository;
    @Mock private CustodyRepository custodyRepository;
    @Mock private VaccineRepository vaccineRepository;
    @Mock private VaccineCorrectionRepository vaccineCorrectionRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    @Mock private AntiparasiticRepository antiparasiticRepository;
    @Mock private AnimalWeightHistoryRepository animalWeightHistoryRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private GrantRepository grantRepository;
    @Mock private SensitiveAccessLogRepository sensitiveAccessLogRepository;
    @Mock private AnimalHealthConditionRepository animalHealthConditionRepository;

    @InjectMocks
    private PersonExportServiceImpl exportService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MARIA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Person ulysses() {
        return Person.builder()
                .personId(OWNER_ID).name("Ulysses").email("ulysses@petfy.com.br")
                .phone("11999999999").address("Rua A, 100")
                .password("$2a$10$hashQueNaoPodeAparecer")
                .emailVerifiedAt(LocalDateTime.now().minusDays(10))
                .creationDate(LocalDateTime.now().minusMonths(6))
                .build();
    }

    private Person maria() {
        return Person.builder().personId(MARIA_ID).name("Maria").email("maria@petfy.com.br").build();
    }

    private Animal rex() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA)
                .breed("Vira-lata").weight(12.5).microchip(true)
                .creationDate(LocalDateTime.now().minusMonths(5)).build();
    }

    private Custody custodiaDe(Person person) {
        Custody c = Custodias.emCurso(person);
        c.setAnimal(rex());
        return c;
    }

    private Grant concessaoPara(Person person, GrantLevel nivel) {
        return Grant.builder()
                .grantId(UUID.randomUUID())
                .animal(rex())
                .granteePerson(person)
                .level(nivel)
                .grantedAt(LocalDateTime.now())
                .build();
    }

    /**
     * O caminho normal, agora em duas variantes.
     *
     * Antes havia um helper so, {@code comUmAnimal(papel)}, porque titular e co-tutor
     * eram linhas da mesma tabela. Depois do P2b sao duas origens diferentes, e o
     * export tem de cobrir as duas - entao o helper se parte junto com o modelo, em
     * vez de fingir que a diferenca nao existe.
     */
    private void comUmAnimalPorCustodia() {
        comUmAnimal(List.of(custodiaDe(ulysses())), List.of());
    }

    private void comUmAnimalPorConcessao(GrantLevel nivel) {
        comUmAnimal(List.of(), List.of(concessaoPara(ulysses(), nivel)));
    }

    private void comUmAnimal(List<Custody> minhasCustodias, List<Grant> minhasConcessoes) {
        when(currentPersonProvider.require()).thenReturn(ulysses());
        when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(minhasCustodias);
        when(grantRepository.findVigentesDaPessoa(eq(OWNER_ID), any())).thenReturn(minhasConcessoes);
        when(custodyRepository.findEmCurso(ANIMAL_ID)).thenReturn(java.util.Optional.empty());
        when(grantRepository.findVigentesDePessoasNoAnimal(eq(ANIMAL_ID), any())).thenReturn(List.of());
        when(consentRecordRepository.findByPersonPersonIdOrderByAcceptedAtDesc(OWNER_ID))
                .thenReturn(List.of());
        when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID)).thenReturn(List.of());
        when(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(ANIMAL_ID)).thenReturn(List.of());
        when(antiparasiticRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID)).thenReturn(List.of());
        when(animalWeightHistoryRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(ANIMAL_ID)).thenReturn(List.of());
        when(attachmentRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID)).thenReturn(List.of());
        when(grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(ANIMAL_ID)).thenReturn(List.of());
        when(sensitiveAccessLogRepository.findByAnimalAnimalIdOrderByAccessedAtDesc(eq(ANIMAL_ID), any()))
                .thenReturn(Page.empty());
        when(animalHealthConditionRepository
                .findByAnimalOrdenadasPorRelevancia(ANIMAL_ID))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("o que entra")
    class OQueEntra {

        @Test
        @DisplayName("os dados do tutor e a data de geracao")
        void dadosDoTutor() {
            comUmAnimalPorCustodia();

            var export = exportService.exportarDoAutenticado();

            assertThat(export.generatedAt()).isNotNull();
            assertThat(export.formatVersion()).isNotBlank();
            assertThat(export.tutor().name()).isEqualTo("Ulysses");
            assertThat(export.tutor().email()).isEqualTo("ulysses@petfy.com.br");
            assertThat(export.tutor().phone()).isEqualTo("11999999999");
        }

        @Test
        @DisplayName("os consentimentos, com a versao aceita")
        void consentimentos() {
            comUmAnimalPorCustodia();
            when(consentRecordRepository.findByPersonPersonIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of(ConsentRecord.builder()
                            .person(ulysses())
                            .document(ConsentDocument.PRIVACY_POLICY)
                            .documentVersion("2026-08-05")
                            .acceptedAt(LocalDateTime.now())
                            .ipAddress("203.0.113.7")
                            .build()));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.consentimentos()).singleElement().satisfies(c -> {
                assertThat(c.document()).isEqualTo(ConsentDocument.PRIVACY_POLICY);
                assertThat(c.documentVersion()).isEqualTo("2026-08-05");
            });
        }

        /**
         * O papel importa: desde a V15 o titular do export pode ser apenas leitor de um
         * animal, e um documento que nao diz isso sugere posse que ele nao tem.
         */
        @Test
        @DisplayName("o papel do titular em cada animal")
        void oPapelEmCadaAnimal() {
            comUmAnimalPorConcessao(GrantLevel.VIEWER);

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals()).singleElement()
                    .satisfies(p -> assertThat(p.minhaRelacao()).isEqualTo("VIEWER"));
        }

        /**
         * Animal compartilhado entra: o titular tem acesso legitimo a ele, e omiti-lo daria
         * um export que contradiz o que o app mostra.
         */
        @Test
        @DisplayName("animal compartilhado entra, com os co-tutores listados")
        void animalCompartilhadoEntra() {
            comUmAnimalPorConcessao(GrantLevel.EDITOR);
            when(custodyRepository.findEmCurso(ANIMAL_ID))
                    .thenReturn(java.util.Optional.of(custodiaDe(maria())));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals()).singleElement().satisfies(p -> {
                assertThat(p.minhaRelacao()).isEqualTo("EDITOR");
                assertThat(p.coTutores()).singleElement().satisfies(c -> {
                    assertThat(c.name()).isEqualTo("Maria");
                    assertThat(c.relacao()).isEqualTo("CUSTODIA");
                });
            });
        }

        /**
         * O rastro de correcao vai junto: um prontuario portado sem ele conta a versao
         * final como se sempre tivesse sido aquela.
         */
        @Test
        @DisplayName("as correcoes de vacina, com quem corrigiu")
        void correcoesDeVacina() {
            var vacina = Vaccine.builder()
                    .vaccineId(UUID.randomUUID()).animal(rex()).vaccineName("Antirrabica")
                    .applicationDate(LocalDate.now().minusMonths(2)).build();

            comUmAnimalPorCustodia();
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(vacina));
            when(vaccineCorrectionRepository
                    .findByVaccineVaccineIdOrderByCorrectedAtDesc(vacina.getVaccineId()))
                    .thenReturn(List.of(VaccineCorrection.builder()
                            .vaccine(vacina)
                            .correctedBy(Person.builder().personId(UUID.randomUUID()).name("Dra. Marina").build())
                            .previousVaccineName("Antirabica")
                            .correctedAt(LocalDateTime.now())
                            .build()));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals().get(0).vacinas()).singleElement()
                    .satisfies(v -> assertThat(v.correcoes()).singleElement().satisfies(c -> {
                        assertThat(c.corrigidoPor()).isEqualTo("Dra. Marina");
                        assertThat(c.valorAnterior()).isEqualTo("Antirabica");
                    }));
        }

        /** Sem o caminho, o titular sabe que o laudo existe e nao tem como busca-lo. */
        @Test
        @DisplayName("o anexo entra com caminho de download e checksum")
        void anexoComCaminho() {
            var anexoId = UUID.randomUUID();

            comUmAnimalPorCustodia();
            when(attachmentRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(Attachment.builder()
                            .attachmentId(anexoId).animal(rex())
                            .originalFilename("laudo.pdf").contentType("application/pdf")
                            .sizeBytes(1024L).checksumSha256("a".repeat(64))
                            .storageKey("animals/" + ANIMAL_ID + "/chave-secreta")
                            .creationDate(LocalDateTime.now()).build()));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals().get(0).anexos()).singleElement().satisfies(a -> {
                assertThat(a.downloadPath()).isEqualTo("/attachments/" + anexoId + "/content");
                assertThat(a.checksumSha256()).hasSize(64);
            });
        }

        /**
         * As limitacoes vao <b>dentro</b> do documento: quem o abre meses depois nao tem o
         * Swagger ao lado, e um export que parece completo sem ser e pior que um
         * declaradamente parcial.
         */
        @Test
        @DisplayName("o documento declara as proprias limitacoes")
        void declaraLimitacoes() {
            comUmAnimalPorCustodia();

            var export = exportService.exportarDoAutenticado();

            assertThat(export.limitacoes()).isNotEmpty();
            assertThat(String.join(" ", export.limitacoes()))
                    .contains("anexos")
                    .contains("senha");
        }
    }

    /**
     * O ponto do teste. Um arquivo de export circula e fica guardado, entao cada campo a
     * mais vaza para onde ninguem controla.
     */
    @Nested
    @DisplayName("o que nao pode entrar")
    class OQueNaoEntra {

        @Test
        @DisplayName("a senha nao aparece, nem como hash")
        void senhaNaoAparece() {
            assertThat(PersonExportDTO.TutorDTO.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("password", "passwordHash", "senha");
        }

        /**
         * O e-mail do co-tutor e dado pessoal de outra pessoa, e a portabilidade e dos
         * dados do titular. O nome basta para ele saber com quem divide o animal.
         */
        @Test
        @DisplayName("o e-mail do co-tutor nao aparece")
        void emailDeCoTutorNaoAparece() {
            assertThat(PersonExportDTO.CoTutorDTO.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .contains("name", "relacao")
                    .doesNotContain("email", "personId");
        }

        @Test
        @DisplayName("nenhum e-mail de terceiro chega ao documento montado")
        void nenhumEmailDeTerceiroNoDocumento() {
            comUmAnimalPorConcessao(GrantLevel.EDITOR);
            when(custodyRepository.findEmCurso(ANIMAL_ID))
                    .thenReturn(java.util.Optional.of(custodiaDe(maria())));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals().get(0).coTutores())
                    .allSatisfy(c -> assertThat(c.name()).doesNotContain("@"));
        }

        /** O hash nao serve ao titular e e material para tentar reverter o link. */
        @Test
        @DisplayName("o hash do token de compartilhamento nao aparece")
        void hashDoTokenNaoAparece() {
            assertThat(PersonExportDTO.LinkCompartilhadoDTO.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("tokenHash", "token");
        }

        /** Detalhe de infraestrutura: muda com o storage, e nao e do titular. */
        @Test
        @DisplayName("a chave de storage do anexo nao aparece")
        void chaveDeStorageNaoAparece() {
            assertThat(PersonExportDTO.AnexoDTO.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("storageKey");
        }

        /**
         * A evidencia do proprio aceite nao acrescenta nada ao titular - mesma decisao do
         * GET /consents/me.
         */
        @Test
        @DisplayName("a evidencia do consentimento nao aparece")
        void evidenciaDoConsentimentoNaoAparece() {
            assertThat(PersonExportDTO.ConsentimentoDTO.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("ipAddress", "userAgent");
        }
    }

    @Nested
    @DisplayName("casos de borda")
    class Borda {

        @Test
        @DisplayName("tutor sem animal exporta o documento com a lista vazia")
        void semAnimalExportaVazio() {
            when(currentPersonProvider.require()).thenReturn(ulysses());
            when(custodyRepository.findEmCursoDaPessoa(OWNER_ID)).thenReturn(List.of());
            when(grantRepository.findVigentesDaPessoa(eq(OWNER_ID), any())).thenReturn(List.of());
            when(consentRecordRepository.findByPersonPersonIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of());

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals()).isEmpty();
            assertThat(export.tutor().name()).isEqualTo("Ulysses");
            // as limitacoes continuam, porque descrevem o formato e nao o conteudo
            assertThat(export.limitacoes()).isNotEmpty();
        }

        /**
         * O log cresce sem teto: uma clinica que acompanha animal cronico gera entrada toda
         * semana por anos. O corte esta declarado nas limitacoes - o que nao pode e
         * truncar em silencio.
         */
        @Test
        @DisplayName("o log de acesso vem truncado, e o corte esta declarado")
        void logDeAcessoVemTruncado() {
            comUmAnimalPorCustodia();
            when(sensitiveAccessLogRepository.findByAnimalAnimalIdOrderByAccessedAtDesc(eq(ANIMAL_ID), any()))
                    .thenReturn(new PageImpl<>(List.of(SensitiveAccessLog.builder()
                            .animal(rex())
                            .actorType(AccessActorType.VET)
                            .actorName("Dra. Marina")
                            .organizationName("Clinica Bicho Feliz")
                            .resource(AccessedResource.HEALTH_RECORDS)
                            .accessedAt(LocalDateTime.now())
                            .ipAddress("203.0.113.7")
                            .build())));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals().get(0).acessosDeTerceiros()).singleElement()
                    .satisfies(a -> assertThat(a.actorName()).isEqualTo("Dra. Marina"));
            assertThat(String.join(" ", export.limitacoes())).contains("truncado");
        }

        /** Conta que corrigiu e depois saiu: o rastro fica, sem nome. */
        @Test
        @DisplayName("correcao de conta que ja saiu nao quebra o export")
        void correcaoSemAutorNaoQuebra() {
            var vacina = Vaccine.builder()
                    .vaccineId(UUID.randomUUID()).animal(rex()).vaccineName("V10").build();

            comUmAnimalPorCustodia();
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(vacina));
            when(vaccineCorrectionRepository
                    .findByVaccineVaccineIdOrderByCorrectedAtDesc(vacina.getVaccineId()))
                    .thenReturn(List.of(VaccineCorrection.builder()
                            .vaccine(vacina)
                            .previousVaccineName("V8")
                            .correctedAt(LocalDateTime.now())
                            .build()));

            var export = exportService.exportarDoAutenticado();

            assertThat(export.animals().get(0).vacinas().get(0).correcoes())
                    .singleElement()
                    .satisfies(c -> assertThat(c.corrigidoPor()).isNull());
        }
    }

}
