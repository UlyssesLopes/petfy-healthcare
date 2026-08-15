package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /**
     * Os vinculos ativos da pessoa.
     *
     * Devolve lista, e nao Optional, porque N vinculos e o caso que este passo existe
     * para permitir. Quem chama decide o que fazer com mais de um - e a resposta nao e
     * escolher o primeiro em silencio.
     *
     * <b>O {@code join fetch} nao e otimizacao: sem ele o contexto ativo estourava.</b> Quem
     * chama e o "Agindo como", que le {@code organization.name} depois que a transacao fechou —
     * e {@code Membership.organization} e lazy, com {@code spring.jpa.open-in-view=false}. O
     * defeito ficou invisivel ate 2026-08-15 porque ninguem tinha vinculo: criar organizacao nao
     * gravava membership, e sem nenhuma linha a lista vinha vazia e nada era desreferenciado.
     * Consertar a criacao sem consertar isto teria trocado uma tela quebrada por 500 em toda tela
     * de quem tem organizacao.
     */
    @Query("select m from Membership m join fetch m.organization "
            + "where m.person.personId = :personId and m.leftAt is null")
    List<Membership> findAtivosDaPessoa(@Param("personId") UUID personId);

    /**
     * Os vinculos ativos de VARIAS pessoas de uma vez — "Clinica Anhangabau", na busca da Tela 45.
     *
     * <b>Existe para nao fazer uma consulta por resultado.</b> A busca devolve ate vinte
     * profissionais, e perguntar onde cada um atende seria o N+1 que a V29 tirou da linha do tempo e
     * o {@code ultimaContribuicaoPorPessoa} evitou na rede de quem cuida. O {@code join fetch} da
     * organizacao vem pela mesma razao de sempre: o DTO le o nome dela fora da transacao.
     */
    @Query("select m from Membership m join fetch m.organization "
            + "where m.person.personId in :personIds and m.leftAt is null")
    List<Membership> findAtivosDasPessoas(@Param("personIds") List<UUID> personIds);

    /**
     * O vinculo da pessoa NAQUELA organizacao — a pergunta do header {@code X-Petfy-Organization}.
     *
     * {@code join fetch} pela mesma razao da consulta acima: quem chama devolve o nome da
     * organizacao para o cliente, fora da transacao.
     */
    @Query("select m from Membership m join fetch m.organization "
            + "where m.person.personId = :personId "
            + "and m.organization.organizationId = :organizationId and m.leftAt is null")
    Optional<Membership> findAtivoDaPessoaNaOrganizacao(@Param("personId") UUID personId,
                                                       @Param("organizationId") UUID organizationId);

    /** Os membros de uma organizacao agora. */
    @Query("select m from Membership m where m.organization.organizationId = :organizationId "
            + "and m.leftAt is null")
    List<Membership> findAtivosDaOrganizacao(@Param("organizationId") UUID organizationId);

    /**
     * Todo vinculo da pessoa, ativo ou nao. Usado na exclusao de conta: a linha aponta
     * para persons e seguraria o delete.
     */
    List<Membership> findByPersonPersonId(UUID personId);

}
