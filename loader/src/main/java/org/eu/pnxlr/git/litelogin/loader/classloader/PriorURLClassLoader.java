package org.eu.pnxlr.git.litelogin.loader.classloader;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Priority class loader.
 */
public class PriorURLClassLoader extends java.net.URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final Set<String> packageName;

    public PriorURLClassLoader(URL[] urls, ClassLoader parent, Set<String> packageName) {
        super(urls, parent);
        this.packageName = new HashSet<>(packageName);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (containPrior(name)) {
                    try {
                        c = findClass(name);
                        if (resolve) resolveClass(c);
                        return c;
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            }
        }
        return super.loadClass(name, resolve);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    public boolean containPrior(String name) {
        for (String s : packageName) {
            if (name.startsWith(s)) return true;
        }
        return false;
    }
}
