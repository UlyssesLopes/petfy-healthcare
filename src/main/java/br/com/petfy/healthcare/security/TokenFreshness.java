package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Decide se um token, embora assinado e dentro da validade, ficou para tras por
 * causa de uma troca de senha.
 *
 * Existe porque a API e stateless: nao ha sessao no servidor para encerrar, e o
 * token emitido antes da troca continuaria valendo ate expirar. Quem troca a
 * senha por suspeita de acesso indevido precisa que a outra sessao caia, e isso
 * so acontece comparando o instante de emissao com o da troca.
 *
 * A checagem fica aqui, chamada pelo filtro, e nao dentro dos providers de owner
 * e vet: assim ela vale para toda rota autenticada, inclusive as que nao sao
 * escopadas por dono e portanto nunca passam por um provider.
 */
@Component
@RequiredArgsConstructor
public class TokenFreshness {

    private final OwnerRepository ownerRepository;
    private final VetRepository vetRepository;

    public boolean isStale(JwtPrincipal principal) {
        Optional<LocalDateTime> trocaDeSenha = principal.getRole() == UserRole.VET
                ? vetRepository.findPasswordChangedAtByEmail(principal.getEmail())
                : ownerRepository.findPasswordChangedAtByEmail(principal.getEmail());

        // vazio cobre dois casos que dao no mesmo: conta que nunca trocou de
        // senha e conta que nao existe mais. Nos dois nao ha token a invalidar,
        // e quem responde pela conta removida e o provider, com 401
        if (trocaDeSenha.isEmpty()) {
            return false;
        }

        Instant instanteDaTroca = trocaDeSenha.get().atZone(ZoneId.systemDefault()).toInstant();

        // o iat do JWT tem precisao de segundo, enquanto a troca e gravada com
        // fracao de segundo. Por isso a comparacao e "nao emitido depois" em vez
        // de "emitido antes": token do mesmo segundo da troca cai fora. Errar
        // para o lado de recusar custa um login novo; errar para o outro deixa
        // viva a sessao que a troca deveria ter derrubado
        return !principal.getIssuedAt().isAfter(instanteDaTroca);
    }

}
