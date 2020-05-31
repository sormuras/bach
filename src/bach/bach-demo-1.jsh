/*
 * Bach - Java Shell Builder - Demo 1 - Jigsaw Greetings!
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
System.out.println("| __  |  _  |     |  |  |       |    \\|   __|     |    1|")
System.out.println("| __ -|     |   --|     |       |  |  |   __| | | |  |  |")
System.out.println("|_____|__|__|_____|__|__|.java  |____/|_____|_|_|_|_____|")
System.out.println()
System.out.println("Greetings! Inspired by Project Jigsaw: Module System Quick-Start Guide")
System.out.println("Find details at https://openjdk.java.net/projects/jigsaw/quick-start")

var base = Path.of("bach-demo-1-jigsaw-greetings")
var main = base.resolve("src/com.greetings/com/greetings/Main.java")
var info = base.resolve("src/com.greetings/module-info.java")
if (Files.notExists(base)) {
  Files.createDirectories(main.getParent());
  Files.write(info, List.of("module com.greetings {}", ""));
  Files.write(main, List.of(
      "package com.greetings;",
      "public class Main {",
      "    public static void main(String[] args) {",
      "        System.out.println(\"Greetings!\");",
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
  System.out.println("  Module Descriptor");
  System.out.println();
  Files.readAllLines(info).forEach(System.out::println);
  System.out.println("  Main Program");
  System.out.println();
  Files.readAllLines(main).forEach(System.out::println);
}

System.out.println()
System.out.println("Change into directory " + base)
System.out.println("and let Bach.java build the modular Java project.")
System.out.println()
System.out.println("  cd " + base + " && jshell https://sormuras.de/bach/build")
System.out.println()

/exit
