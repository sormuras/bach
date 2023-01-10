package run.bach;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import run.duke.ToolCall.Tweak;

public record Tweaks(List<Tweak> list) implements Iterable<Tweak> {
  public Tweaks(Tweak... tweaks) {
    this(List.of(tweaks));
  }

  public Tweaks with(Tweak tweak, Tweak... more) {
    var tweaks = new ArrayList<>(list);
    tweaks.add(tweak);
    tweaks.addAll(List.of(more));
    return new Tweaks(List.copyOf(tweaks));
  }

  @Override
  public Iterator<Tweak> iterator() {
    return list.iterator();
  }
}
