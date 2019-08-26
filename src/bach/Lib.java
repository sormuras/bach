/*
 * Bach - Library Support Tool
 *
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.lang.model.SourceVersion;

/**
 * Library Support Tool.
 *
 * <p>{@code java Lib.java [org.junit.jupiter[@5.5.1],...]}
 */
public class Lib {

  public static void main(String... args) {
    var path = Path.of(System.getProperty("lib", "lib"));
    var src = Path.of(System.getProperty("src", "src"));
    var projectModuleNames = findAllDirectoryNamesIn(src);
    var synthetic = synthesize(src, projectModuleNames, args);
    var lib = new Lib(path, projectModuleNames);
    lib.load(synthetic);
    lib.sync();
    lib.list();
  }

  private static Set<String> findAllDirectoryNamesIn(Path root) {
    if (!Files.isDirectory(root)) {
      return Set.of();
    }
    try (var entries = Files.newDirectoryStream(root, Files::isDirectory)) {
      return StreamSupport.stream(entries.spliterator(), false)
          .map(Path::getFileName)
          .map(Path::toString)
          .collect(Collectors.toCollection(TreeSet::new));
    } catch (IOException e) {
      throw new UncheckedIOException("reading directory failed: " + root, e);
    }
  }

  private static ModuleDescriptor synthesize(Path src, Set<String> modules, String... args) {
    var synthetic = ModuleDescriptor.newModule("$", Set.of(ModuleDescriptor.Modifier.SYNTHETIC));
    if (args.length == 0) {
      var requiresPattern =
          Pattern.compile(
              "(?:requires)" // key word
                  + "(?:\\s+[\\w.]+)?" // optional modifiers
                  + "\\s+([\\w.]+)" // module name
                  + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                  + ";"); // end marker
      for (var module : modules) {
        var root = src.resolve(module);
        try (var stream = Files.find(root, 9, (p, __) -> isModuleInfo(p))) {
          for (var moduleInfo : stream.collect(Collectors.toSet())) {
            var source = Files.readString(moduleInfo);
            var requiresMatcher = requiresPattern.matcher(source);
            while (requiresMatcher.find()) {
              var requiredName = requiresMatcher.group(1);
              Optional.ofNullable(requiresMatcher.group(2))
                  .ifPresentOrElse(
                      version ->
                          synthetic.requires(
                              Set.of(), requiredName, ModuleDescriptor.Version.parse(version)),
                      () -> synthetic.requires(requiredName));
            }
          }
        } catch (IOException e) {
          throw new UncheckedIOException("reading module-info.java files failed: " + root, e);
        }
      }
    }
    for (var arg : args) {
      if (arg.indexOf('@') >= 0) {
        var split = arg.split("@");
        synthetic.requires(Set.of(), split[0], ModuleDescriptor.Version.parse(split[1]));
        continue;
      }
      synthetic.requires(Set.of(), arg);
    }
    return synthetic.build();
  }

  private static boolean isModuleInfo(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
  }

  private final Path path;
  private final Set<String> projectModuleNames;
  private final Set<String> systemModuleNames;
  private final ModuleProperties moduleMavenProperties;
  private final ModuleProperties moduleVersionProperties;

  Lib(Path path, Set<String> projectModuleNames) {
    this.path = path;
    this.projectModuleNames = projectModuleNames;
    this.systemModuleNames = mapAll(ModuleFinder.ofSystem(), ModuleDescriptor::name);
    this.moduleMavenProperties = new ModuleProperties(path, "module-maven.properties");
    this.moduleVersionProperties = new ModuleProperties(path, "module-version.properties");
  }

  private void list() {
    var finder = ModuleFinder.of(path);
    System.out.printf("Library '%s' contains %d module(s)%n", path, finder.findAll().size());
    for (var nameAndVersion : mapAll(finder, ModuleDescriptor::toNameAndVersion)) {
      System.out.println(nameAndVersion);
    }
  }

  private Set<String> mapAll(ModuleFinder finder, Function<ModuleDescriptor, String> mapper) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(mapper)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private void sync() {
    ModuleFinder.of(path).findAll().stream().map(ModuleReference::descriptor).forEach(this::load);
  }

