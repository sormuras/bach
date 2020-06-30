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

package de.sormuras.bach.project;

/** A set of sources. */
public final class Sources {

  public static Sources of() {
    return new Sources(MainSources.of(), TestSources.of(), TestPreview.of());
  }

  public Sources with(MainSources main) {
    return new Sources(main, test, preview);
  }

  public Sources with(TestSources test) {
    return new Sources(main, test, preview);
  }

  public Sources with(TestPreview preview) {
    return new Sources(main, test, preview);
  }

  public Sources withMainUnit(SourceUnit unit) {
    return with(main.with(unit));
  }

  public Sources withTestUnit(SourceUnit unit) {
    return with(test.with(unit));
  }

  public Sources withPreviewUnit(SourceUnit unit) {
    return with(preview.with(unit));
  }

  private final MainSources main;
  private final TestSources test;
  private final TestPreview preview;

  public Sources(MainSources main, TestSources test, TestPreview preview) {
    this.main = main;
    this.test = test;
    this.preview = preview;
  }

  public MainSources main() {
    return main;
  }

  public TestSources test() {
    return test;
  }

  public TestPreview preview() {
    return preview;
  }

  public boolean isEmpty() {
    return main().units().isEmpty() && test().units().isEmpty() && preview().units().isEmpty();
  }
}
