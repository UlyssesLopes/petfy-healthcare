package br.com.petfy.healthcare.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final TokenFreshness tokenFreshness;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFIXO)) {
            String token = header.substring(PREFIXO.length());

            // token invalido nao rejeita a requisicao aqui: apenas nao autentica.
            // Quem decide se a rota exige autenticacao e a SecurityFilterChain,
            // entao um token ruim em rota publica continua passando
            jwtService.extractPrincipal(token).ifPresent(principal -> {

                // assinatura e validade nao bastam: um token emitido antes de uma
                // troca de senha continua intacto, mas nao deve mais autenticar
                if (tokenFreshness.isStale(principal)) {
                    return;
                }

                var authorities = List.of(new SimpleGrantedAuthority(principal.getRole().asAuthority()));

                // o principal e o objeto, e nao a string do email, para o issuedAt
                // continuar disponivel adiante. Quem le authentication.getName()
                // nao percebe diferenca: JwtPrincipal e um AuthenticatedPrincipal
                // e devolve o email nesse metodo
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }

}
