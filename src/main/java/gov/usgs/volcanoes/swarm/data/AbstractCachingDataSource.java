package gov.usgs.volcanoes.swarm.data;

import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.data.Wave;
import gov.usgs.util.Log;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.swarm.Swarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFrame;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * An implementation of <code>SeismicDataSource</code> which caches data that
 * passes through it. Requires another SeismicDataSource to provide data
 * 
 * Mostly build with Dan's code pulled from CachedDataSource
 * 
 * @author Tom Parker
 */
public abstract class AbstractCachingDataSource extends SeismicDataSource implements RsamSource {

  /** roughly max size of a single wave in bytes. Actually, (numSamples * 4) */
  private static final int MAX_WAVE_SIZE = 1000000;

  /** microsecond conversion */
  protected static final long TO_USEC = (long) 1E6;

  /** microsecond conversion */
  protected static final double FROM_USEC = 1E-6;

  protected long maxSize;
  protected Map<String, List<CachedHelicorder>> helicorderCache;
  protected Map<String, List<CachedWave>> waveCache;
  protected Map<String, List<CachedRsam>> rsamCache;
  protected CachePurgeAction[] purgeActions;
  protected static Logger logger;
  protected static final JFrame applicationFrame = Swarm.getApplicationFrame();

  public AbstractCachingDataSource() {
    helicorderCache = new HashMap<String, List<CachedHelicorder>>();
    waveCache = new HashMap<String, List<CachedWave>>();
    rsamCache = new HashMap<String, List<CachedRsam>>();
    maxSize = Runtime.getRuntime().maxMemory() / 6;
    logger = Log.getLogger("gov.usgs.swarm");
    createPurgeActions();
  }

  public void parse(String params) {
    // no-op
  }

  public void flush() {
    flushWaves();
    flushHelicorders();
    System.gc();
  }

  public boolean isActiveSource() {
    return false;
  }

  public synchronized long getSize() {
    long size = getSize(waveCache);
    size += getSize(helicorderCache);
    size += getSize(rsamCache);
    return size;
  }

  private synchronized <T extends CacheEntry> long getSize(Map<String, List<T>> cache) {
    long size = 0;
    for (String key : cache.keySet()) {
      List<T> cwl = cache.get(key);

      for (T ce : cwl)
        size += ce.getMemorySize();
    }
    return size;
  }

  public void output() {
    long size = outputCache("Wave", waveCache);
    size += outputCache("Helicorder", helicorderCache);
    System.out.println("Wave Last Access Order:");
    List<CacheEntry> wl = getEntriesByLastAccess(waveCache);
    for (CacheEntry ce : wl)
      System.out.println(ce.getInfoString());

    System.out.println("Helicorder Last Access Order:");
    List<CacheEntry> hl = getEntriesByLastAccess(helicorderCache);
    for (CacheEntry ce : hl)
      System.out.println(ce.getInfoString());

    System.out.println("Total size: " + size + " bytes");
  }

  private synchronized <T extends CacheEntry> List<CacheEntry> getEntriesByLastAccess(
      Map<String, List<T>> cache) {
    List<CacheEntry> cl = new ArrayList<CacheEntry>();
    for (String key : cache.keySet()) {
      List<T> cwl = cache.get(key);
      for (T ce : cwl)
        cl.add(ce);
    }

    return cl;
  }

  public void createPurgeActions() {
    purgeActions = new CachePurgeAction[] {
        // purge anything that hasn't been hit in 5 minutes
        new TimeLimitWavePurgeAction(waveCache, 5 * 60 * 1000),
        new TimeLimitHelicorderPurgeAction(helicorderCache, 5 * 60 * 1000),
        new TimeLimitRsamPurgeAction(rsamCache, 5 * 60 * 1000),

        // cut waves larger than 3 hours in half keeping latest half
        new HalveLargeWavesPurgeAction(waveCache, 3 * 60 * 60),

        // cut waves larger than 1 hour in half
        new HalveLargeWavesPurgeAction(waveCache, 60 * 60),

        // getting to the last resort, purge wave cache
        new CompleteWavePurgeAction(waveCache),

        // should halve large helis

        // nothing left to do, purge helicorder cache
        new CompleteHelicorderPurgeAction(helicorderCache)};
  }

