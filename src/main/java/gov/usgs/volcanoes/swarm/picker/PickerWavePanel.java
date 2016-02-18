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

public class PickerWavePanel extends AbstractWavePanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(PickerWavePanel.class);

  private Event event;

  public PickerWavePanel(WaveViewPanel insetWavePanel) {
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
    ListIterator<Phase> it = event.phasesIt();
    while (it.hasNext()) {
      markPhase(g2, it.next());
    }
//    repaint();
  }

  private void markPhase(Graphics2D g2, Phase phase) {
    LOGGER.debug("Marking phase: {}", phase);

    double j2k = J2kSec.fromEpoch(phase.time);
    double[] t = getTranslation();
    if (t == null)
      return;

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

    // g2.drawString("iP0", (int)x, yOffset);

    String tag = "iP0";
    Font oldFont = g2.getFont();
    // g.setFont(LARGE_FONT);
    // String c = channel.replace('_', ' ');
    //

    // Font f = g2.getFont();
    // float s = f.getSize();
    FontMetrics fm = g2.getFontMetrics();
    int width = fm.stringWidth(tag);
    // while ((width / (double)graphWidth > .5) && (--s > 1)) {
    // g2.setFont(f.deriveFont(s));
    // fm = g2.getFontMetrics();
    // width = fm.stringWidth(tag);
    // }
    
    // int height = fm.getAscent() + fm.getDescent();
    int height = fm.getAscent();
    
    int offset = 2;
    int lw = width + 2 * offset;
    g2.setColor(new Color(255, 255, 255, 192));

    g2.fillRect((int) x, 3, lw, height + 2 * offset);
    g2.setColor(Color.black);
    g2.drawRect((int) x, 3, lw, height + 2 * offset);

    g2.drawString(tag, (int) x + offset, (fm.getAscent() + offset));
    g2.setFont(oldFont);



    //
    // Graphics2D g2d = (Graphics2D)g;
    // g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
    // RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    // }
    // g2.drawString("This is gona be awesome", 200, 200);

  }

  @Override
  protected void processRightMouseRelease(MouseEvent e) {
    pauseCursorMark = false;
  }
}
