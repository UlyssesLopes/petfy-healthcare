package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.TimelineEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TimelineRepository extends JpaRepository<TimelineEntry, UUID> {

    /**
     * A linha do tempo de um animal, do mais recente para o mais antigo.
     *
     * <b>Ordena por {@code occurredAt}, e nao por {@code recordedAt}</b> - e por isso
     * que a vacina de 2019 cadastrada hoje aparece em 2019. O desempate por
     * {@code recordedAt} existe para a ordem ser estavel entre paginas: sem ele, dois
     * eventos do mesmo dia poderiam trocar de lugar entre a pagina 1 e a 2, e o cliente
     * veria um item duas vezes e outro nunca.
     *
     * <b>Paginada</b> porque a linha do tempo de um animal de dez anos e a maior
     * colecao que este produto vai servir - e a unica que cresce para sempre.
     */
    @Query("select t from TimelineEntry t where t.animalId = :animalId "
            + "order by t.occurredAt desc, t.recordedAt desc")
    Page<TimelineEntry> findDoAnimal(@Param("animalId") UUID animalId, Pageable pageable);

    /**
     * Quando cada pessoa contribuiu por ultimo neste animal.
     *
     * <b>E o que faz a rede de quem cuida parecer viva</b> (DESIGN 5.4): "cada pessoa
     * aparece com iniciais, papel e a ultima contribuicao - registrou hoje, registrou
     * ontem". Sem isso a rede e uma lista de nomes, e nao mostra que alguem esta cuidando.
     *
     * <b>Agregada, e nao uma consulta por pessoa.</b> Uma rede de cinco pessoas custaria
     * cinco consultas, e e o mesmo N+1 que a V29 acabou de tirar da linha do tempo.
     *
     * Ordena por {@code recordedAt} e nao por {@code occurredAt} de proposito: a pergunta
     * e "quando esta pessoa apareceu por aqui", que e digitacao. Quem lanca hoje a vacina
     * de 2019 contribuiu hoje.
     */
    @Query("select t.recordedByPersonId as pessoaId, max(t.recordedAt) as em "
            + "from TimelineEntry t "
            + "where t.animalId = :animalId and t.recordedByPersonId is not null "
            + "group by t.recordedByPersonId")
    List<UltimaContribuicao> ultimaContribuicaoPorPessoa(@Param("animalId") UUID animalId);

    /** O mesmo, por organizacao: "a Clinica Norte registrou ontem". */
    @Query("select t.organizationId as pessoaId, max(t.recordedAt) as em "
            + "from TimelineEntry t "
            + "where t.animalId = :animalId and t.organizationId is not null "
            + "group by t.organizationId")
    List<UltimaContribuicao> ultimaContribuicaoPorOrganizacao(@Param("animalId") UUID animalId);

    /** Projecao das duas consultas acima. O id e de pessoa ou de organizacao, conforme a consulta. */
    interface UltimaContribuicao {

        UUID getPessoaId();

        LocalDateTime getEm();

    }

}
