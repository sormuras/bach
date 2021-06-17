package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JigsawQuickStartWorldTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("JigsawQuickStartWorld");
    assertEquals(0, project.build().waitFor());
  }
}
