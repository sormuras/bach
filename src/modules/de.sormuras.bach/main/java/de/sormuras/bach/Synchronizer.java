package de.sormuras.bach;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Callable;

/*BODY*/
/** Launch the JUnit Platform Console using compiled test modules. */
public /*STATIC*/ class Synchronizer implements Callable<Integer> {

  final Bach bach;
  final Run run;

  Synchronizer(Bach bach) {
    this.bach = bach;
    this.run = bach.run;
  }

  /** Resolve required external assets, like 3rd-party modules. */
  @Override
  public Integer call() throws Exception {
    run.log(TRACE, "Synchronizer::call()");
    if (run.isOffline()) {
      run.log(INFO, "Offline mode is active, no synchronization.");
      return 0;
    }
    sync(bach.project.lib, "module-uri.properties");
    // TODO syncMissingLibrariesByParsingModuleDescriptors();
    return 0;
  }

  private void sync(Path root, String fileName) throws Exception {
    run.log(DEBUG, "Synchronizing 3rd-party module uris below: %s", root.toUri());
    if (Files.notExists(root)) {
      run.log(DEBUG, "Not synchronizing because directory doesn't exist: %s", root);
      return;
    }
    var paths = new ArrayList<Path>();
    try (var stream = Files.walk(root)) {
      stream.filter(path -> path.getFileName().toString().equals(fileName)).forEach(paths::add);
    }
    var synced = new ArrayList<Path>();
    for (var path : paths) {
      var directory = path.getParent();
      var downloader = new Downloader(run, directory);
      var properties = new Properties();
      try (var stream = Files.newInputStream(path)) {
        properties.load(stream);
      }
      if (properties.isEmpty()) {
        run.log(DEBUG, "No module uri declared in %s", path.toUri());
        continue;
      }
      run.log(DEBUG, "Syncing %d module uri(s) to %s", properties.size(), directory.toUri());
      for (var value : properties.values()) {
        var string = run.replaceVariables(value.toString());
        var uri = URI.create(string);
        uri = uri.isAbsolute() ? uri : run.home.resolve(string).toUri();
        run.log(DEBUG, "Syncing %s", uri);
        var target = downloader.download(uri);
        synced.add(target);
        run.log(DEBUG, " o %s", target.toUri());
      }
    }
    run.log(DEBUG, "Synchronized %d module uri(s).", synced.size());
  }
}
