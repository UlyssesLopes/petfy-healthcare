package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ActiveContextResponseDTO;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ActiveContextService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O contexto ativo como estado, e nao como escolha a cada acao (PRODUTO 9.5).
 *
 * <b>Nao ha rota para trocar, e isso e decisao e nao esquecimento.</b> A troca ja
 * acontece pelo header {@code X-Petfy-Organization}, que vale para a requisicao
 * inteira e o cliente configura uma vez no interceptor. Guardar tambem um contexto
 * ativo no servidor criaria duas fontes de verdade para a mesma pergunta - e a que
 * perde e sempre a que o registro nao usou. O que faltava era a <b>leitura</b>: sem
 * ela o cliente descobria que precisava escolher ao tomar 409 no meio de um registro.
 *
 * <b>Fora de {@code /professional/**} de proposito:</b> aquele prefixo exige
 * credencial profissional, e o monitor da creche e membro sem CRMV. Esconder dele os
 * proprios contextos seria trazer de volta a exigencia que a Fase 6 removeu.
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class ActiveContextController {

    private final ActiveContextService activeContextService;

    @GetMapping("/context")
    @Operation(summary = "Em nome de quem estou agindo, e em nome de quem eu poderia")
    public ResponseEntity<ActiveContextResponseDTO> contextoAtivo(
            @RequestHeader(value = CurrentProfessionalProvider.HEADER_ORGANIZACAO, required = false)
            String organizacaoDeclarada) {
        return ResponseEntity.ok(activeContextService.contextoAtivo(organizacaoDeclarada));
    }

}
