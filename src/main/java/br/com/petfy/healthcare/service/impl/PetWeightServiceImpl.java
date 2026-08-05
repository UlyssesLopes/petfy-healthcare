package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetWeightResponseDTO;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetWeightHistory;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.PetWeightHistoryRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.PetWeightService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetWeightServiceImpl implements PetWeightService {

    private final PetWeightHistoryRepository weightHistoryRepository;
    private final PetRepository petRepository;
    private final CurrentOwnerProvider currentOwnerProvider;

    @Override
    @Transactional
    public PetWeightResponseDTO addWeight(UUID petId, PetWeightRequestDTO request) {
        Pet pet = buscarPetDoOwnerAutenticado(petId);

        PetWeightHistory entry = PetWeightHistory.builder()
                .pet(pet)
                .weight(request.getWeight())
                .measuredAt(request.getMeasuredAt())
                .note(request.getNote())
                .creationDate(LocalDateTime.now())
                .build();

        PetWeightHistory saved = weightHistoryRepository.save(entry);

        // atualiza o espelho em Pet.weight com a medicao mais recente
        // compara por data: se o tutor inseriu uma medicao historica que nao e
        // a mais recente, o espelho nao regride
        weightHistoryRepository.findFirstByPetPetIdOrderByMeasuredAtDesc(petId)
                .ifPresent(ultima -> {
                    pet.setWeight(ultima.getWeight());
                    pet.setUpdateDate(LocalDateTime.now());
                    petRepository.save(pet);
                });

        return toResponse(saved);
    }

    @Override
    public List<PetWeightResponseDTO> listWeights(UUID petId) {
        buscarPetDoOwnerAutenticado(petId);

        return weightHistoryRepository.findByPetPetIdOrderByMeasuredAtDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Pet buscarPetDoOwnerAutenticado(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petRepository.findById(petId)
                .filter(pet -> pet.getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private PetWeightResponseDTO toResponse(PetWeightHistory h) {
        return PetWeightResponseDTO.builder()
                .weightHistoryId(h.getWeightHistoryId())
                .petId(h.getPet().getPetId())
                .weight(h.getWeight())
                .measuredAt(h.getMeasuredAt())
                .note(h.getNote())
                .creationDate(h.getCreationDate())
                .build();
    }

}
