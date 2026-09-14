package com.evi.live;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/** Exercises sidebar controls without RuneLite login or real key files. */
public final class EviLivePanelTest {
  private static void visit(Container parent,List<Component> result) {
    for(Component child:parent.getComponents()) {
      result.add(child);
      if(child instanceof Container)visit((Container)child,result);
    }
  }
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(()->{
      AtomicReference<String> saved=new AtomicReference<>();
      EviLivePanel panel=new EviLivePanel(saved::set);
      List<Component> components=new ArrayList<>();visit(panel,components);
      JPasswordField field=(JPasswordField)components.stream().filter(x->x instanceof JPasswordField).findFirst().orElseThrow();
      JButton button=(JButton)components.stream().filter(x->x instanceof JButton).findFirst().orElseThrow();
      String synthetic="abcdef0123456789".repeat(4);
      field.setText(synthetic);button.doClick();
      if(!synthetic.equals(saved.get()))throw new AssertionError("Pairing callback did not receive key");
      if(field.getPassword().length!=0)throw new AssertionError("Pairing input must be cleared after save");
      if(field.getEchoChar()==0)throw new AssertionError("Key input must be masked");
      if(EviLivePanel.icon().getWidth()!=24)throw new AssertionError("Sidebar icon dimensions");
    });
    System.out.println("PASS: sidebar pairing callback, masked key input and clearing after save");
  }
}
