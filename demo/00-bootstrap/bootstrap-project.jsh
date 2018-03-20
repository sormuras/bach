//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

var target = Files.createDirectories(Paths.get("target"))
var remote = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/bach/")
for (var name : Set.of("Bach.java", "Project.java", "PrinterFunction.java")) {
  var script = target.resolve(name);
  try (var stream = new URL(remote, name).openStream()) {
    Files.copy(stream, script, StandardCopyOption.REPLACE_EXISTING);
  }
}

/open target/Bach.java
/open target/Project.java
/open target/PrinterFunction.java

var bach = new Bach()
var project = Project.builder().name("Bootstrap").version("00").build()

bach.run("boot", () -> new PrinterFunction().apply(bach, project))

/exit
