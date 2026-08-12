package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalDeathRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClosedLifeResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalDeath;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.EnrollmentStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalDeathRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.AnimalDeathNotifier;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.AnimalDeathService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Encerrar a linha do tempo de um animal que morreu (Tela 33).
 *
 * <b>O produto nao tinha isto, e o que ele tinha no lugar era pior que nada:</b> o
 * {@code DELETE /animals/{id}}, que apaga a carteira, o prontuario, o peso, os anexos e os
 * arquivos no disco. Quem perdia o animal escolhia entre destruir sete anos de registro e conviver
 * para sempre com um cadastro que continua cobrando vacina.
 *
 * <b>Encerra quem responde, e nao quem atendeu.</b> "A veterinaria que atendeu o Code na ultima
 * noite pode registrar o obito como ato clinico dela — isso e o trabalho dela. Mas fechar a linha
 * do tempo e do Marcelo, e nao pode acontecer sem ele. Ninguem deve descobrir que perdeu o animal
 * por uma notificacao do sistema." Por isso a guarda e {@code requireCustodia}, e nao
 * {@code requireEscrita}: nenhum nivel de concessao chega aqui.
 *
 * <b>A custodia da organizacao encerra tambem</b>, e isso vem de graca do {@code requireCustodia}:
 * um animal do abrigo tambem morre, e nao ha tutor humano nenhum para preencher o formulario.
 */
@Service
@RequiredArgsConstructor
public class AnimalDeathServiceImpl implements AnimalDeathService {

    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final AnimalDeathRepository animalDeathRepository;
    private final CustodyRepository custodyRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TimelineRepository timelineRepository;
    private final AnimalDeathNotifier animalDeathNotifier;

    /**
     * Tudo numa transacao so, e a ordem importa pouco — menos para o aviso.
     *
     * <b>O aviso sai por ultimo e fora da transacao logica</b>: ele so enfileira, e uma falha ali
     * nao pode desfazer o encerramento. O tutor que preencheu este formulario nao vai preenche-lo
     * de novo porque o SMTP caiu.
     */
    @Override
    @Transactional
    public ClosedLifeResponseDTO registrar(UUID animalId, AnimalDeathRequestDTO dto) {
        Animal animal = animalAccessGuard.requireCustodia(animalId);
        Person quemEncerrou = currentPersonProvider.require();

        exigirNaoEncerrado(animalId);
        exigirDataPossivel(dto.getDeceasedOn(), animal);

        LocalDateTime agora = LocalDateTime.now();

        /*
         * saveAndFlush, e nao save: o evento de obito da linha do tempo E esta linha, lida por uma
         * view. Sem o flush, a contagem que este mesmo metodo devolve no fim sairia sem o evento
         * que acabou de ser criado — e a ficha fechada abriria dizendo um evento a menos.
         */
        AnimalDeath obito = animalDeathRepository.saveAndFlush(AnimalDeath.builder()
                .animal(animal)
                .deceasedOn(dto.getDeceasedOn())
                .place(vazioVira(dto.getPlace()))
                .farewellNote(vazioVira(dto.getFarewellNote()))
                .recordedBy(quemEncerrou)
                .recordedAt(agora)
                .build());

        Custody encerrada = encerrarCustodia(animalId, agora);
        encerrarMatriculas(animalId, agora);

        animalDeathNotifier.animalMorreu(animal, quemEncerrou);

        return montar(animal, obito, encerrada);
    }

