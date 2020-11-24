package com.github.sormuras.bach.project;

import com.github.sormuras.bach.module.ModuleSearcher;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An external modules configuration.
 *
 * @param requires the set of additionally required modules
 * @param links the map of preferred module-uri pairs
 * @param searchers the list of module's uri searchers
 * */
public record Library(
    Set<String> requires, Map<String, String> links, List<ModuleSearcher> searchers) {}
