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
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class ResolvableTests {

  @Test
  void resolve(Bach.Util util) throws Exception {
    var resolvable =
        Bach.Resolvable.builder()
            .group("org.opentest4j")
            .artifact("opentest4j")
            .version("1.0.0")
            .classifier("")
            .kind("jar")
            .build();
    var temp = Files.createTempDirectory("resolve-");
    var jar = util.resolve(resolvable, temp, URI.create("http://central.maven.org/maven2"));
    assertTrue(Files.exists(jar));
    assertEquals(6588, Files.size(jar));
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
