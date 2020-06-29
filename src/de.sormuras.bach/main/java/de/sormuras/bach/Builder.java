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

package de.sormuras.bach;

import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.tool.Javac;

/** An extensible build workflow. */
public class Builder {

  @FunctionalInterface
  public interface Factory {
    Builder create(Bach bach);
  }

  private final Bach bach;

  public Builder(Bach bach) {
    this.bach = bach;
  }

  public final Bach bach() {
    return bach;
  }

  public final Project project() {
    return bach.project();
  }

  public final Base base() {
    return bach.project().base();
  }

  public void build() throws Exception {
    bach.executeCall(computeJavacForMainSources());
  }

  public Javac computeJavacForMainSources() {
    return Call.javac().with("-verbose");
  }
}
