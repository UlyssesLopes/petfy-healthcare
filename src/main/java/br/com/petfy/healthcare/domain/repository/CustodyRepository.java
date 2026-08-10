package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Custody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustodyRepository extends JpaRepository<Custody, UUID> {

    /**
     * A custodia em curso do animal.
     *
     * Devolve Optional e nao a entidade porque o animal pode estar entre custodias
     * num instante - obito e perda encerram sem sucessor. Quem chama trata o vazio
     * como "ninguem responde por este animal agora", que e um estado legitimo.
     *
     * O indice unico parcial no banco garante que ha no maximo uma.
     */
    @Query("select c from Custody c where c.animal.animalId = :animalId and c.endedAt is null")
    Optional<Custody> findEmCurso(@Param("animalId") UUID animalId);

    @Query("select c from Custody c where c.animal.animalId = :animalId "
            + "and c.holderPerson.personId = :personId and c.endedAt is null")
    Optional<Custody> findEmCursoDaPessoa(@Param("animalId") UUID animalId,
                                          @Param("personId") UUID personId);

    /** Os animais por que a pessoa responde agora. */
    @Query("select c from Custody c where c.holderPerson.personId = :personId and c.endedAt is null")
    List<Custody> findEmCursoDaPessoa(@Param("personId") UUID personId);

    /**
     * A custodia em curso do animal quando quem responde e uma ORGANIZACAO.
     *
     * <b>Ela existe desde o P2 e nunca teve caminho HTTP.</b> O {@code holderOrganization} e
     * exclusivo com o {@code holderPerson}: um abrigo que responde por um animal resgatado nao
     * tem tutor humano, e e por isso que o modelo previu os dois. Sem esta consulta, a guarda
     * so sabia perguntar por pessoa — e nenhum membro do abrigo conseguia agir sobre o animal
     * do proprio abrigo.
     */
    @Query("select c from Custody c where c.animal.animalId = :animalId "
            + "and c.holderOrganization.organizationId = :organizationId and c.endedAt is null")
    Optional<Custody> findEmCursoDaOrganizacao(@Param("animalId") UUID animalId,
                                               @Param("organizationId") UUID organizationId);

    /**
     * Os animais sob custodia da organizacao agora — a lista do abrigo (Tela 12).
     *
     * Paginada e com busca por nome ou microchip, como a lista da clinica: um abrigo com 84
     * animais nao cabe numa tela, e o desenho ja pede o campo de busca.
     */
    @Query("select c from Custody c where c.holderOrganization.organizationId = :organizationId "
            + "and c.endedAt is null "
            + "and (lower(c.animal.name) like :busca "
            + "  or lower(coalesce(c.animal.microchipNumber, '')) like :busca)")
    Page<Custody> buscarEmCursoDaOrganizacao(@Param("organizationId") UUID organizationId,
                                             @Param("busca") String busca,
                                             Pageable pageable);

    /** A historia inteira do animal, da custodia mais antiga para a mais recente. */
    List<Custody> findByAnimalAnimalIdOrderByStartedAtAsc(UUID animalId);

    /**
     * Toda custodia da pessoa, encerrada ou nao.
     *
     * Usada na exclusao de conta: uma custodia encerrada e historia do animal, e
     * apagar a pessoa nao pode deixar a linha com um buraco sem nome - mas a linha
     * tambem nao pode segurar a exclusao por chave estrangeira.
     */
    List<Custody> findByHolderPersonPersonId(UUID personId);

    List<Custody> findByAnimalAnimalIdIn(List<UUID> animalIds);

}
