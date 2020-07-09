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

  public Configuration(Flags flags, Logbook logbook) {
    this.flags = flags;
    this.logbook = logbook;
  }

  public Flags flags() {
    return flags;
  }

  public Logbook logbook() {
    return logbook;
  }

  //
  // Configuration API
  //

  @Factory
  public static Configuration ofSystem() {
    return new Configuration(Flags.ofSystem(), Logbook.ofSystem());
  }

  @Factory(Kind.SETTER)
  public Configuration flags(Flags flags) {
    return new Configuration(flags, logbook);
  }

  @Factory(Kind.SETTER)
  public Configuration logbook(Logbook logbook) {
    return new Configuration(flags, logbook);
  }

  @Factory(Kind.OPERATOR)
  public Configuration with(Flag... flags) {
    return flags(flags().with(flags));
  }

  @Factory(Kind.OPERATOR)
  public Configuration without(Flag... flags) {
    return flags(flags().without(flags));
  }

  @Factory(Kind.OPERATOR)
  public Configuration with(Level threshold) {
    return logbook(logbook().with(threshold));
  }

  /** A flag represents a feature toggle. */
  public enum Flag {
    DRY_RUN(false),
    FAIL_FAST(true),
    FAIL_ON_ERROR(true);

    private final boolean enabledByDefault;

    Flag(boolean enabledByDefault) {
      this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
      return enabledByDefault;
    }
  }

  /** A set of modifiers and feature toggles. */
  public static class Flags {

    public static Flags ofSystem() {
      var flags = new TreeSet<Flag>();
      for (var flag : Flag.values()) {
        var key = "bach." + flag.name().toLowerCase().replace('_', '-');
        var property = System.getProperty(key, flag.isEnabledByDefault() ? "true" : "false");
        if (Boolean.parseBoolean(property)) flags.add(flag);
      }
      return new Flags(flags);
    }

    private final Set<Flag> set;

    public Flags(Set<Flag> set) {
      this.set = set.isEmpty() ? Set.of() : EnumSet.copyOf(set);
    }

    public Set<Flag> set() {
      return set;
    }

    public boolean isDryRun() {
      return set.contains(Flag.DRY_RUN);
    }

    public boolean isFailFast() {
      return set.contains(Flag.FAIL_FAST);
    }

    public boolean isFailOnError() {
      return set.contains(Flag.FAIL_ON_ERROR);
    }

    public Flags with(Set<Flag> set) {
      return new Flags(set);
    }

    public Flags with(Flag... additionalFlags) {
      var flags = new TreeSet<>(set);
      flags.addAll(Set.of(additionalFlags));
      return with(flags);
    }

    public Flags without(Flag... redundantFlags) {
      var flags = new TreeSet<>(set());
      flags.removeAll(Set.of(redundantFlags));
      return with(flags);
    }
  }
}
