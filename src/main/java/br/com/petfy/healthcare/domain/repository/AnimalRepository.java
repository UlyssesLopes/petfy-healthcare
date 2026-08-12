package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    /**
     * Os animais que a pessoa alcanca: por responder por eles, ou por concessao.
     *
     * <b>Deixou de ser consulta derivada no P2b, e nao por gosto.</b> Era
     * {@code findByTutorsPersonPersonId}, que atravessava uma associacao so porque
     * custodia e acesso moravam na mesma tabela. Agora sao duas, e derivada nao faz
     * OR entre duas associacoes - forcar isso daria dois metodos e uma uniao em
     * memoria, com paginacao impossivel de acertar.
     *
     * Dois {@code exists} em vez de {@code join}: com join a mesma linha de animal
     * volta uma vez por concessao, e a listagem paginada passaria a mentir na
     * contagem. Foi por isso que a paginacao entrou nas dividas operacionais.
     */
    @Query("select a from Animal a where exists ("
            + "  select 1 from Custody c where c.animal = a "
            + "    and c.holderPerson.personId = :personId and c.endedAt is null) "
            + "or exists ("
            + "  select 1 from Grant g where g.animal = a "
            + "    and g.granteePerson.personId = :personId and g.revokedAt is null "
            + "    and (g.expiresAt is null or g.expiresAt > :agora))")
    List<Animal> findAlcancadosPor(@Param("personId") UUID personId,
                                   @Param("agora") LocalDateTime agora);

    @Query("select a from Animal a where exists ("
            + "  select 1 from Custody c where c.animal = a "
            + "    and c.holderPerson.personId = :personId and c.endedAt is null) "
            + "or exists ("
            + "  select 1 from Grant g where g.animal = a "
            + "    and g.granteePerson.personId = :personId and g.revokedAt is null "
            + "    and (g.expiresAt is null or g.expiresAt > :agora))")
    Page<Animal> findAlcancadosPor(@Param("personId") UUID personId,
                                   @Param("agora") LocalDateTime agora,
                                   Pageable pageable);

    void deleteByAnimalIdIn(List<UUID> animalIds);

    /**
     * Outros cadastros com o mesmo microchip — a deteccao da Tela 32.
     *
     * <b>Exclui quem ja foi absorvido</b>, e nao por elegancia: um cadastro absorvido guarda o
     * microchip que tinha, e sem este filtro a duplicata reapareceria para sempre — a clinica
     * uniria os dois e, no dia seguinte, o produto ofereceria unir de novo com o fantasma.
     *
     * <b>E exclui quem ja foi marcado como animal diferente?</b> Nao, e de proposito: a marca diz
     * "alguem afirmou que sao outros bichos", e nao "pare de perguntar". Se a duplicata some da
     * deteccao, o erro de digitacao que a marca denuncia nunca mais e encontrado por ninguem.
     */
    @Query("select a from Animal a where a.microchipNumber = :microchip "
            + "and a.animalId <> :exceto and a.mergedIntoAnimalId is null")
    List<Animal> findOutrosComMicrochip(@Param("microchip") String microchip,
                                        @Param("exceto") UUID exceto);

    /**
     * Os cadastros com este microchip — a busca de animal encontrado (Tela 34).
     *
     * <b>Devolve lista, e nao Optional, porque o duplicado e a regra e nao a excecao neste
     * produto.</b> A Tela 32 existe justamente porque o mesmo animal e cadastrado pela protetora,
     * pela clinica e pelo abrigo. Um {@code Optional} aqui estouraria com
     * {@code NonUniqueResultException} na rua, no pior momento, e quem escolhe qual cadastro
     * responde e o servico — com um criterio escrito.
     *
     * <b>Exclui o absorvido</b>, pela mesma razao da consulta acima: ele guarda o microchip que
     * tinha, e devolver um apontador daria a quem socorre o animal um cartao sem vida registrada.
     */
    @Query("select a from Animal a where a.microchipNumber = :microchip "
            + "and a.mergedIntoAnimalId is null")
    List<Animal> findComMicrochip(@Param("microchip") String microchip);

    /**
     * A busca autenticada da Tela 35: "nome, microchip ou RGA".
     *
     * <b>Os tres campos numa consulta so, porque quem digita nao sabe em qual esta digitando.</b> A
     * pessoa poe "9810" ou "Code" no mesmo campo — obrigar a escolher o tipo antes de buscar seria
     * pedir a ela a resposta que ela veio procurar.
     *
     * <b>A PARCIAL EXISTE AQUI, e no {@code POST /found} nao existe.</b> A diferenca nao e de gosto e
     * vale registrar: la a busca e PUBLICA e por microchip, e "9810" acharia todo animal de uma
     * fabricante de chip — era varredura, e nao servia a ninguem, porque quem esta com o animal na mao
     * le o numero inteiro. Aqui quem busca ja alcanca os animais que a consulta devolve: o recorte de
     * acesso vem do serviço, e a parcial so ordena o que ja e dela.
     *
     * <b>Exclui o absorvido</b>, como as duas consultas acima: devolver um apontador daria a quem
     * busca um cadastro sem vida registrada.
     */
    @Query("select a from Animal a where a.mergedIntoAnimalId is null "
            + "and a.animalId in :alcancados "
            + "and (lower(a.name) like :busca "
            + "  or lower(coalesce(a.microchipNumber, '')) like :busca "
            + "  or lower(coalesce(a.generalRegistry, '')) like :busca) "
            + "order by a.name asc")
    List<Animal> buscarEntre(@Param("alcancados") Collection<UUID> alcancados,
                             @Param("busca") String busca);

    /**
     * <b>Ha animal que casa com a busca e que quem procura NAO alcanca?</b>
     *
     * "Existem outros animais com microchip comecando em 9810 no Petfy. Voce nao tem acesso a eles, e
     * por isso nao aparecem aqui."
     *
     * <b>E a frase mais incomum desta tela, e ela e deliberada.</b> A saida obvia seria devolver uma
     * lista curta e calar sobre o resto — e o efeito seria a pessoa concluir que o animal que ela
     * procura nao esta no Petfy. Dizer que ele existe e que ela nao o alcanca e a unica resposta
     * verdadeira, e e ela que torna util o caminho seguinte: a busca de animal encontrado.
     *
     * <b>Devolve BOOLEAN, e nao contagem.</b> Um numero seria um oraculo: quem variasse o termo mediria
     * quantos animais existem com cada prefixo de microchip. O booleano sustenta a frase e nao mede
     * nada.
     */
    @Query("select count(a) > 0 from Animal a where a.mergedIntoAnimalId is null "
            + "and a.animalId not in :alcancados "
            + "and (lower(a.name) like :busca "
            + "  or lower(coalesce(a.microchipNumber, '')) like :busca "
            + "  or lower(coalesce(a.generalRegistry, '')) like :busca)")
    boolean existeForaDoAlcance(@Param("alcancados") Collection<UUID> alcancados,
                                @Param("busca") String busca);

}
