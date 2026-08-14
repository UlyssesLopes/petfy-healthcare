package br.com.petfy.healthcare.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Um aviso, dois canais.
 *
 * <b>Ele existe para que NENHUM notificador do dominio precise saber que o in-app existe.</b> O
 * {@link Notifier} sempre foi "o canal", e quem escreve aviso escreve uma {@link Notification} e
 * manda — o {@code PetTutorActivityNotifier}, o {@code OrganizationActivityNotifier}, o lembrete de
 * vacina, a confirmacao de e-mail. Fazer cada um deles chamar dois canais seria repetir a decisao em
 * sete lugares, e o oitavo esqueceria.
 *
 * Entao o canal passou a ser este, e os dois de verdade ficam atras dele.
 *
 * ----------------------------------------------------------------- a ordem, e o que ela protege
 *
 * <b>O in-app primeiro, e de proposito.</b> Ele e uma escrita no mesmo banco da transacao de quem
 * chamou; o externo e uma chamada HTTP a um terceiro. Se o e-mail falhasse antes, o aviso in-app
 * nunca seria gravado — e a pessoa perderia os dois por causa do canal mais fragil.
 *
 * <b>E a falha de um nao cala o outro.</b> Cada um vai no seu try: o dispatcher ja tratava falha de
 * envio como coisa a logar e seguir, e essa politica nao muda porque agora sao dois.
 */
@Slf4j
@Component
@Primary
public class Canais implements Notifier {

    private final Notifier externo;
    private final InAppNotifier inApp;

    /*
     * Construtor explicito, e nao `@RequiredArgsConstructor`: o Lombok so copia o `@Qualifier` para
     * o parametro se `lombok.copyableAnnotations` estiver configurado, e sem isso o Spring
     * escolheria o canal externo por tipo — que e ambiguo com este proprio bean.
     */
    public Canais(@Qualifier("canalExterno") Notifier externo, InAppNotifier inApp) {
        this.externo = externo;
        this.inApp = inApp;
    }

    @Override
    public void send(Notification notification) {
        send(notification, null);
    }

    public void send(Notification notification, String evento) {
        try {
            inApp.registrar(notification, evento);
        } catch (Exception e) {
            log.error("Falha ao guardar o aviso in-app de {}", evento, e);
        }

        /*
         * O ENDERECO NAO CONFIRMADO PARA O E-MAIL, E NAO O AVISO.
         *
         * Ate a V48 essa decisao morava nos notificadores do dominio, que PULAVAM o destinatario
         * nao confirmado antes de montar a mensagem. Com o in-app isso virou defeito: ele so aparece
         * para quem ja entrou na conta — nao vaza nada —, e quem nao confirmou o e-mail e exatamente
         * quem mais precisa dele. Pular la em cima matava os dois canais para essa pessoa.
         */
        if (notification.isPorEmail()) {
            externo.send(notification);
        }
    }

}
