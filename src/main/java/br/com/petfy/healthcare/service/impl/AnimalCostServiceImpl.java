package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O custo do animal (Telas 40, 41 e 42).
 *
 * <b>A ASSIMETRIA E A REGRA INTEIRA: escreve quem registra, le so quem responde.</b>
 *
 * Escrever exige o mesmo que registrar qualquer evento — a clinica lanca o valor do atendimento
 * que ela mesma acabou de fazer, e a creche lanca a mensalidade que ela combinou. Se escrever
 * exigisse custodia, o custo so entraria se o tutor digitasse, e o desenho abre justamente
 * recusando isso: "o custo do animal so existe se o dado entrar sem esforco".
 *
 * <b>Ler exige CUSTODIA, e nenhum escopo substitui.</b> "O que a Clinica Vet Norte cobra do
 * Marcelo nao e assunto da creche, do petshop nem de outra clinica. (...) Nenhum escopo de acesso
 * concede preco junto com saude." E por isso que o custo nao entra na linha do tempo: la quem
 * governa a leitura e o escopo, e aqui o escopo nao pode governar nada.
 *
 * <b>A clinica nao le o que ela mesma escreveu, e isso e proposital.</b> Ela sabe o que cobrou —
 * esta no sistema dela. O que ela nao pode ver e o que as OUTRAS cobraram, e uma regra que
 * abrisse excecao para "o meu" precisaria distinguir os dois na leitura, com um filtro que a
 * primeira consulta mal escrita derruba.
 */
@Service
@RequiredArgsConstructor
public class AnimalCostServiceImpl implements AnimalCostService {

    private final AnimalCostRepository animalCostRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    /**
     * O que o tutor ve: tudo que se gastou com o animal.
     *
     * <b>{@code requireCustodia} e nao {@code requireLeitura}</b> — e a unica leitura deste
     * produto que exige responder pelo animal em vez de alcanca-lo.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AnimalCostResponseDTO> doAnimal(UUID animalId) {
        animalAccessGuard.requireCustodia(animalId);

        return animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(animalId)
                .stream()
                .map(AnimalCostServiceImpl::toResponse)
                .toList();
    }

    /**
     * Lanca um valor.
     *
     * <b>{@code requireEscrita}</b>: quem pode registrar um evento no animal pode dizer quanto ele
     * custou. E o mesmo alcance que ja permitiu registrar o atendimento — pedir mais aqui faria o
     * valor ficar de fora justamente de quem tem o dado na mao.
     */
    @Override
    @Transactional
    public AnimalCostResponseDTO lancar(UUID animalId, AnimalCostRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Person eu = currentPersonProvider.require();

        AnimalCost custo = animalCostRepository.save(AnimalCost.builder()
                .animal(animal)
                .description(request.getDescription().trim())
                .amount(request.getAmount())
                .kind(request.getKind() == null ? AnimalCostKind.COMPRA : request.getKind())
                .paid(request.getPaid())
                .recurrence(request.getRecurrence())
                .occurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt())
                .sourceHealthRecordId(request.getSourceHealthRecordId())
                .sourceEnrollmentId(request.getSourceEnrollmentId())
                .recordedBy(eu)
                .organization(currentProfessionalProvider.organizacaoDeclarada(eu).orElse(null))
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(custo);
    }

    private static AnimalCostResponseDTO toResponse(AnimalCost custo) {
        return AnimalCostResponseDTO.builder()
                .animalCostId(custo.getAnimalCostId())
                .description(custo.getDescription())
                .amount(custo.getAmount())
                .kind(custo.getKind())
                .paid(custo.getPaid())
                .recurrence(custo.getRecurrence())
                .occurredAt(custo.getOccurredAt())
                .recordedByName(custo.getRecordedBy() == null ? null : custo.getRecordedBy().getName())
                .organizationName(custo.getOrganization() == null ? null : custo.getOrganization().getName())
                .sourceHealthRecordId(custo.getSourceHealthRecordId())
                .sourceEnrollmentId(custo.getSourceEnrollmentId())
                .build();
    }

}
