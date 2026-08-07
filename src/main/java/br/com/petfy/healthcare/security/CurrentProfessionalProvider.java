package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A pessoa da requisicao em andamento, exigindo credencial profissional ativa.
 *
 * Substitui o CurrentVetProvider. A diferenca nao e de nome: antes um tutor
 * simplesmente nao era encontrado, porque a busca era na tabela {@code vets} e o
 * e-mail dele nao estava la. Com pessoa unica todo mundo e encontrado, entao o
 * que separa quem pratica ato clinico de quem nao pratica passa a ser a
 * credencial - que e o que 5.10 diz governar isso.
 *
 * <b>403 e nao 404.</b> A pessoa esta autenticada e sabe que existe; o que falta
 * e capacidade. Nao ha nada a esconder dela sobre a propria conta.
 */
@Component
@RequiredArgsConstructor
public class CurrentProfessionalProvider {

    /**
     * Como o cliente escolhe em nome de quem esta agindo, quando tem mais de um
     * vinculo.
     *
     * Header, e nao parametro de query: contexto vale para a requisicao inteira, e
     * repeti-lo em cada endpoint faria cada um lembrar de propaga-lo. E header e o
     * lugar em que um cliente configura isso uma vez, no interceptor.
     */
    public static final String HEADER_ORGANIZACAO = "X-Petfy-Organization";

    private final PersonRepository personRepository;
    private final ProfessionalCredentialRepository credentialRepository;
    private final MembershipRepository membershipRepository;

    /**
     * A pessoa e em nome de quem ela age nesta requisicao.
     *
     * <b>Tres desfechos, e nenhum deles e "escolhe o primeiro".</b> Sem vinculo, o
     * contexto e a propria pessoa - o autonomo. Com um vinculo, e ele. Com mais de um
     * e sem o header, responde 409: escolher em silencio faria um ato clinico sair
     * assinado por uma organizacao que a pessoa nao pretendia, e assinatura nao se
     * corrige depois.
     */
    public ProfessionalContext requireContext() {
        Person person = require();

        List<Membership> ativos = membershipRepository.findAtivosDaPessoa(person.getPersonId());

        String escolhido = organizacaoPedida();

        if (escolhido != null) {
            UUID organizationId = parseOrganizationId(escolhido);

            Membership vinculo = membershipRepository
                    .findAtivoDaPessoaNaOrganizacao(person.getPersonId(), organizationId)
                    .orElseThrow(() -> new PetfyHealthcareException(
                            ErrorMessageEnum.NOT_ORGANIZATION_MEMBER.getMessage(),
                            ErrorMessageEnum.NOT_ORGANIZATION_MEMBER.getCode(),
                            HttpStatus.FORBIDDEN));

            return new ProfessionalContext(person, vinculo.getOrganization());
        }

        if (ativos.isEmpty()) {
            // atuando por si: e o veterinario autonomo, e nao um cadastro incompleto
            return new ProfessionalContext(person, null);
        }

        if (ativos.size() > 1) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.AMBIGUOUS_CONTEXT.getMessage(),
                    ErrorMessageEnum.AMBIGUOUS_CONTEXT.getCode(),
                    HttpStatus.CONFLICT);
        }

        return new ProfessionalContext(person, ativos.get(0).getOrganization());
    }

    /**
     * A organizacao que o cliente declarou no header, quando declarou uma.
     *
     * <b>Existe para quem age sem credencial profissional.</b> O monitor da creche que
     * manda um tema de casa e membro, e nao tem CRMV - e {@link #requireContext()}
     * exigiria uma credencial que ele nunca vai ter. Aqui o que se confere e o vinculo,
     * que e o que de fato autoriza agir em nome da organizacao.
     *
     * <b>So responde ao header, e nunca escolhe sozinha.</b> Sem header devolve vazio,
     * ainda que a pessoa tenha vinculos: um registro assinado por uma organizacao que a
     * pessoa nao pretendia nao se corrige depois. Quem quer assinar, declara.
     */
    public Optional<Organization> organizacaoDeclarada(Person person) {
        return organizacaoDeclarada(person, organizacaoPedida());
    }

    /**
     * A mesma coisa, com o valor vindo de fora em vez de ser lido do request.
     *
     * <b>Existe para o controller poder declarar o header.</b> Header declarado em
     * {@code @RequestHeader} aparece no OpenAPI, e a stack do cliente gera o codigo a
     * partir dele - um header que so este provider le seria invisivel para quem gera
     * o cliente. A validacao de vinculo e o 403 continuam aqui, num lugar so.
     */
    public Optional<Organization> organizacaoDeclarada(Person person, String declarada) {
        if (declarada == null || declarada.isBlank()) {
            return Optional.empty();
        }

        UUID organizationId = parseOrganizationId(declarada.trim());

        return Optional.of(membershipRepository
                .findAtivoDaPessoaNaOrganizacao(person.getPersonId(), organizationId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.NOT_ORGANIZATION_MEMBER.getMessage(),
                        ErrorMessageEnum.NOT_ORGANIZATION_MEMBER.getCode(),
                        HttpStatus.FORBIDDEN))
                .getOrganization());
    }

    private String organizacaoPedida() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String valor = atributos.getRequest().getHeader(HEADER_ORGANIZACAO);
            return valor == null || valor.isBlank() ? null : valor.trim();
        }

        return null;
    }

    private UUID parseOrganizationId(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.INVALID_REQUEST.getMessage(),
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public Person require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw naoAutenticado();
        }

        Person person = personRepository.findByEmail(authentication.getName())
                .orElseThrow(this::naoAutenticado);

        if (!credentialRepository.existsAtivaPorEmail(person.getEmail(), CredentialStatus.SUSPENSO)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.PROFESSIONAL_CREDENTIAL_REQUIRED.getMessage(),
                    ErrorMessageEnum.PROFESSIONAL_CREDENTIAL_REQUIRED.getCode(),
                    HttpStatus.FORBIDDEN);
        }

        return person;
    }

    private PetfyHealthcareException naoAutenticado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                HttpStatus.UNAUTHORIZED);
    }

}
