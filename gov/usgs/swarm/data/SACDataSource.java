package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.Wave;

import java.util.ArrayList;
import java.util.List;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2006/04/15 16:00:13  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.2  2005/09/02 16:40:29  dcervelli
 * CurrentTime changes.
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.1  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.1  2004/10/23 19:34:46  cvs
 * Added support for SAC files.
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
			Swarm.getCache().cacheWaveAsHelicorder(station, wave);
			Swarm.getCache().putWave(station, wave);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	 
	public HelicorderData getHelicorder(String station, double t1, double t2)
	{
		double dt = t2 - t1;
		double now = CurrentTime.getInstance().nowJ2K();
		if (Math.abs(now - t2) < 3600)
		{
			t2 = wave.getEndTime();
			t1 = t2 - dt;
		}
		return Swarm.getCache().getHelicorder(station, t1, t2);
	}
	
	public List<String> getChannels()
	{
		List<String> list = new ArrayList<String>();
		list.add(station);
		return list;
	}

	public Wave getWave(String station, double t1, double t2) 
	{
		return Swarm.getCache().getBestWave(station, t1, t2);
	}
	
	public String toConfigString()
	{
		return name + ";sac:" + filename;
	}
	
}
