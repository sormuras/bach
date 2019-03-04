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

import java.nio.file.Path;
import java.util.function.Consumer;

/** OS-agnostic build program. */
class Build {

  /** Main entry-point throwing runtime exception on error. */
  public static void main(String... args) {
    new Build().main(Throwable::printStackTrace, args);
  }

  private final Bach bach;

  Build() {
    this.bach = new Bach(true, Path.of(""));
  }

  /** Build entry-point returning non-zero exit value on error. */
  int main(Consumer<Throwable> throwableConsumer, String... args) {
    try {
      System.out.println("[main] BEGIN");
      System.out.println(bach.toString());
      Thread.sleep(1000);
      // TODO format, compile, package, document, ...
      System.out.println("[main] END.");
      return 0;
    } catch (Throwable throwable) {
      throwableConsumer.accept(throwable);
      return 1;
    }
  }
}
