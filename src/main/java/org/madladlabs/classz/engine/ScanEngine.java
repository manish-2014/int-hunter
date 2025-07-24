package org.madladlabs.classz.engine;

import javassist.ClassPool;
import javassist.CtClass;
import org.madladlabs.classz.reporting.IFindingWriter;
import org.madladlabs.classz.spi.IExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public class ScanEngine {

    private final IFindingWriter writer;
    private final List<IExtractor> extractors = new ArrayList<>();

    public ScanEngine(IFindingWriter writer) {
        this.writer = writer;
    }

    public void loadExtractors() {
        ServiceLoader.load(IExtractor.class).forEach(extractors::add);
    }

    public void scanDirectory(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".class"))
                    .forEach(this::scanClassFile);
        }
    }

    private void scanClassFile(Path classFile) {
        try (InputStream in = Files.newInputStream(classFile)) {
            CtClass ctClass = ClassPool.getDefault().makeClass(in);
            for (IExtractor extractor : extractors) {
                try {
                    extractor.process(ctClass, writer);
                } catch (Exception e) {
                    System.err.println("Extractor " + extractor.name() + " failed on " + ctClass.getName());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process class file: " + classFile);
            e.printStackTrace();
        }
    }
}
