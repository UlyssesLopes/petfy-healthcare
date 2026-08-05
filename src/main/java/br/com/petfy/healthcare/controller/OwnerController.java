package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.OwnerExportDTO;
import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.service.OwnerExportService;
import br.com.petfy.healthcare.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;
    private final OwnerExportService ownerExportService;

    /** Unica rota publica de owner: sem ela nao existe o primeiro usuario. */
    @PostMapping
    public ResponseEntity<OwnerResponseDTO> createOwner(@Valid @RequestBody OwnerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createOwner(request));
    }

    @GetMapping("/me")
    public ResponseEntity<OwnerResponseDTO> getCurrentOwner() {
        return ResponseEntity.ok(ownerService.getCurrentOwner());
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados. Campos ausentes ou
     * nulos no payload sao ignorados e o valor existente e preservado. Por isso
     * @Valid nao e aplicado aqui - nenhum campo e obrigatorio numa atualizacao
     * parcial.
     */
    @Operation(summary = "Atualiza dados do tutor autenticado",
               description = "Atualizacao parcial: apenas os campos presentes no payload sao alterados. " +
                             "Campos ausentes ou nulos preservam o valor existente. " +
                             "Nenhum campo e obrigatorio neste endpoint.")
    @PutMapping("/me")
    public ResponseEntity<OwnerResponseDTO> updateCurrentOwner(@RequestBody OwnerRequestDTO request) {
        return ResponseEntity.ok(ownerService.updateCurrentOwner(request));
    }

    /**
     * Separado do PUT /me porque aqui a validacao vale sempre: o PUT e parcial e
     * preserva campo ausente, o que nao serve para uma troca de senha.
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequestDTO request) {
        ownerService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Portabilidade, irma da exclusao logo abaixo.
     *
     * Fica no {@code /owners/me} e nao numa rota propria porque e um dado do titular como
     * qualquer outro - a diferenca e a completude, nao a natureza.
     */
    @Operation(summary = "Exporta todos os dados do tutor autenticado",
               description = "Documento JSON com o tutor, os consentimentos e todos os pets em que ele "
                             + "e tutor - vacinas, antiparasitarios, pesagens, atendimentos, correcoes, "
                             + "anexos, links de compartilhamento, acessos de clinica e o log de acessos "
                             + "de terceiros. Dado pessoal de terceiro vem reduzido, e o proprio "
                             + "documento declara o que nao carrega no campo limitacoes.")
    @GetMapping("/me/export")
    public ResponseEntity<OwnerExportDTO> exportCurrentOwner() {
        return ResponseEntity.ok(ownerExportService.exportarDoAutenticado());
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentOwner() {
        ownerService.deleteCurrentOwner();
        return ResponseEntity.noContent().build();
    }

}
