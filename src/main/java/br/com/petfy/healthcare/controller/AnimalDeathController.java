package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalDeathRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClosedLifeResponseDTO;
import br.com.petfy.healthcare.service.AnimalDeathService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * O fim da linha do tempo (Tela 33).
 *
 * <b>Controller proprio, e nao mais duas rotas no {@code AnimalController}</b>, pela licao que ja
 * esta escrita la: pendurar dependencia nova naquele controller faz todo slice que o monta
 * carregar o notificador e a creche junto, e foi assim que o {@code CorsConfigTest} quebrou uma
 * vez.
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalDeathController {

    private final AnimalDeathService animalDeathService;

    @Operation(summary = "Encerra a linha do tempo de um animal que morreu",
               description = "So quem responde pelo animal. A veterinaria que o atendeu na ultima "
                             + "noite registra o obito como ato clinico dela, o que e outro "
                             + "registro: ninguem deve descobrir que perdeu o animal por uma "
                             + "notificacao do sistema. Encerra a custodia sem sucessor, encerra "
                             + "as matriculas vivas, poe o fim na linha do tempo e avisa quem "
                             + "cuidava dele — pessoas e organizacoes. NAO apaga nada: para "
                             + "destruir o registro existe o DELETE, e ele e outra coisa.")
    @PostMapping("/{animalId}/death")
    public ResponseEntity<ClosedLifeResponseDTO> registrar(
            @PathVariable UUID animalId,
            @Valid @RequestBody AnimalDeathRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(animalDeathService.registrar(animalId, dto));
    }

    @Operation(summary = "A ficha fechada do animal",
               description = "Os numeros do cartao de depois: eventos registrados, quantas pessoas "
                             + "e quantas organizacoes cuidaram dele, e o periodo em que quem le "
                             + "respondeu por ele. 404 no animal que nao foi encerrado.")
    @GetMapping("/{animalId}/death")
    public ResponseEntity<ClosedLifeResponseDTO> daFichaFechada(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalDeathService.daFichaFechada(animalId));
    }

}
