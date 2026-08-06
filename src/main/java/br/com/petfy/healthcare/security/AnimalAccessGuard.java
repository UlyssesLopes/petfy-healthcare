package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Decide se a pessoa autenticada alcanca um animal, e em que nivel.
 *
 * Existe como peca unica porque antes da V15 essa decisao estava repetida em duas
 * dezenas de lugares. Enquanto a regra era "dono unico", repetir era so feio; com
 * varias origens de alcance, cada copia vira uma chance de alguem esquecer um caso
 * e transformar leitura em escrita - ou pior, deixar passar animal de terceiro.
 *
 * <b>Duas origens de alcance desde o P2b, e elas nao sao equivalentes:</b>
 *
 * <ul>
 *   <li><b>Custodia</b> - a pessoa responde pelo animal. Alcance total, sem prazo e
 *       sem concessao de ninguem. E o que era o papel HOLDER.</li>
 *   <li><b>Concessao</b> - alguem que responde pelo animal deu acesso a ela, com
 *       nivel e prazo. E o que eram os papeis EDITOR e VIEWER.</li>
 * </ul>
 *
 * A ordem da consulta importa: custodia primeiro. Quem responde pelo animal nao
 * precisa de concessao, e checar concessao antes faria o titular depender de uma
 * linha que nao existe para ele.
 *
 * <b>Animal inalcancavel responde 404, nunca 403.</b> Um 403 confirmaria que aquele
 * id existe, permitindo varrer ids para descobrir o que ha na base. A distincao so
 * aparece quando a pessoa ja alcanca o animal e falta nivel: ai o 403 e correto,
 * porque nao revela nada que ela ja nao saiba.
 */
@Component
@RequiredArgsConstructor
public class AnimalAccessGuard {

    private final AnimalRepository animalRepository;
    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final CurrentPersonProvider currentPersonProvider;

    /** Le a carteira: custodia ou qualquer concessao vigente serve. */
    public Animal requireLeitura(UUID animalId) {
        return require(animalId, GrantLevel.VIEWER);
    }

    /** Registra vacina, corrige peso, edita o cadastro: custodia ou concessao EDITOR. */
    public Animal requireEscrita(UUID animalId) {
        return require(animalId, GrantLevel.EDITOR);
    }

    /**
     * Concede acesso, transfere custodia, apaga o animal: <b>so quem responde por
     * ele</b>.
     *
     * Nenhum nivel de concessao chega aqui, e nao por rigor: quem tem acesso
     * concedido poderia conceder acesso a um comparsa e, no limite, tomar o animal
     * de quem responde por ele. Era a mesma razao pela qual o EDITOR nunca pode
     * convidar.
     */
    public Animal requireCustodia(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();

        custodyRepository.findEmCursoDaPessoa(animalId, personId)
                .orElseThrow(() -> {
                    // se a pessoa alcanca o animal por concessao, ela sabe que ele
                    // existe: negar com 404 seria mentir para quem ja tem a informacao
                    if (concessaoVigente(animalId, personId).isPresent()) {
                        return nivelInsuficiente();
                    }
                    return animalNaoEncontrado();
                });

        return carregar(animalId);
    }

    /**
     * A custodia em curso da pessoa autenticada neste animal.
     *
     * Substituiu {@code vinculoDoAutenticado}, que devolvia o PetTutor e servia para
     * qualquer um dos tres papeis. Agora quem quer saber sobre concessao pergunta
     * sobre concessao - misturar as duas coisas num retorno so foi o que este passo
     * desfez.
     */
    public Optional<Custody> custodiaDoAutenticado(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();
        return custodyRepository.findEmCursoDaPessoa(animalId, personId);
    }

    /** Alcanca de qualquer forma - por responder pelo animal ou por concessao. */
    public boolean alcanca(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();

        return custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()
                || concessaoVigente(animalId, personId).isPresent();
    }

    private Animal require(UUID animalId, GrantLevel nivelExigido) {
        UUID personId = currentPersonProvider.require().getPersonId();

        // custodia primeiro: quem responde pelo animal nao depende de concessao
        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            return carregar(animalId);
        }

        Grant concessao = concessaoVigente(animalId, personId)
                .orElseThrow(AnimalAccessGuard::animalNaoEncontrado);

        if (!concessao.getLevel().permite(nivelExigido)) {
            // aqui o 403 e seguro: quem chegou ate este ponto ja alcanca o animal,
            // entao a resposta nao revela existencia de nada novo
            throw nivelInsuficiente();
        }

        return carregar(animalId);
    }

    private Optional<Grant> concessaoVigente(UUID animalId, UUID personId) {
        return grantRepository.findVigenteDaPessoaNoAnimal(animalId, personId, LocalDateTime.now());
    }

    /**
     * Carrega o animal so depois de o alcance estar decidido.
     *
     * A ordem cobre o animal apagado entre o vinculo e a leitura: ele responde igual
     * a quem nunca alcancou, em vez de 500.
     */
    private Animal carregar(UUID animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(AnimalAccessGuard::animalNaoEncontrado);
    }

    private static PetfyHealthcareException nivelInsuficiente() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getMessage(),
                ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode(),
                HttpStatus.FORBIDDEN);
    }

    private static PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

}
