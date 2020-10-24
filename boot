// Bach's Boot Script

void boot(String version, boolean reboot) throws Exception {
  var cache = Path.of(".bach/cache");
  var booting = cache.resolve(".booting");
  var module = "com.github.sormuras.bach";
  var jar = module + '@' + version + ".jar";
  var source = "https://github.com/sormuras/bach/releases/download/" + version + '/' + jar;
  var target = cache.resolve(jar);

  if (Files.deleteIfExists(booting)) return;
  Files.createDirectories(booting);

  var reference = java.lang.module.ModuleFinder.of(cache).find(module);
  if (reference.isPresent()) {
    var cached = Path.of(reference.get().location().orElseThrow());
    var cachedIsEarlyAccess = cached.getFileName().toString().contains("early-access");
    if (cachedIsEarlyAccess || reboot) Files.delete(cached); else return;
  }

  try (var stream = new URL(source).openStream()) { Files.copy(stream, target); }
}

boot(System.getProperty("version", "early-access"), System.getProperty("reboot") != null)

/env --module-path .bach/cache --add-modules com.github.sormuras.bach

System.out.println(
"""
    ___      ___      ___      ___
   /\\  \\    /\\  \\    /\\  \\    /\\__\\
  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_
 /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\
 \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /
  \\::/  /   /:/  /  \\:\\__\\    /:/  /
   \\/__/    \\/__/    \\/__/    \\/__/.java

                Bach %s
        Java Runtime %s
    Operating System %s
   Working Directory %s
"""
.formatted(
  com.github.sormuras.bach.Bach.version(),
  Runtime.version(),
  System.getProperty("os.name"),
  Path.of("").toAbsolutePath()
))

/reset

import com.github.sormuras.bach.*

var BACH = Bach.ofSystem();

void find(String glob) { BACH.printFind(glob); }
