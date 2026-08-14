package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PersonNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PersonNotificationRepository extends JpaRepository<PersonNotification, UUID> {

    Page<PersonNotification> findByPersonPersonIdOrderByCreatedAtDesc(UUID personId, Pageable pageable);

    long countByPersonPersonIdAndReadAtIsNull(UUID personId);

    /**
     * "Marcar todos como lidos" e uma escrita so, e nao N.
     *
     * Quem volta depois de duas semanas tem dezenas de avisos, e carregar todos em memoria para
     * gravar um campo em cada seria pagar a leitura inteira para nao ler nada.
     */
    @Modifying
    @Query("update PersonNotification n set n.readAt = :agora "
            + "where n.person.personId = :personId and n.readAt is null")
    int marcarTodosComoLidos(@Param("personId") UUID personId, @Param("agora") LocalDateTime agora);

}
