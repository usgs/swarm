package gov.usgs.swarm.data;

import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.container.SeedObjectContainer;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.director.ImportDirector;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmDialog;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * 
 * @author Dan Cervelli
 */
public class FileDataSource extends SeismicDataSource
{
	private Map<String, double[]> channelTimes;
	private Set<String> openFiles;
	protected static final int MAX_WAVE_SIZE = 1000000;
	protected long maxSize;

	protected Map<String, List<CachedHelicorder>> helicorderCache;
	protected Map<String, List<CachedWave>> waveCache;

	protected CachePurgeAction[] purgeActions;


	public FileDataSource()
	{
		helicorderCache = new HashMap<String, List<CachedHelicorder>>();
		waveCache = new HashMap<String, List<CachedWave>>();
		maxSize = Runtime.getRuntime().maxMemory() / 6;
		createPurgeActions();
		channelTimes = new HashMap<String, double[]>();
		openFiles = new HashSet<String>();
		maxSize = Integer.MAX_VALUE;
		storeInUserConfig = false;
		name = "Files";
	}
	
	public void flush()
	{
		List<String> channels = getChannels();
		if (channels != null)
			for (String ch : channels)
				Swarm.config.removeMetadata(ch);
		flushWaves();
		flushHelicorders();
		System.gc();

		openFiles.clear();
		channelTimes.clear();
		fireChannelsUpdated();
	}
	
	private void updateChannelTimes(String channel, double t1, double t2)
	{
		double[] ct = channelTimes.get(channel);
		if (ct == null)
		{
			ct = new double[] { t1, t2 };
			channelTimes.put(channel, ct);
		}
		ct[0] = Math.min(ct[0], t1);
		ct[1] = Math.max(ct[1], t2);
	}
	
	private enum FileType 
	{ 
		TEXT, SAC, SEED, UNKNOWN;
		
		public static FileType fromFile(File f)
		{
			String fn = f.getPath().toLowerCase();
			if (fn.endsWith(".sac"))
				return SAC;
			else if (fn.endsWith(".txt"))
				return TEXT;
			else if (fn.endsWith(".seed"))
				return SEED;
			else
				return UNKNOWN;
		}
	}

	private class FileTypeDialog extends SwarmDialog
	{
		private static final long serialVersionUID = 1L;
		private JLabel filename;
		private JList fileTypes;
		private JCheckBox assumeSame;
		private boolean cancelled = true;
		private boolean opened = false;
		
		protected FileTypeDialog()
		{
			super(Swarm.getApplication(), "Unknown File Type", true);
			setSizeAndLocation();
		}
		
		public void setFilename(String fn)
		{
			filename.setText(fn);
		}
		
		protected void createUI()
		{
			super.createUI();
			filename = new JLabel();
			filename.setFont(Font.decode("dialog-BOLD-12"));
			filename.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
			String[] types = new String[] { "SEED/miniSEED volume", "SAC" };
			fileTypes = new JList(types);
			fileTypes.addMouseListener(new MouseAdapter()
					{
						public void mouseClicked(MouseEvent e)
						{
							if (e.getClickCount() == 2)
							{
								if (fileTypes.getSelectedIndex() != -1)
									okButton.doClick();
							}
						}
					});
			fileTypes.setSelectedIndex(0);
			assumeSame = new JCheckBox("Assume all unknown files are of this type", false);
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
			panel.setPreferredSize(new Dimension(300, 200));
			JPanel labelPanel = new JPanel(new GridLayout(3, 1));
			labelPanel.add(new JLabel("Unknown file type for file: "));
			labelPanel.add(filename);
			labelPanel.add(new JLabel("Choose 'Cancel' to skip this file or select file type:"));
			panel.add(labelPanel, BorderLayout.NORTH);
			panel.add(new JScrollPane(fileTypes), BorderLayout.CENTER);
			panel.add(assumeSame, BorderLayout.SOUTH);
			mainPanel.add(panel, BorderLayout.CENTER);
		}
		
		public boolean isAssumeSame()
		{
			return assumeSame.isSelected();
		}
		
