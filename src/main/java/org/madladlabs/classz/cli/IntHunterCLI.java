package org.madladlabs.classz.cli;

import org.apache.commons.cli.*;
import org.madladlabs.classz.ClassExtractor;
import org.madladlabs.classz.engine.ScanEngine;
import org.madladlabs.classz.reporting.ReportAggregator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Command-line entry point for Int-Hunter.
 *
 * Usage examples
 * --------------
 * 1) Existing behaviour – scan an exploded classes directory
 *    java -jar int-hunter.jar --classesDir /opt/app/WEB-INF/classes --out findings.csv
 *
 * 2) NEW behaviour – scan an archive (will unpack to a temp dir if --stagingDir omitted)
 *    java -jar int-hunter.jar --archiveFile /opt/app/app.war --out findings.csv
 *
 * 3) NEW behaviour – scan an archive, supplying your own staging folder
 *    java -jar int-hunter.jar --archiveFile app.war --stagingDir /tmp/ih-work --out findings.json
 */
public class IntHunterCLI {

    public static void main(String[] args) throws Exception {

        /* ────────────────────────────
         * 1. Define CLI options
         * ──────────────────────────── */
        Options options = new Options();

        // Mutually-exclusive: either --classesDir or --archiveFile must be supplied
        OptionGroup inputGroup = new OptionGroup();

        Option classesDirOpt = Option.builder()
                .longOpt("classesDir")
                .hasArg()
                .argName("dir")
                .desc("Root folder containing .class files to analyse")
                .build();

        Option archiveOpt = Option.builder()
                .longOpt("archiveFile")
                .hasArg()
                .argName("jar/war/ear/zip/tar")
                .desc("Archive to analyse (will be unpacked first)")
                .build();

        inputGroup.addOption(classesDirOpt);
        inputGroup.addOption(archiveOpt);
        inputGroup.setRequired(true);               // one of the two is mandatory
        options.addOptionGroup(inputGroup);

        // Optional staging dir (only meaningful with --archiveFile)
        options.addOption(Option.builder()
                .longOpt("stagingDir")
                .hasArg()
                .argName("dir")
                .desc("Working directory where archive will be unpacked; created if absent")
                .required(false)
                .build());

        // Output file (defaults to JSON)
        options.addOption(Option.builder()
                .longOpt("out")
                .hasArg()
                .argName("file")
                .desc("Output report (json|csv|html). Default: scan-report.json")
                .required(false)
                .build());

        /* ────────────────────────────
         * 2. Parse arguments
         * ──────────────────────────── */
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            new HelpFormatter().printHelp("int-hunter", options, true);
            System.exit(2);
            return;
        }

        /* ────────────────────────────
         * 3. Resolve input directory
         * ──────────────────────────── */
        Path classesDir;

        if (cmd.hasOption("archiveFile")) {
            // Use provided staging dir or a temp one
            Path stagingDir = cmd.hasOption("stagingDir")
                    ? Paths.get(cmd.getOptionValue("stagingDir"))
                    : Files.createTempDirectory("int-hunter-");

            Files.createDirectories(stagingDir);    // ensure exists

            File archive = new File(cmd.getOptionValue("archiveFile"));
            System.out.printf("Unpacking %s -> %s%n", archive.getAbsolutePath(), stagingDir);

            ClassExtractor.extractFromFile(archive, stagingDir.toFile());
            classesDir = stagingDir;               // scanner will point here

        } else { // --classesDir path was supplied
            classesDir = Paths.get(cmd.getOptionValue("classesDir"));
        }

        /* ────────────────────────────
         * 4. Run scan
         * ──────────────────────────── */
        Path outFile = Paths.get(cmd.getOptionValue("out", "scan-report.json"));

        ReportAggregator aggregator = new ReportAggregator(outFile);
        ScanEngine engine = new ScanEngine(aggregator);
        engine.loadExtractors();
        engine.scanDirectory(classesDir);

        boolean findingsFound = aggregator.flush(); // true if at least one issue
        System.exit(findingsFound ? 1 : 0);
    }
}
