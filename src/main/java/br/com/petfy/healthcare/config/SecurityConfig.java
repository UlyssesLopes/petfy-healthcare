package br.com.petfy.healthcare.config;

import br.com.petfy.healthcare.security.JwtAuthenticationFilter;
import br.com.petfy.healthcare.security.ProfessionalAccessManager;
import br.com.petfy.healthcare.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RateLimitFilter rateLimitFilter,
                                                   ProfessionalAccessManager professionalAccessManager)
            throws Exception {
        http
                // O CorsFilter entra aqui, e a posicao dele importa: ele roda antes
                // do jwtAuthenticationFilter, entao o preflight (OPTIONS) e respondido
                // e encerrado ali. Se chegasse ate a autorizacao, tomaria 401 - o
                // navegador nunca manda credencial no preflight -, e o efeito seria a
                // requisicao de verdade nem sair. As origens vem do CorsConfig, por perfil
                .cors(Customizer.withDefaults())
                // API stateless com token no header: nao ha cookie de sessao
                // para um site terceiro reaproveitar, entao CSRF nao se aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        // A lista mora no RotasPublicas, e nao aqui, porque o contrato
                        // OpenAPI precisa declarar as mesmas rotas como publicas. Duas
                        // listas escritas a mao divergiriam, e a que perde e sempre a
                        // documentacao - foi o que aconteceu com o README antes do passo 4
                        Stream.concat(RotasPublicas.DE_API.stream(), RotasPublicas.DE_INFRA.stream())
                                .forEach(rota -> {
                                    if (rota.metodo() == null) {
                                        auth.requestMatchers(rota.padrao()).permitAll();
                                    } else {
                                        auth.requestMatchers(rota.metodo(), rota.padrao()).permitAll();
                                    }
                                });

                        auth
                        // era hasRole("VET") em /vet/**, com o papel vindo do token. O papel morreu
                        // no P1, e o espaco /vet/** morreu no P3b: nao ha area de um tipo de
                        // conta, ha operacao que exige credencial. Quem alcanca e quem tem
                        // credencial profissional
                        // ativa, conferida no banco a cada requisicao. O
                        // CurrentProfessionalProvider repete a checagem mais adiante, e as
                        // duas camadas seguem sendo deliberadas
                        .requestMatchers("/professional/**", "/organizations/invites/**").access(professionalAccessManager)
                        .anyRequest().authenticated();
                })
                // sem entry point explicito o Spring Security devolve 403 para
                // quem nao esta autenticado; 401 e o correto - o cliente nao
                // esta proibido, esta sem credencial
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, ex) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                // rateLimitFilter roda antes do JWT: barramos flood antes de
                // gastar validacao de token
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
