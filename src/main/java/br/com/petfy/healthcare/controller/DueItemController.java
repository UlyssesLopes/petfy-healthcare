package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;
import br.com.petfy.healthcare.domain.entity.DueItemKind;
import br.com.petfy.healthcare.service.DueItemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tudo que cobra acao de quem esta autenticado, num lugar so.
 *
 * <b>Nao esta sob {@code /animals}</b>, ao contrario do resto do prontuario: a pergunta
 * que ela responde nao e sobre um animal, e sobre a pessoa - "o que eu preciso fazer?".
 * O consentimento pendente, que nem tem animal, e a prova de que a ancora aqui e outra.
 */
@RestController
@RequestMapping("/due-items")
@RequiredArgsConstructor
public class DueItemController {

    private final DueItemService dueItemService;

    @Operation(summary = "O que cobra acao de quem esta autenticado",
               description = "Dose de vacina, antiparasitario, orientacao a cumprir, convite "
                             + "aguardando resposta e consentimento pendente, do mais atrasado ao "
                             + "menos urgente. Derivada de cada fonte a cada chamada - nao existe "
                             + "tabela de pendencia, porque a primeira a divergir cobraria algo "
                             + "que ja foi feito. windowDays define quanto do futuro entra; o que "
                             + "ja venceu entra sempre.")
    @GetMapping
    public ResponseEntity<List<DueItemResponseDTO>> listar(
            @RequestParam(defaultValue = "30") int windowDays,
            @RequestParam(defaultValue = "false") boolean includeSilenced) {
        return ResponseEntity.ok(dueItemService.doAutenticado(windowDays, includeSilenced));
    }

    /**
     * <b>PUT e nao POST, porque silenciar e idempotente:</b> silenciar duas vezes e o mesmo
     * que silenciar uma, e o cliente que reenvia a requisicao nao merece um erro. Mesma
     * postura da revogacao de acesso.
     *
     * A rota fica <b>sob a pendencia</b>, e nao numa colecao de preferencias: o 5.3 do DESIGN
     * diz que "silenciar mora na pendencia, nao em preferencias - funcionalidade escondida em
     * configuracao nao e oferecida, e escondida".
     */
    @Operation(summary = "Para de cobrar esta pendencia de mim",
               description = "Silencia a COBRANCA, e nao o registro: a proxima dose continua "
                             + "sendo calculada, a orientacao continua vigente e a linha do tempo "
                             + "continua recebendo tudo. Vale so para quem chamou - dois tutores "
                             + "dividem o cuidado e dividem a cobranca, e um silenciar nao cala o "
                             + "outro. Consentimento pendente nao pode ser silenciado, porque "
                             + "bloqueia o resto do produto. Idempotente.")
    @PutMapping("/{kind}/{sourceId}/silence")
    public ResponseEntity<Void> silenciar(@PathVariable DueItemKind kind, @PathVariable UUID sourceId) {
        dueItemService.silenciar(kind, sourceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Volta a cobrar esta pendencia de mim",
               description = "Idempotente, e nao exige que a pendencia ainda exista: se o registro "
                             + "de origem saiu, o silencio dele e lixo e apagar e certo de qualquer "
                             + "forma.")
    @DeleteMapping("/{kind}/{sourceId}/silence")
    public ResponseEntity<Void> voltarACobrar(@PathVariable DueItemKind kind, @PathVariable UUID sourceId) {
        dueItemService.voltarACobrar(kind, sourceId);
        return ResponseEntity.noContent().build();
    }

}
