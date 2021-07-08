package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public record DeclaredPath(Set<PathType> types, Path path, Optional<JavaRelease> release) {}
