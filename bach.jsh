/*
 * Java Shell Builder
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

// Load and open Bach.java
Path bachPath = Paths.get("target")
Path bachJava = bachPath.resolve("Bach.java")
if (Files.notExists(bachJava)) {
  URL bachURL = new URL("https://raw.githubusercontent.com/sormuras/bach/master/bach/Bach.java");
  Files.createDirectories(bachPath);
  try (InputStream in = bachURL.openStream()) {
    Files.copy(in, bachJava, StandardCopyOption.REPLACE_EXISTING);
  }
}
/open target/Bach.java

// Use it!
{
Bach.builder()
    .override(Folder.SOURCE, Paths.get("demo/basic"))
  .bach()
    .compile()
    .run("com.greetings", "com.greetings.Main");
}
/exit
