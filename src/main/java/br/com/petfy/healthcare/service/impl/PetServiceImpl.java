package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.PetPurger;
import br.com.petfy.healthcare.service.PetService;
import br.com.petfy.healthcare.service.PuppyProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetTutorRepository petTutorRepository;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final PetAccessGuard petAccessGuard;
    private final PuppyProtocolService puppyProtocolService;
    private final PetPurger petPurger;

    /**
     * Quem cadastra o pet nasce titular dele. O vinculo e criado aqui, e nao por
     * cascade a partir de Pet: a partir da V15 nao existe mais "o dono" como
     * campo, e deixar o vinculo implicito esconderia a unica linha que decide
     * quem manda no pet.
     */
    @Override
    @Transactional
    public PetResponseDTO createPet(PetRequestDTO dto) {
        Owner owner = currentOwnerProvider.require();

        Pet pet = Pet.builder()
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

        Pet salvo = petRepository.save(pet);

        petTutorRepository.save(PetTutor.builder()
                .pet(salvo)
                .owner(owner)
                .role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now())
                .build());

        // Filhote ganha as doses planejadas do protocolo inicial. E silencioso
        // para adulto: continua registrando vacina caso a caso.
        puppyProtocolService.gerarEsquemaInicialSePuppy(salvo);

        return toResponse(salvo, owner.getOwnerId());
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponseDTO getPetById(UUID petId) {
        return toResponse(petAccessGuard.requireLeitura(petId));
    }

    /** Lista todos os pets em que a pessoa e tutora, em qualquer papel. */
    @Override
    @Transactional(readOnly = true)
    public Page<PetResponseDTO> listAllPets(Pageable pageable) {
        return petRepository.findByTutorsOwnerOwnerId(currentOwnerProvider.require().getOwnerId(), pageable)
                .map(this::toResponse);
    }

    /** Editar o cadastro do pet exige EDITOR - leitor acompanha, nao altera. */
    @Override
    @Transactional
    public PetResponseDTO updatePet(UUID petId, PetRequestDTO dto) {
        Pet pet = petAccessGuard.requireEscrita(petId);

        pet.setName(dto.getName() != null ? dto.getName() : pet.getName());
        pet.setType(dto.getType() != null ? dto.getType() : pet.getType());
        pet.setBreed(dto.getBreed() != null ? dto.getBreed() : pet.getBreed());
        pet.setBornDate(dto.getBornDate() != null ? dto.getBornDate() : pet.getBornDate());
        pet.setWeight(dto.getWeight() != null ? dto.getWeight() : pet.getWeight());
        pet.setGender(dto.getGender() != null ? dto.getGender() : pet.getGender());

        if (dto.getMicrochipNumber() != null) {
            pet.setMicrochipNumber(dto.getMicrochipNumber());
            pet.setMicrochip(Boolean.TRUE);
        }

        if (dto.getCastratedAt() != null) {
            pet.setCastratedAt(dto.getCastratedAt());
        }

        if (castradoSegundo(dto) != null) {
            pet.setCastrated(castradoSegundo(dto));
        }
        // species fica de fora do PUT parcial de proposito: mudar especie de
        // um pet ja com historico deixaria vacinas cruzadas (aplicada como
        // canina agora consulta catalogo felino). Se acontecer de valer, vai
        // pedir endpoint proprio com o rebuild explicito das relacoes
        pet.setUpdateDate(LocalDateTime.now());

        return toResponse(petRepository.save(pet));
    }

    /**
     * Apagar o pet e do titular. Um co-tutor que nao quer mais acompanhar sai do
     * pet - ver o endpoint de tutores -, e isso nao apaga o historico de saude
     * de um animal que continua tendo dono.
     *
     * A limpeza do que pende do pet fica no {@link PetPurger}, compartilhada com a
     * exclusao de conta: eram duas listas separadas, e elas divergiram - o passo 9
     * trouxe peso e antiparasitario e nenhuma das duas foi atualizada, o que fazia
     * este metodo responder 500 para qualquer pet com vacina.
     */
    @Override
    @Transactional
    public void deletePet(UUID petId) {
        Pet pet = petAccessGuard.requireTitular(petId);

        petPurger.purgeConteudo(List.of(petId));
        petRepository.delete(pet);
    }

    /**
     * Castrado, considerando que a data implica o fato.
     *
     * Informar {@code castratedAt} sem {@code castrated} e afirmar que foi castrado -
     * exigir os dois campos juntos seria burocracia sobre o obvio, e deixar o pet com data
     * de castracao e {@code castrated} nulo daria duas respostas para a mesma pergunta.
     */
    private Boolean castradoSegundo(PetRequestDTO dto) {
        if (dto.getCastrated() != null) {
            return dto.getCastrated();
        }

        return dto.getCastratedAt() != null ? Boolean.TRUE : null;
    }

    private PetResponseDTO toResponse(Pet pet) {
        return toResponse(pet, pet.getHolder().map(Owner::getOwnerId).orElse(null));
    }

    /**
     * {@code ownerId} na resposta passou a significar **o titular**, e nao "o
     * dono", que deixou de existir como conceito unico. O nome ficou por
     * compatibilidade do contrato ja publicado.
     */
    private PetResponseDTO toResponse(Pet pet, UUID holderId) {
        return PetResponseDTO.builder()
                .petId(pet.getPetId())
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .bornDate(pet.getBornDate())
                .weight(pet.getWeight())
                .gender(pet.getGender())
                .species(pet.getSpecies())
                .microchipNumber(pet.getMicrochipNumber())
                .microchip(pet.getMicrochip())
                .castrated(pet.getCastrated())
                .castratedAt(pet.getCastratedAt())
                .ownerId(holderId)
                .creationDate(pet.getCreationDate())
                .updateDate(pet.getUpdateDate())
                .build();
    }

}
