package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.tool.JarCall;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MultiReleaseMultiModuleTests {

  @Test
  void build() {
    var name = "MultiReleaseMultiModule";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.ofCommandLineArguments(
                """
                --chroot
                  %s
                --verbose
                --limit-tools
                  javac,jar
                --main-jar-with-sources
                build
                """
                    .formatted(root)));

    assertEquals(0, bach.run(), bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project MultiReleaseMultiModule 0
        >> INFO + BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

    var api = bach.project().folders().modules(CodeSpace.MAIN, "api@0.jar");
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
        bach.run(new JarCall().with("--list").with("--file", api)).output().lines().sorted());

    var engine = bach.project().folders().modules(CodeSpace.MAIN, "engine@0.jar");
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
        bach.run(new JarCall().with("--list").with("--file", engine)).output().lines().sorted());
  }
}
