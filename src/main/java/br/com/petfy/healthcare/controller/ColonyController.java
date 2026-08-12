package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ColonyAnimalDTO;
import br.com.petfy.healthcare.service.ColonyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A lista da colônia (Tela 43).
 *
 * <b>Fora de {@code /professional/**}</b>, pela mesma razão do acordo de duas pessoas: quem cuida
 * de uma colônia não tem CRMV — é a vizinha que alimenta de manhã. Quem entra aqui é membro ativo
 * do grupo declarado, e a checagem é do serviço.
 */
@RestController
@RequestMapping("/group/animals")
@RequiredArgsConstructor
public class ColonyController {

    private final ColonyService colonyService;

    @Operation(summary = "Os animais do grupo, com o ultimo avistamento",
               description = "A lista da colonia. Diferente da lista do abrigo (Tela 12) por uma "
                             + "coluna que so existe na rua: quantos dias desde que alguem viu o "
                             + "animal. Filtros: TODOS, FALTA_CASTRAR, EM_TRATAMENTO, SUMIDOS. "
                             + "SUMIDOS deixa de fora quem nunca foi marcado — sem informacao nao "
                             + "e desaparecido, e contar os dois juntos mandaria o grupo procurar "
                             + "um gato que esta na praca.")
    @GetMapping
    public ResponseEntity<List<ColonyAnimalDTO>> listar(
            @RequestParam(required = false) String filtro) {
        return ResponseEntity.ok(colonyService.listar(filtro));
    }

}
