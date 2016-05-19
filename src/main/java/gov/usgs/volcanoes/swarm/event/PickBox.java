/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Scrollable;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.contrib.PngEncoder;
import gov.usgs.volcanoes.core.contrib.PngEncoderB;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.FileChooser;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesSource;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelAdapter;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelListener;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsDialog;

/**
 * A panel to display picks for a single event.
 * 
 * @author Tom Parker
 *
 */
public class PickBox extends JPanel implements Scrollable, PickToolBarListener {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PickBox.class);
  private static final int DEFAULT_WAVE_PANEL_HEIGHT = 150;
  private static final Color SELECT_COLOR = new Color(200, 220, 241);
  private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);
  private static final String DATASELECT_URL = "http://service.iris.edu/fdsnws/dataselect/1/query";
  private static final String STATION_URL = "http://service.iris.edu/fdsnws/station/1/query";
  private static final JFrame APPLICATION_FRAME = Swarm.getApplicationFrame();
  private static final SwarmConfig SWARM_CONFIG = SwarmConfig.getInstance();

  private final Map<AbstractWavePanel, Stack<double[]>> histories;
  private final Set<PickWavePanel> selectedSet;
  private final List<PickWavePanel> panels;

  private BufferedImage image;
  private double startJ2k;
  private double endJ2k;
  private final JLabel statusLabel;
  private WaveViewPanelListener selectListener;
  private final Map<String, SeismicDataSource> seismicSources;
  private int wavePanelHeight;
  private int lastClickedIndex;
  private final Set<PickBoxListener> listeners;
  private JLabel emptyArrivalLabel;

  public PickBox(JLabel statusLabel) {
    this.statusLabel = statusLabel;
    panels = new CopyOnWriteArrayList<PickWavePanel>();

    seismicSources = new HashMap<String, SeismicDataSource>();
    histories = new HashMap<AbstractWavePanel, Stack<double[]>>();
    listeners = new HashSet<PickBoxListener>();
    selectedSet = new ConcurrentSkipListSet<PickWavePanel>();
    wavePanelHeight = DEFAULT_WAVE_PANEL_HEIGHT;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    emptyArrivalLabel = new JLabel("No arrivals available.");
    add(emptyArrivalLabel);
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

    selectListener = new WaveViewPanelAdapter() {
      public void mousePressed(final AbstractWavePanel src, final MouseEvent e,
          final boolean dragging) {
        PickWavePanel panel = (PickWavePanel) src;
        requestFocusInWindow();
        final int thisIndex = getWaveIndex(src);

        if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
          deselectAll();
          select(panel);
          lastClickedIndex = findPanelIndex(panel);
        } else if (e.isControlDown()) {
          if (selectedSet.contains(src))
            deselect(src);
          else
            select(panel);
        } else if (e.isShiftDown()) {
          if (lastClickedIndex == -1) {
            select(panel);
          } else {
            deselectAll();
            final int min = Math.min(lastClickedIndex, thisIndex);
            final int max = Math.max(lastClickedIndex, thisIndex);
            for (int i = min; i <= max; i++) {
              select(panels.get(i));
            }
          }
        }
        lastClickedIndex = thisIndex;
        // event.notifyObservers();
        validate();
        repaint();
      }

      @Override
      public void waveZoomed(final AbstractWavePanel src, final double st, final double et,
          final double nst, final double net) {
        final double[] t = new double[] {st, et};
        addHistory(src, t);
        for (final AbstractWavePanel wvp : selectedSet) {
          if (wvp != src) {
            addHistory(wvp, t);
            wvp.zoom(nst, net);
            wvp.createImage();
          }
        }
      }

      @Override
      public void waveClosed(final AbstractWavePanel src) {
        LOGGER.debug("Removing wave: {}", src.getChannel());
        remove(src);
      }
    };
  }

  public void addListener(PickBoxListener listener) {
    listeners.add(listener);
  }

  protected int getWaveIndex(AbstractWavePanel src) {

    int panelIdx = -1;
    int searchIdx = 0;
    while (panelIdx < 0 && searchIdx < panels.size()) {

      if (src == panels.get(searchIdx)) {
        panelIdx = searchIdx;
      } else {
        searchIdx++;
      }
    }

    return panelIdx;
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
    if (image == null) {
      createImage();
    }
    super.paint(image.getGraphics());
    g.drawImage(image, 0, 0, null);
    g.setColor(Color.WHITE);
  }

  public void addPick(Arrival arrival) {
    CodeTimer timer = new CodeTimer("arrival");
    this.remove(emptyArrivalLabel);
    Pick pick = arrival.getPick();
    String channel = pick.getChannel();
    
    PickWavePanel wavePanel = findPanel(channel);
    if (wavePanel == null) {
      wavePanel = new PickWavePanel();
      wavePanel.setStatusLabel(statusLabel);
      wavePanel.setChannel(channel);
      SeismicDataSource source = seismicSources.get(channel);
      if (source == null) {
        source = new WebServicesSource();
        source.parse(buildParams(channel));
      }
      wavePanel.setDataSource(source);
      Wave wave = source.getWave(channel, startJ2k, endJ2k);
      if (wave != null) {
        panels.add(wavePanel);
        wavePanel.setWave(wave, startJ2k, endJ2k);
        add(wavePanel);
        wavePanel.setBottomBorderColor(Color.GRAY);
        wavePanel.setSize(getWidth(), wavePanelHeight);
        wavePanel.setDisplayTitle(true);
        wavePanel.setOffsets(54, 8, 21, 19);
        wavePanel.createImage();
        wavePanel.addListener(selectListener);
      }
    }

    wavePanel.addArrival(arrival);
    timer.stopAndReport();
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
    sb.append(DATASELECT_URL).append("|");
    sb.append(STATION_URL);

    return sb.toString();
  }

  private void resizeWaves() {
    for (PickWavePanel panel : panels) {
      panel.setSize(getWidth(), wavePanelHeight);
      panel.createImage();
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

  public void setWaveHeight(int height) {
    wavePanelHeight = height;
    resizeWaves();
  }


  public void writeImage() {
    SwarmConfig swarmConfig = SwarmConfig.getInstance();
    if (panels.size() == 0)
      return;

    final JFileChooser chooser = FileChooser.getFileChooser();
    final File lastPath = new File(swarmConfig.lastPath);
    chooser.setCurrentDirectory(lastPath);
    chooser.setSelectedFile(new File("clipboard.png"));
    chooser.setDialogTitle("Save Clipboard Screen Capture");
    final int result = chooser.showSaveDialog(APPLICATION_FRAME);
    File f = null;
    if (result == JFileChooser.APPROVE_OPTION) {
      f = chooser.getSelectedFile();

      if (f.exists()) {
        final int choice = JOptionPane.showConfirmDialog(APPLICATION_FRAME,
            "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION)
          return;
      }
      swarmConfig.lastPath = f.getParent();
    }
    if (f == null)
      return;

    int height = 0;

    final int width = panels.get(0).getWidth();
    for (final AbstractWavePanel panel : panels) {
      height += panel.getHeight();
    }

    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    final Graphics g = image.getGraphics();
    for (final AbstractWavePanel panel : panels) {
      panel.paint(g);
      g.translate(0, panel.getHeight());
    }
    try {
      final PngEncoderB png = new PngEncoderB(image, false, PngEncoder.FILTER_NONE, 7);
      final FileOutputStream out = new FileOutputStream(f);
      final byte[] bytes = png.pngEncode();
      out.write(bytes);
      out.close();
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
  }


  private void deselect(final AbstractWavePanel p) {
    selectedSet.remove(p);
    p.setBackgroundColor(BACKGROUND_COLOR);
    p.createImage();
    notifyListeners();
  }

  private void deselectAll() {
    final AbstractWavePanel[] panels = selectedSet.toArray(new AbstractWavePanel[0]);
    for (final AbstractWavePanel p : panels) {
      deselect(p);
    }
    notifyListeners();
  }

  private void select(final PickWavePanel p) {
    if (p == null || selectedSet.contains(p))
      return;

    selectedSet.add(p);
    p.setBackgroundColor(SELECT_COLOR);
    p.createImage();
    notifyListeners();
  }

  private void notifyListeners() {
    for (PickBoxListener listener : listeners) {
      listener.selectCountChanged(selectedSet.size());
    }
  }

  private PickWavePanel findPanel(String channel) {
    for (PickWavePanel panel : panels) {
      if (panel.getChannel().equals(channel)) {
        return panel;
      }
    }
    return null;
  }

  private int findPanelIndex(PickWavePanel panel) {
    int panelIdx = -1;
    int searchIdx = 0;
    while (panelIdx < 0 && searchIdx < panels.size()) {
      if (panels.get(searchIdx) == panel) {
        panelIdx = searchIdx;
      } else {
        searchIdx++;
      }
    }
    return panelIdx;
  }

  private void addHistory(final AbstractWavePanel wvp, final double[] t) {
    Stack<double[]> history = histories.get(wvp);
    if (history == null) {
      history = new Stack<double[]>();
      histories.put(wvp, history);
    }
    history.push(t);
  }

  public void scaleTime(final AbstractWavePanel wvp, final double pct) {
    final double st = wvp.getStartTime();
    final double et = wvp.getEndTime();
    final double[] t = new double[] {st, et};
    addHistory(wvp, t);
    final double dt = (et - st) * (1 - pct);
    final double mt = (et - st) / 2 + st;
    final double nst = mt - dt / 2;
    final double net = mt + dt / 2;
    fetchNewWave(wvp, nst, net);
  }

  public void scaleTime(final double pct) {
    for (final AbstractWavePanel p : selectedSet)
      scaleTime(p, pct);
  }

  public void back(final AbstractWavePanel wvp) {
    final Stack<double[]> history = histories.get(wvp);
    if (history == null || history.empty())
      return;

    final double[] t = history.pop();
    fetchNewWave(wvp, t[0], t[1]);
  }

  public void back() {
    for (final AbstractWavePanel p : selectedSet)
      back(p);
  }

  private void shiftTime(final AbstractWavePanel wvp, final double pct) {
    LOGGER.debug("shifting time {}", pct);
    final double st = wvp.getStartTime();
    final double et = wvp.getEndTime();
    final double[] t = new double[] {st, et};
    addHistory(wvp, t);
    final double dt = (et - st) * pct;
    final double nst = st + dt;
    final double net = et + dt;
    fetchNewWave(wvp, nst, net);
  }

  public void shiftTime(final double pct) {
    for (final AbstractWavePanel p : selectedSet)
      shiftTime(p, pct);
  }

  // TODO: This isn't right, this should be a method of waveviewpanel
  private void fetchNewWave(final AbstractWavePanel wvp, final double nst, final double net) {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        final SeismicDataSource sds = wvp.getDataSource();
        // Hacky fix for bug #84
        Wave sw = null;
        if (sds instanceof CachedDataSource)
          sw = ((CachedDataSource) sds).getBestWave(wvp.getChannel(), nst, net);
        else
          sw = sds.getWave(wvp.getChannel(), nst, net);
        wvp.setWave(sw, nst, net);
        return null;
      }

      @Override
      public void finished() {
        repaint();
      }
    };
    worker.start();
  }

  public void displaySettingsDialog() {
    if (panels.size() == 0)
      return;

    WaveViewSettings s = panels.get(0).getSettings();
    WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(s, selectedSet.size());
    wvsd.setVisible(true);
    for (PickWavePanel panel : selectedSet) {
      WaveViewSettings settings = panel.getSettings();
      settings.copy(s);
    }
  }

  public void mapKeyStroke(String keyStroke, String name, AbstractButton button) {
    UiUtils.mapKeyStrokeToButton(this, keyStroke, name, button);
  }

  public void setType(ViewType viewType) {
    for (AbstractWavePanel panel : selectedSet) {
      panel.getSettings().setType(viewType);
      panel.createImage();
    }
  }


  public void gotoTime(final AbstractWavePanel wvp, String t) {
    double j2k = Double.NaN;
    try {
      if (t.length() == 12)
        t = t + "30";

      j2k = J2kSec.parse("yyyyMMddHHmmss", t);
    } catch (final Exception e) {
      JOptionPane.showMessageDialog(APPLICATION_FRAME, "Illegal time value.", "Error",
          JOptionPane.ERROR_MESSAGE);
    }

    if (!Double.isNaN(j2k)) {
      double dt = 60;
      if (wvp.getWave() != null) {
        final double st = wvp.getStartTime();
        final double et = wvp.getEndTime();
        final double[] ts = new double[] {st, et};
        addHistory(wvp, ts);
        dt = (et - st);
      }

      final double tzo =
          SWARM_CONFIG.getTimeZone(wvp.getChannel()).getOffset(System.currentTimeMillis()) / 1000;

      final double nst = j2k - tzo - dt / 2;
      final double net = nst + dt;

      fetchNewWave(wvp, nst, net);
    }
    wvp.createImage();
  }

  public void gotoTime(final String t) {
    for (final AbstractWavePanel p : selectedSet)
      gotoTime(p, t);
  }

  public void sortChannelsByNearest() {
    // TODO Auto-generated method stub
    
  }
}
