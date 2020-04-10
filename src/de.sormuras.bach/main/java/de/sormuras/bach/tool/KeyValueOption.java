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

/** An option consisting of a named key and a generic value. */
public /*static*/ class KeyValueOption<V> implements Option {

  private final String key;
  private final V value;

  public KeyValueOption(String key, V value) {
    this.key = key;
    this.value = value;
  }

  public V value() {
    return value;
  }

  @Override
  public void visit(Arguments arguments) {
    arguments.add(key);
    if (value == null) return;
    arguments.add(value);
  }
}
