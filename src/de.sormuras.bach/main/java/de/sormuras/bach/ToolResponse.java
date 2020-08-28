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

import java.time.Duration;
import java.util.Arrays;
import java.util.StringJoiner;

/** A tool response object represents a result of a tool call run. */
public final class ToolResponse {

  private final String name;
  private final String[] args;
  private final long thread;
  private final Duration duration;
  private final int code;
  private final String out;
  private final String err;

  public ToolResponse(
      String name,
      String[] args,
      long thread,
      Duration duration,
      int code,
      String out,
      String err) {
    this.name = name;
    this.args = args;
    this.thread = thread;
    this.duration = duration;
    this.code = code;
    this.out = out;
    this.err = err;
  }

  public static void requireSuccessful(ToolResponse response) {
    if (response.code == 0) return;
    throw new RuntimeException(response.toString());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ToolResponse.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("args=" + Arrays.toString(args))
        .add("thread=" + thread)
        .add("duration=" + duration)
        .add("code=" + code)
        .add("out='" + out + "'")
        .add("err='" + err + "'")
        .toString();
  }
}
