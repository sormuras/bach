package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.project.ModuleLookup;
import com.github.sormuras.bach.project.ModuleLookups;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleLookupTests {
  @ParameterizedTest
  @ValueSource(strings = {"base", "controls", "fxml", "graphics", "media", "swing", "web"})
  void checkJavaFX(String suffix) {
    var group = "org/openjfx";
    var artifact = "javafx-" + suffix;
    var version = "15";
    var classifier = ModuleLookup.JavaFX.classifier();
    var jar = artifact + "-" + version + "-" + classifier + ".jar";
    var repository = "https://repo.maven.apache.org/maven2";
    var expected = String.join("/", repository, group, artifact, version, jar);

    var lookup = new ModuleLookup.JavaFX(version);
    var module = "javafx." + suffix;
    var actual = lookup.lookup(module).orElseThrow();
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "openal", "opencl", "opengl", "opengles", "openvr"})
  void checkLWJGL(String suffix) {
    var group = "org/lwjgl";
    var artifact = "lwjgl" + (suffix.isEmpty() ? "" : "-" + suffix);
    var version = "3.2.3";
    var jar = artifact + "-" + version + ".jar";
    var repository = "https://repo.maven.apache.org/maven2";
    var expected = String.join("/", repository, group, artifact, version, jar);

    var lookup = new ModuleLookups.LWJGL_3_2_3();
    var module = "org.lwjgl" + (suffix.isEmpty() ? "" : "." + suffix);
    assertEquals(expected, lookup.lookup(module).orElseThrow());

    var classifier = ModuleLookup.LWJGL.classifier();
    var jarNatives = artifact + "-" + version + "-" + classifier + ".jar";
    var expectedNatives = String.join("/", repository, group, artifact, version, jarNatives);
    assertEquals(expectedNatives, lookup.lookup(module + ".natives").orElseThrow());
  }
}
