package gov.usgs.volcanoes.swarm.data;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.data.file.FileType;
import gov.usgs.volcanoes.core.data.file.SeismicDataFile;
import gov.usgs.volcanoes.core.data.file.WinDataFile;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.FileTypeDialog;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.map.MapFrame;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: remove reference to application frame. Non-GUI apps want to use the class too.
 *
 * @author Dan Cervelli
 */
public class FileDataSource extends AbstractCachingDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileDataSource.class);

  private static final FileDataSource INSTANCE = new FileDataSource();

  private final Map<String, double[]> channelTimes;
  private final Set<String> openFiles;
  private static SwarmConfig swarmConfig;
  public static boolean useWinBatch = false;

  private FileDataSource() {
    super();

    channelTimes = new HashMap<String, double[]>();
    openFiles = new HashSet<String>();
    maxSize = Integer.MAX_VALUE;
    storeInUserConfig = false;
    name = "Files";
    swarmConfig = SwarmConfig.getInstance();
  }

  public static FileDataSource getInstance() {
    return INSTANCE;
  }

  @Override
  public void flush() {
    final List<String> channels = getChannels();
    if (channels != null) {
      for (final String ch : channels) {
        swarmConfig.removeMetadata(ch);
      }
    }
    super.flush();
    openFiles.clear();
    channelTimes.clear();
    fireChannelsUpdated();
  }

  private void updateChannelTimes(final String channel, final double t1, final double t2) {
    double[] ct = channelTimes.get(channel);
    if (ct == null) {
      ct = new double[] {t1, t2};
      channelTimes.put(channel, ct);
    }
    ct[0] = Math.min(ct[0], t1);
    ct[1] = Math.max(ct[1], t2);
  }

  /**
   * Open files.
   * @param fs files
   */
  public void openFiles(final File[] fs) {
    useWinBatch = false;
    FileTypeDialog dialog = null;
    for (int i = 0; i < fs.length; i++) {
      final String fileName = fs[i].getPath();
      if (openFiles.contains(fileName)) {
        continue;
      }

      SeismicDataFile file = SeismicDataFile.getFile(fileName);

      if (file == null) {
        if (dialog == null) {
          dialog = new FileTypeDialog(false);
        }
          
        if (!dialog.isOpen() || (dialog.isOpen() && !dialog.isAssumeSame())) {
          dialog.setFilename(fs[i].getName());
          dialog.setVisible(true);
        }

        FileType fileType;
        if (dialog.isCancelled() || dialog.getFileType() == null) {
          fileType = FileType.UNKNOWN;
        } else {
          fileType = dialog.getFileType();
        }
        
        if (fileType == FileType.WIN) { // Open WIN config file
          if (!useWinBatch) {
            openWinConfigFileDialog();
          }
          useWinBatch = dialog.isAssumeSame();
          if (WinDataFile.configFile == null) {
            JOptionPane.showMessageDialog(applicationFrame, "No WIN configuration file set.", "WIN",
                JOptionPane.ERROR_MESSAGE);
            LOGGER.error("Unable to open: {} -> {}", fs[i].getPath(), fileType);
            return;
          }
        }

        LOGGER.info("user input file type: {} -> {}", fs[i].getPath(), fileType);
        file = SeismicDataFile.getFile(fileName, fileType);
      }

      if (file != null) {
        readFile(file);
      } else {
        LOGGER.error("Could not open file: {} ", fs[i].getPath());
        JOptionPane.showMessageDialog(applicationFrame, "Could not open file: " + fileName, "Error",
            JOptionPane.ERROR_MESSAGE);
      }

    }
  }

  private void readFile(final SeismicDataFile file) {
    final String fileName = file.getFileName();

    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        Object result = null;
        fireChannelsProgress(fileName, 0);
        try {
          LOGGER.debug("opening file: {}", fileName);
          file.read();
          double progress = 0.2;
          fireChannelsProgress(fileName, progress);

          final int channelCount = file.getChannels().size();
          final double progressInc = (1 - .2) / channelCount;
          for (final String channel : file.getChannels()) {
            final Metadata md = swarmConfig.getMetadata(channel.replaceAll("\\$", " "), true);
            md.addGroup(file.getGroup());

            final Wave wave = file.getWave(channel);
            progress += progressInc;
            fireChannelsProgress(fileName, progress);
            updateChannelTimes(channel, wave.getStartTime(), wave.getEndTime());
            progress += progressInc;
            fireChannelsProgress(fileName, progress);
            cacheWaveAsHelicorder(channel, wave);
            putWave(channel, wave);
          }

          swarmConfig.assignMetadataSource(file.getChannels(), FileDataSource.this);
          openFiles.add(fileName);
        } catch (final Throwable t) {
          t.printStackTrace();
          result = t;
        } 
        fireChannelsProgress(fileName, 1);
        fireChannelsUpdated();
        MapFrame.getInstance().reset(false);
        return result;
      }

      @Override
      public void finished() {
        if (getValue() != null) {
          JOptionPane.showMessageDialog(applicationFrame, "Could not open file: " + fileName,
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    };
    worker.start();
  }
  

  /**
   * File open dialog for WIN configuration file.
   */
  public static void openWinConfigFileDialog() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("WIN Config (.config)", "config"));
    chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text File (.txt)", "txt"));
    chooser.setCurrentDirectory(new File(swarmConfig.lastPath));
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle("Select WIN configuration file...");
    if (WinDataFile.configFile != null) {
      chooser.setSelectedFile(WinDataFile.configFile);
    }
    int result = chooser.showOpenDialog(applicationFrame);
    if (result == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      SwarmConfig.getInstance().lastPath = f.getParent();
      WinDataFile.configFile = f;
    }
  }
  
  @Override
  public HelicorderData getHelicorder(String channel, double t1, double t2,
      final GulperListener gl) {
    channel = channel.replace(' ', '$');
    final double[] ct = channelTimes.get(channel);
    if (ct == null) {
      return null;
    }

    final double dt = t2 - t1;
    final double now = J2kSec.now();
    if (Math.abs(now - t2) < 3600) {
      t2 = ct[1];
      t1 = t2 - dt;
    }
    return super.getHelicorder(channel, t1, t2, gl);
  }

  @Override
  public Wave getWave(final String station, final double t1, final double t2) {
    Wave wave;
    final List<CachedWave> waves = waveCache.get(station.replace(' ', '$'));
    if (waves == null) {
      return null;
    } else {
      final List<Wave> parts = new ArrayList<Wave>();
      double minT = 1E300;
      double maxT = -1E300;
      for (final CachedWave cw : waves) {
        if (cw.wave.overlaps(t1, t2)) {
          parts.add(cw.wave);
          minT = Math.min(minT, cw.t1);
          maxT = Math.max(maxT, cw.t2);
        }
      }

      if (parts.size() == 1) {
        return parts.get(0);
      }

      wave = Wave.join(parts, minT, maxT);
      if (wave != null) {
        wave = wave.subset(t1, t2);
      }
    }
    return wave;
  }

  @Override
  public String toConfigString() {
    return name + ";file:";
  }
}
