package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.SensitiveAccessLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Leitura do log de acesso pelo tutor. A escrita fica no {@link SensitiveAccessLogger}. */
public interface SensitiveAccessLogService {

    Page<SensitiveAccessLogResponseDTO> listByAnimal(UUID animalId, Pageable pageable);

}
