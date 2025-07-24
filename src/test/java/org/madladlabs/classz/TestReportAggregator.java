package org.madladlabs.classz;

import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.reporting.ReportAggregator;

import java.nio.file.Path;
import java.util.List;

public class TestReportAggregator extends ReportAggregator {
    public TestReportAggregator(Path outputPath) {
        super(outputPath);
    }

    public List<Finding> getFindings() {
        return getFindingsList(); // <-- now uses public method
    }
}
