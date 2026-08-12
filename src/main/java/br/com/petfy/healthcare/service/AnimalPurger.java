package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.domain.repository.AnimalDeathRepository;
import br.com.petfy.healthcare.domain.repository.AnimalMergeRequestRepository;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.AttendanceRepository;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionFulfillmentRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordCorrectionRepository;
import br.com.petfy.healthcare.domain.repository.ObservationRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
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
    private final AnimalCostRepository animalCostRepository;
    private final AnimalDeathRepository animalDeathRepository;
    private final AnimalMergeRequestRepository animalMergeRequestRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorage attachmentStorage;
    private final CustodyRepository custodyRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final VaccineRepository vaccineRepository;
    private final VaccineCorrectionRepository vaccineCorrectionRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    private final AnimalWeightHistoryRepository animalWeightHistoryRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GrantRepository grantRepository;
    private final SensitiveAccessLogRepository sensitiveAccessLogRepository;
    private final AnimalHealthConditionRepository animalHealthConditionRepository;
    private final CareInstructionRepository careInstructionRepository;
    private final ObservationRepository observationRepository;
    private final CareInstructionFulfillmentRepository careInstructionFulfillmentRepository;

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
        // repetivel, em vez de banco limpo com arquivo sobrando. Mesma postura do log de
        // acesso - falhar fechado onde o dado e sensivel.
        List<String> chavesDeAnexo = attachmentRepository.findStorageKeysByAnimalIdIn(animalIds);
        attachmentStorage.delete(chavesDeAnexo);
        attachmentRepository.deleteByAnimalAnimalIdIn(animalIds);

        // netas: apontam para vacina, para historico e para orientacao
        //
        // A PRESENCA E NETA DO ANIMAL: ela aponta para a matricula, que aponta para o animal. Sai
        // aqui, antes da matricula — e as duas entraram porque o
        // `AnimalPurgerCoverageContainerTest` acusou a tabela `enrollments` recem-criada como "nova
        // apontando para animals sem entrar no AnimalPurger". Sem isso, apagar o animal e apagar a
        // conta responderiam 500 no primeiro animal com matricula.
        attendanceRepository.deleteByEnrollmentAnimalAnimalIdIn(animalIds);
        vaccineCorrectionRepository.deleteByVaccineAnimalAnimalIdIn(animalIds);
        healthRecordCorrectionRepository.deleteByHealthRecordAnimalAnimalIdIn(animalIds);
        careInstructionFulfillmentRepository.deleteByAnimalIdIn(animalIds);

        // O PEDIDO DE UNIAO APONTA PARA DOIS ANIMALS, e some com qualquer um deles.
        //
        // Entrou porque o `AnimalPurgerCoverageContainerTest` acusou a tabela recem-criada — a
        // quarta vez que essa guarda pega a mesma classe de defeito. Sem ela, apagar um animal que
        // tenha pedido de uniao em qualquer dos lados responderia 500 por chave estrangeira.
        //
        // Sai antes das filhas por clareza: ele nao depende de nenhuma delas, mas e o unico da
        // lista que pode citar um animal que NAO esta sendo apagado.
        animalMergeRequestRepository.deleteAll(
                animalMergeRequestRepository.findEnvolvendoQualquer(animalIds));

        // O custo aponta para o animal, e tambem para o atendimento e para a matricula de onde
        // saiu — entao sai ANTES das duas, senao o delete delas esbarra nesta chave estrangeira.
        animalCostRepository.deleteByAnimalAnimalIdIn(animalIds);

        // O OBITO E FILHA DO ANIMAL PELA PROPRIA CHAVE (Tela 33): o `animal_id` e a PK dela.
        //
        // Vale dizer o que este delete significa, porque as duas operacoes se parecem e sao
        // opostas: encerrar a linha do tempo GUARDA a vida registrada, e apagar o animal a
        // destroi. Quem chega aqui pediu a segunda coisa — pelo DELETE do animal ou pela exclusao
        // da conta —, e a linha do obito vai junto com todo o resto.
        animalDeathRepository.deleteByAnimalIdIn(animalIds);

        // filhas: apontam para o animal
        vaccineRepository.deleteByAnimalAnimalIdIn(animalIds);
        healthRecordRepository.deleteByAnimalAnimalIdIn(animalIds);
        animalWeightHistoryRepository.deleteByAnimalAnimalIdIn(animalIds);
        antiparasiticRepository.deleteByAnimalAnimalIdIn(animalIds);
        // por entidade, e nao em massa: grant_scopes aponta para grants, e um delete
        // em massa deixaria os escopos orfaos - que o Postgres recusa e o mock nao
        grantRepository.deleteAll(grantRepository.findByAnimalAnimalIdIn(animalIds));
        sensitiveAccessLogRepository.deleteByAnimalAnimalIdIn(animalIds);
        animalHealthConditionRepository.deleteByAnimalAnimalIdIn(animalIds);
        careInstructionRepository.deleteByAnimalAnimalIdIn(animalIds);
        observationRepository.deleteByAnimalAnimalIdIn(animalIds);
        enrollmentRepository.deleteByAnimalAnimalIdIn(animalIds);

        // o convite sai antes da custodia por clareza, nao por dependencia: um
        // aponta para o animal, o outro tambem, e nenhum dos dois aponta para o outro
        petTutorInviteRepository.deleteByAnimalAnimalIdIn(animalIds);

        // A custodia aponta para si mesma pelo sucessor, entao um delete em massa
        // esbarraria na propria FK dependendo da ordem que o banco escolher. Desfazer
        // a corrente antes de apagar e o que torna a ordem irrelevante - e o flush no
        // meio nao e decoracao: sem ele o UPDATE poderia sair depois do DELETE.
        List<Custody> custodias = custodyRepository.findByAnimalAnimalIdIn(animalIds);
        custodias.forEach(c -> c.setSuccessor(null));
        custodyRepository.saveAll(custodias);
        custodyRepository.flush();
        custodyRepository.deleteAll(custodias);
    }

}
