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
import com.github.sormuras.bach.project.*
import com.github.sormuras.bach.tool.*
import java.lang.module.ModuleFinder

var BACH = Bach.ofSystem()
var DIRECTORY = ModuleDirectory.of(Path.of(".bach", "libraries"), Map.of())
var SEARCHER = ModuleSearcher.compose(DIRECTORY::lookup, ModuleSearcher.ofBestEffort(BACH))

void find(String glob) { BACH.printFind(glob); }
Path copy(String source, String file) { return BACH.httpCopy(URI.create(source), Path.of(file)); }
String read(String source) { return BACH.httpRead(URI.create(source)); }

void linkModule(String module, String target) { DIRECTORY = DIRECTORY.withLinks(ModuleLink.link(module).to(target)); }
void linkModuleToUri(String module, String uri) { DIRECTORY = DIRECTORY.withLinks(ModuleLink.link(module).toUri(uri)); }
void linkModuleToMavenCentral(String module, String gav) { DIRECTORY = DIRECTORY.withLinks(ModuleLink.link(module).toMavenCentral(gav)); }

void listDeclaredModules() { BACH.printModules(ModuleInfoFinder.of(Path.of(""), "*/main/java", "*/main/java-module")); }
void listDeclaredTestModules() { BACH.printModules(ModuleInfoFinder.of(Path.of(""), "*/test/java", "*/test/java-module")); }
void listDeclaredTestPreview() { BACH.printModules(ModuleInfoFinder.of(Path.of(""), "*/test-preview/java")); }
void listLoadedModules() { BACH.printModules(DIRECTORY.finder()); }
void listMissingModules() { DIRECTORY.missing().stream().sorted().forEach(System.out::println); }
void listModuleInfoFiles() { find("**/module-info.java"); }
void listModuleLinks() { DIRECTORY.stream().sorted().forEach(System.out::println); }
void listSystemModules() { BACH.printModules(ModuleFinder.ofSystem()); }
void listTools() { BACH.printToolProviders(DIRECTORY.finder()); }
void describeModule(String module) { BACH.printModuleDescription(ModuleFinder.compose(ModuleFinder.ofSystem(), DIRECTORY.finder()), module); }

void loadModule(String module) { BACH.loadModule(DIRECTORY, SEARCHER::search, module); }
void loadMissingModules() { BACH.loadMissingModules(DIRECTORY, SEARCHER::search); }

ToolResponse run(ToolCall tool) { return BACH.toolCall(DIRECTORY, tool); }
void run(String tool, Object... args) { BACH.toolRun(DIRECTORY, tool, args); }

void build(String... args) { BuildProgram.execute(BACH, args); }
