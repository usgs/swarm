package gov.usgs.volcanoes.swarm.picker;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

public class PickerWavePanel extends AbstractWavePanel implements EventObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(PickerWavePanel.class);

  private static final Font ANNOTATION_FONT = new Font("Monospaced", Font.BOLD, 12);
  private static final Color P_BACKGROUND = new Color(0, 255, 0, 32);
  private static final Color S_BACKGROUND = new Color(0, 0, 255, 32);

  private Event event;

  public PickerWavePanel(AbstractWavePanel insetWavePanel) {
    super(insetWavePanel);
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    double[] t = getTranslation();
    int x = e.getX();
    double cursorTime = x * t[0] + t[1];
    LOGGER.debug("New phase: {} @ {}", channel, J2kSec.toDateString(cursorTime));

    PhasePopup phasePopup = new PhasePopup(event, channel, J2kSec.asEpoch(cursorTime));
    phasePopup.show(e.getComponent(), e.getX(), e.getY());
    pauseCursorMark = true;
    WaveViewTime.fireTimeChanged(cursorTime);

  }

  public void setEvent(Event event) {
    this.event = event;
  }

  @Override
  protected void annotateImage(Graphics2D g2) {
    for (Phase.PhaseType type : Phase.PhaseType.values()) {
      Phase phase = event.getPhase(channel, type);
      if (phase != null) {
        markPhase(g2, phase);
      }
    }
    // repaint();
  }

  private void markPhase(Graphics2D g2, Phase phase) {
    LOGGER.debug("Marking phase: {}", phase);

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
    
    Color background;
    if (phase.phaseType == Phase.PhaseType.P) {
      background = P_BACKGROUND;
    } else {
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

  public void updateEvent() {
    repaint();
  }
}
