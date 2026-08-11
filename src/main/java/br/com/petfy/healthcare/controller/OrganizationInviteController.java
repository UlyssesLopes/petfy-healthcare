package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteAcceptRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInvitePreviewResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteResponseDTO;
import br.com.petfy.healthcare.service.OrganizationInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Os convites de membro: emitir, listar, revogar — e, do outro lado, ler e aceitar.
 *
 * <b>Nao esta sob a cadeia que exige credencial profissional</b>, e isso e deliberado: convidar e
 * ato da organizacao, e a administradora do abrigo nao tem CRMV. Quem confere e o servico, pelo
 * VINCULO.
 *
 * <b>Emitir e aceitar sao lados opostos e pedem coisas opostas.</b> Emitir exige estar agindo em
 * nome da organizacao; aceitar exige justamente NAO ser dela ainda — quem aceita e uma pessoa
 * autenticada e mais nada, e o que autoriza e o token que ela tem na mao.
 */
@RestController
@RequestMapping("/organizations/invites")
@RequiredArgsConstructor
public class OrganizationInviteController {

    private final OrganizationInviteService organizationInviteService;

    @Operation(summary = "Convida alguem para ser membro da organizacao",
               description = "Membro age em nome dela - e o vinculo, e nao um papel declarado no "
                             + "cadastro, que da acesso a area de organizacao.")
    @PostMapping
    public ResponseEntity<OrganizationInviteResponseDTO> create(
            @Valid @RequestBody(required = false) OrganizationInviteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationInviteService.create(request));
    }

    @Operation(summary = "Os convites de membro em aberto")
    @GetMapping
    public ResponseEntity<List<OrganizationInviteResponseDTO>> listFromMyOrganization() {
        return ResponseEntity.ok(organizationInviteService.listFromMyOrganization());
    }

    @Operation(summary = "Revoga o convite de membro", description = "Idempotente.")
    @DeleteMapping("/{organizationInviteId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID organizationInviteId) {
        organizationInviteService.revoke(organizationInviteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Le o convite que esta na minha mao, sem aceitar",
               description = "De qual organizacao, com que funcao e ate quando vale. Nao consome "
                             + "o convite: abrir o link para ler nao pode gastar o direito de entrar.")
    @GetMapping("/preview")
    public ResponseEntity<OrganizationInvitePreviewResponseDTO> previewInvite(@RequestParam String token) {
        return ResponseEntity.ok(organizationInviteService.preview(token));
    }

    @Operation(summary = "Aceita o convite com a conta que ja tenho",
               description = "Cria o vinculo com a organizacao e consome o convite. Ate aqui o "
                             + "unico aceite possivel era o inviteToken na criacao da conta, o que "
                             + "obrigava quem ja usa o Petfy a criar uma segunda conta.")
    @PostMapping("/accept")
    public ResponseEntity<MembershipResponseDTO> acceptInvite(
            @Valid @RequestBody OrganizationInviteAcceptRequestDTO request) {
        return ResponseEntity.ok(organizationInviteService.accept(request.getToken()));
    }

}
