package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Leva o convite a quem foi convidado.
 *
 * <b>Ate aqui nenhum convite chegava a ninguem.</b> O {@code invite} gravava o convite e devolvia o
 * token — "unico momento em que o token existe fora do cliente" —, e nao havia envio em lugar
 * nenhum. Tres telas criavam convite e nenhuma mostrava o codigo: ele morria no recarregamento, e o
 * convite ficava de pe esperando alguem que nunca soube dele. Quem convidava saia da tela achando
 * que a pessoa seria avisada.
 *
 * ------------------------------------------------------------ por que este notificador e diferente
 *
 * <b>Ele e o unico que escreve para um endereco que nao e de ninguem.</b> Os outros recebem
 * {@link Person} e recusam quem ainda nao confirmou o e-mail — "o nome do animal e dos tutores nao
 * vai para a caixa de um estranho". Aqui a regra nao pode valer, e a razao esta no proprio convite:
 * o caso comum e o conjuge que <b>ainda nao tem conta</b>. Exigir endereco confirmado seria exigir
 * que a pessoa se cadastrasse para receber o convite que a chama a se cadastrar.
 *
 * O que sustenta a excecao e que ninguem sorteou este endereco: <b>um tutor o digitou</b>, olhando
 * para ele, para chamar uma pessoa que ele conhece. E o mesmo grau de escolha que o
 * {@code EmailVerificationService} ja assume ao mandar codigo para um endereco por confirmar.
 *
 * ------------------------------------------------------------------------ codigo, e nao link
 *
 * <b>Nenhuma notificacao deste produto carrega link, e esta nao inaugura a pratica.</b> A
 * confirmacao de e-mail manda "Codigo: ..." e a pessoa cola; um link exigiria uma URL publica
 * configurada por ambiente, que nao existe em lugar nenhum — e a primeira delas nao deveria nascer
 * dentro de um notificador.
 *
 * O codigo E o token do convite, e nao um segundo segredo: quem o tem aceita, que e exatamente o
 * que o link faria. O que muda e onde ele e colado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteNotifier {

    private static final String IGNORAR =
            "Se voce nao esperava este convite, ignore este e-mail. Nada acontece sem voce aceitar.";

    private final AsyncNotificationDispatcher dispatcher;

    /**
     * "Marcelo convidou voce a cuidar do Code."
     *
     * <b>O assunto nomeia quem convidou e o animal</b>, e e o que separa este e-mail de um golpe:
     * sem os dois nomes a pessoa nao tem como reconhecer o convite, e um e-mail irreconhecivel com
     * um codigo dentro e exatamente a forma de uma fraude. O preco esta medido — quem digita o
     * endereco errado manda o nome do animal para um desconhecido —, e ele e menor que o de um
     * convite que ninguem acredita.
     *
     * <b>Dividir o cuidado e receber a responsabilidade sao ditos com todas as letras</b>, porque
     * sao coisas diferentes: HOLDER transfere a titularidade do animal, e aceitar sem saber disso
     * seria aceitar outra coisa.
     */
    public void conviteDeAnimal(Animal animal, Person quemConvidou, String email, PetTutorRole papel,
                                String token, int validadeEmDias) {
        if (email == null || email.isBlank()) {
            return;
        }

        boolean titularidade = papel == PetTutorRole.HOLDER;

        List<String> linhas = new ArrayList<>();
        linhas.add(String.format("%s quer que voce %s do %s no Petfy.",
                quemConvidou.getName(),
                titularidade ? "assuma a responsabilidade" : "divida o cuidado",
                animal.getName()));
        linhas.add("");

        if (titularidade) {
            linhas.add("Aceitar faz de voce quem responde pelo " + animal.getName()
                    + ": e quem decide sobre convites, acessos e o fim da linha do tempo dele.");
        } else {
            linhas.add("Aceitar deixa voce ver a vida inteira do " + animal.getName()
                    + " e registrar nela em nome proprio, junto com " + quemConvidou.getName() + ".");
        }

        enviar(email, String.format("%s convidou voce a cuidar do %s",
                quemConvidou.getName(), animal.getName()), linhas, token, validadeEmDias,
                "convite de animal");
    }

    /**
     * "A Clinica Vila Nova convidou voce para a equipe."
     *
     * <b>A funcao vai no corpo</b>, e nao so o nome da organizacao: entrar como veterinaria e entrar
     * como voluntaria dao poderes diferentes sobre o prontuario de animais que nao sao seus, e quem
     * aceita tem de saber qual dos dois esta aceitando. Quem escolhe a funcao e a organizacao, no
     * convite — a tela ja diz isso a quem convida, e o e-mail passa a dizer a quem recebe.
     */
    public void conviteDeOrganizacao(Organization organizacao, Person quemConvidou, String email,
                                     MembershipRole funcao, String token, int validadeEmDias) {
        if (email == null || email.isBlank()) {
            return;
        }

        List<String> linhas = new ArrayList<>();
        linhas.add(String.format("%s convidou voce para a equipe da %s no Petfy.",
                quemConvidou.getName(), organizacao.getName()));
        linhas.add("");
        linhas.add("Voce entra como " + descrever(funcao) + ".");
        linhas.add("O que voce registrar leva o seu nome, e nao o da organizacao.");

        enviar(email, String.format("%s convidou voce para a equipe da %s",
                quemConvidou.getName(), organizacao.getName()), linhas, token, validadeEmDias,
                "convite de organizacao");
    }

    private String descrever(MembershipRole funcao) {
        if (funcao == null) {
            return "membro da equipe";
        }

        return switch (funcao) {
            case ADMINISTRADOR -> "administradora, que decide sobre a equipe e os convites";
            case VETERINARIO -> "veterinaria, que registra diagnostico e prescricao";
            case MONITOR -> "monitora, que registra o dia a dia dos animais";
            case VOLUNTARIO -> "voluntaria, que acompanha sem alterar";
        };
    }

    /**
     * O codigo vem depois do que ele faz, e nunca antes.
     *
     * Quem le comeca sem saber se o e-mail e para ele; um codigo no topo e uma instrucao dada a
     * quem ainda nao decidiu nada. Primeiro o convite, depois a chave.
     *
     * <b>Falha nao desfaz o convite</b>, pela mesma politica dos outros avisos: o convite ja esta
     * gravado, e perde-lo porque o canal de e-mail caiu seria trocar um problema pequeno por um
     * grande. Quem convidou continua com o codigo na tela — e essa e a razao de a tela mostra-lo
     * mesmo agora que o e-mail existe.
     */
    private void enviar(String email, String assunto, List<String> linhas, String token,
                        int validadeEmDias, String evento) {
        linhas.add("");
        linhas.add("Codigo: " + token);
        linhas.add("Entre no Petfy e cole o codigo. Ele vale por " + validadeEmDias
                + (validadeEmDias == 1 ? " dia." : " dias."));
        linhas.add("");
        linhas.add(IGNORAR);

        try {
            dispatcher.dispatch(Notification.builder()
                    .toEmail(email.trim())
                    .subject(assunto)
                    .lines(linhas)
                    .build(), evento);
        } catch (Exception e) {
            log.error("Falha ao preparar o {} para {}", evento, email, e);
        }
    }

}
