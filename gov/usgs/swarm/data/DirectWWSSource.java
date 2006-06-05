package gov.usgs.swarm.data;

import gov.usgs.swarm.Swarm;
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
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/04/15 16:00:13  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2005/05/08 16:10:31  cervelli
 * Changes for renaming of WWS.
 *
 * Revision 1.1  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.2  2005/04/24 15:16:19  cervelli
 * Removed database name.
 *
 * Revision 1.1  2005/03/24 22:07:51  cervelli
 * Initial commit.
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
		CachedDataSource cache = Swarm.getCache();
		
		Wave sw = cache.getWave(station, t1, t2);
		if (sw == null)
		{
			sw = data.getWave(station, t1, t2);
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
	
	public synchronized HelicorderData getHelicorder(String station, double t1, double t2)
	{
		CachedDataSource cache = Swarm.getCache();
		HelicorderData hd = cache.getHelicorder(station, t1, t2, this);
		if (hd == null)
		{
			hd = data.getHelicorderData(station, t1, t2);
			
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