package br.com.petfy.healthcare.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** O que o token carrega sobre quem esta autenticado. */
@Getter
@RequiredArgsConstructor
public class JwtPrincipal {

    private final String email;

    private final UserRole role;

}
