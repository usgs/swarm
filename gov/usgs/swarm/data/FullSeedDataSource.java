package gov.usgs.swarm.data;

import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.container.SeedObjectContainer;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.director.ImportDirector;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Class for loading SEED volumes.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2006/04/15 16:00:13  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.3  2005/09/26 15:26:33  dcervelli
 * Fixed bug introduced during change to Java 1.5.
 *
 * Revision 1.2  2005/09/02 16:40:29  dcervelli
 * CurrentTime changes.
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.1  2005/05/02 16:22:10  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.2  2004/10/23 19:35:20  cvs
 * No longer returns wave list.
 *
 * @author Dan Cervelli
 */
public class FullSeedDataSource extends SeismicDataSource
{
	private List<String> stations;
	private static Map<String, Wave> stationMap; 

	private static boolean read = false;
	
	private String filename;
	
	public FullSeedDataSource(String fn)
	{
		if (read)
			return;
		filename = fn;
		stations = new ArrayList<String>();
		try
		{
			Map<String, List<Wave>> tempStationMap = new HashMap<String, List<Wave>>();
			stationMap = new HashMap<String, Wave>();
			
			DataInputStream ls = new DataInputStream(new BufferedInputStream(
	                new FileInputStream(fn)));
			
			ImportDirector importDirector = new SeedImportDirector();
			SeedObjectBuilder objectBuilder = new SeedObjectBuilder();
			importDirector.assignBuilder(objectBuilder);  // register the builder with the director
			// begin reading the stream with the construct command
			importDirector.construct(ls);  // construct SEED objects silently
			SeedObjectContainer container = (SeedObjectContainer)importDirector.getBuilder().getContainer();
			
			Object object;
			container.iterate();
			while ((object = container.getNext()) != null)
			{
				Blockette b = (Blockette)object;
				if (b.getType() != 999)
					continue;
				String code = b.getFieldVal(4) + "_" + b.getFieldVal(6) + "_" + b.getFieldVal(7);
				
				List<Wave> parts = tempStationMap.get(code);
                if (parts == null)
                {
                	parts = new ArrayList<Wave>();
                	tempStationMap.put(code, parts);
                }
                
                if (b.getWaveform() != null)
                {
	                Waveform wf = b.getWaveform();
                	Wave sw = new Wave();
	                sw.setSamplingRate(getSampleRate(((Integer)b.getFieldVal(10)).intValue(), ((Integer)b.getFieldVal(11)).intValue()));
	                Btime bTime = (Btime)b.getFieldVal(8);
	                sw.setStartTime(Util.dateToJ2K(btimeToDate(bTime)));
	                sw.buffer = wf.getDecodedIntegers();
	                sw.register();
	                parts.add(sw);
                }
			}
			for (String key : tempStationMap.keySet())
			{
				List<Wave> parts = tempStationMap.get(key);
				Wave wave = Wave.join(parts);
				Swarm.getCache().cacheWaveAsHelicorder(key, wave);
				Swarm.getCache().putWave(key, wave);
				stationMap.put(key, wave);
				stations.add(key);
			}
			read = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	 
	private Date btimeToDate(Btime bt)
	{
    	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    	cal.set(Calendar.YEAR, bt.getYear());
    	cal.set(Calendar.DAY_OF_YEAR, bt.getDayOfYear());
    	cal.set(Calendar.HOUR_OF_DAY, bt.getHour());
    	cal.set(Calendar.MINUTE, bt.getMinute());
    	cal.set(Calendar.SECOND, bt.getSecond());
    	cal.set(Calendar.MILLISECOND, bt.getTenthMill() / 10);
    	return cal.getTime();
	}
	
	private float getSampleRate (double factor, double multiplier) {
        float sampleRate = (float) 10000.0;  // default (impossible) value;
        if ((factor * multiplier) != 0.0) {  // in the case of log records
            sampleRate = (float) (java.lang.Math.pow
                                      (java.lang.Math.abs(factor),
                                           (factor/java.lang.Math.abs(factor)))
                                      * java.lang.Math.pow
                                      (java.lang.Math.abs(multiplier),
                                           (multiplier/java.lang.Math.abs(multiplier))));
        }
        return sampleRate;
    }
	
	public HelicorderData getHelicorder(String station, double t1, double t2)
	{
		double dt = t2 - t1;
		double now = CurrentTime.getInstance().nowJ2K();
		if (Math.abs(now - t2) < 3600)
		{
			Wave wave = stationMap.get(station);
			if (wave == null)
				return null;
			
			t2 = wave.getEndTime();
			t1 = t2 - dt;
		}
		return Swarm.getCache().getHelicorder(station, t1, t2);
	}
	
	public List<String> getChannels()
	{
		return stations;
	}

	public Wave getWave(String station, double t1, double t2) 
	{
		return Swarm.getCache().getBestWave(station, t1, t2);
	}
	
	public String toConfigString()
	{
		return name + ";seed:" + filename;
	}
}
