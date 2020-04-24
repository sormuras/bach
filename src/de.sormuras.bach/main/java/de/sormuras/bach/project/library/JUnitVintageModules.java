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

package de.sormuras.bach.project.library;

/** JUnit Vintage + JUnit 4 + Hamcrest. */
public /*static*/ class JUnitVintageModules extends JUnit5Modules {

  public JUnitVintageModules() {
    super("org.junit.vintage", "5.7.0-M1");
    put(".engine", 63969, "455be2fc44c7525e7f20099529aec037");
    put("junit", "junit:junit:4.13", 381765, "5da6445d7b80aba2623e73d4561dcfde");
    put("org.hamcrest", "org.hamcrest:hamcrest:2.2", 123360, "10b47e837f271d0662f28780e60388e8");
  }
}
