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

class MultiRelease11Tests {

  @Test
  void build() {
    var name = "MultiRelease-11";
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
                --main-java-release
                  9
                --main-jar-with-sources
                build
                """
                    .formatted(root)));

    assertEquals(0, bach.run(), bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        MultiRelease-11 0
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
