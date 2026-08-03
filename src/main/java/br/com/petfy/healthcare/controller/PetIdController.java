package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.service.PetIdService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/pet-id")
@RequiredArgsConstructor
public class PetIdController {

    private final PetIdService petIdService;

    @PostMapping("/import-pet-id-card")
    public ResponseEntity<PetResponseDTO> importPetIdCard(@RequestParam MultipartFile file) throws IOException, TesseractException {
        PetResponseDTO dto = petIdService.importPetFromIdCard(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

}
