package test.project;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BrokenBuildTests {

  @Test
  void buildBrokenProjectInfo(@TempDir Path temp) throws Exception {
    var context = new Context("broken-project-info", temp, 1);
    var output = context.build();
    assertLinesMatch(
        """
        java.lang.RuntimeException: javac returned error code 1
            .*module-info.java:1: error: cannot find symbol
            @ProjectInfo
             ^
              symbol: class ProjectInfo
            1 error]
        >> STACK TRACE >>
        """
            .lines(),
        output.lines());
  }
}
