package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonNotification;
import br.com.petfy.healthcare.domain.repository.PersonNotificationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * O canal que nao sai do produto: o aviso fica guardado para a pessoa ler quando abrir o app.
 *
 * <b>Nao substitui o e-mail — soma.</b> Os dois nascem da mesma {@link Notification}, entao nenhum
 * notificador do dominio precisou saber que este canal existe: quem escrevia um aviso continua
 * escrevendo um aviso.
 *
 * ------------------------------------------------------- por que ele fecha um buraco de verdade
 *
 * O e-mail funciona desde o P4 e nunca bastou. Ele nao chega a quem <b>ainda nao confirmou o
 * endereco</b> — e essa e a politica correta dos outros notificadores, nao um defeito deles —, e nao
 * chega a quem simplesmente nao abre a caixa. A Tela 03 lista quem esta vencendo e nao avisava
 * ninguem; a 47 promete "voce e avisado na hora".
 *
 * <b>Aqui a regra do e-mail nao se aplica</b>, e por uma razao que nao e afrouxamento: o aviso
 * in-app so aparece para quem <b>ja entrou na conta</b>. Nao ha endereco a que vazar nada — a pessoa
 * autenticada esta lendo o que e dela.
 *
 * ---------------------------------------------------------------------- quem nao tem conta
 *
 * <b>Sai calado, e e o comportamento certo.</b> O convite vai para um endereco que pode nao ser de
 * ninguem; nao ha caixa de entrada onde guardar. Quem nao tem conta e alcancado por e-mail, que e
 * exatamente o canal que existe para isso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotifier {

    private final PersonRepository personRepository;
    private final PersonNotificationRepository personNotificationRepository;

    public void registrar(Notification notification, String evento) {
        if (notification.getToEmail() == null || notification.getToEmail().isBlank()) {
            return;
        }

        Person destinatario = personRepository.findByEmail(notification.getToEmail().trim())
                .orElse(null);

        if (destinatario == null) {
            // convite para quem ainda nao tem conta: nao ha caixa de entrada, e o e-mail ja saiu
            return;
        }

        personNotificationRepository.save(PersonNotification.builder()
                .person(destinatario)
                .subject(recortar(notification.getSubject()))
                .body(String.join("\n", notification.getLines()))
                .event(evento)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * O assunto cabe em 200, e o corpo nao tem limite.
     *
     * <b>Recortar em vez de recusar</b>: um assunto comprido nao pode ser a razao de a pessoa nao
     * ser avisada de que alguem entrou no animal dela. O corpo, que e onde mora o que aconteceu,
     * fica inteiro.
     */
    private String recortar(String assunto) {
        if (assunto == null) {
            return "";
        }

        return assunto.length() <= 200 ? assunto : assunto.substring(0, 197) + "...";
    }

}
