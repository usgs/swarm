package gov.usgs.volcanoes.swarm;

import gov.usgs.volcanoes.swarm.data.CachedDataSource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * A class that shows an about dialog with some extra controls.
 * 
 * @author Dan Cervelli
 */
public class AboutDialog extends JDialog implements Runnable {
  private static final long serialVersionUID = -1;
  private static final JFrame applicationFrame = Swarm.getApplicationFrame();

  private static final int PANE_WIDTH = 344;
  private static final int PANEL_PADDING = 9;

  private static final int TITLE_LOC_Y = 5;
  private static final int TITLE_HEIGHT = 80;

  private static final int ATTRIBUTION_LOC_Y = TITLE_LOC_Y + TITLE_HEIGHT + PANEL_PADDING;
  private static final int ATTRIBUTION_HEIGHT = 140;

  private static final int MEMORY_LOC_Y = ATTRIBUTION_LOC_Y + ATTRIBUTION_HEIGHT + PANEL_PADDING;
  private static final int MEMORY_HEIGHT = 100;

  private static final int BUTTON_PAD_HEIGHT = 11;
  private static final int BUTTON_LOC_Y = MEMORY_LOC_Y + MEMORY_HEIGHT + BUTTON_PAD_HEIGHT;
  private static final int BUTTON_HEIGHT = 30;
  private static final int BG_WIDTH = 357;
  private static final int BG_HEIGHT = BUTTON_LOC_Y + BUTTON_HEIGHT + 10;
  private static final int WIDTH = BG_WIDTH;
  private static final int HEIGHT = BG_HEIGHT + 20;

  private final MemoryPanel memoryPanel;

  private Thread updateThread;

  private boolean kill = false;

