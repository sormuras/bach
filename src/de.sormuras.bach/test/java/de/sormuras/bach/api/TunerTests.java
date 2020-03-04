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

package de.sormuras.bach.api;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TunerTests {

  @Test
  void compiler() {
    var tool = new Tool.JavaCompiler();
    var expected = tool.args();
    new Tuner().tune(tool, null, null);
    assertLinesMatch(expected, tool.args());
  }

  @Nested
  class FineTuner extends Tuner {
    @Override
    public void tune(Tool.JavaCompiler javac, Project project, Realm realm) {
      javac.setGenerateMetadataForMethodParameters(true);
      javac.setTerminateCompilationIfWarningsOccur(true);
    }

    @Test
    void compiler() {
      var tool = new Tool.JavaCompiler();
      tune(tool, null, null);
      var args = tool.args();
      assertTrue(args.contains("-parameters"));
      assertTrue(args.contains("-Werror"));
    }
  }
}
