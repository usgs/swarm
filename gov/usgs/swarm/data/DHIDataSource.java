package gov.usgs.swarm.data;

import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkFinder;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfSeismogramDC.DataCenter;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.model.AllVTFactory;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.varia.NullAppender;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.8  2006/08/12 00:35:54  dcervelli
 * Progress indications from getChannels().
 *
 * Revision 1.7  2006/08/10 14:31:05  cervelli
 * Fixed synch bugs.
 *
 * Revision 1.6  2006/08/08 22:21:30  cervelli
 * Configurable network/seismogram DC/DNS and gulper variables.
 *
 * Revision 1.5  2006/08/04 21:21:09  cervelli
 * Got rid of some useless output.
 *
 * Revision 1.4  2006/08/01 23:44:07  cervelli
 * New metadata system changes.
 *
 * Revision 1.3  2006/07/30 22:46:24  cervelli
 * Change for gulper.
 *
 * Revision 1.2  2006/07/26 00:36:02  cervelli
 * Changes for new gulper system.
 *
 * @author Dan Cervelli
 */
public class DHIDataSource extends SeismicDataSource
{
	protected static final String NAMING_SERVICE_URL = "corbaloc:iiop:dmc.iris.washington.edu:6371/NameService";
	protected String network;
	
	protected String networkDC;
	protected String networkDNS;
	protected String seismoDC;
	protected String seismoDNS;
	
	protected int gulpSize = 30 * 60;
	protected int gulpDelay = 1 * 1000;
	
	protected Map<String, ChannelId> idMap;
	protected FissuresNamingService namingService;
	protected NetworkDCOperations netDC;
	protected DataCenter seisDC;

	protected org.omg.CORBA_2_3.ORB orb;
	protected static Logger logger;
	
	public DHIDataSource(String nw)
	{
		String[] ss = nw.split(":");
		networkDNS = ss[0];
		networkDC = ss[1];
		seismoDNS = ss[2];
		seismoDC = ss[3];
		network = ss[4];
		gulpSize = Integer.parseInt(ss[5]);
		gulpDelay = Integer.parseInt(ss[6]);
	}
	
	public void establish()
	{
		if (logger == null)
		{
			BasicConfigurator.configure(new NullAppender());
			logger = Logger.getLogger(DHIDataSource.class);
			logger.addAppender(new ConsoleAppender(new SimpleLayout()));
		}
		if (orb == null)
		{
			Properties prop = new Properties();
			/* Alternate form of setting timeout:
			//===================================================================
			// set a timeout for response from the server
			//===================================================================
			org.omg.CORBA.Any ULongAny = orb.create_any();
			// time out if no response in 10 minutes
			if( debug ) System.out.println("Setting wait to: "+wait+" ms");
			ULongAny.insert_ulong( wait ); 
			org.omg.CORBA.Policy[] policies = new org.omg.CORBA.Policy[1];
			policies[0] = orb.create_policy(com.ooc.OB.TIMEOUT_POLICY_ID.value, ULongAny);
			org.omg.CORBA.PolicyManager pm =
				org.omg.CORBA.PolicyManagerHelper.narrow(
					orb.resolve_initial_references("ORBPolicyManager"));
			pm.add_policy_overrides(policies);
			 */
	        prop.setProperty("com.sun.CORBA.transport.ORBTCPReadTimeouts", "100:60000:180000:20");
	        orb = (org.omg.CORBA_2_3.ORB)org.omg.CORBA.ORB.init(new String[] {}, prop);
		}
		
		try
		{
	        new AllVTFactory().register(orb);
	        namingService = new FissuresNamingService(orb);
	        namingService.setNameServiceCorbaLoc(NAMING_SERVICE_URL);
        	netDC = namingService.getNetworkDC(networkDNS, networkDC);
        	seisDC = namingService.getSeismogramDC(seismoDNS, seismoDC);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public List<String> getChannels()
	{
		fireChannelsProgress("channels", 0);
		idMap = new HashMap<String, ChannelId>();
		ArrayList<String> result = new ArrayList<String>();
		try
		{
	        NetworkFinder netFinder = netDC.a_finder();
	        NetworkAccess net = netFinder.retrieve_by_code(network)[0];
	        Station[] stations = net.retrieve_stations();
	        int ns = stations.length;
	        int cnt = 0;
	        for (Station s : stations)
	        {
	        	double p = (double)cnt / (double)ns;
	        	fireChannelsProgress("channels", p);
	        	cnt++;
	        	if (s.effective_time.end_time.date_time.startsWith("25"))
	        	{
	        		Swarm.logger.finest("dhi channel: " + s.name);
		        	Channel[] channels = net.retrieve_for_station(s.get_id());
		            for (Channel c : channels)
		            {
		            	double sr = c.sampling_info.numPoints / c.sampling_info.interval.value;
		            	if (c.effective_time.end_time.date_time.startsWith("25") && sr > 1.0)
		            	{
		            		String loc = c.get_id().site_code;
		            		if (loc == null || loc.length() <= 0 || loc.equals("  "))
		            			loc = "";
		            		else
		            			loc = " " + loc;
		            		
		            		String ch = s.get_code() + " " + c.get_code() + " " + network + loc;
		            		Metadata md = Swarm.config.getMetadata(ch, true);
	            			md.updateLongitude(s.my_location.longitude);
	            			md.updateLatitude(s.my_location.latitude);
	            			md.source = this;
		            		result.add(ch);
		            		idMap.put(ch, c.get_id());
		            	}
		            }
	        	}
	        }
	        Collections.sort(result);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = null;
		}
		
		fireChannelsProgress("channels", 1);
		return result;
	}

	public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl)
	{
		double now = CurrentTime.getInstance().nowJ2K();
		// if a time later than now has been asked for make sure to get the latest
		// so that, if possible, a small bit of helicorder data will be displayed
		if ((t2 - now) >= -20)
		{
			getWave(station, now - 2*60, now);
		}
		
		CachedDataSource cache = Swarm.getCache();
		HelicorderData hd = cache.getHelicorder(station, t1, t2, (GulperListener)null);	

		if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10))
			GulperList.getInstance().requestGulper("dhi:" + station, gl, this, station, t1, t2, gulpSize, gulpDelay);

