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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertyTests {

  @Test
  void base() {
    assertEquals(Path.of(".").toAbsolutePath().normalize(), Property.BASE);
  }

  @Test
  void noPropertiesInBaseDirectory() {
    assertEquals(Map.of(), Property.PROPERTIES);
  }

  @Test
  void loadProperties() {
    var lines = List.of("a=1", "# comment", " b = 2=II");
    assertEquals(Map.of("a", "1", "b", "2=II"), Property.load(lines.stream()));
  }

  @Test
  void loadPropertiesWithNoPropertiesYieldsAnEmptyMap() {
    var lines = List.of("#", "# comment", "", "   # with whitespace ", "");
    assertEquals(Map.of(), Property.load(lines.stream()));
  }

  @Test
  void loadPropertiesFromDirectoryFails() {
    assertThrows(UncheckedIOException.class, () -> Property.load(Path.of(".")));
  }

  @Test
  void loadPropertiesFromTestResources() {
    var path = Path.of("src", "test-resources", "Property.load.properties");
    var map = Property.load(path);
    assertEquals("true", map.get("bach.offline"));
    assertEquals("Test Project Name", map.get("project.name"));
    assertEquals("1.2.3-SNAPSHOT", map.get("project.version"));
    assertEquals("level = %s | message = %s %n", map.get("bach.log.format"));
    assertEquals(4, map.size());
  }
}
