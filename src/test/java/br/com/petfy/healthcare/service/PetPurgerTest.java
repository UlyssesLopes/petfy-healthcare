package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetShareRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.domain.repository.PetWeightHistoryRepository;
import br.com.petfy.healthcare.domain.repository.SensitiveAccessLogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * A ordem dos deletes ao apagar um pet.
 *
 * Mora aqui, e nao nos servicos, porque era exatamente a duplicacao dessa
 * sequencia que causou o bug: apagar pet e apagar conta mantinham listas separadas
 * e elas divergiram. Um lugar para a ordem, um teste para a ordem.
 *
 * <b>Este teste nao prova que funciona.</b> Ele prova que os comandos saem na
 * sequencia pretendida; quem recusa uma ordem errada e a chave estrangeira, que so
 * existe no banco - isso esta no {@code PetDeletionContainerTest}. E a cobertura da
 * lista contra o schema esta no {@code PetPurgerCoverageContainerTest}.
 */
@ExtendWith(MockitoExtension.class)
class PetPurgerTest {

    @Mock private PetRepository petRepository;
    @Mock private PetTutorRepository petTutorRepository;
    @Mock private PetTutorInviteRepository petTutorInviteRepository;
    @Mock private VaccineRepository vaccineRepository;
    @Mock private VaccineCorrectionRepository vaccineCorrectionRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    @Mock private PetWeightHistoryRepository petWeightHistoryRepository;
    @Mock private AntiparasiticRepository antiparasiticRepository;
    @Mock private PetShareRepository petShareRepository;
    @Mock private PetClinicAccessRepository petClinicAccessRepository;
    @Mock private SensitiveAccessLogRepository sensitiveAccessLogRepository;

    @InjectMocks
    private PetPurger petPurger;

    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final List<UUID> UM_PET = List.of(PET_ID);

    private InOrder ordemDeTudo() {
        return inOrder(
                vaccineCorrectionRepository, healthRecordCorrectionRepository,
                vaccineRepository, healthRecordRepository,
                petWeightHistoryRepository, antiparasiticRepository,
                petShareRepository, petClinicAccessRepository, sensitiveAccessLogRepository,
                petTutorInviteRepository, petTutorRepository,
                petRepository);
    }

    @Nested
    @DisplayName("purge")
    class Purge {

        /**
         * Netas antes das filhas, filhas antes do pai. As correcoes apontam para
         * vacina e historico; o resto aponta para o pet. Fora dessa ordem, cada
         * delete e recusado por violacao de chave estrangeira.
         */
        @Test
        @DisplayName("apaga de baixo para cima, terminando no pet")
        void apagaDeBaixoParaCima() {
            petPurger.purge(UM_PET);

            InOrder ordem = ordemDeTudo();

            // netas
            ordem.verify(vaccineCorrectionRepository).deleteByVaccinePetPetIdIn(UM_PET);
            ordem.verify(healthRecordCorrectionRepository).deleteByHealthRecordPetPetIdIn(UM_PET);

            // filhas
            ordem.verify(vaccineRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(healthRecordRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petWeightHistoryRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(antiparasiticRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petShareRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petClinicAccessRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(sensitiveAccessLogRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petTutorInviteRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petTutorRepository).deleteByPetPetIdIn(UM_PET);

            // o pai, por ultimo
            ordem.verify(petRepository).deleteByPetIdIn(UM_PET);
        }

        /**
         * Peso e antiparasitario chegaram no passo 9 e ficaram de fora das duas
         * cascatas, o que travou o DELETE /owners/me para qualquer pet com pesagem.
         * Ficam nomeados num caso proprio para que remove-los seja uma decisao, e
         * nao um esquecimento silencioso.
         */
        @Test
        @DisplayName("nao esquece o peso nem o antiparasitario, que ja ficaram de fora")
        void naoEsqueceOPesoNemOAntiparasitario() {
            petPurger.purge(UM_PET);

            InOrder ordem = inOrder(petWeightHistoryRepository, antiparasiticRepository, petRepository);
            ordem.verify(petWeightHistoryRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(antiparasiticRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petRepository).deleteByPetIdIn(UM_PET);
        }

        /** Vale para o lote: apagar a conta pode levar vários pets de uma vez. */
        @Test
        @DisplayName("apaga varios pets no mesmo lote")
        void apagaVariosPets() {
            var doisPets = List.of(PET_ID, UUID.fromString("44444444-4444-4444-4444-444444444444"));

            petPurger.purge(doisPets);

            InOrder ordem = inOrder(vaccineRepository, petRepository);
            ordem.verify(vaccineRepository).deleteByPetPetIdIn(doisPets);
            ordem.verify(petRepository).deleteByPetIdIn(doisPets);
        }

        /**
         * Lista vazia acontece no caminho normal: quem apaga a conta pode nao ter
         * pet que morra, porque todos tem outro tutor. Emitir os deletes com IN ()
         * nao faria nada, mas seriam dez idas ao banco por nada.
         */
        @Test
        @DisplayName("lista vazia nao emite delete nenhum")
        void listaVaziaNaoEmiteDelete() {
            petPurger.purge(List.of());

            verifyNoInteractions(
                    vaccineCorrectionRepository, healthRecordCorrectionRepository,
                    vaccineRepository, healthRecordRepository,
                    petWeightHistoryRepository, antiparasiticRepository,
                    petShareRepository, petClinicAccessRepository, sensitiveAccessLogRepository,
                    petTutorInviteRepository, petTutorRepository,
                    petRepository);
        }
    }

    @Nested
    @DisplayName("purgeConteudo")
    class PurgeConteudo {

        /**
         * Usado pelo DELETE /pets/{id}, que apaga o pet pela entidade que o guard ja
         * carregou. Se este metodo apagasse o pet tambem, o servico tentaria apagar
         * duas vezes.
         */
        @Test
        @DisplayName("limpa tudo mas nao apaga o pet")
        void limpaTudoMasNaoApagaOPet() {
            petPurger.purgeConteudo(UM_PET);

            InOrder ordem = ordemDeTudo();
            ordem.verify(vaccineCorrectionRepository).deleteByVaccinePetPetIdIn(UM_PET);
            ordem.verify(vaccineRepository).deleteByPetPetIdIn(UM_PET);
            ordem.verify(petTutorRepository).deleteByPetPetIdIn(UM_PET);

            verifyNoInteractions(petRepository);
        }

        @Test
        @DisplayName("lista vazia nao emite delete nenhum")
        void listaVaziaNaoEmiteDelete() {
            petPurger.purgeConteudo(List.of());

            verifyNoInteractions(vaccineRepository, petTutorRepository, petRepository);
        }
    }

}
