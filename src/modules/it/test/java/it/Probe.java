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

package it;

import de.sormuras.bach.Bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

class Probe extends Bach {

  private final StringWriter out, err;

  Probe() {
    this(new StringWriter(), new StringWriter(), true);
  }

  private Probe(StringWriter out, StringWriter err, boolean verbose) {
    super(new PrintWriter(out), new PrintWriter(err), verbose);
    this.out = out;
    this.err = err;
  }

  List<String> lines() {
    return out.toString().lines().collect(Collectors.toList());
  }

  List<String> errors() {
    return err.toString().lines().collect(Collectors.toList());
  }
}
