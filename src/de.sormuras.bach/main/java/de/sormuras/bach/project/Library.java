package de.sormuras.bach.project;

import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Uris;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/** Manage external 3rd-party modules. */
public /*record*/ class Library {

  public enum Modifier {
    RESOLVE_RECURSIVELY,
    ADD_MISSING_JUNIT_TEST_ENGINES,
    ADD_MISSING_JUNIT_PLATFORM_CONSOLE
  }

  /** Link a module name to resource and a default version. */
  public static /*record*/ class Link {

    public static final String VERSION = "${VERSION}";
    public static final String JAVAFX_PLATFORM = "${JAVAFX-PLATFORM}";

    public static Link central(String group, String artifact, String version) {
      return central(group, artifact, version, "");
    }

    public static Link central(String group, String artifact, String version, String classifier) {
      return of("https://repo1.maven.org/maven2", group, artifact, version, classifier);
    }

    public static Link of(
        String repository, String group, String artifact, String version, String classifier) {
      var versionAndClassifier = classifier.isBlank() ? VERSION : VERSION + '-' + classifier;
      var type = "jar";
      var file = artifact + '-' + versionAndClassifier + '.' + type;
      var ref = String.join("/", repository, group.replace('.', '/'), artifact, VERSION, file);
      return new Link(ref, Version.parse(version));
    }

    private final String reference;
    private final Version defaultVersion;

    public Link(String reference, Version defaultVersion) {
      this.reference = reference;
      this.defaultVersion = defaultVersion;
    }
  }

  /** Requires description. */
  public static /*record*/ class Requires {
    private final String name;
    private final Version version;

    public Requires(String name, Version version) {
      this.name = Objects.requireNonNull(name);
      this.version = version;
    }
  }

  @SuppressWarnings("serial")
  private static class DefaultLinks extends TreeMap<String, Link> {
    private DefaultLinks() {
      put("org.apiguardian.api", Link.central("org.apiguardian", "apiguardian-api", "1.1.0"));
      put("org.opentest4j", Link.central("org.opentest4j", "opentest4j", "1.2.0"));
      putJavaFX("13.0.1", "base", "controls", "fxml", "graphics", "media", "swing", "web");
      putJUnitJupiter("5.6.0-M1", "", "api", "engine", "params");
      putJUnitPlatform("1.6.0-M1", "commons", "console", "engine", "launcher", "reporting");
    }

    private void putJavaFX(String version, String... names) {
      for (var name : names) {
        var link = Link.central("org.openjfx", "javafx-" + name, version, Link.JAVAFX_PLATFORM);
        put("javafx." + name, link);
      }
    }

    private void putJUnitJupiter(String version, String... names) {
      for (var name : names) {
        var artifact = "junit-jupiter" + (name.isEmpty() ? "" : '-' + name);
        var link = Link.central("org.junit.jupiter", artifact, version);
        var module = "org.junit.jupiter" + (name.isEmpty() ? "" : '.' + name);
        put(module, link);
      }
    }

    private void putJUnitPlatform(String version, String... names) {
      for (var name : names) {
        var link = Link.central("org.junit.platform", "junit-platform-" + name, version);
        put("org.junit.platform." + name, link);
      }
    }
  }

  public static Library of() {
    return of(new Properties());
  }

  public static Library of(Properties properties) {
    var links = new DefaultLinks();
    // TODO Parse passed properties for library links and requires and override default ones...
    return new Library(EnumSet.allOf(Modifier.class), links, Set.of());
  }

  public static void addJUnitTestEngines(Map<String, Set<Version>> map) {
    if (map.containsKey("org.junit.jupiter") || map.containsKey("org.junit.jupiter.api")) {
      map.putIfAbsent("org.junit.jupiter.engine", Set.of());
    }
    if (map.containsKey("junit")) {
      map.putIfAbsent("org.junit.vintage", Set.of());
    }
  }

  public static void addJUnitPlatformConsole(Map<String, Set<Version>> map) {
    if (map.containsKey("org.junit.jupiter.engine") || map.containsKey("org.junit.vintage")) {
      map.putIfAbsent("org.junit.platform.console", Set.of());
    }
  }

  private final Set<Modifier> modifiers;
  private final Map<String, Link> links;
  private final Collection<Requires> requires;
  private final Uris uris;

  Library(Set<Modifier> modifiers, Map<String, Link> links, Collection<Requires> requires) {
    this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
    this.links = Map.copyOf(links);
    this.requires = requires;
    this.uris = Uris.ofSystem();
  }

  public Set<Modifier> modifiers() {
    return modifiers;
  }

  public Map<String, Link> links() {
    return links;
  }

  public Collection<Requires> requires() {
    return requires;
  }

  public void resolveRequires(Path lib) throws Exception {
    for (var required : requires) {
      resolve(lib, required.name, required.version);
    }
  }

  public void resolveModules(Path lib, Map<String, Set<Version>> modules) throws Exception {
    for (var entry : modules.entrySet()) {
      var module = entry.getKey();
      var version = singleton(entry.getValue()).orElse(null);
      resolve(lib, module, version);
    }
  }

  void resolve(Path lib, String module, Version versionOrNull) throws Exception {
    var link = links.get(module);
    if (link == null) {
      throw new Modules.UnmappedModuleException(module);
    }
    var version = versionOrNull != null ? versionOrNull : link.defaultVersion;
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var javafxPlatform = os.contains("win") ? "win" : os.contains("mac") ? "mac" : "linux";
    var uri =
        link.reference
            .replace(Link.VERSION, version.toString())
            .replace(Link.JAVAFX_PLATFORM, javafxPlatform);
    var jar = lib.resolve(module + '-' + version + ".jar");
    uris.copy(URI.create(uri), jar, StandardCopyOption.COPY_ATTRIBUTES);
  }

  @Override
  public String toString() {
    return "Library{" + modifiers + ", links=" + links + ", requires=" + requires + '}';
  }

  private static <T> Optional<T> singleton(Collection<T> collection) {
    if (collection.isEmpty()) {
      return Optional.empty();
    }
    if (collection.size() != 1) {
      throw new IllegalStateException("Too many elements: " + collection);
    }
    return Optional.of(collection.iterator().next());
  }
}
