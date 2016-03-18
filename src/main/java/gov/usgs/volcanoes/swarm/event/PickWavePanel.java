package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.picker.Phase;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;

public class PickWavePanel extends AbstractWavePanel implements EventObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(PickWavePanel.class);

  private static final Font ANNOTATION_FONT = new Font("Monospaced", Font.BOLD, 12);
  private static final Color P_BACKGROUND = new Color(128, 255, 128, 192);
  private static final Color S_BACKGROUND = new Color(128, 128, 255, 192);
  private static final Color CODA_BACKGROUND = new Color(128, 128, 128, 192);
  private final List<Arrival> arrivals;

  public PickWavePanel() {
    super();
    arrivals = new ArrayList<Arrival>();
  }

  public void addArrival(Arrival arrival) {
    arrivals.add(arrival);
  }

  @Override
  protected void annotateImage(Graphics2D g2) {
    for (Arrival arrival : arrivals) {
      Pick pick = arrival.getPick();

      String tag = arrival.getPhase();
      double j2k = J2kSec.fromEpoch(pick.getTime());
      double[] t = getTranslation();
      if (t == null)
        continue;
      
      double x = 2 + (j2k - t[1]) / t[0];
      g2.setColor(DARK_GREEN);
      g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));
      g2.setFont(ANNOTATION_FONT);

      FontMetrics fm = g2.getFontMetrics();
      int width = fm.stringWidth(tag);
      int height = fm.getAscent();

      int offset = 2;
      int lw = width + 2 * offset;

      g2.setColor(backgroundColor);

      g2.fillRect((int) x, 3, lw, height + 2 * offset);
      g2.setColor(Color.black);
      g2.drawRect((int) x, 3, lw, height + 2 * offset);

      g2.drawString(tag, (int) x + offset, 3 + (fm.getAscent() + offset));
    }
  }

  private void markPhase(Graphics2D g2, Phase phase) {
    double j2k = J2kSec.fromEpoch(phase.time);
    double[] t = getTranslation();
    if (t == null)
      return;

    double x = 2 + (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

    String tag = phase.tag();
    Font oldFont = g2.getFont();
    g2.setFont(ANNOTATION_FONT);

    FontMetrics fm = g2.getFontMetrics();
    int width = fm.stringWidth(tag);
    int height = fm.getAscent();

    int offset = 2;
    int lw = width + 2 * offset;

    Color background = null;
    if (phase.phaseType == Phase.PhaseType.P) {
      background = P_BACKGROUND;
    } else if (phase.phaseType == Phase.PhaseType.S) {
      background = S_BACKGROUND;
    }
    g2.setColor(background);

    g2.fillRect((int) x, 3, lw, height + 2 * offset);
    g2.setColor(Color.black);
    g2.drawRect((int) x, 3, lw, height + 2 * offset);

    g2.drawString(tag, (int) x + offset, 3 + (fm.getAscent() + offset));
    g2.setFont(oldFont);
  }

  @Override
  protected void processRightMouseRelease(MouseEvent e) {
    pauseCursorMark = false;
  }

  public void eventUpdated() {
    repaint();
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    // TODO Auto-generated method stub
    
  }
}
