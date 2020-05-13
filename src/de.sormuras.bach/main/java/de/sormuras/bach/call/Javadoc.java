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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** A call to {@code javadoc}, the Java API documentation generating tool. */
public /*static*/ class Javadoc extends GenericSourcesConsumer<Javadoc> {

  private List<String> patternsWhereToFindSourceFiles;
  private Map<String, List<Path>> pathsWhereToFindSourceFiles;
  private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
  private List<Path> pathsWhereToFindApplicationModules;
  private String characterEncodingUsedBySourceFiles;
  private int compileForVirtualMachineVersion;
  private boolean enablePreviewLanguageFeatures;
  private boolean outputMessagesAboutWhatJavadocIsDoing;

  private boolean shutOffDisplayingStatusMessages;

  public Javadoc() {
    super("javadoc");
  }

  @Override
  public String toLabel() {
    return "Generate API documentation for " + getModules();
  }

  @Override
  protected void addConfiguredArguments(Arguments arguments) {
    super.addConfiguredArguments(arguments);

    var patterns = getPatternsWhereToFindSourceFiles();
    if (assigned(patterns)) arguments.add("--module-source-path", joinPaths(patterns));

    var specific = getPathsWhereToFindSourceFiles();
    if (assigned(specific))
      for (var entry : specific.entrySet())
        arguments.add("--module-source-path", entry.getKey() + '=' + join(entry.getValue()));

    var patches = getPathsWhereToFindMoreAssetsPerModule();
    if (assigned(patches))
      for (var patch : patches.entrySet())
        arguments.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));

    var modulePath = getPathsWhereToFindApplicationModules();
    if (assigned(modulePath)) arguments.add("--module-path", join(modulePath));

    var encoding = getCharacterEncodingUsedBySourceFiles();
    if (assigned(encoding)) arguments.add("-encoding", encoding);

    var release = getCompileForVirtualMachineVersion();
    if (assigned(release)) arguments.add("--release", release);

    if (isEnablePreviewLanguageFeatures()) arguments.add("--enable-preview");

    if (isOutputMessagesAboutWhatJavadocIsDoing()) arguments.add("-verbose");

    if (isShutOffDisplayingStatusMessages()) arguments.add("-quiet");
  }

  public List<String> getPatternsWhereToFindSourceFiles() {
    return patternsWhereToFindSourceFiles;
  }

  public Javadoc setPatternsWhereToFindSourceFiles(List<String> patterns) {
    this.patternsWhereToFindSourceFiles = patterns;
    return this;
  }

  public Map<String, List<Path>> getPathsWhereToFindSourceFiles() {
    return pathsWhereToFindSourceFiles;
  }

  public Javadoc setPathsWhereToFindSourceFiles(Map<String, List<Path>> map) {
    this.pathsWhereToFindSourceFiles = map;
    return this;
  }

  public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
    return pathsWhereToFindMoreAssetsPerModule;
  }

  public Javadoc setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> map) {
    this.pathsWhereToFindMoreAssetsPerModule = map;
    return this;
  }

  public List<Path> getPathsWhereToFindApplicationModules() {
    return pathsWhereToFindApplicationModules;
  }

  public Javadoc setPathsWhereToFindApplicationModules(List<Path> paths) {
    this.pathsWhereToFindApplicationModules = paths;
    return this;
  }

  public String getCharacterEncodingUsedBySourceFiles() {
    return characterEncodingUsedBySourceFiles;
  }

  public Javadoc setCharacterEncodingUsedBySourceFiles(String encoding) {
    this.characterEncodingUsedBySourceFiles = encoding;
    return this;
  }

  public int getCompileForVirtualMachineVersion() {
    return compileForVirtualMachineVersion;
  }

  public Javadoc setCompileForVirtualMachineVersion(int release) {
    this.compileForVirtualMachineVersion = release;
    return this;
  }

  public boolean isEnablePreviewLanguageFeatures() {
    return enablePreviewLanguageFeatures;
  }

  public Javadoc setEnablePreviewLanguageFeatures(boolean preview) {
    this.enablePreviewLanguageFeatures = preview;
    return this;
  }

  public boolean isOutputMessagesAboutWhatJavadocIsDoing() {
    return outputMessagesAboutWhatJavadocIsDoing;
  }

  public Javadoc setOutputMessagesAboutWhatJavadocIsDoing(boolean verbose) {
    this.outputMessagesAboutWhatJavadocIsDoing = verbose;
    return this;
  }

  public boolean isShutOffDisplayingStatusMessages() {
    return shutOffDisplayingStatusMessages;
  }

  public Javadoc setShutOffDisplayingStatusMessages(boolean quiet) {
    this.shutOffDisplayingStatusMessages = quiet;
    return this;
  }
}
