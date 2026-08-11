package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

}
