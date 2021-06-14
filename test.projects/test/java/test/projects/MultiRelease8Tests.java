package test.projects;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Folders;
import org.junit.jupiter.api.Test;
import test.projects.builder.ProjectBuilder;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class MultiRelease8Tests {

  @Test
  void build() {
    var name = "MultiRelease-8";
    var root = Path.of("test.projects", name);
    var folders = Folders.of(root);
    var options =
            Options.of()
                .with("--chroot", root.toString())
                .with("--verbose", "true")
                .with("--limit-tool", "javac")
                .with("--limit-tool", "jar")
                .with("--main-java-release", "8")
                .with("--main-jar-with-sources", "true")
                .with("--workflow", "build")
                .underlay(Options.ofDefaultValues());

    var core = new Core(Logbook.ofErrorPrinter(), options, new Factory(), folders);
    var project = new ProjectBuilder(core).build();
    var bach = new Bach(core, project);

    assertEquals(0, bach.run(), () -> bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project MultiRelease-8 0
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
        META-INF/versions/9/
        META-INF/versions/9/foo/
        META-INF/versions/9/foo/Foo.class
        META-INF/versions/9/foo/Foo.java
        META-INF/versions/10/
        META-INF/versions/10/foo/
        META-INF/versions/10/foo/Foo.class
        META-INF/versions/10/foo/Foo.java
        """
            .lines()
            .sorted(),
        bach.run("jar", "--list", "--file", jar).output().lines().sorted());
  }
}
