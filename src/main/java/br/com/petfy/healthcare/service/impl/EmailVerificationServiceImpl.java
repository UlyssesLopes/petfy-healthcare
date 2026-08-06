package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.EmailVerificationConfirmDTO;
import br.com.petfy.healthcare.domain.dto.EmailVerificationResendDTO;
import br.com.petfy.healthcare.domain.entity.EmailVerificationToken;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.EmailVerificationTokenRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.Notification;
import br.com.petfy.healthcare.notification.Notifier;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.EmailVerificationService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final PersonRepository personRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final OpaqueTokenService opaqueTokenService;
    private final Notifier notifier;

    @Value("${petfy.email-verification.expiration-hours:24}")
    private long expirationHours;

    @Value("${petfy.email-verification.cooldown-minutes:5}")
    private long cooldownMinutes;

    /**
     * Falha de envio nao desfaz o cadastro. E a mesma escolha do aviso de vacina
     * registrada: o efeito principal e a conta criada, e perde-la porque o canal
     * caiu seria trocar um problema pequeno - reenviar a confirmacao - por um
     * grande. O reenvio existe justamente para cobrir esse caso.
     */
    @Override
    public void sendVerification(Person person) {
        try {
            emitirEEnviar(person, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Falha ao enviar a confirmacao de e-mail do person {}", person.getPersonId(), e);
        }
    }

    /**
     * Termina em silencio para e-mail sem conta, para conta ja verificada e para
     * pedido dentro do cooldown. Mesmo motivo da recuperacao de senha: distinguir
     * transformaria um endpoint publico em verificador de quem tem cadastro aqui.
     */
    @Override
    @Transactional
    public void resend(EmailVerificationResendDTO request) {
        Optional<Person> talvezPerson = personRepository.findByEmail(request.getEmail());

        if (talvezPerson.isEmpty()) {
            log.info("Reenvio de confirmacao para e-mail sem conta; respondendo igual a um pedido valido");
            return;
        }

        Person person = talvezPerson.get();
        LocalDateTime agora = LocalDateTime.now();

        if (person.podeReceberNotificacao()) {
            log.info("Reenvio pedido para o person {}, que ja tinha verificado o e-mail", person.getPersonId());
            return;
        }

        if (dentroDoCooldown(person, agora)) {
            log.info("Reenvio dentro do cooldown para o person {}; nada enviado", person.getPersonId());
            return;
        }

        // falha de envio nao pode virar erro na resposta. Este endpoint responde
        // igual exista ou nao a conta, e um 500 aqui contra um 202 para e-mail
        // desconhecido entregaria exatamente a informacao que o silencio esconde
        try {
            emitirEEnviar(person, agora);
        } catch (Exception e) {
            log.error("Falha ao reenviar a confirmacao de e-mail do person {}", person.getPersonId(), e);
        }
    }

    @Override
    @Transactional
    public void confirm(EmailVerificationConfirmDTO request) {
        LocalDateTime agora = LocalDateTime.now();

        EmailVerificationToken token = tokenRepository.findByTokenHash(opaqueTokenService.hash(request.getToken()))
                .filter(candidato -> candidato.isUsable(agora))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VERIFICATION_TOKEN_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VERIFICATION_TOKEN_NOT_FOUND.getCode(),
                        HttpStatus.BAD_REQUEST));

        Person person = token.getPerson();
        person.setEmailVerifiedAt(agora);
        person.setUpdateDate(agora);
        personRepository.save(person);

        token.setUsedAt(agora);
        tokenRepository.save(token);

        log.info("E-mail confirmado para o person {}", person.getPersonId());
    }

    private void emitirEEnviar(Person person, LocalDateTime agora) {
        // pedir de novo invalida o anterior, como na recuperacao de senha: dois
        // links vivos so aumentam a superficie sem ajudar quem pediu
        List<EmailVerificationToken> emAberto = tokenRepository.findByPersonPersonIdAndUsedAtIsNull(person.getPersonId());
        emAberto.forEach(anterior -> anterior.setUsedAt(agora));
        tokenRepository.saveAll(emAberto);

        String token = opaqueTokenService.generate();

        tokenRepository.save(EmailVerificationToken.builder()
                .person(person)
                .tokenHash(opaqueTokenService.hash(token))
                .expiresAt(agora.plusHours(expirationHours))
                .creationDate(agora)
                .build());

        notifier.send(mensagem(person, token));
    }

    private boolean dentroDoCooldown(Person person, LocalDateTime agora) {
        return tokenRepository.findFirstByPersonPersonIdOrderByCreationDateDesc(person.getPersonId())
                .map(ultimo -> ultimo.getCreationDate().plusMinutes(cooldownMinutes).isAfter(agora))
                .orElse(false);
    }

    /**
     * A validade e longa - 24 horas contra os 30 minutos da recuperacao de senha
     * - porque o que esta em jogo e diferente: este token confirma um endereco,
     * aquele troca a senha da conta.
     */
    private Notification mensagem(Person person, String token) {
        return Notification.builder()
                .toEmail(person.getEmail())
                .toName(person.getName())
                .subject("Confirme seu e-mail - Petfy")
                .lines(List.of(
                        "Falta confirmar este endereco para voce receber os lembretes de vacina do seu animal.",
                        "Codigo: " + token,
                        "Ele vale por " + expirationHours + " horas.",
                        "Ate confirmar, sua conta funciona normalmente, mas nao enviamos nenhum aviso."))
                .build();
    }

}
