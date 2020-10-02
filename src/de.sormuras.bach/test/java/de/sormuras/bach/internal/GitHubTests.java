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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class GitHubTests {

  @Test
  void simplicissimus() {
    var hub = new GitHub("sormuras", "simplicissimus");
    var module = "com.github.sormuras.simplicissimus";
    assertEquals(Optional.empty(), hub.findReleasedModule(module, "0"));
    assertEquals(Optional.empty(), hub.findReleasedModule(module, "1"));
    assertEquals(Optional.empty(), hub.findReleasedModule(module, "1.3.1"));
    assertEquals(
        "https://github.com/sormuras/simplicissimus/releases/download/1.4.1/com.github.sormuras.simplicissimus@1.4.1.jar",
        hub.findReleasedModule(module, "1.4.1").orElseThrow());

    var latest = hub.findLatestReleaseTag().orElseThrow();
    assertEquals(
        "https://github.com/sormuras/simplicissimus/releases/download/"
            + latest
            + "/com.github.sormuras.simplicissimus@"
            + latest
            + ".jar",
        hub.findReleasedModule(module, latest).orElseThrow());
  }

  @Test
  void sawdust() {
    var hub = new GitHub("sormuras", "sawdust");
    assertTrue(hub.findReleasedModule("com.github.sormuras.sawdust", "0").isPresent());
    assertTrue(hub.findReleasedModule("com.github.sormuras.sawdust.api", "0").isPresent());
    assertTrue(hub.findReleasedModule("com.github.sormuras.sawdust.core", "0").isPresent());
  }
}
