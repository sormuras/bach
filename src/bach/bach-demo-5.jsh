/*
 * Bach - Java Shell Builder - Demo 2 - Jigsaw Greetings World!
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

System.out.println(" _____ _____ _____ _____         ____  _____ _____ _____")
System.out.println("| __  |  _  |     |  |  |       |    \\|   __|     |    5|")
System.out.println("| __ -|     |   --|     |       |  |  |   __| | | |  |  |")
System.out.println("|_____|__|__|_____|__|__|.java  |____/|_____|_|_|_|_____|")


var base = Path.of("bach-demo-5-junit")
var astro = base.resolve("src/org.astro/main/java/module-info.java")
var world = base.resolve("src/org.astro/main/java/org/astro/World.java")
var greetings = base.resolve("src/com.greetings/main/java/module-info.java")
var main = base.resolve("src/com.greetings/main/java/com/greetings/Main.java")
var integration = base.resolve("src/test.integration/test/java/module-info.java")
var integrations = base.resolve("src/test.integration/test/java/test/integration/IntegrationTests.java")
if (Files.notExists(base)) {
  // org.astro
  Files.createDirectories(world.getParent());
  Files.write(astro, List.of("module org.astro {", "  exports org.astro;", "}", ""));
  Files.write(world, List.of(
      "package org.astro;",
      "public class World {",
      "    public World() {}",
      "    public static String name() {",
      "        return \"world\";",
      "    }",
      "}",
      ""));

  // com.greetings
  Files.createDirectories(main.getParent());
  Files.write(greetings, List.of("module com.greetings {", "  requires org.astro;", "}", ""));
  Files.write(main, List.of(
      "package com.greetings;",
      "import org.astro.World;",
      "public class Main {",
      "    public static void main(String[] args) {",
      "        System.out.format(\"Greetings %s!%n\", World.name());",
      "    }",
      "}",
      ""));

  // test
  Files.createDirectories(integrations.getParent());
  Files.write(integration, List.of("open module test.integration {",
      "  requires com.greetings;",
      "  requires org.astro;",
      "  requires org.junit.jupiter;",
      "  requires static org.junit.platform.console;",
      "}",
      ""));
  Files.write(integrations, List.of(
      "package test.integration;",
      "import org.astro.World;",
      "import org.junit.jupiter.api.Assertions;",
      "import org.junit.jupiter.api.Test;",
      "class IntegrationTests {",
      "    @Test",
      "    void checkWorldsName() {",
      "        Assertions.assertEquals(\"world\", World.name());",
      "    }",
      "}",
      ""));
}

try (var stream = Files.walk(base)) {
  System.out.println();
  System.out.println("  Java Source Files");
  System.out.println();
  stream.map(path -> path.toString().replace('\\', '/')).filter(name -> name.endsWith(".java")).sorted().forEach(System.out::println);
  System.out.println();
  System.out.println("  Main Module Descriptors");
  System.out.println();
  Files.readAllLines(astro).forEach(System.out::println);
  Files.readAllLines(greetings).forEach(System.out::println);
  System.out.println("  Test Module Descriptor");
  System.out.println();
  Files.readAllLines(integration).forEach(System.out::println);
}

System.out.println()
System.out.println("Change into directory " + base)
System.out.println("and let Bach.java build the modular Java project.")
System.out.println()
System.out.println("  cd " + base + " && jshell https://sormuras.de/bach/build")
System.out.println()

/exit
