package com.github.sormuras.bach.command;

import java.nio.file.Path;
import java.util.List;

/**
 * Targets a list of paths to a Java feature version, with {@code 0} indicating no target version.
 *
 * @param version the Java feature release version to target
 * @param paths the list of paths
 */
public record TargetedPaths(int version, List<Path> paths) {}
