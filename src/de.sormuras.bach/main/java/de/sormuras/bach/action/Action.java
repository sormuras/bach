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

package de.sormuras.bach.action;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Logbook;
import de.sormuras.bach.Project;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.MainSpace;
import de.sormuras.bach.project.TestSpace;
import de.sormuras.bach.project.TestSpacePreview;
import java.lang.System.Logger.Level;

/** An action that is expected to work via side-effects. */
public interface Action {

  Bach bach();

  void execute();

  default Configuration configuration() {
    return bach().configuration();
  }

  default Project project() {
    return bach().project();
  }

  default Configuration.Flags flags() {
    return configuration().flags();
  }

  default Logbook logbook() {
    return configuration().logbook();
  }

  default String log(Level level, String text) {
    return logbook().log(level, text);
  }

  default String log(Level level, String format, Object... args) {
    return logbook().log(level, format, args);
  }

  default Base base() {
    return project().base();
  }

  default MainSpace main() {
    return project().spaces().main();
  }

  default TestSpace test() {
    return project().spaces().test();
  }

  default TestSpacePreview preview() {
    return project().spaces().preview();
  }
}
