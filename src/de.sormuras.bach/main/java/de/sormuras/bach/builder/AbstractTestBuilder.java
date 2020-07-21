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
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Jar;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;

abstract class AbstractTestBuilder<R> extends AbstractRealmBuilder<R> {

  AbstractTestBuilder(Bach bach, Realm<R> realm) {
    super(bach, realm);
  }

  @Override
  public void buildRealm() {
    super.buildRealm();
    buildReportsByExecutingModules();
  }

  public void buildReportsByExecutingModules() {
    // TODO...
  }

  public JUnit computeJUnitCall(SourceUnit unit, List<Path> modulePaths) {
    var module = unit.name();
    return new JUnit(module, modulePaths, List.of())
        .with("--select-module", module)
        .with("--disable-ansi-colors")
        .with("--reports-dir", base().reports("junit-" + realm().name(), module));
  }
}
