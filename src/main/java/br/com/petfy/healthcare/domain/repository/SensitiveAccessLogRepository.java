package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.SensitiveAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensitiveAccessLogRepository extends JpaRepository<SensitiveAccessLog, UUID> {

    /**
     * Paginado desde o inicio, e nao lista: este log cresce sem teto - uma clinica
     * que acompanha um pet cronico gera acesso toda semana, por anos. Devolver tudo
     * seria a consulta que derruba a resposta no dia em que o produto der certo.
     */
    Page<SensitiveAccessLog> findByPetPetIdOrderByAccessedAtDesc(UUID petId, Pageable pageable);

    /** Usado ao apagar o pet - ver {@code PetPurger}. */
    void deleteByPetPetIdIn(List<UUID> petIds);

}
