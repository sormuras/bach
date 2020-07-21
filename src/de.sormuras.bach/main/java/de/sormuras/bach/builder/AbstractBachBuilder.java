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

package de.sormuras.bach.builder;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Logbook;
import de.sormuras.bach.Project;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.TestPreview;
import de.sormuras.bach.project.TestSources;
import java.lang.System.Logger.Level;

public abstract class AbstractBachBuilder {

  private final Bach bach;

  AbstractBachBuilder(Bach bach) {
    this.bach = bach;
  }

  public final Bach bach() {
    return bach;
  }

  public final Configuration configuration() {
    return bach().configuration();
  }

  public final Project project() {
    return bach().project();
  }

  public final Configuration.Flags flags() {
    return configuration().flags();
  }

  public final Logbook logbook() {
    return configuration().logbook();
  }

  public final String log(Level level, String text) {
    return logbook().log(level, text);
  }

  public final String log(Level level, String format, Object... args) {
    return logbook().log(level, format, args);
  }

  public final Base base() {
    return project().base();
  }

  public final MainSources main() {
    return project().sources().mainSources();
  }

  public final TestSources test() {
    return project().sources().testSources();
  }

  public final TestPreview preview() {
    return project().sources().testPreview();
  }

  public abstract void build();
}
