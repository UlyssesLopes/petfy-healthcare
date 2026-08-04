package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {

    Optional<Owner> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * So a coluna, e nao o owner inteiro: isto roda no filtro, em toda requisicao
     * autenticada, e carregar a entidade completa ali seria desperdicio.
     *
     * Devolve vazio tanto para quem nunca trocou de senha quanto para email que
     * nao existe. Os dois casos dao no mesmo para quem chama: nao ha token a
     * invalidar.
     */
    @Query("select o.passwordChangedAt from Owner o where o.email = :email")
    Optional<LocalDateTime> findPasswordChangedAtByEmail(@Param("email") String email);

}
