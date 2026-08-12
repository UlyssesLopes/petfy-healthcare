package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.GroupApprovalRequestDTO;
import br.com.petfy.healthcare.domain.dto.GroupApprovalResponseDTO;
import br.com.petfy.healthcare.service.GroupApprovalService;
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

import java.util.List;
import java.util.UUID;

/**
 * O acordo de duas pessoas (Telas 43 e 44).
 *
 * <b>Fora de {@code /professional/**} de propósito.</b> Aquele espaço exige credencial
 * profissional conferida no banco, e quem cuida de uma colônia não tem CRMV — é a vizinha que
 * alimenta de manhã. Quem decide aqui é membro do grupo, e a checagem é do serviço, pela mesma
 * razão que tirou o convite de organização daquele espaço.
 */
@RestController
@RequestMapping("/group-approvals")
@RequiredArgsConstructor
public class GroupApprovalController {

    private final GroupApprovalService groupApprovalService;

    @Operation(summary = "Pede que outra pessoa do grupo concorde",
               description = "Os tres atos que, num grupo sem dono, nao podem ser de uma pessoa "
                             + "so: dar um animal para adocao, encerrar a linha do tempo dele e "
                             + "tirar alguem do grupo. Exige organizacao declarada no cabecalho, "
                             + "e que quem pede seja membro ativo dela.")
    @PostMapping
    public ResponseEntity<GroupApprovalResponseDTO> pedir(
            @Valid @RequestBody GroupApprovalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupApprovalService.pedir(dto));
    }

    @Operation(summary = "O que espera decisao no grupo",
               description = "Traz tambem o que a propria pessoa pediu, com canDecide=false: "
                             + "esconder o proprio pedido pareceria mais limpo e seria pior — "
                             + "quem pediu precisa ver que ele continua parado.")
    @GetMapping
    public ResponseEntity<List<GroupApprovalResponseDTO>> pendentes() {
        return ResponseEntity.ok(groupApprovalService.pendentes());
    }

    @Operation(summary = "Concorda, e o ato acontece",
               description = "Concordar e executar sao a mesma transacao: guardar 'concordado, "
                             + "falta executar' criaria um estado em que o grupo acha que deu e o "
                             + "animal nao mudou de mao. Quem pediu NAO pode concordar — 403 com "
                             + "codigo proprio, e e a regra inteira do animal sem dono.")
    @PostMapping("/{groupApprovalId}/agree")
    public ResponseEntity<GroupApprovalResponseDTO> concordar(@PathVariable UUID groupApprovalId) {
        return ResponseEntity.ok(groupApprovalService.concordar(groupApprovalId));
    }

    @Operation(summary = "Recusa o pedido",
               description = "Nao apaga: quem pediu precisa ver que foi recusado, e nao que o "
                             + "pedido sumiu.")
    @PostMapping("/{groupApprovalId}/reject")
    public ResponseEntity<GroupApprovalResponseDTO> recusar(@PathVariable UUID groupApprovalId) {
        return ResponseEntity.ok(groupApprovalService.recusar(groupApprovalId));
    }

}
