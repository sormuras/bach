package run.bach;

import java.util.Iterator;
import java.util.List;
import run.duke.ToolCall.Tweak;

public record Tweaks(List<Tweak> list) implements Iterable<Tweak> {
  public Tweaks(Tweak... tweaks) {
    this(List.of(tweaks));
  }

  @Override
  public Iterator<Tweak> iterator() {
    return list.iterator();
  }
}
