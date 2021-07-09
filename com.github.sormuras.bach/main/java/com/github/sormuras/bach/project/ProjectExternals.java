package com.github.sormuras.bach.project;

import com.github.sormuras.bach.external.ExternalModuleLocator;
import java.util.List;
import java.util.Set;

public record ProjectExternals(Set<String> requires, List<ExternalModuleLocator> locators) {}
