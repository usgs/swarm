package gov.usgs.volcanoes.swarm.data;

/**
 * An implementation of <code>SeismicDataSource</code> that is used by Swarm to cache all data that
 * it comes across.
 * 
 * @author Dan Cervelli
 */
public class CachedDataSource extends AbstractCachingDataSource {

  private CachedDataSource(String name) {
    super();
  }

  private static class CachedDataSourceHolder {
    private static final CachedDataSource INSTANCE = new CachedDataSource("singleton");
  }

  public static CachedDataSource getInstance() {
    return CachedDataSourceHolder.INSTANCE;
  }

  public String toConfigString() {
    return "cache:";
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }
}
