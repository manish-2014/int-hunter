package org.madladlabs.classz;

import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.reporting.IFindingWriter;

import java.util.ArrayList;
import java.util.List;

public class TestWriter implements IFindingWriter {

    private final List<Finding> findings = new ArrayList<>();

    @Override
    public void accept(Finding finding) {
        findings.add(finding);
    }

    public List<Finding> getFindings() {
        return findings;
    }
}
