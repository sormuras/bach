/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Maven;
import de.sormuras.bach.Resources;
import de.sormuras.bach.UnmappedModuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

class MavenTests {

  @Test
  void lookupEmptyPropertiesThrows() {
    var lookup = newLookup(Map.of(), Map.of());
    assertThrows(UnmappedModuleException.class, () -> lookup.apply("foo"));
  }

  @Test
  void lookupViaCustom() {
    var lookup = new Maven.Lookup(m -> "bar", Map.of(), Map.of());
    assertEquals("bar", lookup.apply("foo"));
  }

  @Test
  void lookupViaLibrary() {
    var lookup = newLookup(Map.of("foo", "bar"), Map.of());
    assertEquals("bar", lookup.apply("foo"));
  }

  @Test
  void lookupViaPattern() {
    var lookup = newLookup(Map.of("...", "bar"), Map.of());
    assertEquals("bar", lookup.apply("foo"));
  }

  @Test
  void lookupViaFallback() {
    var lookup = newLookup(Map.of(), Map.of("foo", "bar"));
    assertEquals("bar", lookup.apply("foo"));
  }

  @Test
  void testJUnit4() {
    var recorder = new Recorder();
    var maven = newMaven(recorder);
    assertEquals("junit:junit:4.13-beta-3", maven.lookup("junit"), recorder.toString());
    assertEquals("junit:junit:4.12", maven.lookup("junit", "4.12"), recorder.toString());
    assertEquals(
        URI.create("https://repo1.maven.org/maven2/junit/junit/4.12/junit-4.12.jar"),
        maven.toUri("https://repo1.maven.org/maven2", "junit", "junit", "4.12"));
  }

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void testJUnit5() throws Exception {
    var recorder = new Recorder();
    var resources = newResources(recorder);
    var maven = newMaven(recorder, resources);
    var uri =
        maven.toUri(
            "https://oss.sonatype.org/content/repositories/snapshots",
            "org.junit.jupiter",
            "junit-jupiter",
            "5.6.0-SNAPSHOT");
    var head = resources.head(uri);
    assertEquals(200, head.statusCode(), recorder.toString());
    assertLinesMatch(List.of("Read .+/maven-metadata.xml", ">> LOG >>"), recorder.lines());
    assertLinesMatch(List.of(), recorder.errors());
  }

  private static Resources newResources(Recorder recorder) {
    var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    return new Resources(recorder.log, http);
  }

  private static Maven newMaven(Recorder recorder) {
    return newMaven(recorder, newResources(recorder));
  }

  private static Maven newMaven(Recorder recorder, Resources resources) {
    var gas =
        new Maven.Lookup(
            UnmappedModuleException::throwForString, Map.of(), moduleMavenProperties());
    var ver =
        new Maven.Lookup(
            UnmappedModuleException::throwForString, Map.of(), moduleVersionProperties());
    return new Maven(recorder.log, resources, gas, ver);
  }

  private static Maven.Lookup newLookup(Map<String, String> library, Map<String, String> fallback) {
    return new Maven.Lookup(UnmappedModuleException::throwForString, library, fallback);
  }

  // https://github.com/sormuras/modules/blob/master/module-maven.properties
  private static Map<String, String> moduleMavenProperties() {
    return Map.of("de.sormuras.bach", "de.sormuras.bach:de.sormuras.bach", "junit", "junit:junit");
  }

  // https://github.com/sormuras/modules/blob/master/module-version.properties
  private static Map<String, String> moduleVersionProperties() {
    return Map.of("de.sormuras.bach", "1.9.7", "junit", "4.13-beta-3");
  }
}
