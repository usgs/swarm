package gov.usgs.swarm.data;

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
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;

/**
 *
 * @author Dan Cervelli
 */
@Deprecated
public class DHIDataSource extends SeismicDataSource
{

	@Override
	public List<String> getChannels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void parse(String params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Wave getWave(String station, double t1, double t2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toConfigString() {
		// TODO Auto-generated method stub
		return null;
	}
//	protected static final String NAMING_SERVICE_URL = "corbaloc:iiop:dmc.iris.washington.edu:6371/NameService";
//	protected String network;
//	
//	protected String networkDC;
//	protected String networkDNS;
//	protected String seismoDC;
//	protected String seismoDNS;
//	
//	protected int gulpSize = 30 * 60;
//	protected int gulpDelay = 1 * 1000;
//	
//	protected Map<String, ChannelId> idMap;
//	protected FissuresNamingService namingService;
//	protected NetworkDCOperations netDC;
//	protected DataCenter seisDC;
//
//	protected org.omg.CORBA_2_3.ORB orb;
//	protected static Logger logger;
//	
////	public DHIDataSource()
////	{}
//	
//	public DHIDataSource(String name, String nw)
//	{
//		super(name);
//		String[] ss = nw.split(":");
//		networkDNS = ss[0];
//		networkDC = ss[1];
//		seismoDNS = ss[2];
//		seismoDC = ss[3];
//		network = ss[4];
//		gulpSize = Integer.parseInt(ss[5]);
//		gulpDelay = Integer.parseInt(ss[6]);
//	}
//	
////	public SeismicDataSource getCopy()
////	{
////		System.out.println("DHI Copy");
////		DHIDataSource ds = new DHIDataSource();
////		ds.network = network;
////		ds.networkDNS = networkDNS;
////		ds.networkDC = networkDC;
////		ds.name = name;
////		ds.seismoDC = seismoDC;
////		ds.seismoDNS = seismoDNS;
////		ds.gulpSize = gulpSize;
////		ds.gulpDelay = gulpDelay;
////		ds.storeInUserConfig = storeInUserConfig;
////		ds.useCache = true;
////		ds.orb = orb;
////		ds.namingService = namingService;
////		ds.netDC = netDC;
////		ds.seisDC = seisDC;
////		ds.idMap = idMap;
////		return ds;
////	}
//	
//	public void establish()
//	{
//		if (logger == null)
//		{
//			BasicConfigurator.configure(new NullAppender());
//			logger = Logger.getLogger(DHIDataSource.class);
//			logger.addAppender(new ConsoleAppender(new SimpleLayout()));
//		}
//		if (orb == null)
//		{
//			Properties prop = new Properties();
//	        prop.setProperty("com.sun.CORBA.transport.ORBTCPReadTimeouts", "100:60000:180000:20");
//	        orb = (org.omg.CORBA_2_3.ORB)org.omg.CORBA.ORB.init(new String[] {}, prop);
//		}
//		
//		try
//		{
//	        new AllVTFactory().register(orb);
//	        namingService = new FissuresNamingService(orb);
//	        namingService.setNameServiceCorbaLoc(NAMING_SERVICE_URL);
//        	netDC = namingService.getNetworkDC(networkDNS, networkDC);
//        	seisDC = namingService.getSeismogramDC(seismoDNS, seismoDC);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
//	
//	public List<String> getChannels()
//	{
//		fireChannelsProgress("channels", 0);
//		idMap = new HashMap<String, ChannelId>();
//		ArrayList<String> result = new ArrayList<String>();
//		double now = CurrentTime.getInstance().nowJ2K();
//		try
//		{
//	        NetworkFinder netFinder = netDC.a_finder();
//	        NetworkAccess net = netFinder.retrieve_by_code(network)[0];
//	        Station[] stations = net.retrieve_stations();
//	        int ns = stations.length;
//	        int cnt = 0;
//	        for (Station s : stations)
//	        {
//	        	double p = (double)cnt / (double)ns;
//	        	fireChannelsProgress("channels", p);
//	        	cnt++;
//	        	String t = s.effective_time.end_time.date_time.replaceAll("[-:]", "");
//	        	double j2k = gov.usgs.util.Time.parse(gov.usgs.util.Time.ISO_8601_TIME_FORMAT, t);
//	        	if (j2k >= (now - 3600))
//	        	{
//		        	Channel[] channels = net.retrieve_for_station(s.get_id());
//		            for (Channel c : channels)
//		            {
//		            	String ch = s.get_code() + " " + c.get_code() + " " + network;
//		            	if (c.sampling_info == null)
//		            		continue;
//		            	double sr = c.sampling_info.numPoints / c.sampling_info.interval.value;
//		            	t = s.effective_time.end_time.date_time.replaceAll("[-:]", "");
//			        	j2k = gov.usgs.util.Time.parse(gov.usgs.util.Time.ISO_8601_TIME_FORMAT, t);
//	            		if (j2k >= (now - 3600) && sr > 1.0)
//		            	{
//		            		String loc = c.get_id().site_code;
//		            		if (loc == null || loc.length() <= 0 || loc.equals("  "))
//		            			loc = "";
//		            		else
//		            			loc = " " + loc;
//		            		
//		            		ch = s.get_code() + " " + c.get_code() + " " + network + loc;
//		            		Metadata md = Swarm.config.getMetadata(ch, true);
//	            			md.updateLongitude(s.my_location.longitude);
//	            			md.updateLatitude(s.my_location.latitude);
//	            			md.addGroup(s.name);
//	            			md.updateAlias(s.name);
//	            			md.source = this;
//	            			if (!result.contains(ch))
//	            				result.add(ch);	
//		            		idMap.put(ch, c.get_id());
//		            	}
//		            }
//	        	}
//	        }
//	        Collections.sort(result);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			result = null;
//		}
//		
//		fireChannelsProgress("channels", 1);
//		return result;
//	}
//
//	public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl)
//	{
//		double now = CurrentTime.getInstance().nowJ2K();
//
//		CachedDataSource cache = CachedDataSource.getInstance();
//		HelicorderData hd = cache.getHelicorder(station, t1, t2, (GulperListener)null);
//		
//		if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10))
//			GulperList.getInstance().requestGulper("dhi:" + station, gl, this, station, t1, t2, gulpSize, gulpDelay);
//		
//		// this gets the tail end, replacing commented out section above
//		if (hd != null && hd.getEndTime() < now)
//			getWave(station, hd.getEndTime(), now);
//
//		return hd;
//	}
//
//	public Wave getWave(String station, double t1, double t2)
//	{
//		CachedDataSource cache = CachedDataSource.getInstance();
//		Wave wave = null;
//		if (useCache)
//			wave = cache.getWave(station, t1, t2);
//		if (wave == null)
//		{
//			try
//			{
//		        RequestFilter[] seismogramRequest = new RequestFilter[1];
//		        String st1 = gov.usgs.util.Time.format("yyyy-MM-dd", t1) + "T" + gov.usgs.util.Time.format("HH:mm:ss.SSS", t1) + "0Z";
//		        String st2 = gov.usgs.util.Time.format("yyyy-MM-dd", t2) + "T" + gov.usgs.util.Time.format("HH:mm:ss.SSS", t2) + "0Z";
//		        
//		        Time start = new Time(st1, -1);
//		        Time end = new Time(st2, -1);
//		        
//		        ChannelId cid = idMap.get(station);
//		        seismogramRequest[0] = new RequestFilter(cid, start, end);
//		        
//		        LocalSeismogram[] seis = seisDC.retrieve_seismograms(seismogramRequest);
//		        ArrayList<Wave> waves = new ArrayList<Wave>();
//		        if (seis.length > 0)
//		        {
//			        for (int i = 0; i < seis.length; i++)
//			        {
//
//			            wave = new Wave();
//			            wave.buffer = seis[i].get_as_longs();
//			            
//			            String t = seis[i].begin_time.date_time.replace('T', ' ');
//			            t = t.substring(0, t.length() - 1);
//			            double j2k = gov.usgs.util.Time.parse("yyyy-MM-dd HH:mm:ss.SSS", t);
//			            wave.setStartTime(j2k);
//			            wave.setSamplingRate((seis[i].sampling_info.numPoints / seis[i].sampling_info.interval.value * 1000));
//			            wave.register();
//			            waves.add(wave);
//			        }
//		        }
//		        wave = Wave.join(waves);
//		        if (wave != null && useCache)
//		        {
//			        cache.cacheWaveAsHelicorder(station, wave);
//			        cache.putWave(station, wave);
//		        }
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//			}
//		}
//		return wave;
//	}
//	
//	public synchronized void notifyDataNotNeeded(String station, double t1, double t2, GulperListener gl)
//	{
//		GulperList.getInstance().killGulper("dhi:" + station, gl);
//	}
//	
//	public synchronized boolean isActiveSource()
//	{
//		return true;	
//	}
//	
//	public String toConfigString()
//	{
//		return String.format("%s;dhi:%s:%s:%s:%s:%s:%d:%d",
//				name, networkDNS, networkDC, seismoDNS, seismoDC, network, gulpSize, gulpDelay);
//	}

}
