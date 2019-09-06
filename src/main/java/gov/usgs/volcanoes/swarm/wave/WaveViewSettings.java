package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.legacy.plot.color.Spectrum;
import gov.usgs.volcanoes.core.math.Butterworth;
import gov.usgs.volcanoes.core.util.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Wave View Settings.
 * 
 * @author Dan Cervelli
 */

public class WaveViewSettings {
  private static final String DEFAULTS_FILENAME = "WaveDefaults.config";

  public enum ViewType {
    WAVE("W"), SPECTRA("S"), SPECTROGRAM("G"), PARTICLE_MOTION("M");

    public String code;

    private ViewType(String c) {
      code = c;
    }

    /**
     * Get view type from String.
     * 
     * @param c S for Spectra, G for Specgrogram. Returns Wave otherwise.
     * @return view type enum (e.g. Spectra, Spectrogram, Wave)
     */
    public static ViewType fromString(String c) {
      if (c.equals("S")) {
        return SPECTRA;
      } else if (c.equals("G")) {
        return SPECTROGRAM;
      } else if (c.equals("M")) {
        return PARTICLE_MOTION;
      } else {
        return WAVE;
      }
    }
  }


  public WaveViewPanel view;
  public WaveViewSettingsToolbar toolbar;
  public ViewType viewType;
  
  // wave settings
  public boolean autoScaleAmp;
  public boolean autoScaleAmpMemory;
  public double waveMaxAmp;
  public double waveMinAmp;
  public boolean removeBias;
  public boolean useUnits;
  
  // spectra settings
  public boolean spectraLogPower;
  public boolean spectraLogFreq;
  public boolean autoScaleSpectraPower;
  public boolean autoScaleSpectraPowerMemory;
  public double spectraMinPower;
  public double spectraMaxPower;
  public double spectraMinFreq;
  public double spectraMaxFreq;
  
  // spectrogram settings
  public boolean spectrogramLogPower;
  public boolean autoScaleSpectrogramPower;
  public boolean autoScaleSpectrogramPowerMemory;
  public double spectrogramMinPower;
  public double spectrogramMaxPower;
  public double spectrogramMinFreq;
  public double spectrogramMaxFreq;
  public double spectrogramOverlap;
  public double binSize;
  public int nfft;
  public boolean useAlternateSpectrum = false;
  
  // particle motion settings
  public boolean useAlternateOrientationCode = false;
  public String alternateOrientationCode = "Z12";
  
  // filter settings
  public boolean filterOn;
  public Butterworth filter;
  public boolean zeroPhaseShift;
  
  // pick mode and tag mode settings
  public boolean pickEnabled = false;
  public boolean tagEnabled = false;

  private static WaveViewSettings DEFAULT_WAVE_VIEW_SETTINGS;

  static {
    DEFAULT_WAVE_VIEW_SETTINGS = new WaveViewSettings();
    // view option
    DEFAULT_WAVE_VIEW_SETTINGS.viewType = ViewType.WAVE;
    // wave options
    DEFAULT_WAVE_VIEW_SETTINGS.removeBias = true;
    DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmp = true;
    DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmpMemory = true;
    DEFAULT_WAVE_VIEW_SETTINGS.waveMaxAmp = 1000;
    DEFAULT_WAVE_VIEW_SETTINGS.waveMinAmp = -1000;
    DEFAULT_WAVE_VIEW_SETTINGS.useUnits = true;
    // spectra options
    DEFAULT_WAVE_VIEW_SETTINGS.spectraLogPower = true;
    DEFAULT_WAVE_VIEW_SETTINGS.spectraLogFreq = true;
    DEFAULT_WAVE_VIEW_SETTINGS.spectraMinFreq = 0;
    DEFAULT_WAVE_VIEW_SETTINGS.spectraMaxFreq = 25;
    DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectraPower = true;
    DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectraPowerMemory = true;
    DEFAULT_WAVE_VIEW_SETTINGS.spectraMinPower = 1;
    DEFAULT_WAVE_VIEW_SETTINGS.spectraMaxPower = 5;
    // spectrogram options
    DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectrogramPower = false;
    DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectrogramPowerMemory = true;
    DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMinPower = 20;
    DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMaxPower = 120;
    DEFAULT_WAVE_VIEW_SETTINGS.spectrogramOverlap = 0.859375;
    DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMinFreq = 0;
    DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMaxFreq = 25;
    DEFAULT_WAVE_VIEW_SETTINGS.binSize = 2;
    DEFAULT_WAVE_VIEW_SETTINGS.nfft = 0; // Zero means automatic
    DEFAULT_WAVE_VIEW_SETTINGS.spectrogramLogPower = true;
    DEFAULT_WAVE_VIEW_SETTINGS.useAlternateSpectrum = false;
    // particle motion options
    DEFAULT_WAVE_VIEW_SETTINGS.useAlternateOrientationCode = false;
    DEFAULT_WAVE_VIEW_SETTINGS.alternateOrientationCode = "Z12";
    // filter otions
    DEFAULT_WAVE_VIEW_SETTINGS.filter = new Butterworth();
    DEFAULT_WAVE_VIEW_SETTINGS.filterOn = false;
    DEFAULT_WAVE_VIEW_SETTINGS.zeroPhaseShift = true;

    List<String> candidateNames = new LinkedList<String>();
    candidateNames.add(DEFAULTS_FILENAME);
    candidateNames.add(System.getProperty("user.home") + File.separatorChar + DEFAULTS_FILENAME);
    String defaultsFile = ConfigFile.findConfig(candidateNames);
    if (defaultsFile == null) {
      defaultsFile = DEFAULTS_FILENAME;
    }

    ConfigFile cf = new ConfigFile(defaultsFile);
    if (cf.wasSuccessfullyRead()) {
      ConfigFile sub = cf.getSubConfig("default");
      DEFAULT_WAVE_VIEW_SETTINGS.set(sub);
    } else {
      DEFAULT_WAVE_VIEW_SETTINGS.save(cf, "default");
      cf.writeToFile(DEFAULTS_FILENAME);
    }
  }

