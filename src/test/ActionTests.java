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
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class ActionTests {

  @Test
  void help(BachContext context) {
    Action.HELP.apply(context.bach);
    assertLinesMatch(
        List.of(
            "",
            " build     -> Build project in base directory.",
            " clean     -> Delete all generated assets - but keep caches intact.",
            " erase     -> Delete all generated assets - and also delete caches.",
            " fail      -> Set exit code to an non-zero value to fail the run.",
            " help      -> Display help screen ... F1, F1, F1!",
            " scaffold  -> Create a starter project in current directory.",
            " tool      -> Execute named tool consuming all remaining actions as arguments.",
            ""),
        context.bytesOut.toString().lines().collect(Collectors.toList()));
  }

  @TestFactory
  Stream<DynamicTest> applyToEmptyDirectory() {
    return Stream.of(Action.values())
        .map(action -> dynamicTest(action.name(), () -> applyToEmptyDirectory(action)));
  }

  private void applyToEmptyDirectory(Action action) throws Exception {
    var temp = Files.createTempDirectory("ActionTests-");
    Bach bach = new Bach(temp);
    BachContext context = new BachContext(bach);
    try {
      action.apply(bach);
    } finally {
      Util.removeTree(temp);
    }
  }
}
