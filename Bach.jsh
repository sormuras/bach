/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

/*
 * Define convenient short names for the foundation JDK tools and commands.
 */
void run(String tool, Object... args) { JdkTool.run(tool, args); }
void java(Object... args) { run("java", args); }
void javac(Object... args) { run("javac", args); }
void javadoc(Object... args) { run("javadoc", args); }
void jar(Object... args) { run("jar", args); }
void jlink(Object... args) { run("jlink", args); }
void jmod(Object... args) { run("jmod", args); }
void jdeps(Object... args) { run("jdeps", args); }
void jdeprscan(Object... args) { run("jdeprscan", args); }
void javah(Object... args) { run("javah", args); }
void javap(Object... args) { run("javap", args); }
