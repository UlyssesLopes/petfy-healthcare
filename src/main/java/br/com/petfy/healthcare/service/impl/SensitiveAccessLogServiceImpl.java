package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.SensitiveAccessLogResponseDTO;
import br.com.petfy.healthcare.domain.entity.SensitiveAccessLog;
import br.com.petfy.healthcare.domain.repository.SensitiveAccessLogRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.SensitiveAccessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SensitiveAccessLogServiceImpl implements SensitiveAccessLogService {

    private final SensitiveAccessLogRepository sensitiveAccessLogRepository;
    private final AnimalAccessGuard animalAccessGuard;

    /**
     * Exige apenas leitura: saber quem abriu o historico de saude do animal e parte de
     * acompanhar o animal, e o VIEWER existe para acompanhar. Exigir titular esconderia
     * a informacao de quem mais precisa dela - o co-tutor que so olha e o que tem
     * menos visibilidade do que acontece com o animal.
     *
     * Consultar o log nao gera entrada no log: o tutor nao e terceiro, e um log que
     * registra a propria leitura cresce sozinho sem responder nada.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SensitiveAccessLogResponseDTO> listByAnimal(UUID animalId, Pageable pageable) {
        animalAccessGuard.requireLeitura(animalId);

        return sensitiveAccessLogRepository.findByAnimalAnimalIdOrderByAccessedAtDesc(animalId, pageable)
                .map(this::toResponse);
    }

    private SensitiveAccessLogResponseDTO toResponse(SensitiveAccessLog log) {
        return SensitiveAccessLogResponseDTO.builder()
                .sensitiveAccessLogId(log.getSensitiveAccessLogId())
                .animalId(log.getAnimal().getAnimalId())
                .actorType(log.getActorType())
                .actorName(log.getActorName())
                .organizationName(log.getOrganizationName())
                .resource(log.getResource())
                .accessedAt(log.getAccessedAt())
                .ipAddress(log.getIpAddress())
                .build();
    }

}
