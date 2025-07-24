package org.madladlabs.classz;


import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WarExtractorTest {

    private static final File inputWar = new File("src/test/resources/archive-samples/Lab6A.war");
    private static final File outputDir = new File("build/test-output");

    private static final Logger logger = LogManager.getLogger(WarExtractorTest.class);
    @BeforeEach
    public void cleanOutputDir() throws Exception {
        if (outputDir.exists()) {
            Files.walk(outputDir.toPath())
                 .map(Path::toFile)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(File::delete);
        }
        outputDir.mkdirs();
    }

    @Test
    public void testExtractsClassifyServletClass() throws Exception {
        File input = new File(inputWar.getAbsolutePath());
        File output = new File(outputDir.getAbsolutePath());
        if (!output.exists()) output.mkdirs();
        logger.info("Extracting classes from: " + input.getAbsolutePath());
        logger.info("Output directory: " + output.getAbsolutePath());

        ClassExtractor.extractFromFile(input, output);
        //Main.main(new String[]{inputWar.getAbsolutePath(), outputDir.getAbsolutePath()});
        System.out.println("Output directory: " + outputDir.getAbsolutePath()); 
        File expectedClass = new File(outputDir, "WEB-INF/classes/servlets/ClassifyServlet.class");
        assertTrue(expectedClass.exists(), "Expected servlet class not found: " + expectedClass.getAbsolutePath());
    }

    @Test
    public void testExtractsAttributeListPanelFromJarInsideWar() throws Exception {
        //Main.main(new String[]{inputWar.getAbsolutePath(), outputDir.getAbsolutePath()});
        File input = new File(inputWar.getAbsolutePath());
        File output = new File(outputDir.getAbsolutePath());
        if (!output.exists()) output.mkdirs();
        logger.info("Extracting classes from: " + input.getAbsolutePath());
        logger.info("Output directory: " + output.getAbsolutePath());

        ClassExtractor.extractFromFile(input, output);
        //Main.main(new String[]{inputWar.getAbsolutePath(), outputDir.getAbsolutePath()});
        System.out.println("Output directory: " + outputDir.getAbsolutePath());

        File expectedClass = new File(outputDir, "weka/gui/AttributeListPanel.class");
        assertTrue(expectedClass.exists(), "Expected weka class not found: " + expectedClass.getAbsolutePath());
    }
}
