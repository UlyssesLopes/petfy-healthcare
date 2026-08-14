package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.PersonSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    /**
     * A entrada que apresentou este refresh.
     *
     * <b>Busca pelo HASH</b>, e nunca pelo token: o que chega no cookie e o segredo, e o que esta
     * guardado e o resumo dele. Nao encontrar cobre tudo que da no mesmo — cookie de sessao antiga,
     * token ja rotacionado, e token inventado.
     */
    Optional<PersonSession> findByRefreshTokenHash(String refreshTokenHash);

    /** Se esta pessoa ja entrou alguma vez. O primeiro login de todos nao avisa ninguem. */
    boolean existsByPersonPersonId(UUID personId);

    /**
     * Se ja houve entrada deste mesmo aparelho — a pergunta do aviso da Tela 28.
     *
     * <b>Encerrada conta como conhecida.</b> Quem encerrou a entrada do proprio celular e entrou
     * de novo dele nao esta diante de um aparelho novo, e receber um alerta de seguranca por isso
     * ensinaria a pessoa a ignorar o alerta.
     *
     * <b>O nulo precisa do `is null` explicito:</b> em SQL, `user_agent = null` nunca e verdade, e
     * a comparacao direta faria todo cliente sem User-Agent parecer um aparelho novo a cada login.
     */
    @Query("select count(s) > 0 from PersonSession s where s.person.personId = :personId "
            + "and ((:userAgent is null and s.userAgent is null) or s.userAgent = :userAgent)")
    boolean conheceOAparelho(@Param("personId") UUID personId,
                             @Param("userAgent") String userAgent);

    /**
     * Encerra TODAS as entradas de uma pessoa — o que a troca de senha faz.
     *
     * <b>"Trocar a senha derruba todas de uma vez" deixou de ser so uma frase da tela.</b> Ate a
     * V51 a troca invalidava os tokens por `iat`, e as sessoes continuavam listadas como abertas na
     * Tela 36: a lista mostrava aberto o que ja nao autenticava. Agora ela conta a verdade.
     *
     * <b>E o refresh sai junto</b>: sem isso o navegador pegaria um token novo depois da troca, e a
     * tranca que a pessoa acionou seria contornada pelo mecanismo que existe para ela nao digitar a
     * senha de novo.
     */
    @Modifying
    @Query("update PersonSession s set s.revokedAt = :agora, s.refreshTokenHash = null, "
            + "s.refreshExpiresAt = null "
            + "where s.person.personId = :personId and s.revokedAt is null")
    int encerrarTodasDaPessoa(@Param("personId") UUID personId, @Param("agora") LocalDateTime agora);

}
