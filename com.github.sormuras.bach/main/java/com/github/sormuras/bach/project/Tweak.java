package com.github.sormuras.bach.project;

import java.util.List;

/** An additional list of arguments for a given trigger, usually a tool's name. */
public record Tweak(String trigger, List<String> arguments) {}
