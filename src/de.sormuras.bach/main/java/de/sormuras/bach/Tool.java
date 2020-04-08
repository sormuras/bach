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

import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** Interface for {@code String}-based mutable tool invocation objects. */
public interface Tool {

  /** Return new mutable options builder for the specified tool name. */
  static Any of(String name, Object... arguments) {
    return new Any(name, arguments);
  }

  /** Return new mutable options builder for {@code javac}. */
  static JavaCompiler javac(Object... arguments) {
    return new JavaCompiler(arguments);
  }

  @Convention
  static String join(Collection<Path> paths) {
    return paths.stream()
        .map(Path::toString) // TODO Move replace() to after join()
        .map(string -> string.replace("{MODULE}", "*"))
        .collect(Collectors.joining(File.pathSeparator));
  }

  static List<String> toStrings(String tool, String... args) {
    return toStrings(tool, List.of(args));
  }

  static List<String> toStrings(String tool, List<String> args) {
    if (args.isEmpty()) return List.of(tool);
    if (args.size() == 1) return List.of(tool + ' ' + args.get(0));
    var strings = new ArrayList<String>();
    strings.add(tool + " with " + args.size() + " arguments:");
    var simple = true;
    for (String arg : args) {
      var minus = arg.startsWith("-");
      strings.add((simple | minus ? "\t" : "\t\t") + arg);
      simple = !minus;
    }
    return List.copyOf(strings);
  }

  /** Return name of the tool to run. */
  String name();

  /** Return list of argument strings compiled from option properties. */
  List<String> args();

  default List<String> toStrings() {
    return toStrings(name(), args());
  }

  /** Any tool arguments collector. */
  class Any implements Tool {

    private final String name;
    protected final List<String> args = new ArrayList<>();

    protected Any(String name, Object... arguments) {
      this.name = name;
      addAll(arguments);
    }

    @Override
    public String name() {
      return name;
    }

    /** Append a single non-null argument. */
    public Any add(Object argument) {
      args.add(argument.toString());
      return this;
    }

    /** Append two arguments, a key and a value. */
    public Any add(String key, Object value) {
      return add(key).add(value);
    }

    /** Append three arguments, a key and two values. */
    public Any add(String key, Object first, Object second) {
      return add(key).add(first).add(second);
    }

    /** Conditionally append one or more arguments. */
    public Any add(boolean predicate, Object first, Object... more) {
      return predicate ? add(first).addAll(more) : this;
    }

    /** Append all given arguments, potentially none. */
    public Any addAll(Object... arguments) {
      for (var argument : arguments) add(argument);
      return this;
    }

    /** Walk the given iterable and expect this instance to be changed by side effects. */
    public <T> Any forEach(Iterable<T> iterable, BiConsumer<Any, T> visitor) {
      iterable.forEach(argument -> visitor.accept(this, argument));
      return this;
    }

    /** Return a new array of all collected argument strings. */
    public List<String> args() {
      return List.copyOf(args);
    }
  }

  /** Mutable options collection for {@code javac}. */
  class JavaCompiler extends Any {

    private List<String> compileModulesCheckingTimestamps;
    private Version versionOfModulesThatAreBeingCompiled;
    private List<Path> pathsWhereToFindSourceFilesForModules;
    private List<Path> pathsWhereToFindApplicationModules;
    private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
    private String characterEncodingUsedBySourceFiles;
    private int compileForVirtualMachineVersion;
    private boolean enablePreviewLanguageFeatures;
    private boolean generateMetadataForMethodParameters;
    private boolean outputMessagesAboutWhatTheCompilerIsDoing;
    private boolean outputSourceLocationsOfDeprecatedUsages;
    private boolean terminateCompilationIfWarningsOccur;
    private Path destinationDirectory;

    private JavaCompiler(Object... arguments) {
      super("javac", arguments);
    }

    @Override
    public List<String> args() {
      var args = new ArrayList<>(super.args());
      if (isAssigned(getCompileModulesCheckingTimestamps())) {
        args.add("--module");
        args.add(String.join(",", getCompileModulesCheckingTimestamps()));
      }
      if (isAssigned(getVersionOfModulesThatAreBeingCompiled())) {
        args.add("--module-version");
        args.add(String.valueOf(getVersionOfModulesThatAreBeingCompiled()));
      }
      if (isAssigned(getPathsWhereToFindSourceFilesForModules())) {
        args.add("--module-source-path");
        args.add(join(getPathsWhereToFindSourceFilesForModules()));
      }
      if (isAssigned(getPathsWhereToFindApplicationModules())) {
        args.add("--module-path");
        args.add(join(getPathsWhereToFindApplicationModules()));
      }
      if (isAssigned(getPathsWhereToFindMoreAssetsPerModule())) {
        for (var patch : getPathsWhereToFindMoreAssetsPerModule().entrySet()) {
          args.add("--patch-module");
          args.add(patch.getKey() + '=' + join(patch.getValue()));
        }
      }
      if (isAssigned(getCompileForVirtualMachineVersion())) {
        args.add("--release");
        args.add(String.valueOf(getCompileForVirtualMachineVersion()));
      }
      if (isEnablePreviewLanguageFeatures()) args.add("--enable-preview");
      if (isGenerateMetadataForMethodParameters()) args.add("-parameters");
      if (isOutputSourceLocationsOfDeprecatedUsages()) args.add("-deprecation");
      if (isOutputMessagesAboutWhatTheCompilerIsDoing()) args.add("-verbose");
      if (isTerminateCompilationIfWarningsOccur()) args.add("-Werror");
      if (isAssigned(getCharacterEncodingUsedBySourceFiles())) {
        args.add("-encoding");
        args.add(getCharacterEncodingUsedBySourceFiles());
      }
      if (isAssigned(getDestinationDirectory())) {
        args.add("-d");
        args.add(String.valueOf(getDestinationDirectory()));
      }
      return args;
    }

