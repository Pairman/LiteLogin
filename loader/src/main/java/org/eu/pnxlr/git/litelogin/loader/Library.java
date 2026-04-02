package org.eu.pnxlr.git.litelogin.loader;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a dependency.
 */
@AllArgsConstructor
@Data
public class Library {
    private final String group;
    private final String name;
    private final String version;

    public static Library of(String value, String split) {
        final String[] args = value.split(split);
        return new Library(args[0], args[1], args[2]);
    }

    public String getFileName() {
        return String.format("%s-%s.jar", name, version);
    }

    public String getDownloadUrl() {
        return group.replace(".", "/")
                + "/" + name + "/" + version + "/"
                + name + "-" + version + ".jar";
    }
}
