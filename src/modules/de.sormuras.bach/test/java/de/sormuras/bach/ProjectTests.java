package de.sormuras.bach;

import java.nio.file.Path;
import java.util.List;

public class ProjectTests {
  public static void main(String[] args) {
    System.out.println(Project.class + " is in " + Project.class.getModule());
    checkProjectProperties();
  }

  private static void checkProjectProperties() {
    var project = Project.of(Path.of("bach.properties"));
    assert "bach".equals(project.name) : "expected 'bach' as name, but got: " + project.name;
    assert "2-ea".equals(project.version.toString());
    assert Path.of("").equals(project.paths.home);
    assert Path.of("src/modules").equals(project.paths.sources);
    assert List.of("de.sormuras.bach", "integration").equals(project.options.modules);
  }
}
