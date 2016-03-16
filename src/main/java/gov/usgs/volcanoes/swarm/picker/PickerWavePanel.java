package gov.usgs.volcanoes.swarm.picker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.event.EventObserver;
import gov.usgs.volcanoes.swarm.picker.Phase.PhaseType;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;

public class PickerWavePanel extends AbstractWavePanel implements EventObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(PickerWavePanel.class);

  private static final Font ANNOTATION_FONT = new Font("Monospaced", Font.BOLD, 12);
  private static final Color P_BACKGROUND = new Color(128, 255, 128, 192);
  private static final Color S_BACKGROUND = new Color(128, 128, 255, 192);
  private static final Color CODA_BACKGROUND = new Color(128, 128, 128, 192);
  private EventOld event;
  private Component parent;

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
    phasePopup.setParent(parent);
    phasePopup.show(e.getComponent(), e.getX(), e.getY());
    pauseCursorMark = true;
    WaveViewTime.fireTimeChanged(cursorTime);
  }

  public void instantPick(PhaseType p) {
    Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.P)
        .firstMotion(Phase.FirstMotion.UP).time(J2kSec.asEpoch(time)).weight(1).build();

    event.setPhase(channel, phase);
    WaveViewTime.fireTimeChanged(time);
  }
  public void setEvent(EventOld event) {
    this.event = event;
  }

  @Override
  protected void annotateImage(Graphics2D g2) {
    for (Phase.PhaseType type : Phase.PhaseType.values()) {
      Phase phase = event.getPhase(channel, type);
      if (phase != null) {
        Color background;
        if (phase.phaseType == Phase.PhaseType.P) {
          markPhase(g2, P_BACKGROUND, phase.time, phase.tag());
          long time = event.coda(channel);
          if (time > 0) {
            markPhase(g2, CODA_BACKGROUND, event.coda(channel), "C");
          }
        } else {
          markPhase(g2, S_BACKGROUND, phase.time, phase.tag());
        }
      }
    }
    // repaint();
  }

  private void markPhase(Graphics2D g2, Color backgroundColor, long time, String tag) {
    double j2k = J2kSec.fromEpoch(time);
    double[] t = getTranslation();
    if (t == null)
      return;

    double x = 2 + (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

    Font oldFont = g2.getFont();
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
    g2.setFont(oldFont);
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

  public void setParent(Component parent) {
    this.parent = parent;
  }


}
