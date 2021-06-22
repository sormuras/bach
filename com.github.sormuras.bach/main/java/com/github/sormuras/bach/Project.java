package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleDescriptors;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

public interface Project {

  Name name();

  Version version();

  MainModules mainModules();

  TestModules testModules();

  default void build() {
    build(Bach.configureBach());
  }

  default void build(Bach bach) {
    builder(bach).build();
  }

  default Builder builder(Bach bach) {
    return bach.builder(this);
  }

  record Name(String value) {}

  record Version(ModuleDescriptor.Version value) {}

  record MainModules(Set<Module> set) {}

  record TestModules(Set<Module> set) {}

  record Module(ModuleDescriptor descriptor, URI location) implements Comparable<Module> {

    public static Module of(String string) {
      var path = Path.of(string).normalize();
      if (Files.notExists(path)) throw new IllegalArgumentException("Path must exist: " + path);
      var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
      if (Files.notExists(info)) throw new IllegalArgumentException("No module-info in: " + path);
      return new Module(ModuleDescriptors.parse(info), info.toUri());
    }

    public String name() {
      return descriptor.name();
    }

    @Override
    public int compareTo(Module other) {
      return name().compareTo(other.name());
    }
  }

  static String toString(Project project) {
    var mains = project.mainModules().set().stream().map(Module::descriptor).map(Object::toString);
    var tests = project.testModules().set().stream().map(Module::descriptor).map(Object::toString);
    return """
        Project ${NAME} ${VERSION}
        Main Modules
        ${MAIN_MODULES}
        Test Modules
        ${TEST_MODULES}
        """
        .replace("${NAME}", project.name().value())
        .replace("${VERSION}", project.version().value().toString())
        .replace("${MAIN_MODULES}", String.join("\n", mains.toList()).indent(2).stripTrailing())
        .replace("${TEST_MODULES}", String.join("\n", tests.toList()).indent(2).stripTrailing())
        .strip();
  }

  static Configuration configureProject(String name, String version) {
    return new Configuration(
        new Name(name),
        new Version(ModuleDescriptor.Version.parse(version)),
        new MainModules(Set.of()),
        new TestModules(Set.of()));
  }

  record Configuration(Name name, Version version, MainModules mainModules, TestModules testModules)
      implements Project {

    public Configuration with(Object component) {
      return new Configuration(
          component instanceof Name name ? name : name,
          component instanceof Version version ? version : version,
          component instanceof MainModules mainModules ? mainModules : mainModules,
          component instanceof TestModules testModules ? testModules : testModules);
    }

    public Configuration withName(String name) {
      return with(new Name(name));
    }

    public Configuration withVersion(String version) {
      return with(new Version(ModuleDescriptor.Version.parse(version)));
    }

    public Configuration withMainModule(String path) {
      return withMainModule(Module.of(path));
    }

    public Configuration withMainModule(Module module) {
      var set = new TreeSet<>(mainModules.set());
      set.add(module);
      return with(new MainModules(set));
    }

    public Configuration withTestModule(String path) {
      return withTestModule(Module.of(path));
    }

    public Configuration withTestModule(Module module) {
      var set = new TreeSet<>(testModules.set());
      set.add(module);
      return with(new TestModules(set));
    }
  }
}
