package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import java.util.Optional;

public record ToolRunningToolCall(Optional<ToolFinder> finder, Command<?> command)
    implements ToolCall {}
