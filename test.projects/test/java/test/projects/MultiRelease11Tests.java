package test.projects;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.tool.JarCall;
import org.junit.jupiter.api.Test;
import test.projects.builder.ProjectBuilder;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class MultiRelease11Tests {

  @Test
  void build() {
    var name = "MultiRelease-11";
    var root = Path.of("test.projects", name);
    var folders = Folders.of(root);
    var options = Options.of()
                            .with("--chroot", root.toString())
                            .with("--verbose", "true")
                            .with("--limit-tool", "javac")
                            .with("--limit-tool", "jar")
                            .with("--main-java-release", "11")
                            .with("--main-jar-with-sources", "true")
        .underlay(Options.ofDefaultValues());

    var core = new Core(Logbook.ofErrorPrinter(), options, new Factory(), folders);
    var project = new ProjectBuilder(core).build();
    var settings = Settings.of();
    var bach = new Bach(core, settings, project);

    assertEquals(0, bach.buildAndWriteLogbook(), () -> bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project MultiRelease-11 0
        >> INFO + BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

    var jar = bach.project().folders().modules(CodeSpace.MAIN, "foo@0.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        foo/
        foo/Foo.class
        foo/Foo.java
        module-info.java
        META-INF/versions/16/
        META-INF/versions/16/foo/
        META-INF/versions/16/foo/Foo.class
        META-INF/versions/16/foo/Foo.java
        META-INF/versions/12/
        META-INF/versions/12/foo/
        META-INF/versions/12/foo/Foo.class
        META-INF/versions/12/foo/Foo.java
        META-INF/versions/13/
        META-INF/versions/13/foo/
        META-INF/versions/13/foo/Foo.class
        META-INF/versions/13/foo/Foo.java
        META-INF/versions/14/
        META-INF/versions/14/foo/
        META-INF/versions/14/foo/Foo.class
        META-INF/versions/14/foo/Foo.java
        META-INF/versions/15/
        META-INF/versions/15/foo/
        META-INF/versions/15/foo/Foo.class
        META-INF/versions/15/foo/Foo.java
        """
            .lines()
            .sorted(),
        bach.run(new JarCall().with("--list").with("--file", jar)).output().lines().sorted());
  }
}