  private void load(ModuleDescriptor descriptor) {
    System.out.println("Load requires of " + descriptor.name() + ": " + descriptor.requires());
    var finder = ModuleFinder.of(path);
    for (var requires : descriptor.requires()) {
      load(finder, requires);
    }
  }

  private void load(ModuleFinder libraryFinder, ModuleDescriptor.Requires requires) {
    var name = requires.name();
    if (projectModuleNames.contains(name)) {
      System.out.println("project: " + name);
      return;
    }
    if (systemModuleNames.contains(name)) {
      System.out.println("system : " + name);
      return;
    }
    var libraryModule = libraryFinder.find(name);
    if (libraryModule.isPresent()) {
      System.out.println(
          "library: " + name + " -> " + libraryModule.get().descriptor().toNameAndVersion());
      return;
    }
    var maven = moduleMavenProperties.get(name).split(":");
    var group = maven[0];
    var artifact = maven[1];
    var version = moduleVersionProperties.get(name);
    var host = "https://repo1.maven.org/maven2";
    var file = artifact + '-' + version + ".jar";
    var uri = URI.create(String.join("/", host, group.replace('.', '/'), artifact, version, file));
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new UncheckedIOException("Creating library directory failed: " + path, e);
    }
    loadFile(path.resolve(name + "@" + version + ".jar"), uri);
  }

  static class ModuleProperties {
    final String name;
    final Properties properties;
    final Set<Pattern> patterns;

    ModuleProperties(Path lib, String name) {
      this.name = name;
      var uri = "https://github.com/sormuras/modules/raw/master/" + name;
      var home = Path.of(System.getProperty("user.home")).resolve(".bach/modules");
      try {
        Files.createDirectories(home);
      } catch (IOException e) {
        throw new UncheckedIOException("Creating directories failed: " + home, e);
      }
      var user = loadFile(home.resolve(name), URI.create(uri));
      var defaults = loadProperties(new Properties(), user);
      this.properties = loadProperties(new Properties(defaults), lib.resolve(name));
      this.patterns =
          properties.keySet().stream()
              .map(Object::toString)
              .filter(key -> !SourceVersion.isName(key))
              .map(Pattern::compile)
              .collect(Collectors.toSet());
    }

    String get(String key) {
      var value = properties.getProperty(key);
      if (value != null) {
        return value;
      }
      for (var pattern : patterns) {
        if (pattern.matcher(key).matches()) {
          return properties.getProperty(pattern.pattern());
        }
      }
      return null;
    }

    @Override
    public String toString() {
      var size = properties.size();
      var names = properties.stringPropertyNames().size();
      return String.format("module properties {name: %s, size: %d, names: %d}", name, size, names);
    }
  }

  private static Properties loadProperties(Properties properties, Path path) {
    if (Files.isRegularFile(path)) {
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      } catch (IOException e) {
        throw new UncheckedIOException("Reading properties failed: " + path, e);
      }
    }
    return properties;
  }

  private static Path loadFile(Path file, URI uri) {
    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    return loadFile(file, httpClient, uri);
  }

  private static Path loadFile(Path file, HttpClient httpClient, URI uri) {
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(file)) {
      try {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      } catch (Exception e) {
        // System.err.println("Couldn't get 'user:etag' file attribute: " + e);
      }
    }
    try {
      var handler = HttpResponse.BodyHandlers.ofFile(file);
      var response = httpClient.send(request.build(), handler);
      if (response.statusCode() == 200) {
        var etagHeader = response.headers().firstValue("etag");
        if (etagHeader.isPresent()) {
          try {
            var etag = etagHeader.get();
            Files.setAttribute(file, "user:etag", StandardCharsets.UTF_8.encode(etag));
          } catch (Exception e) {
            // System.err.println("Couldn't set 'user:etag' file attribute: " + e);
          }
        }
        var lastModifiedHeader = response.headers().firstValue("last-modified");
        if (lastModifiedHeader.isPresent()) {
          try {
            var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
            var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
            Files.setLastModifiedTime(file, fileTime);
          } catch (Exception e) {
            // System.err.println("Couldn't set last modified file attribute: " + e);
          }
        }
        System.out.println(file + " <- " + uri);
      }
    } catch (IOException | InterruptedException e) {
      System.err.println("Failed to load: " + uri + " -> " + e);
    }
    return file;
  }
}
