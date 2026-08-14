package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PersonSessionResponseDTO;
import br.com.petfy.healthcare.service.PersonSessionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Os aparelhos conectados (Tela 36) — a unica linha de tela que tinha ficado por construir.
 *
 * <b>A tela dizia "o Petfy nao guarda a lista de aparelhos conectados".</b> A justificativa era o
 * JWT sem estado, e ela descrevia um custo ja pago: o filtro consulta o banco em TODA rota
 * autenticada desde o P1, pelo `passwordChangedAt`. O que faltava nao era arquitetura — era uma
 * linha por sessao.
 */
@RestController
@RequestMapping("/persons/me/sessions")
@RequiredArgsConstructor
public class PersonSessionController {

    private final PersonSessionService personSessionService;

    @Operation(summary = "Os aparelhos conectados a sua conta",
               description = "Das mais novas para as mais velhas, incluindo as ja encerradas. NAO HA "
                             + "LOCALIZACAO: o produto nao guarda de onde alguem entra, e um IP nao "
                             + "ajudaria ninguem a reconhecer o proprio aparelho. O que identifica e "
                             + "o navegador e quando comecou.")
    @GetMapping
    public ResponseEntity<List<PersonSessionResponseDTO>> listMine() {
        return ResponseEntity.ok(personSessionService.listMine());
    }

    @Operation(summary = "Encerra um aparelho",
               description = "O token daquela entrada para de valer na proxima requisicao. ENCERRAR "
                             + "NAO APAGA: a linha fica com a data, porque quem encerra por suspeita "
                             + "de acesso indevido quer que o registro permaneca. Encerrar a sessao "
                             + "atual e sair, e e permitido — a tela avisa antes, o servidor nao "
                             + "impede.")
    @DeleteMapping("/{personSessionId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID personSessionId) {
        personSessionService.revoke(personSessionId);
        return ResponseEntity.noContent().build();
    }

}