  /** TODO: maybe this should be an observer? */
  private synchronized void enforceSize() {
    if (purgeActions == null)
      return;

    long target = getSize() - maxSize;
    int i = 0;
    while (target > 0 && i < purgeActions.length) {
      long chunk = purgeActions[i].purge();
      Log.getLogger("gov.usgs.swarm").finer("purged " + chunk + " bytes from cache");
      target -= chunk;
      i++;
    }
  }

  private synchronized void putWaveInCache(String channel, Wave wave, List<CachedWave> waves) {
    if (wave.getMemorySize() > MAX_WAVE_SIZE) {
      Wave[] splitWaves = wave.split();
      putWaveInCache(channel, splitWaves[0], waves);
      putWaveInCache(channel, splitWaves[1], waves);
      return;
    }
    CachedWave cw = new CachedWave();
    cw.station = channel;
    cw.t1 = wave.getStartTime();
    cw.t2 = wave.getEndTime();
    cw.wave = wave;
    cw.lastAccess = System.currentTimeMillis();
    waves.add(cw);
    enforceSize();
  }

  public void putRsam(String station, RSAMData rsamData) {
    List<CachedRsam> rsams = rsamCache.get(station);
    if (rsams == null) {
      rsams = new ArrayList<CachedRsam>();
      CachedRsam cr = new CachedRsam();
      cr.station = station;
      cr.t1 = rsamData.getStartTime();
      cr.t2 = rsamData.getEndTime();
      cr.rsamData = rsamData;
      cr.lastAccess = System.currentTimeMillis();
      rsams.add(cr);
      rsamCache.put(station, rsams);
      enforceSize();
    } else {
      boolean add = true;
      for (int i = 0; i < rsams.size(); i++) {
        CachedRsam ch = rsams.get(i);
        if (ch.rsamData.overlaps(rsamData) && rsamData != ch.rsamData) {
          rsams.remove(ch);
          RSAMData newRsam = ch.rsamData.combine(rsamData);
          putRsam(station, newRsam);
          rsamData = newRsam;
          i = 0;
          add = false;
        }
      }

      if (add) {
        CachedRsam ch = new CachedRsam();
        ch.station = station;
        ch.t1 = rsamData.getStartTime();
        ch.t2 = rsamData.getEndTime();
        ch.rsamData = rsamData;
        ch.lastAccess = System.currentTimeMillis();
        rsams.add(ch);
        enforceSize();
      }
    }
  }

  public synchronized void putHelicorder(String station, HelicorderData helicorder) {
    List<CachedHelicorder> helis = helicorderCache.get(station);
    if (helis == null) {
      helis = new ArrayList<CachedHelicorder>();
      CachedHelicorder ch = new CachedHelicorder();
      ch.station = station;
      ch.t1 = helicorder.getStartTime();
      ch.t2 = helicorder.getEndTime();
      ch.helicorder = helicorder;
      ch.lastAccess = System.currentTimeMillis();
      helis.add(ch);
      helicorderCache.put(station, helis);
      enforceSize();
    } else {
      boolean add = true;
      for (int i = 0; i < helis.size(); i++) {
        CachedHelicorder ch = helis.get(i);

        if (ch.helicorder.overlaps(helicorder)) {
          HelicorderData newHeli = ch.helicorder.combine(helicorder);
          ch.t1 = newHeli.getStartTime();
          ch.t2 = newHeli.getEndTime();
          ch.helicorder = newHeli;
          ch.lastAccess = System.currentTimeMillis();
          
          helicorder = newHeli;
          i = 0;
          add = false;
          break;
        }
      }

      if (add) {
        CachedHelicorder ch = new CachedHelicorder();
        ch.station = station;
        ch.t1 = helicorder.getStartTime();
        ch.t2 = helicorder.getEndTime();
        ch.helicorder = helicorder;
        ch.lastAccess = System.currentTimeMillis();
        helis.add(ch);
        enforceSize();
      }
    }
  }

