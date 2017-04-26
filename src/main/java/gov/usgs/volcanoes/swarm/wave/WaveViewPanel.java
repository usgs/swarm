package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.event.PickMenu;
import gov.usgs.volcanoes.swarm.event.PickWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

/**
 * A component that renders a wave in either a standard wave view, a frequency spectra, or
 * spectrogram. Relies heavily on the Valve plotting package.
 * 
 * <p>TODO: move filter method
 * 
 * 
 * @author Dan Cervelli
 */
public class WaveViewPanel extends AbstractWavePanel {

  private static final long serialVersionUID = 1L;
  private PickMenu pickMenu;

  /**
   * Constructs a WaveViewPanel with default settings.
   */
  public WaveViewPanel() {
    this(new WaveViewSettings());
  }

  /**
   * Constructs a WaveViewPanel with specified settings.
   * 
   * @param s the settings
   */
  public WaveViewPanel(WaveViewSettings s) {
    super(s);
  }

  /**
   * Constructs a WaveViewPanel set up the same as a source WaveViewPanel. Used when copying a
   * waveform to the clipboard.
   * 
   * @param p the source WaveViewPanel
   */
  public WaveViewPanel(WaveViewPanel p) {
    super(p);
    pickMenu = p.pickMenu;
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    if (settings.pickEnabled && settings.viewType.equals(ViewType.WAVE)) {
      double[] t = getTranslation();
      if (t != null) {
        double j2k = e.getX() * t[0] + t[1];
        if (j2k >= startTime && j2k <= endTime) {
          if (pickMenu == null) {
            pickMenu = new PickMenu(this);
          }
          pickMenu.setJ2k(j2k);
          pickMenu.show(this, e.getX(), e.getY());
        }
      }
    } else {
      settings.cycleType();
    }
  }

  private void paintMark(Graphics2D g2, double j2k) {
    if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime) {
      return;
    }

    double[] t = getTranslation();
    if (t == null) {
      return;
    }

    double x = (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

    GeneralPath gp = new GeneralPath();
    gp.moveTo((float) x, yOffset);
    gp.lineTo((float) x - 5, yOffset - 7);
    gp.lineTo((float) x + 5, yOffset - 7);
    gp.closePath();
    g2.setPaint(Color.GREEN);
    g2.fill(gp);
    g2.setColor(DARK_GREEN);
    g2.draw(gp);
  }

  @Override
  protected void annotateImage(Graphics2D g2) {
    if (!Double.isNaN(mark1)) {
      paintMark(g2, mark1);
    }

    if (!Double.isNaN(mark2)) {
      paintMark(g2, mark2);
    }

  }

  @Override
  protected void processRightMouseRelease(MouseEvent e) {
    // do nothing
  }

  /**
   * @see gov.usgs.volcanoes.swarm.wave.AbstractWavePanel#paint(java.awt.Graphics)
   */
  public void paint(Graphics g) {
    super.paint(g);
    Graphics2D g2 = (Graphics2D) g;
    if (settings.pickEnabled && pickMenu != null) {

      double[] t = getTranslation();
      if (t == null) {
        return;
      }

      if (!pickMenu.isHidePhases()) {
        // Draw P marker
        Pick p = pickMenu.getP();
        if (p != null) {
          drawPick(p.getTag(), g2, p.getTime());
        }
        // Draw S marker
        Pick s = pickMenu.getS();
        if (s != null) {
          drawPick(s.getTag(), g2, s.getTime());
        }
      }
    }
  }
  
  private void drawPick(String label, Graphics2D g2, long time) {
    double[] t = getTranslation();
    double j2k = J2kSec.fromEpoch(time);
    double x = 2 + (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));
    FontMetrics fm = g2.getFontMetrics();
    int width = fm.stringWidth(label);
    int height = fm.getAscent();

    int offset = 2;
    int lw = width + 2 * offset;

    if (label.indexOf('P') != -1) {
      g2.setColor(PickWavePanel.P_BACKGROUND);
    } else if (label.indexOf('S') != -1) {
      g2.setColor(PickWavePanel.S_BACKGROUND);
    } 

    g2.fillRect((int) x, 3, lw, height + 2 * offset);
    g2.setColor(Color.black);
    g2.drawRect((int) x, 3, lw, height + 2 * offset);

    g2.drawString(label, (int) x + offset, 3 + (fm.getAscent() + offset));
  }
}
