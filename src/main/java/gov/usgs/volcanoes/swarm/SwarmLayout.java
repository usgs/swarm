package gov.usgs.volcanoes.swarm;

import gov.usgs.util.Log;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.swarm.chooser.DataChooser;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.heli.HelicorderViewerFrame;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.wave.MultiMonitor;
import gov.usgs.volcanoes.swarm.wave.SwarmMultiMonitors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * 
 *
 * @author Dan Cervelli
 */
public class SwarmLayout implements Comparable<SwarmLayout>
{
    private static final JFrame applicationFrame = Swarm.getApplicationFrame();
	private ConfigFile config;
	
	public SwarmLayout(ConfigFile c)
	{
		config = c;
	}
	
	public static SwarmLayout createSwarmLayout(String fn)
	{
		ConfigFile cf = new ConfigFile(fn);
		if (cf == null || !cf.wasSuccessfullyRead())
			return null;
		
		String name = cf.getString("name");
		if (name == null)
			return null;
		else
			return new SwarmLayout(cf);
	}

	public void save()
	{
		String fn = getName().replace(' ', '_');
		String n = fn.replaceAll("[^a-zA-Z0-9_]", "");
		String pre = "layouts" + File.separatorChar;
		String post = ".config";
		fn =  pre + n + post;
		boolean exists = new File(fn).exists();
		int i = 0;
		while (exists)
		{
			i++;
			fn = pre + n + "_" + i + post;
			exists = new File(fn).exists();
		}
		
		config.writeToFile(fn);
	}
	
	public void delete()
	{
		try
		{
			String fn = config.getName() + ".config";
			Log.getLogger("gov.usgs.swarm").fine("deleting file: " + fn);
			new File(fn).delete();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void setName(String s)
	{
		config.put("name", s, false);
	}
	
	public String getName()
	{
		return config.getString("name");
	}
	
	public void process()
	{
		SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						SwarmInternalFrames.removeAllFrames();
						processChooser();
						processMap();
						processWaves();
						processHelicorders();
						processMonitors();
						return null;
					}
					
					public void finished()
					{
						processKiosk();
					}
				};
		worker.start();
	}
	
	private class ChooserListener implements ActionListener
	{
		private List<String> sources;
		
		private ChooserListener()
		{
			sources = new ArrayList<String>();
		}
		
		public void addSource(String s)
		{
			sources.add(s);
		}
		
		public synchronized void actionPerformed(ActionEvent e)
		{
			String src = e.getActionCommand();
			sources.remove(src);
			if (e.getID() == DataChooser.NO_DATA_SOURCE)
			{
				JOptionPane.showMessageDialog(applicationFrame, 
						"The data source '" + src + "' does not exist.", 
						"Error", JOptionPane.ERROR_MESSAGE);
			}
			else if (e.getID() == DataChooser.NO_CHANNEL_LIST)
			{
				JOptionPane.showMessageDialog(applicationFrame, 
						"The data source '" + src + "' could not be opened.", 
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		public synchronized boolean finished()
		{
			return sources.size() == 0;
		}
	}
	
	private void processChooser()
	{
		ConfigFile cf = config.getSubConfig("chooser");
		ChooserListener cl = new ChooserListener();
		List<String> sources = cf.getList("source");
		if (sources != null)
		{
			for (String src : sources)
				cl.addSource(src);
			DataChooser.getInstance().processLayout(cf, cl);
			while (!cl.finished())
			{
				try { Thread.sleep(100); } catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
	
	private void processWaves()
	{
		
	}
	
	private void processMap()
	{
		ConfigFile cf = config.getSubConfig("map");
		MapFrame mapFrame = MapFrame.getInstance();
		if (cf.getString("x") != null)
		{
			mapFrame.setVisible(true);
			mapFrame.processLayout(cf);
		}
		else
		{
			mapFrame.setVisible(false);
		}
	}
	
	private void processMonitors()
	{
		List<String> monitors = config.getList("monitor");
		if (monitors == null)
			return;
		
		for (String monitor : monitors)
		{
			ConfigFile cf = config.getSubConfig(monitor);
			SeismicDataSource sds = SwarmConfig.getInstance().getSource(cf.getString("source"));
			if (sds != null && DataChooser.getInstance().isSourceOpened(sds.getName()))
			{
				MultiMonitor mm = SwarmMultiMonitors.getMonitor(sds);
				mm.processLayout(cf);
				mm.setVisible(true);
			}
		}
	}
	
	private void processHelicorders()
	{
		List<String> helis = config.getList("helicorder");
		if (helis == null)
			return;
		
		for (String heli : helis)
		{
			ConfigFile cf = config.getSubConfig(heli);
			SeismicDataSource sds = SwarmConfig.getInstance().getSource(cf.getString("source"));
			if (sds != null)
			{
				HelicorderViewerFrame hvf = new HelicorderViewerFrame(cf);
				hvf.addLinkListeners();
				SwarmInternalFrames.add(hvf, false);
			}
		}
	}

	private void processKiosk()
	{
		String k = config.getString("kiosk");
		if (k == null)
			k = "false";
		int x = StringUtils.stringToInt(config.getString("kioskX"), -1);
		int y = StringUtils.stringToInt(config.getString("kioskY"), -1);
		
		boolean kiosk = Boolean.parseBoolean(k);
		if (kiosk && x != -1 && y != -1)
		{
			applicationFrame.setLocation(x, y);
		}
		Swarm.getApplication().setFullScreenMode(kiosk);
	}
	
	public int compareTo(SwarmLayout o)
	{
		return getName().compareToIgnoreCase(o.getName());
	}
}
