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

/**
 * 
 * @author Dan Cervelli
 */
public class FileDataSource extends CachedDataSource
{
	private Map<String, double[]> channelTimes;
	private Set<String> openFiles;
	
	public FileDataSource()
	{
		super();
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
		super.flush();
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
		return super.getHelicorder(channel, t1, t2, gl);
	}
	
	public Wave getWave(String station, double t1, double t2) 
	{
		return super.getBestWave(station, t1, t2);
	}
	
	public String toConfigString()
	{
		return name + ";file:";
	}
}
