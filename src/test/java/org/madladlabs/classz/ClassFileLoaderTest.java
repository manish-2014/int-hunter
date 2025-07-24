package org.madladlabs.classz;

import javassist.CtClass;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ClassFileLoaderTest {

    @Test
    public void testLoadDBManagerClass() throws Exception {
        Path bytecodeRoot = Path.of("src/test/resources/bytecode-samples");
        ClassFileLoader loader = new ClassFileLoader(bytecodeRoot);

        CtClass ctClass = loader.loadClass("com.example.jdbc.DBManager");
        assertNotNull(ctClass);
        assertEquals("com.example.jdbc.DBManager", ctClass.getName());
    }

    @Test
    public void testLoadUserDaoClass() throws Exception {
        Path bytecodeRoot = Path.of("src/test/resources/bytecode-samples");
        ClassFileLoader loader = new ClassFileLoader(bytecodeRoot);

        CtClass ctClass = loader.loadClass("com.example.jdbcsamples.dao.UserDao");
        assertNotNull(ctClass);
        assertEquals("com.example.jdbcsamples.dao.UserDao", ctClass.getName());
    }

    @Test
    public void testSpringDBManagerClass() throws Exception {
        Path bytecodeRoot = Path.of("src/test/resources/bytecode-samples");
        ClassFileLoader loader = new ClassFileLoader(bytecodeRoot);

        CtClass ctClass = loader.loadClass("com.example.jdbcsamples.SpringDBManager");
        assertNotNull(ctClass);
        assertEquals("com.example.jdbcsamples.SpringDBManager", ctClass.getName());
    }

    @Test
    public void testHibernateUserClass() throws Exception {
        Path bytecodeRoot = Path.of("src/test/resources/bytecode-samples");
        ClassFileLoader loader = new ClassFileLoader(bytecodeRoot);

        CtClass ctClass = loader.loadClass("com.example.jdbcsamples.User");
        assertNotNull(ctClass);
        assertEquals("com.example.jdbcsamples.User", ctClass.getName());
    }
}
