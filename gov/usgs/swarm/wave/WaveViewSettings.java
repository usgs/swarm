package gov.usgs.swarm.wave;

import gov.usgs.math.Butterworth;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;
 
/**
 *
 * @author Dan Cervelli
 */

public class WaveViewSettings
{
	public static final String DEFAULTS_FILENAME = "WaveDefaults.config";
	
	public enum ViewType
	{
		WAVE("W"),
		SPECTRA("S"),
		SPECTROGRAM("G");
		
		public String code;
		
		private ViewType(String c)
		{
			code = c;
		}
		
		public static ViewType fromString(String c)
		{
			if (c.equals("S"))
				return SPECTRA;
			else if (c.equals("G"))
				return SPECTROGRAM;
			else
				return WAVE;
		}
	}
		
	public boolean filterOn;
	public boolean zeroPhaseShift;
	public boolean autoScaleAmp;
	public boolean autoScaleAmpMemory;
	public boolean autoScalePower;
	public boolean autoScalePowerMemory;
	public boolean useUnits;
	public boolean logPower;
	public boolean logFreq;
	public boolean removeBias;
	
	public double maxAmp;
	public double minAmp;
	public double minPower;
	public double maxPower;
	public double minFreq;
	public double maxFreq;
	public double spectrogramOverlap;
	public double binSize;
	
	public int nfft;
	
	public WaveViewPanel view;
	public WaveViewSettingsToolbar toolbar;
	public ViewType viewType;
	public Butterworth filter;
	
	private static WaveViewSettings DEFAULT_WAVE_VIEW_SETTINGS;
	
	static
	{
		DEFAULT_WAVE_VIEW_SETTINGS = new WaveViewSettings();
		DEFAULT_WAVE_VIEW_SETTINGS.viewType = ViewType.WAVE;
		DEFAULT_WAVE_VIEW_SETTINGS.removeBias = true;
		DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmp = true;
		DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmpMemory = true;
		DEFAULT_WAVE_VIEW_SETTINGS.maxAmp = 1000;
		DEFAULT_WAVE_VIEW_SETTINGS.minAmp = -1000;
		DEFAULT_WAVE_VIEW_SETTINGS.autoScalePower = false;
		DEFAULT_WAVE_VIEW_SETTINGS.autoScalePowerMemory = true;
		DEFAULT_WAVE_VIEW_SETTINGS.minPower = 20;
		DEFAULT_WAVE_VIEW_SETTINGS.maxPower = 120;
		DEFAULT_WAVE_VIEW_SETTINGS.useUnits = true;
		DEFAULT_WAVE_VIEW_SETTINGS.logPower = true;
		DEFAULT_WAVE_VIEW_SETTINGS.logFreq = true;
		DEFAULT_WAVE_VIEW_SETTINGS.spectrogramOverlap = 0.859375;
		DEFAULT_WAVE_VIEW_SETTINGS.minFreq = 0;
		DEFAULT_WAVE_VIEW_SETTINGS.maxFreq = 25;
		DEFAULT_WAVE_VIEW_SETTINGS.binSize = 2;
		DEFAULT_WAVE_VIEW_SETTINGS.nfft = 0; // Zero means automatic
		DEFAULT_WAVE_VIEW_SETTINGS.filter = new Butterworth();
		DEFAULT_WAVE_VIEW_SETTINGS.filterOn = false;
		DEFAULT_WAVE_VIEW_SETTINGS.zeroPhaseShift = true;
		
		ConfigFile cf = new ConfigFile(DEFAULTS_FILENAME);
		if (cf.wasSuccessfullyRead())
		{
			ConfigFile sub = cf.getSubConfig("default");
			DEFAULT_WAVE_VIEW_SETTINGS.set(sub);
		} else {
			DEFAULT_WAVE_VIEW_SETTINGS.save(cf, "default");
			cf.writeToFile(DEFAULTS_FILENAME);
		}
	}
	
	public WaveViewSettings()
	{
		filter = new Butterworth();
		view = null;
		if (DEFAULT_WAVE_VIEW_SETTINGS != null)
			copy(DEFAULT_WAVE_VIEW_SETTINGS);
	}
	
	public WaveViewSettings(WaveViewSettings s)
	{
		copy(s);
	}
	
	public void copy(WaveViewSettings s)
	{
		viewType = s.viewType;
		removeBias = s.removeBias;
		autoScaleAmp = s.autoScaleAmp;
		autoScaleAmpMemory = s.autoScaleAmpMemory;
		maxAmp = s.maxAmp;
		minAmp = s.minAmp;
		autoScalePowerMemory = s.autoScalePowerMemory;
		autoScalePower = s.autoScalePower;
		minPower = s.minPower;
		maxPower = s.maxPower;
		filter = new Butterworth(s.filter);
		useUnits = s.useUnits;
		minFreq = s.minFreq;
		maxFreq = s.maxFreq;
		binSize = s.binSize;
		nfft = s.nfft;
		spectrogramOverlap = s.spectrogramOverlap;
		logPower = s.logPower;
		logFreq = s.logFreq;
		zeroPhaseShift = s.zeroPhaseShift;
		filterOn = s.filterOn;	
	}
	