  /**
   * Default Constructor.
   */
  public WaveViewSettings() {
    filter = new Butterworth();
    view = null;
    if (DEFAULT_WAVE_VIEW_SETTINGS != null) {
      copy(DEFAULT_WAVE_VIEW_SETTINGS);
    }
  }

  public WaveViewSettings(WaveViewSettings s) {
    copy(s);
  }

  /**
   * Deep copy WaveViewSettings.
   * @param s WaveViewSettings.
   */
  public void copy(WaveViewSettings s) {
    viewType = s.viewType;
    
    // wave options
    autoScaleAmp = s.autoScaleAmp;
    autoScaleAmpMemory = s.autoScaleAmpMemory;
    waveMaxAmp = s.waveMaxAmp;
    waveMinAmp = s.waveMinAmp;
    useUnits = s.useUnits;
    removeBias = s.removeBias;
    
    // spectra options
    spectraLogPower = s.spectraLogPower;
    spectraLogFreq = s.spectraLogFreq;
    autoScaleSpectraPowerMemory = s.autoScaleSpectraPowerMemory;
    autoScaleSpectraPower = s.autoScaleSpectraPower;
    spectraMinPower = s.spectraMinPower;
    spectraMaxPower = s.spectraMaxPower;
    spectraMinFreq = s.spectraMinFreq;
    spectraMaxFreq = s.spectraMaxFreq;
    
    // spectrogram options
    autoScaleSpectrogramPowerMemory = s.autoScaleSpectrogramPowerMemory;
    autoScaleSpectrogramPower = s.autoScaleSpectrogramPower;
    spectrogramMinPower = s.spectrogramMinPower;
    spectrogramMaxPower = s.spectrogramMaxPower;
    spectrogramMinFreq = s.spectrogramMinFreq;
    spectrogramMaxFreq = s.spectrogramMaxFreq;
    binSize = s.binSize;
    nfft = s.nfft;
    spectrogramOverlap = s.spectrogramOverlap;
    spectrogramLogPower = s.spectrogramLogPower;
    useAlternateSpectrum = s.useAlternateSpectrum;
    
    // particle motion options
    useAlternateOrientationCode = s.useAlternateOrientationCode;
    alternateOrientationCode = s.alternateOrientationCode;
    
    // filter options
    zeroPhaseShift = s.zeroPhaseShift;
    filterOn = s.filterOn;
    filter = new Butterworth(s.filter);
    
    // pick mode
    pickEnabled = s.pickEnabled;
  }

