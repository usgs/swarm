package gov.usgs.swarm.data;
 
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * An implementation of <code>SeismicDataSource</code> that is used by 
 * Swarm to cache all data that it comes across.
 *
 * Note: during the conversion to Java 5 I didn't fully understand generics
 * so I had to add some hacky code for the CachePurgeActions.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.1  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.3  2004/10/23 19:34:11  cvs
 * Added null pointer check in getBestWave.
 *
 * Revision 1.2  2004/10/12 23:43:15  cvs
 * Added log info and some comments.
 *  
 * @author Dan Cervelli
 */
public class CachedDataSource extends SeismicDataSource
{
	private static final int MAX_WAVE_SIZE = 1000000;
	private long maxSize;
	
	private Map<String, List<CachedHelicorder>> helicorderCache;
	private Map<String, List<CachedWave>> waveCache;
	
	private CachePurgeAction[] purgeActions;
	
	public CachedDataSource()
	{
		helicorderCache = new HashMap<String, List<CachedHelicorder>>();
		waveCache = new HashMap<String, List<CachedWave>>();	
		maxSize = Runtime.getRuntime().maxMemory() / 2;
		createPurgeActions();
	}

	public boolean isActiveSource()
	{
		return false;
	}
	
	public List<String> getWaveStations()
	{
		List<String> st = new ArrayList<String>();
//		Iterator it = waveCache.keySet().iterator();
//		while (it.hasNext())
		for (String key : waveCache.keySet())
		{
//			String key = (String)it.next();
			st.add(key);
		}
		
		if (st.size() == 0)
			return null;
		else
			return st;
	}
	
