// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ImageUtil {

    private ImageUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

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

    public static File joinImages(File firstFile, File secondFile, File outputFile) {
        try {
            BufferedImage imageF = ImageIO.read(firstFile);
            int widthF = imageF.getWidth();
            int heightF = imageF.getHeight();
            int[] imageArrayF = new int[widthF * heightF];
            imageArrayF = imageF.getRGB(0, 0, widthF, heightF, imageArrayF, 0, widthF);

            BufferedImage imageS = ImageIO.read(secondFile);
            int widthS = imageS.getWidth();
            int heightS = imageS.getHeight();
            int[] imageArrayS = new int[widthS * heightS];
            imageArrayS = imageS.getRGB(0, 0, widthS, heightS, imageArrayS, 0, widthS);

            int heightNew = Math.max(heightF, heightS);
            BufferedImage imageNew =
                    new BufferedImage(widthF + widthS, heightNew, BufferedImage.TYPE_INT_RGB);
            imageNew.setRGB(0, 0, widthF, heightF, imageArrayF, 0, widthF);
            imageNew.setRGB(widthF, 0, widthS, heightS, imageArrayS, 0, widthS);

            ImageIO.write(imageNew, "jpg", outputFile);
        } catch (Exception e) {
            e.printStackTrace();
            return firstFile;
        }
        return outputFile;
    }
}
