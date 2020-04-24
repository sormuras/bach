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
import java.util.StringJoiner;

/** Load and print attributes of modular JAR files. */
class DescribeLibraryModules {

  public static void main(String... args) throws Exception {
    var modules = new DescribeLibraryModules();
    modules.describeJUnitPlatform("1.7.0-M1");
    modules.describeJUnitJupiter("5.7.0-M1");
    modules.describeJUnitVintage("5.7.0-M1");
    modules.describeAsm("8.0.1");
  }

  final HttpClient client = HttpClient.newHttpClient();

  void describeAsm(String version) throws Exception {
    describeAsm("", version);
    describeAsm(".commons", version);
    describeAsm(".tree", version);
    describeAsm(".tree.analysis", "asm-analysis", version);
    describeAsm(".util", version);
  }

  void describeAsm(String suffix, String version) throws Exception {
    var artifact = "asm" + suffix.replace('.', '-');
    describeAsm(suffix, artifact, version);
  }

  void describeAsm(String suffix, String artifact, String version) throws Exception {
    var module = "org.objectweb.asm" + suffix;
    var group = "org.ow2.asm";
    var uri = central(group, artifact, version);
    var head = head(uri);
    var size = head.headers().firstValue("Content-Length").orElseThrow();
    var md5 = read(URI.create(uri.toString() + ".md5"));
    var gav = String.join(":", group, artifact, version);
    System.out.println(
        "  put(\""
            + module
            + "\", Maven.central(\""
            + gav
            + "\", \""
            + module
            + "\", "
            + size
            + ", \""
            + md5
            + "\"));");
  }

  void describeJUnitJupiter(String version) throws Exception {
    System.out.println("  super(\"org.junit.jupiter\", \"" + version + "\");");
    describeJUnitJupiter("", version);
    describeJUnitJupiter(".api", version);
    describeJUnitJupiter(".engine", version);
    describeJUnitJupiter(".params", version);
  }

  void describeJUnitJupiter(String suffix, String version) throws Exception {
    var artifact = "junit-jupiter" + suffix.replace('.', '-');
    var uri = central("org.junit.jupiter", artifact, version);
    var head = head(uri);
    var size = head.headers().firstValue("Content-Length").orElseThrow();
    var md5 = read(URI.create(uri.toString() + ".md5"));
    System.out.println("  put(\"" + suffix + "\", " + size + ", \"" + md5 + "\");");
  }

  void describeJUnitPlatform(String version) throws Exception {
    System.out.println("  super(\"org.junit.platform\", \"" + version + "\");");
    describeJUnitPlatform(".commons", version);
    describeJUnitPlatform(".console", version);
    describeJUnitPlatform(".engine", version);
    describeJUnitPlatform(".launcher", version);
    describeJUnitPlatform(".reporting", version);
    describeJUnitPlatform(".testkit", version);
  }

  void describeJUnitPlatform(String suffix, String version) throws Exception {
    var artifact = "junit-platform" + suffix.replace('.', '-');
    var uri = central("org.junit.platform", artifact, version);
    var head = head(uri);
    var size = head.headers().firstValue("Content-Length").orElseThrow();
    var md5 = read(URI.create(uri.toString() + ".md5"));
    System.out.println("  put(\"" + suffix + "\", " + size + ", \"" + md5 + "\");");
  }

  void describeJUnitVintage(String version) throws Exception {
    System.out.println("  super(\"org.junit.vintage\", \"" + version + "\");");
    describeJUnitVintage(".engine", version);
  }

  void describeJUnitVintage(String suffix, String version) throws Exception {
    var artifact = "junit-vintage" + suffix.replace('.', '-');
    var uri = central("org.junit.vintage", artifact, version);
    var head = head(uri);
    var size = head.headers().firstValue("Content-Length").orElseThrow();
    var md5 = read(URI.create(uri.toString() + ".md5"));
    System.out.println("  put(\"" + suffix + "\", " + size + ", \"" + md5 + "\");");
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
