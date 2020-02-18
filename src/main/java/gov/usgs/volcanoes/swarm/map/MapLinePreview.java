package gov.usgs.volcanoes.swarm.map;

import gov.usgs.volcanoes.swarm.SwarmConfig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@SuppressWarnings("serial")
public class MapLinePreview extends JPanel {

  SwarmConfig config = SwarmConfig.getInstance();
  LinePreviewPane linePreviewPane = new LinePreviewPane();
  LineWidthPane lineWidthPane = new LineWidthPane();
  int lineWidth;

  /**
   * Constructor.
   */
  public MapLinePreview() {
    add(linePreviewPane);
    add(lineWidthPane);
    lineWidth = config.mapLineWidth;
  }

  /**
   * @see javax.swing.JComponent#setForeground(java.awt.Color)
   */
  public void setForeground(Color c) {
    if (linePreviewPane != null) {
      linePreviewPane.setColor(c);
    }
  }

  // @SuppressWarnings("serial")
  public class LinePreviewPane extends JPanel {
    Color lineColor;

    public LinePreviewPane() {
      setPreferredSize(new Dimension(200, 50));
      lineColor = new Color(config.mapLineColor);
    }

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      // System.out.println(lineColor + " : " + lineWidth);
      g2.setPaintMode();
      g2.setStroke(new BasicStroke(config.mapLineWidth));
      g2.setStroke(new BasicStroke(lineWidth));
      g2.setColor(lineColor);
      Line2D.Double line = new Line2D.Double(0, getHeight() / 2, getWidth(), getHeight() / 2);
      g2.draw(line);
    }

    public void setColor(Color c) {
      lineColor = c;
    }
  }

  public class LineWidthPane extends JPanel implements DocumentListener {
    JTextField widthBox;
    JLabel widthBoxLabel;


    /**
     * LineWidthPane constructor.
     */
    public LineWidthPane() {
      widthBoxLabel = new JLabel("Width");
      add(widthBoxLabel);

      widthBox = new JTextField(4);
      widthBox.getDocument().addDocumentListener(this);
      widthBox.setText("" + lineWidth);
      setPreferredSize(new Dimension(100, 50));
      add(widthBox);
    }

    public void insertUpdate(DocumentEvent ignoredEvent) {
      updateLineWidth(widthBox.getText());
    }

    public void removeUpdate(DocumentEvent ignoredEvent) {
      updateLineWidth(widthBox.getText());
    }

    public void changedUpdate(DocumentEvent e) {
      updateLineWidth(widthBox.getText());
    }

    private void updateLineWidth(String w) {
      try {
        lineWidth = Integer.parseInt(w);
        widthBox.setBackground(Color.white);
        linePreviewPane.repaint();
      } catch (NumberFormatException e) {
        widthBox.setBackground(new Color(253, 130, 130));
      }
    }
  }
}
