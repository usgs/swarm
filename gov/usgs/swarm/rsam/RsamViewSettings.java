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

    public int valuesPeriodS;
    public int countsPeriodS;
    public boolean detrend;
    public boolean runningMedian;
    public double runningMedianPeriodS;
    public boolean runningMean;
    public double runningMeanPeriodS;

    public int eventThreshold;
    public double eventRatio;
    public double eventMaxLengthS;
    public BinSize binSize;
    private boolean autoScale;
    public int scaleMax;
    public int scaleMin;
    
    private int spanLengthS;
    private ViewType viewType;

    private static RsamViewSettings DEFAULT_RSAM_VIEW_SETTINGS;
    private Set<SettingsListener> listeners;

    static {
        DEFAULT_RSAM_VIEW_SETTINGS = new RsamViewSettings();
        DEFAULT_RSAM_VIEW_SETTINGS.viewType = ViewType.VALUES;
        DEFAULT_RSAM_VIEW_SETTINGS.valuesPeriodS = 600;
        DEFAULT_RSAM_VIEW_SETTINGS.countsPeriodS = 10;
        DEFAULT_RSAM_VIEW_SETTINGS.detrend = false;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMedian = false;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMedianPeriodS = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMean = false;
        DEFAULT_RSAM_VIEW_SETTINGS.runningMeanPeriodS = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.eventThreshold = 50;
        DEFAULT_RSAM_VIEW_SETTINGS.eventRatio = 1.3;
        DEFAULT_RSAM_VIEW_SETTINGS.eventMaxLengthS = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.binSize = BinSize.HOUR;
        DEFAULT_RSAM_VIEW_SETTINGS.spanLengthS = 60 * 60* 24 * 7;
        DEFAULT_RSAM_VIEW_SETTINGS.autoScale = true;
        DEFAULT_RSAM_VIEW_SETTINGS.scaleMax = 100;
        DEFAULT_RSAM_VIEW_SETTINGS.scaleMin = 0;
        
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
        if (DEFAULT_RSAM_VIEW_SETTINGS != null)
            copy(DEFAULT_RSAM_VIEW_SETTINGS);

        listeners = new HashSet<SettingsListener>();
    }

    public void copy(RsamViewSettings s) {
        viewType = s.viewType;
        valuesPeriodS = s.valuesPeriodS;
        countsPeriodS = s.countsPeriodS;
        detrend = s.detrend;
        runningMedian = s.runningMedian;
        runningMedianPeriodS = s.runningMedianPeriodS;
        runningMean = s.runningMean;
        runningMeanPeriodS = s.runningMeanPeriodS;
        eventRatio = s.eventRatio;
        eventThreshold = s.eventThreshold;
        eventMaxLengthS = s.eventMaxLengthS;
        binSize = s.binSize;
        autoScale = s.autoScale;
        scaleMax = s.scaleMax;
        scaleMin = s.scaleMin;
    }

    public void set(ConfigFile cf) {
        viewType = ViewType.valueOf(cf.getString("viewType"));
        valuesPeriodS = Util.stringToInt(cf.getString("valuesPeriod"), DEFAULT_RSAM_VIEW_SETTINGS.valuesPeriodS);
        countsPeriodS = Util.stringToInt(cf.getString("countsPeriod"), DEFAULT_RSAM_VIEW_SETTINGS.countsPeriodS);
        detrend = Util.stringToBoolean(cf.getString("detrend"), DEFAULT_RSAM_VIEW_SETTINGS.detrend);
        runningMedian = Util.stringToBoolean(cf.getString("runningMedian"), DEFAULT_RSAM_VIEW_SETTINGS.runningMedian);
        runningMedianPeriodS = Util.stringToDouble(cf.getString("runningMedianPeriod"),
                DEFAULT_RSAM_VIEW_SETTINGS.runningMedianPeriodS);
        runningMean = Util.stringToBoolean(cf.getString("runningMean"), DEFAULT_RSAM_VIEW_SETTINGS.runningMean);
        runningMeanPeriodS = Util.stringToDouble(cf.getString("runningMeanPeriod"),
                DEFAULT_RSAM_VIEW_SETTINGS.runningMeanPeriodS);
        eventRatio = Util.stringToDouble(cf.getString("eventRatio"), DEFAULT_RSAM_VIEW_SETTINGS.eventRatio);
        eventThreshold = Util.stringToInt(cf.getString("eventThreshold"), DEFAULT_RSAM_VIEW_SETTINGS.eventThreshold);
        eventMaxLengthS = Util.stringToDouble(cf.getString("eventMaxLength"), DEFAULT_RSAM_VIEW_SETTINGS.eventMaxLengthS);
        binSize = BinSize.fromString(cf.getString("binSize"));
        autoScale = Util.stringToBoolean(cf.getString("autoScale"), DEFAULT_RSAM_VIEW_SETTINGS.autoScale);
        scaleMax = Util.stringToInt(cf.getString("scaleMax"), DEFAULT_RSAM_VIEW_SETTINGS.scaleMax);
        scaleMin = Util.stringToInt(cf.getString("scaleMin"), DEFAULT_RSAM_VIEW_SETTINGS.scaleMin);
    }

    public void save(ConfigFile cf, String prefix) {
        cf.put(prefix + ".valuesPeriod", Integer.toString(valuesPeriodS));
        cf.put(prefix + ".countsPeriod", Integer.toString(countsPeriodS));
        cf.put(prefix + ".viewType", viewType.toString());
        cf.put(prefix + ".detrend", Boolean.toString(detrend));
        cf.put(prefix + ".runningMedian", Boolean.toString(runningMedian));
        cf.put(prefix + ".runningMedianPeriod", Double.toString(runningMedianPeriodS));
        cf.put(prefix + ".runningMean", Boolean.toString(runningMean));
        cf.put(prefix + ".runningMeanPeriod", Double.toString(runningMeanPeriodS));
        cf.put(prefix + ".eventRatio", Double.toString(eventRatio));
        cf.put(prefix + ".eventThreshold", Double.toString(eventThreshold));
        cf.put(prefix + ".eventMaxLength", Double.toString(eventMaxLengthS));
        cf.put(prefix + ".binSize", binSize.toString());
        cf.put(prefix + ".autoScale", Boolean.toString(autoScale));
        cf.put(prefix + ".scaleMax", Double.toString(scaleMax));
        cf.put(prefix + ".scaleMin", Double.toString(scaleMin));
    }

    public void setSpanLength(int spanLengthS) {
        this.spanLengthS = spanLengthS;
        notifyListeners();
    }
    
    public int getSpanLength() {
        return spanLengthS;
    }
    
    public void setType(ViewType t) {
        viewType = t;
        notifyListeners();
    }

    public ViewType getType() {
        return viewType;
    }
    
    public void setAutoScale(boolean isAutoScale) {
        this.autoScale = isAutoScale;
    }
    
    public boolean getAutoScale() {
        return autoScale;
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