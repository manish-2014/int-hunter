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

import org.madladlabs.classz.extractors.JdbcTemplateIntExtractor;

import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.TestWriter;

public class JdbcTemplateIntExtractorTest {

/*
    @Test
    void testJdbcTemplateIntDetection() throws Exception {
        Path bytecodeRoot = Paths.get("src/test/resources/bytecode-samples");
        Path classFile = bytecodeRoot.resolve("com/example/jdbcsamples/dao/UserDao.class");

        System.out.println("Loading class file: " + classFile.toAbsolutePath());

        try (InputStream in = Files.newInputStream(classFile)) {
            CtClass ctClass = ClassPool.getDefault().makeClass(in);

            JdbcTemplateIntExtractor extractor = new JdbcTemplateIntExtractor();
            TestWriter writer = new TestWriter();

            System.out.println("Running JdbcTemplateIntExtractor on class: " + ctClass.getName());
            extractor.process(ctClass, writer);

            List<Finding> findings = writer.getFindings();
            System.out.println("Findings (" + findings.size() + "):");
            for (Finding f : findings) {
                System.out.println("  " + f);
            }

            Assertions.assertTrue(!findings.isEmpty(), "Expected at least one JdbcTemplateIntExtractor finding.");
        }
    }
*/
    @Test
    void testJdbcTemplateIntDetection2() throws Exception {
        Path bytecodeRoot = Paths.get("src/test/resources/bytecode-samples");
        Path classFile = bytecodeRoot.resolve("com/example/jdbcsamples/SpringDBManager.class");

        System.out.println("Loading class file: " + classFile.toAbsolutePath());

        try (InputStream in = Files.newInputStream(classFile)) {
            CtClass ctClass = ClassPool.getDefault().makeClass(in);

            JdbcTemplateIntExtractor extractor = new JdbcTemplateIntExtractor();
            TestWriter writer = new TestWriter();

            System.out.println("Running JdbcTemplateIntExtractor on class: " + ctClass.getName());
            extractor.process(ctClass, writer);

            List<Finding> findings = writer.getFindings();
            System.out.println("Findings (" + findings.size() + "):");

            boolean foundIntInInsert = false;
            boolean foundIntIninsertUserxx = false;
            for (Finding f : findings) {
                System.out.println("  " + f);
                if (f.getMethodName().equals("insertUser")) {
                    foundIntInInsert = true; // We expect no ints in INSERT statements
                } else if (f.getMethodName().equals("insertUserxx")) {
                    // We expect ints in UPDATE statements
                    foundIntIninsertUserxx=true;
                }
            }

            Assertions.assertFalse(foundIntInInsert, "Expected no integer values in insertUser method.");
            Assertions.assertTrue(foundIntIninsertUserxx , "Expected  integer values in insertUserXX method. ");
            Assertions.assertTrue(!findings.isEmpty(), "Expected at least one JdbcTemplateIntExtractor finding.");
        }
    }

}
