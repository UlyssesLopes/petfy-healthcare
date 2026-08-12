package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ServiceAppointmentCloseRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentResponseDTO;
import br.com.petfy.healthcare.service.ServiceAppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A agenda de banho e tosa (Tela 18).
 *
 * <b>Debaixo de `/group`, e nao de `/animals`</b>, porque a pergunta desta tela e da ORGANIZACAO: "o
 * que eu tenho hoje". Todas as rotas exigem a organizacao declarada no cabecalho — quem trabalha em
 * dois petshops tem de dizer em qual esta, senao o banho aparece na agenda errada.
 *
 * <b>Fora de `/professional/**` de proposito:</b> aquele espaco exige credencial profissional
 * conferida no banco, e um tosador nao tem CRMV. E esse e exatamente o ponto da tela — "nenhuma
 * autoridade clinica".
 */
@RestController
@RequestMapping("/group/appointments")
@RequiredArgsConstructor
public class ServiceAppointmentController {

    private final ServiceAppointmentService serviceAppointmentService;

    @Operation(summary = "A agenda do dia",
               description = "Os compromissos da organizacao declarada, do primeiro ao ultimo "
                             + "horario. Cada linha traz 'o que voce precisa saber antes de encostar "
                             + "nele' — as linhas de seguranca que o ESCOPO da concessao permite, e "
                             + "nada de prontuario. Quando a concessao venceu, a lista vem vazia e "
                             + "`inTheDark` diz por que: um cartao sem as linhas parece um animal sem "
                             + "restricao nenhuma.")
    @GetMapping
    public ResponseEntity<List<ServiceAppointmentResponseDTO>> doDia(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        return ResponseEntity.ok(serviceAppointmentService.doDia(day));
    }

    @Operation(summary = "Agenda um banho",
               description = "Exige que a organizacao ALCANCE o animal: sem isso, qualquer uma poria "
                             + "qualquer animal na propria agenda — e a agenda e onde aparece o que o "
                             + "tutor compartilhou.")
    @PostMapping
    public ResponseEntity<ServiceAppointmentResponseDTO> agendar(
            @Valid @RequestBody ServiceAppointmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceAppointmentService.agendar(dto));
    }

    @Operation(summary = "Marca a entrada do animal")
    @PostMapping("/{appointmentId}/check-in")
    public ResponseEntity<ServiceAppointmentResponseDTO> marcarEntrada(
            @PathVariable UUID appointmentId) {
        return ResponseEntity.ok(serviceAppointmentService.marcarEntrada(appointmentId));
    }

    @Operation(summary = "Entrega o animal e avisa quem cuida dele",
               description = "O texto, quando houver, entra na linha do tempo como OBSERVACAO "
                             + "assinada — e nunca como ato clinico: 'descreva o que viu, nao o que "
                             + "acha que e'. Sem texto, nada entra: um banho sem novidade nao e fato "
                             + "de saude, e enche-la de 'deu banho' enterraria o que importa.")
    @PostMapping("/{appointmentId}/deliver")
    public ResponseEntity<ServiceAppointmentResponseDTO> entregar(
            @PathVariable UUID appointmentId,
            @RequestBody(required = false) ServiceAppointmentCloseRequestDTO dto) {
        return ResponseEntity.ok(serviceAppointmentService.entregar(appointmentId, dto));
    }

    @Operation(summary = "O animal nao veio",
               description = "A ausencia e informacao, como a falta na creche: um compromisso que "
                             + "some da agenda sem desfecho faz o petshop perder a conta de quem "
                             + "desmarcou e de quem simplesmente nao apareceu.")
    @PostMapping("/{appointmentId}/no-show")
    public ResponseEntity<ServiceAppointmentResponseDTO> faltou(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(serviceAppointmentService.faltou(appointmentId));
    }

}
