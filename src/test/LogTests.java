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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogTests {

  @Test
  void logOnAllLevels1() {
    var bach = new Bach();
    bach.log.threshold = Level.ALL;
    assertLinesMatch(List.of("a", "t", "d", "i", "w", "e"), logEmAll(bach));
  }

  @Test
  void logOnAllLevels2() {
    var bach = new Bach();
    bach.log.threshold = Level.INFO;
    assertLinesMatch(List.of("i", "w", "e"), logEmAll(bach));
  }

  static List<String> logEmAll(Bach bach) {
    var lines = new ArrayList<String>();
    bach.log.out = lines::add;
    bach.log.err = lines::add;
    bach.log.log(Level.ALL, "a");
    bach.log.log(Level.TRACE, "t");
    bach.log.log(Level.DEBUG, "d");
    bach.log.log(Level.INFO,"i");
    bach.log.log(Level.WARNING, "w");
    bach.log.log(Level.ERROR, "e");
    return lines;
  }
}
