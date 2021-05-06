package com.github.sormuras.bach.api;

import java.lang.module.ModuleDescriptor.Version;

public record Project(
    String name,
    Version version,
    Folders folders,
    Spaces spaces,
    Tools tools,
    Externals externals) {}
