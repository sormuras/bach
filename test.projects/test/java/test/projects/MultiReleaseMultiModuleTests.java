package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.api.CodeSpace;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class MultiReleaseMultiModuleTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("MultiReleaseMultiModule");
    assertEquals(0, project.build().waitFor());

    var api = project.folders().modules(CodeSpace.MAIN, "api@99.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        api/
        api/Api.class
        api/Api.java
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", api).lines().sorted());

    var engine = project.folders().modules(CodeSpace.MAIN, "engine@99.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        engine/
        engine/Main.class
        engine/Main.java
        engine/Overlay.class
        engine/Overlay.java
        engine/OverlaySingleton.class
        engine/OverlaySingleton.java
        META-INF/versions/11/
        META-INF/versions/11/engine/
        META-INF/versions/11/engine/OverlaySingleton.class
        META-INF/versions/11/engine/OverlaySingleton.java
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", engine).lines().sorted());
  }
}
