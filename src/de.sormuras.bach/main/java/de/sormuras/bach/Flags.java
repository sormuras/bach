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

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

/** A set of feature toggles. */
public class Flags {

  public static Flags ofSystem() {
    var flags = new TreeSet<Bach.Flag>();
    for (var flag : Bach.Flag.values()) {
      var key = "bach." + flag.name().toLowerCase().replace('_', '-');
      var property = System.getProperty(key, flag.isEnabledByDefault() ? "true" : "false");
      if (Boolean.parseBoolean(property)) flags.add(flag);
    }
    return new Flags(flags);
  }

  private final Set<Bach.Flag> set;

  public Flags(Set<Bach.Flag> set) {
    this.set = set.isEmpty() ? Set.of() : EnumSet.copyOf(set);
  }

  public Set<Bach.Flag> set() {
    return set;
  }

  public boolean isDryRun() {
    return set.contains(Bach.Flag.DRY_RUN);
  }

  public boolean isFailFast() {
    return set.contains(Bach.Flag.FAIL_FAST);
  }

  public boolean isFailOnError() {
    return set.contains(Bach.Flag.FAIL_ON_ERROR);
  }

  public Flags with(Set<Bach.Flag> set) {
    return new Flags(set);
  }

  public Flags with(Bach.Flag... additionalFlags) {
    var flags = new TreeSet<>(set);
    flags.addAll(Set.of(additionalFlags));
    return with(flags);
  }

  public Flags without(Bach.Flag... redundantFlags) {
    var flags = new TreeSet<>(set());
    flags.removeAll(Set.of(redundantFlags));
    return with(flags);
  }
}
