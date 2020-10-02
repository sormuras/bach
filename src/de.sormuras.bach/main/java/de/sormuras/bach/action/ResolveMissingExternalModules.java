/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

package de.sormuras.bach.action;

import de.sormuras.bach.Bach;
import de.sormuras.bach.internal.GitHub;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Resolver;
import de.sormuras.bach.internal.Resources;
import de.sormuras.bach.internal.SormurasModulesProperties;
import de.sormuras.bach.project.Link;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** An action that resolves missing external modules. */
public class ResolveMissingExternalModules implements Action {

  private final Bach bach;
  private final List<Link> computedLinks;
  private final List<Link> resolvedLinks;
  private /*lazy*/ SormurasModulesProperties sormurasModulesProperties;

  public ResolveMissingExternalModules(Bach bach) {
    this.bach = bach;
    this.computedLinks = new ArrayList<>();
    this.resolvedLinks = new ArrayList<>();
    this.sormurasModulesProperties = null;
  }

  @Override
  public Bach bach() {
    return bach;
  }

  @Override
  public void execute() {
    resolveMissingExternalModules();

    if (!resolvedLinks.isEmpty()) {
      var s = resolvedLinks.size() == 1 ? "" : "s";
      log(Level.INFO, "\n");
      log(Level.INFO, "Resolved %d external module%s", resolvedLinks.size(), s);
    }
    for (var link : computedLinks) {
      var module = link.module();
      var uri = link.toURI();
      log(Level.WARNING, "Computed URI for module %s: %s", module, uri);
    }

    if (Files.isDirectory(base().libraries())) logbook().printSummaryOfModules(base().libraries());
  }

  public Optional<Link> computeLink(String module) {
    // https://github.com/USER/REPO/releases/download/VERSION/MODULE@VERSION.jar
    if (module.startsWith("com.github.")) {
      var split = module.split("\\.");
      if (split.length >= 4) {
        assert "com".equals(split[0]);
        assert "github".equals(split[1]);
        var user = split[2];
        var repo = split[3];
        var github = new GitHub(user, repo);
        var latest = github.findLatestReleaseTag();
        if (latest.isPresent()) {
          var version = latest.orElseThrow();
          var uri = github.findReleasedModule(module, version);
          if (uri.isPresent()) return Optional.of(Link.of(module, uri.orElseThrow()));
        }
      }
    }
    if (sormurasModulesProperties == null) {
      sormurasModulesProperties = new SormurasModulesProperties(bach()::http, Map.of());
    }
    return sormurasModulesProperties.lookup(module);
  }

  public void resolveMissingExternalModules() {
    var libraries = base().libraries();
    var resolver =
        new Resolver(List.of(libraries), project().toDeclaredModuleNames(), this::resolveModules);
    resolver.resolve(project().toRequiredModuleNames()); // from all module-info.java files
    resolver.resolve(project().library().toRequiredModuleNames()); // from project descriptor
  }

  public void resolveModules(Set<String> modules) {
    log(Level.INFO, "\n");
    var listing = String.join(", ", modules);
    if (modules.size() == 1) log(Level.INFO, "Resolve missing external module %s", listing);
    else log(Level.INFO, "Resolve %d missing external modules: %s", modules.size(), listing);

    var links = new ArrayList<Link>();
    for (var module : modules) {
      var optionalLink = project().library().findLink(module);
      if (optionalLink.isEmpty()) {
        optionalLink = computeLink(module);
        if (optionalLink.isEmpty()) {
          log(Level.ERROR, "Module %s not resolvable", module);
          continue;
        }
        computedLinks.add(optionalLink.orElseThrow());
      }
      links.add(optionalLink.orElseThrow());
    }

    bach().run(this::resolveLink, links);
  }

  public void resolveLink(Link link) {
    var module = link.module();
    var uri = link.toURI();
    log(Level.INFO, "- %s << %s", module, uri);
    try {
      var lib = Paths.createDirectories(base().libraries());
      new Resources(bach().http()).copy(uri, lib.resolve(link.toModularJarFileName()));
      resolvedLinks.add(link);
    } catch (Exception e) {
      throw new Error("Resolve module '" + module + "' failed: " + uri + "\n\t" + e, e);
    }
  }
}
