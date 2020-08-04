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
import de.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

/** An action that resursively deletes all classes directories from the workspace. */
public class DeleteClassesDirectories implements Action {

  private final Bach bach;

  public DeleteClassesDirectories(Bach bach) {
    this.bach = bach;
  }

  @Override
  public Bach bach() {
    return bach;
  }

  @Override
  public void execute() {
    var workspace = base().workspace();
    if (!Files.isDirectory(workspace)) return;
    Paths.list(workspace, this::isClassesDirectory).forEach(Paths::deleteDirectories);
  }

  private boolean isClassesDirectory(Path path) {
    return Files.isDirectory(path) && path.getFileName().toString().startsWith("classes");
  }
}
