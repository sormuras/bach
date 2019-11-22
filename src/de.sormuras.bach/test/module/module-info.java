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

// --main-class de.sormuras.bach.Bach
open /*test*/ module de.sormuras.bach /*extends "main" module*/ {

  // "test"

  requires org.junit.jupiter;

  // "main"

  requires java.compiler;
  requires java.net.http;

  exports de.sormuras.bach;
  exports de.sormuras.bach.project;

  uses java.util.spi.ToolProvider;
}
