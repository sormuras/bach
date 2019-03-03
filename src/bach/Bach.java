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

// default package

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Java Shell Builder. */
class Bach {

  /** {@code -Debug=true} flag. */
  boolean debug;

  /** Base path defaults to user's current working directory. */
  final Path base;

  /** Logging helper. */
  final Log log;

  /** Initialize Bach instance in current working directory. */
  Bach() {
    this(Boolean.getBoolean("ebug"), Path.of(""));
  }

  /** Initialize Bach instance in supplied working directory. */
  Bach(boolean debug, Path base) {
    this.debug = debug;
    this.base = base;
    this.log = new Log();
  }

  /** Logging helper. */
  class Log {

    /** Current logging level threshold. */
    Level threshold = debug ? Level.ALL : Level.INFO;

    /** Standard output message consumer. */
    Consumer<String> out = System.out::println;

    /** Error output stream. */
    Consumer<String> err = System.err::println;

    /** Log message unless threshold suppresses it. */
    void log(Level level, String message) {
      if (level.getSeverity() < threshold.getSeverity()) {
        return;
      }
      var consumer = level.getSeverity() < Level.WARNING.getSeverity() ? out : err;
      consumer.accept(message);
    }
  }
}