    public JavaCompiler setCompileModulesCheckingTimestamps(List<String> modules) {
      this.compileModulesCheckingTimestamps = modules;
      return this;
    }

    public List<String> getCompileModulesCheckingTimestamps() {
      return compileModulesCheckingTimestamps;
    }

    public JavaCompiler setPathsWhereToFindSourceFilesForModules(List<Path> moduleSourcePath) {
      this.pathsWhereToFindSourceFilesForModules = moduleSourcePath;
      return this;
    }

    public List<Path> getPathsWhereToFindSourceFilesForModules() {
      return pathsWhereToFindSourceFilesForModules;
    }

    public JavaCompiler setPathsWhereToFindApplicationModules(List<Path> modulePath) {
      this.pathsWhereToFindApplicationModules = modulePath;
      return this;
    }

    public List<Path> getPathsWhereToFindApplicationModules() {
      return pathsWhereToFindApplicationModules;
    }

    public JavaCompiler setDestinationDirectory(Path destinationDirectory) {
      this.destinationDirectory = destinationDirectory;
      return this;
    }

    public Path getDestinationDirectory() {
      return destinationDirectory;
    }

    public JavaCompiler setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> patches) {
      this.pathsWhereToFindMoreAssetsPerModule = patches;
      return this;
    }

    public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
      return pathsWhereToFindMoreAssetsPerModule;
    }

    public JavaCompiler setCharacterEncodingUsedBySourceFiles(String encoding) {
      this.characterEncodingUsedBySourceFiles = encoding;
      return this;
    }

    public String getCharacterEncodingUsedBySourceFiles() {
      return characterEncodingUsedBySourceFiles;
    }

    public JavaCompiler setCompileForVirtualMachineVersion(int release) {
      this.compileForVirtualMachineVersion = release;
      return this;
    }

    public int getCompileForVirtualMachineVersion() {
      return compileForVirtualMachineVersion;
    }

    public JavaCompiler setEnablePreviewLanguageFeatures(boolean enablePreview) {
      this.enablePreviewLanguageFeatures = enablePreview;
      return this;
    }

    public boolean isEnablePreviewLanguageFeatures() {
      return enablePreviewLanguageFeatures;
    }

    public JavaCompiler setGenerateMetadataForMethodParameters(boolean parameters) {
      this.generateMetadataForMethodParameters = parameters;
      return this;
    }

    public boolean isGenerateMetadataForMethodParameters() {
      return generateMetadataForMethodParameters;
    }

    public JavaCompiler setOutputSourceLocationsOfDeprecatedUsages(boolean deprecation) {
      this.outputSourceLocationsOfDeprecatedUsages = deprecation;
      return this;
    }

    public boolean isOutputSourceLocationsOfDeprecatedUsages() {
      return outputSourceLocationsOfDeprecatedUsages;
    }

    public JavaCompiler setOutputMessagesAboutWhatTheCompilerIsDoing(boolean verbose) {
      this.outputMessagesAboutWhatTheCompilerIsDoing = verbose;
      return this;
    }

    public boolean isOutputMessagesAboutWhatTheCompilerIsDoing() {
      return outputMessagesAboutWhatTheCompilerIsDoing;
    }

    public JavaCompiler setTerminateCompilationIfWarningsOccur(boolean warningsAreErrors) {
      this.terminateCompilationIfWarningsOccur = warningsAreErrors;
      return this;
    }

    public boolean isTerminateCompilationIfWarningsOccur() {
      return terminateCompilationIfWarningsOccur;
    }

    public JavaCompiler setVersionOfModulesThatAreBeingCompiled(Version moduleVersion) {
      this.versionOfModulesThatAreBeingCompiled = moduleVersion;
      return this;
    }

    public Version getVersionOfModulesThatAreBeingCompiled() {
      return versionOfModulesThatAreBeingCompiled;
    }
  }

  private static boolean isAssigned(Object object) {
    if (object == null) return false;
    if (object instanceof Number) return ((Number) object).intValue() != 0;
    if (object instanceof Optional) return ((Optional<?>) object).isPresent();
    if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
    return true;
  }
}
