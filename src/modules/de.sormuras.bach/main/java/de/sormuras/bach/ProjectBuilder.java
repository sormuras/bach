package de.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/*BODY*/
/** Build project. */
public /*STATIC*/ class ProjectBuilder {

  /** Supported properties. */
  public enum Property {
    /** Name of the project. */
    NAME("project"),

    /** Version of the project, consumable by {@link Version#parse(String)}. */
    VERSION("0");

    public final String key;
    public final String defaultValue;

    Property(String defaultValue) {
      this.key = name().toLowerCase();
      this.defaultValue = defaultValue;
    }
  }

  /** Create default project scanning the passed base directory. */
  public static Project build(Path base) {
    if (!Files.isDirectory(base)) {
      throw new IllegalArgumentException("Expected a directory but got: " + base);
    }
    return new Scanner(base).project();
  }

  static class Scanner {

    private final Path base;
    private final Properties properties;

    Scanner(Path base) {
      this.base = base;
      this.properties = Util.load(new Properties(), base.resolve(".bach").resolve(".properties"));
    }

    String get(Property property) {
      return get(property, property.defaultValue);
    }

    String get(Property property, String defaultValue) {
      return System.getProperty(property.key, properties.getProperty(property.key, defaultValue));
    }

    List<Project.ModuleUnit> units(Path src, String realm) {
      var units = new ArrayList<Project.ModuleUnit>();
      for (var module : Util.list(src, Files::isDirectory)) {
        var path = module.resolve(realm);
        if (Files.notExists(path)) {
          continue;
        }
        // jigsaw
        if (Files.isDirectory(path.resolve("java"))) {
          try {
            units.add(Project.ModuleUnit.of(path.resolve("java")));
            continue;
          } catch (IllegalArgumentException e) {
            // ignore
          }
        }
        if (!Util.list(path, "java-*").isEmpty()) {
          Project.ModuleInfo info = null;
          var sources = new ArrayList<Project.Source>();
          for (int feature = 7; feature <= Runtime.version().feature(); feature++) {
            var sourced = path.resolve("java-" + feature);
            if (Files.notExists(sourced)) {
              continue;
            }
            sources.add(Project.Source.of(sourced, feature));
            var infoPath = sourced.resolve("module-info.java");
            if (Util.isModuleInfo(infoPath)) {
              info = Project.ModuleInfo.of(infoPath);
            }
          }
          units.add(new Project.ModuleUnit(info, sources, List.of(), null));
        }
      }
      return units;
    }

    Project.Realm realm(String name, Project.Realm... realms) {
      var units = units(base.resolve("src"), name);
      return Project.Realm.of(name, units, realms);
    }

    Project project() {
      var main = realm("main");
      var test = realm("test", main);
      return new Project(
          base,
          base.resolve("bin"),
          get(Property.NAME, Util.findFileName(base).orElse(Property.NAME.defaultValue)),
          Version.parse(get(Property.VERSION)),
          new Project.Library(base.resolve("lib")),
          List.of(main, test));
    }
  }
}
