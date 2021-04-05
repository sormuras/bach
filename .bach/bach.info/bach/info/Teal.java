package bach.info;

import com.github.sormuras.bach.Bach;
import java.awt.Color;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.image.BufferedImage;

public class Teal implements Bach.OnTestsSuccessful {
  @Override
  public void onTestsSuccessful(Event event) {
    event.bach().say("Teal");
    if (!SystemTray.isSupported()) return;
    var image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    image.getGraphics().setColor(new Color(0x008080));
    image.getGraphics().fillRect(0, 0, 64, 64);
    var icon = new TrayIcon(image);
    icon.setImageAutoSize(true);
    try {
      SystemTray.getSystemTray().add(icon);
      icon.displayMessage("caption", "text", MessageType.INFO);
    } catch (Exception exception) {
      event.bach().log("Display message via tray failed: " + exception);
    }
  }
}
