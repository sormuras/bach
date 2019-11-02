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

import de.sormuras.bach.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class Recorder {

  private static List<String> lines(StringWriter writer) {
    return List.copyOf(writer.toString().lines().collect(Collectors.toList()));
  }

  static Optional<String> findMethodName(int skip) {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        .walk(frames -> frames.skip(skip).findFirst().map(StackWalker.StackFrame::getMethodName));
  }

  final String caption;
  final Log log;
  final StringWriter out, err;

  Recorder() {
    this(findMethodName(2).orElse("?"), new StringWriter(), new StringWriter());
  }

  private Recorder(String caption, StringWriter out, StringWriter err) {
    this.caption = caption;
    this.log = new Log(new PrintWriter(out, true), new PrintWriter(err, true), true);
    this.out = out;
    this.err = err;
  }

  List<String> lines() {
    return lines(out);
  }

  List<String> errors() {
    return lines(err);
  }

  @Override
  public String toString() {
    return "Recorder{" + "caption=" + caption + ", out=" + out + ", err=" + err + '}';
  }
}
