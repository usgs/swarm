package gov.usgs.swarm.data;

import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.swarm.FileTypeDialog;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmConfig;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.util.CurrentTime;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * 
 * @author Dan Cervelli
 */
public class FileDataSource extends AbstractCachingDataSource {

    private static final FileDataSource INSTANCE = new FileDataSource();

    private Map<String, double[]> channelTimes;
    private Set<String> openFiles;
    private static SwarmConfig swarmConfig;

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

    public void flush() {
        List<String> channels = getChannels();
        if (channels != null)
            for (String ch : channels)
                swarmConfig.removeMetadata(ch);
        super.flush();
        openFiles.clear();
        channelTimes.clear();
        fireChannelsUpdated();
    }

    private void updateChannelTimes(String channel, double t1, double t2) {
        double[] ct = channelTimes.get(channel);
        if (ct == null) {
            ct = new double[] { t1, t2 };
            channelTimes.put(channel, ct);
        }
        ct[0] = Math.min(ct[0], t1);
        ct[1] = Math.max(ct[1], t2);
    }

    public void openFiles(File[] fs) {
        FileTypeDialog dialog = null;
        for (int i = 0; i < fs.length; i++) {
            String fileName = fs[i].getPath();
            if (openFiles.contains(fileName))
                continue;

            SeismicDataFile file = SeismicDataFile.getFile(fileName);

            if (file == null) {
                if (dialog == null)
                    dialog = new FileTypeDialog();
                if (!dialog.isOpen() || (dialog.isOpen() && !dialog.isAssumeSame())) {
                    dialog.setFilename(fs[i].getName());
                    dialog.setVisible(true);
                }

                FileType fileType;
                if (dialog.isCancelled())
                    fileType = FileType.UNKNOWN;
                else
                    fileType = dialog.getFileType();

                logger.warning("user input file type: " + fs[i].getPath() + " -> " + fileType);
                file = SeismicDataFile.getFile(fileName, fileType);
            }

            if (file == null)
                JOptionPane.showMessageDialog(Swarm.getApplication(), "Could not open file: " + fileName, "Error",
                        JOptionPane.ERROR_MESSAGE);

            readFile(file);
            swarmConfig.lastPath = fs[i].getParent();
        }
    }

    private void readFile(final SeismicDataFile file) {
        final String fileName = file.getFileName();

        SwingWorker worker = new SwingWorker() {
            public Object construct() {
                Object result = null;
                fireChannelsProgress(fileName, 0);
                try {
                    logger.fine("opening file: " + fileName);
                    file.read();
                    double progress = 0.2;
                    fireChannelsProgress(fileName, progress);

                    int channelCount = file.getChannels().size();
                    double progressInc = (1 - .2) / channelCount;
                    for (String channel : file.getChannels()) {
                        Metadata md = swarmConfig.getMetadata(channel, true);
                        md.addGroup(file.getGroup());

                        Wave wave = file.getWave(channel);
                        progress += progressInc;
                        fireChannelsProgress(fileName, progress);
                        updateChannelTimes(channel, wave.getStartTime(), wave.getEndTime());
                        progress += progressInc;
                        fireChannelsProgress(fileName, progress);
                        cacheWaveAsHelicorder(channel, wave);
                        putWave(channel, wave);
                    }

                    openFiles.add(fileName);
                } catch (Throwable t) {
                    t.printStackTrace();
                    result = t;
                }
                fireChannelsProgress(fileName, 1);
                fireChannelsUpdated();
                return result;
            }

            public void finished() {
                if (getValue() != null) {
                    JOptionPane.showMessageDialog(Swarm.getApplication(), "Could not open file: " + fileName, "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.start();
    }

    public HelicorderData getHelicorder(String channel, double t1, double t2, GulperListener gl) {
        double[] ct = channelTimes.get(channel);
        if (ct == null)
            return null;

        double dt = t2 - t1;
        double now = CurrentTime.getInstance().nowJ2K();
        if (Math.abs(now - t2) < 3600) {
            t2 = ct[1];
            t1 = t2 - dt;
        }

        return super.getHelicorder(channel, t1, t2, gl);
    }

    public Wave getWave(String station, double t1, double t2) {
        Wave wave;
        List<CachedWave> waves = waveCache.get(station);
        if (waves == null)
            return null;
        else {
            List<Wave> parts = new ArrayList<Wave>();
            double minT = 1E300;
            double maxT = -1E300;
            for (CachedWave cw : waves) {
                if (cw.wave.overlaps(t1, t2)) {
                    parts.add(cw.wave);
                    minT = Math.min(minT, cw.t1);
                    maxT = Math.max(maxT, cw.t2);
                }
            }

            if (parts.size() == 1)
                return parts.get(0);

            wave = Wave.join(parts, minT, maxT);
            if (wave != null)
                wave = wave.subset(t1, t2);
        }
        return wave;
    }

    public String toConfigString() {
        return name + ";file:";
    }
}
