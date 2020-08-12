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

package de.sormuras.bach;

/** A flag represents a feature toggle. */
public enum Flag {
  DRY_RUN(false),
  FAIL_FAST(true),
  FAIL_ON_ERROR(true),

  SUMMARY_WITH_TOOL_CALL_OVERVIEW(true),
  SUMMARY_WITH_MAIN_MODULE_OVERVIEW(true),
  SUMMARY_LINES_UNCUT(false);

  private final boolean initially;

  Flag(boolean initially) {
    this.initially = initially;
  }

  public boolean isInitiallyTrue() {
    return initially;
  }
}
