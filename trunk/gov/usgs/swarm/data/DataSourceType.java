package gov.usgs.swarm.data;

import gov.usgs.swarm.data.fdsnWs.WebServicesSource;
import gov.usgs.swarm.data.seedLink.SeedLinkSource;

/**
 * Enumerate known types of SeismicDataSources and their short names.
 * 
 * @author Tom Parker
 */
public enum DataSourceType {
	WAVE_SERVER_V("ws", WaveServerSource.class), 
	WINSTON_WAVE_SERVER("wws", WWSSource.class), 
	WINSTON_DIRECT("wwsd", DirectWWSSource.class), 
	CACHE("cache", CachedDataSource.class), 
	FDSN_WS("wsc", WebServicesSource.class), 
	SEED_LINK("sls", SeedLinkSource.class);

	public String shortName;
	public Class<? extends SeismicDataSource> seismicDataSource;

	/**
	 * 
	 * @param shortName
	 * @param seismicDataSource
	 */
	private DataSourceType(String shortName, Class<? extends SeismicDataSource> seismicDataSource) {
		this.shortName = shortName;
		this.seismicDataSource = seismicDataSource;
	}

	public Class<? extends SeismicDataSource> getSDS() {
		return seismicDataSource;
	}

	public static String getShortName(Class<? extends SeismicDataSource> sds) {
		for (DataSourceType type : DataSourceType.values())
			if (type.seismicDataSource.equals(sds))
				return type.shortName;

		return ("Unknown data type" + sds.getClass().getName());
	}

	public static DataSourceType parse(String s) {
		for (DataSourceType type : DataSourceType.values())
			if (type.shortName.equals(s))
				return type;

		throw new IllegalArgumentException("No known DataSource " + s);
	}

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
