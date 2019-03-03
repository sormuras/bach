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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BachTests {

  @Test
  @ResourceLock(Resources.SYSTEM_OUT)
  @ResourceLock(Resources.SYSTEM_ERR)
  void constructDefaultInstance() {
    var bach = new Bach();

    assertFalse(bach.debug);
    assertEquals(Path.of(""), bach.base);
    assertNotNull(bach.log);
    assertNotNull(bach.log.out);
    assertNotNull(bach.log.err);
    assertSame(System.Logger.Level.INFO, bach.log.threshold);
  }

  @Test
  void constructInstanceInDebugModeAndDifferentBaseDirectory(@TempDir Path empty) {
    var bach = new Bach(true, empty);

    assertTrue(bach.debug);
    assertEquals(empty, bach.base);
    assertNotNull(bach.log);
    assertNotNull(bach.log.out);
    assertNotNull(bach.log.err);
    assertSame(System.Logger.Level.ALL, bach.log.threshold);
  }
}
