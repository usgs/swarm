package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.Wave;

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
	
	//private static List loadedList = new ArrayList();
	
	public SACDataSource(String fn)
	{
		try
		{
			filename = fn;
//			if (loadedList.contains(fn))
//				return;
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
		return name + ";sac:" + filename;
	}
	
}
