package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.SponsoredEventDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipOfferDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipRequestDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipResponseDTO;
import br.com.petfy.healthcare.service.SponsorshipService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quem banca o cuidado de um animal de abrigo (Tela 46).
 *
 * <b>"Publico, com conta"</b> e o que o desenho escreve no cabecalho da tela, e e literal: a oferta
 * exige estar autenticado e nao exige alcancar o animal. Nao ha rota anonima aqui — ao contrario do
 * {@code POST /found} do bloco 5, que e publico de verdade. A diferenca e o que se faz depois: quem
 * encontra um animal na rua precisa de um telefone agora, e quem apadrinha assume um compromisso que
 * so existe se houver uma conta a que ele pertenca.
 *
 * <b>E o padrinho nao ganha acesso ao animal por nenhuma destas rotas.</b> O que ele le sao eventos de
 * custo, a partir do apadrinhamento dele.
 */
@RestController
@RequiredArgsConstructor
public class SponsorshipController {

    private final SponsorshipService sponsorshipService;

    @Operation(summary = "A tela de apadrinhar um animal de abrigo",
               description = "Quem ele e, o que o abrigo gasta com ele por mes, e quantas pessoas "
                             + "ja bancam algo dele. Responde 404 quando o abrigo nao abriu o animal "
                             + "a padrinhos, e tambem quando quem responde por ele e uma pessoa: "
                             + "apadrinhar o cachorro de alguem seria pagar a conta de uma pessoa. "
                             + "NENHUM dado clinico viaja aqui.")
    @GetMapping("/animals/{animalId}/sponsorship-offer")
    public ResponseEntity<SponsorshipOfferDTO> oferta(@PathVariable UUID animalId) {
        return ResponseEntity.ok(sponsorshipService.oferta(animalId));
    }

    @Operation(summary = "Passa a bancar um custo do animal",
               description = "Registra um COMPROMISSO, e nao um pagamento: o Petfy nao movimenta "
                             + "valor em lugar nenhum. O que o padrinho recebe em troca sao os "
                             + "eventos de custo do que ele banca, assinados e datados.")
    @PostMapping("/animals/{animalId}/sponsorships")
    public ResponseEntity<SponsorshipResponseDTO> apadrinhar(
            @PathVariable UUID animalId,
            @Valid @RequestBody SponsorshipRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sponsorshipService.apadrinhar(animalId, dto));
    }

    @Operation(summary = "O que voce banca",
               description = "Inclusive o que ja acabou: quem bancou por dois anos precisa poder ver "
                             + "que bancou.")
    @GetMapping("/sponsorships/mine")
    public ResponseEntity<List<SponsorshipResponseDTO>> meus() {
        return ResponseEntity.ok(sponsorshipService.meus());
    }

    @Operation(summary = "O que voce passa a receber",
               description = "Os eventos de custo do que este apadrinhamento banca, do mais recente "
                             + "para o mais antigo, e nunca de antes de ele comecar. Nao e a linha do "
                             + "tempo do animal: nenhum historico clinico e aberto ao padrinho.")
    @GetMapping("/sponsorships/{sponsorshipId}/events")
    public ResponseEntity<List<SponsoredEventDTO>> eventos(@PathVariable UUID sponsorshipId) {
        return ResponseEntity.ok(sponsorshipService.eventos(sponsorshipId));
    }

    @Operation(summary = "Para de bancar, com trinta dias",
               description = "Nao pede justificativa, e nao encerra na hora: o padrinho continua "
                             + "cobrindo o custo ate a data, e o abrigo e avisado para se organizar.")
    @PostMapping("/sponsorships/{sponsorshipId}/end")
    public ResponseEntity<SponsorshipResponseDTO> encerrar(@PathVariable UUID sponsorshipId) {
        return ResponseEntity.ok(sponsorshipService.encerrar(sponsorshipId));
    }

    @Operation(summary = "Quem banca os animais da sua organizacao",
               description = "Traz o que esta terminando junto com o que esta ativo — e essa lista "
                             + "que mostra, com trinta dias de antecedencia, qual custo vai deixar de "
                             + "ser coberto. Exige organizacao declarada no cabecalho.")
    @GetMapping("/group/sponsorships")
    public ResponseEntity<List<SponsorshipResponseDTO>> daOrganizacao() {
        return ResponseEntity.ok(sponsorshipService.daOrganizacao());
    }

}
