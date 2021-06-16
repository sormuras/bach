package test.projects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Folders;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.projects.builder.ProjectBuilder;

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
            .underlay(Options.ofDefaultValues());

    var logbook = Logbook.ofErrorPrinter();
    var settings = Settings.of(options, logbook).with(folders);
    var bach = new Bach(settings, new ProjectBuilder(settings).build());

    assertDoesNotThrow(bach::buildAndWriteLogbook, () -> bach.logbook().toString());

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
