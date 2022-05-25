package test.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LayoutTests {

  record Layout(Path root, Path info, Folders base, Map<Integer, Folders> targeted) {}

  record Folders(List<Path> sources, List<Path> resources) {
    static Folders of(Path... sources) {
      return new Folders(Stream.of(sources).map(Path::normalize).toList(), List.of());
    }

    Folders withSiblings(Path container, String suffix) {
      var sources = container.resolve("java" + suffix);
      var resources = container.resolve("resources" + suffix);
      var folders = this;
      if (Files.isDirectory(sources)) folders = folders.withSourcePath(sources);
      if (Files.isDirectory(resources)) folders = folders.withResourcePath(resources);
      return folders;
    }

    Folders withSourcePath(Path candidate) {
      var path = candidate.normalize();
      if (sources.contains(path)) return this;
      return new Folders(Stream.concat(sources.stream(), Stream.of(path)).toList(), resources);
    }

    Folders withResourcePath(Path candidate) {
      var path = candidate.normalize();
      if (resources.contains(path)) return this;
      return new Folders(sources, Stream.concat(resources.stream(), Stream.of(path)).toList());
    }

    boolean isEmpty() {
      return sources.isEmpty() && resources.isEmpty();
    }
  }

  static final Path PROJECTS = Path.of("test.workflow/example-projects");

  @Test
  void checkAggregator() {
    var root = PROJECTS.resolve("aggregator");
    var aggregator = new Layout(root, root.resolve("module-info.java"), Folders.of(root), Map.of());
    assertEquals(aggregator, walk(root, root.resolve("module-info.java")));
  }

  @Test
  void checkHello() {
    var root = PROJECTS.resolve("hello");
    var hello = new Layout(root, root.resolve("module-info.java"), Folders.of(root), Map.of());
    assertEquals(hello, walk(root, root.resolve("module-info.java")));
  }

  @Test
  void checkHelloWorld() {
    var root = PROJECTS.resolve("hello-world");
    var hello =
        new Layout(
            root.resolve("hello"),
            root.resolve("hello/module-info.java"),
            Folders.of(root.resolve("hello")),
            Map.of());
    assertEquals(hello, walk(root.resolve("hello"), root.resolve("hello/module-info.java")));
    var world =
        new Layout(
            root.resolve("world"),
            root.resolve("world/module-info.java"),
            Folders.of(root.resolve("world")),
            Map.of());
    assertEquals(world, walk(root.resolve("world"), root.resolve("world/module-info.java")));
  }

  @Test
  void checkMultiRelease() {
    var project = PROJECTS.resolve("multi-release");
    var root = project.resolve("foo");
    var foo =
        new Layout(
            root,
            root.resolve("java/module-info.java"),
            Folders.of(root.resolve("java")),
            Map.of(
                11, Folders.of(root.resolve("java-11")),
                17, Folders.of(root.resolve("java-17"))));
    assertEquals(foo, walk(root, root.resolve("java/module-info.java")));
  }

  @Test
  void checkMultiReleaseWithResources() {
    var project = PROJECTS.resolve("multi-release-with-resources");
    var root = project.resolve("foo/main");
    var foo =
        new Layout(
            root,
            root.resolve("java/module-info.java"),
            Folders.of(root.resolve("java")).withResourcePath(root.resolve("resources")),
            Map.of(
                11,
                    Folders.of()
                        .withSourcePath(root.resolve("java-11"))
                        .withResourcePath(root.resolve("resources-11")),
                13, Folders.of().withResourcePath(root.resolve("resources-13")),
                15, Folders.of().withSourcePath(root.resolve("java-15")),
                17, Folders.of().withSourcePath(root.resolve("java-17"))));
    assertEquals(foo, walk(root, root.resolve("java/module-info.java")));
  }

  static Layout walk(Path root, Path info) {
    // trivial case: "module-info.java" resides directly in content root directory
    if (root.resolve("module-info.java").equals(info)) {
      return new Layout(root, info, new Folders(List.of(root), List.of()), Map.of());
    }
    // "module-info.java" resides in a subdirectory, usually named "java" or "java-module"
    var parent = info.getParent();
    if (parent == null) throw new UnsupportedOperationException("No parent of: " + info);
    var container = parent.getParent();
    if (container == null) throw new UnsupportedOperationException("No container of: " + parent);
    // find base siblings
    var base = Folders.of(parent).withSiblings(container, "");
    // find targeted siblings
    var targeted = new TreeMap<Integer, Folders>();
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var folders = Folders.of().withSiblings(container, "-" + release);
      if (folders.isEmpty()) continue;
      targeted.put(release, folders);
    }
    return new Layout(root, info, base, Map.copyOf(targeted));
  }
}
