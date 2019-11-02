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

package de.sormuras.bach;

import java.net.URI;

/*BODY*/
/** Unchecked exception thrown when a module name is not mapped. */
public /*STATIC*/ class UnmappedModuleException extends RuntimeException {

  public static String throwForString(String module) {
    throw new UnmappedModuleException(module);
  }

  public static URI throwForURI(String module) {
    throw new UnmappedModuleException(module);
  }

  private static final long serialVersionUID = 6985648789039587477L;

  public UnmappedModuleException(String module) {
    super("Module " + module + " is not mapped");
  }
}
