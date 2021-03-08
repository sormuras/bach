package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Recording;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.util.Records;
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
public interface LogbookWriterAPI {

  Bach bach();

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

    md.addAll(writeLogbookModulesOverviewLines());
    md.addAll(writeLogbookRecordingsOverviewLines());
    md.addAll(writeLogbookRecordingsDetailLines());
    md.addAll(writeLogbookProjectConfiguration());

    md.add("");
    md.add("## Thanks for using Bach");
    md.add("");
    md.add("Support its development at <https://github.com/sponsors/sormuras>");
    return md;
  }

  default List<String> writeLogbookModulesOverviewLines() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Modules");
    md.add("");

    var directory = bach().folders().workspace("modules");

    record ModularJar(Path path, long size, ModuleDescriptor descriptor) {}
    var jars = new ArrayList<ModularJar>();
    try (var stream = Files.newDirectoryStream(directory, "*.jar")) {
      for (var path : stream) {
        var size = Files.size(path);
        var descriptor = ModuleFinder.of(path).findAll().iterator().next().descriptor();
        jars.add(new ModularJar(path, size, descriptor));
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
    md.add("| Size | File | Module |");
    md.add("|-----:|------|--------|");
    for (var jar : jars) {
      var file = jar.path.getFileName();
      var module = jar.descriptor.name();
      md.add(String.format("|%,d bytes|`%s`|`%s`", jar.size, file, module));
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

  default List<String> writeLogbookRecordingsOverviewLines() {
    var recordings = bach().recordings();
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Run Overview");
    md.add("");
    var size = recordings.size();
    md.add(String.format("Recorded %d tool run%s.", size, size == 1 ? "" : "s"));
    md.add("");
    md.add("|Thread| Duration |Tool|Arguments");
    md.add("|-----:|---------:|----|---------");
    for (var recording : recordings) {
      var thread = recording.thread();
      var duration = Strings.toString(recording.duration());
      var tool = "[" + recording.name() + "](#" + markdownAnchor(recording) + ")";
      var arguments = "`" + String.join("` `", recording.args()) + "`";
      var row = String.format("|%6X|%10s|%s|%s", thread, duration, tool, arguments);
      md.add(row);
    }
    return md;
  }

  default List<String> writeLogbookRecordingsDetailLines() {
    var recordings = bach().recordings();
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Recordings");
    md.add("");
    var size = recordings.size();
    md.add(String.format("%d recording%s", size, size == 1 ? "" : "s"));

    for (var recording : recordings) {
      md.add("");
      md.add("### " + markdownAnchor(recording));
      md.add("");
      md.add("- tool = `" + recording.name() + '`');
      md.add("- args = `" + String.join("` `", recording.args()) + '`');
      md.add("- thread = " + recording.thread());
      md.add("- duration = " + Strings.toString(recording.duration()));
      md.add("- code = " + recording.code());
      if (!recording.output().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(recording.output()));
        md.add("```");
      }
      if (!recording.errors().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(recording.errors()));
        md.add("```");
      }
    }
    return md;
  }

  default List<String> writeLogbookProjectConfiguration() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Project Configuration");
    md.add("");
    md.add("```text");
    md.add(Records.toLines(bach().project()));
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

  private static String markdownAnchor(Recording recording) {
    return recording.name() + '-' + Integer.toHexString(System.identityHashCode(recording));
  }
}
