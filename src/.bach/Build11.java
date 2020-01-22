// default package

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

/** Custom build program using Bach. */
class Build11 {

  public static void main(String... args) {
    var bach = new Bach11();
    var project = bach.newProjectBuilder(Path.of("")).setVersion(Version.parse("2.3.1")).build();
    System.out.println(project);
  }
}
