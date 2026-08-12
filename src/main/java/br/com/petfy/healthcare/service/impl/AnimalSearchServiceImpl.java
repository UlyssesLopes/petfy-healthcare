package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalSearchItemDTO;
import br.com.petfy.healthcare.domain.dto.AnimalSearchResultDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalSearchService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A busca autenticada (Tela 35).
 *
 * <b>O recorte de acesso e feito AQUI, e a consulta so ordena o que ja e de quem procura.</b> Este
 * servico monta primeiro o conjunto que a pessoa alcanca — custodia dela, custodia da organizacao
 * declarada, concessao dela, concessao da organizacao — e so entao pergunta ao banco quais daqueles
 * casam com o termo. O contrario (buscar e depois filtrar) devolveria paginas cheias de animais que
 * ela nao pode ver, e o filtro seria a unica coisa entre um estranho e a lista de cadastros do Petfy.
 *
 * <b>E a frase mais incomum da tela sai do que sobra:</b> "existem outros animais com microchip
 * comecando em 9810 no Petfy. Voce nao tem acesso a eles, e por isso nao aparecem aqui." Calar sobre
 * o resto faria a pessoa concluir que o animal que ela procura nao esta no produto.
 */
@Service
@RequiredArgsConstructor
public class AnimalSearchServiceImpl implements AnimalSearchService {

    /** Menos de tres letras nao e buscar: e listar tudo que a pessoa alcanca, com passos extras. */
    private static final int MINIMO = 3;

    private final AnimalRepository animalRepository;
    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    @Override
    @Transactional(readOnly = true)
    public AnimalSearchResultDTO buscar(String termo) {
        String limpo = termo == null ? "" : termo.trim();

        if (limpo.length() < MINIMO) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.SEARCH_TERM_TOO_SHORT.getMessage(),
                    ErrorMessageEnum.SEARCH_TERM_TOO_SHORT.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        Person eu = currentPersonProvider.require();
        LocalDateTime agora = LocalDateTime.now();

        Optional<Organization> organizacao = currentProfessionalProvider.organizacaoDeclarada(eu);

        Set<UUID> meus = new LinkedHashSet<>();
        custodyRepository.findEmCursoDaPessoa(eu.getPersonId())
                .forEach(c -> meus.add(c.getAnimal().getAnimalId()));

        /*
         * A CONCESSAO PESSOAL ENTRA EM "SEUS ANIMAIS", e nao no grupo da organizacao. O co-tutor nao
         * alcanca o animal por trabalhar em lugar nenhum: alguem o convidou pessoalmente, e para ele
         * aquele animal e tao seu quanto o proprio. Po-lo no segundo grupo diria que o acesso vem do
         * emprego, e ele sobreviveria a demissao.
         */
        grantRepository.findVigentesDaPessoa(eu.getPersonId(), agora)
                .forEach(g -> meus.add(g.getAnimal().getAnimalId()));

        Set<UUID> pelaOrganizacao = new LinkedHashSet<>();

        organizacao.ifPresent(declarada -> {
            custodyRepository.buscarEmCursoDaOrganizacao(declarada.getOrganizationId(), "%",
                            PageRequest.of(0, 500))
                    .forEach(c -> pelaOrganizacao.add(c.getAnimal().getAnimalId()));

            grantRepository.findVigentesDaClinica(declarada.getOrganizationId(), agora)
                    .forEach(g -> pelaOrganizacao.add(g.getAnimal().getAnimalId()));
        });

        // um animal que a pessoa responde E que a clinica dela alcanca aparece uma vez so, e no
        // primeiro grupo: o vinculo mais forte manda
        pelaOrganizacao.removeAll(meus);

        Set<UUID> alcancados = new LinkedHashSet<>(meus);
        alcancados.addAll(pelaOrganizacao);

        String busca = "%" + limpo.toLowerCase() + "%";

        List<Animal> encontrados = alcancados.isEmpty()
                ? List.of()
                : animalRepository.buscarEntre(alcancados, busca);

        return AnimalSearchResultDTO.builder()
                .mine(encontrados.stream()
                        .filter(a -> meus.contains(a.getAnimalId()))
                        .map(a -> toItem(a, false))
                        .collect(Collectors.toList()))
                .throughOrganization(encontrados.stream()
                        .filter(a -> pelaOrganizacao.contains(a.getAnimalId()))
                        .map(a -> toItem(a, true))
                        .collect(Collectors.toList()))
                .organizationName(organizacao.map(Organization::getName).orElse(null))
                /*
                 * A consulta do "existem outros" precisa de um conjunto NAO VAZIO para o `not in`
                 * funcionar. Quem nao alcanca animal nenhum recebe um id impossivel, e a pergunta
                 * continua sendo a certa: "ha algum que case e que eu nao alcance?".
                 */
                .othersExist(animalRepository.existeForaDoAlcance(
                        alcancados.isEmpty() ? Set.of(new UUID(0L, 0L)) : alcancados, busca))
                .build();
    }

    /**
     * "Amora · tutor Ricardo Alves".
     *
     * <b>O nome de quem responde so aparece no grupo da organizacao</b>, e nao nos proprios animais:
     * dizer a alguem quem e o tutor do proprio cachorro seria o produto explicando a pessoa quem ela
     * e. E ele vem da custodia em curso — a organizacao que ja alcanca o animal ja alcanca o contato
     * de quem cuida dele quando o escopo permite, entao nao ha nada aqui que ela nao pudesse ver.
     */
    private AnimalSearchItemDTO toItem(Animal animal, boolean comTutor) {
        String tutor = null;

        if (comTutor) {
            tutor = custodyRepository.findEmCurso(animal.getAnimalId())
                    .map(Custody::getHolderPerson)
                    .map(Person::getName)
                    .orElse(null);
        }

        return AnimalSearchItemDTO.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .microchipNumber(animal.getMicrochipNumber())
                .holderName(tutor)
                .build();
    }

}
