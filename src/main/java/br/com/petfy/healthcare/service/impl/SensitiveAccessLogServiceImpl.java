package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.SensitiveAccessLogResponseDTO;
import br.com.petfy.healthcare.domain.entity.SensitiveAccessLog;
import br.com.petfy.healthcare.domain.repository.SensitiveAccessLogRepository;
import br.com.petfy.healthcare.security.PetAccessGuard;
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
    private final PetAccessGuard petAccessGuard;

    /**
     * Exige apenas leitura: saber quem abriu o historico de saude do pet e parte de
     * acompanhar o pet, e o VIEWER existe para acompanhar. Exigir titular esconderia
     * a informacao de quem mais precisa dela - o co-tutor que so olha e o que tem
     * menos visibilidade do que acontece com o animal.
     *
     * Consultar o log nao gera entrada no log: o tutor nao e terceiro, e um log que
     * registra a propria leitura cresce sozinho sem responder nada.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SensitiveAccessLogResponseDTO> listByPet(UUID petId, Pageable pageable) {
        petAccessGuard.requireLeitura(petId);

        return sensitiveAccessLogRepository.findByPetPetIdOrderByAccessedAtDesc(petId, pageable)
                .map(this::toResponse);
    }

    private SensitiveAccessLogResponseDTO toResponse(SensitiveAccessLog log) {
        return SensitiveAccessLogResponseDTO.builder()
                .sensitiveAccessLogId(log.getSensitiveAccessLogId())
                .petId(log.getPet().getPetId())
                .actorType(log.getActorType())
                .actorName(log.getActorName())
                .clinicName(log.getClinicName())
                .resource(log.getResource())
                .accessedAt(log.getAccessedAt())
                .ipAddress(log.getIpAddress())
                .build();
    }

}
