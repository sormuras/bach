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

package de.sormuras.bach.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Interface for {@code String}-based mutable tool options. */
public interface Tool {

  /** Return name of the tool. */
  String name();

  /** Return list of arguments compiled from option properties. */
  List<String> args();

  /** Base class for common {@code String}-based mutable tool options. */
  abstract class AbstractTool implements Tool {

    private final String name;
    private boolean verbose;
    private boolean version;

    public AbstractTool(String name) {
      this.name = name;
      setVerbose(false);
      setVersion(false);
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public List<String> args() {
      var args = new ArrayList<String>();
      if (isVerbose()) args.add("--verbose");
      if (isVersion()) args.add("--version");
      return args;
    }

    public void setVerbose(boolean verbose) {
      this.verbose = verbose;
    }

    public boolean isVerbose() {
      return verbose;
    }

    public void setVersion(boolean version) {
      this.version = version;
    }

    public boolean isVersion() {
      return version;
    }

    @Override
    public String toString() {
      var args = args();
      if (args.isEmpty()) return name();
      return name() + ' ' + String.join(" ", args);
    }
  }

  /** Mutable options collection for {@code javac}. */
  class JavaCompiler extends AbstractTool {

    private Path destinationDirectory;
    private boolean generateMetadataForMethodParameters;
    private boolean terminateCompilationIfWarningsOccur;

    public JavaCompiler() {
      super("javac");
      setDestinationDirectory(null);
      setGenerateMetadataForMethodParameters(false);
      setTerminateCompilationIfWarningsOccur(false);
    }

    @Override
    public List<String> args() {
      var args = new ArrayList<String>();
      if (getDestinationDirectory() != null) {
        args.add("-d");
        args.add(destinationDirectory.toString());
      }
      if (isGenerateMetadataForMethodParameters()) args.add("-parameters");
      if (isTerminateCompilationIfWarningsOccur()) args.add("-Werror");
      if (isVerbose()) args.add("-verbose");
      if (isVersion()) args.add("-version");
      return args;
    }

    public void setDestinationDirectory(Path destinationDirectory) {
      this.destinationDirectory = destinationDirectory;
    }

    public Path getDestinationDirectory() {
      return destinationDirectory;
    }

    public void setGenerateMetadataForMethodParameters(boolean parameters) {
      this.generateMetadataForMethodParameters = parameters;
    }

    public boolean isGenerateMetadataForMethodParameters() {
      return generateMetadataForMethodParameters;
    }

    public void setTerminateCompilationIfWarningsOccur(boolean warningsAreErrors) {
      this.terminateCompilationIfWarningsOccur = warningsAreErrors;
    }

    public boolean isTerminateCompilationIfWarningsOccur() {
      return terminateCompilationIfWarningsOccur;
    }
  }
}
