//usr/bin/env jshell --execution local --show-version "$0" "$@"; exit $?

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

/open https://github.com/sormuras/bach/raw/master/BUILDING

get("lib", URI.create("https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/bach-master-SNAPSHOT.jar"))

var java = Path.of(System.getProperty("java.home")).resolve("bin/java").toString()
var code = exe(java, "--module-path", "lib", "--module", "de.sormuras.bach/de.sormuras.bach.Bach")

/exit code
