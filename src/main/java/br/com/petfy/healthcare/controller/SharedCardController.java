package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.service.AnimalShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rota publica: quem recebe o link nao tem conta no Petfy. O token no path e a
 * unica credencial, e por isso e gerado com 32 bytes de SecureRandom.
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class SharedCardController {

    private final AnimalShareService animalShareService;

    @Operation(summary = "O cartao do animal, aberto por quem tem o link",
               description = "Rota PUBLICA e sem conta, de proposito: e lida por quem socorre o animal, "
                             + "a partir de um QR na coleira, no meio de uma emergencia - nao instala "
                             + "app e nao cria login. O token E a credencial, entao ele nao aparece em "
                             + "nenhuma outra leitura. O que se ve e delimitado pelo escopo que o tutor "
                             + "escolheu ao criar o link, e o acesso fica registrado no log de leitura "
                             + "de dado sensivel.")
    @GetMapping("/{token}")
    public ResponseEntity<SharedVaccineCardDTO> viewSharedCard(@PathVariable String token) {
        return ResponseEntity.ok(animalShareService.viewSharedCard(token));
    }

}
