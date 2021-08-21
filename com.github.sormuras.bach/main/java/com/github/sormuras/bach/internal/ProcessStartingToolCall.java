package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;
import java.util.List;

public record ProcessStartingToolCall(Path executable, List<String> arguments)
    implements ToolCall {}
