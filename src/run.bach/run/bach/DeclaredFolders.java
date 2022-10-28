package run.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** A collection of source and resource directories. */
public record DeclaredFolders(List<Path> sources, List<Path> resources) {

  static Map<Integer, DeclaredFolders> mapFoldersByJavaFeatureReleaseNumber(Path container) {
    var targeted = new TreeMap<Integer, DeclaredFolders>();
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var folders = DeclaredFolders.of().withSiblings(container, release);
      if (folders.isEmpty()) continue;
      targeted.put(release, folders);
    }
    return Map.copyOf(targeted);
  }

  public static DeclaredFolders of(Path... sources) {
    return new DeclaredFolders(Stream.of(sources).map(Path::normalize).toList(), List.of());
  }

  public DeclaredFolders withSiblings(Path container) {
    return withSiblings(container, "");
  }

  public DeclaredFolders withSiblings(Path container, int release) {
    return withSiblings(container, "-" + release);
  }

  public DeclaredFolders withSiblings(Path container, String suffix) {
    var sources = container.resolve("java" + suffix);
    var resources = container.resolve("resources" + suffix);
    var folders = this;
    if (Files.isDirectory(sources)) folders = folders.withSourcePath(sources);
    if (Files.isDirectory(resources)) folders = folders.withResourcePath(resources);
    return folders;
  }

  public DeclaredFolders withSourcePath(Path candidate) {
    var path = candidate.normalize();
    if (sources.contains(path)) return this;
    return new DeclaredFolders(
        Stream.concat(sources.stream(), Stream.of(path)).toList(), resources);
  }

  public DeclaredFolders withResourcePath(Path candidate) {
    var path = candidate.normalize();
    if (resources.contains(path)) return this;
    return new DeclaredFolders(
        sources, Stream.concat(resources.stream(), Stream.of(path)).toList());
  }

  public boolean isEmpty() {
    return sources.isEmpty() && resources.isEmpty();
  }
}
