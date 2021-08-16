import static com.github.sormuras.bach.Note.caption;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolFinder;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.spi.ToolProvider;

class versions {
  public static void main(String... args) {
    var bach = new Bach("--verbose");

    bach.log(caption("Default Tool Finder"));
    bach.run("jar", "--version");

    bach.log(caption("Explicit Tool Finder"));
    bach.run(ToolFinder.ofSystem(), "jar", "--version");

    bach.log(caption("Start an external process"));
    bach.run(Path.of(System.getProperty("java.home"), "bin", "jar"), "--version");

    bach.log(caption("Versions of all tools provided by the runtime system"));
    ToolFinder.ofSystem().findAll().stream()
        .sorted(Comparator.comparing(ToolProvider::name))
        .forEach(tool -> bach.run(tool, tool.name().equals("javap") ? "-version" : "--version"));
  }
}
