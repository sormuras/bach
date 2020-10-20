// Bach's Boot Script

var version = System.getProperty("version", "15-ea+3")

void main() throws Exception {
  System.out.print(
    """
    Booting Bach %s in directory %s
    """
    .formatted(
      version,
      Path.of("").toAbsolutePath()
    )
  );

  load("bach").toFile().setExecutable(true);
  load("bach.bat");
  load("bach.java");
  load(".gitignore");
}

Path load(String file) throws Exception {
  var source = "https://github.com/sormuras/bach/raw/main/" + file;
  var target = Path.of(file);
  System.out.println("  " + file + " << " + source);
  Files.deleteIfExists(target);
  try (var stream = new URL(source).openStream()) { Files.copy(stream, target); }
  return target;
}

main()

/open bach.java

bach.main("version", version)
bach.main("version")

/exit
