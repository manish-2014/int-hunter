package org.madladlabs.classz;
import javassist.ClassPool;
import javassist.CtClass;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.madladlabs.classz.extractors.HibernateIntFieldExtractor;

import org.madladlabs.classz.extractors.PreparedStmtExtractor;
import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.TestWriter;

public class HibernateIntFieldExtractorTest {

    @Test
    void testPreparedStatementDetection() throws Exception {
        Path bytecodeRoot = Paths.get("src/test/resources/bytecode-samples");
        Path classFile = bytecodeRoot.resolve("com/example/jdbcsamples/User.class");

        System.out.println("Loading class file: " + classFile.toAbsolutePath());

        try (InputStream in = Files.newInputStream(classFile)) {
            CtClass ctClass = ClassPool.getDefault().makeClass(in);

            HibernateIntFieldExtractor extractor = new HibernateIntFieldExtractor();
            TestWriter writer = new TestWriter();

            System.out.println("Running HibernateIntFieldExtractor on class: " + ctClass.getName());
            extractor.process(ctClass, writer);

            List<Finding> findings = writer.getFindings();
            System.out.println("Findings (" + findings.size() + "):");
            for (Finding f : findings) {
                System.out.println("  " + f);
            }

            Assertions.assertTrue(!findings.isEmpty(), "Expected at least one PreparedStatement finding.");
        }
    }
}
