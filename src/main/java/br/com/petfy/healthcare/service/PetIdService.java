package br.com.petfy.healthcare.service;


import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PetIdService {

    /** O pet importado e sempre vinculado ao owner autenticado na requisicao. */
    PetResponseDTO importPetFromIdCard(MultipartFile file) throws IOException, TesseractException;

}
