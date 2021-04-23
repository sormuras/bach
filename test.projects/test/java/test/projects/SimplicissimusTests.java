package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Option;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimplicissimusTests {

  @Test
  void build() throws Exception {
    var root = Path.of("test.projects", "Simplicissimus");
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of("Simplicissimus Options")
                .with(Option.CHROOT, root)
                // "--strict"
                // "--limit-tools", "javac,jar"
                // "--jar-with-sources"
                .with(Option.VERBOSE)
                .with(Action.BUILD));

    assertTrue(bach.options().is(Option.VERBOSE));
    assertEquals(root, bach.project().folders().root());
    assertEquals("Simplicissimus", bach.project().name());
    assertEquals(0, bach.run(), bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Simplicissimus 0
        Run BUILD action
        >> BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

    var folders = bach.project().folders();
    var jar = folders.modules(CodeSpace.MAIN, "simplicissimus@0.jar");

    var classes = folders.workspace("classes");
    ToolProviders.run("javac", folders.root("module-info.java"), "-d", classes);
    Files.createDirectories(jar.getParent());
    ToolProviders.run("jar", "--create", "--file", jar, "-C", classes, ".");

    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
