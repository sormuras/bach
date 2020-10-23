// Bach's Boot Script

void boot() throws Exception {
  var module = "com.github.sormuras.bach";
  var cache = Path.of(".bach/cache");
  if (java.lang.module.ModuleFinder.of(cache).find(module).isPresent()) return;

  var version = System.getProperty("version", "early-access");
  var jar = module + '@' + version + ".jar";
  var source = "https://github.com/sormuras/bach/releases/download/" + version + '/' + jar;
  var target = Files.createDirectories(cache).resolve(jar);
  try (var stream = new URL(source).openStream()) { Files.copy(stream, target); }
}

boot()

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
"""
.formatted(
  com.github.sormuras.bach.Bach.version(),
  Runtime.version(),
  System.getProperty("os.name")
))

/reset

import com.github.sormuras.bach.*
