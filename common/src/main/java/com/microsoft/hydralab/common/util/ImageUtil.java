// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageUtil {
    public static BufferedImage scaleBufferedImage(BufferedImage before, double ratio) {
        int w = before.getWidth();
        int h = before.getHeight();
        BufferedImage after = new BufferedImage((int) (w * ratio), (int) (h * ratio), BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(ratio, ratio);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(before, after);
    }

    public static boolean writeBufferedImageToFile(BufferedImage image, File outputFile) throws IOException {
        boolean foundWriter = ImageIO.write(image, "jpg", outputFile);
        if (!foundWriter) {
            foundWriter = ImageIO.write(image, "png", outputFile);
        }
        return foundWriter;
    }

    public static File joinImages(File outputFileDir, String outputFileName, @NotNull List<File> files) {
        Assert.notEmpty(files, "files should not be empty");
        if (files.size() == 1) {
            return files.get(0);
        }
        File outputFile = new File(outputFileDir, outputFileName);
        int outputHeight = 0;
        int outputWidth = 0;
        List<BufferedImage> images = new ArrayList<>();
        try {
            for (File file : files) {
                BufferedImage image = ImageIO.read(file);
                outputHeight = Math.max(outputHeight, image.getHeight());
                outputWidth += image.getWidth();
                images.add(image);
            }

            int writedWidth = 0;
            BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < images.size(); i++) {
                BufferedImage image = images.get(i);
                outputImage.setRGB(writedWidth, 0, image.getWidth(), image.getHeight(),
                        image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()),
                        0, image.getWidth());
                writedWidth += image.getWidth();
            }
            ImageIO.write(outputImage, "jpg", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HydraLabRuntimeException("Failed to join images", e);
        }
        return outputFile;
    }
}
