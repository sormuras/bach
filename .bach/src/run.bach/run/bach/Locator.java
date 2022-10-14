package run.bach;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import run.bach.internal.PathSupport;

/** An external modules locator maps module names to their remote locations. */
@FunctionalInterface
public interface Locator {
  String locate(String name, OperatingSystem os);

  default String locate(String name) {
    return locate(name, OperatingSystem.SYSTEM);
  }

  default String description() {
    return getClass().getSimpleName();
  }

  static Locator ofProperties(Path file) {
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

  record PropertiesLocator(String description, Properties properties) implements Locator {
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
}
