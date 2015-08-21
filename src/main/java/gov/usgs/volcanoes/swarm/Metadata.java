package gov.usgs.volcanoes.swarm;

import gov.usgs.proj.Projection;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Pair;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

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

/**
 * 
 * 
 * @author Dan Cervelli
 */
public class Metadata implements Comparable<Metadata> {
    public static final String DEFAULT_METADATA_FILENAME = "SwarmMetadata.config";

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

    public boolean isTouched() {
        return !(alias == null && unit == null && multiplier == 1 && offset == 0 && Double.isNaN(longitude)
                && Double.isNaN(latitude) && Double.isNaN(height) && tzSet == false && groups == null && ancillaryMetadata == null);
    }

    public void updateTimeZone(String tz) {
        if (!tzSet && tz != null && tz.length() >= 1) {
            timeZone = TimeZone.getTimeZone(tz);
            tzSet = true;
        }
    }

    public void updateChannel(String ch) {
        if (channel == null) {
            channel = ch;
            scnl = new SCNL(channel);
        }
    }

    public void updateAlias(String a) {
        if (alias == null)
            alias = a;
    }

    public void updateUnits(String u) {
        if (unit == null)
            unit = u;
    }

    public void updateLinearCoefficients(double mult, double off) {
        if (!linearSet) {
            if (!Double.isNaN(mult))
                multiplier = mult;
            if (!Double.isNaN(off))
                offset = off;
            linearSet = true;
        }
    }

    public void updateLongitude(double lon) {
        if (Double.isNaN(longitude))
            longitude = lon;
    }

    public void updateLatitude(double lat) {
        if (Double.isNaN(latitude))
            latitude = lat;
    }

    public void updateHeight(double h) {
        if (Double.isNaN(height))
            height = h;
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
        return (!Double.isNaN(longitude) && longitude != -999 && !Double.isNaN(latitude) && latitude != -999);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getMinTime() {
        if (minTime == 1E300 || minTime == -1E300)
            return Double.NaN;
        else
            return minTime;
    }

    public double getMaxTime() {
        if (maxTime == 1E300 || maxTime == -1E300)
            return Double.NaN;
        else
            return maxTime;
    }

    public void addGroup(String g) {
        if (groups == null)
            groups = new HashSet<String>();
        groups.add(g);
    }

    public Set<String> getGroups() {
        return groups;
    }

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
        } else if (kv[0].equals("Height")) {
            height = Double.parseDouble(kv[1]);
        } else if (kv[0].equals("TimeZone")) {
            timeZone = TimeZone.getTimeZone(kv[1]);
        } else if (kv[0].equals("Group")) {
            addGroup(kv[1]);
        } else {
            if (ancillaryMetadata == null)
                ancillaryMetadata = new HashMap<String, String>();
            ancillaryMetadata.put(kv[0], kv[1]);
        }
    }

    public double getLocationHashCode() {
        return longitude * 100000 + latitude;
    }

    public static Map<String, Metadata> loadMetadata(String fn) {
        Map<String, Metadata> data = new HashMap<String, Metadata>();
        ConfigFile cf = new ConfigFile(fn);

        Map<String, List<String>> config = cf.getConfig();

        for (String key : config.keySet()) {
            Metadata md = data.get(key);
            if (md == null) {
                md = new Metadata(key);
                data.put(key, md);
            } else
                md = data.get(key);

            for (String value : config.get(key)) {
                for (String item : value.split(";")) {
                    md.interpret(item);
                }
            }
        }

        return data;
    }

    public double distanceTo(Point2D.Double pt) {
        if (pt == null || Double.isNaN(latitude) || Double.isNaN(longitude) || Double.isNaN(pt.x) || Double.isNaN(pt.y))
            return Double.NaN;

        return Projection.distanceBetween(new Point2D.Double(longitude, latitude), pt);
    }

    public double distanceTo(Metadata other) {
        if (other == null || Double.isNaN(latitude) || Double.isNaN(longitude) || Double.isNaN(other.latitude)
                || Double.isNaN(other.longitude))
            return Double.NaN;

        return Projection.distanceBetween(new Point2D.Double(longitude, latitude), new Point2D.Double(other.longitude,
                other.latitude));
    }

    public static Comparator<Pair<Double, String>> getDistanceComparator() {
        return new Comparator<Pair<Double, String>>() {
            public int compare(Pair<Double, String> o1, Pair<Double, String> o2) {
                if (Math.abs(o1.item1 - o2.item1) < 0.00001)
                    return o1.item2.compareTo(o2.item2);
                else
                    return Double.compare(o1.item1, o2.item1);
            }
        };
    }

    public static List<Pair<Double, String>> findNearest(Map<String, Metadata> metadata, Point2D.Double pt,
            boolean requireDS) {
        ArrayList<Pair<Double, String>> result = new ArrayList<Pair<Double, String>>();
        synchronized (metadata) {
            for (String key : metadata.keySet()) {
                Metadata md = metadata.get(key);
                if (md.hasLonLat() && (!requireDS || md.source != null)) {
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

    public static List<Pair<Double, String>> findNearest(Map<String, Metadata> metadata, String channel) {
        Metadata md = metadata.get(channel);
        if (md == null || Double.isNaN(md.latitude) || Double.isNaN(md.longitude))
            return null;

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
        return channel + "," + alias + "," + unit + "," + multiplier + "," + offset + "," + longitude + "," + latitude
                + "," + height + "," + timeZone;
    }

    public int compareTo(Metadata o) {
        return channel.compareTo(o.channel);
    }

    public Point2D.Double getLonLat() {
        return new Point2D.Double(longitude, latitude);
    }
}
