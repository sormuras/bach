/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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

// default package

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Java Shell Builder.
 *
 * @see <a href="https://github.com/sormuras/bach">https://github.com/sormuras/bach</a>
 */
class Bach {

  /** Quiet mode switch. */
  boolean quiet = Boolean.getBoolean("bach.quiet");

  /** Logger function. */
  Consumer<String> logger = System.out::println;

  /** Log statement, if not quiet. */
  void log(String format, Object... arguments) {
    if (!quiet) {
      var message = String.format(format, arguments);
      logger.accept(message);
    }
  }

  /** Run executable with given arguments. */
  int run(String executable, Object... arguments) {
    log("[run] %s %s", executable, List.of(arguments));
    return 1;
  }

  /** Run tasks */
  int run(String caption, Stream<Supplier<Integer>> stream) {
    log("[run] %s...", caption);
    var result =
        stream
            .map(CompletableFuture::supplyAsync)
            .map(CompletableFuture::join)
            .mapToInt(t -> t)
            .sum();
    log("[run] %s done.", caption);
    if (result != 0) {
      throw new IllegalStateException("0 expected, but got: " + result);
    }
    return result;
  }
}
