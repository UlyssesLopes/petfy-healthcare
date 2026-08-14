package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * "Entrada em um aparelho novo" — o terceiro estado da Tela 28.
 *
 * <b>Aviso, e nao bloqueio</b>, e o desenho e explicito: "quem esta com pressa entra; quem foi
 * invadido descobre". Barrar a entrada puniria quem trocou de celular, que e o caso comum.
 *
 * <b>O desenho pede um lugar, e nao ha lugar.</b> O mockup diz "Hoje, 21h14 · Sao Paulo", e a
 * segunda metade nao tem de onde sair: o produto decidiu nao guardar IP, e esta escrito no
 * {@code PersonSession} — "guardar por onde alguem entra e o que a Tela 34 recusa". Sobram a hora
 * e o aparelho, que sao o que a pessoa usa para reconhecer a si mesma. Mesma divergencia, pelo
 * mesmo motivo, que o rodape do "achei um animal na rua".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovoAparelhoNotifier {

    private static final String EVENTO = "entrada de aparelho novo";

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH'h'mm");

    private final AsyncNotificationDispatcher dispatcher;

    public void entrou(Person pessoa, PersonSession sessao) {
        boolean porEmail = pessoa.podeReceberNotificacao();

        if (!porEmail) {
            log.info("Pessoa {} ainda nao confirmou o e-mail; aviso de {} fica so no app",
                    pessoa.getPersonId(), EVENTO);
        }

        try {
            dispatcher.dispatch(Notification.builder()
                    .toEmail(pessoa.getEmail())
                    .toName(pessoa.getName())
                    .subject("Entrada em um aparelho novo")
                    .lines(corpo(sessao))
                    .porEmail(porEmail)
                    .build(), EVENTO);
        } catch (Exception e) {
            log.error("Falha ao preparar o aviso de {}", EVENTO, e);
        }
    }

    /**
     * O texto diz o que aconteceu, quando, de onde, e a saida — nessa ordem.
     *
     * <b>A saida e um caminho, e nao um link.</b> Nao ha URL publica configurada, e e a mesma
     * razao que faz o convite ir por codigo: um link montado no escuro chegaria quebrado
     * exatamente para quem esta desconfiado de invasao.
     */
    private List<String> corpo(PersonSession sessao) {
        List<String> linhas = new ArrayList<>();

        linhas.add("Sua conta foi acessada de um aparelho que ainda nao tinha entrado aqui.");
        linhas.add(sessao.getCreatedAt().format(QUANDO));

        String aparelho = RotuloDoAparelho.de(sessao.getUserAgent());
        if (aparelho != null) {
            linhas.add(aparelho);
        }

        linhas.add("Se foi voce, nao ha nada a fazer.");
        linhas.add("Se nao foi, entre em Conta > Aparelhos conectados, encerre essa entrada e "
                + "troque a senha — trocar a senha derruba todas as sessoes abertas.");

        return linhas;
    }

}
