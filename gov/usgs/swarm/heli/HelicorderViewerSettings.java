package gov.usgs.swarm.heli;

import gov.usgs.swarm.SwarmConfig;
import gov.usgs.util.ConfigFile;

/**
 * Settings for a helicorder.
 * 
 * TODO: eliminate this class in favor of vdx.HelicorderSettings
 *
 * @author Dan Cervelli
 */
public class HelicorderViewerSettings
{
	public String channel;
	public int timeChunk; // seconds
	public int span; // minutes
	public int waveZoomOffset; // seconds
	private double bottomTime; // Double.NaN for now
	public int refreshInterval;
	public int scrollSize;
	public boolean forceCenter;
	public int clipBars;
	private long lastBottomTimeSet;
	
	public boolean autoScale;
	public boolean showClip;
	public boolean alertClip;
	public int alertClipTimeout;
	public int clipValue;
	public int barRange;
	public double barMult;
	public HelicorderViewPanel view;
	
	private static SwarmConfig swarmConfig;
	
	public HelicorderViewerSettings(String ch)
	{
		swarmConfig = SwarmConfig.getInstance();
		
		channel = ch;
		timeChunk = swarmConfig.timeChunk * 60;
		span = swarmConfig.span * 60;
		waveZoomOffset = 30;
		bottomTime = Double.NaN;
		refreshInterval = 15;
		scrollSize = 24;
		forceCenter = false;
		clipBars = 21;
		
		clipValue = 2999;
		showClip = swarmConfig.showClip; 
		alertClip = swarmConfig.alertClip; 
		alertClipTimeout = swarmConfig.alertClipTimeout * 60;
		barRange = 1500;
		barMult = 3;
		autoScale = true;
	}
	
	public long getLastBottomTimeSet()
	{
		return System.currentTimeMillis() - lastBottomTimeSet;
	}
	
	public void setBottomTime(double bt)
	{
		lastBottomTimeSet = System.currentTimeMillis();
		bottomTime = bt;
	}
	
	public double getBottomTime()
	{
		return bottomTime;
	}
	
	public void set(ConfigFile cf)
	{
		timeChunk = Integer.parseInt(cf.getString("timeChunk"));
		span = Integer.parseInt(cf.getString("span"));
		waveZoomOffset = Integer.parseInt(cf.getString("waveZoomOffset"));
		refreshInterval = Integer.parseInt(cf.getString("refreshInterval"));
		scrollSize = Integer.parseInt(cf.getString("scrollSize"));
		clipValue = Integer.parseInt(cf.getString("clipValue"));
		clipBars = Integer.parseInt(cf.getString("clipBars"));
		barRange = Integer.parseInt(cf.getString("barRange"));
		alertClipTimeout = Integer.parseInt(cf.getString("alertClipTimeout"));
		setBottomTime(Double.parseDouble(cf.getString("bottomTime")));
		barMult = Double.parseDouble(cf.getString("barMult"));
		forceCenter = Boolean.parseBoolean(cf.getString("forceCenter"));
		autoScale = Boolean.parseBoolean(cf.getString("autoScale"));
		showClip = Boolean.parseBoolean(cf.getString("showClip"));
		alertClip = Boolean.parseBoolean(cf.getString("alertClip"));
	}
	
	public void save(ConfigFile cf, String prefix)
	{
		cf.put(prefix + ".channel", channel);
		cf.put(prefix + ".timeChunk", Integer.toString(timeChunk));
		cf.put(prefix + ".span", Integer.toString(span));
		cf.put(prefix + ".waveZoomOffset", Integer.toString(waveZoomOffset));
		cf.put(prefix + ".refreshInterval", Integer.toString(refreshInterval));
		cf.put(prefix + ".scrollSize", Integer.toString(scrollSize));
		cf.put(prefix + ".clipValue", Integer.toString(clipValue));
		cf.put(prefix + ".clipBars", Integer.toString(clipBars));
		cf.put(prefix + ".barRange", Integer.toString(barRange));
		cf.put(prefix + ".alertClipTimeout", Integer.toString(alertClipTimeout));
		cf.put(prefix + ".bottomTime", Double.toString(bottomTime));
		cf.put(prefix + ".barMult", Double.toString(barMult));
		cf.put(prefix + ".forceCenter", Boolean.toString(forceCenter));
		cf.put(prefix + ".autoScale", Boolean.toString(autoScale));
		cf.put(prefix + ".showClip", Boolean.toString(showClip));
		cf.put(prefix + ".alertClip", Boolean.toString(alertClip));
	}
	
	public void parseSettingsString(String o)
	{
//		String[] opts = Util.splitString(o, ",");
		String[] opts = o.split(",");
		for (int i = 0; i < opts.length; i++)
		{
			try
			{
				String key = opts[i].substring(0, opts[i].indexOf('='));
				String value = opts[i].substring(opts[i].indexOf('=') + 1);
				
				if (key.equals("x")) // minutes
					timeChunk = Integer.parseInt(value) * 60;
				else if (key.equals("y")) // hours
					span =  Integer.parseInt(value) * 60;
			}
			catch (Exception e)
			{
				System.err.println("Could not parse setting: " + opts[i]);
			}
		}
	}
	
	public void notifyView()
	{
		if (view != null)
			view.settingsChanged();	
	}
}