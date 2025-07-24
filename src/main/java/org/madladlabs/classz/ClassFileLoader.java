package org.madladlabs.classz;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.nio.file.Path;

public class ClassFileLoader {
    private final Path bytecodeRoot;

    public ClassFileLoader(Path bytecodeRoot) {
        this.bytecodeRoot = bytecodeRoot;
    }

    public CtClass loadClass(String className) throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(bytecodeRoot.toAbsolutePath().toString());
        return pool.get(className);
    }
}
