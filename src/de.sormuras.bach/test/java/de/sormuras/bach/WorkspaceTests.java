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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class WorkspaceTests {

  @Test
  void defaults() {
    var N = Runtime.version().feature();
    var workspace = Workspace.of();
    var realm = API.newRealm("realm");
    assertEquals(Path.of(""), workspace.base());
    assertEquals(Path.of(".bach/workspace"), workspace.workspace());
    assertEquals(Path.of(".bach/workspace", "first"), workspace.workspace("first"));
    assertEquals(Path.of(".bach/workspace", "first/more"), workspace.workspace("first", "more"));
    assertEquals(Path.of(".bach/workspace", "classes/realm/" + N), workspace.classes(realm));
    assertEquals(Path.of(".bach/workspace", "classes/realm/" + N), workspace.classes(realm, 0));
    assertEquals(Path.of(".bach/workspace", "classes/realm/9"), workspace.classes(realm, 9));
    assertEquals(Path.of(".bach/workspace", "modules/realm"), workspace.modules(realm));
    var project = API.emptyProject();
    var unit = API.newUnit("unit");
    assertEquals("unit-0.jar", workspace.jarFileName(project, unit, ""));
    assertEquals("unit-0-classifier.jar", workspace.jarFileName(project, unit, "classifier"));
    assertEquals(
        Path.of(".bach/workspace", "modules/realm/unit-0.jar"),
        workspace.jarFilePath(project, realm, unit));
  }

  @TestFactory
  Stream<DynamicTest> stringRepresentationContainsAllComponentNames() {
    var string = Workspace.of().toString();
    return componentNames(Workspace.class)
        .map(name -> dynamicTest(name, () -> assertTrue(string.contains(name))));
  }

  private Stream<String> componentNames(Class<?> type) {
    return Stream.of(type.getDeclaredFields()).filter(this::isComponent).map(Field::getName);
  }

  private boolean isComponent(Field field) {
    try {
      var method = field.getDeclaringClass().getMethod(field.getName());
      return method.getReturnType() == field.getType();
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
