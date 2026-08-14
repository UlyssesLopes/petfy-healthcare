package br.com.petfy.healthcare.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.time.Instant;
import java.util.UUID;

/**
 * O que o token carrega sobre quem esta autenticado.
 *
 * <b>Nao carrega papel, e essa e a mudanca do P1.</b> Antes havia um claim
 * {@code role}, derivado de qual tabela o e-mail aparecia. Com pessoa unica nao
 * ha duas tabelas, e o que a pessoa alcanca decorre dos vinculos dela - custodia,
 * acesso, credencial -, nao de um campo dentro do token. Papel no token seria uma
 * copia do vinculo, e copia envelhece: credencial suspensa a meio caminho da
 * validade continuaria autorizando ato clinico ate o token expirar.
 *
 * Implementa AuthenticatedPrincipal para que o principal possa ser este objeto,
 * e nao a string do email, sem quebrar quem le authentication.getName(): o
 * Spring usa o getName() abaixo nesse caso.
 */
@Getter
@RequiredArgsConstructor
public class JwtPrincipal implements AuthenticatedPrincipal {

    private final String email;

    /**
     * Quando o token foi emitido. E o que permite recusar token anterior a uma
     * troca de senha - sem isso, trocar a senha nao expulsaria sessao alguma.
     */
    private final Instant issuedAt;

    /**
     * Qual entrada na conta emitiu este token (Tela 36).
     *
     * <b>Nulo em token anterior a V50</b>, e ele continua autenticando: quem estava logado nao e
     * deslogado por uma migracao. O que ele perde e poder ser encerrado individualmente — so a
     * troca de senha o alcanca, que era a unica ferramenta antes.
     */
    private final UUID sessionId;

    /** O token de antes da V50, e os testes que nao tem sessao a declarar. */
    public JwtPrincipal(String email, Instant issuedAt) {
        this(email, issuedAt, null);
    }

    @Override
    public String getName() {
        return email;
    }

}
