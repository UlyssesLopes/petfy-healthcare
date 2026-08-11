package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.TimelineEntryResponseDTO;
import br.com.petfy.healthcare.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "A vida do animal em ordem, atravessando custodias e organizacoes",
               description = "Ordenada por quando aconteceu, e nao por quando foi digitado - a vacina "
                             + "de 2019 lancada hoje aparece em 2019. Nao recomeca na transferencia: o "
                             + "adotante recebe a vida inteira. Cada entrada traz quem registrou, em "
                             + "nome de que organizacao, a credencial com o estado dela, quantas "
                             + "correcoes sofreu, e o peso anterior quando e uma pesagem. Evento fora "
                             + "do escopo de quem le aparece SEM conteudo em vez de desaparecer, com "
                             + "visivel=false - sumir diria que o animal nunca foi ao veterinario. "
                             + "Os dois recortes da Tela 30 sao opcionais e combinaveis: "
                             + "onlyMyOrganization limita ao que foi registrado em nome da "
                             + "organizacao em que voce age agora, e onlyMine ao que voce mesmo "
                             + "registrou. Eles recortam o que voce PEDIU; o escopo continua "
                             + "mascarando o que voce nao alcanca, e evento fora de escopo "
                             + "continua aparecendo opaco em vez de sumir.")
    @GetMapping("/{animalId}/timeline")
    public ResponseEntity<Page<TimelineEntryResponseDTO>> timeline(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "false") boolean onlyMyOrganization,
            @RequestParam(defaultValue = "false") boolean onlyMine,
            Pageable pageable) {
        return ResponseEntity.ok(timelineService.doAnimal(animalId, onlyMyOrganization, onlyMine, pageable));
    }

}
