import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.ProjectBuilder;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Build {
  public static void main(String... args) {
    var pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").withZone(ZoneId.of("UTC"));
    var version = Version.parse(pattern.format(Instant.now()));
    var structure = ProjectBuilder.structure(Folder.of(Path.of("")));
    var project = new Project("Bach.java", version, structure);
    Bach.build(Log.ofSystem(true), project);
  }
}
