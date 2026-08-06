package br.com.petfy.healthcare.service;


import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PetIdService {

    /** O animal importado e sempre vinculado ao person autenticado na requisicao. */
    AnimalResponseDTO importAnimalFromIdCard(MultipartFile file) throws IOException, TesseractException;

}
