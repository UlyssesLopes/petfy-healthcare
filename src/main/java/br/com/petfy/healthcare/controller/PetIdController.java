package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import br.com.petfy.healthcare.service.PetIdService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/pet-id")
@RequiredArgsConstructor
public class PetIdController {

    private final PetIdService petIdService;

    @Operation(summary = "Cria o animal a partir da foto de uma carteirinha de papel",
               description = "Le a carteirinha por OCR. Existe porque a historia de saude que importa "
                             + "esta em papel, e digitar tudo a mao e o que faz o tutor desistir antes "
                             + "de comecar. O resultado e um ponto de partida para revisao, e nao um "
                             + "registro conferido - OCR erra, e o que ele produz nao carrega assinatura "
                             + "de quem afirmou nada.")
    @PostMapping(value = "/import-pet-id-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnimalResponseDTO> importPetIdCard(@RequestPart("file") MultipartFile file) throws IOException, TesseractException {
        AnimalResponseDTO dto = petIdService.importAnimalFromIdCard(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

}
