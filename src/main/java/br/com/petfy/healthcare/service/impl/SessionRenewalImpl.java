package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonSession;
import br.com.petfy.healthcare.domain.repository.PersonSessionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.security.RefreshCookie;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.SessionRenewal;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRenewalImpl implements SessionRenewal {

    private final PersonSessionRepository personSessionRepository;
    private final OpaqueTokenService opaqueTokenService;
    private final JwtService jwtService;
    private final RefreshCookie refreshCookie;

    @Override
    @Transactional
    public Renovada renovar(String refreshToken) {
        LocalDateTime agora = LocalDateTime.now();

        PersonSession sessao = personSessionRepository
                .findByRefreshTokenHash(opaqueTokenService.hash(refreshToken))
                .filter(candidata -> candidata.podeRenovar(agora))
                .orElseThrow(this::recusar);

        Person pessoa = sessao.getPerson();

        /*
         * A TROCA DE SENHA TAMBEM DERRUBA A RENOVACAO, e nao so o token.
         *
         * Sem isto, quem trocou a senha por suspeita de acesso indevido veria o token do invasor
         * cair na proxima requisicao — e o navegador dele pegar um token novo em seguida, com o
         * refresh que ninguem invalidou. A trava que a pessoa acionou seria contornada pelo proprio
         * mecanismo que existe para ela nao ter de digitar a senha de novo.
         *
         * <b>Esta e a segunda tranca, e nao a primeira.</b> A troca de senha ENCERRA as sessoes no
         * momento em que acontece — e ali, e nao aqui, que a lista da Tela 36 passa a dizer a
         * verdade. Este bloco cobre a sessao que escapou por um segundo de diferenca, e por isso ele
         * so RECUSA: revogar daqui seria escrever dentro de uma transacao que esta prestes a lancar,
         * e o rollback desfaria a revogacao — uma tranca que parece existir e nao existe.
         */
        if (pessoa.getPasswordChangedAt() != null
                && !sessao.getCreatedAt().isAfter(pessoa.getPasswordChangedAt())) {
            log.info("Sessao {} e anterior a troca de senha; renovacao recusada",
                    sessao.getPersonSessionId());
            throw recusar();
        }

        /*
         * ROTACIONA: o refresh apresentado morre aqui, e o cookie leva um novo.
         *
         * Um cookie copiado deixa de valer no primeiro refresh legitimo que o dono fizer — o
         * intruso fica com um papel sem valor em vez de acesso permanente. E o prazo tambem anda:
         * quem usa o produto nao e deslogado por ter usado.
         */
        String novoRefresh = opaqueTokenService.generate();

        sessao.setRefreshTokenHash(opaqueTokenService.hash(novoRefresh));
        sessao.setRefreshExpiresAt(agora.plus(refreshCookie.getValidade()));
        personSessionRepository.save(sessao);

        return new Renovada(
                jwtService.generateToken(pessoa.getEmail(), pessoa.getPersonId(),
                        sessao.getPersonSessionId()),
                novoRefresh,
                jwtService.getExpirationMinutes(),
                pessoa,
                sessao);
    }

    /**
     * Sair encerra a entrada, e nao so joga o token fora.
     *
     * <b>Sem refresh nao ha o que encerrar, e sair mesmo assim da certo:</b> quem entrou antes da
     * V51 nao tem cookie, e continua saindo do jeito que sempre saiu — o cliente esquece o token.
     * Recusar aqui faria "sair" falhar para essas pessoas.
     */
    @Override
    @Transactional
    public void sair(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        personSessionRepository.findByRefreshTokenHash(opaqueTokenService.hash(refreshToken))
                .ifPresent(sessao -> encerrar(sessao, LocalDateTime.now()));
    }

    /**
     * Encerrar apaga o hash junto.
     *
     * A sessao encerrada ja nao renovaria — o `podeRenovar` cobre isso —, mas deixar o hash guardado
     * manteria um segredo vivo no banco sem nenhum uso. O que fica e o registro de que a entrada
     * existiu, que e o que a Tela 36 mostra.
     */
    private void encerrar(PersonSession sessao, LocalDateTime agora) {
        if (sessao.vigente()) {
            sessao.setRevokedAt(agora);
        }

        sessao.setRefreshTokenHash(null);
        sessao.setRefreshExpiresAt(null);
        personSessionRepository.save(sessao);
    }

    private PetfyHealthcareException recusar() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                HttpStatus.UNAUTHORIZED);
    }

}
