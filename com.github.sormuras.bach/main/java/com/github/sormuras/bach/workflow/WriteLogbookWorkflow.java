package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.internal.Durations;
import com.github.sormuras.bach.internal.Paths;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
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
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class WriteLogbookWorkflow extends Workflow {

  public WriteLogbookWorkflow(Bach bach) {
    super(bach);
  }

  public void execute() {
    // Generate lines to write
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var lines = generate(now);
    try {
      var file = write(now, lines);
      bach.log(Level.INFO, "Logbook written to %s", file.toUri());
    } catch (IOException exception) {
      throw new UncheckedIOException("Write logbook failed", exception);
    }
  }

  private Path write(LocalDateTime now, List<String> lines) throws IOException {
    // Write file in workspace directory
    var folders = bach.folders();
    Files.createDirectories(folders.workspace());
    var file =
        Files.write(
            folders.workspace("logbook.md"),
            lines,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
    // Store a copy with timestamp in "build history" directory
    var logbooks = Files.createDirectories(folders.workspace("logbooks"));
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

    var folders = bach.folders();
    md.add("- root = `%s`".formatted(folders.root().toAbsolutePath()));
    md.add("- external-modules = `%s`".formatted(folders.externalModules()));
    md.add("- workspace = `%s`".formatted(folders.workspace()));

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

    var directory = bach.folders().workspace("modules");
    record ModularJar(Path path, long size, ModuleDescriptor descriptor, String md5, String sha) {}
    var jars = new ArrayList<ModularJar>();
    try {
      for (var path : Paths.list(directory, Paths::isJarFile)) {
        var size = Files.size(path);
        var descriptor = ModuleFinder.of(path).findAll().iterator().next().descriptor();
        var md5 = Paths.hash(path, "MD5");
        var sha = Paths.hash(path, "SHA-256");
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
    var runs = bach.logbook().runs();
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Overview");
    md.add("");
    var size = runs.size();
    md.add("Recorded %d tool run result%s.".formatted(size, size == 1 ? "" : "s"));
    if (size < 1) return md;

    md.add("");
    md.add("|Thread| Duration |Tool|Arguments");
    md.add("|-----:|---------:|----|---------");
    for (var run : runs) {
      var thread = run.thread();
      var duration = Durations.beautify(run.duration());
      var tool = "[" + run.name() + "](#" + markdownAnchor(run) + ")";
      var arguments = markdownJoin(run.args(), 6, false, " ");
      var row = "|%6X|%10s|%s|%s".formatted(thread, duration, tool, arguments);
      md.add(row);
    }
    return md;
  }

  public List<String> generateToolRunDetails() {
    var runs = bach.logbook().runs();
    if (runs.isEmpty()) return List.of();

    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Details");
    md.add("");
    var size = runs.size();
    md.add("%d recording%s".formatted(size, size == 1 ? "" : "s"));
    for (var run : runs) {
      md.add("");
      md.add("### " + markdownAnchor(run));
      md.add("");
      md.add("```");
      if (run.args().isEmpty()) md.add(run.name());
      else md.add(run.name() + " " + String.join(" ", run.args()));
      md.add("```");
      md.add("");
      md.add("- thread = " + run.thread());
      md.add("- duration = " + Durations.beautify(run.duration()));
      md.add("- code = " + run.code());
      if (!run.output().isEmpty()) {
        md.add("");
        md.add(
            markdownDetails(
                "Normal (expected) output",
                """
                ```text
                %s
                ```
                """
                    .formatted(markdown(run.output()))));
      }
      if (!run.errors().isEmpty()) {
        md.add("");
        md.add(
            markdownDetails(
                "Error messages",
                """
                ```text
                %s
                ```
                """
                    .formatted(markdown(run.errors()))));
      }
    }
    return md;
  }

  public List<String> generateMessages() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Log Messages");
    md.add("");

    var lines = new StringJoiner("\n");
    lines.add("```text");
    for (var message : bach.logbook().messages()) {
      var level = message.level().name().toCharArray()[0];
      var text = markdown(message.text());
      lines.add("[%s] %s".formatted(level, text));
    }
    lines.add("```");
    md.add(markdownDetails("Messages", lines.toString()));
    return md;
  }

  private static String markdown(Object object) {
    return object.toString().replace('\t', ' ').replaceAll("\\e\\[[\\d;]*[^\\d;]", "");
  }

  private static String markdownJoin(Collection<?> collection) {
    return markdownJoin(collection, Long.MAX_VALUE, true, ", ");
  }

  private static String markdownJoin(Collection<?> collection, long limit, boolean sorted, String delimiter) {
    if (collection.isEmpty()) return "`-`";
    var strings = collection.stream().limit(limit).map(Object::toString);
    var collected = (sorted ? strings.sorted() : strings)
        .collect(Collectors.joining("`" + delimiter + "`", "`", "`"));
    return limit < collection.size() ? collected + " [...]" : collected;
  }

  private static String markdownAnchor(Logbook.Run run) {
    return run.name() + '-' + Integer.toHexString(System.identityHashCode(run));
  }

  private static String markdownDetails(String label, String contents) {
    return """
        <details>
        <summary>%s</summary>
        
        %s
        
        </details>
        """
        .formatted(label, contents.strip());
  }
}
