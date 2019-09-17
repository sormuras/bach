package de.sormuras.bach;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/*BODY*/
/** Modular project model domain. */
public interface Domain {

  /** Modular project model. */
  class Project {
    /** Name of the project. */
    public final String name;
    /** Version of the project. */
    public final Version version;
    /** Library. */
    public final Library library;
    /** List of realms. */
    public final List<Realm> realms;
    /** Target directory. */
    public final Path target;

    public Project(String name, Version version, Library library, List<Realm> realms, Path target) {
      this.name = name;
      this.version = version;
      this.library = library;
      this.realms = List.copyOf(realms);
      this.target = target;
    }
  }

  /** Manage external 3rd-party modules. */
  class Library {
    /** List of library paths to external 3rd-party modules. */
    public final List<Path> modulePaths;
    /** Map external 3rd-party module names to their {@code URI}s. */
    public final Function<String, URI> moduleMapper;

    public Library(List<Path> modulePaths, Function<String, URI> moduleMapper) {
      this.modulePaths = List.copyOf(modulePaths);
      this.moduleMapper = moduleMapper;
    }
  }

  /** Common base class for main- and test realms. */
  abstract class Realm {
    /** Name of the realm. */
    public final String name;
    /** Map of declared module source unit. */
    public final Map<String, ModuleSource> modules;

    /** Module source path specifies where to find input source files for multiple modules. */
    final String moduleSourcePath;

    Realm(String name, String moduleSourcePath, Map<String, ModuleSource> modules) {
      this.name = name;
      this.moduleSourcePath = moduleSourcePath;
      this.modules = Map.copyOf(modules);
    }
  }

  /** Main realm. */
  class MainRealm extends Realm {
    public MainRealm(String moduleSourcePath, Map<String, ModuleSource> declaredModules) {
      super("main", moduleSourcePath, declaredModules);
    }
  }

  /** Test realm. */
  class TestRealm extends Realm {
    /** Main realm reference. */
    public final MainRealm main;

    public TestRealm(
        MainRealm main, String moduleSourcePath, Map<String, ModuleSource> declaredModules) {
      super("test", moduleSourcePath, declaredModules);
      this.main = main;
    }
  }

  /** Describes a Java module source unit. */
  class ModuleSource {
    /** Relative to the backing {@code module-info.java} file. */
    public final Path info;
    /** Relative path to the resources directory. */
    public final Path sources;
    /** Relative path to the resources directory. */
    public final Path resources;
    /** Associated module descriptor, normally parsed from module {@link #info} file. */
    public final ModuleDescriptor descriptor;

    public ModuleSource(Path info, Path sources, Path resources, ModuleDescriptor descriptor) {
      this.info = info;
      this.sources = sources;
      this.resources = resources;
      this.descriptor = descriptor;
    }
  }
}
