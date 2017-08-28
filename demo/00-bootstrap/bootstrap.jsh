//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/*
 * Download "Bach.java" and "Bach.jsh" from github to local "target" directory.
 */
Path target = Files.createDirectories(Paths.get("target"))
URL context = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/bach/")
for (Path script : Set.of(target.resolve("Bach.java"), target.resolve("Bach.jsh"))) {
  // if (Files.exists(script)) continue; // uncomment to preserve existing files
  try (InputStream stream = new URL(context, script.getFileName().toString()).openStream()) {
    Files.copy(stream, script, StandardCopyOption.REPLACE_EXISTING);
  }
}

/*
 * Source "Bach.java" and "Bach.jsh" into this jshell session.
 */
/open target/Bach.java
/open target/Bach.jsh

/*
 * Use it!
 */
java("--version")

/exit
