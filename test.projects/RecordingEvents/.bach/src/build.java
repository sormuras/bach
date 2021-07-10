import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.external.JUnit;

class build {
  public static void main(String... args) {
    Bach.build(
        project ->
            project
                .withName("RecordingEvents")
                .withVersion("99")
                .withTestSpace(
                    test ->
                        test.withModule("foo/test/java/module-info.java")
                            .withModule("bar/test/java/module-info.java")
                            .withModulePaths(".bach/external-modules"))
                .withExternals(
                    externals ->
                        externals
                            .withRequires("org.junit.platform.console", "org.junit.platform.jfr")
                            .with(JUnit.V_5_7_2)));
  }
}
