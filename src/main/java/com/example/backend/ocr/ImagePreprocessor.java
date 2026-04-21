package com.example.backend.ocr;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImagePreprocessor {

  private static final int MAX_WIDTH = 1200;
  private static final float JPEG_QUALITY = 0.75f;

  public byte[] resize(byte[] imageBytes) {
    try {
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
      if (original == null) return imageBytes;

      int originalWidth = original.getWidth();
      int originalHeight = original.getHeight();

      BufferedImage target;
      if (originalWidth > MAX_WIDTH) {
        int newHeight = (int) ((double) originalHeight * MAX_WIDTH / originalWidth);
        target = new BufferedImage(MAX_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = target.createGraphics();
        g2d.drawImage(original, 0, 0, MAX_WIDTH, newHeight, null);
        g2d.dispose();
      } else {
        target = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = target.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(JPEG_QUALITY);
      ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
      writer.setOutput(ios);
      writer.write(null, new IIOImage(target, null, null), param);
      writer.dispose();
      ios.close();

      byte[] result = baos.toByteArray();
      log.info(
          "=== 이미지 압축 완료: {}bytes → {}bytes ({}%)",
          imageBytes.length,
          result.length,
          (int) ((double) result.length / imageBytes.length * 100));
      return result;

    } catch (Exception e) {
      log.warn("=== 이미지 압축 실패, 원본 사용: {}", e.getMessage());
      return imageBytes;
    }
  }
}
