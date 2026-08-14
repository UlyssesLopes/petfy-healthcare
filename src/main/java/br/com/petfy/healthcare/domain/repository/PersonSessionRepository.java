package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PersonSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PersonSessionRepository extends JpaRepository<PersonSession, UUID> {

    List<PersonSession> findByPersonPersonIdOrderByCreatedAtDesc(UUID personId);

    /**
     * Se esta sessao foi encerrada — a pergunta do filtro, e ela roda em toda requisicao
     * autenticada.
     *
     * <b>Devolve o instante, e nao a entidade:</b> carregar a `PersonSession` inteira arrastaria a
     * `Person` associada em toda requisicao do produto, para responder se uma coluna e nula.
     */
    @Query("select s.revokedAt is not null from PersonSession s where s.personSessionId = :id")
    Boolean estaEncerrada(@Param("id") UUID id);

}
