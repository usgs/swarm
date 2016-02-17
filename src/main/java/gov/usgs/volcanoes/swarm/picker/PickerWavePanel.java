package gov.usgs.volcanoes.swarm.picker;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.time.UiTime;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

public class PickerWavePanel extends WaveViewPanel {

  private PhasePopup phasePopup;
  
  public PickerWavePanel(WaveViewPanel insetWavePanel) {
    super(insetWavePanel);
    
    phasePopup = new PhasePopup();
    
    // todo: fix this. 
    for (MouseListener m : this.getMouseListeners()) {
      this.removeMouseListener(m);
    }
    setupMouseHandler();
  }


  private void setupMouseHandler() {
    Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    this.setCursor(crosshair);
    this.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        UiTime.touchTime();

        double[] t = getTranslation();
        if (t != null) {
          int x = e.getX();
          double j2k = x * t[0] + t[1];
          if (timeSeries)
            System.out.printf("%s UTC: %s j2k: %.3f ew: %d\n", channel, J2kSec.toDateString(j2k),
                j2k, J2kSec.asEpoch(j2k));

          if (SwingUtilities.isRightMouseButton(e)) {
            phasePopup.show(e.getComponent(), e.getX(), e.getY());
          }

          if (timeSeries && j2k >= startTime && j2k <= endTime)
            fireTimePressed(e, j2k);

          if (timeSeries && allowDragging && SwingUtilities.isLeftMouseButton(e)) {
            Dimension size = getSize();
            int y = e.getY();
            if (t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset
                && x < size.width - rightWidth) {
              j2k1 = j2k2 = j2k;
              if (e.isControlDown()) {
                System.out.println(channel + ": " + J2kSec.toDateString(j2k1));
              } else if (!e.isShiftDown()) {
                highlightX1 = highlightX2 = x;
                dragging = true;
              }
            }
          }
        }

        fireMousePressed(e);
      }

      public void mouseReleased(MouseEvent e) {
        UiTime.touchTime();
        if (SwingUtilities.isLeftMouseButton(e) && dragging) {
          dragging = false;
          if (j2k1 != j2k2 && source != null) {
            double st = Math.min(j2k1, j2k2);
            double et = Math.max(j2k1, j2k2);
            zoom(st, et);
            fireZoomed(e, getStartTime(), getEndTime(), st, et);
          }
          repaint();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
          phasePopup.setVisible(false);
        }

        
        int mx = e.getX();
        int my = e.getY();
        if (allowClose && SwingUtilities.isLeftMouseButton(e)
            && mx > getWidth() - 17 && mx < getWidth() - 3
            && my > 2 && my < 17) {
          fireClose();
        }
      }

      public void mouseExited(MouseEvent e) {
        WaveViewTime.fireTimeChanged(Double.NaN);
        dragging = false;
        repaint();
      }
    });

    this.addMouseMotionListener(new MouseMotionListener() {
      public void mouseMoved(MouseEvent e) {
        UiTime.touchTime();
        processMousePosition(e.getX(), e.getY());
      }

      public void mouseDragged(MouseEvent e) {
        UiTime.touchTime();
        /*
         * // This used to be the launcher for the microview. // It was
         * removed because it wasn't very useful, but this // stub is
         * left here in case something like it ever gets // put in if
         * (SwingUtilities.isLeftMouseButton(e) && e.isControlDown() &&
         * settings.type != WaveViewSettings.SPECTRA) { Dimension size =
         * getSize(); double[] t = getTranslation(); int x = e.getX();
         * int y = e.getY(); if (t != null && y > Y_OFFSET && y <
         * (size.height - BOTTOM_HEIGHT) && x > X_OFFSET && x <
         * size.width - RIGHT_WIDTH) { double j2k = x * t[0] + t[1];
         * createMicroView(j2k); } }
         */

        processMousePosition(e.getX(), e.getY());
        if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown() && dragging) {
          double[] t = getTranslation();
          int x = e.getX();
          int y = e.getY();
          Dimension size = getSize();
          if (t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset
              && x < size.width - rightWidth) {
            j2k2 = x * t[0] + t[1];
            highlightX2 = x;
            repaint();
          }
        }
      }
    });
  }

}
