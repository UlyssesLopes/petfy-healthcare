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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Apaga um animal e tudo que pende dele, na ordem que o banco aceita.
 *
 * <b>Existe porque a lista estava duplicada e as duas copias divergiram.</b>
 * Apagar o animal e apagar a conta terminam no mesmo lugar - um animal deixando de
 * existir -, mas cada um mantinha sua propria sequencia de deletes. Quando o passo
 * 9 trouxe peso e antiparasitario, nenhuma das duas foi atualizada: o
 * {@code DELETE /animals/{id}} passou a falhar para qualquer animal com vacina, e o
 * {@code DELETE /persons/me} para qualquer animal com pesagem. Os dois respondiam 500,
 * e nenhum teste de mock viu - quem recusa e a chave estrangeira, que so existe no
 * banco.
 *
 * Com uma lista so, a proxima tabela que apontar para {@code animals} tem um lugar
 * unico para entrar, e a divergencia deixa de ser possivel.
 *
 * <b>A ordem e obrigatoria:</b> netas antes das filhas, filhas antes das pais. As
 * correcoes apontam para vacina e historico; vacina, historico, peso,
 * antiparasitario, share, acesso de clinica, vinculo de tutor e convite apontam
 * para o animal. O schema nao tem {@code ON DELETE CASCADE} em lugar nenhum - foi
 * escolha deliberada, para manter a decisao visivel em codigo e testavel.
 */
@Service
@RequiredArgsConstructor
public class AnimalPurger {

    private final AnimalRepository animalRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorage attachmentStorage;
    private final PetTutorRepository petTutorRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final VaccineRepository vaccineRepository;
    private final VaccineCorrectionRepository vaccineCorrectionRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    private final AnimalWeightHistoryRepository animalWeightHistoryRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final AnimalShareRepository animalShareRepository;
    private final PetClinicAccessRepository petClinicAccessRepository;
    private final SensitiveAccessLogRepository sensitiveAccessLogRepository;
    private final AnimalHealthConditionRepository animalHealthConditionRepository;

    /**
     * Apaga os animals informados e todo o rastro deles, o proprio animal incluido.
     *
     * Lista vazia nao e erro: quem apaga a conta pode nao ter animal que morra, e
     * emitir os deletes com {@code IN ()} nao adianta nada.
     */
    @Transactional
    public void purge(List<UUID> animalIds) {
        if (animalIds.isEmpty()) {
            return;
        }

        purgeConteudo(animalIds);
        animalRepository.deleteByAnimalIdIn(animalIds);
    }

    /**
     * Tudo que pende do animal, sem apagar o animal.
     *
     * Separado para que {@link #purge(List)} nao seja a unica forma de usar isto: a
     * exclusao de conta apaga em lote com {@code deleteByAnimalIdIn}, e apagar um animal
     * so passa pela entidade ja carregada pelo guard - ver
     * {@code AnimalServiceImpl.deleteAnimal}.
     */
    @Transactional
    public void purgeConteudo(List<UUID> animalIds) {
        if (animalIds.isEmpty()) {
            return;
        }

        // Os anexos saem primeiro, e por dois motivos.
        //
        // Ordem no banco: a linha aponta para vacina e para historico, entao seguraria
        // os deletes seguintes - ela e neta e filha ao mesmo tempo.
        //
        // E os BYTES tem de sair junto. O storage nao participa da transacao, entao as
        // chaves sao lidas antes de qualquer delete: apagar as linhas primeiro perderia
        // a unica referencia ao que ficou no disco, e arquivo orfao com laudo dentro e
        // dado pessoal nao apagado - o oposto do que um pedido de exclusao pede.
        //
        // Se o storage falhar, a excecao sobe e nada e apagado: estado consistente e
        // reanimalivel, em vez de banco limpo com arquivo sobrando. Mesma postura do log de
        // acesso - falhar fechado onde o dado e sensivel.
        List<String> chavesDeAnexo = attachmentRepository.findStorageKeysByAnimalIdIn(animalIds);
        attachmentStorage.delete(chavesDeAnexo);
        attachmentRepository.deleteByAnimalAnimalIdIn(animalIds);

        // netas: apontam para vacina e para historico
        vaccineCorrectionRepository.deleteByVaccineAnimalAnimalIdIn(animalIds);
        healthRecordCorrectionRepository.deleteByHealthRecordAnimalAnimalIdIn(animalIds);

        // filhas: apontam para o animal
        vaccineRepository.deleteByAnimalAnimalIdIn(animalIds);
        healthRecordRepository.deleteByAnimalAnimalIdIn(animalIds);
        animalWeightHistoryRepository.deleteByAnimalAnimalIdIn(animalIds);
        antiparasiticRepository.deleteByAnimalAnimalIdIn(animalIds);
        animalShareRepository.deleteByAnimalAnimalIdIn(animalIds);
        petClinicAccessRepository.deleteByAnimalAnimalIdIn(animalIds);
        sensitiveAccessLogRepository.deleteByAnimalAnimalIdIn(animalIds);
        animalHealthConditionRepository.deleteByAnimalAnimalIdIn(animalIds);

        // o convite sai antes do vinculo por clareza, nao por dependencia: um
        // aponta para o animal, o outro tambem, e nenhum dos dois aponta para o outro
        petTutorInviteRepository.deleteByAnimalAnimalIdIn(animalIds);
        petTutorRepository.deleteByAnimalAnimalIdIn(animalIds);
    }

}
