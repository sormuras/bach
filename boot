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
import com.github.sormuras.bach.module.*

var BACH = Bach.ofSystem()
var MODULES = ModuleDirectory.of(Path.of("lib"))

// Listing
void find(String glob) { BACH.printFind(glob); }
void listModules() { BACH.printModules(MODULES.finder()); }
void listModulesOfSystem() { BACH.printModules(java.lang.module.ModuleFinder.ofSystem()); }
void listTools() { BACH.printToolProviders(MODULES.finder()); }
// Browser
Path copy(String source, String file) { return BACH.httpCopy(URI.create(source), Path.of(file)); }
String read(String source) { return BACH.httpRead(URI.create(source)); }
// Modules
void listModuleLinks() { MODULES.stream().sorted().forEach(System.out::println); }
void linkModule(String module, String uri) { MODULES = MODULES.withLinks(ModuleLink.module(module).toUri(uri)); }
void linkModuleToMavenCentral(String module, String gav) { MODULES = MODULES.withLinks(ModuleLink.module(module).toMavenCentral(gav)); }
// TODO void loadModule(String module) { BACH.loadModule(MODULES, module); }
// TODO void loadMissingModules() { BACH.loadMissingModules(MODULES); }
