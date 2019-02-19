//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

/open src/main/Bach.java
var bach = new Bach()

bach.var.out = System.out::println
bach.var.err = System.err::println

var format = new Bach.Tool.GoogleJavaFormat(Boolean.getBoolean("bach.format.replace"), Set.of(Path.of("src"), Path.of("demo")))
var target = "target/build"
var compileMain = new Bach.Action.Tool("javac", "-d", target + "/main", "src/main/Bach.java")
var downloadJUnit = new Bach.Action.Download(Path.of(target), URI.create(bach.var.get(Bach.Property.TOOL_JUNIT_URI)))
var junit = Path.of(target + "/junit-platform-console-standalone-1.4.0.jar")
var compileTest = new Bach.Action.Tool(new Bach.Command("javac").add("-d").add(target + "/test").add("-cp").add(List.of(Path.of(target + "/main"), junit)).addAllJavaFiles(List.of(Path.of("src/test"))))
var copyTestResources = new Bach.Action.TreeCopy(Path.of("src/test-resources"), Path.of(target + "/test"))
var runTest = new Bach.Action.Tool(new Bach.Command("java").add("-ea").add("-cp").add(List.of(Path.of(target + "/test"), Path.of(target + "/main"), junit)).add("org.junit.platform.console.ConsoleLauncher").add("--scan-class-path"))

/exit bach.run(format, compileMain, downloadJUnit, compileTest, copyTestResources, runTest)
