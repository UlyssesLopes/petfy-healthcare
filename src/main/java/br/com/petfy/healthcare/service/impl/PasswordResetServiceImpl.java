package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PasswordResetConfirmDTO;
import br.com.petfy.healthcare.domain.dto.PasswordResetRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PasswordResetToken;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PasswordResetTokenRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.Notification;
import br.com.petfy.healthcare.notification.Notifier;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.PasswordResetService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PersonRepository personRepository;
    private final br.com.petfy.healthcare.domain.repository.PersonSessionRepository personSessionRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final OpaqueTokenService opaqueTokenService;
    private final PasswordEncoder passwordEncoder;
    private final Notifier notifier;

    @Value("${petfy.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${petfy.password-reset.cooldown-minutes:5}")
    private long cooldownMinutes;

    /**
     * O metodo termina em silencio em tres situacoes diferentes - e-mail que nao
     * existe, pedido dentro do cooldown e pedido atendido - e o cliente nao tem
     * como distinguir.
     *
     * E de proposito. Responder "esse e-mail nao esta cadastrado" transformaria o
     * endpoint, que e publico, num verificador de quem tem conta aqui. Para uma
     * base de tutores de animal isso ja e exposicao; combinado com o vazamento de
     * outro servico, vira lista de alvos.
     */
    @Override
    @Transactional
    public void requestReset(PasswordResetRequestDTO request) {
        Optional<Person> talvezPerson = personRepository.findByEmail(request.getEmail());

        if (talvezPerson.isEmpty()) {
            log.info("Pedido de recuperacao para e-mail sem conta; respondendo igual a um pedido valido");
            return;
        }

        Person person = talvezPerson.get();
        LocalDateTime agora = LocalDateTime.now();

        if (dentroDoCooldown(person, agora)) {
            log.info("Pedido de recuperacao dentro do cooldown para a pessoa {}; nada enviado", person.getPersonId());
            return;
        }

        // pedir de novo invalida o pedido anterior: dois links vivos ao mesmo
        // tempo dobram a janela de quem interceptou o e-mail antigo, sem dar nada
        // em troca a quem esqueceu a senha
        List<PasswordResetToken> emAberto = tokenRepository.findByPersonPersonIdAndUsedAtIsNull(person.getPersonId());
        emAberto.forEach(anterior -> anterior.setUsedAt(agora));
        tokenRepository.saveAll(emAberto);

        String token = opaqueTokenService.generate();

        tokenRepository.save(PasswordResetToken.builder()
                .person(person)
                .tokenHash(opaqueTokenService.hash(token))
                .expiresAt(agora.plusMinutes(expirationMinutes))
                .creationDate(agora)
                .build());

        // unico momento em que o token existe fora do e-mail.
        //
        // Falha de envio nao pode virar erro na resposta: este endpoint responde
        // igual exista ou nao a conta, e um 500 aqui contra o 202 de um e-mail
        // desconhecido entregaria exatamente a informacao que o silencio esconde.
        // Quem pediu tenta de novo depois do cooldown
        try {
            notifier.send(mensagem(person, token));
        } catch (Exception e) {
            log.error("Falha ao enviar a recuperacao de senha da pessoa {}", person.getPersonId(), e);
        }
    }

    /**
     * Falha de token nao distingue inexistente, expirado e ja usado: quem tem um
     * link velho na mao nao precisa saber qual dos tres, e distinguir contaria a
     * quem interceptou o e-mail que aquele endereco tem conta.
     */
    @Override
    @Transactional
    public void confirmReset(PasswordResetConfirmDTO request) {
        LocalDateTime agora = LocalDateTime.now();

        PasswordResetToken token = tokenRepository.findByTokenHash(opaqueTokenService.hash(request.getToken()))
                .filter(candidato -> candidato.isUsable(agora))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.RESET_TOKEN_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.RESET_TOKEN_NOT_FOUND.getCode(),
                        HttpStatus.BAD_REQUEST));

        Person person = token.getPerson();
        person.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // derruba as sessoes abertas. Aqui importa ainda mais que na troca comum:
        // se a conta foi tomada, quem esta recuperando precisa expulsar quem
        // entrou, e nao apenas voltar a conseguir entrar junto
        person.setPasswordChangedAt(agora);
        person.setUpdateDate(agora);
        personRepository.save(person);

        /*
         * E ENCERRA AS ENTRADAS, e nao so invalida os tokens (V51).
         *
         * Aqui isto pesa mais do que na troca comum, pela razao que ja estava escrita acima: se a
         * conta foi tomada, quem recupera precisa EXPULSAR quem entrou. Sem esta linha, o navegador
         * do invasor renovaria o token com o refresh que ninguem invalidou — e a recuperacao de
         * senha teria devolvido o acesso sem tirar o dele.
         */
        personSessionRepository.encerrarTodasDaPessoa(person.getPersonId(), agora);

        token.setUsedAt(agora);
        tokenRepository.save(token);
    }

    private boolean dentroDoCooldown(Person person, LocalDateTime agora) {
        return tokenRepository.findFirstByPersonPersonIdOrderByCreationDateDesc(person.getPersonId())
                .map(ultimo -> ultimo.getCreationDate().plusMinutes(cooldownMinutes).isAfter(agora))
                .orElse(false);
    }

    /**
     * O texto vai com o token, e nao com um link: nao existe cliente web ainda, e
     * inventar uma URL agora seria fixar um endereco que ninguem serve. Quando o
     * cliente existir, e aqui que o link e montado.
     */
    private Notification mensagem(Person person, String token) {
        return Notification.builder()
                .toEmail(person.getEmail())
                .toName(person.getName())
                .subject("Recuperacao de senha - Petfy")
                .lines(List.of(
                        "Recebemos um pedido para redefinir a sua senha.",
                        "Codigo: " + token,
                        "Ele vale por " + expirationMinutes + " minutos e so pode ser usado uma vez.",
                        "Se nao foi voce quem pediu, ignore esta mensagem: sua senha continua a mesma."))
                .build();
    }

}
