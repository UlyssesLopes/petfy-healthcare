package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ReferralCandidateDTO;
import br.com.petfy.healthcare.domain.dto.ReferralOptionsDTO;
import br.com.petfy.healthcare.domain.dto.ReferralRequestDTO;
import br.com.petfy.healthcare.domain.dto.ReferralResponseDTO;
import br.com.petfy.healthcare.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * O caso que passa adiante (Tela 45).
 *
 * <b>As rotas de pedir vivem debaixo do animal; as de decidir, nao.</b> Nao e organizacao de codigo:
 * encaminhar e um ato SOBRE um animal, feito por quem o atende, e a guarda do animal e o que autoriza.
 * Decidir e responder a uma caixa de entrada — o tutor chega pelo e-mail sem saber de que animal se
 * trata, e uma rota que exigisse o id do animal na URL o obrigaria a descobri-lo antes de poder ler o
 * pedido.
 *
 * <b>Fora de {@code /professional/**} de proposito</b>, como o {@link GroupApprovalController}: quem
 * DECIDE e o tutor, e ele nao tem CRMV. Exigir credencial no espaco inteiro trancaria a metade da
 * tela que o desenho chama de "o Marcelo recebe e decide". Que quem ENCAMINHA seja profissional e
 * consequencia da guarda de escrita, e nao de um prefixo de rota.
 */
@RestController
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @Operation(summary = "O que pode ir junto num encaminhamento",
               description = "As caixas do 'o que vai junto', cada uma com quantos eventos daquele "
                             + "escopo existem no animal e desde quando. As contagens saem da linha "
                             + "do tempo, com o mesmo mapeamento de tipo para escopo que a guarda "
                             + "aplica ao mascarar — o numero prometido e o que o especialista abre.")
    @GetMapping("/animals/{animalId}/referral-options")
    public ResponseEntity<ReferralOptionsDTO> opcoes(@PathVariable UUID animalId) {
        return ResponseEntity.ok(referralService.opcoes(animalId));
    }

    @Operation(summary = "Buscar outro profissional",
               description = "Busca parcial por nome ou especialidade, entre quem tem credencial "
                             + "ativa. Menos de tres letras devolve lista vazia. Vive debaixo do "
                             + "animal porque assim responde 'ele ja registrou algo neste animal' e "
                             + "porque a guarda de escrita do animal e o que impede a varredura de "
                             + "dado pessoal.")
    @GetMapping("/animals/{animalId}/referral-candidates")
    public ResponseEntity<List<ReferralCandidateDTO>> candidatos(
            @PathVariable UUID animalId,
            @RequestParam(name = "busca", required = false) String busca) {
        return ResponseEntity.ok(referralService.candidatos(animalId, busca));
    }

    @Operation(summary = "Encaminha o caso a um especialista",
               description = "Nao concede nada: cria um pedido que quem responde pelo animal decide. "
                             + "Se quem encaminha JA responde pelo animal, nasce autorizado e a "
                             + "concessao sai na hora — pedir autorizacao a si mesmo seria teatro.")
    @PostMapping("/animals/{animalId}/referrals")
    public ResponseEntity<ReferralResponseDTO> encaminhar(
            @PathVariable UUID animalId,
            @Valid @RequestBody ReferralRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referralService.encaminhar(animalId, dto));
    }

    @Operation(summary = "Os encaminhamentos deste animal",
               description = "Para a clinica acompanhar o que foi autorizado e o que nao foi.")
    @GetMapping("/animals/{animalId}/referrals")
    public ResponseEntity<List<ReferralResponseDTO>> doAnimal(@PathVariable UUID animalId) {
        return ResponseEntity.ok(referralService.doAnimal(animalId));
    }

    @Operation(summary = "O que espera minha decisao",
               description = "Os encaminhamentos pendentes dos animais por que quem esta lendo "
                             + "responde. A pergunta parte da custodia, e nao de um destinatario "
                             + "gravado: a custodia passa de mao em mao, e um pedido enderecado a "
                             + "quem respondia ontem ficaria parado para sempre.")
    @GetMapping("/referrals/pending")
    public ResponseEntity<List<ReferralResponseDTO>> pendentes() {
        return ResponseEntity.ok(referralService.pendentesParaDecisao());
    }

    @Operation(summary = "O que encaminharam para mim",
               description = "A caixa do especialista. O pendente aparece SEM o animal: ele precisa "
                             + "saber que ha um caso esperando autorizacao, e o tutor ainda nao "
                             + "autorizou nada.")
    @GetMapping("/referrals/received")
    public ResponseEntity<List<ReferralResponseDTO>> recebidos() {
        return ResponseEntity.ok(referralService.recebidos());
    }

    @Operation(summary = "Autoriza o encaminhamento",
               description = "Autorizar e conceder na mesma transacao: nasce um Grant de 90 dias com "
                             + "granted_by sendo quem autorizou, revogavel por ele como qualquer "
                             + "outro. Exige responder pelo animal — nenhum nivel de concessao chega "
                             + "aqui, porque conceder acesso continua sendo de quem responde.")
    @PostMapping("/referrals/{referralId}/authorize")
    public ResponseEntity<ReferralResponseDTO> autorizar(@PathVariable UUID referralId) {
        return ResponseEntity.ok(referralService.autorizar(referralId));
    }

    @Operation(summary = "Recusa o encaminhamento",
               description = "Nao apaga, e nao pede justificativa: autorizar acesso ao proprio "
                             + "prontuario e decisao de quem responde pelo animal, e nao ha a quem "
                             + "justificar. Quem encaminhou e avisado; o especialista, que nunca "
                             + "soube do pedido, nao.")
    @PostMapping("/referrals/{referralId}/reject")
    public ResponseEntity<ReferralResponseDTO> recusar(@PathVariable UUID referralId) {
        return ResponseEntity.ok(referralService.recusar(referralId));
    }

}
