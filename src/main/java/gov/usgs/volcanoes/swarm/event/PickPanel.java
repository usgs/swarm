package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Scrollable;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesClient;

public class PickPanel extends JPanel implements Scrollable {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PickPanel.class);

  private BufferedImage image;
  private final Map<String, PickWavePanel> panels;
  private double startJ2k;
  private double endJ2k;

  public PickPanel() {
    panels = new HashMap<String, PickWavePanel>();
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentMoved(final ComponentEvent e) {
        resizeWaves();
      }

      @Override
      public void componentResized(final ComponentEvent e) {
        createImage();
        resizeWaves();
      }
    });
  }
  
  public void setStart(Double startJ2k) {
    this.startJ2k = startJ2k;
  }

  public void setEnd(Double startJ2k) {
    this.endJ2k = startJ2k;
  }

  private synchronized void createImage() {
    if (getWidth() > 0 && getHeight() > 0)
      image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
  }

  @Override
  public void paint(final Graphics g) {
    if (getWidth() <= 0 || getHeight() <= 0)
      return;

    if (image == null || panels.size() == 0) {
      super.paint(g);
      final Dimension dim = this.getSize();
      g.setColor(Color.black);
      g.drawString("Monitor empty.", dim.width / 2 - 40, dim.height / 2);
    } else {
      super.paint(image.getGraphics());
      g.drawImage(image, 0, 0, null);
      g.setColor(Color.WHITE);
    }
  }

  public void addPick(Arrival arrival) {
    Pick pick = arrival.getPick();
    PickWavePanel wavePanel = panels.get(pick.getChannel());
    if (wavePanel == null) {
      wavePanel = new PickWavePanel();
      wavePanel.setChannel(pick.getChannel());
      Wave wave = WebServicesClient.getWave(pick.getChannel(), startJ2k, endJ2k);
      panels.put(pick.getChannel(), wavePanel);      
      wavePanel.setWave(wave, startJ2k, endJ2k);
      add(wavePanel);
      wavePanel.setSize(getWidth(), 200);
      wavePanel.setDisplayTitle(true);
      wavePanel.setOffsets(60, 0, 0, 0);
      wavePanel.createImage();
    }
    
    wavePanel.addArrival(arrival);
//    
//    if (wave == null) {
//      return;
//    }

//    LOGGER.debug("Got {} samples", wave.numSamples());
  }
  
  private synchronized void resizeWaves() {
    for (int idx = 0; idx < panels.size(); idx++) {
      PickWavePanel panel = panels.get(idx);
      Dimension d = panel.getSize();
      
      panel.setSize(getWidth(), d.height);
      panel.createImage();
      panel.repaint();
    }
  }

  public Dimension getPreferredScrollableViewportSize() {
    return null;
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 0;
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 0;
  }

  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  public boolean getScrollableTracksViewportHeight() {
    // TODO Auto-generated method stub
    return false;
  }
}