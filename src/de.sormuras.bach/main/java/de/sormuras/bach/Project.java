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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Scribe;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.CodeSpaces;
import de.sormuras.bach.project.CodeUnit;
import de.sormuras.bach.project.Feature;
import de.sormuras.bach.project.JavaRelease;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSpace;
import de.sormuras.bach.project.TestSpace;
import de.sormuras.bach.project.TestSpacePreview;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Bach's project model. */
public final class Project {

  private final Base base;
  private final String name;
  private final Version version;
  private final CodeSpaces spaces;
  private final Library library;

  public Project(Base base, String name, Version version, CodeSpaces spaces, Library library) {
    this.base = base;
    this.name = name;
    this.version = version;
    this.spaces = spaces;
    this.library = library;
  }

  public Base base() {
    return base;
  }

  public String name() {
    return name;
  }

  public Version version() {
    return version;
  }

  public CodeSpaces spaces() {
    return spaces;
  }

  public Library library() {
    return library;
  }

  //
  // Configuration API
  //

  @Factory
  public static Project of() {
    return new Project(Base.of(), "unnamed", Version.parse("1-ea"), CodeSpaces.of(), Library.of());
  }

  @Factory
  public static Project ofCurrentDirectory() {
    return ofDirectory(Base.of());
  }

  @Factory
  public static Project ofDirectory(Base base) {
    var main = MainSpace.of();

    try {
      var release = Integer.parseInt(System.getProperty("bach.javac.release"));
      main = main.release(JavaRelease.of(release));
    } catch (NumberFormatException e) {
      // ignore
    }

    var test = TestSpace.of();
    var preview = TestSpacePreview.of();
    for (var info : Paths.findModuleInfoJavaFiles(base.directory(), 9)) {
      if (info.startsWith(".bach")) continue;
      var unit = CodeUnit.of(info);
      if (Paths.isModuleInfoJavaFileForRealm(info, "test")) {
        test = test.with(unit);
        continue;
      }
      if (Paths.isModuleInfoJavaFileForRealm(info, "test-preview")) {
        preview = preview.with(unit);
        continue;
      }
      main = main.with(unit);
    }
    var sources = new CodeSpaces(main, test, preview);
    var absolute = base.directory().toAbsolutePath();
    var name = System.getProperty("bach.project.name", Paths.name(absolute).toLowerCase());
    var version = System.getProperty("bach.project.version", "1-ea");
    return new Project(base, name, Version.parse(version), sources, Library.of());
  }

  @Factory(Kind.SETTER)
  public Project base(Base base) {
    return new Project(base, name, version, spaces, library);
  }

  @Factory(Kind.SETTER)
  public Project name(String name) {
    return new Project(base, name, version, spaces, library);
  }

  @Factory(Kind.SETTER)
  public Project version(Version version) {
    return new Project(base, name, version, spaces, library);
  }

  @Factory(Kind.SETTER)
  public Project version(String version) {
    return version(Version.parse(version));
  }

  @Factory(Kind.SETTER)
  public Project spaces(CodeSpaces spaces) {
    return new Project(base, name, version, spaces, library);
  }

  @Factory(Kind.SETTER)
  public Project library(Library library) {
    return new Project(base, name, version, spaces, library);
  }

  @Factory(Kind.OPERATOR)
  public Project with(Feature... modifiers) {
    return spaces(spaces.main(spaces.main().with(modifiers)));
  }

  @Factory(Kind.OPERATOR)
  public Project without(Feature... modifiers) {
    return spaces(spaces.main(spaces.main().without(modifiers)));
  }

  /**
   * Compile main code space for the given Java SE release.
   *
   * <p>Compiles source code according to the rules of the Java programming language for the
   * specified Java SE release, generating class files which target that release. Source code is
   * compiled against the combined Java SE and JDK API for the specified release.
   *
   * <p>The supported values of release are the current Java SE release and a limited number of
   * previous releases, detailed in the command-line help.
   *
   * @param release the feature number of the Java release to compile for
   * @return new {@code Project} instance whose main code space is compiled the given Java release
   */
  @Factory(Kind.OPERATOR)
  public Project targetJavaRelease(int release) {
    return spaces(spaces.main(spaces.main().release(JavaRelease.of(release))));
  }

  /**
   * Add a code unit to the main space by parsing the given path for a {@code module-info.java}.
   *
   * @param path to a {@code module-info.java} file or its parent directory
   * @return new {@code Project} instance with the code unit added to its main code space
   * @see CodeUnit
   * @see MainSpace
   */
  @Factory(Kind.OPERATOR)
  public Project module(String path) {
    var unit = CodeUnit.of(Path.of(path));
    return spaces(spaces.main(spaces.main().with(unit)));
  }

  /**
   * Add a code unit to the main space by parsing the given path for a {@code module-info.java}.
   *
   * @param path to a {@code module-info.java} file or its parent directory
   * @param defaultJavaRelease to use if the directory doesn't contain a parsable release number
   * @return new {@code Project} instance with the code unit added to its main code space
   * @see CodeUnit
   * @see MainSpace
   * @see de.sormuras.bach.project.SourceFolders#of(Path, int)
   */
  @Factory(Kind.OPERATOR)
  public Project module(String path, int defaultJavaRelease) {
    var unit = CodeUnit.of(Path.of(path), defaultJavaRelease);
    return spaces(spaces.main(spaces.main().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project withTestModule(String path) {
    var unit = CodeUnit.of(Path.of(path));
    return spaces(spaces.test(spaces.test().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project withTestPreviewModule(String path) {
    var unit = CodeUnit.of(Path.of(path));
    return spaces(spaces.preview(spaces.preview().with(unit)));
  }

  @Factory(Kind.OPERATOR)
  public Project with(Link... moreLinks) {
    return library(library.with(moreLinks));
  }

  @Factory(Kind.OPERATOR)
  public Project withLibraryRequires(String... moreRequires) {
    return library(library.withRequires(moreRequires));
  }

  //
  // Normal API
  //

  public String toNameAndVersion() {
    return name + ' ' + version;
  }

  public Set<String> toDeclaredModuleNames() {
    return toUnits().map(CodeUnit::name).collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> toExternalModuleNames() {
    return Modules.external(toDeclaredModuleNames(), toRequiredModuleNames());
  }

  public Set<String> toRequiredModuleNames() {
    return Modules.required(toUnits().map(CodeUnit::descriptor));
  }

  public Stream<CodeUnit> toUnits() {
    return Stream.concat(
        spaces.main().units().toUnits(),
        Stream.concat(spaces.test().units().toUnits(), spaces.preview().units().toUnits()));
  }

  public Path toMainModuleArchive(String module) {
    return toModuleArchive("", module);
  }

  public Path toMainSourceArchive(String module) {
    return base.sources("").resolve(module + '@' + version + "-sources.jar");
  }

  public Path toMainApiDocumentationArchive() {
    return toMainApiDocumentationArchive(name);
  }

  public Path toMainApiDocumentationArchive(String module) {
    return base.documentation(module + '@' + version + "-api.jar");
  }

  public Path toModuleArchive(String realm, String module) {
    return toModuleArchive(realm, module, version);
  }

  public Path toModuleArchive(String realm, String module, Version version) {
    var suffix = realm.isEmpty() ? "" : '-' + realm;
    return base.modules(realm).resolve(module + '@' + version + suffix + ".jar");
  }

  public List<String> toStrings() {
    return new Scribe.Scroll("  ").add(this).toString().lines().collect(Collectors.toList());
  }
}
