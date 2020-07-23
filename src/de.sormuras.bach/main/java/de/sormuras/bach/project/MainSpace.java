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

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** A code space for {@code main} modules. */
public final class MainSpace implements CodeSpace<MainSpace> {

  /** A modifier on a main code space. */
  public enum Modifier {
    /** Include {@code *.java} files alongside with {@code *.class} files into each modular JAR. */
    INCLUDE_SOURCES_IN_MODULAR_JAR,

    /** Include all resource files alongside with {@code *.java} files into each sources JAR. */
    INCLUDE_RESOURCES_IN_SOURCES_JAR,

    /** Generate HTML pages of API documentation from main source files. */
    API_DOCUMENTATION,

    /** Assemble and optimize main modules and their dependencies into a custom runtime image. */
    CUSTOM_RUNTIME_IMAGE
  }

  /** Tool call tweaks. */
  public static final class Tweaks {

    private final Javac.Tweak javacTweak;
    private final Javadoc.Tweak javadocTweak;

    public Tweaks(Javac.Tweak javacTweak, Javadoc.Tweak javadocTweak) {
      this.javacTweak = javacTweak;
      this.javadocTweak = javadocTweak;
    }

    public Javac.Tweak javacTweak() {
      return javacTweak;
    }

    public Javadoc.Tweak javadocTweak() {
      return javadocTweak;
    }

    @Factory
    public static Tweaks of() {
      return new Tweaks(javac -> javac, javadoc -> javadoc);
    }

    @Factory(Factory.Kind.SETTER)
    public Tweaks javacTweak(Javac.Tweak tweak) {
      return new Tweaks(tweak, javadocTweak);
    }

    @Factory(Factory.Kind.SETTER)
    public Tweaks javadocTweak(Javadoc.Tweak tweak) {
      return new Tweaks(javacTweak, tweak);
    }
  }

  private final Set<Modifier> modifiers;
  private final JavaRelease release;
  private final CodeUnits units;
  private final Tweaks tweaks;

  public MainSpace(Set<Modifier> modifiers, JavaRelease release, CodeUnits units, Tweaks tweaks) {
    this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
    this.release = release;
    this.units = units;
    this.tweaks = tweaks;
  }

  public Set<Modifier> modifiers() {
    return modifiers;
  }

  public JavaRelease release() {
    return release;
  }

  public CodeUnits units() {
    return units;
  }

  public Tweaks tweaks() {
    return tweaks;
  }

  @Factory
  public static MainSpace of() {
    return new MainSpace(
        Set.of(Modifier.API_DOCUMENTATION, Modifier.CUSTOM_RUNTIME_IMAGE),
        JavaRelease.ofRuntime(),
        CodeUnits.of(),
        Tweaks.of());
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace modifiers(Set<Modifier> modifiers) {
    return new MainSpace(modifiers, release, units, tweaks);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace release(JavaRelease release) {
    return new MainSpace(modifiers, release, units, tweaks);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace units(CodeUnits units) {
    return new MainSpace(modifiers, release, units, tweaks);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace tweaks(Tweaks tweaks) {
    return new MainSpace(modifiers, release, units, tweaks);
  }

  @Factory(Factory.Kind.OPERATOR)
  public MainSpace with(Modifier... moreModifiers) {
    var mergedModifiers = new TreeSet<>(modifiers);
    mergedModifiers.addAll(Set.of(moreModifiers));
    return modifiers(mergedModifiers);
  }

  @Factory(Factory.Kind.OPERATOR)
  public MainSpace without(Modifier... redundantModifiers) {
    var mergedModifiers = new TreeSet<>(modifiers);
    mergedModifiers.removeAll(Set.of(redundantModifiers));
    return modifiers(mergedModifiers);
  }

  @Override
  public String name() {
    return "";
  }

  public boolean is(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  public Optional<String> findMainModule() {
    return Modules.findMainModule(units.toUnits().map(CodeUnit::descriptor));
  }
}
