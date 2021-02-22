package test.integration;

import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.project.JavaStyle;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Settings;
import com.github.sormuras.bach.project.Spaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProjectTests {

  private static Project project() {
    return new Project(Settings.of("", "noname", "0"), Libraries.of(), Spaces.of());
  }

  @Test
  void defaults() {
    var project = project();
    assertEquals("noname", project.name());
    assertEquals("0", project.version());
    assertEquals(Libraries.of(), project.libraries());
    assertEquals(JavaStyle.FREE, project.spaces().style());
  }

  @Test
  void generators() {
    assertEquals("name", project().name("name").name());
    assertEquals("1-ea", project().version("1-ea+1").versionNumberAndPreRelease());
    assertEquals("1-ea+1", project().version("1-ea+1").version());
    var libraries = Libraries.of();
    assertSame(libraries, project().libraries(libraries).libraries());
    var spaces = Spaces.of();
    assertSame(spaces, project().spaces(spaces).spaces());
  }
}
