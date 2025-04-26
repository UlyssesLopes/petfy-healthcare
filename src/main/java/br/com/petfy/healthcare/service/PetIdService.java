package br.com.petfy.healthcare.service;


import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PetIdService {

    PetResponseDTO importPetFromIdCard(MultipartFile file) throws IOException, TesseractException;

}
