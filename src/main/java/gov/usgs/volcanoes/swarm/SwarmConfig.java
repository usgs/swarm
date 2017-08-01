package gov.usgs.volcanoes.swarm;

import gov.usgs.plot.map.WMSGeoImageSet;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.swarm.data.DataSourceType;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.map.hypocenters.HypocenterSource;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Swarm configuration class. 
 * 
 * <p>TODO: This is getting our of hand. Extract configs for individual components. e.g. map
 * 
 * @author Dan Cervelli
 */
public class SwarmConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SwarmConfig.class);

  private final List<ConfigListener> listeners;
  
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
  
  public double pVelocity;

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
  private HypocenterSource hypocenterSource;

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

  public String fdsnDataselectURL;
  public String fdsnStationURL;
  
  public String user;

  private SwarmConfig() {
    listeners = new ArrayList<ConfigListener>();
  }

  public void addListener(ConfigListener configListener) {
    listeners.add(configListener);
  }
  
  private void notifyListeners() {
    for (ConfigListener listener : listeners) {
      listener.settingsChanged();
    }  
  }
  
  public static SwarmConfig getInstance() {
    return SwarmConfigHolder.swarmConfig;
  }
  
  public void setHypocenterSource(HypocenterSource hypocenterSource) {
    this.hypocenterSource = hypocenterSource;
    notifyListeners();
  }
  
  public HypocenterSource getHypocenterSource() {
    return hypocenterSource;
  }

  /**
   * Create Swarm configurations.
   * @param args arguments
   */
  public void createConfig(final String[] args) {
    LOGGER.info("current directory: " + System.getProperty("user.dir"));
    LOGGER.info("user.home: " + System.getProperty("user.home"));

    metadata = Collections.synchronizedMap(new HashMap<String, Metadata>());
    
    // Identify configuration file to use
    String configFile;
    final int n = args.length - 1;
    if (n >= 0 && !args[n].startsWith("-")) {
      configFile = args[n];
    } else {
      final List<String> candidateNames = new LinkedList<String>();
      candidateNames.add(DEFAULT_CONFIG_FILE);
      candidateNames
          .add(System.getProperty("user.home") + File.separatorChar + DEFAULT_CONFIG_FILE);
      candidateNames.add("gov.usgs.swarm.Swam");
      configFile = ConfigFile.findConfig(candidateNames);
    }

    if (configFile == null) {
      configFile = DEFAULT_CONFIG_FILE;
    }

    LOGGER.info("Using configuration file: " + configFile);

    // Load default metadata
    final List<String> candidateNames = new LinkedList<String>();
    candidateNames.add(Metadata.DEFAULT_METADATA_FILENAME);
    candidateNames.add(
        System.getProperty("user.home") + File.separatorChar + Metadata.DEFAULT_METADATA_FILENAME);

    String metadataConfigFile = ConfigFile.findConfig(candidateNames);
    if (metadataConfigFile == null) {
      metadataConfigFile = Metadata.DEFAULT_METADATA_FILENAME;
    } else {
      LOGGER.info("Using metadata configuration file: " + metadataConfigFile);
    }

    defaultMetadata = Metadata.loadMetadata(metadataConfigFile);
    
    // Parse configuration file
    final ConfigFile cf = new ConfigFile(configFile);
    cf.put("configFile", configFile, false);

    for (int i = 0; i <= n; i++) {
      if (args[i].startsWith("--")) {
        final String key = args[i].substring(2, args[i].indexOf('='));
        final String val = args[i].substring(args[i].indexOf('=') + 1);
        LOGGER.info("command line: " + key + " = " + val);
        cf.put(key, val, false);
      }
    }
    parseConfig(cf);

    loadDataSources();
    loadLayouts();
  }

  private void loadDataSources() {

    final List<String> candidateNames = new LinkedList<String>();
    candidateNames.add(DEFAULT_DATA_SOURCES_FILE);
    candidateNames
        .add(System.getProperty("user.home") + File.separatorChar + DEFAULT_DATA_SOURCES_FILE);
    final String configName = StringUtils.stringToString(ConfigFile.findConfig(candidateNames),
        DEFAULT_DATA_SOURCES_FILE);

    final ConfigFile cf = new ConfigFile(configName);
    final List<String> servers = cf.getList("server");
    if (servers != null) {
      for (final String server : servers) {
        final SeismicDataSource sds = DataSourceType.parseConfig(server);
        if (sds == null) {
          LOGGER.info("Skipping unknown data soruce " + server);
          continue;
        }

        sds.setStoreInUserConfig(false);
        sources.put(sds.getName(), sds);
      }
    }
  }

  private void loadLayouts() {
    layouts = new TreeMap<String, SwarmLayout>();

    final File[] files = new File("layouts").listFiles();
    if (files == null) {
      return;
    }

    for (final File f : files) {
      if (!f.isDirectory()) {
        final SwarmLayout sl = SwarmLayout.createSwarmLayout(f.getPath());
        if (sl != null) {
          layouts.put(sl.getName(), sl);
        }
      }
    }
  }

  public void addLayout(final SwarmLayout sl) {
    layouts.put(sl.getName(), sl);
  }

  public void removeLayout(final SwarmLayout layout) {
    layouts.remove(layout.getName());
    layout.delete();
  }

  public void removeMetadata(final String ch) {
    metadata.remove(ch);
  }

  public Map<String, Metadata> getMetadata() {
    return metadata;
  }

  public Metadata getMetadata(final String channel) {
    return getMetadata(channel, false);
  }

  /**
   * Get metadata.
   * @param channel waveform identifier
   * @param create true if creating new metadata
   * @return metadata
   */
  public Metadata getMetadata(final String channel, final boolean create) {
    Metadata md = metadata.get(channel);
    if (md == null) {
      md = defaultMetadata.get(channel);
    }
    if (md == null && create) {
      md = new Metadata(channel);
    }
    if (md != null) {
      metadata.put(channel, md);
    }
    return md;
  }

  /**
   * Assign metadata source.
   * @param channels waveform identifier
   * @param source seismic data source
   */
  public void assignMetadataSource(final Collection<String> channels,
      final SeismicDataSource source) {
    for (final String ch : channels) {
      final Metadata md = getMetadata(ch, true);
      md.source = source;
    }
  }

  /**
   * Sets Swarm configuration variables based on the contents of a ConfigFile; sets default values
   * if missing.
   *
   * @param config the configuration information
   */
  public void parseConfig(final ConfigFile config) {
    configFilename = config.getString("configFile");

    windowX = StringUtils.stringToInt(config.getString("windowX"), 10);
    windowY = StringUtils.stringToInt(config.getString("windowY"), 10);
    windowWidth = StringUtils.stringToInt(config.getString("windowSizeX"), 1000);
    windowHeight = StringUtils.stringToInt(config.getString("windowSizeY"), 700);
    windowMaximized = StringUtils.stringToBoolean(config.getString("windowMaximized"), false);

    chooserDividerLocation =
        StringUtils.stringToInt(config.getString("chooserDividerLocation"), 200);
    chooserVisible = StringUtils.stringToBoolean(config.getString("chooserVisible"), true);

    nearestDividerLocation =
        StringUtils.stringToInt(config.getString("nearestDividerLocation"), 600);

    specificTimeZone = TimeZone
        .getTimeZone(StringUtils.stringToString(config.getString("specificTimeZone"), "UTC"));
    useInstrumentTimeZone =
        StringUtils.stringToBoolean(config.getString("useInstrumentTimeZone"), true);
    useLocalTimeZone = StringUtils.stringToBoolean(config.getString("useLocalTimeZone"), true);

    useLargeCursor = StringUtils.stringToBoolean(config.getString("useLargeCursor"), false);

    span = StringUtils.stringToInt(config.getString("span"), 24);
    timeChunk = StringUtils.stringToInt(config.getString("timeChunk"), 30);

    lastPath = StringUtils.stringToString(config.getString("lastPath"), "default");

    kiosk = StringUtils.stringToString(config.getString("kiosk"), "false");

    saveConfig = StringUtils.stringToBoolean(config.getString("saveConfig"), true);

    durationEnabled = StringUtils.stringToBoolean(config.getString("durationEnabled"), false);
    durationA = StringUtils.stringToDouble(config.getString("durationA"), 1.86);
    durationB = StringUtils.stringToDouble(config.getString("durationB"), -0.85);

    pVelocity = StringUtils.stringToDouble(config.getString("pVelocity"), 6.0);

    showClip = StringUtils.stringToBoolean(config.getString("showClip"), true);
    alertClip = StringUtils.stringToBoolean(config.getString("alertClip"), false);
    alertClipTimeout = StringUtils.stringToInt(config.getString("alertClipTimeout"), 5);

    clipboardVisible = StringUtils.stringToBoolean(config.getString("clipboardVisible"), true);
    clipboardX = StringUtils.stringToInt(config.getString("clipboardX"), 25);
    clipboardY = StringUtils.stringToInt(config.getString("clipboardY"), 25);
    clipboardWidth = StringUtils.stringToInt(config.getString("clipboardSizeX"), 600);
    clipboardHeight = StringUtils.stringToInt(config.getString("clipboardSizeY"), 600);
    clipboardMaximized = StringUtils.stringToBoolean(config.getString("clipboardMaximized"), false);

    mapPath = StringUtils.stringToString(config.getString("mapPath"), "mapdata");
    mapVisible = StringUtils.stringToBoolean(config.getString("mapVisible"), true);
    mapX = StringUtils.stringToInt(config.getString("mapX"), 5);
    mapY = StringUtils.stringToInt(config.getString("mapY"), 5);

    mapWidth = StringUtils.stringToInt(config.getString("mapWidth"), 600);
    mapWidth = Math.max(mapWidth, 100);

    mapHeight = StringUtils.stringToInt(config.getString("mapHeight"), 510);
    mapHeight = Math.max(mapHeight, 100);

    mapMaximized = StringUtils.stringToBoolean(config.getString("mapMaximized"), false);

    mapScale = StringUtils.stringToDouble(config.getString("mapScale"), 80000);
    mapLongitude = StringUtils.stringToDouble(config.getString("mapLongitude"), -180);
    mapLatitude = StringUtils.stringToDouble(config.getString("mapLatitude"), 0);
    mapLineWidth = StringUtils.stringToInt(config.getString("mapLineWidth"), 2);
    mapLineColor = StringUtils.stringToInt(config.getString("mapLineColor"), 0x000000);

    useWMS = StringUtils.stringToBoolean(config.getString("useWMS"));
    wmsServer =
        StringUtils.stringToString(config.getString("wmsServer"), WMSGeoImageSet.DEFAULT_SERVER);
    wmsLayer =
        StringUtils.stringToString(config.getString("wmsLayer"), WMSGeoImageSet.DEFAULT_LAYER);
    wmsStyles =
        StringUtils.stringToString(config.getString("wmsStyles"), WMSGeoImageSet.DEFAULT_STYLE);
    
    hypocenterSource = HypocenterSource.valueOf(
        StringUtils.stringToString(config.getString("hypocenterSource"), "NONE"));

    fdsnDataselectURL = StringUtils.stringToString(config.getString("fdsnDataselectURL"),
        "http://service.iris.edu/fdsnws/dataselect/1/query");
    fdsnStationURL = StringUtils.stringToString(config.getString("fdsnStationURL"),
        "http://service.iris.edu/fdsnws/station/1/query");

    sources = new HashMap<String, SeismicDataSource>();
    final List<String> servers = config.getList("server");
    if (servers != null && servers.size() > 0) {
      for (final String server : servers) {
        // SeismicDataSource sds =
        // SeismicDataSource.getDataSource(server);
        final SeismicDataSource sds = DataSourceType.parseConfig(server);
        if (sds == null) {
          LOGGER.info("Skipping unknown data soruce " + server);
          continue;
        }
        sources.put(sds.getName(), sds);
      }
    } else {
      for (final String s : DEFAULT_SERVERS) {
        // SeismicDataSource sds = SeismicDataSource.getDataSource(s);
        final SeismicDataSource sds = DataSourceType.parseConfig(s);
        sources.put(sds.getName(), sds);
      }
    }

    userTimes = StringUtils.stringToString(config.getString("userTimes"), "").split(",");

    heliColorsString = StringUtils.stringToString(config.getString("heliColors"), "");
    if (heliColorsString != null) {
      if (heliColorsString.length() > 3) {
        final String[] color = heliColorsString.split(":");
        heliColors = new Color[color.length];
        for (int i = 0; i < color.length; i++) {
          final String[] parts = color[i].split(",");
          if (parts.length == 3) {
            final float red = Float.parseFloat(parts[0].trim());
            final float green = Float.parseFloat(parts[1].trim());
            final float blue = Float.parseFloat(parts[2].trim());
            try {
              heliColors[i] = new Color(red / 256, green / 256, blue / 256);
            } catch (final RuntimeException e) {
              heliColors[i] = Color.magenta;
            }
          } else {
            heliColors[i] = Color.magenta; // If the color is illegal, make it magenta
          }
        }
      }

    }
  }
  
  public Map<String, SeismicDataSource> getSources(){
    return sources;
  }

  public SeismicDataSource getSource(final String key) {
    return sources.get(key);
  }

  public boolean sourceExists(final String key) {
    return sources.containsKey(key);
  }

  public void addSource(final SeismicDataSource source) {
    sources.put(source.getName(), source);
  }

  public void removeSource(final String key) {
    sources.remove(key);
  }

  public double getDurationMagnitude(final double t) {
    return durationA * (Math.log(t) / Math.log(10)) + durationB;
  }

  /**
   * Get time zone.
   * @param channel waveform id
   * @return time zone
   */
  public TimeZone getTimeZone(final String channel) {
    if (useInstrumentTimeZone && channel != null) {
      final Metadata md = getMetadata(channel, false);
      if (md != null && md.getTimeZone() != null) {
        return md.getTimeZone();
      }
    }

    if (useLocalTimeZone) {
      return TimeZone.getDefault();
    } else {
      return specificTimeZone;
    }
  }

  public boolean isKiosk() {
    return !kiosk.toLowerCase().equals("false");
  }

  /**
   * Create ConfigFile object.
   * @return config file
   */
  public ConfigFile toConfigFile() {
    final ConfigFile config = new ConfigFile();
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
    
    config.put("pVelocity", Double.toString(pVelocity));

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

    config.put("hypocenterSource", hypocenterSource.name());

    config.put("fdsnDataselectURL", fdsnDataselectURL);
    config.put("fdsnStationURL", fdsnStationURL);

    final List<String> servers = new ArrayList<String>();
    for (final SeismicDataSource sds : sources.values()) {
      if (sds.isStoreInUserConfig()) {
        servers.add(sds.toConfigString());
      }
    }

    config.putList("server", servers);

    final StringBuilder utsb = new StringBuilder();
    for (int i = 0; i < userTimes.length - 1; i++) {
      utsb.append(userTimes[i]);
      utsb.append(",");
    }
    if (userTimes.length > 0) {
      utsb.append(userTimes[userTimes.length - 1]);
    }
    config.put("userTimes", utsb.toString());

    if (heliColorsString != null) {
      if (heliColorsString.length() > 3) {
        config.put("heliColors", heliColorsString);
      }
    }
    return config;
  }

  @Override
  public String toString() {
    return toConfigFile().toString();
  }

  private static class SwarmConfigHolder {
    public static SwarmConfig swarmConfig = new SwarmConfig();
  }

  /**
   * Get Swarm user.
   * @return username
   */
  public String getUser() {
    if (user == null) {
      user = System.getProperty("user.name");
    }
    return user;
  }

}
