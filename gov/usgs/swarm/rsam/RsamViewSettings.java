package gov.usgs.swarm.rsam;

import gov.usgs.math.BinSize;
import gov.usgs.math.Butterworth;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Tom Parker
 */

public class RsamViewSettings {
    private static final String DEFAULTS_FILENAME = "RsamDefaults.config";

    public enum ViewType {
        VALUES, COUNTS;
    }

    public boolean detrend;
    public boolean despike;
    public double despikePeriod;
    public boolean bandpass;
    public double bandpassMin;
    public double bandpassMax;
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
    public ViewType viewType;

    public Butterworth filter;

    private static RsamViewSettings DEFAULT_RSAM_VIEW_SETTINGS;

    static {
        DEFAULT_RSAM_VIEW_SETTINGS = new RsamViewSettings();
        DEFAULT_RSAM_VIEW_SETTINGS.viewType = ViewType.VALUES;
        DEFAULT_RSAM_VIEW_SETTINGS.detrend = false;
        DEFAULT_RSAM_VIEW_SETTINGS.despike = false;
        DEFAULT_RSAM_VIEW_SETTINGS.despikePeriod = 300;
        DEFAULT_RSAM_VIEW_SETTINGS.bandpass = false;
        DEFAULT_RSAM_VIEW_SETTINGS.bandpassMin = 2;
        DEFAULT_RSAM_VIEW_SETTINGS.bandpassMax = 20;
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
    }

    public RsamViewSettings(RsamViewSettings s) {
        copy(s);
    }

    public void copy(RsamViewSettings s) {
        viewType = s.viewType;
        detrend = s.detrend;
        despike = s.despike;
        despikePeriod = s.despikePeriod;
        bandpass = s.bandpass;
        bandpassMin = s.bandpassMin;
        bandpassMax = s.bandpassMax;
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
        detrend = Util.stringToBoolean(cf.getString("detrend"), DEFAULT_RSAM_VIEW_SETTINGS.detrend);
        despike = Util.stringToBoolean(cf.getString("despike"), DEFAULT_RSAM_VIEW_SETTINGS.despike);
        despikePeriod = Util.stringToDouble(cf.getString("despikePeriod"), DEFAULT_RSAM_VIEW_SETTINGS.despikePeriod);
        bandpass = Util.stringToBoolean(cf.getString("bandpass"), DEFAULT_RSAM_VIEW_SETTINGS.bandpass);
        bandpassMin = Util.stringToDouble(cf.getString("bandpassMin"), DEFAULT_RSAM_VIEW_SETTINGS.bandpassMin);
        bandpassMax = Util.stringToDouble(cf.getString("bandpassMax"), DEFAULT_RSAM_VIEW_SETTINGS.bandpassMax);
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
        cf.put(prefix + ".viewType", viewType.toString());
        cf.put(prefix + ".detrend", Boolean.toString(detrend));
        cf.put(prefix + ".despike", Boolean.toString(despike));
        cf.put(prefix + ".despikePeriod", Double.toString(despikePeriod));
        cf.put(prefix + ".bandpass", Boolean.toString(bandpass));
        cf.put(prefix + ".bandpassMin", Double.toString(bandpassMin));
        cf.put(prefix + ".bandpassMax", Double.toString(bandpassMax));
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
        notifyView();
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
        notifyView();
    }

    public void notifyView() {
        if (view != null)
            view.settingsChanged();

        if (toolbar != null)
            toolbar.settingsChanged();
    }

}