  public synchronized void cacheWaveAsHelicorder(String station, Wave wave) {
    if (inHelicorderCache(station, wave.getStartTime(), wave.getEndTime()))
      return;

    int seconds = (int) Math.ceil(wave.numSamples() * wave.getSamplingPeriod());
    DoubleMatrix2D data = DoubleFactory2D.dense.make(seconds, 3);
    for (int i = 0; i < seconds; i++) {
      data.setQuick(i, 1, Integer.MAX_VALUE);
      data.setQuick(i, 2, Integer.MIN_VALUE);
    }

    int sPeriod = (int) (wave.getSamplingPeriod() * TO_USEC);
    long startTime = (long) (wave.getStartTime() * TO_USEC);
    for (int sampleIndex = 0; sampleIndex < wave.numSamples(); sampleIndex++) {
      long sampleTime = startTime + sampleIndex * sPeriod;
      int sample = wave.buffer[sampleIndex];
      
      if (sample != Wave.NO_DATA) {
        int secondIndex = (int) ((sampleTime - startTime) / TO_USEC);
        data.setQuick(secondIndex, 0, sampleTime / TO_USEC);
        data.setQuick(secondIndex, 1, Math.min(data.getQuick(secondIndex, 1), sample));
        data.setQuick(secondIndex, 2, Math.max(data.getQuick(secondIndex, 2), sample));
      }
      
    }

    for (int i = 0; i < seconds; i++) {
      double min = data.getQuick(i, 1);
      if (min == Integer.MAX_VALUE)
        data.setQuick(i,  1,  wave.mean());
      
      double max = data.getQuick(i, 2);
      if (max == Integer.MIN_VALUE)
        data.setQuick(i,  2,  wave.mean());
    }

    HelicorderData hd = new HelicorderData();
    hd.setData(data);
    putHelicorder(station, hd);
  }

  public boolean isEmpty() {
    return helicorderCache.size() + waveCache.size() == 0;
  }

  public synchronized boolean inHelicorderCache(String station, double t1, double t2) {
    List<CachedHelicorder> helis = helicorderCache.get(station);
    if (helis == null)
      return false;

    for (CachedHelicorder ch : helis) {
      if (t1 >= ch.t1 && t2 <= ch.t2)
        return true;
    }

    return false;
  }

  public RSAMData getRsam(String channel, double t1, double t2, int period) {
    List<CachedRsam> rsam = rsamCache.get(channel);
    if (rsam == null)
      return null;
    else {

      for (CachedRsam cr : rsam) {
        if (cr.rsamData.getPeriod() != period)
          continue;

        if (t1 >= cr.t1 && t2 <= cr.t2) {
          return cr.slice(t1, t2);
        }
      }
    }
    return null;
  }

  public synchronized Wave getWave(String station, double t1, double t2) {

    List<CachedWave> waves = waveCache.get(station);
    if (waves == null)
      return null;
    else {

      for (CachedWave cw : waves) {
        if (t1 >= cw.t1 && t2 <= cw.t2) {
          // TODO: fix this. It's a sloppy.
          try {
            int[] newbuf = new int[(int) ((t2 - t1) * cw.wave.getSamplingRate())];
            int i = (int) ((t1 - cw.wave.getStartTime()) * cw.wave.getSamplingRate());
            System.arraycopy(cw.wave.buffer, i, newbuf, 0, newbuf.length);
            Wave sw = new Wave(newbuf, t1, cw.wave.getSamplingRate());
            cw.lastAccess = System.currentTimeMillis();
            return sw;
          } catch (ArrayIndexOutOfBoundsException e) {
            return null;
          }
        }
      }
    }
    return null;
  }

  public List<String> getChannels() {
    List<String> st = new ArrayList<String>();
    for (String key : helicorderCache.keySet()) {
      st.add(key.replace('$', ' '));
    }

    Collections.sort(st);

    if (st.size() == 0)
      return null;
    else
      return st;
  }

  public synchronized Wave getBestWave(String station, double t1, double t2) {
    Wave wave;
    List<CachedWave> waves = waveCache.get(station);
    if (waves == null)
      return null;
    else {
      List<Wave> parts = new ArrayList<Wave>();
      double minT = 1E300;
      double maxT = -1E300;
      for (CachedWave cw : waves) {
        if (cw.wave.overlaps(t1, t2)) {
          parts.add(cw.wave);
          minT = Math.min(minT, cw.t1);
          maxT = Math.max(maxT, cw.t2);
        }
      }

      if (parts.size() == 1)
        return parts.get(0);

      wave = Wave.join(parts, minT, maxT);
      if (wave != null)
        wave = wave.subset(t1, t2);
    }
    return wave;
  }

