package gov.usgs.swarm.data;

/**
 * An implementation of <code>SeismicDataSource</code> that is used by Swarm to
 * cache all data that it comes across.
 * 
 * @author Dan Cervelli
 */
public class CachedDataSource extends AbstractCachingDataSource {

	private CachedDataSource() {
		super();
	}

	private static class CachedDataSourceHolder {
		private static final CachedDataSource INSTANCE = new CachedDataSource();
	}

	public static CachedDataSource getInstance() {
		return CachedDataSourceHolder.INSTANCE;
	}

	public String toConfigString() {
		return "cache:";
	}
}