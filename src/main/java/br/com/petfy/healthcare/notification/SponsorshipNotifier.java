package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Sponsorship;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Avisa o abrigo que alguem passou a bancar — e que alguem vai parar (Tela 46).
 *
 * <b>O segundo aviso e o que a tela promete com numero:</b> <i>"Pode parar quando quiser, sem
 * justificar. O abrigo e avisado com 30 dias para se organizar."</i> Sem ele os trinta dias nao
 * existiriam de fato: o abrigo descobriria no dia em que o remedio nao fosse comprado.
 *
 * <b>Quem recebe e a ORGANIZACAO, e nao as pessoas dela.</b> O
 * {@link br.com.petfy.healthcare.service.AnimalReach} resolveria pessoas e estaria errado aqui: quem
 * banca um custo do abrigo nao esta falando com um voluntario, e a lista de quem alcanca o animal
 * inclui clinicas e creches que nao tem nada com o dinheiro dele.
 *
 * <b>E o padrinho nao recebe nada</b>, nem confirmacao nem agradecimento: ele acabou de clicar e a
 * tela ja respondeu. Um e-mail de "obrigado por apadrinhar" seria o comeco da newsletter de campanha
 * que o desenho recusa em voz alta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SponsorshipNotifier {

    private static final String EVENTO = "apadrinhamento";

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AsyncNotificationDispatcher dispatcher;

    public void apadrinhado(Sponsorship apadrinhamento) {
        List<String> linhas = new ArrayList<>();
        linhas.add(apadrinhamento.getSponsor().getName() + " passou a bancar \""
                + apadrinhamento.getDescription() + "\" do "
                + apadrinhamento.getAnimal().getName() + ".");
        linhas.add("");
        linhas.add("Valor combinado: R$ " + apadrinhamento.getAmount() + " por mes.");
        linhas.add("O Petfy nao movimenta esse valor — ele registra o compromisso e mostra a quem "
                + "banca os gastos que voces lancarem no animal.");

        avisar(apadrinhamento.getOrganization(),
                "Alguem passou a bancar o " + apadrinhamento.getAnimal().getName(), linhas);
    }

    public void apadrinhamentoVaiAcabar(Sponsorship apadrinhamento) {
        List<String> linhas = new ArrayList<>();
        linhas.add(apadrinhamento.getSponsor().getName() + " vai parar de bancar \""
                + apadrinhamento.getDescription() + "\" do "
                + apadrinhamento.getAnimal().getName() + ".");
        linhas.add("");
        linhas.add("Ele continua cobrindo esse custo ate "
                + apadrinhamento.getEndsOn().format(DIA) + ".");
        // NAO PEDE E NAO CARREGA MOTIVO: "sem justificar" e parte da promessa, e repassar uma
        // justificativa que ninguem deu seria inventa-la
        linhas.add("Sao trinta dias para voces se organizarem. Nenhuma justificativa foi pedida.");

        avisar(apadrinhamento.getOrganization(),
                "Um apadrinhamento do " + apadrinhamento.getAnimal().getName() + " termina em 30 dias",
                linhas);
    }

    private void avisar(Organization destino, String assunto, List<String> linhas) {
        // organizacao sem e-mail nao e erro: o campo e opcional desde a Tela 15, e um abrigo pode
        // ter sido cadastrado so com telefone
        if (destino.getEmail() == null || destino.getEmail().isBlank()) {
            log.info("Organizacao {} nao tem e-mail; aviso de {} suprimido",
                    destino.getOrganizationId(), EVENTO);
            return;
        }

        try {
            dispatcher.dispatch(Notification.builder()
                    .toEmail(destino.getEmail())
                    .toName(destino.getName())
                    .subject(assunto)
                    .lines(linhas)
                    .build(), EVENTO);
        } catch (Exception e) {
            log.error("Falha ao preparar o aviso de {}", EVENTO, e);
        }
    }

}
