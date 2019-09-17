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
interface Domain {

  /** Modular project model. */
  class Project {
    /** Name of the project. */
    final String name;
    /** Version of the project. */
    final Version version;
    /** Library. */
    final Library library;
    /** List of realms. */
    final List<Realm> realms;

    public Project(String name, Version version, Library library, List<Realm> realms) {
      this.name = name;
      this.version = version;
      this.library = library;
      this.realms = List.copyOf(realms);
    }
  }

  /** Manage external 3rd-party modules. */
  class Library {
    /** List of library paths to external 3rd-party modules. */
    final List<Path> modulePaths;
    /** Map external 3rd-party module names to their {@code URI}s. */
    final Function<String, URI> moduleMapper;

    public Library(List<Path> modulePaths, Function<String, URI> moduleMapper) {
      this.modulePaths = List.copyOf(modulePaths);
      this.moduleMapper = moduleMapper;
    }
  }

  /** Common base class for main- and test realms. */
  abstract class Realm {
    /** Name of the realm. */
    final String name;
    /** Map of declared module source unit. */
    final Map<String, ModuleSource> modules;

    /** Module source path specifies where to find input source files for multiple modules. */
    final String moduleSourcePath;

    Realm(String name, String moduleSourcePath, Map<String, ModuleSource> modules) {
      this.name = name;
      this.modules = modules;
      this.moduleSourcePath = moduleSourcePath;
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
    final MainRealm main;

    public TestRealm(
        MainRealm main, String moduleSourcePath, Map<String, ModuleSource> declaredModules) {
      super("test", moduleSourcePath, declaredModules);
      this.main = main;
    }
  }

  /** Describes a Java module source unit. */
  class ModuleSource {
    /** Relative to the backing {@code module-info.java} file. */
    final Path info;
    /** Associated module descriptor, normally parsed from module {@link #info} file. */
    final ModuleDescriptor descriptor;

    public ModuleSource(Path info, ModuleDescriptor descriptor) {
      this.info = info;
      this.descriptor = descriptor;
    }
  }
}
