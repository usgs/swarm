package gov.usgs.swarm.rsam;

import gov.usgs.math.BinSize;
import gov.usgs.math.Butterworth;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Tom Parker
 */

public class RsamViewSettings {
    private static final String DEFAULTS_FILENAME = "RsamDefaults.config";

    public enum ViewType {
        VALUES, COUNTS;
    }

    public int valuesPeriod;
    public int countsPeriod;
    public boolean detrend;
    public boolean runningMedian;
    public double runningMedianPeriod;
    public boolean runningMean;
    public double runningMeanPeriod;

    public int eventThreshold;
    public double eventRatio;
    public double eventMaxLength;
    public BinSize binSize;

    public RsamViewPanel view;
    public RsamViewSettingsToolbar toolbar;
    private ViewType viewType;

    public Butterworth filter;

    private static RsamViewSettings DEFAULT_RSAM_VIEW_SETTINGS;
    private Set<SettingsListener> listeners;

    static {
        DEFAULT_RSAM_VIEW_SETTINGS = new RsamViewSettings();
        DEFAULT_RSAM_VIEW_SETTINGS.viewType = ViewType.VALUES;
        DEFAULT_RSAM_VIEW_SETTINGS.valuesPeriod = 600;
        DEFAULT_RSAM_VIEW_SETTINGS.countsPeriod = 10;
        DEFAULT_RSAM_VIEW_SETTINGS.detrend = false;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMedian = false;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMedianPeriod = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMean = false;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMeanPeriod = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.eventThreshold = 50;
        DEFAULT_RSAM_VIEW_SETTINGS.eventRatio = 1.3;
        DEFAULT_RSAM_VIEW_SETTINGS.eventMaxLength = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.binSize = BinSize.HOUR;

        List<String> candidateNames = new LinkedList<String>();
        candidateNames.add(DEFAULTS_FILENAME);
        candidateNames.add(System.getProperty("user.home") + File.separatorChar + DEFAULTS_FILENAME);
        String defaultsFile = ConfigFile.findConfig(candidateNames);
        if (defaultsFile == null)
            defaultsFile = DEFAULTS_FILENAME;

        ConfigFile cf = new ConfigFile(defaultsFile);
        if (cf.wasSuccessfullyRead()) {
            ConfigFile sub = cf.getSubConfig("default");
            DEFAULT_RSAM_VIEW_SETTINGS.set(sub);
        } else {
            DEFAULT_RSAM_VIEW_SETTINGS.save(cf, "default");
            cf.writeToFile(DEFAULTS_FILENAME);
        }
    }

    public RsamViewSettings() {
        filter = new Butterworth();
        view = null;
        if (DEFAULT_RSAM_VIEW_SETTINGS != null)
            copy(DEFAULT_RSAM_VIEW_SETTINGS);

        listeners = new HashSet<SettingsListener>();
    }

    public void copy(RsamViewSettings s) {
        viewType = s.viewType;
        valuesPeriod = s.valuesPeriod;
        countsPeriod = s.countsPeriod;
        detrend = s.detrend;
        runningMedian = s.runningMedian;
        runningMedianPeriod = s.runningMedianPeriod;
        runningMean = s.runningMean;
        runningMeanPeriod = s.runningMeanPeriod;
        eventRatio = s.eventRatio;
        eventThreshold = s.eventThreshold;
        eventMaxLength = s.eventMaxLength;
        binSize = s.binSize;
    }

    public void set(ConfigFile cf) {
        viewType = ViewType.valueOf(cf.getString("viewType"));
        valuesPeriod = Util.stringToInt(cf.getString("valuesPeriod"), DEFAULT_RSAM_VIEW_SETTINGS.valuesPeriod);
        countsPeriod = Util.stringToInt(cf.getString("countsPeriod"), DEFAULT_RSAM_VIEW_SETTINGS.countsPeriod);
        detrend = Util.stringToBoolean(cf.getString("detrend"), DEFAULT_RSAM_VIEW_SETTINGS.detrend);
        runningMedian = Util.stringToBoolean(cf.getString("runningMedian"), DEFAULT_RSAM_VIEW_SETTINGS.runningMedian);
        runningMedianPeriod = Util.stringToDouble(cf.getString("runningMedianPeriod"),
                DEFAULT_RSAM_VIEW_SETTINGS.runningMedianPeriod);
        runningMean = Util.stringToBoolean(cf.getString("runningMean"), DEFAULT_RSAM_VIEW_SETTINGS.runningMean);
        runningMeanPeriod = Util.stringToDouble(cf.getString("runningMeanPeriod"),
                DEFAULT_RSAM_VIEW_SETTINGS.runningMeanPeriod);
        eventRatio = Util.stringToDouble(cf.getString("eventRatio"), DEFAULT_RSAM_VIEW_SETTINGS.eventRatio);
        eventThreshold = Util.stringToInt(cf.getString("eventThreshold"), DEFAULT_RSAM_VIEW_SETTINGS.eventThreshold);
        eventMaxLength = Util.stringToDouble(cf.getString("eventMaxLength"), DEFAULT_RSAM_VIEW_SETTINGS.eventMaxLength);
        binSize = BinSize.fromString(cf.getString("binSize"));
    }

    public void save(ConfigFile cf, String prefix) {
        cf.put(prefix + ".valuesPeriod", Integer.toString(valuesPeriod));
        cf.put(prefix + ".countsPeriod", Integer.toString(countsPeriod));
        cf.put(prefix + ".viewType", viewType.toString());
        cf.put(prefix + ".detrend", Boolean.toString(detrend));
        cf.put(prefix + ".runningMedian", Boolean.toString(runningMedian));
        cf.put(prefix + ".runningMedianPeriod", Double.toString(runningMedianPeriod));
        cf.put(prefix + ".runningMean", Boolean.toString(runningMean));
        cf.put(prefix + ".runningMeanPeriod", Double.toString(runningMeanPeriod));
        cf.put(prefix + ".eventRatio", Double.toString(eventRatio));
        cf.put(prefix + ".eventThreshold", Double.toString(eventThreshold));
        cf.put(prefix + ".eventMaxLength", Double.toString(eventMaxLength));
        cf.put(prefix + ".binSize", binSize.toString());
    }

    public void setType(ViewType t) {
        viewType = t;
        notifyListeners();
    }

    public ViewType getType() {
        return viewType;
    }
    
    public void cycleType() {
        switch (viewType) {
        case VALUES:
            viewType = ViewType.COUNTS;
            break;
        case COUNTS:
            viewType = ViewType.VALUES;
            break;
        }
        notifyListeners();
    }
    
    public void addListener(SettingsListener l) {
        listeners.add(l);
    }

    private void notifyListeners() {
        for (SettingsListener l : listeners) 
            l.settingsChanged();
    }
}