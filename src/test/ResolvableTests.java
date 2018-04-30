/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(BachContext.class)
class ResolvableTests {

  @Test
  void builder() {
    var builder = new Bach.ResolvableBuilder();
    builder
        .group("group.with.dots")
        .artifact("artifact")
        .version("47.11")
        .classifier("classifier")
        .kind("kind");
    var resolvable = builder.build();
    var file = "artifact-47.11-classifier.kind";
    assertEquals("group.with.dots", resolvable.getGroup());
    assertEquals("artifact", resolvable.getArtifact());
    assertEquals("47.11", resolvable.getVersion());
    assertEquals("classifier", resolvable.getClassifier());
    assertEquals("kind", resolvable.getKind());
    assertEquals(file, resolvable.getFile());
    assertFalse(resolvable.isSnapshot());
    assertFalse(resolvable.isLatest());
    assertFalse(resolvable.isRelease());
    assertEquals("group/with/dots/artifact/47.11/" + file, resolvable.toPathString());
  }

  @ParameterizedTest
  @CsvSource({"1.0.0, 6588", "1.1.0, 6819"})
  void resolveOpenTest4J(String version, long expectedJarSize, Bach.Util util) throws Exception {
    var resolvable =
        Bach.Resolvable.builder()
            .group("org.opentest4j")
            .artifact("opentest4j")
            .version(version)
            .classifier("")
            .kind("jar")
            .build();
    var temp = Files.createTempDirectory("resolveOpenTest4J-");
    var jar = util.resolve(resolvable, temp, URI.create("http://central.maven.org/maven2"));
    assertTrue(Files.exists(jar));
    assertEquals(expectedJarSize, Files.size(jar));
    util.removeTree(temp);
  }

  @Test
  @Disabled("does not compute, yet")
  void resolveFromJitPack(BachContext context) throws Exception {
    var resolvable =
        Bach.Resolvable.builder()
            .group("com.github.sormuras")
            .artifact("bach")
            .version("master-SNAPSHOT")
            .classifier("")
            .kind("jar")
            .build();
    var temp = Files.createTempDirectory("resolveFromJitPack-");
    var jarOpt = context.bach.util.resolve(resolvable, temp);
    if (jarOpt.isPresent()) {
      var jar = jarOpt.get();
      assertTrue(Files.exists(jar));
      assertEquals(6588, Files.size(jar));
    } else {
      assertLinesMatch(List.of("1", "2"), context.recorder.all);
    }
    context.bach.util.removeTree(temp);
  }
}
