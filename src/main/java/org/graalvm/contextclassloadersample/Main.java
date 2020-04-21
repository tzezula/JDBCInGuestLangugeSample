package org.graalvm.contextclassloadersample;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.graalvm.polyglot.Context;


public class Main {

    private static final String SCRIPT_1 = "(function() {" +
            "var DriverManager = Java.type('org.graalvm.contextclassloadersample.JDBCConnectionProvider')\n" +
            "var found = false\n" +
            "var it = DriverManager.getDrivers()\n" +
            "while (it.hasMoreElements()) {\n" +
                "var d = it.nextElement();\n" +
                "found |= d.getClass().getName() == 'org.me.driver.DriverImpl'\n" +
            "}" +
            "return found" +
            "})()";

    private static final String SCRIPT_2 = "(function() {" +
            "var DriverManager = Java.type('org.graalvm.contextclassloadersample.JDBCConnectionProvider')\n" +
            "var found = false\n" +
            "var c = DriverManager.getConnection('myjdbc:localhost')\n" +
            "return !!c" +
            "})()";


    public static void main(String[] args) throws Exception {
        File driver = new File("lib/driver.jar"); //Jar file providing java.sql.Driver with org.me.driver.DriverImpl
        if (!driver.isFile()) {
            throw new IOException("Driver not found");
        }
        // Required because of the driver filtering done by the DriverManager.
        // The driver manager loads drivers by the `java.util.ServiceLoader`
        // which uses Thread's context class loader, but call to `getConnection`
        // filters the loaded drivers to those whose implementation class can be loaded
        // by the application (not context) class loader, see: http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/tip/src/share/classes/java/sql/DriverManager.java#l547
        // This is quite surprising behaviour which disallows isolation of JDBC driver from the application.
        ClassLoader loader = new FilterClassLoader(new URL[]{
            driver.toURI().toURL(),
            JDBCConnectionProvider.class.getProtectionDomain().getCodeSource().getLocation()
        },  JDBCConnectionProvider.class);

        Context.Builder builder = Context.newBuilder("js").hostClassLoader(loader).allowAllAccess(true);
        GraalJSScriptEngine engine = GraalJSScriptEngine.create(null, builder);
        System.out.println("Driver found: " + engine.eval(SCRIPT_1));
        System.out.println("Got connection: " + engine.eval(SCRIPT_2));
    }

    private static final class FilterClassLoader extends URLClassLoader {

        private final Set<String> entryPoints;

        FilterClassLoader(URL[] roots, Class<?>... entryPoints) {
            super(roots);
            this.entryPoints = Arrays.stream(entryPoints).map(Class::getName).collect(Collectors.toSet());
        }

        @Override
         protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
             if (entryPoints.contains(name)) {
                 synchronized (getClassLoadingLock(name)) {
                     Class<?> c = findLoadedClass(name);
                     if (c == null) {
                       c = findClass(name);
                     }
                     if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                 }
             } else {
                 return super.loadClass(name, resolve);
             }
         }
    }
}
