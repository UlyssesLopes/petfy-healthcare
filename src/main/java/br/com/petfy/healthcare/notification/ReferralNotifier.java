package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Referral;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Avisa quem precisa decidir e quem passou a poder abrir o caso (Tela 45).
 *
 * <b>Sem isto a tela nao tem como cumprir "O Marcelo recebe e decide".</b> O encaminhamento e o
 * unico fluxo do produto em que a acao de uma pessoa fica parada esperando outra que nao esta na
 * tela — a clinica clica e sai do consultorio. Sem aviso, o pedido ficaria esperando um tutor que
 * nao sabe que existe, e a Ana descobriria semanas depois que o Roberto nunca abriu nada.
 *
 * <b>Tres momentos, e nao quatro.</b> O especialista NAO e avisado do pendente: seria oferecer um
 * caso que ele ainda nao pode abrir, e o pedido pode ser recusado. Ele aparece na caixa de entrada
 * dele se ele abrir, sem o nome do animal — que e outra coisa.
 *
 * <b>Falhar aqui nao pode desfazer nada</b>, pela mesma razao do {@link AnimalDeathNotifier}: o
 * efeito principal e o registro, e perde-lo porque o SMTP caiu trocaria um problema pequeno por um
 * grande.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralNotifier {

    private static final String EVENTO = "encaminhamento";

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AsyncNotificationDispatcher dispatcher;
    private final CustodyRepository custodyRepository;

    /**
     * "O Marcelo recebe e decide."
     *
     * <b>O destinatario e quem RESPONDE pelo animal, e nao quem alcanca.</b> O
     * {@link br.com.petfy.healthcare.service.AnimalReach} resolveria uma lista maior — a co-tutora, a
     * clinica com acesso — e estaria errada aqui: quem tem concessao nao pode autorizar nada, e um
     * e-mail pedindo decisao a quem nao decide produz a pior forma de aviso, a que faz a pessoa achar
     * que precisa agir e descobrir que nao pode.
     *
     * <b>A organizacao que responde tambem e avisada</b>, pelo e-mail dela: o animal do abrigo nao
     * tem tutor humano, e sem isto o encaminhamento de um animal resgatado ficaria parado para sempre.
     */
    public void encaminhamentoPedido(Referral referral) {
        Custody custodia = custodyRepository.findEmCurso(referral.getAnimal().getAnimalId())
                .orElse(null);

        if (custodia == null) {
            // sem custodia em curso nao ha quem autorize, e o servico ja recusou antes de chegar
            // aqui. O log existe para o caso de a custodia ter sido encerrada no meio
            log.info("Animal {} nao tem custodia em curso; aviso de {} suprimido",
                    referral.getAnimal().getAnimalId(), EVENTO);
            return;
        }

        avisarPessoa(custodia.getHolderPerson(), assuntoPedido(referral), paraQuemDecide(referral));
        avisarOrganizacao(custodia.getHolderOrganization(), assuntoPedido(referral),
                paraQuemDecide(referral));
    }

    /**
     * O especialista descobre que pode abrir, e quem encaminhou descobre que foi autorizado.
     *
     * <b>Os dois no mesmo momento, e com textos diferentes.</b> O especialista precisa saber o que
     * esta sendo perguntado e ate quando alcanca; quem encaminhou precisa saber que nao precisa mais
     * telefonar. Um texto unico serviria mal aos dois.
     */
    public void encaminhamentoAutorizado(Referral referral) {
        avisarPessoa(referral.getToPerson(),
                "Encaminhamento do " + referral.getAnimal().getName() + " liberado para voce",
                paraEspecialista(referral));

        avisarPessoa(referral.getReferredBy(),
                "O encaminhamento do " + referral.getAnimal().getName() + " foi autorizado",
                paraQuemEncaminhou(referral, true));
    }

    /**
     * So quem encaminhou e avisado.
     *
     * <b>O especialista nao recebe nada</b>, e nao e omissao: ele nunca foi avisado do pedido, e um
     * e-mail dizendo "o encaminhamento que voce nao sabia que existia foi recusado" informaria a
     * existencia de um animal cujo tutor acabou de decidir que ele nao deve ver.
     *
     * <b>E o motivo da recusa nao viaja</b>, porque nao e pedido: autorizar acesso ao proprio
     * prontuario e decisao de quem responde pelo animal, e nao ha a quem justificar.
     */
    public void encaminhamentoRecusado(Referral referral) {
        avisarPessoa(referral.getReferredBy(),
                "O encaminhamento do " + referral.getAnimal().getName() + " nao foi autorizado",
                paraQuemEncaminhou(referral, false));
    }

    private String assuntoPedido(Referral referral) {
        return "Um encaminhamento do " + referral.getAnimal().getName() + " espera sua decisao";
    }

    private List<String> paraQuemDecide(Referral referral) {
        List<String> linhas = new ArrayList<>();
        linhas.add(quemEncaminha(referral) + " encaminhou o " + referral.getAnimal().getName()
                + " para " + referral.getToPerson().getName() + ".");
        linhas.add("");
        linhas.add(referral.getReason());
        linhas.add("");
        linhas.add("Encaminhar e indicar o caminho; conceder acesso continua sendo seu. Se voce "
                + "autorizar, o acesso vale " + referral.getAccessDays()
                + " dias e depois fecha sozinho.");
        linhas.add("Abra o Petfy para ver o que iria junto e decidir.");
        return linhas;
    }

    private List<String> paraEspecialista(Referral referral) {
        List<String> linhas = new ArrayList<>();
        linhas.add(quemEncaminha(referral) + " encaminhou o " + referral.getAnimal().getName()
                + " para voce.");
        linhas.add("");
        linhas.add(referral.getReason());
        linhas.add("");

        // a data vem da concessao, e nao de decidedAt + accessDays: quem revoga antes muda aquela
        // linha, e o texto continuaria anunciando um prazo que nao vale mais
        if (referral.getGrantAcesso() != null && referral.getGrantAcesso().getExpiresAt() != null) {
            linhas.add("Acesso liberado por quem responde pelo animal ate "
                    + referral.getGrantAcesso().getExpiresAt().format(DIA) + ".");
        }

        linhas.add("Abra o Petfy para ver o caso.");
        return linhas;
    }

    private List<String> paraQuemEncaminhou(Referral referral, boolean autorizado) {
        List<String> linhas = new ArrayList<>();

        if (autorizado) {
            linhas.add("Quem responde pelo " + referral.getAnimal().getName()
                    + " autorizou o encaminhamento para " + referral.getToPerson().getName() + ".");
            linhas.add("");
            linhas.add("Ele ja pode abrir o caso, e o acesso fecha sozinho em "
                    + referral.getAccessDays() + " dias.");
            return linhas;
        }

        linhas.add("Quem responde pelo " + referral.getAnimal().getName()
                + " nao autorizou o encaminhamento para " + referral.getToPerson().getName() + ".");
        linhas.add("");
        linhas.add("O caso nao foi compartilhado. Se ainda faz sentido encaminhar, converse com "
                + "quem responde pelo animal.");
        return linhas;
    }

    /** "Ana Ferreira, pela Clinica Vet Norte" — ou so o nome, no autonomo. */
    private String quemEncaminha(Referral referral) {
        String nome = referral.getReferredBy().getName();

        return referral.getFromOrganization() == null
                ? nome
                : nome + ", pela " + referral.getFromOrganization().getName();
    }

    private void avisarPessoa(Person destinatario, String assunto, List<String> linhas) {
        if (destinatario == null) {
            return;
        }

        if (!destinatario.podeReceberNotificacao()) {
            log.info("Pessoa {} ainda nao confirmou o e-mail; aviso de {} suprimido",
                    destinatario.getPersonId(), EVENTO);
            return;
        }

        despachar(() -> Notification.builder()
                .toEmail(destinatario.getEmail())
                .toName(destinatario.getName())
                .subject(assunto)
                .lines(linhas)
                .build());
    }

    private void avisarOrganizacao(Organization destino, String assunto, List<String> linhas) {
        if (destino == null) {
            return;
        }

        // organizacao sem e-mail nao e erro: o campo e opcional desde a Tela 15
        if (destino.getEmail() == null || destino.getEmail().isBlank()) {
            log.info("Organizacao {} nao tem e-mail; aviso de {} suprimido",
                    destino.getOrganizationId(), EVENTO);
            return;
        }

        despachar(() -> Notification.builder()
                .toEmail(destino.getEmail())
                .toName(destino.getName())
                .subject(assunto)
                .lines(linhas)
                .build());
    }

    /** O try e por destinatario: falhar para um nao pode calar os outros. */
    private void despachar(java.util.function.Supplier<Notification> mensagem) {
        try {
            dispatcher.dispatch(mensagem.get(), EVENTO);
        } catch (Exception e) {
            log.error("Falha ao preparar o aviso de {}", EVENTO, e);
        }
    }

}
