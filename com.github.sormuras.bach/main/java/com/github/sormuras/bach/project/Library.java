package com.github.sormuras.bach.project;

import java.util.Map;
import java.util.Set;

/**
 * An external modules configuration.
 */
public record Library(Set<String> requires, Map<String, String> links) {}
