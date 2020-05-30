/*
 * Bach - Java Shell Builder - Demo 99 - Red Balloons
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

System.out.println()
System.out.println("Bach.java - Demo 99 - Red Balloons")
System.out.println()

var base = Path.of("bach-demo-99-balloons")
if (Files.notExists(base)) {
  Files.createDirectories(base);

  var ball00n = Files.createDirectories(base.resolve("src/ball00n"));
  Files.write(ball00n.resolve("module-info.java"), List.of("module ball00n {}", ""));

  for (int i = 1; i <= 99; i++) {
    var name = String.format("ball%02dn", i);
    var ball = Files.createDirectories(base.resolve("src/" + name));
    var lines = new ArrayList<String>();
    lines.add("module " + name + " {");
    for (int j = 0; j < i; j++) lines.add(String.format("  requires ball%02dn;", j));
    lines.add("}");
    Files.write(ball.resolve("module-info.java"), lines);
  }
}

try (var stream = Files.walk(base)) {
  stream.map(path -> path.toString().replace('\\', '/')).filter(name -> name.endsWith(".java")).sorted().forEach(System.out::println);
}

System.out.println()
System.out.println("Change into directory " + base + " and let Bach.java build the modular Java project.")
System.out.println()
System.out.println("  cd " + base + " && jshell https://sormuras.de/bach/build")
System.out.println()

/exit
