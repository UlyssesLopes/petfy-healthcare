package br.com.petfy.healthcare.config;

import br.com.petfy.healthcare.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // API stateless com token no header: nao ha cookie de sessao
                // para um site terceiro reaproveitar, entao CSRF nao se aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        // recuperacao de senha e publica por definicao: quem esqueceu a
                        // senha nao tem como se autenticar para pedir a troca
                        .requestMatchers(HttpMethod.POST, "/auth/password-reset").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/password-reset/confirm").permitAll()
                        // confirmar e-mail tambem: quem clica no link pode nem ter feito
                        // login ainda, e o token do e-mail e a credencial do fluxo
                        .requestMatchers(HttpMethod.POST, "/auth/email-verification/resend").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/email-verification/confirm").permitAll()
                        // cadastro precisa ser publico, senao nao existe primeiro usuario
                        .requestMatchers(HttpMethod.POST, "/owners/include").permitAll()
                        .requestMatchers(HttpMethod.POST, "/vets/include").permitAll()
                        // carteira compartilhada: quem recebe o link nao tem conta. O
                        // token no path faz o papel da credencial
                        .requestMatchers(HttpMethod.GET, "/share/*").permitAll()
                        // o provedor de hospedagem chama o health check sem credencial
                        // para decidir se a instancia esta viva. So o health: os demais
                        // endpoints do actuator seguem exigindo token
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // o CurrentVetProvider ja barraria um tutor, mas exigir o papel
                        // aqui responde 403 em vez de 401 e evita que a autorizacao
                        // dependa so da busca falhar na tabela certa
                        .requestMatchers("/vet/**").hasRole("VET")
                        .anyRequest().authenticated())
                // sem entry point explicito o Spring Security devolve 403 para
                // quem nao esta autenticado; 401 e o correto - o cliente nao
                // esta proibido, esta sem credencial
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, ex) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
