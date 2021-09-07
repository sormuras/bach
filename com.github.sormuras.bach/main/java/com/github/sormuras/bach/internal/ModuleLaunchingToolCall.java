package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import java.lang.module.ModuleFinder;

public record ModuleLaunchingToolCall(ModuleFinder finder, Command<?> command)
    implements ToolCall {}