		return hd;
	}

	public Wave getWave(String station, double t1, double t2)
	{
		CachedDataSource cache = Swarm.getCache();
		Wave wave = cache.getWave(station, t1, t2);
		if (wave == null)
		{
			try
			{
		        RequestFilter[] seismogramRequest = new RequestFilter[1];
		        String st1 = gov.usgs.util.Time.format("yyyy-MM-dd", t1) + "T" + gov.usgs.util.Time.format("HH:mm:ss.SSS", t1) + "0Z";
		        String st2 = gov.usgs.util.Time.format("yyyy-MM-dd", t2) + "T" + gov.usgs.util.Time.format("HH:mm:ss.SSS", t2) + "0Z";
		        
		        Time start = new Time(st1, -1);
		        Time end = new Time(st2, -1);
		        
		        ChannelId cid = idMap.get(station);
		        seismogramRequest[0] = new RequestFilter(cid, start, end);
		        
		        LocalSeismogram[] seis = seisDC.retrieve_seismograms(seismogramRequest);
		        ArrayList<Wave> waves = new ArrayList<Wave>();
		        if (seis.length > 0)
		        {
			        for (int i = 0; i < seis.length; i++)
			        {
			            Swarm.logger.finer("seis[" + i + "] has " + seis[i].num_points
			                    + " points and starts at " + seis[i].begin_time.date_time + " " + 
			                    (seis[i].sampling_info.numPoints / seis[i].sampling_info.interval.value * 1000));
			            
			            wave = new Wave();
			            wave.buffer = seis[i].get_as_longs();
			            String t = seis[i].begin_time.date_time.replace('T', ' ');
			            t = t.substring(0, t.length() - 1);
			            double j2k = gov.usgs.util.Time.parse("yyyy-MM-dd HH:mm:ss.SSS", t);
			            wave.setStartTime(j2k);
			            wave.setSamplingRate((seis[i].sampling_info.numPoints / seis[i].sampling_info.interval.value * 1000));
			            wave.register();
			            waves.add(wave);
//			            System.out.println(wave);
			        }
		        }
//		        Collections.sort(waves);
		        wave = Wave.join(waves);
		        if (wave != null)
		        {
			        cache.cacheWaveAsHelicorder(station, wave);
			        cache.putWave(station, wave);
		        }
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return wave;
	}
	
	public synchronized void notifyDataNotNeeded(String station, double t1, double t2, GulperListener gl)
	{
		GulperList.getInstance().killGulper("dhi:" + station, gl);
	}
	
	public synchronized boolean isActiveSource()
	{
		return true;	
	}
	
	public String toConfigString()
	{
		return String.format("%s;dhi:%s:%s:%s:%s:%s:%d:%d",
				name, networkDNS, networkDC, seismoDNS, seismoDC, network, gulpSize, gulpDelay);
	}
}
