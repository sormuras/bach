//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/*
 * Download "Bach.java" from github to local "target" directory.
 */
URL url = new URL("https://raw.githubusercontent.com/sormuras/bach/master/Bach.java")
Path target = Paths.get("target")
Path script = target.resolve("Bach.java")
if (Files.notExists(script)) {
  Files.createDirectories(target);
  try (InputStream stream = url.openStream()) { Files.copy(stream, script); }
}

/*
 * Source "Bach.java" and convenient methods from "BACH" script into this jshell session.
 */
/open target/Bach.java
Bach.generateJShellScript(Paths.get("target"), "BACH")
/open target/BACH

/*
 * Use it!
 */
java("--version")

/exit
