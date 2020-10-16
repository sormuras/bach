interface Init {
  Path base = Path.of("");
  Path bach = base.resolve(".bach");
  Path cache = bach.resolve("cache");
  String version = System.getProperty("version", "15-ea+2");
  String jar = "com.github.sormuras.bach@" + version + ".jar";
  String repository = "https://github.com/sormuras/bach";
  String raws = repository + "/raw/" + version;
  String releases = repository + "/releases/download/" + version;
  boolean force = System.getProperty("force") != null;
}

void main() throws Exception {
  System.out.print(
    """
    Initializing Bach in directory %s
    """
    .formatted(
      Init.base.toAbsolutePath()
    )
  );

  if (Files.exists(Init.bach) && !Init.force) {
    System.err.println("Bach already initialized: " + Init.bach.toAbsolutePath());
    return;
  }

  load(Init.repository + "/raw/main" + "/bach.bat", Init.base.resolve("bach.bat"));
  // TODO load(Init.repository + "/bach", Init.base.resolve("bach")).toFile().setExecutable(true);

  Files.createDirectories(Init.cache);
  load(Init.releases + "/" + Init.jar, Init.cache.resolve(Init.jar));
}

Path load(String source, Path target) throws Exception {
  System.out.println("Download " + source + " to " + target.toAbsolutePath().getParent().toUri());
  if (Init.force) Files.deleteIfExists(target);
  try (var stream = new URL(source).openStream()) { Files.copy(stream, target); }
  return target;
}

main()

/exit
