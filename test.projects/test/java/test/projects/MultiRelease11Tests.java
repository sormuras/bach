package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

@Disabled
class MultiRelease11Tests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("MultiRelease-11");
    assertEquals(0, project.build().waitFor());

    var jar = project.root().resolve("workspace/modules/foo@11.jar");
    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        module-info.java
        foo/
        foo/Foo.class
        foo/Foo.java
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
        META-INF/versions/16/
        META-INF/versions/16/foo/
        META-INF/versions/16/foo/Foo.class
        META-INF/versions/16/foo/Foo.java
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
