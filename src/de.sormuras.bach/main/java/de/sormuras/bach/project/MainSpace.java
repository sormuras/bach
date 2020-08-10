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
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import de.sormuras.bach.tool.Jlink;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** A code space for {@code main} modules. */
public final class MainSpace implements CodeSpace<MainSpace> {

  /** Tool call tweaks. */
  @Deprecated(forRemoval = true)
  public static final class Tweaks {

    private final Jar.Tweak jarTweak;
    private final Javac.Tweak javacTweak;
    private final Javadoc.Tweak javadocTweak;
    private final Jlink.Tweak jlinkTweak;

    public Tweaks(
        Jar.Tweak jarTweak,
        Javac.Tweak javacTweak,
        Javadoc.Tweak javadocTweak,
        Jlink.Tweak jlinkTweak) {
      this.jarTweak = jarTweak;
      this.javacTweak = javacTweak;
      this.javadocTweak = javadocTweak;
      this.jlinkTweak = jlinkTweak;
    }

    public Jar.Tweak jarTweak() {
      return jarTweak;
    }

    public Javac.Tweak javacTweak() {
      return javacTweak;
    }

    public Javadoc.Tweak javadocTweak() {
      return javadocTweak;
    }

    public Jlink.Tweak jlinkTweak() {
      return jlinkTweak;
    }

    @Factory
    public static Tweaks of() {
      return new Tweaks(jar -> jar, javac -> javac, javadoc -> javadoc, jlink -> jlink);
    }

    @Factory(Factory.Kind.SETTER)
    public Tweaks jarTweak(Jar.Tweak tweak) {
      return new Tweaks(tweak, javacTweak, javadocTweak, jlinkTweak);
    }

    @Factory(Factory.Kind.SETTER)
    public Tweaks javacTweak(Javac.Tweak tweak) {
      return new Tweaks(jarTweak, tweak, javadocTweak, jlinkTweak);
    }

    @Factory(Factory.Kind.SETTER)
    public Tweaks javadocTweak(Javadoc.Tweak tweak) {
      return new Tweaks(jarTweak, javacTweak, tweak, jlinkTweak);
    }

    @Factory(Factory.Kind.SETTER)
    public Tweaks jlinkTweak(Jlink.Tweak tweak) {
      return new Tweaks(jarTweak, javacTweak, javadocTweak, tweak);
    }
  }

  private final Set<Feature> features;
  private final JavaRelease release;
  private final CodeUnits units;
  private final Tweaks tweaks;

  public MainSpace(Set<Feature> features, JavaRelease release, CodeUnits units, Tweaks tweaks) {
    this.features = features.isEmpty() ? Set.of() : EnumSet.copyOf(features);
    this.release = release;
    this.units = units;
    this.tweaks = tweaks;
  }

  public Set<Feature> features() {
    return features;
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
    return new MainSpace(Feature.DEFAULTS, JavaRelease.ofRuntime(), CodeUnits.of(), Tweaks.of());
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace features(Set<Feature> features) {
    return new MainSpace(features, release, units, tweaks);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace release(JavaRelease release) {
    return new MainSpace(features, release, units, tweaks);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace units(CodeUnits units) {
    return new MainSpace(features, release, units, tweaks);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace tweaks(Tweaks tweaks) {
    return new MainSpace(features, release, units, tweaks);
  }

  @Factory(Factory.Kind.OPERATOR)
  public MainSpace with(Feature... moreFeatures) {
    var mergedFeatures = new TreeSet<>(features);
    mergedFeatures.addAll(Set.of(moreFeatures));
    return features(mergedFeatures);
  }

  @Factory(Factory.Kind.OPERATOR)
  public MainSpace without(Feature... redundantFeatures) {
    var mergedFeatures = new TreeSet<>(features);
    mergedFeatures.removeAll(Set.of(redundantFeatures));
    return features(mergedFeatures);
  }

  @Override
  public String name() {
    return "";
  }

  public boolean is(Feature feature) {
    return features.contains(feature);
  }

  public Optional<String> findMainModule() {
    return Modules.findMainModule(units.toUnits().map(CodeUnit::descriptor));
  }
}
