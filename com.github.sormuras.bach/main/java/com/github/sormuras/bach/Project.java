package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.RecordComponents;
import com.github.sormuras.bach.project.JavaRelease;
import com.github.sormuras.bach.project.MainModules;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectName;
import com.github.sormuras.bach.project.ProjectVersion;
import com.github.sormuras.bach.project.TestModules;
import java.lang.module.ModuleDescriptor;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

public interface Project {

  ProjectName name();

  ProjectVersion version();

  MainModules mainModules();

  TestModules testModules();

  default String toNameAndVersion() {
    return "%s %s".formatted(name().value(), version().value().toString());
  }

  default String toTextBlock() {
    var mains = mainModules().set().stream().map(DeclaredModule::descriptor).map(Object::toString);
    var tests = testModules().set().stream().map(DeclaredModule::descriptor).map(Object::toString);
    return """
        Project ${NAME&VERSION}
        Main Modules
        ${MAIN MODULES}
        Test Modules
        ${TEST MODULES}
        """
        .replace("${NAME&VERSION}", toNameAndVersion())
        .replace("${MAIN MODULES}", String.join("\n", mains.toList()).indent(2).stripTrailing())
        .replace("${TEST MODULES}", String.join("\n", tests.toList()).indent(2).stripTrailing())
        .strip();
  }

  static NewProject newProject(String name, String version) {
    return new NewProject(
        new ProjectName(name),
        new ProjectVersion(ModuleDescriptor.Version.parse(version)),
        new MainModules(Set.of(), Optional.empty()),
        new TestModules(Set.of()));
  }

  record NewProject(
      ProjectName name, ProjectVersion version, MainModules mainModules, TestModules testModules)
      implements Project {

    public NewProject assertJDK(int feature) {
      return assertJDK(
          version -> version.feature() == feature,
          "Expected JDK %d but runtime version is %s".formatted(feature, Runtime.version()));
    }

    public NewProject assertJDK(Predicate<Runtime.Version> predicate, String message) {
      if (predicate.test(Runtime.version())) return this;
      throw new AssertionError(message);
    }

    public NewProject with(Object component) {
      RecordComponents.of(NewProject.class).findUnique(component.getClass()).orElseThrow();
      return new NewProject(
          component instanceof ProjectName name ? name : name,
          component instanceof ProjectVersion version ? version : version,
          component instanceof MainModules mainModules ? mainModules : mainModules,
          component instanceof TestModules testModules ? testModules : testModules);
    }

    public NewProject withName(String name) {
      return with(new ProjectName(name));
    }

    public NewProject withVersion(String version) {
      return with(new ProjectVersion(ModuleDescriptor.Version.parse(version)));
    }

    public NewProject withMainModule(String path) {
      return withMainModule(DeclaredModule.of(path));
    }

    public NewProject withMainModule(DeclaredModule module) {
      var set = new TreeSet<>(mainModules.set());
      set.add(module);
      return with(new MainModules(set, mainModules.release()));
    }

    public NewProject withCompileMainModulesForJavaRelease(int feature) {
      return with(new MainModules(mainModules.set(), Optional.of(new JavaRelease(feature))));
    }

    public NewProject withTestModule(String path) {
      return withTestModule(DeclaredModule.of(path));
    }

    public NewProject withTestModule(DeclaredModule module) {
      var set = new TreeSet<>(testModules.set());
      set.add(module);
      return with(new TestModules(set));
    }
  }
}
