package gov.usgs.volcanoes.swarm.data;

import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesSource;
import gov.usgs.volcanoes.swarm.data.seedlink.SeedLinkSource;

/**
 * Enumerate known types of SeismicDataSources and their short names.
 * 
 * @author Tom Parker
 */
public enum DataSourceType {
  WAVE_SERVER_V("ws", WaveServerSource.class), 
  WINSTON_WAVE_SERVER("wws", WwsSource.class), 
  WINSTON_DIRECT("wwsd", DirectWwsSource.class), 
  CACHE("cache", CachedDataSource.class), 
  FDSN_WS("wsc", WebServicesSource.class), 
  SEED_LINK("sls", SeedLinkSource.class);

  public String shortName;
  public Class<? extends SeismicDataSource> seismicDataSource;

  /**
   * Data source type constructor.
   * @param shortName data source type short name
   * @param seismicDataSource seismic data source
   */
  private DataSourceType(String shortName, Class<? extends SeismicDataSource> seismicDataSource) {
    this.shortName = shortName;
    this.seismicDataSource = seismicDataSource;
  }

  /**
   * Get short name for data source type.
   * @param sds seismic data source
   * @return name string
   */
  public static String getShortName(Class<? extends SeismicDataSource> sds) {
    for (DataSourceType type : DataSourceType.values()) {
      if (type.seismicDataSource.equals(sds)) {
        return type.shortName;
      }
    }

    return ("Unknown data type" + sds.getClass().getName());
  }

  /**
   * Parse data source.
   * @param s data source string
   * @return data source
   */
  public static DataSourceType parse(String s) {
    for (DataSourceType type : DataSourceType.values()) {
      if (type.shortName.equals(s)) {
        return type;
      }
    }
    throw new IllegalArgumentException("No known DataSource " + s);
  }

  /**
   * Parse config for seismic data source.
   * @param config data source config string
   * @return seismic data source
   */
  public static SeismicDataSource parseConfig(String config) {
    String name = config.substring(0, config.indexOf(";"));
    config = config.substring(config.indexOf(";") + 1);
    String type = config.substring(0, config.indexOf(":"));
    String params = config.substring(config.indexOf(":") + 1);

    SeismicDataSource sds = null;
    try {
      DataSourceType dataSourceType = DataSourceType.parse(type);
      sds = dataSourceType.seismicDataSource.newInstance();
      sds.setName(name);
      sds.parse(params);
    } catch (Exception e) {
      ; // do nothing
    }
    return sds;
  }

  public String toString() {
    return shortName;
  }
}
