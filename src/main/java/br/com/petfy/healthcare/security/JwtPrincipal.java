package br.com.petfy.healthcare.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.time.Instant;

/**
 * O que o token carrega sobre quem esta autenticado.
 *
 * Implementa AuthenticatedPrincipal para que o principal possa ser este objeto,
 * e nao a string do email, sem quebrar quem le authentication.getName(): o
 * Spring usa o getName() abaixo nesse caso. Foi o que permitiu carregar o
 * issuedAt ate o filtro sem mexer nos CurrentOwnerProvider e CurrentVetProvider.
 */
@Getter
@RequiredArgsConstructor
public class JwtPrincipal implements AuthenticatedPrincipal {

    private final String email;

    private final UserRole role;

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
