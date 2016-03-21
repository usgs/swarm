package gov.usgs.volcanoes.swarm.event;

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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesSource;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelListener;

public class PickPanel extends JPanel implements Scrollable {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PickPanel.class);

  private static final String IRIS_DATASELECT_URL = "http://service.iris.edu/fdsnws/dataselect/1/query";
  private static final String IRIS_STATION_URL = "http://service.iris.edu/fdsnws/station/1/query";
    
  private BufferedImage image;
  private final Map<String, PickWavePanel> panels;
  private double startJ2k;
  private double endJ2k;
  private final JLabel statusLabel;
  private WaveViewPanelListener selectListener;
  private final Map<String, SeismicDataSource> seismicSources;

  public PickPanel(JLabel statusLabel) {
    this.statusLabel = statusLabel;
    panels = new HashMap<String, PickWavePanel>();
    seismicSources = new HashMap<String, SeismicDataSource>();
    
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
    String channel = pick.getChannel();
    PickWavePanel wavePanel = panels.get(channel);
    if (wavePanel == null) {
      wavePanel = new PickWavePanel();
      wavePanel.setStatusLabel(statusLabel);
      wavePanel.setChannel(channel);
      wavePanel.setViewport(((JViewport)getParent()));
      SeismicDataSource source = seismicSources.get(channel);
      if (source == null) {
        source = new WebServicesSource();
        source.parse(buildParams(channel));
      }
      wavePanel.setDataSource(source);
      Wave wave = source.getWave(channel, startJ2k, endJ2k);
      if (wave != null) {
        panels.put(pick.getChannel(), wavePanel);      
        wavePanel.setWave(wave, startJ2k, endJ2k);
        add(wavePanel);
//        add(Box.createRigidArea(new Dimension(0, 10)));
//        p.setSize(w, calculateWaveHeight());
        wavePanel.setBottomBorderColor(Color.GRAY);
        wavePanel.setSize(getWidth(), 100);
        wavePanel.setDisplayTitle(true);
        wavePanel.setOffsets(54, 8, 21, 19);
        wavePanel.createImage();
        wavePanel.addListener(selectListener);
      }
    }
    
    wavePanel.addArrival(arrival);
  }
  
  private String buildParams(String channel) {
    String[] comps = channel.split("\\$");
    LOGGER.debug("SPLIT {}", channel);
    StringBuilder sb = new StringBuilder();
    sb.append(comps[2]).append("|");
    sb.append(comps[0]).append("|");
    
    if (comps.length > 3) {
      sb.append(comps[3]).append("|");      
    } else {
      sb.append("--|");
    }
    sb.append(comps[1]).append("|");
    sb.append(3600).append("|");
    sb.append(1000).append("|");
    sb.append(IRIS_DATASELECT_URL).append("|");
    sb.append(IRIS_STATION_URL);

    return sb.toString();
  }
  private synchronized void resizeWaves() {
    for (PickWavePanel panel : panels.values().toArray(new PickWavePanel[0])) {
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