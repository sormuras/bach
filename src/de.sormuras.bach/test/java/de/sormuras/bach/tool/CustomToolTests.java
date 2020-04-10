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

package de.sormuras.bach.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.tool.Custom.Granularity;
import de.sormuras.bach.util.Strings;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CustomToolTests {

  @Test
  void custom() {
    var custom = new Custom("BEGIN", List.of(new Granularity(TimeUnit.DAYS)));
    assertThrows(NoSuchElementException.class, () -> custom.get(Option.class));
    assertEquals(TimeUnit.DAYS, custom.get(Granularity.class).unit());
    var args = custom.toArgumentStrings();
    assertLinesMatch(List.of("BEGIN", "--granularity", "DAYS", "END."), args);
    assertLinesMatch(
        List.of("custom with 4 arguments:", "\tBEGIN", "\t--granularity", "\t\tDAYS", "\tEND."),
        Strings.list(custom.name(), args));
  }
}
