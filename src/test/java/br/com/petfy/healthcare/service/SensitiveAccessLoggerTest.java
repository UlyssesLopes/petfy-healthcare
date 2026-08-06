package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.AccessActorType;
import br.com.petfy.healthcare.domain.entity.AccessedResource;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.SensitiveAccessLog;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.SensitiveAccessLogRepository;
import br.com.petfy.healthcare.security.RequestEvidenceProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveAccessLoggerTest {

    @Mock
    private SensitiveAccessLogRepository sensitiveAccessLogRepository;

    @Mock
    private RequestEvidenceProvider requestEvidenceProvider;

    @InjectMocks
    private SensitiveAccessLogger logger;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID VET_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private Animal rex() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA).build();
    }

    private Person marina() {
        return Person.builder()
                .personId(VET_ID).name("Dra. Marina").email("marina@vet.com.br")
                .organization(Organization.builder().organizationId(UUID.randomUUID()).name("Clinica Bicho Feliz").build())
                .build();
    }

    private SensitiveAccessLog gravado() {
        var captor = ArgumentCaptor.forClass(SensitiveAccessLog.class);
        verify(sensitiveAccessLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("vetLeu")
    class VetLeu {

        @Test
        @DisplayName("registra o animal, o recurso e quem leu")
        void registraOEssencial() {
            logger.vetLeu(marina(), rex(), AccessedResource.HEALTH_RECORDS);

            var log = gravado();
            assertThat(log.getAnimal().getAnimalId()).isEqualTo(ANIMAL_ID);
            assertThat(log.getActorType()).isEqualTo(AccessActorType.VET);
            assertThat(log.getActorId()).isEqualTo(VET_ID);
            assertThat(log.getResource()).isEqualTo(AccessedResource.HEALTH_RECORDS);
            assertThat(log.getAccessedAt()).isNotNull();
        }

        /**
         * Nome, e nao chave estrangeira: o veterinario pode fechar a conta depois, e o
         * registro de que ele leu o historico nao pode virar linha sem nome.
         */
        @Test
        @DisplayName("guarda o nome do veterinario no momento do acesso")
        void guardaONomeComoSnapshot() {
            logger.vetLeu(marina(), rex(), AccessedResource.VACCINES);

            assertThat(gravado().getActorName()).isEqualTo("Dra. Marina");
        }

        /** O tutor autorizou uma clinica, nao uma pessoa: e a clinica que ele reconhece. */
        @Test
        @DisplayName("guarda a clinica, que e o que o tutor autorizou")
        void guardaAClinica() {
            logger.vetLeu(marina(), rex(), AccessedResource.VACCINES);

            assertThat(gravado().getOrganizationName()).isEqualTo("Clinica Bicho Feliz");
        }

        /** Person sem clinica nao deveria existir, mas o log nao e o lugar de estourar por isso. */
        @Test
        @DisplayName("veterinario sem clinica registra o acesso sem o nome dela")
        void vetSemClinicaNaoQuebra() {
            var semClinica = Person.builder().personId(VET_ID).name("Dra. Marina").build();

            logger.vetLeu(semClinica, rex(), AccessedResource.VACCINES);

            assertThat(gravado().getOrganizationName()).isNull();
        }

        @Test
        @DisplayName("guarda a evidencia de rede do acesso")
        void guardaEvidencia() {
            when(requestEvidenceProvider.ip()).thenReturn("203.0.113.7");
            when(requestEvidenceProvider.userAgent()).thenReturn("Mozilla/5.0");

            logger.vetLeu(marina(), rex(), AccessedResource.VACCINES);

            var log = gravado();
            assertThat(log.getIpAddress()).isEqualTo("203.0.113.7");
            assertThat(log.getUserAgent()).isEqualTo("Mozilla/5.0");
        }
    }

    @Nested
    @DisplayName("linkPublicoAberto")
    class LinkPublico {

        /**
         * Sem ator identificado: quem abre o link nao tem conta. O que sobra e a
         * evidencia de rede, e e por isso que ela volta ao tutor - sem o IP, dois
         * acessos pelo mesmo link sao indistinguiveis.
         */
        @Test
        @DisplayName("registra sem ator, porque quem abre o link nao tem conta")
        void registraSemAtor() {
            when(requestEvidenceProvider.ip()).thenReturn("198.51.100.4");

            logger.linkPublicoAberto(rex());

            var log = gravado();
            assertThat(log.getActorType()).isEqualTo(AccessActorType.SHARE_LINK);
            assertThat(log.getActorId()).isNull();
            assertThat(log.getActorName()).isNull();
            assertThat(log.getOrganizationName()).isNull();
            assertThat(log.getResource()).isEqualTo(AccessedResource.SHARED_CARD);
            assertThat(log.getIpAddress()).isEqualTo("198.51.100.4");
        }
    }

    @Nested
    @DisplayName("politica de falha")
    class PoliticaDeFalha {

        /**
         * <b>Falha propaga, de proposito.</b> E o oposto do OrganizationActivityNotifier, e
         * a diferenca nao e descuido: la o efeito principal era o registro e o aviso
         * era acessorio; aqui o log <b>e</b> a garantia. Servir historico de saude sem
         * conseguir registrar quem leu entrega o dado e perde a unica prova de que
         * alguem o viu - e log de auditoria que falha em silencio e pior que log
         * nenhum, porque cria a impressao de cobertura.
         *
         * Este teste existe para que inverter a escolha seja uma decisao, e nao um
         * try acrescentado sem ninguem notar.
         */
        @Test
        @DisplayName("falha ao gravar o log derruba a leitura, e nao e engolida")
        void falhaDerrubaALeitura() {
            doThrow(new RuntimeException("tabela indisponivel"))
                    .when(sensitiveAccessLogRepository).save(any(SensitiveAccessLog.class));

            assertThatThrownBy(() -> logger.vetLeu(marina(), rex(), AccessedResource.HEALTH_RECORDS))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("tabela indisponivel");
        }

        @Test
        @DisplayName("vale igual para o link publico")
        void falhaDerrubaOLinkPublico() {
            doThrow(new RuntimeException("tabela indisponivel"))
                    .when(sensitiveAccessLogRepository).save(any(SensitiveAccessLog.class));

            assertThatThrownBy(() -> logger.linkPublicoAberto(rex()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

}
