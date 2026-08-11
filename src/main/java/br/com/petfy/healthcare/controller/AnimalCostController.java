package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostResponseDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostSummaryResponseDTO;
import br.com.petfy.healthcare.service.AnimalCostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Por onde o valor entra (Telas 40, 41 e 42).
 *
 * <b>Duas rotas e nada mais.</b> O desenho e explicito sobre o que NAO existe: "nenhuma integracao
 * com banco ou cartao. O Petfy nao olha sua conta. Nenhum orcamento mensal, nenhuma meta, nenhum
 * aviso de que voce passou do limite". E nao ha rota que agregue preco por organizacao — "registro
 * clinico que vira comparador de preco deixa de ser lugar seguro para a clinica registrar a
 * verdade".
 */
@RestController
@RequestMapping("/animals/{animalId}/costs")
@RequiredArgsConstructor
public class AnimalCostController {

    private final AnimalCostService animalCostService;

    @Operation(summary = "O que se gastou com este animal",
               description = "EXIGE CUSTODIA, e nenhum escopo de concessao substitui: o que uma "
                             + "clinica cobra do tutor nao e assunto da creche, do petshop nem de "
                             + "outra clinica. E a unica leitura deste produto que pede responder "
                             + "pelo animal em vez de alcanca-lo — e por isso o custo nao entra na "
                             + "linha do tempo, onde quem governa a leitura e o escopo.")
    @GetMapping
    public ResponseEntity<List<AnimalCostResponseDTO>> costs(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalCostService.doAnimal(animalId));
    }

    @Operation(summary = "Quanto o animal custou, somado (Tela 37)",
               description = "Os tres numeros do topo, o 'onde foi' por categoria e o 'quem pagou o "
                             + "que'. EXIGE CUSTODIA, como toda leitura de custo — e aqui com mais "
                             + "razao: o total e exatamente o numero que alguem de fora gostaria de "
                             + "saber sem ver os itens. `window` aceita DOZE_MESES (padrao) ou "
                             + "SEMPRE; qualquer outro valor e lido como DOZE_MESES. O total de "
                             + "sempre e o ano do primeiro valor viajam nas duas janelas, porque o "
                             + "cartao 'desde 2019' do desenho nao muda quando o recorte muda. NAO "
                             + "HA comparacao com outros tutores nem com outras organizacoes, e "
                             + "nao havera: 'quem cuida de um animal doente ja tem o suficiente na "
                             + "cabeca'.")
    @GetMapping("/summary")
    public ResponseEntity<AnimalCostSummaryResponseDTO> summary(
            @PathVariable UUID animalId,
            @RequestParam(required = false) String window) {
        return ResponseEntity.ok(animalCostService.resumo(animalId, window));
    }

    @Operation(summary = "Lanca um valor gasto com o animal",
               description = "Exige o mesmo alcance de escrita que registrar qualquer evento: a "
                             + "clinica lanca o valor do atendimento que ela mesma fez, a creche a "
                             + "mensalidade que combinou, e o tutor a racao que comprou. Pedir "
                             + "custodia aqui faria o valor ficar de fora justamente de quem tem o "
                             + "dado na mao. O campo e opcional em toda tela — evento sem valor e "
                             + "normal, nunca um erro e nunca um alerta.")
    @PostMapping
    public ResponseEntity<AnimalCostResponseDTO> addCost(
            @PathVariable UUID animalId,
            @Valid @RequestBody AnimalCostRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(animalCostService.lancar(animalId, request));
    }

}
