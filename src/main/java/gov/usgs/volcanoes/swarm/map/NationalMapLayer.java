package gov.usgs.volcanoes.swarm.map;

/**
 * https://basemap.nationalmap.gov/arc
 * 
 * @author Tom Parker
 *
 */
public enum NationalMapLayer {

  TOPO("Topo", "https://basemap.nationalmap.gov/arcgis/services/USGSTopo/MapServer/WMSServer?"), 
  SHADED_RELIEF("Shaded Relief",  "https://basemap.nationalmap.gov/arcgis/services/USGSShadedReliefOnly/MapServer/WMSServer?"), 
  IMAGERY_ONLY("Imagery Only", "https://basemap.nationalmap.gov/arcgis/services/USGSImageryOnly/MapServer/WMSServer?"),
  IMAGERY_TOPO("Imagery Topo", "https://basemap.nationalmap.gov/arcgis/services/USGSImageryTopo/MapServer/WMSServer?"),
  OTHER("Other","")
  ;

  public final String server;
  public final String layer = "0";
  public final String style = "";

  private final String title;

  private NationalMapLayer(String title, String server) {
    this.title = title;
    this.server = server;
  }

  public String toString() {
    return title;
  }

  /**
   * Get layer from server URL.
   * @param server server URL.
   * @return layer
   */
  public static NationalMapLayer getFromServer(String server) {
    for (NationalMapLayer layer : NationalMapLayer.values()) {
      if (layer.server.equals(server)) {
        return layer;
      }
    }
    return null;
  }
}