  public synchronized void putWave(String station, Wave wave) {
    List<CachedWave> waves = waveCache.get(station);
    if (waves == null) {
      waves = new ArrayList<CachedWave>();
      waveCache.put(station, waves);
      putWaveInCache(station, wave, waves);
    } else {
      for (CachedWave cw : waves) {
        boolean join = false;
        if (cw.wave.adjacent(wave))
          if (cw.wave.getMemorySize() + wave.getMemorySize() < MAX_WAVE_SIZE)
            join = true;
        if (cw.wave.overlaps(wave))
          join = true;

        if (join) {
          Wave newWave = cw.wave.combine(wave);
          if (newWave != null) {
            waves.remove(cw);
            putWave(station, newWave);
          }
          return;
        }
      }

      putWaveInCache(station, wave, waves);
    }
  }

  // this version, the one that implements SeismicDataSource, will only
  // composite
  // a Helicorder from the cache. If you want to try and fill data on either
  // side use
  // the version below
  public synchronized HelicorderData getHelicorder(String station, double startTime, double endTime,
      GulperListener gl) {
    station = station.replace(' ', '$');
    List<CachedHelicorder> helis = helicorderCache.get(station);
    if (helis == null)
      return null;
    else {
      HelicorderData hd = new HelicorderData();
      HelicorderData hd2 = null;
      for (CachedHelicorder ch : helis) {
        // found the whole thing, just return the needed subset
        if (startTime >= ch.t1 && endTime <= ch.t2) {
          hd2 = ch.helicorder.subset(startTime, endTime);
          ch.lastAccess = System.currentTimeMillis();
          return hd2;
        }

        // just a piece, put it in the result
        if (startTime <= ch.t1 && endTime >= ch.t2) {
          hd2 = ch.helicorder;
        }
        // cached is right side
        else if (endTime >= ch.t1 && endTime <= ch.t2) {
          hd2 = ch.helicorder.subset(ch.t1, endTime);
        }
        // cached is left side
        else if (startTime >= ch.t1 && startTime <= ch.t2) {
          hd2 = ch.helicorder.subset(startTime, ch.t2);
        }
        
        // if cached data found
        if (hd2 != null) {
          hd.concatenate(hd2);
          ch.lastAccess = System.currentTimeMillis();
          hd2 = null;
        }
      }
      hd.sort();
      if (hd.getData() == null)
        hd = null;

      return hd;
    }
  }

  public synchronized HelicorderData getHelicorder(String station, double t1, double t2,
      SeismicDataSource source) {
    List<CachedHelicorder> helis = helicorderCache.get(station);
    if (helis == null)
      return null;
    else {
      HelicorderData hd;
      for (CachedHelicorder ch : helis) {
        // asked for area completely within one cache entry
        if (t1 >= ch.t1 && t2 <= ch.t2) {
          // System.out.println("totally cached");
          hd = ch.helicorder.subset(t1, t2);
          ch.lastAccess = System.currentTimeMillis();
          return hd;
        }

        // one entry completely within asked for area
        if (t1 < ch.t1 && t2 > ch.t2) {
          // System.out.println("cache is centered chunk");
          HelicorderData nhd = source.getHelicorder(station, t1, ch.t1, null);
          if (nhd != null)
            hd = ch.helicorder.combine(nhd);
          else
            hd = ch.helicorder;

          nhd = source.getHelicorder(station, ch.t2, t2, null);
          if (nhd != null)
            hd = hd.combine(nhd);

          ch.lastAccess = System.currentTimeMillis();
          return hd;
        }

        // asked for area is to left but overlaps
        if (t1 < ch.t1 && t2 > ch.t1 && t2 <= ch.t2) {
          // System.out.println("cache overlaps on right side");
          HelicorderData nhd = source.getHelicorder(station, t1, ch.t1, null);
          if (nhd != null) {
            hd = ch.helicorder.combine(nhd);
            ch.lastAccess = System.currentTimeMillis();
            return hd;
          } else {
            ch.lastAccess = System.currentTimeMillis();
            return ch.helicorder;
          }

        }

        // asked for area is to right but overlaps
        if (t1 > ch.t1 && t1 < ch.t2 && t2 > ch.t2) {
          // System.out.println("cache overlaps on left side");
          HelicorderData nhd = source.getHelicorder(station, ch.t2, t2, null);
          if (nhd != null) {
            hd = ch.helicorder.combine(nhd);
            ch.lastAccess = System.currentTimeMillis();
            return hd;
          } else {
            ch.lastAccess = System.currentTimeMillis();
            return ch.helicorder;
          }
        }
      }
    }
    return null;
  }

