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
  public static void main(String[] args) {
    System.out.println(IntegrationTests.class.getModule());
    System.out.println(IntegrationTests.class.getPackage());
    System.out.println("class = " + IntegrationTests.class.getSimpleName());
    System.out.println("loader = " + IntegrationTests.class.getClassLoader());
    System.out.println("args = " + java.util.Arrays.deepToString(args));

    var bach = new Bach();
    System.out.println("bach = " + bach);
    System.out.println("version = " + Bach.VERSION);
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
