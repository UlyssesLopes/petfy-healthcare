package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Vaccine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VaccineRepository extends JpaRepository<Vaccine, UUID> {

    /**
     * As vacinas dos animais que a pessoa alcanca - por custodia ou por concessao.
     *
     * Deixou de ser derivada no P2b: derivada nao faz OR entre duas associacoes. Ver
     * o javadoc de {@code AnimalRepository.findAlcancadosPor}, que explica tambem por
     * que sao dois {@code exists} e nao um join.
     */
    @Query(ALCANCADOS_POR)
    List<Vaccine> findAlcancadasPor(@Param("personId") UUID personId,
                                    @Param("agora") LocalDateTime agora);

    /** Usado pela listagem paginada do controller. */
    @Query(ALCANCADOS_POR)
    Page<Vaccine> findAlcancadasPor(@Param("personId") UUID personId,
                                    @Param("agora") LocalDateTime agora,
                                    Pageable pageable);

    String ALCANCADOS_POR = "select v from Vaccine v where exists ("
            + "  select 1 from Custody c where c.animal = v.animal "
            + "    and c.holderPerson.personId = :personId and c.endedAt is null) "
            + "or exists ("
            + "  select 1 from Grant g where g.animal = v.animal "
            + "    and g.granteePerson.personId = :personId and g.revokedAt is null "
            + "    and (g.expiresAt is null or g.expiresAt > :agora))";

    /**
     * Usado pela rotina de lembretes, que varre a base inteira e nao um tutor.
     * O filtro por ultimo envio fica no service: aqui a data da proxima dose ja
     * corta a maior parte, e deixar o resto em memoria mantem a regra de
     * reenvio testavel sem banco.
     */
    List<Vaccine> findByNextDoseDateLessThanEqual(LocalDate limite);

    /**
     * As doses do animal naquele dia — a lista curta em que a recusa de dose duplicada procura.
     *
     * A comparacao de QUAL vacina e (catalogo, com queda para nome) fica em Java de proposito: e
     * a mesma regra da comprovacao da creche, e escreve-la em duas linguagens seria a garantia de
     * as duas divergirem. Aqui o banco so faz o recorte barato.
     */
    List<Vaccine> findByAnimalAnimalIdAndApplicationDate(UUID animalId, LocalDate applicationDate);

    List<Vaccine> findByAnimalAnimalIdOrderByApplicationDateDesc(UUID animalId);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
