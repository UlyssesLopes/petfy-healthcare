package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.TimelineEntryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TimelineService {

    Page<TimelineEntryResponseDTO> doAnimal(UUID animalId, Pageable pageable);

}
