package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.Log;
import de.sormuras.bach.Maven;
import de.sormuras.bach.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

class MavenTests {

  @Test
  void testJUnit4() {
    var log = Log.ofSystem();
    var resources = new Resources(log, HttpClient.newHttpClient());
    var maven = new Maven(log, resources, moduleMavenProperties(), moduleVersionProperties());
    assertEquals("junit:junit:4.13-beta-3", maven.lookup("junit"));
    assertEquals("junit:junit:4.12", maven.lookup("junit", "4.12"));
    assertEquals(
        "https://repo1.maven.org/maven2/junit/junit/4.12/junit-4.12.jar",
        maven.toUri("junit", "junit", "4.12").toString());
  }

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void testJUnit5() throws Exception {
    var recorder = new Recorder();
    var resources = new Resources(recorder.log, HttpClient.newHttpClient());
    var maven = new Maven(recorder.log, resources, moduleMavenProperties(), moduleVersionProperties());
    var uri = maven.toUri("org.junit.jupiter", "junit-jupiter", "5.6.0-SNAPSHOT");
    var request =
        HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(20))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build();
    var response = resources.http().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), recorder.toString());
    assertLinesMatch(List.of("Read .+/maven-metadata.xml", ">> LOG >>"), recorder.lines());
    assertLinesMatch(List.of(), recorder.errors());
  }

  // https://github.com/sormuras/modules/blob/master/module-maven.properties
  private static Properties moduleMavenProperties() {
    var properties = new Properties();
    properties.setProperty("de.sormuras.bach", "de.sormuras.bach:de.sormuras.bach");
    properties.setProperty("junit", "junit:junit");
    return properties;
  }

  // https://github.com/sormuras/modules/blob/master/module-version.properties
  private static Properties moduleVersionProperties() {
    var properties = new Properties();
    properties.setProperty("de.sormuras.bach", "1.9.7");
    properties.setProperty("junit", "4.13-beta-3");
    return properties;
  }
}