  private void flushHelicorders() {
    helicorderCache.clear();// = new HashMap();
    System.out.println("Helicorder Cache Flushed");
  }

  private void flushWaves() {
    waveCache.clear();// = new HashMap();
    System.out.println("Wave Cache Flushed");
  }

  private <T extends CacheEntry> long outputCache(String type, Map<String, List<T>> cache) {
    long size = 0;
    System.out.println(type + " cache");
    for (String key : cache.keySet()) {
      System.out.println("\t" + key);
      List<T> cwl = cache.get(key);

      for (T ce : cwl) {
        size += ce.getMemorySize();
        System.out.println("\t\t" + ce.getInfoString());
      }
    }
    System.out.println(type + " size: " + size + " bytes");
    return size;
  }

  private synchronized <T extends CacheEntry> void removeEntryFromCache(CacheEntry ce,
      Map<String, List<T>> cache) {
    List<T> cl = cache.get(ce.station);
    cl.remove(ce);
    System.out.println("Removed: " + ce.getInfoString());
  }

  private class HalveLargeWavesPurgeAction extends CachePurgeAction {
    private int maxTime;
    private Map<String, List<CachedWave>> cache;

    public HalveLargeWavesPurgeAction(Map<String, List<CachedWave>> c, int m) {
      cache = c;
      maxTime = m;
    }

    public long purge() {
      List<CacheEntry> items = getEntriesByLastAccess(cache);

      long chunk = 0;

      for (CacheEntry ce : items) {
        CachedWave cw = (CachedWave) ce;
        if (cw.wave.getEndTime() - cw.wave.getStartTime() > maxTime) {
          long before = cw.getMemorySize();
          double nst = cw.wave.getEndTime() - (cw.wave.getEndTime() - cw.wave.getStartTime()) / 2;
          cw.wave = cw.wave.subset(nst, cw.wave.getEndTime());
          cw.t1 = cw.wave.getStartTime();
          cw.t2 = cw.wave.getEndTime();
          chunk += cw.getMemorySize() - before;
        }
      }
      return chunk;
    }
  }

  private class TimeLimitWavePurgeAction extends CachePurgeAction {
    private long interval;
    private Map<String, List<CachedWave>> cache;

    public TimeLimitWavePurgeAction(Map<String, List<CachedWave>> c, long i) {
      cache = c;
      interval = i;
    }

    public long purge() {
      List<CacheEntry> items = getEntriesByLastAccess(cache);

      long chunk = 0;
      long now = System.currentTimeMillis();

      for (CacheEntry ce : items) {
        if (now - ce.lastAccess > interval) {
          removeEntryFromCache(ce, cache);
          chunk += ce.getMemorySize();
        }
      }
      return chunk;
    }
  }

  private class TimeLimitHelicorderPurgeAction extends CachePurgeAction {
    private long interval;
    private Map<String, List<CachedHelicorder>> cache;

    public TimeLimitHelicorderPurgeAction(Map<String, List<CachedHelicorder>> c, long i) {
      cache = c;
      interval = i;
    }

    public long purge() {
      List<CacheEntry> items = getEntriesByLastAccess(cache);

      long chunk = 0;
      long now = System.currentTimeMillis();

      for (CacheEntry ce : items) {
        if (now - ce.lastAccess > interval) {
          removeEntryFromCache(ce, cache);
          chunk += ce.getMemorySize();
        }
      }
      return chunk;
    }
  }

