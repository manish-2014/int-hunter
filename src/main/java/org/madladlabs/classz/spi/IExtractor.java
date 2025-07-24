package org.madladlabs.classz.spi;

import javassist.CtClass;
import org.madladlabs.classz.reporting.IFindingWriter;

public interface IExtractor {
    String name();
    void process(CtClass ctClass, IFindingWriter writer) throws Exception;
}
