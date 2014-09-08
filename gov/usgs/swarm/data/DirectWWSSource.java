package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.UtilException;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.winston.Channel;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.Data;
import gov.usgs.winston.db.WinstonDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of <code>SeismicDataSource</code> that communicates
 * directly with a Winston database.  Essentially identical to 
 * DirectWinstonSource.
 *
 *
 * @author Dan Cervelli
 */
public class DirectWWSSource extends SeismicDataSource
{
	private String dbDriver;
	private String dbURL;
	private String dbPrefix;
	
	private WinstonDatabase winston;
	private Data data;
	private Channels stations;
	
	public DirectWWSSource(String d, String u, String db)
	{
		dbDriver = d;
		dbURL = u;
		dbPrefix = db;
		winston = new WinstonDatabase(dbDriver, dbURL, dbPrefix);
		stations = new Channels(winston);
		data = new Data(winston);
	}
	
	public void close()
	{
		winston.close();
	}
	
	public synchronized Wave getWave(String station, double t1, double t2)
	{
		CachedDataSource cache = CachedDataSource.getInstance();
		
		Wave sw = cache.getWave(station, t1, t2);
		if (sw == null)
		{
			try{
				sw = data.getWave(station, t1, t2, 0);
			} catch (UtilException e){
			}
			if (sw != null && !sw.isData())
				sw = null;
			if (sw != null && sw.buffer != null && sw.buffer.length > 0)
				cache.putWave(station, sw);
		}
		return sw;	
	}
	
	public synchronized List<String> getChannels()
	{
		List<Channel> chs = stations.getChannels();
		List<String> result = new ArrayList<String>();
		for (Channel ch : chs)
			result.add(ch.toString());
		return result;
	}
	
	public synchronized HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl)
	{
		CachedDataSource cache = CachedDataSource.getInstance();
		HelicorderData hd = cache.getHelicorder(station, t1, t2, this);
		if (hd == null)
		{
			try{
				hd = data.getHelicorderData(station, t1, t2, 0);
			} catch (UtilException e){
			}
			if (hd != null && hd.rows() != 0)
				cache.putHelicorder(station, hd);
			else
				hd = null;
		}
		return hd;
	}
	
	public String toConfigString()
	{
		return "winston:";
	}
	
}