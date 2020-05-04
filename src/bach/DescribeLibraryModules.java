/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/** Load and print attributes of modular JAR files. */
class DescribeLibraryModules {

  public static void main(String... args) throws Exception {
    var modules = new DescribeLibraryModules();
    modules.mapASM("8.0.1");
    modules.mapByteBuddy("1.10.9");
    modules.mapJUnitPlatform("1.7.0-M1");
    modules.mapJUnitJupiter("5.7.0-M1");
    modules.mapJUnitVintage("5.7.0-M1");
    modules.mapVariousArtists();
    var format = "map.put(\"%s\", \"%s\");%n";
    modules.map.forEach((module, uri) -> System.out.printf(format, module, uri));
  }

  final HttpClient client = HttpClient.newHttpClient();
  final boolean fragment = false;
  final Map<String, String> map = new TreeMap<>();

  void map(String module, String gav) throws Exception {
    var split = gav.split(":");
    var group = split[0];
    var artifact = split[1];
    var version = split[2];
    var uri = central(group, artifact, version);
    var fragments = new LinkedHashMap<String, String>();
    if (fragment) {
      fragments.put("size", head(uri).headers().firstValue("Content-Length").orElseThrow());
      fragments.put("md5", read(URI.create(uri.toString() + ".md5")));
    }
    var value = uri.toString() + (fragments.isEmpty() ? "" : '#' + fragments.toString());
    map.put(module, value);
  }

  void mapVariousArtists() throws Exception {
    map("org.apiguardian.api", "org.apiguardian:apiguardian-api:1.1.0");
    map("org.assertj.core", "org.assertj:assertj-core:3.15.0");
    map("org.opentest4j", "org.opentest4j:opentest4j:1.2.0");
  }

  void mapASM(String version) throws Exception {
    mapASM("", version);
    mapASM(".commons", version);
    mapASM(".tree", version);
    mapASM(".tree.analysis", "asm-analysis", version);
    mapASM(".util", version);
  }

  void mapASM(String suffix, String version) throws Exception {
    var artifact = "asm" + suffix.replace('.', '-');
    mapASM(suffix, artifact, version);
  }

  void mapASM(String suffix, String artifact, String version) throws Exception {
    var module = "org.objectweb.asm" + suffix;
    var group = "org.ow2.asm";
    map(module, group + ':' + artifact + ':' + version);
  }

  void mapByteBuddy(String version) throws Exception {
    map("net.bytebuddy", "net.bytebuddy:byte-buddy:" + version);
    map("net.bytebuddy.agent", "net.bytebuddy:byte-buddy-agent:" + version);
  }

  void mapJUnitJupiter(String version) throws Exception {
    mapJUnitJupiter("", version);
    mapJUnitJupiter(".api", version);
    mapJUnitJupiter(".engine", version);
    mapJUnitJupiter(".params", version);
  }

  void mapJUnitJupiter(String suffix, String version) throws Exception {
    var artifact = "junit-jupiter" + suffix.replace('.', '-');
    map("org.junit.jupiter" + suffix, "org.junit.jupiter:" + artifact + ":" + version);
  }

  void mapJUnitPlatform(String version) throws Exception {
    mapJUnitPlatform(".commons", version);
    mapJUnitPlatform(".console", version);
    mapJUnitPlatform(".engine", version);
    mapJUnitPlatform(".launcher", version);
    mapJUnitPlatform(".reporting", version);
    mapJUnitPlatform(".testkit", version);
  }

  void mapJUnitPlatform(String suffix, String version) throws Exception {
    var artifact = "junit-platform" + suffix.replace('.', '-');
    map("org.junit.platform" + suffix, "org.junit.platform:" + artifact + ":" + version);
  }

  void mapJUnitVintage(String version) throws Exception {
    map("org.junit.vintage.engine", "org.junit.vintage:junit-vintage-engine:" + version);
    map("junit", "junit:junit:4.13");
    map("org.hamcrest", "org.hamcrest:hamcrest:2.2");
  }

  HttpResponse<Void> head(URI uri) throws Exception {
    var nobody = HttpRequest.BodyPublishers.noBody();
    var duration = Duration.ofSeconds(10);
    var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
    return client.send(request, HttpResponse.BodyHandlers.discarding());
  }

  String read(URI uri) throws Exception {
    var request = HttpRequest.newBuilder(uri).GET();
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
  }

  static URI central(String group, String artifact, String version) {
    var CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";
    return maven(CENTRAL_REPOSITORY, group, artifact, version, "");
  }

  static URI maven(String repository, String g, String a, String v, String classifier) {
    var filename = a + '-' + (classifier.isEmpty() ? v : v + '-' + classifier);
    var joiner = new StringJoiner("/").add(repository);
    joiner.add(g.replace('.', '/')).add(a).add(v).add(filename + ".jar");
    return URI.create(joiner.toString());
  }
}
