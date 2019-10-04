package de.sormuras.bach;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/*BODY*/
/** Build project. */
public /*STATIC*/ class ProjectBuilder {

    public enum Property {

        NAME("project"),

        VERSION("0");

        private final String key;
        private final String defaultValue;

        Property(String defaultValue) {
            this.key = name().toLowerCase();
            this.defaultValue = defaultValue;
        }
    }

    private static class Configuration {

        private final Properties loaded;

        Configuration(Path properties) {
            this.loaded = Util.load(new Properties(), properties);
        }

        String get(Property property) {
            return get(property, property.defaultValue);
        }

        String get(Property property, String defaultValue) {
            return System.getProperty(property.key, loaded.getProperty(property.key, defaultValue));
        }
    }

    /** Create default project parsing the passed base directory. */
    public static Project of(Path base) {
        if (!Files.isDirectory(base)) {
            throw new IllegalArgumentException("Expected a directory but got: " + base);
        }
        var configuration = new Configuration(base.resolve(".bach").resolve(".properties"));
        var main = new Project.Realm("main", false, 0, "src/*/main/java", Project.ToolArguments.of(), List.of());
        var defaultName = Optional.ofNullable(base.toAbsolutePath().getFileName()).map(Path::toString).orElse(Property.NAME.defaultValue);
        return new Project(
                base,
                base.resolve("bin"),
                configuration.get(Property.NAME, defaultName),
                ModuleDescriptor.Version.parse(configuration.get(Property.VERSION)),
                new Project.Library(base.resolve("lib")),
                List.of(main));
    }
}
