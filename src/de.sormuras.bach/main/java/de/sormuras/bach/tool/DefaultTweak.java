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

import de.sormuras.bach.Call;
import de.sormuras.bach.Tweak;

/** An extensible tweak implementation. */
public class DefaultTweak implements Tweak {

  @Override
  public final Call<?> apply(Call<?> call) {
    if (call instanceof Jar) return tweakJar((Jar) call);
    if (call instanceof Javac) return tweakJavac((Javac) call);
    if (call instanceof Javadoc) return tweakJavadoc((Javadoc) call);
    if (call instanceof Jlink) return tweakJlink((Jlink) call);
    if (call instanceof JUnit) return tweakJUnit((JUnit) call);
    if (call instanceof TestModule) return tweakTestModule((TestModule) call);
    return tweak(call);
  }

  public Call<?> tweak(Call<?> call) {
    return call;
  }

  public Jar tweakJar(Jar jar) {
    return jar;
  }

  public Javac tweakJavac(Javac javac) {
    return javac;
  }

  public Javadoc tweakJavadoc(Javadoc javadoc) {
    return javadoc;
  }

  public Jlink tweakJlink(Jlink jlink) {
    return jlink;
  }

  public JUnit tweakJUnit(JUnit junit) {
    return junit;
  }

  public TestModule tweakTestModule(TestModule testModule) {
    return testModule;
  }
}