  private class TimeLimitRsamPurgeAction extends CachePurgeAction {
    private long interval;
    private Map<String, List<CachedRsam>> cache;

    public TimeLimitRsamPurgeAction(Map<String, List<CachedRsam>> c, long i) {
      cache = c;
      interval = i;
    }

    public long purge() {
      List<CacheEntry> items = getEntriesByLastAccess(cache);

      long chunk = 0;
      long now = System.currentTimeMillis();

      for (CacheEntry ce : items) {
        if (now - ce.lastAccess > interval) {
          removeEntryFromCache(ce, cache);
          chunk += ce.getMemorySize();
        }
      }
      return chunk;
    }
  }

  abstract protected class CachePurgeAction {
    public CachePurgeAction() {}

    abstract public long purge();
  }

  abstract private class CacheEntry implements Comparable<CacheEntry> {
    public String station;
    public double t1;
    public double t2;
    public long lastAccess;

    public int compareTo(CacheEntry oce) {
      return (int) (lastAccess - oce.lastAccess);
    }

    abstract public String getInfoString();

    abstract public int getMemorySize();
  }

  public class CachedWave extends CacheEntry implements Comparable<CacheEntry> {
    public Wave wave;

    public String getInfoString() {
      long ms = System.currentTimeMillis() - lastAccess;
      return "[" + ms + "ms] " + (t2 - t1) + "s, " + wave.getMemorySize() + " bytes, " + t1 + " => "
          + t2;
    }

    public int getMemorySize() {
      return wave.getMemorySize();
    }
  }

  public class CachedHelicorder extends CacheEntry {
    public HelicorderData helicorder;

    public String toString() {
      return station + " " + t1 + " " + t2;
    }

    public String getInfoString() {
      long ms = System.currentTimeMillis() - lastAccess;
      return "[" + ms + "ms] " + (t2 - t1) + "s, " + helicorder.getMemorySize() + " bytes, " + t1
          + " => " + t2;
    }

    public int getMemorySize() {
      return helicorder.getMemorySize();
    }
  }

  public class CachedRsam extends CacheEntry {
    public RSAMData rsamData;

    public String toString() {
      return station + " " + t1 + " " + t2;
    }

    public RSAMData slice(double t1, double t2) {
      if (t1 >= this.t2 || t2 <= this.t1)
        return null;

      DoubleMatrix2D d = rsamData.getData();
      int i = 0;

      int firstRow = Integer.MAX_VALUE;
      while (firstRow == Integer.MAX_VALUE && i < d.rows()) {
        double t = d.getQuick(i, 0);
        if (t >= t1)
          firstRow = i;
        else
          i++;
      }

      int lastRow = -Integer.MAX_VALUE;
      while (lastRow == -Integer.MAX_VALUE && i < d.rows()) {
        double t = d.getQuick(i, 0);
        if (t >= t2)
          lastRow = i;
        else
          i++;
      }

      RSAMData rd = new RSAMData();
      rd.setData(d.viewPart(firstRow, 0, lastRow - firstRow, 2).copy());

      return rd;
    }

    @Override
    public String getInfoString() {
      long ms = System.currentTimeMillis() - lastAccess;
      return "[" + ms + "ms] " + (t2 - t1) + "s, " + rsamData.getMemorySize() + " bytes, " + t1
          + " => " + t2;
    }

    @Override
    public int getMemorySize() {
      return rsamData.getMemorySize();
    }
  }

  private class CompleteWavePurgeAction extends CachePurgeAction {
    private Map<String, List<CachedWave>> cache;

    public CompleteWavePurgeAction(Map<String, List<CachedWave>> c) {
      cache = c;
    }

    public long purge() {
      long size = getSize(cache);
      cache.clear();// = new HashMap();
      return size;
    }
  }

  private class CompleteHelicorderPurgeAction extends CachePurgeAction {
    private Map<String, List<CachedHelicorder>> cache;

    public CompleteHelicorderPurgeAction(Map<String, List<CachedHelicorder>> c) {
      cache = c;
    }

    public long purge() {
      long size = getSize(cache);
      cache.clear();// = new HashMap();
      return size;
    }
  }
}
