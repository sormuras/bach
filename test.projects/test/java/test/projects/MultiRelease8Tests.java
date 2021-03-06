package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.api.CodeSpace;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class MultiRelease8Tests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("MultiRelease-8");
    assertEquals(0, project.build().waitFor());

    var jar = project.folders().modules(CodeSpace.MAIN, "foo@8.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        foo/
        foo/Foo.class
        foo/Foo.java
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
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
