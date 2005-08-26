package gov.usgs.swarm;

import java.awt.*;
import gov.usgs.math.*;
 
public class WaveViewSettings
{
	public static final int WAVE = 1;
	public static final int SPECTRA = 2;
	public static final int SPECTROGRAM = 3;
	public int type;
	
	public Butterworth filter;
	public boolean filterOn;
	public boolean zeroPhaseShift;
	
	public Color color;
	public Color clipColor;
	
	public boolean autoScaleAmp;
	public boolean autoScaleAmpMemory;
	public double maxAmp;
	public double minAmp;
	public boolean autoScalePower;
	public boolean autoScalePowerMemory;
	public double maxPower;
	
	public boolean removeBias;
	
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
		type = WAVE;
		removeBias = true;
		autoScaleAmp = true;
		autoScaleAmpMemory = true;
		maxAmp = 1000;
		minAmp = -1000;
		autoScalePower = true;
		autoScalePowerMemory = true;
		maxPower = 40000;
		logFreq = false;
		logPower = false;
		spectrogramOverlap = 0.2;
		minFreq = 0.75;
		maxFreq = 25;
		fftSize = "Auto";
		color = Color.blue;
		clipColor = Color.red;
		filter = new Butterworth();
		filterOn = false;
		zeroPhaseShift = true;
		view = null;
	}
	
	public WaveViewSettings(WaveViewSettings s)
	{
		type = s.type;
		removeBias = s.removeBias;
		autoScaleAmp = s.autoScaleAmp;
		autoScaleAmpMemory = s.autoScaleAmpMemory;
		maxAmp = s.maxAmp;
		minAmp = s.minAmp;
		autoScalePowerMemory = s.autoScalePowerMemory;
		autoScalePower = s.autoScalePower;
		maxPower = s.maxPower;
		color = s.color;
		clipColor = s.clipColor;
		filter = new Butterworth(s.filter);
		logFreq = s.logFreq;
		minFreq = s.minFreq;
		maxFreq = s.maxFreq;
		fftSize = s.fftSize;
		spectrogramOverlap = s.spectrogramOverlap;
		logPower = s.logPower;
		zeroPhaseShift = s.zeroPhaseShift;
		filterOn = s.filterOn;
	}
	
	public void setType(int t)
	{
		type = t;
		notifyView();	
	}
	
	public void cycleType()
	{
		type++;
		if (type > SPECTROGRAM)
			type = WAVE;
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
	
	public void notifyView()
	{
		if (view != null)
			view.settingsChanged();	
			
		if (toolbar != null)
			toolbar.settingsChanged();
	}
	
}