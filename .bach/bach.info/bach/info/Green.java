package bach.info;

import com.github.sormuras.bach.Bach;

public class Green implements Bach.OnTestsSuccessful {
  @Override
  public void onTestsSuccessful(Event event) {
    event.bach().say("Green");
  }
}
