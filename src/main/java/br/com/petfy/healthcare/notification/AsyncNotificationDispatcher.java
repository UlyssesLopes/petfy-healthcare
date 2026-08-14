package br.com.petfy.healthcare.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Tira o envio do caminho da requisicao.
 *
 * Recebe uma Notification ja montada, e nao a entidade, por um motivo que nao e
 * estilo: montar a mensagem passa por associacoes lazy - o animal, o tutor - que so
 * existem dentro da transacao de quem chamou. Em outra thread elas estariam
 * fechadas, e o aviso morreria com LazyInitializationException. Entao o que sai
 * da requisicao e apenas o envio, que e a parte lenta.
 *
 * Vale so para os avisos do OrganizationActivityNotifier. O lembrete de vacina continua
 * sincrono de proposito: la a excecao precisa subir para o rollback desmarcar a
 * dose, senao ficaria registrada como avisada uma dose que ninguem recebeu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncNotificationDispatcher {

    /*
     * O tipo e `Canais`, e nao `Notifier`, para que o NOME DO EVENTO chegue ao canal in-app: ele
     * grava o aviso, e "tutor que entrou no animal" e o que permite agrupar e depurar depois. Pela
     * interface o evento morreria aqui, que e onde ele ja e conhecido.
     */
    private final Canais notifier;

    /**
     * Falha e apenas logada. Quem chamou ja seguiu adiante - nao ha mais para
     * quem propagar, e o efeito principal, o registro no historico do animal, ja
     * esta gravado.
     */
    @Async("notificationExecutor")
    public void dispatch(Notification notification, String evento) {
        try {
            notifier.send(notification, evento);
        } catch (Exception e) {
            log.error("Falha ao notificar o tutor sobre {}", evento, e);
        }
    }

}
