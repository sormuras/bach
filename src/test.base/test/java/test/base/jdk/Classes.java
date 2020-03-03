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

package test.base.jdk;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;

public class Classes {
  public static int getMajorVersionNumber(Path path) {
    try (var data = new DataInputStream(new FileInputStream(path.toFile()))) {
      data.skipBytes(4); // var magicNumber = data.readInt(); 0xCAFEBABE
      data.skipBytes(2); // var minorVersion = data.readUnsignedShort();
      return data.readUnsignedShort();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public static int feature(Path path) {
    return getMajorVersionNumber(path) - 44;
  }
}
