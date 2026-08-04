package br.com.petfy.healthcare.service.impl;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;

@Service
public class TesseractOcrServiceImpl {

    private final Tesseract tesseract;

    public TesseractOcrServiceImpl(@Value("${petfy.ocr.tessdata-path:}") String tessdataPath,
                                   @Value("${petfy.ocr.language:por}") String language) {
        this.tesseract = new Tesseract();
        // quando o caminho nao vem configurado, o tess4j resolve sozinho pelo
        // TESSDATA_PREFIX - o que mantem a imagem docker independente do
        // diretorio onde a distribuicao instala o tessdata
        if (StringUtils.hasText(tessdataPath)) {
            this.tesseract.setDatapath(tessdataPath);
        }
        this.tesseract.setLanguage(language);
    }

    public String extractText(BufferedImage fileImage) throws TesseractException {
        return tesseract.doOCR(fileImage);
    }

}
