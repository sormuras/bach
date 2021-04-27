package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.ExternalLibraryName;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocations;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.external.JUnit;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ProjectBuilder {

  protected final Logbook logbook;
  protected final Options options;

  public ProjectBuilder(Logbook logbook, Options options) {
    this.logbook = logbook;
    this.options = options;
  }

  public Project build() {
    var name = options.get(Option.PROJECT_NAME);
    var folders = Folders.of(options.get(Option.CHROOT));
    var spaces = new Spaces(new CodeSpaceMain(), new CodeSpaceTest());
    var externals = buildExternals();
    return new Project(name, folders, spaces, externals);
  }

  public Externals buildExternals() {
    var requires = Set.copyOf(options.list(Option.PROJECT_REQUIRES));

    var locators = new ArrayList<ExternalModuleLocator>();
    var locationMap = new TreeMap<String, ExternalModuleLocation>();
    var locationDeque = new ArrayDeque<>(options.list(Option.EXTERNAL_MODULE_LOCATION));
    while (!locationDeque.isEmpty()) {
      var module = locationDeque.removeFirst();
      var uri = locationDeque.removeFirst();
      var old = locationMap.put(module, new ExternalModuleLocation(module, uri));
      if (old != null) logbook.log(Level.WARNING, "Replaced %s with -> %s".formatted(old, uri));
    }
    locators.add(new ExternalModuleLocations(Map.copyOf(locationMap)));
    var libraryDeque = new ArrayDeque<>(options.list(Option.EXTERNAL_LIBRARY_VERSION));
    while (!libraryDeque.isEmpty()) {
      var name = libraryDeque.removeFirst();
      var version = libraryDeque.removeFirst();
      //noinspection SwitchStatementWithTooFewBranches
      switch (ExternalLibraryName.ofCli(name)) {
        case JUNIT -> locators.add(JUnit.of(version));
      }
    }
    return new Externals(requires, locators);
  }
}