    @Override
    @Transactional(readOnly = true)
    public ClosedLifeResponseDTO daFichaFechada(UUID animalId) {
        Animal animal = animalAccessGuard.requireLeitura(animalId);

        AnimalDeath obito = animalDeathRepository.findById(animalId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        Custody encerrada = custodyRepository
                .findEncerradaPorObitoDaPessoa(animalId,
                        currentPersonProvider.require().getPersonId())
                .orElse(null);

        return montar(animal, obito, encerrada);
    }

    /**
     * "Os avisos param hoje. Ninguem mais vai te cobrar uma vacina do Code."
     *
     * <b>E a custodia encerrada que produz isso, e nao um campo novo.</b> As pendencias sao
     * derivadas de quem responde pelo animal — sem custodia em curso, nao ha de quem cobrar. Um
     * marcador "silenciar avisos" faria o mesmo efeito por um segundo caminho, e o dia em que os
     * dois discordassem alguem receberia lembrete de vacina de um animal morto.
     *
     * <b>Sem sucessor, e o {@code CustodyEndReason.OBITO} e quem autoriza isso</b> — o quarto
     * invariante do produto diz que nenhuma custodia termina sem sucessor, e a excecao nao e um
     * caso esquecido: e o fim da propria necessidade de sucessor.
     */
    private Custody encerrarCustodia(UUID animalId, LocalDateTime agora) {
        return custodyRepository.findEmCurso(animalId)
                .map(custodia -> {
                    custodia.setEndedAt(agora);
                    custodia.setEndReason(CustodyEndReason.OBITO);
                    return custodyRepository.saveAndFlush(custodia);
                })
                // animal sem custodia em curso nao e erro aqui: pode ter havido uma perda antes,
                // que ja encerrou sem sucessor. O obito continua sendo o fato a registrar
                .orElse(null);
    }

    /**
     * "A matricula na creche e encerrada."
     *
     * <b>Encerrar aqui e o que evita o pior desfecho da tela:</b> sem isso, a creche continuaria
     * com o animal na lista da turma, e alguem perguntaria ao tutor por que ele nao apareceu.
     * Todas as vivas, e nao so as ativas — a matricula pendente de comprovacao tambem espera um
     * animal que nao vem.
     */
    private void encerrarMatriculas(UUID animalId, LocalDateTime agora) {
        List<Enrollment> vivas = enrollmentRepository.findVivasDoAnimal(animalId);

        vivas.forEach(matricula -> {
            matricula.setStatus(EnrollmentStatus.ENCERRADA);
            matricula.setEndedAt(agora);
        });

        enrollmentRepository.saveAll(vivas);
    }

    /**
     * Encerrar duas vezes e recusado, e nao aceito em silencio.
     *
     * A segunda chamada sobrescreveria a data que o tutor informou na primeira — o unico campo do
     * formulario que ele nao consegue reconstruir depois. E um cliente que repete a requisicao
     * quase sempre esta olhando um estado antigo da tela.
     */
    private void exigirNaoEncerrado(UUID animalId) {
        if (animalDeathRepository.existsById(animalId)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ANIMAL_TIMELINE_ALREADY_CLOSED.getMessage(),
                    ErrorMessageEnum.ANIMAL_TIMELINE_ALREADY_CLOSED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * As duas datas impossiveis, com mensagens diferentes e o mesmo codigo.
     *
     * <b>A recusa mora aqui e nao no banco</b> porque {@code CURRENT_DATE} nao e IMMUTABLE e o
     * Postgres nao aceita a expressao dentro de um CHECK. E o mesmo codigo para as duas porque a
     * tela faz a mesma coisa nos dois casos: aponta o campo "Quando foi".
     */
    private void exigirDataPossivel(LocalDate deceasedOn, Animal animal) {
        if (deceasedOn.isAfter(LocalDate.now())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_DEATH_DATE.getMessage(),
                    ErrorMessageEnum.INVALID_DEATH_DATE.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        if (animal.getBornDate() != null && deceasedOn.isBefore(animal.getBornDate())) {
            throw new PetfyHealthcareException(
                    "a data do obito e anterior ao nascimento do animal",
                    ErrorMessageEnum.INVALID_DEATH_DATE.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Texto em branco vira nulo.
     *
     * Um campo opcional que o cliente manda como string vazia nao e "o tutor escreveu nada": e o
     * formulario enviando o que tinha. Guardar a diferenca faria a linha do tempo mostrar um
     * evento de obito com uma despedida vazia.
     */
    private String vazioVira(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ClosedLifeResponseDTO montar(Animal animal, AnimalDeath obito, Custody custodia) {
        TimelineRepository.Tamanho tamanho = timelineRepository.tamanhoDe(animal.getAnimalId());

        return ClosedLifeResponseDTO.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .bornDate(animal.getBornDate())
                .deceasedOn(obito.getDeceasedOn())
                .place(obito.getPlace())
                .farewellNote(obito.getFarewellNote())
                .recordedAt(obito.getRecordedAt())
                .holderSince(custodia != null ? custodia.getStartedAt() : null)
                .holderUntil(custodia != null ? custodia.getEndedAt() : null)
                .eventCount(tamanho.getEventos())
                .caregiverPersonCount(tamanho.getPessoas())
                .caregiverOrganizationCount(tamanho.getOrganizacoes())
                .build();
    }

}
