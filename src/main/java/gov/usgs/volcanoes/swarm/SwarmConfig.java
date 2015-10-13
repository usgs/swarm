package gov.usgs.volcanoes.swarm;

import gov.usgs.plot.map.WMSGeoImageSet;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.swarm.data.DataSourceType;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Swarm configuration class.
 * 
 * @author Dan Cervelli
 */
public class SwarmConfig {

  private static Logger logger = Log.getLogger("gov.usgs.swarm");

  private static String[] DEFAULT_SERVERS =
      new String[] {"AVO Winston;wws:pubavo1.wr.usgs.gov:16022:10000:1"
      // "IRIS DMC - New
      // Zealand;dhi:edu/iris/dmc:IRIS_NetworkDC:edu/iris/dmc:IRIS_BudDataCenter:NZ:3600:1000"
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
  public int mapLineWidth;
  public int mapLineColor;

  public String[] userTimes;
  public Color[] heliColors;
  public String heliColorsString;

  public Map<String, SeismicDataSource> sources;

  // TODO: use ConcurrentHashMap
  private Map<String, Metadata> metadata;
  private Map<String, Metadata> defaultMetadata;

  public SortedMap<String, SwarmLayout> layouts;

  public boolean useWMS;
  public String wmsServer;
  public String wmsLayer;
  public String wmsStyles;

  public String labelSource;
  public String fdsnDataselectURL;
  public String fdsnStationURL;

  private SwarmConfig() {}

  public static SwarmConfig getInstance() {
    return SwarmConfigHolder.swarmConfig;
  }

  public void createConfig(String[] args) {
    logger.fine("current directory: " + System.getProperty("user.dir"));
    logger.fine("user.home: " + System.getProperty("user.home"));
    String configFile;

    int n = args.length - 1;
    if (n >= 0 && !args[n].startsWith("-"))
      configFile = args[n];
    else {
      List<String> candidateNames = new LinkedList<String>();
      candidateNames.add(DEFAULT_CONFIG_FILE);
      candidateNames
          .add(System.getProperty("user.home") + File.separatorChar + DEFAULT_CONFIG_FILE);
      candidateNames.add("gov.usgs.swarm.Swam");
      configFile = ConfigFile.findConfig(candidateNames);
    }

    if (configFile == null)
      configFile = DEFAULT_CONFIG_FILE;

    logger.fine("Using configuration file: " + configFile);

    ConfigFile cf = new ConfigFile(configFile);
    cf.put("configFile", configFile, false);

    for (int i = 0; i <= n; i++) {
      if (args[i].startsWith("--")) {
        String key = args[i].substring(2, args[i].indexOf('='));
        String val = args[i].substring(args[i].indexOf('=') + 1);
        logger.fine("command line: " + key + " = " + val);
        cf.put(key, val, false);
      }
    }
    parseConfig(cf);

    List<String> candidateNames = new LinkedList<String>();
    candidateNames.add(Metadata.DEFAULT_METADATA_FILENAME);
    candidateNames.add(
        System.getProperty("user.home") + File.separatorChar + Metadata.DEFAULT_METADATA_FILENAME);

    String metadataConfigFile = ConfigFile.findConfig(candidateNames);
    if (metadataConfigFile == null)
      metadataConfigFile = Metadata.DEFAULT_METADATA_FILENAME;
    else
      logger.fine("Using metadata configuration file: " + metadataConfigFile);

    defaultMetadata = Metadata.loadMetadata(metadataConfigFile);
    metadata = Collections.synchronizedMap(new HashMap<String, Metadata>());

    loadDataSources();
    loadLayouts();
  }

  private void loadDataSources() {

    List<String> candidateNames = new LinkedList<String>();
    candidateNames.add(DEFAULT_DATA_SOURCES_FILE);
    candidateNames
        .add(System.getProperty("user.home") + File.separatorChar + DEFAULT_DATA_SOURCES_FILE);
    String configName =
        Util.stringToString(ConfigFile.findConfig(candidateNames), DEFAULT_DATA_SOURCES_FILE);

    ConfigFile cf = new ConfigFile(configName);
    List<String> servers = cf.getList("server");
    if (servers != null) {
      for (String server : servers) {
        SeismicDataSource sds = DataSourceType.parseConfig(server);
        if (sds == null) {
          logger.info("Skipping unknown data soruce " + server);
          continue;
        }

        sds.setStoreInUserConfig(false);
        sources.put(sds.getName(), sds);
      }
    }
  }

  private void loadLayouts() {
    layouts = new TreeMap<String, SwarmLayout>();

    File[] files = new File("layouts").listFiles();
    if (files == null)
      return;

    for (File f : files) {
      if (!f.isDirectory()) {
        SwarmLayout sl = SwarmLayout.createSwarmLayout(f.getPath());
        if (sl != null) {
          layouts.put(sl.getName(), sl);
        }
      }
    }
  }

  public void addLayout(SwarmLayout sl) {
    layouts.put(sl.getName(), sl);
  }

  public void removeLayout(SwarmLayout layout) {
    layouts.remove(layout.getName());
    layout.delete();
  }

  public void removeMetadata(String ch) {
    metadata.remove(ch);
  }

  public Map<String, Metadata> getMetadata() {
    return metadata;
  }

  public Metadata getMetadata(String channel) {
    return getMetadata(channel, false);
  }

  public Metadata getMetadata(String channel, boolean create) {
    Metadata md = metadata.get(channel);

    if (md == null)
      md = defaultMetadata.get(channel);

    if (md == null && create)
      md = new Metadata(channel);

    if (md != null)
      metadata.put(channel, md);

    return md;
  }

  public void assignMetadataSource(Collection<String> channels, SeismicDataSource source) {
    for (String ch : channels) {
      Metadata md = getMetadata(ch, true);
      md.source = source;
    }
  }

  /**
   * Sets Swarm configuration variables based on the contents of a ConfigFile;
   * sets default values if missing.
   * 
   * @param config
   *          the configuration information
   */
  public void parseConfig(ConfigFile config) {
    configFilename = config.getString("configFile");

    windowX = Util.stringToInt(config.getString("windowX"), 10);
    windowY = Util.stringToInt(config.getString("windowY"), 10);
    windowWidth = Util.stringToInt(config.getString("windowSizeX"), 1000);
    windowHeight = Util.stringToInt(config.getString("windowSizeY"), 700);
    windowMaximized = Util.stringToBoolean(config.getString("windowMaximized"), false);

    chooserDividerLocation = Util.stringToInt(config.getString("chooserDividerLocation"), 200);
    chooserVisible = Util.stringToBoolean(config.getString("chooserVisible"), true);

    nearestDividerLocation = Util.stringToInt(config.getString("nearestDividerLocation"), 600);

    specificTimeZone =
        TimeZone.getTimeZone(Util.stringToString(config.getString("specificTimeZone"), "UTC"));
    useInstrumentTimeZone = Util.stringToBoolean(config.getString("useInstrumentTimeZone"), true);
    useLocalTimeZone = Util.stringToBoolean(config.getString("useLocalTimeZone"), true);

    useLargeCursor = Util.stringToBoolean(config.getString("useLargeCursor"), false);

    span = Util.stringToInt(config.getString("span"), 24);
    timeChunk = Util.stringToInt(config.getString("timeChunk"), 30);

    lastPath = Util.stringToString(config.getString("lastPath"), "default");

    kiosk = Util.stringToString(config.getString("kiosk"), "false");

    saveConfig = Util.stringToBoolean(config.getString("saveConfig"), true);

    durationEnabled = Util.stringToBoolean(config.getString("durationEnabled"), false);
    durationA = Util.stringToDouble(config.getString("durationA"), 1.86);
    durationB = Util.stringToDouble(config.getString("durationB"), -0.85);

    showClip = Util.stringToBoolean(config.getString("showClip"), true);
    alertClip = Util.stringToBoolean(config.getString("alertClip"), false);
    alertClipTimeout = Util.stringToInt(config.getString("alertClipTimeout"), 5);

    clipboardVisible = Util.stringToBoolean(config.getString("clipboardVisible"), true);
    clipboardX = Util.stringToInt(config.getString("clipboardX"), 25);
    clipboardY = Util.stringToInt(config.getString("clipboardY"), 25);
    clipboardWidth = Util.stringToInt(config.getString("clipboardSizeX"), 600);
    clipboardHeight = Util.stringToInt(config.getString("clipboardSizeY"), 600);
    clipboardMaximized = Util.stringToBoolean(config.getString("clipboardMaximized"), false);

    mapPath = Util.stringToString(config.getString("mapPath"), "mapdata");
    mapVisible = Util.stringToBoolean(config.getString("mapVisible"), true);
    mapX = Util.stringToInt(config.getString("mapX"), 5);
    mapY = Util.stringToInt(config.getString("mapY"), 5);

    mapWidth = Util.stringToInt(config.getString("mapWidth"), 600);
    mapWidth = Math.max(mapWidth, 100);

    mapHeight = Util.stringToInt(config.getString("mapHeight"), 510);
    mapHeight = Math.max(mapHeight, 100);

    mapMaximized = Util.stringToBoolean(config.getString("mapMaximized"), false);

    mapScale = Util.stringToDouble(config.getString("mapScale"), 80000);
    mapLongitude = Util.stringToDouble(config.getString("mapLongitude"), -180);
    mapLatitude = Util.stringToDouble(config.getString("mapLatitude"), 0);
    mapLineWidth = Util.stringToInt(config.getString("mapLineWidth"), 2);
    mapLineColor = Util.stringToInt(config.getString("mapLineColor"), 0x000000);

    useWMS = Util.stringToBoolean(config.getString("useWMS"));
    wmsServer = Util.stringToString(config.getString("wmsServer"), WMSGeoImageSet.DEFAULT_SERVER);
    wmsLayer = Util.stringToString(config.getString("wmsLayer"), WMSGeoImageSet.DEFAULT_LAYER);
    wmsStyles = Util.stringToString(config.getString("wmsStyles"), WMSGeoImageSet.DEFAULT_STYLE);

    labelSource = Util.stringToString(config.getString("labelSource"), "");

    fdsnDataselectURL = Util.stringToString(config.getString("fdsnDataselectURL"),
        "http://service.iris.edu/fdsnws/dataselect/1/query");
    fdsnStationURL = Util.stringToString(config.getString("fdsnStationURL"),
        "http://service.iris.edu/fdsnws/station/1/query");

    sources = new HashMap<String, SeismicDataSource>();
    List<String> servers = config.getList("server");
    if (servers != null && servers.size() > 0) {
      for (String server : servers) {
        // SeismicDataSource sds =
        // SeismicDataSource.getDataSource(server);
        SeismicDataSource sds = DataSourceType.parseConfig(server);
        if (sds == null) {
          logger.info("Skipping unknown data soruce " + server);
          continue;
        }
        sources.put(sds.getName(), sds);
      }
    } else {
      for (String s : DEFAULT_SERVERS) {
        // SeismicDataSource sds = SeismicDataSource.getDataSource(s);
        SeismicDataSource sds = DataSourceType.parseConfig(s);
        sources.put(sds.getName(), sds);
      }
    }

    userTimes = Util.stringToString(config.getString("userTimes"), "").split(",");

    heliColorsString = Util.stringToString(config.getString("heliColors"), "");
    if (heliColorsString != null) {
      if (heliColorsString.length() > 3) {
        String[] color = heliColorsString.split(":");
        heliColors = new Color[color.length];
        for (int i = 0; i < color.length; i++) {
          String[] parts = color[i].split(",");
          if (parts.length == 3) {
            float red = Float.parseFloat(parts[0].trim());
            float green = Float.parseFloat(parts[1].trim());
            float blue = Float.parseFloat(parts[2].trim());
            try {
              heliColors[i] = new Color(red / 256, green / 256, blue / 256);
            } catch (RuntimeException e) {
              heliColors[i] = Color.magenta;
            }
          } else
            heliColors[i] = Color.magenta; // If the color is
                                           // illegal, make it
                                           // magenta
        }
      }

    }
  }

  public SeismicDataSource getSource(String key) {
    return sources.get(key);
  }

  public boolean sourceExists(String key) {
    return sources.containsKey(key);
  }

  public void addSource(SeismicDataSource source) {
    sources.put(source.getName(), source);
  }

  public void removeSource(String key) {
    sources.remove(key);
  }

  public double getDurationMagnitude(double t) {
    return durationA * (Math.log(t) / Math.log(10)) + durationB;
  }

  public TimeZone getTimeZone(String channel) {
    if (useInstrumentTimeZone && channel != null) {
      Metadata md = getMetadata(channel, false);
      if (md != null && md.getTimeZone() != null)
        return md.getTimeZone();
    }

    if (useLocalTimeZone)
      return TimeZone.getDefault();
    else
      return specificTimeZone;
  }

  public boolean isKiosk() {
    return !kiosk.toLowerCase().equals("false");
  }

  public ConfigFile toConfigFile() {
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
    config.put("mapLineWidth", Integer.toString(mapLineWidth));
    config.put("mapLineColor", Integer.toString(mapLineColor));

    config.put("useWMS", Boolean.toString(useWMS));
    config.put("wmsServer", wmsServer);
    config.put("wmsLayer", wmsLayer);
    config.put("wmsStyles", wmsStyles);

    config.put("labelSource", labelSource);

    config.put("fdsnDataselectURL", fdsnDataselectURL);
    config.put("fdsnStationURL", fdsnStationURL);

    List<String> servers = new ArrayList<String>();
    for (SeismicDataSource sds : sources.values()) {
      if (sds.isStoreInUserConfig())
        servers.add(sds.toConfigString());
    }

    config.putList("server", servers);

    StringBuilder utsb = new StringBuilder();
    for (int i = 0; i < userTimes.length - 1; i++) {
      utsb.append(userTimes[i]);
      utsb.append(",");
    }
    if (userTimes.length > 0)
      utsb.append(userTimes[userTimes.length - 1]);
    config.put("userTimes", utsb.toString());

    if (heliColorsString != null)
      if (heliColorsString.length() > 3)
        config.put("heliColors", heliColorsString);
    return config;
  }

  public String toString() {
    return toConfigFile().toString();
  }

  private static class SwarmConfigHolder {
    public static SwarmConfig swarmConfig = new SwarmConfig();
  }
}
