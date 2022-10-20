package run.bach;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import run.bach.internal.ModuleInfoFinder;
import run.bach.internal.ModuleInfoReference;
import run.bach.internal.ModuleSourcePathSupport;

/** A sequence of declared modules. */
public record DeclaredModules(List<DeclaredModule> list) implements Iterable<DeclaredModule> {

  public static DeclaredModules of(DeclaredModule... modules) {
    return of(List.of(modules));
  }

  public static DeclaredModules of(List<DeclaredModule> modules) {
    return new DeclaredModules(modules.stream().sorted().toList());
  }

  public Optional<DeclaredModule> find(String name) {
    return list.stream().filter(module -> module.name().equals(name)).findFirst();
  }

  @Override
  public Iterator<DeclaredModule> iterator() {
    return list.iterator();
  }

  public List<String> names() {
    return list.stream().sorted().map(DeclaredModule::name).toList();
  }

  public String names(String delimiter) {
    return String.join(delimiter, names());
  }

  public ModuleFinder toModuleFinder() {
    var moduleInfoReferences =
        list.stream()
            .map(module -> new ModuleInfoReference(module.info(), module.descriptor()))
            .toList();
    return new ModuleInfoFinder(moduleInfoReferences);
  }

  public List<String> toModuleSourcePaths() {
    var map = new TreeMap<String, List<Path>>();
    for (var module : list) map.put(module.name(), module.baseSourcePaths());
    return ModuleSourcePathSupport.compute(map, false);
  }

  public DeclaredModules with(DeclaredModule... more) {
    var stream = Stream.concat(list.stream(), Stream.of(more)).sorted();
    return new DeclaredModules(stream.toList());
  }
}