	public synchronized Wave getBestWave(String station, double t1, double t2)
	{
		Wave wave;
		List<CachedWave> waves = waveCache.get(station);
		if (waves == null)
			return null;
		else
		{
			List<Wave> parts = new ArrayList<Wave>();
			double minT = 1E300;
			double maxT = -1E300;
			for (CachedWave cw : waves)
			{
				if (cw.wave.overlaps(t1, t2))
				{
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
	
	public synchronized Wave getWave(String station, double t1, double t2)
	{
		List<CachedWave> waves = waveCache.get(station);
		if (waves == null)
			return null;
		else
		{
//			Iterator it = waves.iterator();
//			while (it.hasNext())
			for (CachedWave cw : waves)
			{
//				CachedWave cw = (CachedWave)it.next();
//				System.out.println(t1 + " " + t2 + " " + cw.t1 + " " + cw.t2);
//				System.out.println(t1 >= cw.t1 && t2 <= cw.t2);
				if (t1 >= cw.t1 && t2 <= cw.t2)
				{
					// there is an intermittent problem here so I will catch this exception 
					// so the program doesn't lose functionality 
					try
					{
						// removed + 1 from line below
						int[] newbuf = new int[(int)((t2 - t1) * cw.wave.getSamplingRate())];
						int i = (int)((t1 - cw.wave.getStartTime()) * cw.wave.getSamplingRate());
						//System.out.println(i + " " + newbuf.length + " " + cw.wave.buffer.length);
						System.arraycopy(cw.wave.buffer, i, newbuf, 0, newbuf.length);
						Wave sw = new Wave(newbuf, t1, cw.wave.getSamplingRate());
						cw.lastAccess = System.currentTimeMillis();
						return sw;
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
						//e.printStackTrace();
						return null;	
					}
				}
			}
		}
		return null;
	}
	
	public List<String> getHelicorderStations()
	{
		List<String> st = new ArrayList<String>();
//		Iterator it = helicorderCache.keySet().iterator();
//		while (it.hasNext())
		for (String key : helicorderCache.keySet())
		{
//			String key = (String)it.next();
			st.add(key);
		}
		
		if (st.size() == 0)
			return null;
		else
			return st;
	}
	
	// this version, the one that implements SeismicDataSource, will only composite
	// a Helicorder from the cache.  If you want to try and fill data on either side use
	// the version below
	public synchronized HelicorderData getHelicorder(String station, double t1, double t2)
	{
//		Vector helis = (Vector)helicorderCache.get(station);
		List<CachedHelicorder> helis = helicorderCache.get(station);
		if (helis == null)
			return null;
		else
		{
			HelicorderData hd = new HelicorderData();
//			for (int i = 0; i < helis.size(); i++)
			for (CachedHelicorder ch : helis)
			{
//				CachedHelicorder ch = (CachedHelicorder)helis.elementAt(i);
				
				// found the whole thing, just return the needed subset
				if (t1 >= ch.t1 && t2 <= ch.t2)
				{
					HelicorderData hd2 = ch.helicorder.subset(t1, t2);
					ch.lastAccess = System.currentTimeMillis();
					return hd2;
				}
				
				// just a piece, put it in the result
				if ((t1 < ch.t1 && t2 >= ch.t2) || (t1 <= ch.t1 && t2 > ch.t2))
				{
					hd.concatenate(ch.helicorder);
					ch.lastAccess = System.currentTimeMillis();
				}
				
				// cached is right side
				if (t1 < ch.t1 && t2 > ch.t1 && t2 <= ch.t2)
				{
					HelicorderData hd2 = ch.helicorder.subset(ch.t1, t2);
					hd.concatenate(hd2);
					ch.lastAccess = System.currentTimeMillis();
				}
				
				// cached is left side
				if (t1 >= ch.t1 && t1 < ch.t2 && t2 > ch.t2)
				{
					HelicorderData hd2 = ch.helicorder.subset(t1, ch.t2);
					hd.concatenate(hd2);	
					ch.lastAccess = System.currentTimeMillis();
				}
			}
			hd.sort();
			if (hd.getData() == null)
				hd = null;
			return hd;
		}
	}
	
	public synchronized HelicorderData getHelicorder(String station, double t1, double t2, SeismicDataSource source)
	{
//		Vector helis = (Vector)helicorderCache.get(station);
		List<CachedHelicorder> helis = helicorderCache.get(station);
		if (helis == null)
			return null;
		else
		{
			HelicorderData hd;
//			for (int i = 0; i < helis.size(); i++)
			for (CachedHelicorder ch : helis)
			{
//				CachedHelicorder ch = (CachedHelicorder)helis.elementAt(i);
				// asked for area completely within one cache entry
				if (t1 >= ch.t1 && t2 <= ch.t2)
				{
					//System.out.println("totally cached");
					hd = ch.helicorder.subset(t1, t2);
					ch.lastAccess = System.currentTimeMillis();
					return hd;
				}
				
				// one entry completely within asked for area
				if (t1 < ch.t1 && t2 > ch.t2)
				{
					//System.out.println("cache is centered chunk");
					HelicorderData nhd = source.getHelicorder(station, t1, ch.t1);
					if (nhd != null)
						hd = ch.helicorder.combine(nhd);
					else
						hd = ch.helicorder;
					
					nhd = source.getHelicorder(station, ch.t2, t2);
					if (nhd != null)
						hd = hd.combine(nhd);
					
					ch.lastAccess = System.currentTimeMillis();
					return hd;					
				}
				
				// asked for area is to left but overlaps
				if (t1 < ch.t1 && t2 > ch.t1 && t2 <= ch.t2)
				{
					//System.out.println("cache overlaps on right side");
					HelicorderData nhd = source.getHelicorder(station, t1, ch.t1);
					if (nhd != null)
					{
						hd = ch.helicorder.combine(nhd);
						ch.lastAccess = System.currentTimeMillis();
						return hd;
					}
					else
					{
						ch.lastAccess = System.currentTimeMillis();
						return ch.helicorder;
					}
					
				}
				
				// asked for area is to right but overlaps
				if (t1 > ch.t1 && t1 < ch.t2 && t2 > ch.t2)
				{
					//System.out.println("cache overlaps on left side");	
					HelicorderData nhd = source.getHelicorder(station, ch.t2, t2);
					if (nhd != null)
					{
						hd = ch.helicorder.combine(nhd);
						ch.lastAccess = System.currentTimeMillis();
						return hd;
					}
					else
					{
						ch.lastAccess = System.currentTimeMillis();
						return ch.helicorder;
					}
				}
			}
		}
		return null;	
	}
	
	public synchronized void putWave(String station, Wave wave)
	{
		List<CachedWave> waves = waveCache.get(station);
		if (waves == null)
		{
			waves = new ArrayList<CachedWave>();
			waveCache.put(station, waves);
			putWaveInCache(station, wave, waves);
		}
		else
		{
//			Iterator it = waves.iterator();
			// could be faster if wave vector was known to be sorted by start time
//			while (it.hasNext())
			for (CachedWave cw : waves)
			{
//				CachedWave cw = (CachedWave)it.next();
				boolean join = false;
				if (cw.wave.adjacent(wave))
					if (cw.wave.getMemorySize() + wave.getMemorySize() < MAX_WAVE_SIZE)
						join = true;
					
				if (cw.wave.overlaps(wave))
					join = true;
				
				if (join)
				{
					Wave newWave = cw.wave.combine(wave);
					waves.remove(cw);
					putWave(station, newWave);
					return;
				}
			}
			
			putWaveInCache(station, wave, waves);
		}
	}
	
	private synchronized void putWaveInCache(String channel, Wave wave, List<CachedWave> waves)
	{
		if (wave.getMemorySize() > MAX_WAVE_SIZE)
		{
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
	
	public synchronized void putHelicorder(String station, HelicorderData helicorder)
	{
		//System.out.println("putHeli, rows: " + helicorder.getData().rows());
//		Vector helis = (Vector)helicorderCache.get(station);
		List<CachedHelicorder> helis = helicorderCache.get(station);
		if (helis == null)	
		{
//			Vector v = new Vector();
			helis = new ArrayList<CachedHelicorder>();
			CachedHelicorder ch = new CachedHelicorder();
			ch.station = station;
			ch.t1 = helicorder.getStartTime();
			ch.t2 = helicorder.getEndTime();
			ch.helicorder = helicorder;
			ch.lastAccess = System.currentTimeMillis();
			//System.out.println("new heli added");
			helis.add(ch);
			helicorderCache.put(station, helis);
			enforceSize();
		}
		else
		{
			//Iterator it = helis.iterator();
			// could be faster if wave vector was known to be sorted by start time
			int overlaps = 0;
			boolean add = true;
			for (int i = 0; i < helis.size(); i++)
			{
				CachedHelicorder ch = helis.get(i);
				
				if (ch.helicorder.overlaps(helicorder) && helicorder != ch.helicorder)
				{
					helis.remove(ch);
					HelicorderData newHeli = ch.helicorder.combine(helicorder);
					//System.out.println("calling putheli from putheli");
					putHelicorder(station, newHeli);
					overlaps++;
					helicorder = newHeli;
					i = 0;
					add = false;
				}
			}
			
			if (add)
			{
				CachedHelicorder ch = new CachedHelicorder();
				ch.station = station;
				ch.t1 = helicorder.getStartTime();
				ch.t2 = helicorder.getEndTime();
				ch.helicorder = helicorder;
				ch.lastAccess = System.currentTimeMillis();
				helis.add(ch);
				enforceSize();
				//System.out.println("helicorder stored, " + overlaps + " overlaps, " + helis.size() + " total size");
				//for (int i = 0; i < helis.size(); i++)
				//	System.out.println(helis.elementAt(i));
			}
		}
	}

	public synchronized boolean inHelicorderCache(String station, double t1, double t2)
	{
//		Vector helis = (Vector)helicorderCache.get(station);
		List<CachedHelicorder> helis = helicorderCache.get(station);
		if (helis == null)
			return false;
			
//		for (int i = 0; i < helis.size(); i++)
		for (CachedHelicorder ch : helis)
		{
//			CachedHelicorder ch = (CachedHelicorder)helis.elementAt(i);
			if (t1 >= ch.t1 && t2 <= ch.t2)
				return true;
		}
		
		return false;
	}

	public synchronized void cacheWaveAsHelicorder(String station, Wave wave)
	{
		//System.out.println(wave.getStartTime() + " " + wave.getEndTime());
		double st = Math.ceil(wave.getStartTime());
		double et = Math.floor(wave.getEndTime());
		if (inHelicorderCache(station, st, et))
			return;
			
//		int seconds = (int)(Math.floor(et) - Math.ceil(st));
		int seconds = (int)(Math.floor(wave.getEndTime()) - Math.ceil(wave.getStartTime()));
		//int bufIndex = (int)Math.floor(wave.getStartTime() % 1 * wave.getSamplingRate());
		double bi = Math.floor((Math.ceil(wave.getStartTime()) - wave.getStartTime()) * wave.getSamplingRate());
//		System.out.println(bi);
		int bufIndex = (int)bi;
		int sr = (int)wave.getSamplingRate();
		DoubleMatrix2D data = DoubleFactory2D.dense.make(seconds, 3);
//		System.out.println(st + " " + et + " " + seconds + " " + sr + " " + bufIndex + " " + sr * seconds);
		for (int i = 0; i < seconds; i++)
		{
			int min = Integer.MAX_VALUE;
			int max = Integer.MIN_VALUE;
			for (int j = 0; j < sr; j++)
			{
				int sample = wave.buffer[bufIndex];
				if (sample != Wave.NO_DATA)
				{
					min = Math.min(min, sample);
					max = Math.max(max, sample);
				}
				bufIndex++;
			}
			data.setQuick(i, 0, st);
			data.setQuick(i, 1, min);
			data.setQuick(i, 2, max);
			st += 1.0;
		}
		if (data.rows() > 0)
		{
			HelicorderData hd = new HelicorderData();
			hd.setData(data);
			//System.out.println(" hd st et: " + hd.rows() + " " + hd.getStartTime() + " " + hd.getEndTime());
			putHelicorder(station, hd);
		}
	}

	private synchronized <T extends CacheEntry> long getSize(Map<String, List<T>> cache)
	{
		long size = 0;
//		Iterator it = cache.keySet().iterator();
//		while (it.hasNext())
		for (String key : cache.keySet())
		{
//			String key = (String)it.next();
			List<T> cwl = cache.get(key);
			
//			Iterator it2 = cwl.iterator();
//			while (it2.hasNext())
			for (T ce : cwl)
			{
//				CacheEntry ce = (CacheEntry)it2.next();
				size += ce.getMemorySize();
			}
		}
		return size;
	}
	
	public synchronized long getSize()
	{
		long size = getSize(waveCache);
		size += getSize(helicorderCache);
		return size;
	}
	
	public void flush()
	{
		//enforceSize();
		flushWaves();
		flushHelicorders();
		System.gc();
	}
	
	public void flushHelicorders()
	{
		helicorderCache.clear();// = new HashMap();
		System.out.println("Helicorder Cache Flushed");
	}
	
	public void flushWaves()
	{
		waveCache.clear();// = new HashMap();	
		System.out.println("Wave Cache Flushed");
	}

	private synchronized <T extends CacheEntry> List<CacheEntry> getEntriesByLastAccess(Map<String, List<T>> cache)
	{
//		Vector v = new Vector();
		List<CacheEntry> cl = new ArrayList<CacheEntry>();
//		Iterator it = cache.keySet().iterator();
//		while (it.hasNext())
		for (String key : cache.keySet())
		{
//			String key = (String)it.next();
			List<T> cwl = cache.get(key);
			for (T ce : cwl)
				cl.add(ce);
//			Iterator it2 = cwl.iterator();
//			while (it2.hasNext())
//				v.add(it2.next());
		}
		
//		Object[] o = v.toArray();
//		Arrays.sort(o);
		Collections.sort(cl);
		
		return cl;
		//return Arrays.asList(o);
	}
	
	private synchronized <T extends CacheEntry> void removeEntryFromCache(CacheEntry ce, Map<String, List<T>> cache)
	{
		List<T> cl = cache.get(ce.station);
		cl.remove(ce);
		System.out.println("Removed: " + ce.getInfoString());
	}
	
	public synchronized void enforceSize()
	{
		if (purgeActions == null)
			return;
		
		long target = getSize() - maxSize;
		int i = 0;
		while (target > 0 && i < purgeActions.length)
		{
			long chunk = purgeActions[i].purge();
			target -= chunk;
			i++;
		}
	}
	
	private <T extends CacheEntry> long outputCache(String type, Map<String, List<T>> cache)
	{
		long size = 0;
//		Iterator it = cache.keySet().iterator();
		System.out.println(type + " cache");
//		while (it.hasNext())
		for (String key : cache.keySet())
		{
//			String key = (String)it.next();
			System.out.println("\t" + key);
			List<T> cwl = cache.get(key);
			
//			Iterator it2 = cwl.iterator();
//			while (it2.hasNext())
			for (T ce : cwl)
			{
//				CacheEntry ce = (CacheEntry)it2.next();
				size += ce.getMemorySize();
				System.out.println("\t\t" + ce.getInfoString());
			}
		}
		System.out.println(type + " size: " + size + " bytes");
		return size;
	}
	
	public void output()
	{
		long size = outputCache("Wave", waveCache);
		size += outputCache("Helicorder", helicorderCache);
		System.out.println("Wave Last Access Order:");
		List<CacheEntry> wl = getEntriesByLastAccess(waveCache);
//		for (Iterator it = wl.iterator(); it.hasNext();)
		for (CacheEntry ce : wl)
		{
//			CacheEntry ce = (CacheEntry) it.next();
			System.out.println(ce.getInfoString());
		}
		System.out.println("Helicorder Last Access Order:");
		List<CacheEntry> hl = getEntriesByLastAccess(helicorderCache);
//		for (Iterator it = hl.iterator(); it.hasNext();)
		for (CacheEntry ce : hl)
		{
//			CacheEntry ce = (CacheEntry) it.next();
			System.out.println(ce.getInfoString());
		}
		System.out.println("Total size: " + size + " bytes");
	}

	abstract private class CacheEntry implements Comparable<CacheEntry>
	{
		public String station;
		public double t1;
		public double t2;
		public long lastAccess;
		
		public int compareTo(CacheEntry oce)
		{
			return (int)(lastAccess - oce.lastAccess); 
		}
		
		abstract public String getInfoString();
		abstract public int getMemorySize();
	}
	
	private class CachedWave extends CacheEntry implements Comparable<CacheEntry>
	{
		public Wave wave;
		
		public String getInfoString()
		{
			long ms = System.currentTimeMillis() - lastAccess;
			return "[" + ms + "ms] " + (t2 - t1) + "s, " + wave.getMemorySize() + " bytes, " + t1 + " => " + t2;	
		}
		
		public int getMemorySize()
		{
			return wave.getMemorySize();	
		}
	}
	
	private class CachedHelicorder extends CacheEntry
	{
		public HelicorderData helicorder;	
		
		public String toString()
		{
			return station + " " + t1 + " " + t2;	
		}
		
		public String getInfoString()
		{
			long ms = System.currentTimeMillis() - lastAccess;
			return "[" + ms + "ms] " + (t2 - t1) + "s, " + helicorder.getMemorySize() + " bytes, " + t1 + " => " + t2;	
		}
		
		public int getMemorySize()
		{
			return helicorder.getMemorySize();
		}
	}
	
	public void createPurgeActions()
	{
		purgeActions = new CachePurgeAction[] {
				// purge anything that hasn't been hit in 5 minutes
				new TimeLimitWavePurgeAction(waveCache, 5 * 60 * 1000),
				new TimeLimitHelicorderPurgeAction(helicorderCache, 5 * 60 * 1000),
				
				// cut waves larger than 3 hours in half keeping latest half
				new HalveLargeWavesPurgeAction(waveCache, 3 * 60 * 60),
				
				// cut waves larger than 1 hour in half
				new HalveLargeWavesPurgeAction(waveCache, 60 * 60),
				
				// getting to the last resort, purge wave cache
				new CompleteWavePurgeAction(waveCache),
				
				// should halve large helis
				
				// nothing left to do, purge helicorder cache
				new CompleteHelicorderPurgeAction(helicorderCache)
				};
	}
	
	abstract private class CachePurgeAction
	{
//		protected Map<String, List<? extends CacheEntry>> cache;
		
		public CachePurgeAction() {}
		
//		public <T extends CacheEntry> CachePurgeAction(Map<String, List<? extends CacheEntry>> c)
//		{
//			cache = c;
//		}
		
		abstract public long purge();
	}
	
	private class CompleteWavePurgeAction extends CachePurgeAction
	{
		private Map<String, List<CachedWave>> cache;
		
		public CompleteWavePurgeAction(Map<String, List<CachedWave>> c)
		{
			cache = c;
		}
		
		public long purge()
		{
			long size = getSize(cache);
			cache.clear();// = new HashMap();
			return size;
		}		
	}
	
	private class CompleteHelicorderPurgeAction extends CachePurgeAction
	{
		private Map<String, List<CachedHelicorder>> cache;
		
		public CompleteHelicorderPurgeAction(Map<String, List<CachedHelicorder>> c)
		{
			cache = c;
		}
		
		public long purge()
		{
			long size = getSize(cache);
			cache.clear();// = new HashMap();
			return size;
		}		
	}
	
	private class HalveLargeWavesPurgeAction extends CachePurgeAction
	{
		private int maxTime;
		private Map<String, List<CachedWave>> cache;
		
		public HalveLargeWavesPurgeAction(Map<String, List<CachedWave>> c, int m)
		{
			cache = c;
			maxTime = m;
		}
	
		public long purge()
		{
			List<CacheEntry> items = getEntriesByLastAccess(cache);
			
			long chunk = 0;
//			long now = System.currentTimeMillis();
			
//			for (Iterator it = items.iterator(); it.hasNext();)
			for (CacheEntry ce : items)
			{
				CachedWave cw = (CachedWave)ce;
				if (cw.wave.getEndTime() - cw.wave.getStartTime() > maxTime)
				{
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
	
	private class TimeLimitWavePurgeAction extends CachePurgeAction
	{
		private long interval;
		private Map<String, List<CachedWave>> cache;
		
		public TimeLimitWavePurgeAction(Map<String, List<CachedWave>> c, long i)
		{
			cache = c;
			interval = i;
		}
		
		public long purge()
		{
			List<CacheEntry> items = getEntriesByLastAccess(cache);
			
			long chunk = 0;
			long now = System.currentTimeMillis();
			
//			for (Iterator it = items.iterator(); it.hasNext();)
			for (CacheEntry ce : items)
			{
//				CacheEntry ce = (CacheEntry)it.next();
				if (now - ce.lastAccess > interval)
				{
					removeEntryFromCache(ce, cache);
					chunk += ce.getMemorySize();
				}
			}
			return chunk;
		}
	}
	
	private class TimeLimitHelicorderPurgeAction extends CachePurgeAction
	{
		private long interval;
		private Map<String, List<CachedHelicorder>> cache;
		
		public TimeLimitHelicorderPurgeAction(Map<String, List<CachedHelicorder>> c, long i)
		{
			cache = c;
			interval = i;
		}
		
		public long purge()
		{
			List<CacheEntry> items = getEntriesByLastAccess(cache);
			
			long chunk = 0;
			long now = System.currentTimeMillis();
			
//			for (Iterator it = items.iterator(); it.hasNext();)
			for (CacheEntry ce : items)
			{
//				CacheEntry ce = (CacheEntry)it.next();
				if (now - ce.lastAccess > interval)
				{
					removeEntryFromCache(ce, cache);
					chunk += ce.getMemorySize();
				}
			}
			return chunk;
		}
	}
}