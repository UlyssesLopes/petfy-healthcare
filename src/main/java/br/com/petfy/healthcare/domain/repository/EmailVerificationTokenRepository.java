package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findByOwnerOwnerIdAndUsedAtIsNull(UUID ownerId);

    Optional<EmailVerificationToken> findFirstByOwnerOwnerIdOrderByCreationDateDesc(UUID ownerId);

}
