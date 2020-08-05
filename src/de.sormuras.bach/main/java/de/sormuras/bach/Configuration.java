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
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

/** A configuration record. */
public final class Configuration {

  private final Flags flags;
  private final Logbook logbook;
  private final Tweak tweak;

  public Configuration(Flags flags, Logbook logbook, Tweak tweak) {
    this.flags = flags;
    this.logbook = logbook;
    this.tweak = tweak;
  }

  public Flags flags() {
    return flags;
  }

  public Logbook logbook() {
    return logbook;
  }

  public Tweak tweak() {
    return tweak;
  }

  //
  // Configuration API
  //

  @Factory
  public static Configuration ofSystem() {
    return new Configuration(Flags.ofSystem(), Logbook.ofSystem(), Tweak.identity());
  }

  @Factory(Kind.SETTER)
  public Configuration flags(Flags flags) {
    return new Configuration(flags, logbook, tweak);
  }

  @Factory(Kind.SETTER)
  public Configuration logbook(Logbook logbook) {
    return new Configuration(flags, logbook, tweak);
  }

  @Factory(Kind.SETTER)
  public Configuration tweak(Tweak tweak) {
    return new Configuration(flags, logbook, tweak);
  }

  @Factory(Kind.OPERATOR)
  public Configuration with(Flag... moreFlags) {
    return flags(flags.with(moreFlags));
  }

  @Factory(Kind.OPERATOR)
  public Configuration without(Flag... redundantFlags) {
    return flags(flags.without(redundantFlags));
  }

  @Factory(Kind.OPERATOR)
  public Configuration with(Level threshold) {
    return logbook(logbook().threshold(threshold));
  }

  /** A set of modifiers and feature toggles. */
  public static class Flags {

    private final Set<Flag> set;

    public Flags(Set<Flag> set) {
      this.set = set.isEmpty() ? Set.of() : EnumSet.copyOf(set);
    }

    public Set<Flag> set() {
      return set;
    }

    @Factory
    public static Flags ofSystem() {
      var flags = new TreeSet<Flag>();
      for (var flag : Flag.values()) {
        var key = "bach." + flag.name().toLowerCase().replace('_', '-');
        var property = System.getProperty(key, flag.isInitiallyTrue() ? "true" : "false");
        if (Boolean.parseBoolean(property)) flags.add(flag);
      }
      return new Flags(flags);
    }

    @Factory(Kind.SETTER)
    public Flags set(Set<Flag> set) {
      return new Flags(set);
    }

    @Factory(Kind.OPERATOR)
    public Flags with(Flag... moreFlags) {
      var flags = new TreeSet<>(set);
      flags.addAll(Set.of(moreFlags));
      return set(flags);
    }

    @Factory(Kind.OPERATOR)
    public Flags without(Flag... redundantFlags) {
      var flags = new TreeSet<>(set());
      flags.removeAll(Set.of(redundantFlags));
      return set(flags);
    }
  }
}
