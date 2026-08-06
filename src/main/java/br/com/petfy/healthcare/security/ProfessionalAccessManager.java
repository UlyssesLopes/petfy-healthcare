package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Decide, na cadeia de filtros, se a requisicao alcanca {@code /vet/**}.
 *
 * <b>Por que existe.</b> Antes a regra era {@code hasRole("VET")}, e o papel
 * vinha do token. O papel morreu no P1 - a pessoa nao declara o que e, e o que
 * ela alcanca decorre dos vinculos. Trocar isso por {@code authenticated()} e
 * deixar a checagem so no provider perderia uma das duas camadas que o README
 * documenta como deliberadas: a autorizacao nao deve depender apenas de uma
 * busca falhar mais adiante.
 *
 * <b>Por que consulta o banco.</b> Manter a capacidade dentro do token seria
 * barato e errado: credencial tem estado, e um token emitido antes de uma
 * suspensao continuaria autorizando ato clinico ate expirar. Uma consulta por
 * requisicao de {@code /vet/**} e o preco de a suspensao valer na hora - e ela
 * conta linhas por e-mail indexado, nao carrega entidade.
 */
@Component
@RequiredArgsConstructor
public class ProfessionalAccessManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ProfessionalCredentialRepository credentialRepository;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication,
                                       RequestAuthorizationContext context) {
        Authentication auth = authentication.get();

        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return new AuthorizationDecision(false);
        }

        return new AuthorizationDecision(
                credentialRepository.existsAtivaPorEmail(auth.getName(), CredentialStatus.SUSPENSO));
    }

}
