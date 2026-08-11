package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.dto.MembershipRoleRequestDTO;
import br.com.petfy.healthcare.service.OrganizationMemberService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A equipe da organizacao.
 *
 * <b>Controller proprio, e nao mais um metodo no OrganizationController.</b> A licao e do
 * `CorsConfigTest`, que quebrou sete casos quando a rota de matricula ficou pendurada no
 * `AnimalController`: pendurar assunto novo num controller existente faz todo mundo que monta
 * aquele controller carregar o assunto junto.
 *
 * As rotas nao levam `organizationId` no caminho porque a organizacao NAO vem da URL - ela vem
 * do contexto declarado, que e o unico lugar onde ja se sabe que a pessoa e membro. Aceita-la
 * pela URL abriria a porta de pedir a equipe de uma organizacao qualquer.
 *
 * <b>Os metodos tem nome proprio (`listMembers`, e nao `list`) por causa do contrato.</b> O
 * springdoc deriva o `operationId` do nome do metodo e desempata colisao com sufixo numerico -
 * chamar de `list` fez ele RENOMEAR a rota de outro controller (`list_1` virou `list_2`), e o
 * sufixo depende da ordem de varredura. Nome unico mantem o contrato estavel.
 */
@RestController
@RequestMapping("/organizations/members")
@RequiredArgsConstructor
public class OrganizationMemberController {

    private final OrganizationMemberService organizationMemberService;

    @Operation(summary = "A equipe da organizacao",
               description = "Quem esta na equipe agora, com funcao, desde quando e o registro "
                             + "profissional de quem tem. Qualquer membro ve - esconder a lista da "
                             + "propria equipe nao protege ninguem.")
    @GetMapping
    public ResponseEntity<List<MembershipResponseDTO>> listMembers() {
        return ResponseEntity.ok(organizationMemberService.listMembers());
    }

    @Operation(summary = "Ajusta a funcao de quem e da equipe",
               description = "So administrador. Rebaixar o ultimo administrador e recusado: sem ele "
                             + "ninguem convida, ajusta nem desliga.")
    @PatchMapping("/{membershipId}")
    public ResponseEntity<MembershipResponseDTO> changeMemberRole(
            @PathVariable UUID membershipId,
            @Valid @RequestBody MembershipRoleRequestDTO request) {
        return ResponseEntity.ok(organizationMemberService.changeMemberRole(membershipId, request.getRole()));
    }

    @Operation(summary = "Desliga a pessoa da equipe",
               description = "Marca a saida e nao apaga o vinculo - o que ela registrou continua no "
                             + "historico dos animais. Idempotente.")
    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID membershipId) {
        organizationMemberService.removeMember(membershipId);
        return ResponseEntity.noContent().build();
    }

}
