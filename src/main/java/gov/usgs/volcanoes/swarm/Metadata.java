package gov.usgs.volcanoes.swarm;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.math.proj.Projection;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.Pair;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;

/**
 * Metadata class.
 * 
 * @author Dan Cervelli
 */
public class Metadata implements Comparable<Metadata> {
  public static final String DEFAULT_METADATA_FILENAME = "SwarmMetadata.config";

  //private static final Logger LOGGER = LoggerFactory.getLogger(Metadata.class);

  private SCNL scnl;

  private String channel = null;

  private String alias = null;
  private String unit = null;

  private double multiplier = 1;
  private double offset = 0;
  private boolean linearSet = false;

  private double longitude = Double.NaN;
  private double latitude = Double.NaN;
  private double height = Double.NaN;
  private double minTime = Double.NaN;
  private double maxTime = Double.NaN;
  
  private double delay = Double.NaN;
  private double xmagCorrection = Double.NaN;
  private double fmagCorrection = Double.NaN;

  public SeismicDataSource source;

  private TimeZone timeZone = null;
  private boolean tzSet = false;

  private Set<String> groups = null;

  public Map<String, String> ancillaryMetadata = null;

  // public Metadata()
  // {}

  public Metadata(String ch) {
    updateChannel(ch);
  }

  /**
   * Determine if metadata has been touched.
   * @return true if so
   */
  public boolean isTouched() {
    return !(alias == null && unit == null && multiplier == 1 && offset == 0
        && Double.isNaN(longitude) && Double.isNaN(latitude) && Double.isNaN(height)
        && tzSet == false && groups == null && ancillaryMetadata == null);
  }

  /**
   * Update time zone.
   * @param tz time zone
   */
  public void updateTimeZone(String tz) {
    if (!tzSet && tz != null && tz.length() >= 1) {
      timeZone = TimeZone.getTimeZone(tz);
      tzSet = true;
    }
  }

  /**
   * Update channel.
   * @param ch channel
   */
  public void updateChannel(String ch) {
    if (channel == null) {
      channel = ch;
      scnl = new SCNL(channel);
    }
  }

  /**
   * Update alias.
   * @param a alias
   */
  public void updateAlias(String a) {
    if (alias == null) {
      alias = a;
    }
  }

  /**
   * Update units.
   * @param u unit
   */
  public void updateUnits(String u) {
    if (unit == null) {
      unit = u;
    }
  }

  /**
   * Update linear coefficients.
   * @param mult multiplier
   * @param off offset
   */
  public void updateLinearCoefficients(double mult, double off) {
    if (!linearSet) {
      if (!Double.isNaN(mult)) {
        multiplier = mult;
      }
      if (!Double.isNaN(off)) {
        offset = off;
      }
      linearSet = true;
    }
  }

  /**
   * Update longitude.
   * @param lon longitude
   */
  public void updateLongitude(double lon) {
    if (Double.isNaN(longitude)) {
      longitude = lon;
    }
  }

  /**
   * Update latitude.
   * @param lat latitude
   */
  public void updateLatitude(double lat) {
    if (Double.isNaN(latitude)) {
      latitude = lat;
    }
  }

  /**
   * Update height.
   * @param h height
   */
  public void updateHeight(double h) {
    if (Double.isNaN(height)) {
      height = h;
    }
  }

  public void updateMinTime(double minTime) {
    this.minTime = minTime;
  }

