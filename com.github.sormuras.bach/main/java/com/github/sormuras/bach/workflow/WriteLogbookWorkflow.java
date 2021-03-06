package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.internal.Strings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WriteLogbookWorkflow extends BachWorkflow {

  public WriteLogbookWorkflow(Bach bach) {
    super(bach);
  }

  public void write() {
    // Generate lines to write
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var lines = generate(now);
    try {
      var file = write(now, lines);
      bach().say("Logbook written to %s".formatted(file.toUri()));
    } catch (IOException exception) {
      throw new UncheckedIOException("Write logbook failed", exception);
    }
  }

  private Path write(LocalDateTime now, List<String> lines) throws IOException {
    // Write file in workspace directory
    var folder = bach().project().folders();
    Files.createDirectories(folder.workspace());
    var file =
        Files.write(
            folder.workspace("logbook.md"),
            lines,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
    // Store a copy with timestamp in "build history" directory
    var logbooks = Files.createDirectories(folder.workspace("logbooks"));
    var timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(now);
    Files.copy(
        file,
        logbooks.resolve("logbook-" + timestamp + ".md"),
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING);
    return file;
  }

  public List<String> generate(LocalDateTime now) {
    var md = new ArrayList<String>();
    md.add("""
        # Logbook

        """);

    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    md.add("- %s".formatted(formatter.format(now)));

    var folder = bach().project().folders();
    md.add("- root = `%s`".formatted(folder.root().toAbsolutePath()));
    md.add("- external-modules = `%s`".formatted(folder.externalModules()));
    md.add("- workspace = `%s`".formatted(folder.workspace()));

    md.addAll(generateModulesOverview());
    md.addAll(generateToolRunOverview());
    md.addAll(generateToolRunDetails());
    md.addAll(generateMessages());

    md.add(
        """
        ## Thanks for using Bach

        Support its development at <https://github.com/sponsors/sormuras>
        """);
    return md;
  }

  public List<String> generateModulesOverview() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Modules");
    md.add("");

    var directory = bach().project().folders().modules(CodeSpace.MAIN);
    record ModularJar(Path path, long size, ModuleDescriptor descriptor, String md5, String sha) {}
    var jars = new ArrayList<ModularJar>();
    try (var stream = Files.newDirectoryStream(directory, "*.jar")) {
      for (var path : stream) {
        var size = Files.size(path);
        var descriptor = ModuleFinder.of(path).findAll().iterator().next().descriptor();
        var md5 = Strings.hash("MD5", path);
        var sha = Strings.hash("SHA-256", path);
        jars.add(new ModularJar(path, size, descriptor, md5, sha));
      }
    } catch (Exception exception) {
      md.add("Stream directory `%s` failed due to:".formatted(directory));
      md.add("");
      md.add("> " + markdown(exception.getClass()));
      md.add(">");
      md.add("> " + markdown(exception.getMessage()));
      return md;
    }

    md.add(
        String.format(
            "Directory `%s` contains %d modular JAR file%s.",
            directory.toAbsolutePath(), jars.size(), jars.size() == 1 ? "" : "s"));
    if (jars.isEmpty()) return md;

    md.add("");
    md.add("| Size | File | Module | MD5 | SHA-256 |");
    md.add("|-----:|------|--------|-----|---------|");
    for (var jar : jars) {
      var size = jar.size;
      var file = jar.path.getFileName();
      var name = jar.descriptor.name();
      var md5 = jar.md5;
      var sha = jar.sha;
      md.add("|%,d bytes|`%s`|`%s` | %s | %s ".formatted(size, file, name, md5, sha));
    }

    md.add("");
    md.add("### Module API");
    md.add("");
    md.add("| Name | Version | Exports | Provides | Main Class |");
    md.add("|------|---------|---------|----------|------------|");
    for (var jar : jars) {
      var descriptor = jar.descriptor;
      var module = descriptor.name();
      var version = descriptor.version().map(Object::toString).orElse("-");
      var exports = markdownJoin(descriptor.exports());
      var provides = markdownJoin(descriptor.provides());
      var main = descriptor.mainClass().map(Object::toString).orElse("-");
      md.add("|`%s`|%s|%s|%s|`%s`|".formatted(module, version, exports, provides, main));
    }
    return md;
  }

  public List<String> generateToolRunOverview() {
    var results = bach().logbook().runs();
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Overview");
    md.add("");
    var size = results.size();
    md.add("Recorded %d tool run result%s.".formatted(size, size == 1 ? "" : "s"));
    if (size < 1) return md;

    md.add("");
    md.add("|Thread| Duration |Tool|Arguments");
    md.add("|-----:|---------:|----|---------");
    for (var result : results) {
      var thread = result.thread();
      var duration = Strings.toString(result.duration());
      var tool = "[" + result.name() + "](#" + markdownAnchor(result) + ")";
      var arguments = "`" + String.join("` `", result.args()) + "`";
      var row = "|%6X|%10s|%s|%s".formatted(thread, duration, tool, arguments);
      md.add(row);
    }
    return md;
  }

  public List<String> generateToolRunDetails() {
    var results = bach().logbook().runs();
    if (results.isEmpty()) return List.of();

    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Details");
    md.add("");
    var size = results.size();
    md.add("%d recording%s".formatted(size, size == 1 ? "" : "s"));
    for (var result : results) {
      md.add("");
      md.add("### " + markdownAnchor(result));
      md.add("");
      md.add("- tool = `" + result.name() + '`');
      md.add("- args = `" + String.join("` `", result.args()) + '`');
      md.add("- thread = " + result.thread());
      md.add("- duration = " + Strings.toString(result.duration()));
      md.add("- code = " + result.code());
      if (!result.output().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(result.output()));
        md.add("```");
      }
      if (!result.errors().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(result.errors()));
        md.add("```");
      }
    }
    return md;
  }

  public List<String> generateMessages() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Log Messages");
    md.add("");
    md.add("```text");
    for (var message : bach().logbook().messages()) {
      var level = message.level().name().toCharArray()[0];
      var text = message.text();
      md.add("[%s] %s".formatted(level, text));
    }
    md.add("```");
    return md;
  }

  private static String markdown(Object object) {
    return object.toString().replace('\t', ' ').replaceAll("\\e\\[[\\d;]*[^\\d;]", "");
  }

  private static String markdownJoin(Collection<?> collection) {
    if (collection.isEmpty()) return "`-`";
    return collection.stream()
        .map(Object::toString)
        .sorted()
        .collect(Collectors.joining("`, `", "`", "`"));
  }

  private static String markdownAnchor(ToolRun run) {
    return run.name() + '-' + Integer.toHexString(System.identityHashCode(run));
  }
}
