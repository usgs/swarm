package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Swarm configuration class. 
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.9  2006/08/01 23:38:12  cervelli
 * New metadata system.
 *
 * Revision 1.8  2006/07/30 22:43:03  cervelli
 * Changes for layouts.
 *
 * Revision 1.7  2006/07/28 15:02:03  cervelli
 * Outputs config file path.
 *
 * Revision 1.6  2006/07/26 22:39:10  cervelli
 * Added mapPath.
 *
 * Revision 1.5  2006/07/22 20:21:59  cervelli
 * Many Swarm 2.0 changes: time zones, map parameters, data sources, etc.
 *
 * Revision 1.4  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.3  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.2  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.1  2006/04/15 15:53:09  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class Config
{
	private static String[] DEFAULT_SERVERS = new String[] {
				"AVO Winston;wws:pubavo1.wr.usgs.gov:16022:10000:1",
				"IRIS DMC - New Zealand;dhi:NZ"
			};
	
	private static String DEFAULT_CONFIG_FILE = "Swarm.config";
	private static String DEFAULT_DATA_SOURCES_FILE = "DataSources.config";
	
	public String configFilename;
	public int windowX;
	public int windowY;
	public int windowWidth;
	public int windowHeight;
	public boolean windowMaximized;
	
	public TimeZone specificTimeZone;
	public boolean useInstrumentTimeZone;
	public boolean useLocalTimeZone;
	
	public String lastPath;
	
	public boolean useLargeCursor;
	public boolean durationEnabled;
	public double durationA;
	public double durationB;
	
	public int span;
	public int timeChunk;
	public boolean showClip;
	public boolean alertClip;
	public int alertClipTimeout;
	
	public String kiosk;
	
	public boolean saveConfig;
	public String groupConfigFile;
	
	public int chooserDividerLocation;
	public boolean chooserVisible;
	
	public int nearestDividerLocation;
	
	public boolean clipboardVisible;
	public int clipboardX;
	public int clipboardY;
	public int clipboardWidth;
	public int clipboardHeight;
	public boolean clipboardMaximized;

	public boolean mapVisible;
	public int mapX;
	public int mapY;
	public int mapWidth;
	public int mapHeight;
	public boolean mapMaximized;
	
	public double mapScale;
	public double mapLongitude;
	public double mapLatitude;
	public String mapPath;
	
	public Map<String, SeismicDataSource> sources;
	
	private Map<String, Metadata> metadata;
	private Map<String, Metadata> defaultMetadata;
	
	public List<SwarmLayout> layouts;
	
	public static Config createConfig(String[] args)
	{
		Swarm.logger.fine("user.home: " + System.getProperty("user.home"));
		String configFile = System.getProperty("user.home") + File.separatorChar + DEFAULT_CONFIG_FILE;
		
		int n = args.length - 1;
		if (n >= 0 && !args[n].startsWith("-"))
			configFile = args[n];

		Swarm.logger.fine("using config: " + configFile);
		
		ConfigFile cf = new ConfigFile(configFile);
		cf.put("configFile", configFile, false);
		   
		for (int i = 0; i <= n; i++)
		{
			if (args[i].startsWith("--"))
			{
				String key = args[i].substring(2, args[i].indexOf('='));
				String val = args[i].substring(args[i].indexOf('=') + 1);
				Swarm.logger.fine("command line: " + key + " = " + val);
				cf.put(key, val, false);
			}
		}
		Config config = new Config(cf);
		config.defaultMetadata = Metadata.loadMetadata(Metadata.DEFAULT_METADATA_FILENAME);
		config.metadata = Collections.synchronizedMap(new HashMap<String, Metadata>());
		config.loadDataSources();
		config.loadLayouts();
		return config;
	}
	
	private void loadDataSources()
	{
		ConfigFile cf = new ConfigFile(DEFAULT_DATA_SOURCES_FILE);
//		cf.getList("server");
		List<String> servers = cf.getList("server");
		if (servers != null)
		{
			for (String server : servers)
			{
				SeismicDataSource sds = SeismicDataSource.getDataSource(server);
				sds.setStoreInUserConfig(false);
				sources.put(sds.getName(), sds);
			}
		}
	}

	private void loadLayouts()
	{
		layouts = new ArrayList<SwarmLayout>();
		
		File[] files = new File("layouts").listFiles();
		if (files == null)
			return;
		
		for (File f : files)
		{
			if (!f.isDirectory())
			{
				SwarmLayout sl = SwarmLayout.createSwarmLayout(f.getPath());
				if (sl != null)
				{
					layouts.add(sl);
				}
			}
		}
		Collections.sort(layouts);
	}
	
	public void addLayout(SwarmLayout sl)
	{
		layouts.add(sl);
	}
	
	public Map<String, Metadata> getMetadata()
	{
		return metadata;
	}
	
	public Metadata getMetadata(String channel)
	{
		return getMetadata(channel, false);
	}
	
	public Metadata getMetadata(String channel, boolean create)
	{
		Metadata md = metadata.get(channel);
		
		if (md == null)
			md = defaultMetadata.get(channel);
		
		if (md == null && create)
			md = new Metadata(channel);
		
		if (md != null)
			metadata.put(channel, md);
		
		return md;
	}
	
	public void assignMetadataSource(List<String> channels, SeismicDataSource source)
	{
		for (String ch : channels)
		{
			Metadata md = getMetadata(ch, true);
			md.source = source;
		}
	}
	
	/**
	 * Sets Swarm configuration variables based on the contents of a 
	 * ConfigFile; sets default values if missing.
	 * 
	 * @param config the configuration information
	 */
	public Config(ConfigFile config)
	{
		configFilename = config.getString("configFile");
		
		windowX = Util.stringToInt(config.getString("windowX"), 10);
		windowY = Util.stringToInt(config.getString("windowY"), 10);
		windowWidth = Util.stringToInt(config.getString("windowSizeX"), 1000);
		windowHeight = Util.stringToInt(config.getString("windowSizeY"), 700);
		windowMaximized = Util.stringToBoolean(config.getString("windowMaximized"), false);
		
		chooserDividerLocation = Util.stringToInt(config.getString("chooserDividerLocation"), 200);
		chooserVisible = Util.stringToBoolean(config.getString("chooserVisible"), true);
	
		nearestDividerLocation = Util.stringToInt(config.getString("nearestDividerLocation"), 600);
		
		specificTimeZone = TimeZone.getTimeZone(Util.stringToString(config.getString("specificTimeZone"), "UTC"));
		useInstrumentTimeZone = Util.stringToBoolean(config.getString("useInstrumentTimeZone"), true);
		useLocalTimeZone = Util.stringToBoolean(config.getString("useLocalTimeZone"), true);
		
		useLargeCursor = Util.stringToBoolean(config.getString("useLargeCursor"), false);
		
		span = Util.stringToInt(config.getString("span"), 24);
		timeChunk = Util.stringToInt(config.getString("timeChunk"), 30);
		
		lastPath = Util.stringToString(config.getString("lastPath"), "default");
		
		kiosk = Util.stringToString(config.getString("kiosk"), "false");
		
		groupConfigFile = Util.stringToString(config.getString("groupConfigFile"), "SwarmGroups.config");
		saveConfig = Util.stringToBoolean(config.getString("saveConfig"), true);
		
		durationEnabled = Util.stringToBoolean(config.getString("durationEnabled"), false);
		durationA = Util.stringToDouble(config.getString("durationA"), 1.86);
		durationB = Util.stringToDouble(config.getString("durationB"), -0.85);
		
		showClip = Util.stringToBoolean(config.getString("showClip"), true);
		alertClip = Util.stringToBoolean(config.getString("alertClip"), false);
		alertClipTimeout = Util.stringToInt(config.getString("alertClipTimeout"), 5);
		
		clipboardVisible = Util.stringToBoolean(config.getString("clipboardVisible"), true);
		clipboardX = Util.stringToInt(config.getString("clipboardX"), 5);
		clipboardY = Util.stringToInt(config.getString("clipboardY"), 5);
		clipboardWidth = Util.stringToInt(config.getString("clipboardSizeX"), 600);
		clipboardHeight = Util.stringToInt(config.getString("clipboardSizeY"), 700);
		clipboardMaximized = Util.stringToBoolean(config.getString("clipboardMaximized"), false);

		mapPath = Util.stringToString(config.getString("mapPath"), "mapdata");
		mapVisible = Util.stringToBoolean(config.getString("mapVisible"), true);
		mapX = Util.stringToInt(config.getString("mapX"), 10);
		mapY = Util.stringToInt(config.getString("mapY"), 10);
		mapWidth = Util.stringToInt(config.getString("mapWidth"), 600);
		mapHeight = Util.stringToInt(config.getString("mapHeight"), 400);
		mapMaximized = Util.stringToBoolean(config.getString("mapMaximized"), false);
	
		mapScale = Util.stringToDouble(config.getString("mapScale"), 80000);
		mapLongitude = Util.stringToDouble(config.getString("mapLongitude"), 0);
		mapLatitude = Util.stringToDouble(config.getString("mapLatitude"), 0);
		
		sources = new HashMap<String, SeismicDataSource>();
		List<String> servers = config.getList("server");
		if (servers != null && servers.size() > 0)
		{
			for (String server : servers)
			{
				SeismicDataSource sds = SeismicDataSource.getDataSource(server);
				sources.put(sds.getName(), sds);
			}
		}
		else
		{
			for (String s : DEFAULT_SERVERS)
			{
				SeismicDataSource sds = SeismicDataSource.getDataSource(s);
				sources.put(sds.getName(), sds);
			}
		}
	}
	
	public SeismicDataSource getSource(String key)
	{
		return sources.get(key);
	}
	
	public boolean sourceExists(String key)
	{
		return sources.containsKey(key);
	}
	
	public void addSource(SeismicDataSource source)
	{
		sources.put(source.getName(), source);
	}
	
	public void removeSource(String key)
	{
		sources.remove(key);
	}
	
	public double getDurationMagnitude(double t)
	{
		return durationA * (Math.log(t) / Math.log(10)) + durationB;
	}
	
	public TimeZone getTimeZone(String channel)
	{
		if (useInstrumentTimeZone && channel != null)
		{
			Metadata md = metadata.get(channel);
			if (md != null && md.getTimeZone() != null)
				return md.getTimeZone();
		}
		
		if (useLocalTimeZone)
			return TimeZone.getDefault();
		else
			return specificTimeZone;
	}
	
	public boolean isKiosk()
	{
		return !kiosk.toLowerCase().equals("false");
	}
	
	public ConfigFile toConfigFile()
	{
		ConfigFile config = new ConfigFile();
		config.put("configFile", configFilename);
		
		config.put("windowX", Integer.toString(windowX));
		config.put("windowY", Integer.toString(windowY));
		config.put("windowSizeX", Integer.toString(windowWidth));
		config.put("windowSizeY", Integer.toString(windowHeight));
		config.put("chooserDividerLocation", Integer.toString(chooserDividerLocation));
		config.put("chooserVisible", Boolean.toString(chooserVisible));
		
		config.put("nearestDividerLocation", Integer.toString(nearestDividerLocation));
		
		config.put("specificTimeZone", specificTimeZone.getID());
		config.put("useInstrumentTimeZone", Boolean.toString(useInstrumentTimeZone));
		config.put("useLocalTimeZone", Boolean.toString(useLocalTimeZone));
		
		config.put("windowMaximized", Boolean.toString(windowMaximized));
		config.put("useLargeCursor", Boolean.toString(useLargeCursor));
		
		config.put("span", Integer.toString(span));
		config.put("timeChunk", Integer.toString(timeChunk));
		
		config.put("lastPath", lastPath);
		
		config.put("kiosk", kiosk);

		config.put("groupConfigFile", groupConfigFile);
		config.put("saveConfig", Boolean.toString(saveConfig));
		
		config.put("durationEnabled", Boolean.toString(durationEnabled));
		config.put("durationA", Double.toString(durationA));
		config.put("durationB", Double.toString(durationB));

		config.put("showClip", Boolean.toString(showClip));
		config.put("alertClip", Boolean.toString(alertClip));
		config.put("alertClipTimeout", Integer.toString(alertClipTimeout));

		config.put("clipboardVisible", Boolean.toString(clipboardVisible));
		config.put("clipboardX", Integer.toString(clipboardX));
		config.put("clipboardY", Integer.toString(clipboardY));
		config.put("clipboardSizeX", Integer.toString(clipboardWidth));
		config.put("clipboardSizeY", Integer.toString(clipboardHeight));
		config.put("clipboardMaximized", Boolean.toString(clipboardMaximized));
		
		config.put("mapPath", mapPath);
		config.put("mapVisible", Boolean.toString(mapVisible));
		config.put("mapX", Integer.toString(mapX));
		config.put("mapY", Integer.toString(mapY));
		config.put("mapWidth", Integer.toString(mapWidth));
		config.put("mapHeight", Integer.toString(mapHeight));
		config.put("mapMaximized", Boolean.toString(mapMaximized));
		config.put("mapScale", Double.toString(mapScale));
		config.put("mapLongitude", Double.toString(mapLongitude));
		config.put("mapLatitude", Double.toString(mapLatitude));
		
		List<String> servers = new ArrayList<String>(); 
		for (SeismicDataSource sds : sources.values())
		{
			if (sds.isStoreInUserConfig())
				servers.add(sds.toConfigString());
		}
		
		config.putList("server", servers);
		
		return config;
	}
	
	public String toString()
	{
		return toConfigFile().toString();
	}
}