  public void updateMaxTime(double maxTime) {
    this.maxTime = maxTime;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public double getMultiplier() {
    return multiplier;
  }

  public double getOffset() {
    return offset;
  }

  public String getAlias() {
    return alias;
  }

  public String getUnit() {
    return unit;
  }

  public String getChannel() {
    return channel;
  }

  public SCNL getSCNL() {
    return scnl;
  }

  public boolean hasLonLat() {
    return (!Double.isNaN(longitude) && longitude != -999 && !Double.isNaN(latitude)
        && latitude != -999);
  }

  public double getLongitude() {
    return longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  /**
   * Get min time.
   * @return min time or NaN
   */
  public double getMinTime() {
    if (minTime == 1E300 || minTime == -1E300) {
      return Double.NaN;
    } else {
      return minTime;
    }
  }

  /**
   * Get max time.
   * @return max time or NaN
   */
  public double getMaxTime() {
    if (maxTime == 1E300 || maxTime == -1E300) {
      return Double.NaN;
    } else {
      return maxTime;
    }
  }

  /**
   * Add group.
   * @param g group
   */
  public void addGroup(String g) {
    if (groups == null) {
      groups = new HashSet<String>();
    }
    groups.add(g);
  }

  public Set<String> getGroups() {
    return groups;
  }

  /**
   * Interpret metadata string.
   * @param s string
   */
  public void interpret(String s) {
    String[] kv = new String[2];
    kv[0] = s.substring(0, s.indexOf(":")).trim();
    kv[1] = s.substring(s.indexOf(":") + 1).trim();
    if (kv[0].equals("Alias")) {
      alias = kv[1];
    } else if (kv[0].equals("Unit")) {
      unit = kv[1];
    } else if (kv[0].equals("Multiplier")) {
      multiplier = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("Offset")) {
      offset = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("Longitude")) {
      longitude = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("Latitude")) {
      latitude = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("Delay")) {
      delay = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("FMAG Correction")) {
      fmagCorrection = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("XMAG Correction")) {
      xmagCorrection = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("Height")) {
      height = Double.parseDouble(kv[1]);
    } else if (kv[0].equals("TimeZone")) {
      timeZone = TimeZone.getTimeZone(kv[1]);
    } else if (kv[0].equals("Group")) {
      addGroup(kv[1]);
    } else {
      if (ancillaryMetadata == null) {
        ancillaryMetadata = new HashMap<String, String>();
      }
      ancillaryMetadata.put(kv[0], kv[1]);
    }
  }

  public double getLocationHashCode() {
    return longitude * 100000 + latitude;
  }

  /**
   * Load metadata.
   * @param fn metadata configuration file name
   * @return map of metadata
   */
  public static Map<String, Metadata> loadMetadata(String fn) {
    Map<String, Metadata> data = new HashMap<String, Metadata>();
    ConfigFile cf = new ConfigFile(fn);
    Map<String, List<String>> config = cf.getConfig();

    for (String key : config.keySet()) {
      Metadata md = data.get(key);
      if (md == null) {
        md = new Metadata(key);
        data.put(key, md);
      } else {
        md = data.get(key);
      }

      for (String value : config.get(key)) {
        for (String item : value.split(";")) {
          md.interpret(item);
        }
      }
    }

    return data;
  }

  
  /**
   * Update metadata given a Channel.
   * @param chan source channel
   */
  public void update(Channel chan) {
    
    Instrument ins = chan.instrument;
    updateLongitude(ins.longitude);
    updateLatitude(ins.latitude);
    
    TimeSpan timeSpan = chan.timeSpan;
    updateMinTime(J2kSec.fromEpoch(timeSpan.startTime));
    updateMaxTime(J2kSec.fromEpoch(timeSpan.endTime));
    
    if (getGroups() != null) {
      getGroups().clear();
    }
    
    List<String> groups = chan.groups;
    if (groups != null) {
      for (String g : groups) {
        addGroup(g);
      }
    }

    updateLinearCoefficients(chan.linearA, chan.linearB);
    updateAlias(chan.alias);
    updateUnits(chan.unit);
    updateTimeZone(ins.timeZone);

  }
  
  
  /**
   * Get distance to another point.
   * @param pt the point
   * @return distance in meters
   */
  public double distanceTo(Point2D.Double pt) {
    if (pt == null || Double.isNaN(latitude) || Double.isNaN(longitude) || Double.isNaN(pt.x)
        || Double.isNaN(pt.y)) {
      return Double.NaN;
    }

    return Projection.distanceBetweenM(new Point2D.Double(longitude, latitude), pt);
  }

  /**
   * Get distance to another station.
   * @param other metadata of other station
   * @return distance in meters
   */
  public double distanceTo(Metadata other) {
    if (other == null || Double.isNaN(latitude) || Double.isNaN(longitude)
        || Double.isNaN(other.latitude) || Double.isNaN(other.longitude)) {
      return Double.NaN;
    }

    return Projection.distanceBetweenM(new Point2D.Double(longitude, latitude),
        new Point2D.Double(other.longitude, other.latitude));
  }

  /**
   * Get distance comparator.
   * @return comparator
   */
  public static Comparator<Pair<Double, String>> getDistanceComparator() {
    return new Comparator<Pair<Double, String>>() {
      public int compare(Pair<Double, String> o1, Pair<Double, String> o2) {
        if (Math.abs(o1.item1 - o2.item1) < 0.00001) {
          return o1.item2.compareTo(o2.item2);
        } else {
          return Double.compare(o1.item1, o2.item1);
        }
      }
    };
  }

  /**
   * Find nearest stations to point.
   * @param metadata map of metadata
   * @param pt point 
   * @param requireDs true if data source required
   * @return list of dance and channels
   */
  @Deprecated
  public static List<Pair<Double, String>> findNearest(Map<String, Metadata> metadata,
      Point2D.Double pt, boolean requireDs) {
    ArrayList<Pair<Double, String>> result = new ArrayList<Pair<Double, String>>();
    synchronized (metadata) {
      for (String key : metadata.keySet()) {
        Metadata md = metadata.get(key);
        if (md.hasLonLat() && (!requireDs || md.source != null)) {
          double d = md.distanceTo(pt);
          if (!Double.isNaN(d) && d > 0) {
            result.add(new Pair<Double, String>(new Double(d), md.channel));
          }
        }
      }
    }
    Collections.sort(result, getDistanceComparator());
    return result.size() == 0 ? null : result;
  }

  /**
   * Find nearest stations to channel.
   * @param metadata map of metadata
   * @param channel channel name
   * @return list of distance and channels
   */
  public static List<Pair<Double, String>> findNearest(Map<String, Metadata> metadata,
      String channel) {
    Metadata md = metadata.get(channel);
    if (md == null || Double.isNaN(md.latitude) || Double.isNaN(md.longitude)) {
      return null;
    }

    ArrayList<Pair<Double, String>> result = new ArrayList<Pair<Double, String>>();
    for (String key : metadata.keySet()) {
      Metadata other = metadata.get(key);
      double d = md.distanceTo(other);
      if (!Double.isNaN(d) && !other.channel.equals(channel)) {
        result.add(new Pair<Double, String>(new Double(d), other.channel));
      }
    }
    Collections.sort(result, getDistanceComparator());
    return result.size() == 0 ? null : result;
  }

  public String toString() {
    return channel + "," + alias + "," + unit + "," + multiplier + "," + offset + "," + longitude
        + "," + latitude + "," + height + "," + timeZone + ", " + delay + ", "
        + xmagCorrection + ", " + fmagCorrection;
  }

  public int compareTo(Metadata o) {
    return channel.compareTo(o.channel);
  }

  public Point2D.Double getLonLat() {
    return new Point2D.Double(longitude, latitude);
  }

  public double getDelay() {
    return delay;
  }

  public double getXmagCorrection() {
    return xmagCorrection;
  }

  public double getFmagCorrection() {
    return fmagCorrection;
  }

  public double getHeight() {
    return height;
  }
}
