package gov.usgs.swarm.data.file;

import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.SAC;
import gov.usgs.plot.data.Wave;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.DataSourceType;
import gov.usgs.swarm.data.GulperListener;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dan Cervelli
 */
public class SACDataSource extends SeismicDataSource 
{
	private String station;
	private Wave wave;
	private String filename;
	
	public SACDataSource(String fn)
	{
		try
		{
			filename = fn;
			SAC sac = new SAC();
			sac.read(fn);
			station = sac.getStationInfo();
			wave = sac.toWave();

			CachedDataSource cache = CachedDataSource.getInstance();
			cache.cacheWaveAsHelicorder(station, wave);
			cache.putWave(station, wave);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void parse(String params) {
	}
	 
	public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl)
	{
		double dt = t2 - t1;
		double now = CurrentTime.getInstance().nowJ2K();
		if (Math.abs(now - t2) < 3600)
		{
			t2 = wave.getEndTime();
			t1 = t2 - dt;
		}
		
		CachedDataSource cache = CachedDataSource.getInstance();
		return cache.getHelicorder(station, t1, t2, gl);
	}
	
	public List<String> getChannels()
	{
		List<String> list = new ArrayList<String>();
		list.add(station);
		return list;
	}

	public Wave getWave(String station, double t1, double t2) 
	{
		CachedDataSource cache = CachedDataSource.getInstance();
		return cache.getBestWave(station, t1, t2);
	}
	
	public String toConfigString()
	{
		String typeString = DataSourceType.getShortName(this.getClass());
		return name + ";" + typeString + ":" + filename;
	}

}
