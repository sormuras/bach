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
void java(Object... args) { Bach.run("java", args); }
void javac(Object... args) { Bach.run("javac", args); }
void javadoc(Object... args) { Bach.run("javadoc", args); }
void jar(Object... args) { Bach.run("jar", args); }
void jlink(Object... args) { Bach.run("jlink", args); }
void jmod(Object... args) { Bach.run("jmod", args); }
void jdeps(Object... args) { Bach.run("jdeps", args); }
void jdeprscan(Object... args) { Bach.run("jdeprscan", args); }
void javah(Object... args) { Bach.run("javah", args); }
void javap(Object... args) { Bach.run("javap", args); }
