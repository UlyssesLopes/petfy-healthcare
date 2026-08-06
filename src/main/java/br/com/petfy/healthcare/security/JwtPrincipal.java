package br.com.petfy.healthcare.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.time.Instant;

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

    @Override
    public String getName() {
        return email;
    }

}
