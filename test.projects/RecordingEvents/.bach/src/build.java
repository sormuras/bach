import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.project.ProjectExternals;
import java.util.List;
import java.util.Set;

class build {
  public static void main(String... args) {
    com.github.sormuras.bach.Bach.build(
        project ->
            project
                .withName("RecordingEvents")
                .withVersion("99")
                .withTestProjectSpace(
                    test ->
                        test.withModule("foo/test/java/module-info.java")
                            .withModule("bar/test/java/module-info.java")
                            .withModulePaths(".bach/external-modules"))
                .with(
                    new ProjectExternals(
                        Set.of("org.junit.platform.console", "org.junit.platform.jfr"),
                        List.of(JUnit.V_5_7_2))));
  }
}
