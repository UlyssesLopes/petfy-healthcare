package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Pedidos ainda em aberto de um person, para invalidar os anteriores quando um novo e emitido. */
    List<PasswordResetToken> findByPersonPersonIdAndUsedAtIsNull(UUID personId);

    /** O mais recente, independente de estado - e ele que define se ja houve pedido demais. */
    Optional<PasswordResetToken> findFirstByPersonPersonIdOrderByCreationDateDesc(UUID personId);

    /**
     * Usado ao apagar a conta. Sem isto a chave estrangeira recusa a exclusao, e
     * como todo cadastro gera token, nenhuma conta conseguiria ser apagada.
     */
    void deleteByPersonPersonId(UUID personId);

}
