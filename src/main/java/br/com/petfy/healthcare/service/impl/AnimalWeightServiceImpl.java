package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalWeightResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AnimalWeightHistoryRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.AnimalWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalWeightServiceImpl implements AnimalWeightService {

    private final AnimalWeightHistoryRepository weightHistoryRepository;
    private final AnimalRepository animalRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;

    @Override
    @Transactional
    public AnimalWeightResponseDTO addWeight(UUID animalId, AnimalWeightRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        AnimalWeightHistory entry = AnimalWeightHistory.builder()
                .animal(animal)
                .recordedBy(currentPersonProvider.require())
                .weight(request.getWeight())
                .measuredAt(request.getMeasuredAt())
                .note(request.getNote())
                .creationDate(LocalDateTime.now())
                .build();

        AnimalWeightHistory saved = weightHistoryRepository.save(entry);

        // atualiza o espelho em Animal.weight com a medicao mais recente
        // compara por data: se o tutor inseriu uma medicao historica que nao e
        // a mais recente, o espelho nao regride
        weightHistoryRepository.findFirstByAnimalAnimalIdOrderByMeasuredAtDesc(animalId)
                .ifPresent(ultima -> {
                    animal.setWeight(ultima.getWeight());
                    animal.setUpdateDate(LocalDateTime.now());
                    animalRepository.save(animal);
                });

        return toResponse(saved);
    }

    @Override
    public List<AnimalWeightResponseDTO> listWeights(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return weightHistoryRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AnimalWeightResponseDTO toResponse(AnimalWeightHistory h) {
        return AnimalWeightResponseDTO.builder()
                .weightHistoryId(h.getWeightHistoryId())
                .animalId(h.getAnimal().getAnimalId())
                .weight(h.getWeight())
                .measuredAt(h.getMeasuredAt())
                .note(h.getNote())
                .creationDate(h.getCreationDate())
                .build();
    }

}
