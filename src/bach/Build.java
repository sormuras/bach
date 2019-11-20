import de.sormuras.bach.Log;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.ProjectBuilder;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

public class Build {
  public static void main(String... args) {
    var structure = ProjectBuilder.structure(Folder.of(Path.of("")));
    var project = new Project("Bach.java", Version.parse("2.0-ea"), structure);
    var bach = new de.sormuras.bach.Bach(Log.ofSystem(true), project);
    bach.execute(Task.build());
  }
}
