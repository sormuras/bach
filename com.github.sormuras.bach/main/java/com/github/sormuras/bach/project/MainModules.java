package com.github.sormuras.bach.project;

import java.util.Optional;
import java.util.Set;

public record MainModules(
    Set<DeclaredModule> set,
    Optional<JavaRelease> release,
    ModuleSourcePaths moduleSourcePaths) {}
