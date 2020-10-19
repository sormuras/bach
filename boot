// Bach's Boot Script

void main() throws Exception {
  System.out.print(
    """
    Booting Bach in directory %s
    """
    .formatted(
      Path.of("").toAbsolutePath()
    )
  );

  load("bach").toFile().setExecutable(true);
  load("bach.bat");
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

/exit
