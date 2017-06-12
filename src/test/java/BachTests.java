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

import java.util.logging.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void isInterface() {
    Assertions.assertTrue(Bach.class.isInterface());
  }

  @Test
  void builder() {
    Assertions.assertNotNull(Bach.builder());
    Assertions.assertNotNull(Bach.builder().build());
  }

  @Test
  void defaultConfiguration() {
    Bach.Configuration configuration = Bach.builder().build().configuration();
    Assertions.assertEquals("bach", configuration.name());
    Assertions.assertEquals("1.0.0-SNAPSHOT", configuration.version());
  }

  @Test
  void customConfiguration() {
    Bach.Configuration configuration =
        Bach.builder()
            .name("kernel")
            .version("4.12-rc5")
            .handler(null)
            .level(Level.WARNING)
            .build()
            .configuration();
    Assertions.assertEquals("kernel", configuration.name());
    Assertions.assertEquals("4.12-rc5", configuration.version());
  }
}
