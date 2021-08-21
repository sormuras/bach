package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ToolCall;
import java.lang.module.ModuleFinder;
import java.util.List;

public record ModuleLaunchingToolCall(ModuleFinder finder, String module, List<String> arguments)
    implements ToolCall {}
