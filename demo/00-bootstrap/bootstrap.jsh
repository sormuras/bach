//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/*
 * Download "Bach.java" and "Bach.jsh" from github to local "target" directory.
 */
var target = Files.createDirectories(Paths.get("target"))
var remote = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/bach/")
for (var name : Set.of("Bach.java", "Bach.jsh")) {
  var script = target.resolve(name);
  // if (Files.exists(script)) continue; // uncomment to preserve existing files
  try (var stream = new URL(remote, name).openStream()) {
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
