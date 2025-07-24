package org.madladlabs.classz;


import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.madladlabs.classz.ClassExtractor;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClassExtractorTest {

    private static final File inputJar = new File("src/test/resources/archive-samples/antlr-2.7.7.jar");
    private static final File outputDir = new File("build/test-output");

    private static final Logger logger = LogManager.getLogger(ClassExtractorTest.class);
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
    public void testExtractsPreprocessorToolClass() throws Exception {
        // Run the extractor
        // Main.main(new String[]{inputJar.getAbsolutePath(), outputDir.getAbsolutePath()});

        File input = new File(inputJar.getAbsolutePath());
        File output = new File(outputDir.getAbsolutePath());
        if (!output.exists()) output.mkdirs();
        logger.info("Extracting classes from: " + input.getAbsolutePath());
        logger.info("Output directory: " + output.getAbsolutePath());

        ClassExtractor.extractFromFile(input, output);
        // Check expected class exists
        File expectedClass = new File(outputDir, "antlr/preprocessor/Tool.class");
        assertTrue(expectedClass.exists(), "Expected class file not found: " + expectedClass.getAbsolutePath());
    }
}

/*
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java -jar class-extractor.jar <input-archive> <output-folder>");
            System.exit(1);
        }

        File input = new File(args[0]);
        File output = new File(args[1]);
        if (!output.exists()) output.mkdirs();

        logger.info("Extracting classes from: " + input.getAbsolutePath());
        logger.info("Output directory: " + output.getAbsolutePath());

        ClassExtractor.extractFromFile(input, output);
    }

}

 */
