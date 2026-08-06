package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;
import br.com.petfy.healthcare.service.DueItemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tudo que cobra acao de quem esta autenticado, num lugar so.
 *
 * <b>Nao esta sob {@code /animals}</b>, ao contrario do resto do prontuario: a pergunta
 * que ela responde nao e sobre um animal, e sobre a pessoa - "o que eu preciso fazer?".
 * O consentimento pendente, que nem tem animal, e a prova de que a ancora aqui e outra.
 */
@RestController
@RequestMapping("/due-items")
@RequiredArgsConstructor
public class DueItemController {

    private final DueItemService dueItemService;

    @Operation(summary = "O que cobra acao de quem esta autenticado",
               description = "Dose de vacina, antiparasitario, orientacao a cumprir, convite "
                             + "aguardando resposta e consentimento pendente, do mais atrasado ao "
                             + "menos urgente. Derivada de cada fonte a cada chamada - nao existe "
                             + "tabela de pendencia, porque a primeira a divergir cobraria algo "
                             + "que ja foi feito. windowDays define quanto do futuro entra; o que "
                             + "ja venceu entra sempre.")
    @GetMapping
    public ResponseEntity<List<DueItemResponseDTO>> listar(
            @RequestParam(defaultValue = "30") int windowDays) {
        return ResponseEntity.ok(dueItemService.doAutenticado(windowDays));
    }

}