  /**
   * Set configuration.
   * @param cf Configuration file.
   */
  public void set(ConfigFile cf) {
    viewType = ViewType.fromString(cf.getString("viewType"));
    
    // wave settings
    waveMaxAmp =
        StringUtils.stringToDouble(cf.getString("waveMaxAmp"),
            DEFAULT_WAVE_VIEW_SETTINGS.waveMaxAmp);
    waveMinAmp =
        StringUtils.stringToDouble(cf.getString("waveMinAmp"),
            DEFAULT_WAVE_VIEW_SETTINGS.waveMinAmp);
    autoScaleAmp = StringUtils.stringToBoolean(cf.getString("autoScaleAmp"),
        DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmp);
    autoScaleAmpMemory = StringUtils.stringToBoolean(cf.getString("autoScaleAmpMemory"),
        DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmpMemory);
    removeBias = StringUtils.stringToBoolean(cf.getString("removeBias"),
        DEFAULT_WAVE_VIEW_SETTINGS.removeBias);
    useUnits =
        StringUtils.stringToBoolean(cf.getString("useUnits"), DEFAULT_WAVE_VIEW_SETTINGS.useUnits);
    
    // spectra settings
    spectraLogFreq =
        StringUtils.stringToBoolean(cf.getString("spectraLogFreq"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectraLogFreq);
    spectraLogPower =
        StringUtils.stringToBoolean(cf.getString("spectraLogPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectraLogPower);
    spectraMinFreq =
        StringUtils.stringToDouble(cf.getString("spectraMinFreq"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectraMinFreq);
    spectraMaxFreq =
        StringUtils.stringToDouble(cf.getString("spectraMaxFreq"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectraMaxFreq);
    autoScaleSpectraPower = StringUtils.stringToBoolean(cf.getString("autoScaleSpectraPower"),
        DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectraPower);
    autoScaleSpectraPowerMemory =
        StringUtils.stringToBoolean(cf.getString("autoScaleSpectraPowerMemory"),
            DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectraPowerMemory);
    spectraMaxPower =
        StringUtils.stringToDouble(cf.getString("spectraMaxPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectraMaxPower);
    spectraMinPower =
        StringUtils.stringToDouble(cf.getString("spectraMinPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectraMinPower);
    
    // spectrogram settings
    spectrogramMinFreq =
        StringUtils.stringToDouble(cf.getString("spectrogramMinFreq"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMinFreq);
    spectrogramMaxFreq =
        StringUtils.stringToDouble(cf.getString("spectrogramMaxFreq"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMaxFreq);
    autoScaleSpectrogramPower =
        StringUtils.stringToBoolean(cf.getString("autoScaleSpectrogramPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectrogramPower);
    autoScaleSpectrogramPowerMemory =
        StringUtils.stringToBoolean(cf.getString("autoScaleSpectrogramPowerMemory"),
            DEFAULT_WAVE_VIEW_SETTINGS.autoScaleSpectrogramPowerMemory);
    spectrogramMaxPower =
        StringUtils.stringToDouble(cf.getString("spectrogramMaxPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMaxPower);
    spectrogramMinPower =
        StringUtils.stringToDouble(cf.getString("spectrogramMinPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectrogramMinPower);
    binSize =
        StringUtils.stringToDouble(cf.getString("binSize"), DEFAULT_WAVE_VIEW_SETTINGS.binSize);
    nfft = StringUtils.stringToInt(cf.getString("nfft"), DEFAULT_WAVE_VIEW_SETTINGS.nfft);
    spectrogramOverlap = StringUtils.stringToDouble(cf.getString("spectrogramOverlap"),
        DEFAULT_WAVE_VIEW_SETTINGS.spectrogramOverlap);
    spectrogramLogPower =
        StringUtils.stringToBoolean(cf.getString("spectrogramLogPower"),
            DEFAULT_WAVE_VIEW_SETTINGS.spectrogramLogPower);
    useAlternateSpectrum = 
        StringUtils.stringToBoolean(cf.getString("useAlternateSpectrum"),
            DEFAULT_WAVE_VIEW_SETTINGS.useAlternateSpectrum);
    
    // particle motion settings
    useAlternateOrientationCode =
        StringUtils.stringToBoolean(cf.getString("useAlternateOrientationCode"),
            DEFAULT_WAVE_VIEW_SETTINGS.useAlternateOrientationCode);
    alternateOrientationCode = StringUtils.stringToString(cf.getString("alternateOrientationCode"),
        DEFAULT_WAVE_VIEW_SETTINGS.alternateOrientationCode);
    
    // filter settings
    filterOn =
        StringUtils.stringToBoolean(cf.getString("filterOn"), DEFAULT_WAVE_VIEW_SETTINGS.filterOn);
    filter.set(cf.getSubConfig("filter"));
    zeroPhaseShift = StringUtils.stringToBoolean(cf.getString("zeroPhaseShift"),
        DEFAULT_WAVE_VIEW_SETTINGS.zeroPhaseShift);
  }

  /**
   * Save configuration file.
   * @param cf Configuration file.
   * @param prefix Configuration name prefix.
   */
  public void save(ConfigFile cf, String prefix) {
    cf.put(prefix + ".viewType", viewType.code);
    filter.save(cf, prefix + ".filter");
    // wave settings
    cf.put(prefix + ".autoScaleAmp", Boolean.toString(autoScaleAmp));
    cf.put(prefix + ".autoScaleAmpMemory", Boolean.toString(autoScaleAmpMemory));
    cf.put(prefix + ".waveMaxAmp", Double.toString(waveMaxAmp));
    cf.put(prefix + ".waveMinAmp", Double.toString(waveMinAmp));
    cf.put(prefix + ".removeBias", Boolean.toString(removeBias));
    cf.put(prefix + ".useUnits", Boolean.toString(useUnits));
    // spectra settings
    cf.put(prefix + ".spectraLogFreq", Boolean.toString(spectraLogFreq));
    cf.put(prefix + ".spectraLogPower", Boolean.toString(spectraLogPower));
    cf.put(prefix + ".spectraMinFreq", Double.toString(spectraMinFreq));
    cf.put(prefix + ".spectraMaxFreq", Double.toString(spectraMaxFreq));
    cf.put(prefix + ".autoScaleSpectrogramPower", Boolean.toString(autoScaleSpectrogramPower));
    cf.put(prefix + ".autoScaleSpectrogramPowerMemory",
        Boolean.toString(autoScaleSpectrogramPowerMemory));
    cf.put(prefix + ".spectraMinPower", Double.toString(spectraMinPower));
    cf.put(prefix + ".spectraMaxPower", Double.toString(spectraMaxPower));
    // spectrogram settings
    cf.put(prefix + ".spectrogramMinFreq", Double.toString(spectrogramMinFreq));
    cf.put(prefix + ".spectrogramMaxFreq", Double.toString(spectrogramMaxFreq));
    cf.put(prefix + ".autoScalePower", Boolean.toString(autoScaleSpectrogramPower));
    cf.put(prefix + ".autoScalePowerMemory", Boolean.toString(autoScaleSpectrogramPowerMemory));
    cf.put(prefix + ".spectrogramMinPower", Double.toString(spectrogramMinPower));
    cf.put(prefix + ".spectrogramMaxPower", Double.toString(spectrogramMaxPower));
    cf.put(prefix + ".binSize", Double.toString(binSize));
    cf.put(prefix + ".nfft", Integer.toString(nfft));
    cf.put(prefix + ".spectrogramOverlap", Double.toString(spectrogramOverlap));
    cf.put(prefix + ".spectrogramLogPower", Boolean.toString(spectrogramLogPower));
    cf.put(prefix + ".useAlternateSpectrum", Boolean.toString(useAlternateSpectrum));
    // particle motion settings
    cf.put(prefix + ".useAlternateOrientationCode", Boolean.toString(useAlternateOrientationCode));
    cf.put(prefix + ".alternateOrientationCode", alternateOrientationCode);
    // filter settings
    cf.put(prefix + ".filterOn", Boolean.toString(filterOn));
    cf.put(prefix + ".zeroPhaseShift", Boolean.toString(zeroPhaseShift));
  }

  public void setType(ViewType t) {
    viewType = t;
    notifyView();
  }

  /**
   * Get cycle type based on view type.
   */
  public void cycleType() {
    switch (viewType) {
      case WAVE:
        viewType = ViewType.SPECTRA;
        break;
      case SPECTRA:
        viewType = ViewType.SPECTROGRAM;
        break;
      case SPECTROGRAM:
        viewType = ViewType.PARTICLE_MOTION;
        break;
      case PARTICLE_MOTION:
        viewType = ViewType.WAVE;
        break;
      default:
        break;
    }
    notifyView();
  }

  /**
   * Set cycle log settings.
   */
  public void cycleLogSettings() {

    if (spectraLogFreq == spectraLogPower) {
      spectraLogPower = !spectraLogPower;
    } else {
      spectraLogFreq = !spectraLogFreq;
    }

    notifyView();
  }

  public void toggleSpectraLogFreq() {
    spectraLogFreq = !spectraLogFreq;
    notifyView();
  }

  public void toggleSpectraLogPower() {
    spectraLogPower = !spectraLogPower;
    notifyView();
  }
  
  public void toggleSpectrogramLogPower() {
    spectrogramLogPower = !spectrogramLogPower;
    notifyView();
  }

  public void toggleFilter() {
    filterOn = !filterOn;
    notifyView();
  }

  /**
   * Reset view's auto scale memory setting.
   */
  public void resetAutoScaleMemory() {
    if (view != null) {
      view.resetAutoScaleMemory();
    }
  }

  /**
   * Adjust view's scale.
   * @param pct Scale percent.
   */
  public void adjustScale(double pct) {
    if (view != null) {
      view.adjustScale(pct);
    }
  }

  /**
   * Notify view of settings change.
   */
  public void notifyView() {
    if (view != null) {
      view.settingsChanged();
    }

    if (toolbar != null) {
      toolbar.settingsChanged();
    }
  }

}
