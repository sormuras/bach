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

package de.sormuras.bach.call;

import de.sormuras.bach.Call;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** An abstract tool implementation providing support for shared {@code module}-related options. */
@SuppressWarnings("unchecked")
public /*static*/ abstract class GenericSourcesConsumer<T> extends Call {

  /** Value of {@code -d <directory>} option. */
  private Path destinationDirectory;
  /** Value of {@code -encoding <encoding>} option. */
  private String characterEncodingUsedBySourceFiles;
  /** Value of {@code --module <module>(,<module>)*} option. */
  private Set<String> modules;

  public GenericSourcesConsumer(String name) {
    super(name);
  }

  @Override
  protected void addConfiguredArguments(Arguments arguments) {
    var destination = getDestinationDirectory();
    if (assigned(destination)) arguments.add("-d", destination);

    var encoding = getCharacterEncodingUsedBySourceFiles();
    if (assigned(encoding)) arguments.add("-encoding", encoding);

    var modules = getModules();
    if (assigned(modules)) arguments.add("--module", String.join(",", new TreeSet<>(modules)));
  }

  /** Get value for {@code -d} option. */
  public Path getDestinationDirectory() {
    return destinationDirectory;
  }

  /** Set value for {@code -d} option. */
  public T setDestinationDirectory(Path directory) {
    this.destinationDirectory = directory;
    return (T) this;
  }

  /** Get value of {@code -encoding <encoding>} option. */
  public String getCharacterEncodingUsedBySourceFiles() {
    return characterEncodingUsedBySourceFiles;
  }

  /** Set value for {@code -encoding <encoding>} option. */
  public T setCharacterEncodingUsedBySourceFiles(String encoding) {
    this.characterEncodingUsedBySourceFiles = encoding;
    return (T) this;
  }

  /** Get value for {@code --module <module>(,<module>)*} option. */
  public Set<String> getModules() {
    return modules;
  }

  /** Set value for {@code --module <module>(,<module>)*} option. */
  public T setModules(Set<String> modules) {
    this.modules = modules;
    return (T) this;
  }
}
