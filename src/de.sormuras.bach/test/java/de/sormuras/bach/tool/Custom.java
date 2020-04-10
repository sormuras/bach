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

package de.sormuras.bach.tool;

import java.util.List;
import java.util.concurrent.TimeUnit;

class Custom extends Tool {

  private final String initialArgument;

  public Custom(String initialArgument, List<? extends Option> options) {
    super("custom", options);
    this.initialArgument = initialArgument;
  }

  @Override
  protected void addInitialArguments(Arguments arguments) {
    arguments.add(initialArgument);
  }

  @Override
  protected void addMoreArguments(Arguments arguments) {
    arguments.add("END.");
  }

  public static class Granularity implements Option {

    private final TimeUnit unit;

    public Granularity(TimeUnit unit) {
      this.unit = unit;
    }

    public TimeUnit unit() {
      return unit;
    }

    @Override
    public void visit(Arguments arguments) {
      arguments.add("--granularity", unit);
    }
  }
}
