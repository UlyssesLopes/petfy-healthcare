package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PersonExportDTO;
import br.com.petfy.healthcare.domain.dto.PersonRequestDTO;
import br.com.petfy.healthcare.domain.dto.PersonResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.service.PersonExportService;
import br.com.petfy.healthcare.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final PersonExportService personExportService;

    /** Unica rota publica de person: sem ela nao existe o primeiro usuario. */
    @Operation(summary = "Cria a conta", description = "Rota publica. Nao existe conta de tutor nem conta de veterinario: e uma pessoa so, e o que ela pode fazer vem do que ela TEM - custodia de um animal, vinculo com uma organizacao, credencial profissional -, nunca de um tipo escolhido no cadastro.")
    @PostMapping
    public ResponseEntity<PersonResponseDTO> createPerson(@Valid @RequestBody PersonRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.createPerson(request));
    }

    @Operation(summary = "Meus dados")
    @GetMapping("/me")
    public ResponseEntity<PersonResponseDTO> getCurrentPerson() {
        return ResponseEntity.ok(personService.getCurrentPerson());
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
    public ResponseEntity<PersonResponseDTO> updateCurrentPerson(@RequestBody PersonRequestDTO request) {
        return ResponseEntity.ok(personService.updateCurrentPerson(request));
    }

    /**
     * Separado do PUT /me porque aqui a validacao vale sempre: o PUT e parcial e
     * preserva campo ausente, o que nao serve para uma troca de senha.
     */
    @Operation(summary = "Troca a minha senha", description = "Exige a senha atual, e derruba as sessoes abertas: quem trocou porque desconfiou de acesso indevido nao teria ganhado nada se o token do invasor continuasse valendo. O cliente recebe 401 em token que ainda nao expirou.")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequestDTO request) {
        personService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Portabilidade, irma da exclusao logo abaixo.
     *
     * Fica no {@code /persons/me} e nao numa rota propria porque e um dado do titular como
     * qualquer outro - a diferenca e a completude, nao a natureza.
     */
    @Operation(summary = "Exporta todos os dados do tutor autenticado",
               description = "Documento JSON com o tutor, os consentimentos e todos os animals em que ele "
                             + "e tutor - vacinas, antiparasitarios, pesagens, atendimentos, correcoes, "
                             + "anexos, links de compartilhamento, acessos de clinica e o log de acessos "
                             + "de terceiros. Dado pessoal de terceiro vem reduzido, e o proprio "
                             + "documento declara o que nao carrega no campo limitacoes.")
    @GetMapping("/me/export")
    public ResponseEntity<PersonExportDTO> exportCurrentPerson() {
        return ResponseEntity.ok(personExportService.exportarDoAutenticado());
    }

    @Operation(summary = "Apaga a minha conta", description = "Irreversivel. Animal com outro tutor sobrevive e a titularidade passa a ele; animal que so eu tinha morre com a carteira inteira. O registro de consentimento sai junto - guardar prova de aceite de quem pediu para ser esquecido inverteria o proposito da prova.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentPerson() {
        personService.deleteCurrentPerson();
        return ResponseEntity.noContent().build();
    }

}
