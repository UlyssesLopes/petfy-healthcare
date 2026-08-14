package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PersonNotificationResponseDTO;
import br.com.petfy.healthcare.service.PersonNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Os avisos de quem esta lendo.
 *
 * <b>Debaixo de {@code /persons/me} de proposito:</b> aviso e dado da PESSOA, e nao do animal. O
 * mesmo fato — "Ana entrou no Code" — vira um aviso para cada tutor, e cada um leu o que leu.
 *
 * <b>Nao ha rota para os avisos de outra pessoa</b>, nem com id na URL: o servico parte sempre do
 * autenticado, e o id de um aviso alheio responde 404.
 */
@RestController
@RequestMapping("/persons/me/notifications")
@RequiredArgsConstructor
public class PersonNotificationController {

    private final PersonNotificationService personNotificationService;
    private final br.com.petfy.healthcare.notification.AvisoStream avisoStream;
    private final br.com.petfy.healthcare.security.CurrentPersonProvider currentPersonProvider;

    @Operation(summary = "Os seus avisos, do mais novo para o mais velho",
               description = "O texto vem pronto do banco, e nao remontado a partir do estado atual: "
                             + "o aviso e um FATO, e continua sendo o que a pessoa leu mesmo depois "
                             + "de o fato deixar de valer. E o mesmo texto que foi por e-mail.")
    @GetMapping
    public ResponseEntity<Page<PersonNotificationResponseDTO>> listMyNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(personNotificationService.listMine(pageable));
    }

    @Operation(summary = "Quantos avisos ainda nao foram lidos",
               description = "Rota propria porque a marca no sino e perguntada de qualquer tela: "
                             + "carregar uma pagina de avisos so para contar seria pagar a leitura "
                             + "inteira para mostrar um numero.")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> countMineUnread() {
        return ResponseEntity.ok(personNotificationService.countMineUnread());
    }

    @Operation(summary = "Marca um aviso como lido",
               description = "Ler duas vezes nao reescreve a data: 'quando ela viu' e a PRIMEIRA vez "
                             + "que viu. Aviso de outra pessoa responde 404.")
    @PostMapping("/{personNotificationId}/read")
    public ResponseEntity<PersonNotificationResponseDTO> markAsRead(
            @PathVariable UUID personNotificationId) {
        return ResponseEntity.ok(personNotificationService.markAsRead(personNotificationId));
    }

    @Operation(summary = "O canal que empurra o aviso na hora",
               description = "SSE. O cliente abre com `fetch` e o token no header — nao com "
                             + "`EventSource`, que nao manda header e obrigaria a mandar credencial "
                             + "na URL. O evento nao carrega dado: ele diz que ha algo novo, e a "
                             + "tela recarrega a contagem.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return avisoStream.abrir(currentPersonProvider.require().getPersonId());
    }

    @Operation(summary = "Marca todos como lidos",
               description = "Uma escrita so, e nao uma por aviso. Devolve quantos deixaram de estar "
                             + "por ler.")
    @PostMapping("/read")
    public ResponseEntity<Integer> markAllAsRead() {
        return ResponseEntity.ok(personNotificationService.markAllAsRead());
    }

}
