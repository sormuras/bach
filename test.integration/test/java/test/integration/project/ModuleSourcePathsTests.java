package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ModuleSourcePaths;
import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModuleSourcePathsTests {
  @Test
  void emptyIsEmpty() {
    assertTrue(ModuleSourcePaths.EMPTY.patterns().isEmpty());
    assertTrue(ModuleSourcePaths.EMPTY.specifics().isEmpty());
  }

  @Test
  void modulePatternFormFromPathWithoutModulesNameFails() {
    var path = Path.of("a/b/c/module-info.java");
    var exception = assertThrows(FindException.class, () -> computePatternForm(path, "d"));
    assertEquals("Name 'd' not found: " + path, exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
    ".               , foo/module-info.java",
    "src             , src/foo/module-info.java",
    "./*/src         , foo/src/module-info.java",
    "src/*/main/java , src/foo/main/java/module-info.java"
  })
  void modulePatternFormForModuleFoo(String expected, Path path) {
    var actual = computePatternForm(path, "foo");
    assertEquals(expected.replace('/', File.separatorChar), actual);
  }

  /** {@return a string in module-pattern form usable as a {@code --module-source-path} value} */
  private static String computePatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    if (info.isAbsolute()) return info.getRoot() + pattern;
    return pattern;
  }

  private static List<String> compute(
      Iterable<DeclaredModule> modules, boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var declared : modules) {
      var sourcePaths = List.of(declared.path());
      if (forceModuleSpecificForm) {
        specific.put(declared.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) {
          patterns.add(computePatternForm(path, declared.name()));
        }
      } catch (FindException e) {
        specific.put(declared.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + join(entry.getValue()));
    return List.copyOf(paths);
  }

  /** {@return a string composed of paths joined via the system-dependent path-separator} */
  private static String join(Collection<Path> paths) {
    return join(paths, File.pathSeparator);
  }

  /** {@return a string composed of paths joined via the given delimiter} */
  private static String join(Collection<Path> paths, CharSequence delimiter) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(delimiter));
  }
}
