package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class ProcessingCodeTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("ProcessingCode");
    assertEquals(0, project.build().waitFor());

    assertLinesMatch(
        """
        >>>>
        # ShowProcessor.init
        >>>>
        # ShowPlugin.init
        >>>>
        # ShowDoclet.run
        >>>>
        """
            .lines(),
        Files.readString(project.root().resolve("build-out.log")).lines());
  }
}
