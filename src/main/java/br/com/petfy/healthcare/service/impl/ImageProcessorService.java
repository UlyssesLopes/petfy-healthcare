package br.com.petfy.healthcare.service.impl;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class ImageProcessorService {

    public BufferedImage preProcess(BufferedImage originalImage) {

        BufferedImage grayImage = new BufferedImage(
          originalImage.getWidth(),
          originalImage.getHeight(),
          BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics graphics = grayImage.getGraphics();
        graphics.drawImage(originalImage, 0, 0, null);
        graphics.dispose();

        BufferedImage binaryImage = new BufferedImage(
          grayImage.getWidth(),
          grayImage.getHeight(),
          BufferedImage.TYPE_BYTE_BINARY
        );

        Graphics2D graphics2D = binaryImage.createGraphics();
        graphics2D.drawImage(grayImage, 0, 0, null);
        graphics.dispose();

        return binaryImage;
    }


}
