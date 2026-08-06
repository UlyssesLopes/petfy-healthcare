package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.TimelineEntryResponseDTO;
import br.com.petfy.healthcare.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A linha do tempo do animal.
 *
 * Encerra a ideia de o cliente montar a cronologia chamando seis endpoints e ordenando
 * em memoria: o que entra, o que quem le pode ver e como se ordena sao regra de
 * dominio, e nao formatacao de tela. Com seis clientes, seriam seis implementacoes da
 * mesma regra - e a primeira a divergir mostraria a ordem errada de fatos de saude.
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/{animalId}/timeline")
    public ResponseEntity<Page<TimelineEntryResponseDTO>> timeline(@PathVariable UUID animalId,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(timelineService.doAnimal(animalId, pageable));
    }

}
