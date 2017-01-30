package gov.usgs.volcanoes.swarm;

import org.pegdown.PegDownProcessor;

import java.awt.Component;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public final class HelpDialog {
  
  private static final PegDownProcessor pegdownProcessor = new PegDownProcessor();
  
  public static void displayHelp(Component parent, String helpFile) {
    final JPanel helpPanel = new JPanel();
    helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.PAGE_AXIS));
    JTextPane htmlPane = new JTextPane();

    Scanner scanner = new Scanner(HelpDialog.class.getResourceAsStream("/help/" + helpFile), "UTF-8");
    String text = scanner.useDelimiter("\\A").next();
    scanner.close();
    
    htmlPane.setContentType("text/html");
    htmlPane.setText(pegdownProcessor.markdownToHtml(text));
    JScrollPane paneScrollPane = new JScrollPane(htmlPane);

    helpPanel.add(paneScrollPane);
  

    JOptionPane.showMessageDialog(parent,
        helpPanel,
        "A plain message",
        JOptionPane.PLAIN_MESSAGE);

  }
}
