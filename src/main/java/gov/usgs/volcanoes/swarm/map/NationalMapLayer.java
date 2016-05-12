package gov.usgs.volcanoes.swarm.map;

/**
 * 
 * http://basemap.nationalmap.gov/arc
 * 
 * @author Tom Parker
 *
 */
public enum NationalMapLayer {

  TOPO("Topo", "http://basemap.nationalmap.gov/arcgis/services/USGSTopo/MapServer/WMSServer?"), 
  SHADED_RELIEF("Shaded Relief",  "http://basemap.nationalmap.gov/arcgis/services/USGSShadedReliefOnly/MapServer/WMSServer?"), 
  IMAGERY_ONLY( "Imagery Only", "http://basemap.nationalmap.gov/arcgis/services/USGSImageryOnly/MapServer/WMSServer?"),
  IMAGERY_TOPO("Imagery Topo", "http://basemap.nationalmap.gov/arcgis/services/USGSTopo/USGSImageryTopo/WMSServer?"),
  ;

  public final String server;
  public final String layer = "0";
  public final String sytels = "";

  private final String title;

  private NationalMapLayer(String title, String server) {
    this.title = title;
    this.server = server;
  }

  public String toString() {
    return title;
  }
}
