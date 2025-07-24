package org.madladlabs.classz.reporting;

import org.madladlabs.classz.model.Finding;

public interface IFindingWriter {
    void accept(Finding finding);
}
