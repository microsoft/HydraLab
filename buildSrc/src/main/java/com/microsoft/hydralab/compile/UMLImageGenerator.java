// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.compile;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.SourceFileReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UMLImageGenerator {
    public void generateUMLImageFromFile(File source) throws IOException {
        generateUMLImageFromFile(source, source.getAbsoluteFile().getParentFile(), false);
    }

    public void generateUMLImageFromFile(File source, File outputDir) throws IOException {
        generateUMLImageFromFile(source, outputDir, false);
    }

    public void generateUMLImageFromFile(File source, File outputDir, boolean svg) throws IOException {
        if (!source.exists()) throw new RuntimeException(source.getAbsolutePath() + " file doesn't exist");

        SourceFileReader reader = svg ?
                new SourceFileReader(source, outputDir, new FileFormatOption(FileFormat.SVG)) :
                new SourceFileReader(source, outputDir);

        List<GeneratedImage> list = reader.getGeneratedImages();
        System.out.printf("Successfully generated %d UML images.\n", list.size());
    }
}