package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.service.PetIdService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PetIdServiceImpl implements PetIdService {

    private final TesseractOcrServiceImpl tesseractOcrService;
    private final ImageProcessorService imageProcessorService;

    @Override
    public PetResponseDTO importPetFromIdCard(MultipartFile file) throws IOException, TesseractException {

        BufferedImage imageFile = ImageIO.read(file.getInputStream());
        BufferedImage bufferedImage = imageProcessorService.preProcess(imageFile);

        String extractedText = tesseractOcrService.extractText(bufferedImage);

        PetResponseDTO petResponseDTO = parse(extractedText);

        return petResponseDTO;
    }

    public PetResponseDTO parse(String text) {
        PetResponseDTO petResponse = new PetResponseDTO();
        petResponse.setName(extractField(text, "Nome do Animal"));
        petResponse.setBreed(extractField(text, "Espécie")); // pode mapear baseado em `Espécie`
        petResponse.setGender(extractField(text, "Sexo"));
//        petResponse.setColor(extractField(text, "Cor"));
//        petResponse.setCastrated(parseBoolean(extractField(text, "Castrado")));
//        petResponse.setBirthDate(parseDate(extractField(text, "Data de Nascimento")));

        return petResponse;
    }

    private String extractField(String ocrText, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + "\\s+(.+)");
        Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

}