		public FileType getFileType()
		{
			switch (fileTypes.getSelectedIndex())
			{
				case 0:
					return FileType.SEED;
				case 1:
					return FileType.SAC;
				default:
					return null;
			}
		}
		
		public void wasOK()
		{
			cancelled = false;
		}
		
		public void wasCancelled()
		{
			cancelled = true;
			opened = false;
		}
		
		public void setVisible(boolean b)
		{
			if (b)
				opened = true;
			super.setVisible(b);
		}
	}
	
	public void openFiles(File[] fs)
	{
		FileTypeDialog dialog = null;
		for (int i = 0; i < fs.length; i++)
		{
			FileType ft = FileType.fromFile(fs[i]);
			if (ft == FileType.UNKNOWN)
			{
				if (dialog == null)
					dialog = new FileTypeDialog();
				if (!dialog.opened || (dialog.opened && !dialog.isAssumeSame()))
				{
					dialog.setFilename(fs[i].getName());
					dialog.setVisible(true);
				}
				
				if (dialog.cancelled)
					ft = FileType.UNKNOWN;
				else
					ft = dialog.getFileType();
				
				Swarm.logger.warning("user input file type: " + fs[i].getPath() + " -> " + ft);
			}
			
			switch (ft)
			{
				case SAC:
					openSACFile(fs[i].getPath());
					break;
				case SEED:
					openSeedFile(fs[i].getPath());
					break;
				case UNKNOWN:
					Swarm.logger.warning("unknown file type: " + fs[i].getPath());
					break;
				default:
					Swarm.logger.warning("Cannot load file type " + ft + ": " + fs[i].getPath());
					break;
			}
			Swarm.config.lastPath = fs[i].getParent();
		}
	}
	
	public void openSACFile(final String fn)
	{
		if (openFiles.contains(fn))
			return;
		
		SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						Object result = null;
						fireChannelsProgress(fn, 0);
						try
						{
							Swarm.logger.fine("opening SAC file: " + fn);
							SAC sac = new SAC();
							sac.read(fn);
							fireChannelsProgress(fn, 0.5);
							String channel = sac.getStationInfo();
							Metadata md = Swarm.config.getMetadata(channel, true);
							md.addGroup("SAC^" + fn);
							
							Wave wave = sac.toWave();
							updateChannelTimes(channel, wave.getStartTime(), wave.getEndTime());
							cacheWaveAsHelicorder(channel, wave);
							putWave(channel, wave);
							
							openFiles.add(fn);
						}
						catch (Throwable t)
						{
							t.printStackTrace();
							result = t;
						}
						fireChannelsProgress(fn, 1);
						fireChannelsUpdated();
						return result;
					}
					
