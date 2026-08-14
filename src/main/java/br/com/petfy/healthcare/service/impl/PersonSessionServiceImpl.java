package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PersonSessionResponseDTO;
import br.com.petfy.healthcare.domain.entity.PersonSession;
import br.com.petfy.healthcare.domain.repository.PersonSessionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.JwtPrincipal;
import br.com.petfy.healthcare.service.PersonSessionService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonSessionServiceImpl implements PersonSessionService {

    private final PersonSessionRepository personSessionRepository;
    private final CurrentPersonProvider currentPersonProvider;

    @Override
    @Transactional(readOnly = true)
    public List<PersonSessionResponseDTO> listMine() {
        UUID eu = currentPersonProvider.require().getPersonId();
        UUID atual = sessaoAtual();

        return personSessionRepository.findByPersonPersonIdOrderByCreatedAtDesc(eu).stream()
                .map(sessao -> toResponse(sessao, atual))
                .toList();
    }

    /**
     * <b>Encerrar nao apaga.</b> A linha fica com {@code revokedAt} preenchido, e a razao e a mesma
     * de a revogacao de acesso nao apagar o que a organizacao registrou: quem encerra uma sessao por
     * suspeita de acesso indevido tem interesse em que o registro de que ela existiu permaneca.
     *
     * <b>Encerrar de novo nao mexe na data.</b> "Quando ela caiu" e a primeira vez.
     */
    @Override
    @Transactional
    public void revoke(UUID personSessionId) {
        UUID eu = currentPersonProvider.require().getPersonId();

        PersonSession sessao = personSessionRepository.findById(personSessionId)
                // sessao de outra pessoa nao existe para mim: confirmar que ela existe ja diria que
                // aquela conta esta em uso
                .filter(candidata -> candidata.getPerson().getPersonId().equals(eu))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.SESSION_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.SESSION_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        if (sessao.vigente()) {
            sessao.setRevokedAt(LocalDateTime.now());
            personSessionRepository.save(sessao);
        }
    }

    /**
     * De qual sessao esta chegando esta requisicao.
     *
     * <b>Nula quando o token e anterior a V50</b> — e ai nenhuma linha aparece como "esta" —, e
     * tambem nos testes que autenticam com a string do e-mail em vez do principal. Nos dois casos a
     * lista continua correta: o que se perde e so a marca de "voce esta aqui".
     */
    private UUID sessaoAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof JwtPrincipal principal)) {
            return null;
        }

        return principal.getSessionId();
    }

    private PersonSessionResponseDTO toResponse(PersonSession sessao, UUID atual) {
        return PersonSessionResponseDTO.builder()
                .personSessionId(sessao.getPersonSessionId())
                .createdAt(sessao.getCreatedAt())
                .revokedAt(sessao.getRevokedAt())
                .userAgent(sessao.getUserAgent())
                .current(atual != null && atual.equals(sessao.getPersonSessionId()))
                .build();
    }

}
