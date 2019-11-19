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

package de.sormuras.bach.project;

import java.util.List;

public /*record*/ class Structure {
  private final Folder folder;
  private final Library library;
  private final List<Realm> realms;
  private final List<Unit> units;

  public Structure(Folder folder, Library library, List<Realm> realms, List<Unit> units) {
    this.folder = folder;
    this.library = library;
    this.realms = List.copyOf(realms);
    this.units = List.copyOf(units);
  }

  public Folder folder() {
    return folder;
  }

  public Library library() {
    return library;
  }

  public List<Realm> realms() {
    return realms;
  }

  public List<Unit> units() {
    return units;
  }
}
