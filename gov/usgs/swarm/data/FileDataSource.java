package gov.usgs.swarm.data;

import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.container.SeedObjectContainer;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.director.ImportDirector;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.Wave;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * $Log: not supported by cvs2svn $ 
 * @author Dan Cervelli
 */
public class FileDataSource extends CachedDataSource
{
	private Map<String, double[]> channelTimes;
	private Set<String> openFiles;
	
	public FileDataSource()
	{
		super();
		channelTimes = new HashMap<String, double[]>();
		openFiles = new HashSet<String>();
		maxSize = Integer.MAX_VALUE;
		storeInUserConfig = false;
		name = "Files";
	}
	
	public void flush()
	{
		super.flush();
		openFiles.clear();
		channelTimes.clear();
		fireChannelsUpdated();
	}
	
	private void updateChannelTimes(String channel, double t1, double t2)
	{
		double[] ct = channelTimes.get(channel);
		if (ct == null)
		{
			ct = new double[] { t1, t2 };
			channelTimes.put(channel, ct);
		}
		ct[0] = Math.min(ct[0], t1);
		ct[1] = Math.max(ct[1], t2);
	}
	
	private enum FileType 
	{ 
		TEXT, SAC, SEED, UNKNOWN;
		
		public static FileType fromFile(File f)
		{
			if (f.getPath().endsWith(".sac"))
				return SAC;
			else if (f.getPath().endsWith(".txt"))
				return TEXT;
			else if (f.getPath().endsWith(".seed"))
				return SEED;
			else
				return UNKNOWN;
		}
	}
	
	public void openFiles(File[] fs)
	{
		for (int i = 0; i < fs.length; i++)
		{
			FileType ft = FileType.fromFile(fs[i]);
			switch (ft)
			{
				case SAC:
					openSACFile(fs[i].getPath());
					break;
				case SEED:
					openSeedFile(fs[i].getPath());
					break;
				case UNKNOWN:
					Swarm.logger.warning("unknown file type: " + fs[i].getPath());
					break;
			}
		}
	}
	
	public void openSACFile(String fn)
	{
		if (openFiles.contains(fn))
			return;
		try
		{
			Swarm.logger.fine("opening SAC file: " + fn);
			SAC sac = new SAC();
			sac.read(fn);
			String channel = sac.getStationInfo();
			Metadata md = Swarm.config.getMetadata(channel, true);
			md.addGroup("SAC^" + fn);
			
			Wave wave = sac.toWave();
			updateChannelTimes(channel, wave.getStartTime(), wave.getEndTime());
			cacheWaveAsHelicorder(channel, wave);
			putWave(channel, wave);
			fireChannelsUpdated();
			openFiles.add(fn);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// TODO: can optimize memory usage further by added combined parts as you go.
	public void openSeedFile(String fn)
	{
		if (openFiles.contains(fn))
			return;
		try
		{
			Swarm.logger.fine("opening SEED file: " + fn);
			Map<String, List<Wave>> tempStationMap = new HashMap<String, List<Wave>>();
			
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
			CodeTimer ct = new CodeTimer("seed");
			while ((object = container.getNext()) != null)
			{
				Blockette b = (Blockette)object;
				if (b.getType() != 999)
					continue;
				String code = b.getFieldVal(4) + "_" + b.getFieldVal(6) + "_" + b.getFieldVal(7);
				Metadata md = Swarm.config.getMetadata(code, true);
				md.addGroup("SEED^" + fn);
				
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
//	                System.out.println(sw);
	                parts.add(sw);
                }
			}
			ct.mark("read");
			for (String code : tempStationMap.keySet())
			{
				List<Wave> parts = tempStationMap.get(code);
				ArrayList<Wave> subParts = new ArrayList<Wave>();
				int ns = 0;
				int sp = 0;
				for (int i = 0; i < parts.size(); i++)
				{
					ns += parts.get(i).samples();
					subParts.add(parts.get(i));
					if (ns > 3600 * 100 || i == parts.size() - 1)
					{
						sp++;
						Wave wave = Wave.join(subParts);
						updateChannelTimes(code, wave.getStartTime(), wave.getEndTime());
		                cacheWaveAsHelicorder(code, wave);
						putWave(code, wave);
						ns = 0;
						subParts.clear();
					}
				}	
			}
			ct.mark("insert");
			ct.stop();
			fireChannelsUpdated();
			openFiles.add(fn);
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
	
	public HelicorderData getHelicorder(String channel, double t1, double t2, GulperListener gl)
	{
		double[] ct = channelTimes.get(channel);
		if (ct == null)
			return null;
		
		double dt = t2 - t1;
		double now = CurrentTime.getInstance().nowJ2K();
		if (Math.abs(now - t2) < 3600)
		{
			t2 = ct[1];
			t1 = t2 - dt;
		}
		return super.getHelicorder(channel, t1, t2, gl);
	}
	
	public Wave getWave(String station, double t1, double t2) 
	{
		return super.getBestWave(station, t1, t2);
	}
	
	public String toConfigString()
	{
		return name + ";file:";
	}
}
