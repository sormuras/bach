/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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
int java(Object... args) { return new Bach().run("java", args); }
int javac(Object... args) { return new Bach().run("javac", args); }
int javadoc(Object... args) { return new Bach().run("javadoc", args); }
int jar(Object... args) { return new Bach().run("jar", args); }
int jlink(Object... args) { return new Bach().run("jlink", args); }
int jmod(Object... args) { return new Bach().run("jmod", args); }
int jdeps(Object... args) { return new Bach().run("jdeps", args); }
int jdeprscan(Object... args) { return new Bach().run("jdeprscan", args); }
int javap(Object... args) { return new Bach().run("javap", args); }
