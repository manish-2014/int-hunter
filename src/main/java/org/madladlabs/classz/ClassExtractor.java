package org.madladlabs.classz;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassExtractor {
    private static final Logger logger = LogManager.getLogger(ClassExtractor.class);



    public static void extractFromFile(File file, File outputDir) throws IOException {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".ear") || name.endsWith(".zip")) {
            try (JarFile jarFile = new JarFile(file)) {
                jarFile.stream().forEach(entry -> {
                    try {
                        if (entry.isDirectory()) return;
                        String entryName = entry.getName();

                        if (entryName.endsWith(".class")) {
                            saveEntry(jarFile.getInputStream(entry), new File(outputDir, entryName));
                        } else if (entryName.endsWith(".jar") || entryName.endsWith(".war") || entryName.endsWith(".ear")) {
                            String suffixString = entryName.substring(entryName.lastIndexOf(File.separator) + 1);
                            File tempFile = File.createTempFile("nested", "."+suffixString);//".zip");
                            tempFile.deleteOnExit();
                            saveEntry(jarFile.getInputStream(entry), tempFile);
                            extractFromFile(tempFile, outputDir);
                        }
                    } catch (IOException e) {
                        logger.error("Error extracting entry: " + entry.getName(), e);
                    }
                });
            }
        } else if (name.endsWith(".tar")) {
            try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new FileInputStream(file))) {
                TarArchiveEntry entry;
                while ((entry = tarIn.getNextTarEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    String entryName = entry.getName().toLowerCase();
                    File tempFile = File.createTempFile("tarEntry", null);
                    tempFile.deleteOnExit();
                    saveEntry(tarIn, tempFile);

                    if (entryName.endsWith(".class")) {
                        saveEntry(new FileInputStream(tempFile), new File(outputDir, entry.getName()));
                    } else if (entryName.endsWith(".jar") || entryName.endsWith(".war") || entryName.endsWith(".ear")) {
                        extractFromFile(tempFile, outputDir);
                    }
                }
            }
        } else {
            logger.error("Unsupported file type: " + file.getName());
        }
    }

    private static void saveEntry(InputStream in, File outFile) throws IOException {
        outFile.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(outFile)) {
            in.transferTo(out);
        }
    }
}
