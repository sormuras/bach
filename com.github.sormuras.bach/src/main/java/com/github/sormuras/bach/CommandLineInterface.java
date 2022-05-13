package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ArgVester.Opt;
import java.util.List;
import java.util.Optional;

/** Bach's command-line interface. */
public record CommandLineInterface(
    Optional<Boolean> help,
    Optional<Boolean> verbose,
    Optional<Boolean> dry_run,
    // Basic Properties
    Optional<String> root_directory,
    Optional<String> output_directory,
    Optional<String> module_info_find_pattern,
    // Project Properties
    Optional<String> project_name,
    Optional<String> project_version,
    Optional<String> project_version_date,
    Optional<String> project_targets_java,
    Optional<String> project_launcher,
    // Initial Tool Call
    @Opt(help = "The initial tool call: TOOL-NAME [TOOL-ARGS...]") List<String> command) {}
