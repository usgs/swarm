package gov.usgs.volcanoes.swarm;

/**
 * SCNL data class.
 * 
 * @author Dan Cervelli
 */
public class Scnl {
  public String station;
  public String channel;
  public String network;
  public String location;

  /**
   * Constructor.
   * 
   * @param s channel string
   */
  public Scnl(String s) {
    String[] ss = s.split(" ");
    switch (ss.length) {
      case 4:
        location = ss[3];
      case 3:
        network = ss[2];
      case 2:
        channel = ss[1];
      default:
        station = ss[0];
    }
  }

  public String toString() {
    return station + " " + channel + " " + network + (location != null ? (" " + location) : "");
  }
}
