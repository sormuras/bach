package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class MultiRelease9Tests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("MultiRelease-9");
    assertEquals(0, project.build().waitFor());

    var jar = project.root().resolve(".bach/workspace/modules/foo@9.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        foo/
        foo/Foo.class
        foo/Foo.java
        foo/Foo.txt
        META-INF/versions/11/
        META-INF/versions/11/foo/
        META-INF/versions/11/foo/Foo.class
        META-INF/versions/11/foo/Foo.java
        META-INF/versions/11/foo/Foo.txt
        META-INF/versions/13/
        META-INF/versions/13/foo/
        META-INF/versions/13/foo/Foo.txt
        META-INF/versions/15/
        META-INF/versions/15/foo/
        META-INF/versions/15/foo/Foo.class
        META-INF/versions/15/foo/Foo.java
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
