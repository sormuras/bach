package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Project;
import java.lang.module.ModuleDescriptor.Version;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void toNumberAndPreRelease() {
    assertEquals("1", Project.toNumberAndPreRelease(Version.parse("1")));
    assertEquals("1-ea", Project.toNumberAndPreRelease(Version.parse("1-ea")));
    assertEquals("1-ea", Project.toNumberAndPreRelease(Version.parse("1-ea+2")));
    assertEquals("1+ea", Project.toNumberAndPreRelease(Version.parse("1+ea+3")));
  }
}