	public void set(ConfigFile cf)
	{
		viewType = ViewType.fromString(cf.getString("viewType"));
		filter.set(cf.getSubConfig("filter"));
		maxAmp = Util.stringToDouble(cf.getString("maxAmp"), DEFAULT_WAVE_VIEW_SETTINGS.maxAmp);
		minAmp = Util.stringToDouble(cf.getString("minAmp"), DEFAULT_WAVE_VIEW_SETTINGS.minAmp);
		maxPower = Util.stringToDouble(cf.getString("maxPower"), DEFAULT_WAVE_VIEW_SETTINGS.maxPower);
		minPower = Util.stringToDouble(cf.getString("minPower"), DEFAULT_WAVE_VIEW_SETTINGS.minPower);
		minFreq = Util.stringToDouble(cf.getString("minFreq"), DEFAULT_WAVE_VIEW_SETTINGS.minFreq);
		maxFreq = Util.stringToDouble(cf.getString("maxFreq"), DEFAULT_WAVE_VIEW_SETTINGS.maxFreq);
		spectrogramOverlap = Util.stringToDouble(cf.getString("spectrogramOverlap"), DEFAULT_WAVE_VIEW_SETTINGS.spectrogramOverlap);
		
		removeBias = Util.stringToBoolean(cf.getString("removeBias"), DEFAULT_WAVE_VIEW_SETTINGS.removeBias);
		filterOn = Util.stringToBoolean(cf.getString("filterOn"), DEFAULT_WAVE_VIEW_SETTINGS.filterOn);
		zeroPhaseShift = Util.stringToBoolean(cf.getString("zeroPhaseShift"), DEFAULT_WAVE_VIEW_SETTINGS.zeroPhaseShift);
		autoScaleAmp = Util.stringToBoolean(cf.getString("autoScaleAmp"), DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmp);
		autoScaleAmpMemory = Util.stringToBoolean(cf.getString("autoScaleAmpMemory"), DEFAULT_WAVE_VIEW_SETTINGS.autoScaleAmpMemory);
		autoScalePower = Util.stringToBoolean(cf.getString("autoScalePower"), DEFAULT_WAVE_VIEW_SETTINGS.autoScalePower);
		autoScalePowerMemory = Util.stringToBoolean(cf.getString("autoScalePowerMemory"), DEFAULT_WAVE_VIEW_SETTINGS.autoScalePowerMemory);
		useUnits = Util.stringToBoolean(cf.getString("useUnits"), DEFAULT_WAVE_VIEW_SETTINGS.useUnits);
		logFreq = Util.stringToBoolean(cf.getString("logFreq"), DEFAULT_WAVE_VIEW_SETTINGS.logFreq);
		logPower = Util.stringToBoolean(cf.getString("logPower"), DEFAULT_WAVE_VIEW_SETTINGS.logPower);
		binSize = Util.stringToDouble(cf.getString("binSize"), DEFAULT_WAVE_VIEW_SETTINGS.binSize);
		nfft = Util.stringToInt(cf.getString("nfft"), DEFAULT_WAVE_VIEW_SETTINGS.nfft);
	}
	
	public void save(ConfigFile cf, String prefix)
	{
		cf.put(prefix + ".viewType", viewType.code);
		filter.save(cf, prefix + ".filter");
		cf.put(prefix + ".maxAmp", Double.toString(maxAmp));
		cf.put(prefix + ".minAmp", Double.toString(minAmp));
		cf.put(prefix + ".minPower", Double.toString(minPower));
		cf.put(prefix + ".maxPower", Double.toString(maxPower));
		cf.put(prefix + ".minFreq", Double.toString(minFreq));
		cf.put(prefix + ".maxFreq", Double.toString(maxFreq));
		cf.put(prefix + ".spectrogramOverlap", Double.toString(spectrogramOverlap));
		cf.put(prefix + ".removeBias", Boolean.toString(removeBias));
		cf.put(prefix + ".filterOn", Boolean.toString(filterOn));
		cf.put(prefix + ".zeroPhaseShift", Boolean.toString(zeroPhaseShift));
		cf.put(prefix + ".autoScaleAmp", Boolean.toString(autoScaleAmp));
		cf.put(prefix + ".autoScaleAmpMemory", Boolean.toString(autoScaleAmpMemory));
		cf.put(prefix + ".autoScalePower", Boolean.toString(autoScalePower));
		cf.put(prefix + ".autoScalePowerMemory", Boolean.toString(autoScalePowerMemory));
		cf.put(prefix + ".useUnits", Boolean.toString(useUnits));
		cf.put(prefix + ".logFreq", Boolean.toString(logFreq));
		cf.put(prefix + ".logPower", Boolean.toString(logPower));
		cf.put(prefix + ".binSize", Double.toString(binSize));
		cf.put(prefix + ".nfft", Integer.toString(nfft));
	}
	
	public void setType(ViewType t)
	{
		viewType = t;
		notifyView();	
	}
	
	public void cycleType()
	{
		switch (viewType)
		{
			case WAVE:
				viewType = ViewType.SPECTRA;
				break;
			case SPECTRA:
				viewType = ViewType.SPECTROGRAM;
				break;
			case SPECTROGRAM:
				viewType = ViewType.WAVE;
				break;
		}
		notifyView();	
	}

	public void cycleLogSettings()
	{
		
		if (logFreq == logPower)
			logPower = !logPower;
		else
			logFreq = !logFreq;
		
		notifyView();
	}
	
	public void toggleLogFreq()
	{
		logFreq = !logFreq;
		notifyView();	
	}
	
	public void toggleLogPower()
	{
		logPower = !logPower;
		notifyView();
	}
	
	public void toggleFilter()
	{
		filterOn = !filterOn;
		notifyView();	
	}
	
	public void resetAutoScaleMemory()
	{
		if (view != null)
			view.resetAutoScaleMemory();
	}
	
	public void adjustScale(double pct)
	{
		if (view != null)
			view.adjustScale(pct);
	}
	
	public void notifyView()
	{
		if (view != null)
			view.settingsChanged();	
			
		if (toolbar != null)
			toolbar.settingsChanged();
	}
	
}