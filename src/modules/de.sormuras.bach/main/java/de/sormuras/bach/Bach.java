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

package de.sormuras.bach;

import java.nio.file.Path;

/*BODY*/
/** Java Shell Builder. */
public class Bach {

  /** Version is either {@code master} or {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "master";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Convenient short-cut to {@code "user.dir"} as a path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main entry-point throwing a runtime exception on error. */
  public static void main(String... args) {
    var bach = new Bach();
    System.out.println(bach);
    System.out.println(bach.project);
  }

  final Project project = new Project("bach", VERSION);

  @Override
  public String toString() {
    return "Bach (" + VERSION + ")";
  }
}
