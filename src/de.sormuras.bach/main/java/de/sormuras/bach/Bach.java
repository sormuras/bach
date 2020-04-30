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

import de.sormuras.bach.util.Functions;
import java.lang.module.ModuleDescriptor.Version;
import java.net.http.HttpClient;
import java.util.function.Supplier;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11.0-ea");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** HttpClient supplier. */
  private final Supplier<HttpClient> httpClient;

  /** Initialize this instance with default values. */
  public Bach() {
    this(HttpClient.newBuilder()::build);
  }

  /** Initialize this instance with the specified line printer, workspace, and other values. */
  public Bach(Supplier<HttpClient> httpClient) {
    this.httpClient = Functions.memoize(httpClient);
  }

  public HttpClient getHttpClient() {
    return httpClient.get();
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
