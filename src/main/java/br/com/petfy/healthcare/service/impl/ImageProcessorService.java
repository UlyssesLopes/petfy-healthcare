package br.com.petfy.healthcare.service.impl;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ImageProcessorService {

    public BufferedImage preProcess(BufferedImage originalImage) {

        BufferedImage croppedImage = cropToDataRegion(originalImage);

        saveDebugImage(croppedImage, "corte-preview.png");

        BufferedImage grayImage = new BufferedImage(
                croppedImage.getWidth(),
                croppedImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g2dGray = grayImage.createGraphics();
        g2dGray.drawImage(croppedImage, 0, 0, null);
        g2dGray.dispose();

        BufferedImage binaryImage = new BufferedImage(
                grayImage.getWidth(),
                grayImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        Graphics2D g2dBinary = binaryImage.createGraphics();
        g2dBinary.drawImage(grayImage, 0, 0, null);
        g2dBinary.dispose();

        return binaryImage;
    }

    private BufferedImage cropToDataRegion(BufferedImage original) {

        int x = (int) (original.getWidth() * 0.22);
        int y = (int) (original.getHeight() * 0.20);
        int width = (int) (original.getWidth() * 0.28);
        int height = (int) (original.getHeight() * 0.68);

        return original.getSubimage(x, y, width, height);
    }

    private void saveDebugImage(BufferedImage image, String fileName) {
        try {
            File outputfile = new File(fileName);
            ImageIO.write(image, "png", outputfile);
            System.out.println("Imagem cortada salva em: " + outputfile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar imagem cortada: " + e.getMessage());
        }
    }

}
