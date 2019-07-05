package integration;

import de.sormuras.bach.Project;

import java.io.PrintWriter;
import java.nio.file.Path;

public class IntegrationTests {
  public static void main(String[] args) {
    System.out.println(Project.class + " is in " + Project.class.getModule());
    Project.help(new PrintWriter(System.out, true));
    var project = Project.of(Path.of("bach.properties"));
    System.out.println(project);
  }
}
