package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import java.util.List;
import java.util.Optional;

public record ToolRunningToolCall(Optional<ToolFinder> finder, String name, List<String> arguments)
    implements ToolCall {}
