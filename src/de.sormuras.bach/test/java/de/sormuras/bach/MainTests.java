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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import test.base.SwallowSystem;

class MainTests {

  @Test
  void mainClassContainsLegitMainEntryPoint() {
    assertDoesNotThrow(() -> Main.class.getMethod("main", String[].class));
  }

  @Test
  @SwallowSystem
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void callMainMethodWithoutArguments(SwallowSystem.Streams streams) {
    try {
      System.setProperty("ebug", "");
      System.setProperty("ry-run", "");
      Main.main();
    } finally {
      System.clearProperty("ry-run");
      System.clearProperty("ebug");
    }
    try {
      var message = "TODO: Custom build program execution is not supported, yet.";
      assertTrue(streams.errors().contains(message));
      // assertTrue(streams.lines().contains("Bach.java " + Bach.VERSION));
    } catch (Throwable throwable) {
      streams.addShutdownHook(() -> System.err.println(streams));
      throw throwable;
    }
  }
}
