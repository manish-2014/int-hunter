package org.madladlabs.classz;

import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EarExtractorTest {

    private static final File inputEar = new File("src/test/resources/archive-samples/AccessEmployee.ear");
    private static final File outputDir = new File("build/test-output");

    private static final Logger logger = LogManager.getLogger(EarExtractorTest.class);

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
    public void testExtractsAccessEmpServletFromWarInsideEar() throws Exception {
        //Main.main(new String[]{inputEar.getAbsolutePath(), outputDir.getAbsolutePath()});
        File input = new File(inputEar.getAbsolutePath());
        File output = new File(outputDir.getAbsolutePath());
        if (!output.exists()) output.mkdirs();
        logger.info("Extracting classes from: " + input.getAbsolutePath());
        logger.info("Output directory: " + output.getAbsolutePath());

        ClassExtractor.extractFromFile(input, output);

        File expectedClass = new File(outputDir, "WEB-INF/classes/client/AccessEmpServlet.class");
        assertTrue(expectedClass.exists(), "Expected servlet class not found in WAR: " + expectedClass.getAbsolutePath());
    }

    
}