					public void finished()
					{
						if (getValue() != null)
						{
							JOptionPane.showMessageDialog(
									Swarm.getApplication(),
									"Could not open SAC file: " + fn, "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				};
		worker.start();
	}
	
	// TODO: can optimize memory usage further by added combined parts as you go.
	public void openSeedFile(final String fn)
	{
		if (openFiles.contains(fn))
			return;
		
		SwingWorker worker = new SwingWorker()
		{
			public Object construct()
			{
				Object result = null;
				try
				{
					fireChannelsProgress(fn, 0);
					CodeTimer ct = new CodeTimer("seed");
					Swarm.logger.fine("opening SEED file: " + fn);
					Map<String, List<Wave>> tempStationMap = new HashMap<String, List<Wave>>();
					
					DataInputStream ls = new DataInputStream(new BufferedInputStream(
			                new FileInputStream(fn)));
					ct.mark("dis");
					ImportDirector importDirector = new SeedImportDirector();
					SeedObjectBuilder objectBuilder = new SeedObjectBuilder();
					importDirector.assignBuilder(objectBuilder);  // register the builder with the director
					// begin reading the stream with the construct command
					importDirector.construct(ls);  // construct SEED objects silently
					SeedObjectContainer container = (SeedObjectContainer)importDirector.getBuilder().getContainer();
					ct.mark("prep");
					Object object;
					int total = container.iterate();
					ct.mark("iterate: " + total);
					int cnt = 0;
					while ((object = container.getNext()) != null)
					{
						fireChannelsProgress(fn, 0.5 + 0.5 * ((double)cnt / (double)total));
						cnt++;
						Blockette b = (Blockette)object;
						if (b.getType() != 999)
							continue;
						String loc = ("_" + b.getFieldVal(5)).trim();
						if (loc.length() == 1)
							loc = "";
						String code = b.getFieldVal(4) + "_" + b.getFieldVal(6) + "_" + b.getFieldVal(7) + loc;
						Metadata md = Swarm.config.getMetadata(code, true);
						md.addGroup("SEED^" + fn);
						
						List<Wave> parts = tempStationMap.get(code);
			            if (parts == null)
			            {
			            	parts = new ArrayList<Wave>();
			            	tempStationMap.put(code, parts);
			            }
			            
			            if (b.getWaveform() != null)
			            {
			                Waveform wf = b.getWaveform();
			            	Wave sw = new Wave();
			                sw.setSamplingRate(getSampleRate(((Integer)b.getFieldVal(10)).intValue(), ((Integer)b.getFieldVal(11)).intValue()));
			                Btime bTime = (Btime)b.getFieldVal(8);
			                sw.setStartTime(Util.dateToJ2K(btimeToDate(bTime)));
			                sw.buffer = wf.getDecodedIntegers();
			                sw.register();
			                parts.add(sw);
			            }
					}
					ct.mark("read: " + cnt);
					for (String code : tempStationMap.keySet())
					{
						List<Wave> parts = tempStationMap.get(code);
						ArrayList<Wave> subParts = new ArrayList<Wave>();
						int ns = 0;
						for (int i = 0; i < parts.size(); i++)
						{
							ns += parts.get(i).samples();
							subParts.add(parts.get(i));
							if (ns > 3600 * 100 || i == parts.size() - 1)
							{
								Wave wave = Wave.join(subParts);
								updateChannelTimes(code, wave.getStartTime(), wave.getEndTime());
				                cacheWaveAsHelicorder(code, wave);
								putWave(code, wave);
								ns = 0;
								subParts.clear();
							}
						}	
					}
					ct.mark("insert");
					ct.stop();
					openFiles.add(fn);
				}
				catch (Throwable t)
				{
					t.printStackTrace();
					result = t;
				}
				fireChannelsProgress(fn, 1);
				fireChannelsUpdated();
				return result;
			}
			
			public void finished()
			{
				if (getValue() != null)
				{
					JOptionPane.showMessageDialog(
							Swarm.getApplication(),
							"Could not open SEED file: " + fn, "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		worker.start();
	}
	
	private Date btimeToDate(Btime bt)
	{
    	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    	cal.set(Calendar.YEAR, bt.getYear());
    	cal.set(Calendar.DAY_OF_YEAR, bt.getDayOfYear());
    	cal.set(Calendar.HOUR_OF_DAY, bt.getHour());
    	cal.set(Calendar.MINUTE, bt.getMinute());
    	cal.set(Calendar.SECOND, bt.getSecond());
    	cal.set(Calendar.MILLISECOND, bt.getTenthMill() / 10);
    	return cal.getTime();
	}
	
	private float getSampleRate (double factor, double multiplier) {
        float sampleRate = (float) 10000.0;  // default (impossible) value;
        if ((factor * multiplier) != 0.0) {  // in the case of log records
            sampleRate = (float) (java.lang.Math.pow
                                      (java.lang.Math.abs(factor),
                                           (factor/java.lang.Math.abs(factor)))
                                      * java.lang.Math.pow
                                      (java.lang.Math.abs(multiplier),
                                           (multiplier/java.lang.Math.abs(multiplier))));
        }
        return sampleRate;
    }
	
	public HelicorderData getHelicorder(String channel, double t1, double t2, GulperListener gl)
	{
		double[] ct = channelTimes.get(channel);
		if (ct == null)
			return null;
		
		double dt = t2 - t1;
		double now = CurrentTime.getInstance().nowJ2K();
		if (Math.abs(now - t2) < 3600)
		{
			t2 = ct[1];
			t1 = t2 - dt;
		}

			List<CachedHelicorder> helis = helicorderCache.get(channel);
			if (helis == null)
				return null;
			else {
				HelicorderData hd = new HelicorderData();
				for (CachedHelicorder ch : helis) {
					// found the whole thing, just return the needed subset
					if (t1 >= ch.t1 && t2 <= ch.t2) {
						HelicorderData hd2 = ch.helicorder.subset(t1, t2);
						ch.lastAccess = System.currentTimeMillis();
						return hd2;
					}

					// just a piece, put it in the result
					if ((t1 < ch.t1 && t2 >= ch.t2) || (t1 <= ch.t1 && t2 > ch.t2)) {
						hd.concatenate(ch.helicorder);
						ch.lastAccess = System.currentTimeMillis();
					}

					// cached is right side
					if (t1 < ch.t1 && t2 > ch.t1 && t2 <= ch.t2) {
						HelicorderData hd2 = ch.helicorder.subset(ch.t1, t2);
						hd.concatenate(hd2);
						ch.lastAccess = System.currentTimeMillis();
					}

					// cached is left side
					if (t1 >= ch.t1 && t1 < ch.t2 && t2 > ch.t2) {
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
	
	public Wave getWave(String station, double t1, double t2) 
	{
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
	
	public String toConfigString()
	{
		return name + ";file:";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

		public boolean isActiveSource() {
			return false;
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


		public List<String> getChannels() {
			List<String> st = new ArrayList<String>();
			for (String key : helicorderCache.keySet()) {
				st.add(key);
			}

			if (st.size() == 0)
				return null;
			else
				return st;
		}


		public synchronized HelicorderData getHelicorder(String station, double t1,
				double t2, SeismicDataSource source) {
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
						HelicorderData nhd = source.getHelicorder(station, t1,
								ch.t1, null);
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
						HelicorderData nhd = source.getHelicorder(station, t1,
								ch.t1, null);
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
						HelicorderData nhd = source.getHelicorder(station, ch.t2,
								t2, null);
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

		private synchronized void putWaveInCache(String channel, Wave wave,
				List<CachedWave> waves) {
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

		public synchronized void putHelicorder(String station,
				HelicorderData helicorder) {
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

					if (ch.helicorder.overlaps(helicorder)
							&& helicorder != ch.helicorder) {
						helis.remove(ch);
						HelicorderData newHeli = ch.helicorder.combine(helicorder);
						putHelicorder(station, newHeli);
						helicorder = newHeli;
						i = 0;
						add = false;
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

		public synchronized boolean inHelicorderCache(String station, double t1,
				double t2) {
			List<CachedHelicorder> helis = helicorderCache.get(station);
			if (helis == null)
				return false;

			for (CachedHelicorder ch : helis) {
				if (t1 >= ch.t1 && t2 <= ch.t2)
					return true;
			}

			return false;
		}

		public synchronized void cacheWaveAsHelicorder(String station, Wave wave) {
			double st = Math.ceil(wave.getStartTime());
			double et = Math.floor(wave.getEndTime());
			if (inHelicorderCache(station, st, et))
				return;

			int seconds = (int) (Math.floor(wave.getEndTime()) - Math.ceil(wave
					.getStartTime()));
			double bi = Math.floor((Math.ceil(wave.getStartTime()) - wave
					.getStartTime()) * wave.getSamplingRate());
			int bufIndex = (int) bi;
			int sr = (int) wave.getSamplingRate();
			DoubleMatrix2D data = DoubleFactory2D.dense.make(seconds, 3);
			for (int i = 0; i < seconds; i++) {
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for (int j = 0; j < sr; j++) {
					int sample = wave.buffer[bufIndex];
					if (sample != Wave.NO_DATA) {
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
			if (data.rows() > 0) {
				HelicorderData hd = new HelicorderData();
				hd.setData(data);
				putHelicorder(station, hd);
			}
		}

		private synchronized <T extends CacheEntry> long getSize(
				Map<String, List<T>> cache) {
			long size = 0;
			for (String key : cache.keySet()) {
				List<T> cwl = cache.get(key);

				for (T ce : cwl)
					size += ce.getMemorySize();
			}
			return size;
		}

		public synchronized long getSize() {
			long size = getSize(waveCache);
			size += getSize(helicorderCache);
			return size;
		}

		public boolean isEmpty() {
			return helicorderCache.size() + waveCache.size() == 0;
		}


		public void flushHelicorders() {
			helicorderCache.clear();// = new HashMap();
			System.out.println("Helicorder Cache Flushed");
		}

		public void flushWaves() {
			waveCache.clear();// = new HashMap();
			System.out.println("Wave Cache Flushed");
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

		private synchronized <T extends CacheEntry> void removeEntryFromCache(
				CacheEntry ce, Map<String, List<T>> cache) {
			List<T> cl = cache.get(ce.station);
			cl.remove(ce);
			System.out.println("Removed: " + ce.getInfoString());
		}

		public synchronized void enforceSize() {
			if (purgeActions == null)
				return;

			long target = getSize() - maxSize;
			int i = 0;
			while (target > 0 && i < purgeActions.length) {
				long chunk = purgeActions[i].purge();
				Swarm.logger.finer("purged " + chunk + " bytes from cache");
				target -= chunk;
				i++;
			}
		}

		private <T extends CacheEntry> long outputCache(String type,
				Map<String, List<T>> cache) {
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

		protected class CachedWave extends CacheEntry implements
				Comparable<CacheEntry> {
			public Wave wave;

			public String getInfoString() {
				long ms = System.currentTimeMillis() - lastAccess;
				return "[" + ms + "ms] " + (t2 - t1) + "s, " + wave.getMemorySize()
						+ " bytes, " + t1 + " => " + t2;
			}

			public int getMemorySize() {
				return wave.getMemorySize();
			}
		}

		protected class CachedHelicorder extends CacheEntry {
			public HelicorderData helicorder;

			public String toString() {
				return station + " " + t1 + " " + t2;
			}

			public String getInfoString() {
				long ms = System.currentTimeMillis() - lastAccess;
				return "[" + ms + "ms] " + (t2 - t1) + "s, "
						+ helicorder.getMemorySize() + " bytes, " + t1 + " => "
						+ t2;
			}

			public int getMemorySize() {
				return helicorder.getMemorySize();
			}
		}

		public void createPurgeActions() {
			purgeActions = new CachePurgeAction[] {
					// purge anything that hasn't been hit in 5 minutes
					new TimeLimitWavePurgeAction(waveCache, 5 * 60 * 1000),
					new TimeLimitHelicorderPurgeAction(helicorderCache,
							5 * 60 * 1000),

					// cut waves larger than 3 hours in half keeping latest half
					new HalveLargeWavesPurgeAction(waveCache, 3 * 60 * 60),

					// cut waves larger than 1 hour in half
					new HalveLargeWavesPurgeAction(waveCache, 60 * 60),

					// getting to the last resort, purge wave cache
					new CompleteWavePurgeAction(waveCache),

					// should halve large helis

					// nothing left to do, purge helicorder cache
					new CompleteHelicorderPurgeAction(helicorderCache) };
		}

		abstract private class CachePurgeAction {
			public CachePurgeAction() {
			}

			abstract public long purge();
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

			public CompleteHelicorderPurgeAction(
					Map<String, List<CachedHelicorder>> c) {
				cache = c;
			}

			public long purge() {
				long size = getSize(cache);
				cache.clear();// = new HashMap();
				return size;
			}
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
						double nst = cw.wave.getEndTime()
								- (cw.wave.getEndTime() - cw.wave.getStartTime())
								/ 2;
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

			public TimeLimitHelicorderPurgeAction(
					Map<String, List<CachedHelicorder>> c, long i) {
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

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
