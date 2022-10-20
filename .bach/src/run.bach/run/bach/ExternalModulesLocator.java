package run.bach;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import run.bach.internal.PathSupport;

/** An external modules locator maps module names to their remote locations. */
@FunctionalInterface
public interface ExternalModulesLocator {
  String locate(String name, OperatingSystem os);

  default String locate(String name) {
    return locate(name, OperatingSystem.SYSTEM);
  }

  default String description() {
    return getClass().getSimpleName();
  }

  static ExternalModulesLocator ofProperties(Path file) {
    var properties = PathSupport.properties(file);
    var annotation =
        Optional.ofNullable(properties.remove("@description"))
            .map(Object::toString)
            .orElse(PathSupport.name(file, "?").replace(".properties", ""));
    var modules = new TreeSet<String>();
    for (var name : properties.stringPropertyNames()) {
      if (name.startsWith("@")) {
        properties.remove(name);
        continue;
      }
      var index = name.indexOf('|');
      modules.add(index <= 0 ? name : name.substring(0, index));
    }
    var uri = file.toUri();
    var description = "%s [%d/%d] %s".formatted(annotation, modules.size(), properties.size(), uri);
    return new PropertiesLocator(description, properties);
  }

  record PropertiesLocator(String description, Properties properties)
      implements ExternalModulesLocator {
    @Override
    public String locate(String module, OperatingSystem os) {
      var moduleAndOperatingSystem = module + '|' + os.name();
      {
        var location = properties.getProperty(moduleAndOperatingSystem + '-' + os.architecture());
        if (location != null) return location;
      }
      {
        var location = properties.getProperty(moduleAndOperatingSystem);
        if (location != null) return location;
      }
      return properties.getProperty(module);
    }
  }

  record OperatingSystem(Name name, Architecture architecture) {

    public enum Name {
      ANY(".*"),
      LINUX("^linux.*"),
      MAC("^(macos|osx|darwin).*"),
      WINDOWS("^windows.*");

      private final String identifier;
      private final Pattern pattern;

      Name(String regex) {
        this.identifier = name().toLowerCase(Locale.ROOT);
        this.pattern = Pattern.compile(regex);
      }

      boolean matches(String string) {
        return pattern.matcher(string).matches();
      }

      @Override
      public String toString() {
        return identifier;
      }

      public static Name ofSystem() {
        return of(System.getProperty("os.name", "").toLowerCase(Locale.ROOT));
      }

      public static Name of(String string) {
        var normalized = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return Stream.of(LINUX, MAC, WINDOWS)
            .filter(platform -> platform.matches(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown operating system: " + string));
      }
    }

    public enum Architecture {
      ANY(".*"),
      ARM_32("^(arm|arm32)$"),
      ARM_64("^(aarch64|arm64)$"),
      X86_32("^(x8632|x86|i[3-6]86|ia32|x32)$"),
      X86_64("^(x8664|amd64|ia32e|em64t|x64)$");

      private final String identifier;
      private final Pattern pattern;

      Architecture(String regex) {
        this.identifier = name().toLowerCase(Locale.ROOT);
        this.pattern = Pattern.compile(regex);
      }

      boolean matches(String string) {
        return pattern.matcher(string).matches();
      }

      @Override
      public String toString() {
        return identifier;
      }

      public static Architecture ofSystem() {
        return of(System.getProperty("os.arch", ""));
      }

      public static Architecture of(String string) {
        var normalized = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return Stream.of(X86_64, ARM_64, X86_32, ARM_32)
            .filter(architecture -> architecture.matches(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown architecture: " + string));
      }
    }

    public static final OperatingSystem ANY = new OperatingSystem(Name.ANY, Architecture.ANY);

    public static final OperatingSystem SYSTEM =
        new OperatingSystem(Name.ofSystem(), Architecture.ofSystem());

    public static OperatingSystem of(String classifier) {
      if (classifier == null || classifier.isEmpty()) return ANY;
      var index = classifier.indexOf('-');
      return index == -1
          ? new OperatingSystem(Name.of(classifier), Architecture.ANY)
          : new OperatingSystem(
              Name.of(classifier.substring(0, index)),
              Architecture.of(classifier.substring(index + 1)));
    }
  }
}
