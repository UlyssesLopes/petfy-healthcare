package br.com.petfy.healthcare.notification;

/** Canal de envio. Nao conhece o dominio: recebe mensagem pronta. */
public interface Notifier {

    void send(Notification notification);

}
