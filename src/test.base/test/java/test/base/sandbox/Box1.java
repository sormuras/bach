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

package test.base.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public interface Box1 {

  final class Realm {

    private final String name;
    private final int release;
    private final boolean preview;
    private final List<Unit> units;

    public Realm(String name, int release, boolean preview, List<Unit> units) {
      this.name = name;
      this.release = release;
      this.preview = preview;
      this.units = units;
    }
  }

  final class Unit {

    static Unit of(String name, Vault... vaults) {
      return new Unit(name, 0, List.of(vaults));
    }

    private final String name;
    private final int release;
    private final List<Vault> vaults;

    public Unit(String name, int release, List<Vault> vaults) {
      this.name = name;
      this.release = release;
      this.vaults = vaults;
    }
  }

  final class Vault {

    enum Flag {}

    static int javaReleaseFeatureNumber(String name) {
      try {
        return Integer.parseInt(name);
      } catch (NumberFormatException e) {
        return 0;
      }
    }

    static Vault of(Path path, Flag... flags) {
      var release = javaReleaseFeatureNumber(String.valueOf(path.getFileName()));
      return new Vault(path, release, Set.of(flags));
    }

    static Vault of(Path path, int release, Flag... flags) {
      return new Vault(path, release, Set.of(flags));
    }

    private final Path path;
    private final int release;
    private final Set<Flag> flags;

    public Vault(Path path, int release, Set<Flag> flags) {
      this.path = path;
      this.release = release;
      this.flags = flags;
    }

    boolean isModuleInfoPresent() {
      return Files.isRegularFile(path.resolve("module-info.java"));
    }
  }

  class Tests {
    @Test
    void buildMultiRealmUnitReleaseAndPreview() {
      var name = "MultiRealmUnitReleaseAndPreview";
      var base = Path.of("src/test.base/test/resources/test/base/sandbox", name);
      //Tree.walk(base).forEach(System.out::println);

      var foo0 = Vault.of(base.resolve("foo/main-11/java"));
      var bar8 = Vault.of(base.resolve("bar/main-11/java-8"), 8);
      var bar9 = Vault.of(base.resolve("bar/main-11/java-9"), 9);
      var bar10 = Vault.of(base.resolve("bar/main-11/java-10"), 10);

      assertTrue(foo0.isModuleInfoPresent());
      assertFalse(bar8.isModuleInfoPresent());
      assertTrue(bar9.isModuleInfoPresent());
      assertFalse(bar10.isModuleInfoPresent());

      var foo = Unit.of("foo", foo0);
      var bar = Unit.of("bar", bar8, bar9, bar10);

      var main = new Realm("main", 11, false, List.of(foo, bar));
      assertEquals(3, main.units.get(1).vaults.size());
    }
  }
}
