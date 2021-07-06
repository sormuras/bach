package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.RecordComponents;
import com.github.sormuras.bach.project.ProjectDefaults;
import com.github.sormuras.bach.project.ProjectName;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.project.ProjectSpaces;
import com.github.sormuras.bach.project.ProjectVersion;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface Project {

  ProjectName name();

  ProjectVersion version();

  ProjectDefaults defaults();

  ProjectSpaces spaces();

  default String toNameAndVersion() {
    return "%s %s".formatted(name().value(), version().value().toString());
  }

  static NewProject newProject(String name, String version) {
    return new NewProject(
        new ProjectName(name),
        new ProjectVersion(ModuleDescriptor.Version.parse(version)),
        new ProjectDefaults(StandardCharsets.UTF_8),
        new ProjectSpaces(new ProjectSpace("main", ""), new ProjectSpace("test", "-test")));
  }

  record NewProject(
      ProjectName name, ProjectVersion version, ProjectDefaults defaults, ProjectSpaces spaces)
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
          component instanceof ProjectDefaults defaults ? defaults : defaults,
          component instanceof ProjectSpaces spaces ? spaces : spaces);
    }

    public NewProject withName(String name) {
      return with(new ProjectName(name));
    }

    public NewProject withVersion(String version) {
      return with(new ProjectVersion(ModuleDescriptor.Version.parse(version)));
    }

    public NewProject withDefaultSourceFileEncoding(String encoding) {
      return withDefaultSourceFileEncoding(Charset.forName(encoding));
    }

    public NewProject withDefaultSourceFileEncoding(Charset encoding) {
      return with(new ProjectDefaults(encoding));
    }

    public NewProject withMainProjectSpace(UnaryOperator<ProjectSpace> operator) {
      return with(new ProjectSpaces(operator.apply(spaces.main()), spaces.test()));
    }

    public NewProject withTestProjectSpace(UnaryOperator<ProjectSpace> operator) {
      return with(new ProjectSpaces(spaces.main(), operator.apply(spaces.test())));
    }
  }
}
