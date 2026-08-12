package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalSearchResultDTO;
import br.com.petfy.healthcare.service.AnimalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A busca autenticada (Tela 35).
 *
 * <b>Autenticada, ao contrario do `POST /found`</b> — e as duas existem de proposito. A publica
 * responde a quem esta na calcada com um animal na mao e nao tem conta; esta responde a quem tem
 * conta e quer achar um animal que ela ja alcanca. Uma so faria uma das duas mal: ou entregaria
 * contato de tutor a qualquer um, ou exigiria cadastro de quem esta com um cachorro perdido.
 */
@RestController
@RequiredArgsConstructor
public class AnimalSearchController {

    private final AnimalSearchService animalSearchService;

    @Operation(summary = "Busca por nome, microchip ou RGA",
               description = "Os tres campos numa consulta so, porque quem digita nao sabe em qual "
                             + "esta digitando. Devolve em DOIS grupos — os que voce responde e os "
                             + "que voce alcanca pela organizacao em que age — e diz se existem "
                             + "outros que casam e que voce NAO alcanca. Essa ultima parte e "
                             + "deliberada: calar sobre o resto faria a pessoa concluir que o animal "
                             + "nao esta no Petfy. E booleano, e nao contagem: um numero seria um "
                             + "oraculo sobre quantos animais existem com cada prefixo de microchip.")
    @GetMapping("/animals/search")
    public ResponseEntity<AnimalSearchResultDTO> buscar(@RequestParam("q") String q) {
        return ResponseEntity.ok(animalSearchService.buscar(q));
    }

}
