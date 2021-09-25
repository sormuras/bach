import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.external.JUnit;

class build {
  public static void main(String... args) {
    var project =
        Project.of("RecordingEvents", "99")
            .withSpaces(
                spaces ->
                    spaces.withSpace(
                        "test",
                        test -> test.withModule("bar/test/java").withModule("foo/test/java")))
            .withExternals(
                externals ->
                    externals
                        .withExternalModuleLocator(JUnit.version("5.8.1"))
                        .withRequiresModule("org.junit.platform.jfr"));

    Bach.build(project);
  }
}
