package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AnimalShareRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.domain.repository.AnimalWeightHistoryRepository;
import br.com.petfy.healthcare.domain.repository.SensitiveAccessLogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.storage.AttachmentStorage;
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

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * A ordem dos deletes ao apagar um animal.
 *
 * Mora aqui, e nao nos servicos, porque era exatamente a duplicacao dessa
 * sequencia que causou o bug: apagar animal e apagar conta mantinham listas separadas
 * e elas divergiram. Um lugar para a ordem, um teste para a ordem.
 *
 * <b>Este teste nao prova que funciona.</b> Ele prova que os comandos saem na
 * sequencia pretendida; quem recusa uma ordem errada e a chave estrangeira, que so
 * existe no banco - isso esta no {@code AnimalDeletionContainerTest}. E a cobertura da
 * lista contra o schema esta no {@code AnimalPurgerCoverageContainerTest}.
 */
@ExtendWith(MockitoExtension.class)
class AnimalPurgerTest {

    @Mock private AnimalRepository animalRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AttachmentStorage attachmentStorage;
    @Mock private PetTutorRepository petTutorRepository;
    @Mock private PetTutorInviteRepository petTutorInviteRepository;
    @Mock private VaccineRepository vaccineRepository;
    @Mock private VaccineCorrectionRepository vaccineCorrectionRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    @Mock private AnimalWeightHistoryRepository animalWeightHistoryRepository;
    @Mock private AntiparasiticRepository antiparasiticRepository;
    @Mock private AnimalShareRepository animalShareRepository;
    @Mock private PetClinicAccessRepository petClinicAccessRepository;
    @Mock private SensitiveAccessLogRepository sensitiveAccessLogRepository;
    @Mock private AnimalHealthConditionRepository animalHealthConditionRepository;

    @InjectMocks
    private AnimalPurger animalPurger;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final List<UUID> UM_PET = List.of(ANIMAL_ID);

    private InOrder ordemDeTudo() {
        return inOrder(
                attachmentRepository, vaccineCorrectionRepository, healthRecordCorrectionRepository,
                vaccineRepository, healthRecordRepository,
                animalWeightHistoryRepository, antiparasiticRepository,
                animalShareRepository, petClinicAccessRepository, sensitiveAccessLogRepository,
                animalHealthConditionRepository,
                petTutorInviteRepository, petTutorRepository,
                animalRepository, attachmentStorage);
    }

    @Nested
    @DisplayName("purge")
    class Purge {

        /**
         * Netas antes das filhas, filhas antes do pai. As correcoes apontam para
         * vacina e historico; o resto aponta para o animal. Fora dessa ordem, cada
         * delete e recusado por violacao de chave estrangeira.
         */
        @Test
        @DisplayName("apaga de baixo para cima, terminando no animal")
        void apagaDeBaixoParaCima() {
            animalPurger.purge(UM_PET);

            InOrder ordem = ordemDeTudo();

            // O anexo sai primeiro: a linha aponta para vacina e para historico, entao e
            // neta e filha ao mesmo tempo. E as chaves de storage sao lidas ANTES de
            // qualquer delete, senao se perde a referencia ao que ficou no disco.
            ordem.verify(attachmentRepository).findStorageKeysByAnimalIdIn(UM_PET);
            ordem.verify(attachmentStorage).delete(anyCollection());
            ordem.verify(attachmentRepository).deleteByAnimalAnimalIdIn(UM_PET);

            // netas
            ordem.verify(vaccineCorrectionRepository).deleteByVaccineAnimalAnimalIdIn(UM_PET);
            ordem.verify(healthRecordCorrectionRepository).deleteByHealthRecordAnimalAnimalIdIn(UM_PET);

            // filhas
            ordem.verify(vaccineRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(healthRecordRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(animalWeightHistoryRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(antiparasiticRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(animalShareRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(petClinicAccessRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(sensitiveAccessLogRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(animalHealthConditionRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(petTutorInviteRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(petTutorRepository).deleteByAnimalAnimalIdIn(UM_PET);

            // o pai, por ultimo
            ordem.verify(animalRepository).deleteByAnimalIdIn(UM_PET);
        }

        /**
         * Peso e antiparasitario chegaram no passo 9 e ficaram de fora das duas
         * cascatas, o que travou o DELETE /owners/me para qualquer animal com pesagem.
         * Ficam nomeados num caso proprio para que remove-los seja uma decisao, e
         * nao um esquecimento silencioso.
         */
        @Test
        @DisplayName("nao esquece o peso nem o antiparasitario, que ja ficaram de fora")
        void naoEsqueceOPesoNemOAntiparasitario() {
            animalPurger.purge(UM_PET);

            InOrder ordem = inOrder(animalWeightHistoryRepository, antiparasiticRepository, animalRepository);
            ordem.verify(animalWeightHistoryRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(antiparasiticRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(animalRepository).deleteByAnimalIdIn(UM_PET);
        }

        /** Vale para o lote: apagar a conta pode levar vários animals de uma vez. */
        @Test
        @DisplayName("apaga varios animals no mesmo lote")
        void apagaVariosAnimals() {
            var doisAnimals = List.of(ANIMAL_ID, UUID.fromString("44444444-4444-4444-4444-444444444444"));

            animalPurger.purge(doisAnimals);

            InOrder ordem = inOrder(vaccineRepository, animalRepository);
            ordem.verify(vaccineRepository).deleteByAnimalAnimalIdIn(doisAnimals);
            ordem.verify(animalRepository).deleteByAnimalIdIn(doisAnimals);
        }

        /**
         * Lista vazia acontece no caminho normal: quem apaga a conta pode nao ter
         * animal que morra, porque todos tem outro tutor. Emitir os deletes com IN ()
         * nao faria nada, mas seriam dez idas ao banco por nada.
         */
        @Test
        @DisplayName("lista vazia nao emite delete nenhum")
        void listaVaziaNaoEmiteDelete() {
            animalPurger.purge(List.of());

            verifyNoInteractions(
                    attachmentRepository, vaccineCorrectionRepository, healthRecordCorrectionRepository,
                    vaccineRepository, healthRecordRepository,
                    animalWeightHistoryRepository, antiparasiticRepository,
                    animalShareRepository, petClinicAccessRepository, sensitiveAccessLogRepository,
                animalHealthConditionRepository,
                    petTutorInviteRepository, petTutorRepository,
                    animalRepository, attachmentStorage);
        }
    }

    @Nested
    @DisplayName("purgeConteudo")
    class PurgeConteudo {

        /**
         * Usado pelo DELETE /animals/{id}, que apaga o animal pela entidade que o guard ja
         * carregou. Se este metodo apagasse o animal tambem, o servico tentaria apagar
         * duas vezes.
         */
        @Test
        @DisplayName("limpa tudo mas nao apaga o animal")
        void limpaTudoMasNaoApagaOAnimal() {
            animalPurger.purgeConteudo(UM_PET);

            InOrder ordem = ordemDeTudo();
            ordem.verify(vaccineCorrectionRepository).deleteByVaccineAnimalAnimalIdIn(UM_PET);
            ordem.verify(vaccineRepository).deleteByAnimalAnimalIdIn(UM_PET);
            ordem.verify(petTutorRepository).deleteByAnimalAnimalIdIn(UM_PET);

            verifyNoInteractions(animalRepository);
        }

        @Test
        @DisplayName("lista vazia nao emite delete nenhum")
        void listaVaziaNaoEmiteDelete() {
            animalPurger.purgeConteudo(List.of());

            verifyNoInteractions(vaccineRepository, petTutorRepository, animalRepository);
        }
    }

}
