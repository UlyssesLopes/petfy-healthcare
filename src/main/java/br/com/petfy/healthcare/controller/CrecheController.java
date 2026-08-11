package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AttendanceRequestDTO;
import br.com.petfy.healthcare.domain.dto.AttendanceResponseDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupResponseDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentAgreementRequestDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementResponseDTO;
import br.com.petfy.healthcare.service.CrecheService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A operacao da creche: turma, exigencia, matricula e o dia.
 *
 * <b>Tudo sob {@code /professional}</b>, e nao por gosto de prefixo: e o caminho que o
 * {@code ProfessionalAccessManager} do SecurityConfig guarda, e toda operacao daqui exige
 * organizacao declarada no cabecalho. Uma monitora que trabalha em duas creches marcaria entrada na
 * turma errada, e o animal apareceria presente num lugar onde nao esta.
 */
@RestController
@RequestMapping("/professional/creche")
@RequiredArgsConstructor
public class CrecheController {

    private final CrecheService crecheService;

    @GetMapping("/class-groups")
    @Operation(summary = "As turmas da organizacao ativa, com a ocupacao contada")
    public ResponseEntity<List<ClassGroupResponseDTO>> listClassGroups() {
        return ResponseEntity.ok(crecheService.listClassGroups());
    }

    @PostMapping("/class-groups")
    @Operation(summary = "Cria uma turma")
    public ResponseEntity<ClassGroupResponseDTO> createClassGroup(
            @Valid @RequestBody ClassGroupRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crecheService.createClassGroup(request));
    }

    @GetMapping("/vaccine-requirements")
    @Operation(summary = "O que a organizacao exige da carteira de quem entra")
    public ResponseEntity<List<VaccineRequirementResponseDTO>> listRequirements() {
        return ResponseEntity.ok(crecheService.listRequirements());
    }

    @PostMapping("/vaccine-requirements")
    @Operation(summary = "Passa a exigir uma vacina do catalogo")
    public ResponseEntity<VaccineRequirementResponseDTO> addRequirement(
            @Valid @RequestBody VaccineRequirementRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crecheService.addRequirement(request));
    }

    @DeleteMapping("/vaccine-requirements/{requirementId}")
    @Operation(summary = "Deixa de exigir uma vacina")
    public ResponseEntity<Void> removeRequirement(@PathVariable UUID requirementId) {
        crecheService.removeRequirement(requirementId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Matricula, e ela pode nascer PENDENTE.
     *
     * Responde 201 nos dois casos, e nao 202 para a pendente: a matricula foi criada, e o
     * {@code status} do corpo diz o que ela e. Um 202 sugeriria que o servidor vai fazer algo depois
     * por conta propria — e o que completa a matricula e a dose ser registrada, nao um processo nosso.
     */
    @PostMapping("/class-groups/{classGroupId}/enrollments/{animalId}")
    @Operation(summary = "Matricula o animal na turma; nasce PENDENTE se a comprovacao nao fecha")
    public ResponseEntity<EnrollmentResponseDTO> enroll(@PathVariable UUID classGroupId,
                                                        @PathVariable UUID animalId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(crecheService.enroll(animalId, classGroupId));
    }

    /**
     * "Combinado com o tutor" — a caixa da Tela 41.
     *
     * <b>PUT, e nao PATCH.</b> Combinar e um ato unico: quem renegocia diz de novo o que passou a
     * valer, e o que ele nao repetir deixou de valer. Num PATCH, "apagar a mensalidade" e "nao
     * falei da mensalidade" chegariam iguais ao servidor — e o valor velho que sobrasse iria para
     * a conta do tutor sem ninguem ter dito nada.
     *
     * <b>200 e nao 201</b>: nao nasce recurso nenhum aqui. O combinado e um lado da matricula que
     * ja existe, e o corpo devolve a matricula inteira porque e ela que a tela estava mostrando.
     */
    @PutMapping("/enrollments/{enrollmentId}/agreement")
    @Operation(summary = "Grava o combinado da matricula: mensalidade, vencimento, diaria e os dias",
               description = "SUBSTITUI o combinado inteiro — campo omitido passa a ser nulo, "
                             + "porque quem renegocia diz de novo o que vale. Todos os campos sao "
                             + "opcionais: a propria caixa do desenho se chama 'Combinado com o "
                             + "tutor · opcional'. Nada aqui cobra nem lanca valor: o Petfy guarda "
                             + "o que foi combinado. Os DIAS decidem, sozinhos, se o check-in de "
                             + "um dia fora da combinacao lanca a diaria avulsa; sem dias "
                             + "declarados a diaria nunca entra, porque o silencio da creche nao "
                             + "pode virar cobranca ao tutor.")
    public ResponseEntity<EnrollmentResponseDTO> setAgreement(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody EnrollmentAgreementRequestDTO request) {
        return ResponseEntity.ok(crecheService.setAgreement(enrollmentId, request));
    }

    @GetMapping("/class-groups/{classGroupId}/enrollments")
    @Operation(summary = "As matriculas da turma, com a comprovacao reavaliada agora")
    public ResponseEntity<List<EnrollmentResponseDTO>> listEnrollments(@PathVariable UUID classGroupId) {
        return ResponseEntity.ok(crecheService.listEnrollments(classGroupId));
    }

    /**
     * O dia da turma.
     *
     * A data e parametro e nao caminho porque o caso comum e HOJE, e omitir e o gesto de 7h30. Passar
     * a data serve para conferir o dia anterior sem trocar de tela.
     */
    @GetMapping("/class-groups/{classGroupId}/day")
    @Operation(summary = "O dia da turma: esperados, presentes, saidas e faltas")
    public ResponseEntity<List<AttendanceResponseDTO>> listDay(
            @PathVariable UUID classGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        return ResponseEntity.ok(crecheService.listDay(classGroupId, day));
    }

    @PostMapping("/enrollments/{enrollmentId}/check-in")
    @Operation(summary = "Marca a entrada de hoje")
    public ResponseEntity<AttendanceResponseDTO> checkIn(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody(required = false) AttendanceRequestDTO request) {
        return ResponseEntity.ok(crecheService.checkIn(enrollmentId, request));
    }

    @PostMapping("/enrollments/{enrollmentId}/check-out")
    @Operation(summary = "Marca a saida de hoje")
    public ResponseEntity<AttendanceResponseDTO> checkOut(@PathVariable UUID enrollmentId) {
        return ResponseEntity.ok(crecheService.checkOut(enrollmentId));
    }

    @PostMapping("/enrollments/{enrollmentId}/absence")
    @Operation(summary = "Marca falta de hoje")
    public ResponseEntity<AttendanceResponseDTO> markAbsence(@PathVariable UUID enrollmentId) {
        return ResponseEntity.ok(crecheService.markAbsence(enrollmentId));
    }
}
