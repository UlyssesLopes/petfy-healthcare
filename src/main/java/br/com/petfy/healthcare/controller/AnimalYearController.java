package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalYearDTO;
import br.com.petfy.healthcare.service.AnimalYearService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O ano do animal (Tela 48).
 *
 * <b>Uma rota de LEITURA, e so.</b> Nao ha nada a criar aqui: o resumo e uma leitura da vida
 * registrada, montada na hora. Guardar o resumo do ano numa tabela seria congelar numeros que mudam
 * quando alguem corrige uma data — e corrigir data e um direito que este produto garante desde a
 * contestacao de registro.
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalYearController {

    private final AnimalYearService animalYearService;

    @Operation(summary = "O ano do animal, em uma pagina",
               description = "Os doze meses que terminam em `to` (hoje, se omitido): quantos "
                             + "registros, por quantas pessoas e organizacoes, dias na creche e de "
                             + "hospedagem, peso no comeco e no fim, e o que aconteceu de saude. "
                             + "INCLUI O QUE DEU ERRADO — os dias em que uma vacina esteve vencida e "
                             + "as condicoes cronicas que ninguem reavaliou —, porque um resumo que "
                             + "so mostra o bonito nao serve para cuidar. Exige apenas leitura, e o "
                             + "escopo de quem le nao mascara os numeros: eles sao contagens, e nao "
                             + "conteudo clinico.")
    @GetMapping("/{animalId}/year")
    public ResponseEntity<AnimalYearDTO> ano(
            @PathVariable UUID animalId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(animalYearService.resumo(animalId, to));
    }

}
