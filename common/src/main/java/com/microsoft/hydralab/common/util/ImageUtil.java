package com.microsoft.hydralab.common.util;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
}
