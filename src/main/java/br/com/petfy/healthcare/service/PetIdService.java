package br.com.petfy.healthcare.service;


import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface PetIdService {

    PetResponseDTO importPetFromIdCard(UUID ownerId, MultipartFile file) throws IOException, TesseractException;

}
