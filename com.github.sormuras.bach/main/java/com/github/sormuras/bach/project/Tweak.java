package com.github.sormuras.bach.project;

import java.util.List;

/**
 * A tool call tweak.
 *
 * @param trigger the trigger of this tweak, usually a name of a tool
 * @param arguments the additional arguments to be passed to a triggered tool call
 */
public record Tweak(String trigger, List<String> arguments) {}
