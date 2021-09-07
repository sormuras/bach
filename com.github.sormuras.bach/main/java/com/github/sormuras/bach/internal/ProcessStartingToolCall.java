package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.ToolCall;
import java.nio.file.Path;

public record ProcessStartingToolCall(Path executable, Command<?> command) implements ToolCall {}
