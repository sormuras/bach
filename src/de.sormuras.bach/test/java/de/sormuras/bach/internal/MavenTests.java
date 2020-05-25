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

package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MavenTests {

  @Nested
  class Resources {

    @ParameterizedTest
    @ValueSource(strings = {"3.7", "4.13"})
    void centralJUnit(String version) {
      var uri = "https://repo.maven.apache.org/maven2/junit/junit/{VERSION}/junit-{VERSION}.jar";
      var expected = uri.replace("{VERSION}", version);
      var actual = Maven.central("junit", "junit", version);
      assertEquals(expected, actual);
    }

    @Test
    void customMavenURI() throws Exception {
      var expected = new URI("https", "host", "/path/g/a/v/a-v-c.t", null);
      var actual =
          new Maven.Joiner()
              .repository("https://host/path")
              .group("g")
              .artifact("a")
              .version("v")
              .classifier("c")
              .type("t")
              .toString();
      assertEquals(expected.toString(), actual);
    }
  }
}
