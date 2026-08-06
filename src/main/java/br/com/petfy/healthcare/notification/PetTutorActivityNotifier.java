package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Avisa quem cuida do animal quando o time de tutores muda.
 *
 * Entrar num animal, virar titular e perder acesso sao eventos de privacidade: quem
 * divide o cuidado do animal precisa saber que outra pessoa passou a ler o
 * historico de saude dele, e quem perdeu acesso precisa saber que perdeu. Sem
 * aviso, a unica forma de descobrir e abrir o app e comparar a lista de tutores
 * com o que se lembrava dela.
 *
 * <b>E o irmao do {@link ClinicActivityNotifier}:</b> la o aviso e sobre o que uma
 * clinica escreveu no animal, aqui e sobre quem passou a poder escrever. Mesma
 * politica de falha, mesmo respeito ao e-mail nao confirmado, mesma orientacao no
 * rodape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetTutorActivityNotifier {

    private static final String ORIENTACAO =
            "Se nao reconhece esta mudanca, revise os tutores do animal no Petfy.";

    private final AsyncNotificationDispatcher dispatcher;

    /**
     * Alguem aceitou um convite e passou a cuidar do animal.
     *
     * Quem entrou nao recebe o aviso: acabou de aceitar o convite, entao contar-lhe
     * o que ele mesmo fez e ruido. Os outros e que precisam saber.
     */
    public void tutorEntrou(Animal animal, List<Owner> tutores, Owner novoTutor, PetTutorRole papel) {
        enviar(tutores, novoTutor, destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s passou a cuidar do %s com voce.",
                    novoTutor.getName(), animal.getName()));
            linhas.add("- papel: " + descrever(papel));

            return montar(destinatario, novoTutor.getName() + " entrou no " + animal.getName(), linhas);
        }, "tutor que entrou no animal");
    }

    /**
     * O animal trocou de titular, por transferencia direta ou por convite aceito.
     *
     * Vai para todos os tutores, e nao so para os dois envolvidos: quem e o titular
     * define quem pode convidar, remover e apagar o animal, entao a informacao e de
     * quem cuida do animal, nao dos dois que fizeram a troca. Quem executou a acao
     * fica de fora, por ja saber.
     */
    public void titularidadeMudou(Animal animal, List<Owner> tutores, Owner titularAnterior,
                                  Owner novoTitular, Owner quemAgiu) {
        enviar(tutores, quemAgiu, destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("A titularidade do %s passou de %s para %s.",
                    animal.getName(), titularAnterior.getName(), novoTitular.getName()));
            linhas.add("- quem e titular decide sobre convites, remocoes e exclusao do animal");
            linhas.add(String.format("- %s continua tutor, agora como %s",
                    titularAnterior.getName(), descrever(PetTutorRole.EDITOR)));

            return montar(destinatario, "Novo titular do " + animal.getName(), linhas);
        }, "mudanca de titularidade");
    }

    /**
     * Um tutor deixou de cuidar do animal, por vontade propria ou removido pelo
     * titular.
     *
     * <b>Quem saiu tambem e avisado, mas so quando nao foi ele quem saiu por conta
     * propria.</b> Perder acesso ao historico de saude de um animal que se cuidava
     * sem receber uma linha sobre isso e a pior versao deste evento - a pessoa
     * descobre tentando abrir a carteira.
     */
    public void tutorSaiu(Animal animal, List<Owner> tutoresQueFicam, Owner queSaiu, Owner quemAgiu) {
        boolean removidoPorOutro = !queSaiu.getOwnerId().equals(quemAgiu.getOwnerId());

        enviar(tutoresQueFicam, quemAgiu, destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s deixou de cuidar do %s.", queSaiu.getName(), animal.getName()));

            return montar(destinatario, queSaiu.getName() + " saiu do " + animal.getName(), linhas);
        }, "tutor que saiu do animal");

        if (removidoPorOutro) {
            enviar(List.of(queSaiu), null, destinatario -> {
                List<String> linhas = new ArrayList<>();
                linhas.add(String.format("Voce deixou de ter acesso ao %s.", animal.getName()));
                linhas.add("- o historico de saude do animal continua com quem cuida dele");

                return montar(destinatario, "Seu acesso ao " + animal.getName() + " foi encerrado", linhas);
            }, "acesso encerrado");
        }
    }

    private String descrever(PetTutorRole papel) {
        return switch (papel) {
            case HOLDER -> "titular, que decide sobre convites e exclusao";
            case EDITOR -> "editor, que registra vacina e corrige dados";
            case VIEWER -> "leitor, que acompanha sem alterar";
        };
    }

    /**
     * Mesma politica do {@link ClinicActivityNotifier}, pelas mesmas razoes.
     *
     * A mensagem e montada aqui, ainda na transacao de quem chamou, porque monta-la
     * passa por associacoes lazy - o animal, o tutor. So o envio sai para outra thread.
     *
     * Falha nao desfaz a mudanca: o vinculo ja esta gravado, e perde-lo porque o
     * canal de e-mail caiu seria trocar um problema pequeno por um grande. E o
     * try e por destinatario, porque falhar para um tutor nao pode calar os outros.
     *
     * Enquanto o e-mail nao for confirmado, nada sai para aquele endereco: o nome do
     * animal e dos tutores nao vai para a caixa de um estranho.
     */
    private void enviar(List<Owner> destinatarios, Owner excluir, Function<Owner, Notification> mensagem,
                        String evento) {
        for (Owner destinatario : destinatarios) {
            if (excluir != null && destinatario.getOwnerId().equals(excluir.getOwnerId())) {
                continue;
            }

            if (!destinatario.podeReceberNotificacao()) {
                log.info("Tutor {} ainda nao confirmou o e-mail; aviso de {} suprimido",
                        destinatario.getOwnerId(), evento);
                continue;
            }

            try {
                dispatcher.dispatch(mensagem.apply(destinatario), evento);
            } catch (Exception e) {
                log.error("Falha ao preparar o aviso de {} para o tutor {}",
                        evento, destinatario.getOwnerId(), e);
            }
        }
    }

    private Notification montar(Owner owner, String assunto, List<String> linhas) {
        linhas.add("");
        linhas.add(ORIENTACAO);

        return Notification.builder()
                .toEmail(owner.getEmail())
                .toName(owner.getName())
                .subject(assunto)
                .lines(linhas)
                .build();
    }

}
