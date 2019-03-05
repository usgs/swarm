package gov.usgs.volcanoes.swarm;

import gov.usgs.volcanoes.core.configfile.ConfigFile;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * Swarm Frame.
 * 
 * @author Dan Cervelli
 */
public class SwarmFrame extends JInternalFrame {
  private static final long serialVersionUID = 1L;

  protected JComponent northPane;
  protected Dimension oldNorthPaneSize;
  protected boolean fullScreen = false;
  protected static SwarmConfig swarmConfig;
  protected static final JFrame applicationFrame = Swarm.getApplicationFrame();


  /**
   * Swarm Frame.
   * @param title frame title
   * @param resizable whether resizable
   * @param closable whether closable
   * @param maximizable whether maximizable
   * @param iconifiable whether iconifiable
   */
  public SwarmFrame(String title, boolean resizable, boolean closable, boolean maximizable,
      boolean iconifiable) {
    super(title, resizable, closable, maximizable, iconifiable);
    setOpaque(false);
    setBackground(new Color(255, 255, 255, 0));
    swarmConfig = SwarmConfig.getInstance();
  }

  protected void processStandardLayout(ConfigFile cf) {
    int x = Integer.parseInt(cf.getString("x"));
    int y = Integer.parseInt(cf.getString("y"));
    setLocation(x, y);
    int w = Integer.parseInt(cf.getString("w"));
    int h = Integer.parseInt(cf.getString("h"));
    setSize(w, h);

    boolean m = Boolean.parseBoolean(cf.getString("maximized"));
    try {
      setMaximum(m);
    } catch (Exception e) {
      //
    }
  }

  /**
   * Save layout.
   * @param cf config file
   * @param prefix config file prefix
   */
  public void saveLayout(ConfigFile cf, String prefix) {
    Point pt = getLocation();
    cf.put(prefix + ".x", Integer.toString(pt.x));
    cf.put(prefix + ".y", Integer.toString(pt.y));

    Dimension d = getSize();
    cf.put(prefix + ".w", Integer.toString(d.width));
    cf.put(prefix + ".h", Integer.toString(d.height));

    cf.put(prefix + ".maximized", Boolean.toString(isMaximum()));
  }

  protected void setDefaultKioskMode(boolean b) {
    fullScreen = b;
    this.setResizable(!fullScreen);
    this.setIconifiable(!fullScreen);
    this.setMaximizable(!fullScreen);
    this.setClosable(!fullScreen);
    this.putClientProperty("JInternalFrame.isPalette", new Boolean(fullScreen));
    BasicInternalFrameUI ui = (BasicInternalFrameUI) this.getUI();
    if (fullScreen) {
      northPane = ui.getNorthPane();
      oldNorthPaneSize = northPane.getSize();
      northPane.setVisible(false);
      northPane.setPreferredSize(new Dimension(0, 0));
    } else {
      if (northPane != null) {
        northPane.setVisible(true);
        northPane.setPreferredSize(oldNorthPaneSize);
      }
    }
  }
}
