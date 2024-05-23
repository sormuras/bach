package run.demo;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Optional;
import java.util.Set;
import run.bach.ModuleFinders;

public record MavenRepositoryModuleFinder(
    String repository, ModuleFinders.ModuleReferenceFinder finder) implements ModuleFinder {

  public static MavenRepositoryModuleFinder ofMavenCentral() {
    return ofMavenRepository("https://repo.maven.apache.org/maven2");
  }

  public static MavenRepositoryModuleFinder ofMavenRepository(String repository) {
    return new MavenRepositoryModuleFinder(repository, new ModuleFinders.ModuleReferenceFinder());
  }

  @Override
  public Optional<ModuleReference> find(String name) {
    return finder.find(name);
  }

  @Override
  public Set<ModuleReference> findAll() {
    return finder.findAll();
  }

  public MavenRepositoryModuleFinder with(String module, String coordinates) {
    var split = coordinates.split(":");
    var group = split[0];
    var artifact = split[1];
    var version = split[2];
    return with(module, group, artifact, version);
  }

  public MavenRepositoryModuleFinder with(
      String module, String group, String artifact, String version) {
    var location = repository + "/";
    return new MavenRepositoryModuleFinder(repository, finder.with(module, location));
  }
}
