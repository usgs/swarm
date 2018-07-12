package gov.usgs.volcanoes.swarm;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.swarm.chooser.DataChooser;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.heli.HelicorderViewerFrame;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings;
import gov.usgs.volcanoes.swarm.wave.MultiMonitor;
import gov.usgs.volcanoes.swarm.wave.SwarmMultiMonitors;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Swarm Layout class.
 *
 * @author Dan Cervelli
 */
public class SwarmLayout implements Comparable<SwarmLayout> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SwarmLayout.class);

  private static final JFrame applicationFrame = Swarm.getApplicationFrame();
  private final ConfigFile config;

  public SwarmLayout(final ConfigFile c) {
    config = c;
  }

  /**
   * Create Swarm layout.
   * @param fn config file name
   * @return swarm layout
   */
  public static SwarmLayout createSwarmLayout(final String fn) {
    final ConfigFile cf = new ConfigFile(fn);
    if (cf == null || !cf.wasSuccessfullyRead()) {
      return null;
    }

    final String name = cf.getString("name");
    if (name == null) {
      return null;
    } else {
      return new SwarmLayout(cf);
    }
  }

  /**
   * Save layout.
   */
  public void save() {
    String fn = getName().replace(' ', '_');
    final String n = fn.replaceAll("[^a-zA-Z0-9_]", "");
    final String pre = "layouts" + File.separatorChar;
    final String post = ".config";
    fn = pre + n + post;
    boolean exists = new File(fn).exists();
    int i = 0;
    while (exists) {
      i++;
      fn = pre + n + "_" + i + post;
      exists = new File(fn).exists();
    }

    config.writeToFile(fn);
  }

  /**
   * Delete layout.
   */
  public void delete() {
    try {
      final String fn = config.getName() + ".config";
      LOGGER.info("deleting file: " + fn);
      new File(fn).delete();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  public void setName(final String s) {
    config.put("name", s, false);
  }

  public String getName() {
    return config.getString("name");
  }

  /**
   * Process layout.
   */
  public void process() {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        SwarmInternalFrames.removeAllFrames();
        processChooser();
        processMap();
        processWaves();
        processHelicorders();
        processMonitors();
        processClipboard();
        processRsam();
        return null;
      }

      @Override
      public void finished() {
        processKiosk();
      }
    };
    worker.start();
  }

  private class ChooserListener implements ActionListener {
    private final List<String> sources;

    private ChooserListener() {
      sources = new ArrayList<String>();
    }

    public void addSource(final String s) {
      sources.add(s);
    }

    public synchronized void actionPerformed(final ActionEvent e) {
      final String src = e.getActionCommand();
      sources.remove(src);
      if (e.getID() == DataChooser.NO_DATA_SOURCE) {
        JOptionPane.showMessageDialog(applicationFrame,
            "The data source '" + src + "' does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
      } else if (e.getID() == DataChooser.NO_CHANNEL_LIST) {
        JOptionPane.showMessageDialog(applicationFrame,
            "The data source '" + src + "' could not be opened.", "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }

    public synchronized boolean finished() {
      return sources.size() == 0;
    }
  }

  private void processChooser() {
    final ConfigFile cf = config.getSubConfig("chooser");
    final ChooserListener cl = new ChooserListener();
    final List<String> sources = cf.getList("source");
    if (sources != null) {
      for (final String src : sources) {
        cl.addSource(src);
      }
      DataChooser.getInstance().processLayout(cf, cl);
      while (!cl.finished()) {
        try {
          Thread.sleep(100);
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void processMap() {
    final ConfigFile cf = config.getSubConfig("map");
    final MapFrame mapFrame = MapFrame.getInstance();
    if (cf.getString("x") != null) {
      mapFrame.setVisible(true);
      mapFrame.processLayout(cf);
    } else {
      mapFrame.setVisible(false);
    }
  }

  private void processMonitors() {
    final List<String> monitors = config.getList("monitor");
    if (monitors == null) {
      return;
    }

    for (final String monitor : monitors) {
      final ConfigFile cf = config.getSubConfig(monitor);
      final SeismicDataSource sds = SwarmConfig.getInstance().getSource(cf.getString("source"));
      if (sds != null && DataChooser.getInstance().isSourceOpened(sds.getName())) {
        final MultiMonitor mm = SwarmMultiMonitors.getMonitor(sds);
        mm.processLayout(cf);
        mm.setVisible(true);
      }
    }
  }

  private void processHelicorders() {
    final List<String> helis = config.getList("helicorder");
    if (helis == null) {
      return;
    }

    for (final String heli : helis) {
      final ConfigFile cf = config.getSubConfig(heli);
      final SeismicDataSource sds = SwarmConfig.getInstance().getSource(cf.getString("source"));
      if (sds != null) {
        final HelicorderViewerFrame hvf = new HelicorderViewerFrame(cf);
        hvf.addLinkListeners();
        SwarmInternalFrames.add(hvf, false);
      }
    }
  }
  
  private void processWaves() {
    final List<String> waves = config.getList("wave");
    if (waves == null) {
      return;
    }

    for (final String wave : waves) {
      final ConfigFile cf = config.getSubConfig(wave);
      final SeismicDataSource sds = SwarmConfig.getInstance().getSource(cf.getString("source"));
      String channel = cf.getString("channel");
      WaveViewSettings wvs = new WaveViewSettings();
      wvs.set(cf);
      Swarm.openRealtimeWave(sds, channel, wvs);
    }
  }

  private void processKiosk() {
    String k = config.getString("kiosk");
    if (k == null) {
      k = "false";
    }
    final int x = StringUtils.stringToInt(config.getString("kioskX"), -1);
    final int y = StringUtils.stringToInt(config.getString("kioskY"), -1);

    final boolean kiosk = Boolean.parseBoolean(k);
    if (kiosk && x != -1 && y != -1) {
      applicationFrame.setLocation(x, y);
    }
    Swarm.getApplication().setFullScreenMode(kiosk);
  }
  
  private void processClipboard() {
    WaveClipboardFrame wcf = WaveClipboardFrame.getInstance();
    final ConfigFile cf = config.getSubConfig("clipboard");
    if (cf.getConfig().size() == 0) {
      wcf.hide();
      return;
    }
    wcf.removeWaves();
    final int waves = cf.getInt("waves");
    for (int i = 0; i < waves; i++) {
      final ConfigFile scf = cf.getSubConfig("wave-" + i);
      WaveViewSettings wvs = new WaveViewSettings();
      wvs.set(scf);
      WaveViewPanel wvp = new WaveViewPanel(wvs);
      SeismicDataSource ds = SwarmConfig.getInstance().getSource(scf.getString("source"));
      wvp.setDataSource(ds);
      String channel = scf.getString("channel");
      wvp.setChannel(channel);
      double st = scf.getDouble("startTime");
      double et = scf.getDouble("endTime");
      Wave wave = ds.getWave(channel, st, et);
      wvp.setWave(wave, st, et);
      wcf.addWave(wvp);
    }
    wcf.show();
  }
  
  private void processRsam() {
    final List<String> rsams = config.getList("rsam");
    if (rsams == null) {
      return;
    }
    for (final String rsam : rsams) {
      final ConfigFile cf = config.getSubConfig(rsam);
      final SeismicDataSource sds = SwarmConfig.getInstance().getSource(cf.getString("source"));
      String channel = cf.getString("channel");
      RsamViewSettings setting = new RsamViewSettings();
      setting.set(cf);
      Swarm.openRsam(sds, channel, setting);
    }
  }

  public int compareTo(final SwarmLayout o) {
    return getName().compareToIgnoreCase(o.getName());
  }
}
