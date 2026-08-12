package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Sponsorship;
import br.com.petfy.healthcare.domain.entity.SponsorshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, UUID> {

    /**
     * O mesmo padrinho bancando a mesma coisa no mesmo animal, ainda vivo.
     *
     * Bancar duas vezes "o remedio da artrose" nao e generosidade dobrada: e a mesma pessoa tendo
     * clicado duas vezes, e o abrigo somaria R$ 160 de um custo que continua sendo R$ 80.
     *
     * <b>Compara em minusculas</b>, como o indice parcial do banco: "O remedio" e "o remedio" sao a
     * mesma coisa bancada, e deixar as duas passar reintroduziria a duplicata pela porta da
     * digitacao.
     */
    @Query("select s from Sponsorship s where s.animal.animalId = :animalId "
            + "and s.sponsor.personId = :personId "
            + "and lower(s.description) = lower(:description) "
            + "and s.status <> br.com.petfy.healthcare.domain.entity.SponsorshipStatus.ENCERRADO")
    Optional<Sponsorship> findVivoDoPadrinho(@Param("animalId") UUID animalId,
                                             @Param("personId") UUID personId,
                                             @Param("description") String description);

    /** "O que voce banca" — a lista do padrinho, inclusive o que ja acabou. */
    @Query("select s from Sponsorship s where s.sponsor.personId = :personId "
            + "order by s.creationDate desc")
    List<Sponsorship> findDoPadrinho(@Param("personId") UUID personId);

    /**
     * Quem banca os animais desta organizacao.
     *
     * <b>Traz o {@code ENCERRAMENTO_PEDIDO} junto</b>, e e o principal: e essa a lista em que o
     * abrigo ve, com trinta dias de antecedencia, qual custo vai deixar de ser coberto. Filtrar por
     * {@code ATIVO} esconderia exatamente a informacao que a tela promete entregar.
     */
    @Query("select s from Sponsorship s where s.organization.organizationId = :organizationId "
            + "and s.status <> br.com.petfy.healthcare.domain.entity.SponsorshipStatus.ENCERRADO "
            + "order by s.creationDate desc")
    List<Sponsorship> findVivosDaOrganizacao(@Param("organizationId") UUID organizationId);

    /** Os que bancam este animal agora — para a tela publica dizer quantos padrinhos ele tem. */
    @Query("select s from Sponsorship s where s.animal.animalId = :animalId "
            + "and s.status <> br.com.petfy.healthcare.domain.entity.SponsorshipStatus.ENCERRADO")
    List<Sponsorship> findVivosDoAnimal(@Param("animalId") UUID animalId);

    /**
     * Os que passaram da data e continuam abertos — o encerramento que ninguem fechou.
     *
     * <b>Existe porque o fim e uma DATA, e nao um evento.</b> Ninguem clica no dia 30; o
     * apadrinhamento simplesmente vence. Sem esta consulta, um pedido de encerramento ficaria em
     * {@code ENCERRAMENTO_PEDIDO} para sempre, e o abrigo continuaria contando com um custo que o
     * padrinho parou de cobrir dois meses atras.
     */
    @Query("select s from Sponsorship s "
            + "where s.status = br.com.petfy.healthcare.domain.entity.SponsorshipStatus.ENCERRAMENTO_PEDIDO "
            + "and s.endsOn < :hoje")
    List<Sponsorship> findVencidosNaoFechados(@Param("hoje") LocalDate hoje);

    List<Sponsorship> findByStatusAndAnimalAnimalId(SponsorshipStatus status, UUID animalId);

    List<Sponsorship> findByAnimalAnimalIdIn(List<UUID> animalIds);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

    /** Para a exclusao de conta: a tabela aponta para {@code persons}. */
    List<Sponsorship> findBySponsorPersonId(UUID personId);

}
