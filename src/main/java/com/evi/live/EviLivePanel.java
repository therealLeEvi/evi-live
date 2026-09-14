package com.evi.live;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

final class EviLivePanel extends PluginPanel {
  private final JTextArea status = text("Waiting for setup.");

  EviLivePanel(Consumer<String> pair) {
    setLayout(new BorderLayout());
    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
    content.add(new JLabel("EVI Live · Local"));
    content.add(text("Observes Grand Exchange offers only. You place and manage every offer yourself."));
    content.add(status);
    content.add(text("Start the EVI bridge, then paste its RuneLite plugin key below. This is not your Scanner key or Jagex login."));
    JPasswordField key = new JPasswordField(20);
    key.getAccessibleContext().setAccessibleName("RuneLite plugin key");
    content.add(key);
    JButton save = new JButton("Save pairing key");
    save.addActionListener(e -> {
      char[] entered = key.getPassword();
      try { pair.accept(new String(entered)); }
      finally { Arrays.fill(entered, '\0'); key.setText(""); }
    });
    content.add(save);
    content.add(text("Saved only on this PC. Destination: 127.0.0.1:51743. No account password, chat, or inventory is collected."));
    add(content, BorderLayout.NORTH);
  }

  void status(String message) {
    SwingUtilities.invokeLater(() -> status.setText(message));
  }

  private static JTextArea text(String value) {
    JTextArea field = new JTextArea(value);
    field.setEditable(false);
    field.setLineWrap(true);
    field.setWrapStyleWord(true);
    field.setOpaque(false);
    field.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
    return field;
  }

  static BufferedImage icon() {
    BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setColor(new Color(83, 205, 180));
      g.fillRoundRect(2, 2, 20, 20, 6, 6);
      g.setColor(new Color(15, 30, 40));
      g.fillRect(7, 6, 3, 12);
      g.fillRect(10, 6, 7, 3);
      g.fillRect(10, 11, 5, 2);
      g.fillRect(10, 15, 7, 3);
    } finally { g.dispose(); }
    return image;
  }
}
