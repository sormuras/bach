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

package it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.bach.Call;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommandTests {
  @Test
  void addPath() {
    var call = new Call("a").add(Path.of("b/c"));
    assertEquals(List.of("a", "b" + File.separator + "c"), call.toList(true));
  }

  @Test
  void addListOfPath() {
    var noPaths = new Call("a").add("b", List.of());
    assertEquals(List.of("a"), noPaths.toList(true));
    var singlePath = new Call("a").add("b", List.of(Path.of("c")));
    assertEquals(List.of("b", "c"), singlePath.toList(false));
    var twoPaths = new Call("a").add("b", List.of(Path.of("c"), Path.of("d")));
    assertEquals(List.of("b", "c" + File.pathSeparator + "d"), twoPaths.toList(false));
  }

  @Test
  void addConditionally() {
    var iffFalse = new Call("a").iff(false, call -> call.add("b"));
    var iffTrue = new Call("a").iff(true, call -> call.add("b"));
    assertEquals(List.of("a"), iffFalse.toList(true));
    assertEquals(List.of("a", "b"), iffTrue.toList(true));
  }

  @Test
  void addOptionally() {
    var iffEmpty = new Call("a").iff(Optional.empty(), Call::add);
    var iffPresent = new Call("a").iff(Optional.of("b"), Call::add);
    assertEquals(List.of("a"), iffEmpty.toList(true));
    assertEquals(List.of("a", "b"), iffPresent.toList(true));
  }

  @Test
  void forEach() {
    var empty = new Call("a").forEach(List.of(), Call::add);
    assertEquals(List.of("a"), empty.toList(true));
    var single = new Call("a").forEach(List.of("b"), Call::add);
    assertEquals(List.of("a", "b"), single.toList(true));
    var two = new Call("a").forEach(List.of("b", "c"), Call::add);
    assertEquals(List.of("a", "b", "c"), two.toList(true));
  }

  @Test
  void toArray() {
    assertArrayEquals(new String[0], new Call("empty").toArray(false));
    assertArrayEquals(new String[] {"1"}, new Call("one").add(1).toArray(false));
    assertArrayEquals(new String[] {"2", "2"}, new Call("two").add("2", 2).toArray(false));
  }

  @Test
  void toArrayWithName() {
    assertArrayEquals(new String[] {"empty"}, new Call("empty").toArray(true));
    assertArrayEquals(new String[] {"one", "1"}, new Call("one").add(1).toArray(true));
    assertArrayEquals(new String[] {"two", "2", "2"}, new Call("two").add("2", 2).toArray(true));
  }
}
