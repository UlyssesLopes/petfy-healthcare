package br.com.petfy.healthcare.notification;

/**
 * Canal de envio do lembrete. Existe como interface para que a regra de quando
 * avisar nao dependa de como avisar: hoje sai por log ou e-mail, e um eventual
 * push nao mexe no VaccineReminderService.
 */
public interface ReminderNotifier {

    void notify(VaccineReminder reminder);

}
