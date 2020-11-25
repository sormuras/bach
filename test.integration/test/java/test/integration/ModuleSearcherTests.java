package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.module.ModuleSearcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleSearcherTests {
  @ParameterizedTest
  @ValueSource(strings = {"base", "controls", "fxml", "graphics", "media", "swing", "web"})
  void checkJavaFX(String suffix) {
    var group = "org/openjfx";
    var artifact = "javafx-" + suffix;
    var version = "15";
    var classifier = ModuleSearcher.JavaFXSearcher.classifier();
    var jar = artifact + "-" + version + "-" + classifier + ".jar";
    var repository = "https://repo.maven.apache.org/maven2";
    var expected = String.join("/", repository, group, artifact, version, jar);

    var searcher = new ModuleSearcher.JavaFXSearcher(version);
    var module = "javafx." + suffix;
    var actual = searcher.search(module).orElseThrow();
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

    var searcher = new ModuleSearcher.LWJGLSearcher(version);
    var module = "org.lwjgl" + (suffix.isEmpty() ? "" : "." + suffix);
    assertEquals(expected, searcher.search(module).orElseThrow());

    var classifier = ModuleSearcher.LWJGLSearcher.classifier();
    var jarNatives = artifact + "-" + version + "-" + classifier + ".jar";
    var expectedNatives = String.join("/", repository, group, artifact, version, jarNatives);
    assertEquals(expectedNatives, searcher.search(module + ".natives").orElseThrow());
  }
}
