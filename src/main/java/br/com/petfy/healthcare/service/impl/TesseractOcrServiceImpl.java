package br.com.petfy.healthcare.service.impl;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class TesseractOcrServiceImpl {

    private final Tesseract tesseract;

    public TesseractOcrServiceImpl() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        this.tesseract.setLanguage("por");
    }

    public String extractText(BufferedImage fileImage) throws TesseractException {
        return tesseract.doOCR(fileImage);
    }


}