  /**
   * Construct an about dialog.
   */
  public AboutDialog() {
    super(applicationFrame, "About", true);
    this.setSize(WIDTH, HEIGHT);
    this.setLocation();
    this.setResizable(false);

    ImageIcon background = Icons.honeycomb;
    if (background.getIconWidth() != BG_WIDTH || background.getIconHeight() != BG_HEIGHT) {
      // resize the background image
      Image image = background.getImage();
      image = image.getScaledInstance(BG_WIDTH, BG_HEIGHT, Image.SCALE_FAST);
      background.setImage(image);
    }
    JPanel bp = new JPanel(new BorderLayout());
    bp.add(new JLabel(background), BorderLayout.CENTER);
    bp.setSize(BG_WIDTH, BG_HEIGHT);
    bp.setLocation(0, 0);

    this.getLayeredPane().add(bp);
    this.getLayeredPane().setLayer(bp, JLayeredPane.DEFAULT_LAYER.intValue());

    JTextPane title = getTitlePane();
    this.getLayeredPane().add(title);
    this.getLayeredPane().setLayer(title, JLayeredPane.PALETTE_LAYER.intValue());

    JTextPane attribution = getAttributionPane();
    this.getLayeredPane().add(attribution);
    this.getLayeredPane().setLayer(attribution, JLayeredPane.PALETTE_LAYER.intValue());


    memoryPanel = new MemoryPanel(new GridLayout(5, 2));

    JPanel memoryContainer = new JPanel(new BorderLayout());
    memoryContainer.setBorder(LineBorder.createBlackLineBorder());
    memoryContainer.add(memoryPanel, BorderLayout.CENTER);
    memoryContainer.setSize(PANE_WIDTH, MEMORY_HEIGHT);
    memoryContainer.setLocation(5, MEMORY_LOC_Y);
    memoryContainer.setBackground(new Color(0, 0, 0, 0));

    this.getLayeredPane().add(memoryContainer);
    this.getLayeredPane().setLayer(memoryContainer, JLayeredPane.PALETTE_LAYER.intValue());

    JButton okButton = new JButton("OK");
    okButton.setSize(60, BUTTON_HEIGHT);
    okButton.setLocation(170, BUTTON_LOC_Y);
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
        kill = true;
      }
    });

    JButton gcButton = new JButton("Run GC");
    gcButton.setSize(90, BUTTON_HEIGHT);
    gcButton.setLocation(70, BUTTON_LOC_Y);
    gcButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.gc();
        // update();
        // no need to update since it will be done by thread
      }
    });

    this.getLayeredPane().add(okButton);
    this.getLayeredPane().add(gcButton);

    this.getLayeredPane().setLayer(okButton, JLayeredPane.PALETTE_LAYER.intValue());
    this.getLayeredPane().setLayer(gcButton, JLayeredPane.PALETTE_LAYER.intValue());
  }

  private JTextPane getTitlePane() {
    JTextPane pane = new JTextPane();
    pane.setBorder(new EmptyBorder(3, 6, 3, 6));

    pane.setEditable(false); // make title not modifiable
    pane.setBackground(new Color(255, 255, 0, 210));
    pane.setSize(PANE_WIDTH, TITLE_HEIGHT);
    pane.setLocation(5, TITLE_LOC_Y);
    pane.setBorder(LineBorder.createBlackLineBorder());
    pane.setContentType("text/html");
    pane.setText("<HTML><DIV style=\"text-align: center;\">"
        + "<SPAN style=\"color: red;\">SWARM:</SPAN><BR>"
        + "<SPAN style=\"color: blue;\">Seismic Wave Analysis / "
        + "Real-time Monitoring</SPAN><BR>https://volcanoes.usgs.gov/software/swarm/<br>"
        + "Version: " + Version.POM_VERSION + "<BR></DIV>" + "</HTML>");
    return pane;
  }

  private JTextPane getAttributionPane() {
    JTextPane pane = new JTextPane();
    pane.setBorder(new EmptyBorder(3, 6, 3, 6));
    pane.setEditable(false); // make title not modifiable
    pane.setBackground(new Color(255, 255, 0, 210));
    pane.setSize(PANE_WIDTH, ATTRIBUTION_HEIGHT);
    pane.setLocation(5, ATTRIBUTION_LOC_Y);
    pane.setBorder(LineBorder.createBlackLineBorder());
    pane.setContentType("text/html");
    pane.setText("<HTML>"
        + "<DIV style=\"text-align: center;\"><B>Developed by:</B><br>U.S. Geological Survey"
        + "<p><B>With contributions from:</B>"
        + "<BR>Instrumental Software Technologies, Inc. (ISTI)"
        + "<BR>The Incorporated Research Institutions for Seismology (IRIS)"
        + "</DIV></HTML>");
    return pane;
  }

  private void setLocation() {
    Dimension parentSize = applicationFrame.getSize();
    Point parentLoc = applicationFrame.getLocation();
    this.setLocation(parentLoc.x + (parentSize.width / 2 - WIDTH / 2),
        parentLoc.y + (parentSize.height / 2 - HEIGHT / 2));
  }

  /**
   * @see java.awt.Dialog#setVisible(boolean)
   */
  public void setVisible(boolean v) {
    setLocation();
    updateThread = new Thread(this, "About");
    updateThread.start();
    super.setVisible(v);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while (!kill) {
      try {
        update();
        Thread.sleep(500);
      } catch (Exception e) {
        //
      }
    }
    kill = false;
  }

  /**
   * The memory panel overrides the paint method to update the values.
   */
  private class MemoryPanel extends JPanel {
    /** serial version UID. */
    private static final long serialVersionUID = 1L;

    /** re-use the number format safely on the Event Dispatch Thread. */
    private final NumberFormat nf = new DecimalFormat("#.##");

    private final JLabel freeMemory;
    private final JLabel totalMemory;
    private final JLabel usedMemory;
    private final JLabel maxMemory;
    private final JLabel cacheMemory;

    public MemoryPanel(LayoutManager layout) {
      super(layout);
      setBorder(new EmptyBorder(3, 6, 3, 6));
      add(new JLabel("Free memory: "));
      freeMemory = new JLabel();
      add(freeMemory);
      add(new JLabel("Total memory: "));
      totalMemory = new JLabel();
      add(totalMemory);
      add(new JLabel("Used memory: "));
      usedMemory = new JLabel();
      add(usedMemory);
      add(new JLabel("Max memory: "));
      maxMemory = new JLabel();
      add(maxMemory);
      add(new JLabel("Cache size: "));
      cacheMemory = new JLabel();
      add(cacheMemory);
      setSize(PANE_WIDTH, MEMORY_HEIGHT);
      setLocation(5, MEMORY_LOC_Y);
      setBackground(new Color(255, 255, 0));
    }

    /**
     * Invoked by Swing to draw components. This method should not be called directly, use the
     * repaint method.
     * 
     * @param g the graphics.
     */
    public void paint(Graphics g) {
      update();
      super.paint(g);
    }

    /**
     * Formats a long as a number of bytes.
     * 
     * @param bytes the number of bytes
     * @return a formatted string
     */
    private String toByteString(long bytes) {
      return nf.format(bytes / 1000000.0) + " MB";
    }

    /**
     * Updates the memory information displayed in the dialog. This should only be called on the
     * Event Dispatch Thread.
     */
    private void update() {
      Runtime r = Runtime.getRuntime();
      freeMemory.setText(toByteString(r.freeMemory()));
      totalMemory.setText(toByteString(r.totalMemory()));
      usedMemory.setText(toByteString(r.totalMemory() - r.freeMemory()));
      maxMemory.setText(toByteString(r.maxMemory()));

      CachedDataSource cache = CachedDataSource.getInstance();
      cacheMemory.setText(toByteString(cache.getSize()));
    }
  }

  /**
   * Updates the memory information displayed in the dialog.
   */
  public void update() {
    // this will cause paint to be called on the Event Dispatch Thread
    memoryPanel.repaint();
  }
}
