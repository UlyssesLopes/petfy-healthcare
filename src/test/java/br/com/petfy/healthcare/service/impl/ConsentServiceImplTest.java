package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ConsentStatusResponseDTO;
import br.com.petfy.healthcare.domain.entity.ConsentDocument;
import br.com.petfy.healthcare.domain.entity.ConsentRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.ConsentRecordRepository;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.RequestEvidenceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceImplTest {

    @Mock
    private ConsentRecordRepository consentRecordRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @Mock
    private RequestEvidenceProvider requestEvidenceProvider;

    private ConsentServiceImpl consentService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String VIGENTE = "2026-08-05";
    private static final String ANTIGA = "2026-01-01";

    @BeforeEach
    void setUp() {
        consentService = new ConsentServiceImpl(
                consentRecordRepository, currentOwnerProvider, requestEvidenceProvider);
        ReflectionTestUtils.setField(consentService, "termsVersion", VIGENTE);
        ReflectionTestUtils.setField(consentService, "privacyVersion", VIGENTE);
    }

    private Owner owner() {
        return Owner.builder().ownerId(OWNER_ID).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private ConsentRecord aceite(ConsentDocument documento, String versao, LocalDateTime quando) {
        return ConsentRecord.builder()
                .consentRecordId(UUID.randomUUID())
                .owner(owner())
                .document(documento)
                .documentVersion(versao)
                .acceptedAt(quando)
                .build();
    }

    @Nested
    @DisplayName("registrarAceiteNoCadastro")
    class RegistrarNoCadastro {

        /**
         * Os dois documentos sao aceitos, e nao um so: termos e politica tratam de
         * coisas diferentes, e a versao aceita e por documento.
         */
        @Test
        @DisplayName("grava um aceite por documento vigente")
        void gravaUmAceitePorDocumento() {
            consentService.registrarAceiteNoCadastro(owner());

            var captor = ArgumentCaptor.forClass(ConsentRecord.class);
            verify(consentRecordRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues())
                    .extracting(ConsentRecord::getDocument)
                    .containsExactlyInAnyOrder(
                            ConsentDocument.TERMS_OF_SERVICE, ConsentDocument.PRIVACY_POLICY);
            assertThat(captor.getAllValues())
                    .allSatisfy(r -> {
                        assertThat(r.getDocumentVersion()).isEqualTo(VIGENTE);
                        assertThat(r.getAcceptedAt()).isNotNull();
                        assertThat(r.getOwner().getOwnerId()).isEqualTo(OWNER_ID);
                    });
        }

        /** A evidencia entra quando existe requisicao de onde extrai-la. */
        @Test
        @DisplayName("guarda IP e user agent como evidencia do aceite")
        void guardaEvidencia() {
            when(requestEvidenceProvider.ip()).thenReturn("203.0.113.7");
            when(requestEvidenceProvider.userAgent()).thenReturn("Mozilla/5.0");

            consentService.registrarAceiteNoCadastro(owner());

            var captor = ArgumentCaptor.forClass(ConsentRecord.class);
            verify(consentRecordRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).allSatisfy(r -> {
                assertThat(r.getIpAddress()).isEqualTo("203.0.113.7");
                assertThat(r.getUserAgent()).isEqualTo("Mozilla/5.0");
            });
        }

        /**
         * Evidencia ausente nao invalida consentimento: exigi-la faria a criacao de
         * conta falhar por causa de um proxy mal configurado.
         */
        @Test
        @DisplayName("grava o aceite mesmo sem evidencia disponivel")
        void gravaSemEvidencia() {
            when(requestEvidenceProvider.ip()).thenReturn(null);
            when(requestEvidenceProvider.userAgent()).thenReturn(null);

            consentService.registrarAceiteNoCadastro(owner());

            verify(consentRecordRepository, times(2)).save(any(ConsentRecord.class));
        }
    }

    @Nested
    @DisplayName("statusDoAutenticado")
    class Status {

        @Test
        @DisplayName("quem aceitou as versoes vigentes nao tem pendencia")
        void semPendenciaQuandoTudoAceito() {
            when(currentOwnerProvider.require()).thenReturn(owner());
            when(consentRecordRepository.findByOwnerOwnerIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of(
                            aceite(ConsentDocument.TERMS_OF_SERVICE, VIGENTE, LocalDateTime.now()),
                            aceite(ConsentDocument.PRIVACY_POLICY, VIGENTE, LocalDateTime.now())));

            var status = consentService.statusDoAutenticado();

            assertThat(status.isTudoAceito()).isTrue();
            assertThat(status.getPendentes()).isEmpty();
            assertThat(status.getAceites()).hasSize(2);
        }

        /**
         * O caso que faz a tabela guardar versao em vez de booleano: quem aceitou a
         * politica de janeiro nao aceitou a de agosto, e um "aceitou = true" nao
         * saberia distinguir.
         */
        @Test
        @DisplayName("politica que mudou de versao volta a ficar pendente")
        void politicaNovaVoltaAFicarPendente() {
            when(currentOwnerProvider.require()).thenReturn(owner());
            when(consentRecordRepository.findByOwnerOwnerIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of(
                            aceite(ConsentDocument.TERMS_OF_SERVICE, VIGENTE, LocalDateTime.now()),
                            aceite(ConsentDocument.PRIVACY_POLICY, ANTIGA, LocalDateTime.now().minusMonths(7))));

            var status = consentService.statusDoAutenticado();

            assertThat(status.isTudoAceito()).isFalse();
            assertThat(status.getPendentes()).singleElement().satisfies(pendente -> {
                assertThat(pendente.getDocument()).isEqualTo(ConsentDocument.PRIVACY_POLICY);
                assertThat(pendente.getDocumentVersion()).isEqualTo(VIGENTE);
                // diz o que a pessoa aceitou antes, para o cliente poder mostrar o que mudou
                assertThat(pendente.getVersaoAceitaAnteriormente()).isEqualTo(ANTIGA);
            });
        }

        /**
         * Conta anterior a V16 nunca foi perguntada, e nenhuma migration podia
         * inventar o aceite. Aparece pendente sem versao anterior.
         */
        @Test
        @DisplayName("conta sem aceite nenhum tem os dois documentos pendentes")
        void contaAnteriorTemTudoPendente() {
            when(currentOwnerProvider.require()).thenReturn(owner());
            when(consentRecordRepository.findByOwnerOwnerIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of());

            var status = consentService.statusDoAutenticado();

            assertThat(status.isTudoAceito()).isFalse();
            assertThat(status.getPendentes()).hasSize(2)
                    .allSatisfy(p -> assertThat(p.getVersaoAceitaAnteriormente()).isNull());
        }

        /** Consultar nao grava: era o jeito mais facil de esta rota mentir. */
        @Test
        @DisplayName("consultar o status nao grava aceite")
        void consultarNaoGrava() {
            when(currentOwnerProvider.require()).thenReturn(owner());
            when(consentRecordRepository.findByOwnerOwnerIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of());

            consentService.statusDoAutenticado();

            verify(consentRecordRepository, never()).save(any());
        }

        /**
         * A resposta nao devolve IP nem user agent: existem para auditoria interna, e
         * expo-los ao cliente contaria de onde a pessoa acessou sem proposito nenhum
         * para ela.
         */
        @Test
        @DisplayName("a resposta nao expoe a evidencia do aceite")
        void respostaNaoExpoeEvidencia() {
            assertThat(ConsentStatusResponseDTO.AceiteDTO.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("ipAddress", "userAgent");
        }
    }

    @Nested
    @DisplayName("aceitarVigentes")
    class AceitarVigentes {

        /**
         * Reenviar o aceite da mesma versao e no-op, e nao erro: o cliente pode
         * reenviar por perda de resposta, e a chave unica (owner, documento, versao)
         * recusaria a segunda linha com 500.
         */
        @Test
        @DisplayName("aceitar de novo a mesma versao nao cria outro registro")
        void aceitarDeNovoNaoDuplica() {
            when(currentOwnerProvider.require()).thenReturn(owner());
            when(consentRecordRepository.existsByOwnerOwnerIdAndDocumentAndDocumentVersion(
                    eq(OWNER_ID), any(ConsentDocument.class), eq(VIGENTE))).thenReturn(true);
            when(consentRecordRepository.findByOwnerOwnerIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of(
                            aceite(ConsentDocument.TERMS_OF_SERVICE, VIGENTE, LocalDateTime.now()),
                            aceite(ConsentDocument.PRIVACY_POLICY, VIGENTE, LocalDateTime.now())));

            var status = consentService.aceitarVigentes();

            verify(consentRecordRepository, never()).save(any());
            assertThat(status.isTudoAceito()).isTrue();
        }

        /** Aceita so o que falta: quem ja aceitou os termos e a politica nova, nao. */
        @Test
        @DisplayName("grava apenas o documento que ainda falta")
        void gravaApenasOQueFalta() {
            when(currentOwnerProvider.require()).thenReturn(owner());
            when(consentRecordRepository.existsByOwnerOwnerIdAndDocumentAndDocumentVersion(
                    OWNER_ID, ConsentDocument.TERMS_OF_SERVICE, VIGENTE)).thenReturn(true);
            when(consentRecordRepository.existsByOwnerOwnerIdAndDocumentAndDocumentVersion(
                    OWNER_ID, ConsentDocument.PRIVACY_POLICY, VIGENTE)).thenReturn(false);
            when(consentRecordRepository.findByOwnerOwnerIdOrderByAcceptedAtDesc(OWNER_ID))
                    .thenReturn(List.of());

            consentService.aceitarVigentes();

            var captor = ArgumentCaptor.forClass(ConsentRecord.class);
            verify(consentRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getDocument()).isEqualTo(ConsentDocument.PRIVACY_POLICY);
        }
    }

}
