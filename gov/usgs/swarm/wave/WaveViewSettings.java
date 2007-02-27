package gov.usgs.swarm.wave;

import gov.usgs.math.Butterworth;
import gov.usgs.util.ConfigFile;
 
/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/10/26 00:57:09  dcervelli
 * Function for manually adjusting scales.
 *
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.2  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * @author Dan Cervelli
 */
public class WaveViewSettings
{
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
	
	public ViewType viewType;
	
	public Butterworth filter;
	public boolean filterOn;
	public boolean zeroPhaseShift;
	
//	public Color color;
//	public Color clipColor;
	
	public boolean autoScaleAmp;
	public boolean autoScaleAmpMemory;
	public double maxAmp;
	public double minAmp;
	public boolean autoScalePower;
	public boolean autoScalePowerMemory;
	public double maxPower;
	
	public boolean removeBias;
	
	public boolean useUnits;
	public boolean logFreq;
	public boolean logPower;
	public double minFreq;
	public double maxFreq;
	public String fftSize;
	public double spectrogramOverlap;
	
	public WaveViewPanel view;
	public WaveViewSettingsToolbar toolbar;
	
	public WaveViewSettings()
	{
		viewType = ViewType.WAVE;
		removeBias = true;
		autoScaleAmp = true;
		autoScaleAmpMemory = true;
		maxAmp = 1000;
		minAmp = -1000;
		autoScalePower = true;
		autoScalePowerMemory = true;
		maxPower = 40000;
		useUnits = true;
		logFreq = false;
		logPower = true;
		spectrogramOverlap = 0.2;
		minFreq = 0.75;
		maxFreq = 25;
		fftSize = "Auto";
//		color = Color.blue;
//		clipColor = Color.red;
		filter = new Butterworth();
		filterOn = false;
		zeroPhaseShift = true;
		view = null;
	}
	
	public WaveViewSettings(WaveViewSettings s)
	{
		viewType = s.viewType;
		removeBias = s.removeBias;
		autoScaleAmp = s.autoScaleAmp;
		autoScaleAmpMemory = s.autoScaleAmpMemory;
		maxAmp = s.maxAmp;
		minAmp = s.minAmp;
		autoScalePowerMemory = s.autoScalePowerMemory;
		autoScalePower = s.autoScalePower;
		maxPower = s.maxPower;
//		color = s.color;
//		clipColor = s.clipColor;
		filter = new Butterworth(s.filter);
		useUnits = s.useUnits;
		logFreq = s.logFreq;
		minFreq = s.minFreq;
		maxFreq = s.maxFreq;
		fftSize = s.fftSize;
		spectrogramOverlap = s.spectrogramOverlap;
		logPower = s.logPower;
		zeroPhaseShift = s.zeroPhaseShift;
		filterOn = s.filterOn;
	}
	
	public void set(ConfigFile cf)
	{
//		public ViewType viewType;
		viewType = ViewType.fromString(cf.getString("viewType"));
		filter.set(cf.getSubConfig("filter"));
//		public Butterworth filter;
		maxAmp = Double.parseDouble(cf.getString("maxAmp"));
		minAmp = Double.parseDouble(cf.getString("minAmp"));
		maxPower = Double.parseDouble(cf.getString("maxPower"));
		minFreq = Double.parseDouble(cf.getString("minFreq"));
		maxFreq = Double.parseDouble(cf.getString("maxFreq"));
		spectrogramOverlap = Double.parseDouble(cf.getString("spectrogramOverlap"));
		
		removeBias = Boolean.parseBoolean(cf.getString("removeBias"));
		filterOn = Boolean.parseBoolean(cf.getString("filterOn"));
		zeroPhaseShift = Boolean.parseBoolean(cf.getString("zeroPhaseShift"));
		autoScaleAmp = Boolean.parseBoolean(cf.getString("autoScaleAmp"));
		autoScaleAmpMemory = Boolean.parseBoolean(cf.getString("autoScaleAmpMemory"));
		autoScalePower = Boolean.parseBoolean(cf.getString("autoScalePower"));
		autoScalePowerMemory = Boolean.parseBoolean(cf.getString("autoScalePowerMemory"));
		useUnits = Boolean.parseBoolean(cf.getString("useUnits"));
		logFreq = Boolean.parseBoolean(cf.getString("logFreq"));
		logPower = Boolean.parseBoolean(cf.getString("logPower"));
		fftSize = cf.getString("fftSize");
	}
	
	public void save(ConfigFile cf, String prefix)
	{
		cf.put(prefix + ".viewType", viewType.code);
		filter.save(cf, prefix + ".filter");
		cf.put(prefix + ".maxAmp", Double.toString(maxAmp));
		cf.put(prefix + ".minAmp", Double.toString(minAmp));
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
		cf.put(prefix + ".fftSize", fftSize);
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
		if (logFreq && logPower)
		{
			logFreq = false;
			logPower = false;	
		}
		else if (logFreq)
		{
			logFreq = true;
			logPower = true;
		}
		else if (logPower)
		{
			logFreq = true;
			logPower = false;	
		}
		else
		{
			logPower = true;	
		}
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