package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalDeath;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.repository.AnimalDeathRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.AnimalPurger;
import br.com.petfy.healthcare.service.AnimalService;
import br.com.petfy.healthcare.service.PuppyProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalDeathRepository animalDeathRepository;
    private final CustodyRepository custodyRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final AnimalAccessGuard animalAccessGuard;
    private final PuppyProtocolService puppyProtocolService;
    private final AnimalPurger animalPurger;

    /**
     * Quem cadastra o animal nasce titular dele. O vinculo e criado aqui, e nao por
     * cascade a partir de Animal: a partir da V15 nao existe mais "o dono" como
     * campo, e deixar o vinculo implicito esconderia a unica linha que decide
     * quem manda no animal.
     */
    @Override
    @Transactional
    public AnimalResponseDTO createAnimal(AnimalRequestDTO dto) {
        Person person = currentPersonProvider.require();

        Animal animal = Animal.builder()
                .name(dto.getName())
                .type(dto.getType())
                .breed(dto.getBreed())
                .bornDate(dto.getBornDate())
                .weight(dto.getWeight())
                .gender(dto.getGender())
                .species(dto.getSpecies())
                .microchipNumber(dto.getMicrochipNumber())
                // o booleano segue o numero: informar o numero e afirmar que tem chip, e
                // deixar os dois divergirem daria duas respostas para a mesma pergunta
                .microchip(dto.getMicrochipNumber() != null ? Boolean.TRUE : null)
                .castrated(castradoSegundo(dto))
                .castratedAt(dto.getCastratedAt())
                .creationDate(LocalDateTime.now())
                .build();

        Animal salvo = animalRepository.save(animal);

        // quem cadastra nasce respondendo pelo animal. Natureza DEFINITIVA: e o tutor
        // comum, sem prazo - lar transitorio e abrigo entram por outro caminho
        custodyRepository.save(Custody.builder()
                .animal(salvo)
                .holderPerson(person)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now())
                .build());

        // Filhote ganha as doses planejadas do protocolo inicial. E silencioso
        // para adulto: continua registrando vacina caso a caso.
        puppyProtocolService.gerarEsquemaInicialSePuppy(salvo);

        return toResponse(salvo, person.getPersonId());
    }

    @Override
    @Transactional(readOnly = true)
    public AnimalResponseDTO getAnimalById(UUID animalId) {
        return toResponse(animalAccessGuard.requireLeitura(animalId));
    }

    /** Lista todos os animals em que a pessoa e tutora, em qualquer papel. */
    @Override
    @Transactional(readOnly = true)
    public Page<AnimalResponseDTO> listAllAnimals(Pageable pageable) {
        return comDataDeObito(animalRepository.findAlcancadosPor(
                currentPersonProvider.require().getPersonId(), LocalDateTime.now(), pageable));
    }

    /**
     * A lista que recebe o animal que saiu da outra (Tela 33).
     *
     * <b>Paginada como a principal</b>, e nao uma lista inteira: quem cuida de animais ha vinte
     * anos tem mais nomes aqui do que na lista de agora, e essa e a lista que so cresce.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AnimalResponseDTO> queJaEstiveramComigo(Pageable pageable) {
        return comDataDeObito(custodyRepository.findQueJaEstiveramComAPessoa(
                currentPersonProvider.require().getPersonId(), LocalDateTime.now(), pageable));
    }

    /**
     * Preenche a data de obito da pagina inteira com UMA consulta.
     *
     * Um {@code findById} dentro do {@code map} custaria uma ida ao banco por animal — vinte numa
     * pagina de vinte, e em "quem ja esteve com voce" praticamente todos respondem. E o mesmo N+1
     * que a V29 tirou da linha do tempo trazendo autoria para dentro da view.
     */
    private Page<AnimalResponseDTO> comDataDeObito(Page<Animal> pagina) {
        List<UUID> ids = pagina.getContent().stream().map(Animal::getAnimalId).toList();

        Map<UUID, LocalDate> obitos = ids.isEmpty()
                ? Map.of()
                : animalDeathRepository.findByAnimalIdIn(ids).stream()
                        .collect(Collectors.toMap(AnimalDeath::getAnimalId, AnimalDeath::getDeceasedOn));

        return pagina.map(animal -> toResponse(animal,
                animal.getHolder().map(Person::getPersonId).orElse(null),
                obitos.get(animal.getAnimalId())));
    }

    /** Editar o cadastro do animal exige EDITOR - leitor acompanha, nao altera. */
    @Override
    @Transactional
    public AnimalResponseDTO updateAnimal(UUID animalId, AnimalRequestDTO dto) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        animal.setName(dto.getName() != null ? dto.getName() : animal.getName());
        animal.setType(dto.getType() != null ? dto.getType() : animal.getType());
        animal.setBreed(dto.getBreed() != null ? dto.getBreed() : animal.getBreed());
        animal.setBornDate(dto.getBornDate() != null ? dto.getBornDate() : animal.getBornDate());
        animal.setWeight(dto.getWeight() != null ? dto.getWeight() : animal.getWeight());
        animal.setGender(dto.getGender() != null ? dto.getGender() : animal.getGender());

        if (dto.getMicrochipNumber() != null) {
            animal.setMicrochipNumber(dto.getMicrochipNumber());
            animal.setMicrochip(Boolean.TRUE);
        }

        if (dto.getCastratedAt() != null) {
            animal.setCastratedAt(dto.getCastratedAt());
        }

        if (castradoSegundo(dto) != null) {
            animal.setCastrated(castradoSegundo(dto));
        }
        // species fica de fora do PUT parcial de proposito: mudar especie de
        // um animal ja com historico deixaria vacinas cruzadas (aplicada como
        // canina agora consulta catalogo felino). Se acontecer de valer, vai
        // pedir endpoint proprio com o rebuild explicito das relacoes
        animal.setUpdateDate(LocalDateTime.now());

        return toResponse(animalRepository.save(animal));
    }

    /**
     * Apagar o animal e do titular. Um co-tutor que nao quer mais acompanhar sai do
     * animal - ver o endpoint de tutores -, e isso nao apaga o historico de saude
     * de um animal que continua tendo dono.
     *
     * A limpeza do que pende do animal fica no {@link AnimalPurger}, compartilhada com a
     * exclusao de conta: eram duas listas separadas, e elas divergiram - o passo 9
     * trouxe peso e antiparasitario e nenhuma das duas foi atualizada, o que fazia
     * este metodo responder 500 para qualquer animal com vacina.
     */
    @Override
    @Transactional
    public void deleteAnimal(UUID animalId) {
        Animal animal = animalAccessGuard.requireCustodia(animalId);

        animalPurger.purgeConteudo(List.of(animalId));
        animalRepository.delete(animal);
    }

    /**
     * Castrado, considerando que a data implica o fato.
     *
     * Informar {@code castratedAt} sem {@code castrated} e afirmar que foi castrado -
     * exigir os dois campos juntos seria burocracia sobre o obvio, e deixar o animal com data
     * de castracao e {@code castrated} nulo daria duas respostas para a mesma pergunta.
     */
    private Boolean castradoSegundo(AnimalRequestDTO dto) {
        if (dto.getCastrated() != null) {
            return dto.getCastrated();
        }

        return dto.getCastratedAt() != null ? Boolean.TRUE : null;
    }

    /**
     * A ficha de um animal so, com a data de obito lida a parte.
     *
     * Uma consulta por ficha aberta e o custo certo aqui — o que nao pode e uma por LINHA de uma
     * lista, e para isso existe o {@link #comDataDeObito}.
     */
    private AnimalResponseDTO toResponse(Animal animal) {
        return toResponse(animal,
                animal.getHolder().map(Person::getPersonId).orElse(null),
                animalDeathRepository.findById(animal.getAnimalId())
                        .map(AnimalDeath::getDeceasedOn)
                        .orElse(null));
    }

    private AnimalResponseDTO toResponse(Animal animal, UUID holderId) {
        return toResponse(animal, holderId, null);
    }

    /**
     * {@code personId} na resposta passou a significar **o titular**, e nao "o
     * dono", que deixou de existir como conceito unico. O nome ficou por
     * compatibilidade do contrato ja publicado.
     */
    private AnimalResponseDTO toResponse(Animal animal, UUID holderId, LocalDate deceasedOn) {
        return AnimalResponseDTO.builder()
                .deceasedOn(deceasedOn)
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .type(animal.getType())
                .breed(animal.getBreed())
                .bornDate(animal.getBornDate())
                .weight(animal.getWeight())
                .gender(animal.getGender())
                .species(animal.getSpecies())
                .microchipNumber(animal.getMicrochipNumber())
                .microchip(animal.getMicrochip())
                .castrated(animal.getCastrated())
                .castratedAt(animal.getCastratedAt())
                .personId(holderId)
                .creationDate(animal.getCreationDate())
                .updateDate(animal.getUpdateDate())
                .build();
    }

}
