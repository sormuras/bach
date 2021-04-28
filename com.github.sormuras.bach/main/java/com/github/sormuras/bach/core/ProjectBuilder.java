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
import java.util.List;
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
    var name = buildProjectName();
    var folders = buildFolders();
    var spaces = buildSpaces();
    var externals = buildExternals();
    return new Project(name, folders, spaces, externals);
  }

  public String buildProjectName() {
    return options.get(Option.PROJECT_NAME);
  }

  public Folders buildFolders() {
    return Folders.of(options.get(Option.CHROOT));
  }

  public Spaces buildSpaces() {
    var main = CodeSpaceMain.empty();
    var test = CodeSpaceTest.empty();
    return new Spaces(main, test);
  }

  public Externals buildExternals() {
    var requires = buildExternalsRequires();
    var locators = buildExternalsLocators();
    return new Externals(requires, locators);
  }

  public Set<String> buildExternalsRequires() {
    return Set.copyOf(options.list(Option.PROJECT_REQUIRES));
  }

  public List<ExternalModuleLocator> buildExternalsLocators() {
    var locators = new ArrayList<ExternalModuleLocator>();
    fillExternalsLocatorsFromOptionModuleLocation(locators);
    fillExternalsLocatorsFromOptionLibraryVersion(locators);
    return List.copyOf(locators);
  }

  public void fillExternalsLocatorsFromOptionModuleLocation(List<ExternalModuleLocator> locators) {
    var deque = new ArrayDeque<>(options.list(Option.EXTERNAL_MODULE_LOCATION));
    var locationMap = new TreeMap<String, ExternalModuleLocation>();
    while (!deque.isEmpty()) {
      var module = deque.removeFirst();
      var uri = deque.removeFirst();
      var old = locationMap.put(module, new ExternalModuleLocation(module, uri));
      if (old != null) logbook.log(Level.WARNING, "Replaced %s with -> %s".formatted(old, uri));
    }
    locators.add(new ExternalModuleLocations(Map.copyOf(locationMap)));
  }

  public void fillExternalsLocatorsFromOptionLibraryVersion(List<ExternalModuleLocator> locators) {
    var deque = new ArrayDeque<>(options.list(Option.EXTERNAL_LIBRARY_VERSION));
    while (!deque.isEmpty()) {
      var name = deque.removeFirst();
      var version = deque.removeFirst();
      //noinspection SwitchStatementWithTooFewBranches
      switch (ExternalLibraryName.ofCli(name)) {
        case JUNIT -> locators.add(JUnit.of(version));
      }
    }
  }
}
