package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.config.PetfyMetrics;
import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonSession;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PersonSessionRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PersonRepository personRepository;
    private final ProfessionalCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PersonSessionRepository personSessionRepository;
    private final br.com.petfy.healthcare.security.OpaqueTokenService opaqueTokenService;
    private final br.com.petfy.healthcare.security.RefreshCookie refreshCookie;
    private final PetfyMetrics petfyMetrics;

    /**
     * Uma busca so, numa tabela so.
     *
     * <b>O que sumiu daqui.</b> Antes o login procurava em {@code owners} e, se
     * nao achasse, em {@code vets}, e o papel saia de qual das duas respondeu.
     * Isso obrigava o cadastro a conferir o e-mail nos dois lados para o login nao
     * ficar ambiguo - checagem que o banco nao garantia e que dependia de ninguem
     * esquecer. Com pessoa unica a unicidade e do banco, e o papel nao existe.
     *
     * <b>O que a resposta diz no lugar do papel.</b> {@code professional} nao e um
     * papel disfarcado: e o estado atual da credencial, lido agora, e serve para o
     * cliente saber se oferece a area profissional. Se a credencial for suspensa
     * amanha, a resposta de amanha muda - o que nao aconteceria com um papel
     * carimbado dentro do token.
     */
    @Override
    public Autenticada login(LoginRequestDTO request, String userAgent) {
        Optional<Person> encontrado = personRepository.findByEmail(request.getEmail());

        // a mesma resposta para e-mail inexistente e para senha errada: distinguir
        // os dois casos entregaria de graca quais e-mails estao cadastrados
        Optional<Person> validas = encontrado
                .filter(p -> passwordEncoder.matches(request.getPassword(), p.getPassword()));

        if (validas.isEmpty()) {
            petfyMetrics.loginAttempt("failure");
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                    ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                    HttpStatus.UNAUTHORIZED);
        }

        Person person = validas.get();
        petfyMetrics.loginAttempt("success");

        /*
         * A ENTRADA FICA REGISTRADA, e e o que a Tela 36 pedia desde o bloco 9.
         *
         * A sessao e gravada ANTES do token porque o `sid` dela entra dentro dele: e a linha no
         * banco que passa a poder ser encerrada, e o token so aponta para ela.
         *
         * <b>O user agent e o unico rotulo, e nao ha IP.</b> "189.4.x.x" nao ajuda ninguem a
         * reconhecer o proprio aparelho, e guardar por onde alguem entra e o que a Tela 34 recusa
         * quando diz "nao guardamos quem fez a busca, e por onde".
         *
         * <b>E DESDE A V51 O REFRESH NASCE JUNTO</b> — e a mesma entrada vista pelo outro lado:
         * enquanto ela vale, o navegador troca um JWT vencido por um novo sem pedir a senha. E o que
         * faz a sessao sobreviver a recarga da pagina.
         *
         * Guardamos o HASH, e nao o token — mesmo criterio do convite e da recuperacao de senha:
         * quem le o banco nao pode sair usando as sessoes de ninguem. O segredo existe fora do
         * servidor uma vez so, no cookie que a resposta leva.
         */
        String refresh = opaqueTokenService.generate();
        LocalDateTime agora = LocalDateTime.now();

        PersonSession sessao = personSessionRepository.save(PersonSession.builder()
                .person(person)
                .createdAt(agora)
                .userAgent(recortar(userAgent))
                .refreshTokenHash(opaqueTokenService.hash(refresh))
                .refreshExpiresAt(agora.plus(refreshCookie.getValidade()))
                .build());

        LoginResponseDTO corpo = LoginResponseDTO.builder()
                .token(jwtService.generateToken(person.getEmail(), person.getPersonId(),
                        sessao.getPersonSessionId()))
                .tokenType("Bearer")
                .expiresInMinutes(jwtService.getExpirationMinutes())
                .personId(person.getPersonId())
                .professional(credentialRepository.existsAtivaPorEmail(
                        person.getEmail(), CredentialStatus.SUSPENSO))
                .build();

        return new Autenticada(corpo, refresh);
    }

    @Override
    public boolean temCredencialAtiva(String email) {
        return credentialRepository.existsAtivaPorEmail(email, CredentialStatus.SUSPENSO);
    }

    /**
     * O user agent cabe em 400, e um mais comprido e recortado em vez de recusado.
     *
     * Nenhum navegador chega perto disso; quem chega e coletor automatizado. Recusar o login por
     * causa do tamanho de um ROTULO seria trocar o essencial pelo acessorio.
     */
    private String recortar(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }

        return userAgent.length() <= 400 ? userAgent : userAgent.substring(0, 400);
    }

}
