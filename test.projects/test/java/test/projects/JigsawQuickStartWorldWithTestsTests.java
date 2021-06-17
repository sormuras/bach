package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class JigsawQuickStartWorldWithTestsTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("JigsawQuickStartWorldWithTests");
    assertEquals(0, project.build().waitFor());

    assertLinesMatch(
        """
        # Logbook
        >>>>
        ## Log Messages

        ```text
        >>>>
        [I] Work on project JigsawQuickStartWorldWithTests 99
        >>>>
        [I] Compile 2 main modules: com.greetings, org.astro
        >>>>
        [I] Compile 1 test module: test.modules
        >>>>
        [I] Test module test.modules
        >>>>
        ```
        >>>>
        """
            .lines(),
        Files.readString(project.folders().workspace("logbook.md")).lines());
  }
}
