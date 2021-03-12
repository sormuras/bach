package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.internal.Strings;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Methods for writing logbooks in markdown format. */
public interface LogbookWriterAPI extends API {

  default Path writeLogbook() throws Exception {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var lines = writeLogbookLines(now);

    var folder = bach().folders();
    Files.createDirectories(folder.workspace());
    var file = Files.write(folder.workspace("logbook.md"), lines);
    var logbooks = Files.createDirectories(folder.workspace("logbooks"));
    var timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(now);
    Files.copy(file, logbooks.resolve("logbook-" + timestamp + ".md"));
    return file;
  }

  default List<String> writeLogbookLines(LocalDateTime now) {
    var md = new ArrayList<String>();
    md.add("# Logbook");
    md.add("");

    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    md.add(String.format("- %s", formatter.format(now)));

    var folder = bach().folders();
    md.add(String.format("- root = `%s`", folder.root().toAbsolutePath()));
    md.add(String.format("- external-modules = `%s`", folder.externalModules()));
    md.add(String.format("- external-tools = `%s`", folder.externalTools()));
    md.add(String.format("- workspace = `%s`", folder.workspace()));

    md.addAll(writeLogbookModulesOverview());
    md.addAll(writeLogbookToolRunOverview());
    md.addAll(writeLogbookToolRunDetails());
    md.addAll(writeLogbookMessages());

    md.add("");
    md.add("## Thanks for using Bach");
    md.add("");
    md.add("Support its development at <https://github.com/sponsors/sormuras>");
    return md;
  }

  default List<String> writeLogbookModulesOverview() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Modules");
    md.add("");

    var directory = bach().folders().workspace("modules");

    record ModularJar(Path path, long size, ModuleDescriptor descriptor, String md5, String sha) {}
    var jars = new ArrayList<ModularJar>();
    try (var stream = Files.newDirectoryStream(directory, "*.jar")) {
      for (var path : stream) {
        var size = Files.size(path);
        var descriptor = ModuleFinder.of(path).findAll().iterator().next().descriptor();
        var md5 = ExternalModuleAPI.hash("MD5", path);
        var sha = ExternalModuleAPI.hash("SHA-256", path);
        jars.add(new ModularJar(path, size, descriptor, md5, sha));
      }
    } catch (Exception exception) {
      md.add(String.format("Streaming directory `%s` failed", directory));
      md.add("");
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
      md.add(String.format("|%,d bytes|`%s`|`%s` | %s | %s ", size, file, name, md5, sha));
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
      md.add(String.format("|`%s`|%s|%s|%s|`%s`|", module, version, exports, provides, main));
    }
    return md;
  }

  default List<String> writeLogbookToolRunOverview() {
    var runs = bach().logbook().runs();
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Overview");
    md.add("");
    var size = runs.size();
    md.add(String.format("Recorded %d tool run%s.", size, size == 1 ? "" : "s"));
    md.add("");
    md.add("|Thread| Duration |Tool|Arguments");
    md.add("|-----:|---------:|----|---------");
    for (var run : runs) {
      var thread = run.thread();
      var duration = Strings.toString(run.duration());
      var tool = "[" + run.name() + "](#" + markdownAnchor(run) + ")";
      var arguments = "`" + String.join("` `", run.args()) + "`";
      var row = String.format("|%6X|%10s|%s|%s", thread, duration, tool, arguments);
      md.add(row);
    }
    return md;
  }

  default List<String> writeLogbookToolRunDetails() {
    var runs = bach().logbook().runs();
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Details");
    md.add("");
    var size = runs.size();
    md.add(String.format("%d recording%s", size, size == 1 ? "" : "s"));

    for (var run : runs) {
      md.add("");
      md.add("### " + markdownAnchor(run));
      md.add("");
      md.add("- tool = `" + run.name() + '`');
      md.add("- args = `" + String.join("` `", run.args()) + '`');
      md.add("- thread = " + run.thread());
      md.add("- duration = " + Strings.toString(run.duration()));
      md.add("- code = " + run.code());
      if (!run.output().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(run.output()));
        md.add("```");
      }
      if (!run.errors().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(run.errors()));
        md.add("```");
      }
    }
    return md;
  }

  default List<String> writeLogbookMessages() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Log Messages");
    md.add("");
    md.add("```text");
    for (var message : bach().logbook().messages()) {
      md.add(String.format("[%s] %s", message.level().name().toCharArray()[0], message.text()));
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

  private static String markdownAnchor(Logbook.Run recording) {
    return recording.name() + '-' + Integer.toHexString(System.identityHashCode(recording));
  }
}
