package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ConsentStatusResponseDTO;
import br.com.petfy.healthcare.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O que o titular aceitou, e o aceite da versao vigente.
 *
 * Nao ha rota para <b>revogar</b> consentimento, e a ausencia e deliberada: sem
 * consentimento nao ha base legal para tratar dado de saude, entao revogar e sair -
 * o que ja existe em {@code DELETE /owners/me}. Uma rota de revogacao que deixasse a
 * conta de pe criaria um estado em que a aplicacao guarda dado sem poder trata-lo.
 */
@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @Operation(summary = "Consentimentos do tutor autenticado",
               description = "Lista os aceites registrados e os documentos vigentes que ainda faltam "
                             + "aceitar. Fica pendente quando a politica muda de versao e quando a conta "
                             + "e anterior ao registro de consentimento - nenhuma migration pode inventar "
                             + "consentimento que nunca foi dado.")
    @GetMapping("/me")
    public ResponseEntity<ConsentStatusResponseDTO> statusDoAutenticado() {
        return ResponseEntity.ok(consentService.statusDoAutenticado());
    }

    @Operation(summary = "Aceita os documentos vigentes",
               description = "Idempotente: aceitar de novo a mesma versao nao cria outro registro, e o "
                             + "primeiro aceite continua sendo o que vale.")
    @PostMapping("/accept")
    public ResponseEntity<ConsentStatusResponseDTO> aceitarVigentes() {
        return ResponseEntity.ok(consentService.aceitarVigentes());
    }

}
