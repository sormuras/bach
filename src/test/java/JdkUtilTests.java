/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class JdkUtilTests {

  @Test
  void isJavaFile() {
    assertFalse(JdkUtil.isJavaFile(Paths.get("")));
    assertFalse(JdkUtil.isJavaFile(Paths.get("a/b")));
    assertTrue(JdkUtil.isJavaFile(Paths.get("src/test/java/JdkUtilTests.java")));
  }

  @Test
  void isJarFile() {
    assertFalse(JdkUtil.isJarFile(Paths.get("")));
    assertFalse(JdkUtil.isJarFile(Paths.get("a/b")));
  }

  @Test
  void resolve() {
    new JdkUtil.Resolvable("org.opentest4j", "opentest4j", "1.0.0-SNAPSHOT")
        .resolve(Paths.get("target"), JdkUtil.Resolvable.REPOSITORIES);
    new JdkUtil.Resolvable("org.opentest4j", "opentest4j", "1.0.0-ALPHA")
        .resolve(Paths.get("target"), JdkUtil.Resolvable.REPOSITORIES);
  }
}
