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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Call;
import de.sormuras.bach.Project;
import de.sormuras.bach.call.Javac;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModulesWalkerTests {

  @Nested
  class DeSormurasBachTests {

    @Test
    void walkSelf() {
      var builder = new Project.Builder().tuner(this::tune);

      var moduleInfoFiles = Paths.find(List.of(Path.of("src")), this::isModuleFile);
      var walker = new ModulesWalker(builder, moduleInfoFiles);
      var structure = walker.newStructureWithMainTestPreviewRealms();

      var main = structure.realms().get(0);
      assertEquals("main", main.name());
      assertFalse(main.javac().isEnablePreviewLanguageFeatures());
      assertEquals(11, main.javac().getCompileForVirtualMachineVersion());

      var test = structure.realms().get(1);
      assertEquals("test", test.name());
      assertFalse(test.javac().isEnablePreviewLanguageFeatures());
      assertEquals(0, test.javac().getCompileForVirtualMachineVersion());

      var preview = structure.realms().get(2);
      assertEquals("test-preview", preview.name());
      assertTrue(preview.javac().isEnablePreviewLanguageFeatures());
      assertEquals(
          Runtime.version().feature(), preview.javac().getCompileForVirtualMachineVersion());
    }

    // src/*/${REALM}/java/module-info.java
    boolean isModuleFile(Path path) {
      return Paths.isModuleInfoJavaFile(path) && path.getNameCount() == 5;
    }

    void tune(Call call, Map<String, String> context) {
      if ("main".equals(context.get("realm"))) {
        if (call instanceof Javac) {
          ((Javac) call).setCompileForVirtualMachineVersion(11);
        }
      }
    }
  }
}
