package org.madladlabs.classz;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassExtractor {
    private static final Logger logger = LogManager.getLogger(ClassExtractor.class);



    /**
     * Converts an archive entry such as
     *     lib/com.example.something.war
     * to a host-path like
     *     lib/com/example/something.war  (on *nix)
     *     lib\com\example\something.war  (on Windows)
     *
     *  • Inside the entry we look for .jar / .war / .ear (case-insensitive).
     *  • Dots that come *before* the extension become the platform separator.
     *  • Any '/' already present in the entry is also mapped to the host
     *    separator, so nested archive directories are preserved.
     */
    public static Path toHostPath(String entryName) {
        if (entryName == null || entryName.isEmpty()) {
            throw new IllegalArgumentException("entryName must not be null/empty");
        }

        // Normalise the entry to a forward-slash path first (JAR spec)
        String normalized = entryName.replace('\\', '/');

        int lastDot   = normalized.lastIndexOf('.');
        int lastSlash = normalized.lastIndexOf('/');

        if (lastDot == -1 || lastDot <= lastSlash) {
            // no recognised extension – map archive path dirs to host dirs verbatim
            return Paths.get(normalized.replace('/', java.io.File.separatorChar));
        }

        String ext = normalized.substring(lastDot + 1).toLowerCase();
        if (!(ext.equals("jar") || ext.equals("war") || ext.equals("ear"))) {
            // we only rewrite if it’s one of the three known container types
            return Paths.get(normalized.replace('/', java.io.File.separatorChar));
        }

        String dirPart   = (lastSlash == -1) ? "" : normalized.substring(0, lastSlash);
        String namePart  = normalized.substring(lastSlash + 1, lastDot); // no slash, no dot-ext
        String converted = namePart.replace('.', '/');                   // dots → archive slash

        // Build the archive-style path (still using '/'), then convert to host separators.
        String archiveStyle =
                (dirPart.isEmpty() ? "" : dirPart + '/') + converted + '.' + ext;

        return Paths.get(archiveStyle.replace('/', java.io.File.separatorChar));
    }


    public static void extractFromFile(File file, File outputDir) throws IOException {
        String name = file.getName().toLowerCase();
        Random random = new Random();
        if (name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".ear") || name.endsWith(".zip")) {
            try (JarFile jarFile = new JarFile(file)) {
                jarFile.stream().forEach(entry -> {
                    try {
                        if (entry.isDirectory()) return;
                        String entryName = entry.getName();

                        if (entryName.endsWith(".class")) {
                            saveEntry(jarFile.getInputStream(entry), new File(outputDir, entryName));
                        } else if (entryName.endsWith(".jar") || entryName.endsWith(".war") || entryName.endsWith(".ear")) {
                            logger.info("processing entry: "+ entryName);
                            String newName = toHostPath(entryName).toString();

                            logger.info("converted entry name: "+ newName);
                            String suffixString = newName.substring(newName.lastIndexOf(File.separator) + 1);
                            File tempFile = File.createTempFile(Integer.toString(random.nextInt()), suffixString);//".zip");
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
