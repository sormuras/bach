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
    var name = "Simplicissimus";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of(name + " Options")
                .with(Option.CHROOT, root)
                // "--strict"
                // "--limit-tools", "javac,jar"
                .with(Option.VERBOSE)
                .with(Option.PROJECT_VERSION, "123")
                .with(Option.MAIN_JAVA_RELEASE, 9)
                .with(Option.MAIN_JAR_WITH_SOURCES)
                .with(Action.BUILD));

    assertTrue(bach.options().is(Option.VERBOSE));
    assertTrue(bach.options().is(Option.MAIN_JAR_WITH_SOURCES));
    assertEquals(root, bach.project().folders().root());
    assertEquals(name, bach.project().name());
    assertEquals("123", bach.project().version().toString());

    var main = bach.project().spaces().main();
    assertEquals(9, main.release());
    assertEquals(1, main.modules().size());
    assertEquals("simplicissimus", main.modules().toNames(","));
    var test = bach.project().spaces().test();
    assertEquals(0, test.modules().size());

    assertEquals(0, bach.run(), bach.logbook().toString());
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Simplicissimus 0
        run(BUILD)
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
