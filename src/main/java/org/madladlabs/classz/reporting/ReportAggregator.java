package org.madladlabs.classz.reporting;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.madladlabs.classz.model.Finding;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReportAggregator implements IFindingWriter {

    private final Path outputPath;
    private final List<Finding> findings = new ArrayList<>();

    public ReportAggregator(Path outputPath) {
        this.outputPath = outputPath;
    }


    @Override
    public void accept(Finding f) {
        findings.add(f);
    }

    public boolean flush() throws IOException {
        findings.sort(Comparator.comparing(Finding::getClassName));

        if (outputPath.toString().endsWith(".csv")) {
            writeCsv();
        } else {
            System.err.println("Only CSV output supported in this version.");
        }
        return !findings.isEmpty();
    }

    // Add this at the bottom of the ReportAggregator class
    public List<Finding> getFindingsList() {
        return this.findings;
    }

    private void writeCsv() throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Finding.class).withHeader();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            mapper.writer(schema).writeValue(writer, findings);
        }
    }
}
