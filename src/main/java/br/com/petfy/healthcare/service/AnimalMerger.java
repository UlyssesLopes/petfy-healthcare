package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCost;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalSighting;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.Attachment;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Observation;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Move a vida registrada de um animal para o cadastro que o absorveu (Tela 32).
 *
 * <b>EVENTO MOVE, VINCULO NAO — e este e o criterio inteiro.</b> A vida registrada do animal e
 * uma so e atravessa quem cuidou dele; ja custodia e acesso sao relacoes que <i>cada cadastro</i>
 * estabeleceu com pessoas diferentes, e fundi-las daria a alguem acesso que ninguem concedeu.
 *
 * O desenho e explicito sobre isso: <b>"Marcelo continua sendo quem responde pelo animal. Unir
 * nao transfere custodia, e o acesso da sua clinica continua sendo o que ele concedeu."</b>
 *
 * <b>A lista e escrita a mao, e isso e deliberado</b> — a mesma escolha que o {@link AnimalPurger}
 * fez e pela mesma razao: derivar por reflexao transformaria a decisao em automatismo, e a
 * decisao de mover ou nao mover uma tabela nova tem de ser <i>tomada</i> por alguem. O
 * {@code AnimalMergerCoverageContainerTest} pergunta ao banco quem aponta para {@code animals} e
 * quebra o build enquanto a tabela nova nao for classificada aqui.
 *
 * <b>As netas nao entram, e nao por esquecimento:</b> correcao aponta para vacina e para
 * atendimento, cumprimento aponta para orientacao, presenca aponta para matricula, e escopo
 * aponta para concessao. Elas seguem o pai — mover o pai as leva junto, e move-las tambem seria
 * mover duas vezes.
 */
@Service
@RequiredArgsConstructor
public class AnimalMerger {

    /**
     * O que MOVE: a vida registrada.
     *
     * Sao os eventos que a linha do tempo mostra. Se dois cadastros sao o mesmo bicho, o que
     * aconteceu com ele aconteceu com ele — e "nenhum evento e descartado" e a promessa que a
     * tela faz a quem decide.
     */
    private static final List<Class<?>> MOVEM = List.of(
            // O custo e da vida do animal: "o custo do Code" nao muda porque descobriram que havia
            // dois cadastros dele. Se ficasse para tras, unir os cadastros faria metade do que o
            // tutor gastou desaparecer da conta — sem aviso nenhum.
            AnimalCost.class,
            Attachment.class,
            Vaccine.class,
            HealthRecord.class,
            AnimalWeightHistory.class,
            Antiparasitic.class,
            AnimalHealthCondition.class,
            CareInstruction.class,
            Observation.class,
            // O avistamento (V41) e o caso que mais parece vinculo desta lista, e nao e: ele
            // registra um fato do ANIMAL — ele estava vivo e na praca naquele dia. Se ficasse
            // para tras, unir dois cadastros do mesmo gato faria "visto por ultimo" saltar para
            // "ha 22 dias" no instante da uniao, e a colonia sairia procurando um gato que
            // alguem viu hoje de manha.
            //
            // <b>Ele e o unico da lista com indice unico</b>, e por isso tem um passo proprio
            // antes do loop. Ver {@link #desfazerAvistamentosEmDuplicata}.
            AnimalSighting.class);

    /*
     * O que NAO move, e por que cada um:
     *
     *   · custodies  — o animal ficaria com dois responsaveis, e "quem responde por ele" nao
     *                  admite dois. O sobrevivente mantem o seu; o absorvido leva o dele para o
     *                  apontador, onde ele conta a historia de quem cadastrou aquele registro.
     *   · grants     — "o acesso da sua clinica continua sendo o que ele concedeu". Mover daria
     *                  a clinica que PEDIU a uniao acesso ao animal inteiro, que e exatamente o
     *                  que ela nao pediu e ninguem lhe deu.
     *   · enrollments— matricula ocupa vaga numa turma. Mover dobraria a ocupacao, e a Tela 17
     *                  passaria a esperar o mesmo animal duas vezes na segunda de manha.
     *   · pet_tutor_invites — convite pendente e uma conversa comecada sobre AQUELE cadastro.
     *   · sensitive_access_log — log de acesso e o registro de que alguem leu AQUELA linha.
     *                  Reescrever a quem ele se refere seria falsificar uma auditoria.
     */

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Move os eventos do absorvido para o sobrevivente.
     *
     * <b>Nao mexe em {@code recordedBy} nem em {@code organization}</b>, e essa e a segunda
     * promessa da tela: "cada evento continua assinado por quem o registrou, com a data de
     * lancamento original". Reatribuir autoria transformaria uma uniao de cadastros numa
     * falsificacao de prontuario.
     *
     * Devolve quantas linhas moveram — o numero vai para o evento da uniao, porque "147 eventos"
     * e o que faz quem decide entender o tamanho do que esta aceitando.
     */
    @Transactional
    public int mover(UUID doAbsorvido, UUID paraOSobrevivente) {
        Animal sobrevivente = entityManager.getReference(Animal.class, paraOSobrevivente);

        desfazerAvistamentosEmDuplicata(doAbsorvido, paraOSobrevivente);

        int movidos = 0;

        for (Class<?> tipo : MOVEM) {
            movidos += entityManager
                    .createQuery("update " + tipo.getSimpleName()
                            + " e set e.animal = :sobrevivente where e.animal.animalId = :absorvido")
                    .setParameter("sobrevivente", sobrevivente)
                    .setParameter("absorvido", doAbsorvido)
                    .executeUpdate();
        }

        /* O contexto de persistencia guarda entidades ja carregadas com o animal ANTIGO: o
           `update` em JPQL vai direto ao banco e nao as atualiza. Sem limpar, quem ler o animal
           logo depois nesta mesma transacao veria o estado de antes. */
        entityManager.flush();
        entityManager.clear();

        return movidos;
    }

    /**
     * Apaga do absorvido os avistamentos que colidiriam com os do sobrevivente.
     *
     * <b>O avistamento e o unico dos que MOVEM com indice unico</b> — {@code (animal, pessoa,
     * dia)} —, e a colisao nao e hipotetica: ela e o caso TIPICO da uniao. Dois cadastros do
     * mesmo gato existem justamente porque duas pessoas o registraram, e as duas o veem no mesmo
     * dia. Sem este passo, o {@code update} em massa estoura e a uniao inteira falha por
     * violacao de chave.
     *
     * <b>Apagar aqui nao perde fato nenhum</b>, e e por isso que a saida e esta e nao um
     * {@code on conflict do nothing} escondido: os dois cadastros eram o mesmo animal, entao
     * Sandra vendo "os dois" no dia 12 viu um gato. A linha que fica diz exatamente o mesmo que
     * a que sai.
     */
    private void desfazerAvistamentosEmDuplicata(UUID doAbsorvido, UUID paraOSobrevivente) {
        entityManager.createQuery(
                        "delete from AnimalSighting s where s.animal.animalId = :absorvido "
                                + "and exists (select 1 from AnimalSighting outro "
                                + "  where outro.animal.animalId = :sobrevivente "
                                + "    and outro.recordedBy = s.recordedBy "
                                + "    and outro.seenOn = s.seenOn)")
                .setParameter("absorvido", doAbsorvido)
                .setParameter("sobrevivente", paraOSobrevivente)
                .executeUpdate();
    }

}
