package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecordingEventsTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("RecordingEvents");
    assertEquals(0, project.build().waitFor());
  }
}
