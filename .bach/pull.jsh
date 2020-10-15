interface Pull {
  Path CACHE = Path.of(".bach/cache");
  Path TOKEN = CACHE.resolve(".pull-in-progress-token");
}

if (!Files.deleteIfExists(Pull.TOKEN)) {
  var found = java.lang.module.ModuleFinder.of(Pull.CACHE).find("com.github.sormuras.bach");
  if (found.isPresent()) {
    var module = found.get();
    System.out.println("Delete module " + module.descriptor().toNameAndVersion());
    Files.delete(Path.of(module.location().orElseThrow()));
  }
  Files.createDirectories(Pull.TOKEN);
}

/open https://github.com/sormuras/bach/raw/HEAD/.bach/boot.jsh

System.out.println("Done.")

/exit
