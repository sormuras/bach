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

package integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import de.sormuras.bach.Bach;
import org.junit.jupiter.api.Test;

public class IntegrationTests {

  public static void main(String... args) {
    assert "integration".equals(IntegrationTests.class.getPackage().getName());
    assert "IntegrationTests".equals(IntegrationTests.class.getSimpleName());

    var module = IntegrationTests.class.getModule();
    if (module.isNamed()) {
      assert "integration".equals(module.getName());
    } else {
      assert null == module.getName();
      assert module.toString().startsWith("unnamed module @");
    }
  }

  @Test
  void versionIsMasterXorConsumableByRuntimeVersionParse() {
    //noinspection ConstantConditions
    if (Bach.VERSION.equals("master")) {
      return;
    }
    assertDoesNotThrow(() -> Runtime.Version.parse(Bach.VERSION));
  }
}
