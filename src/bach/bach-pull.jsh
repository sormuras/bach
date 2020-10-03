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

if (!Files.deleteIfExists(Path.of(".bach/lib/.pull-in-progress"))) {
  var found = java.lang.module.ModuleFinder.of(Path.of(".bach/lib")).find("de.sormuras.bach");
  if (found.isPresent()) {
    var module = found.get();
    System.out.println("Delete module " + module.descriptor().toNameAndVersion());
    Files.delete(Path.of(module.location().orElseThrow()));
  }
  Files.createDirectories(Path.of(".bach/lib/.pull-in-progress"));
}

/open https://github.com/sormuras/bach/raw/HEAD/src/bach/bach-boot.jsh

/exit
