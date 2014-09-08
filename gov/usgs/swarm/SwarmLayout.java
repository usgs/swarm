package gov.usgs.swarm;

import gov.usgs.swarm.chooser.DataChooser;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.heli.HelicorderViewerFrame;
import gov.usgs.swarm.wave.MultiMonitor;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.7  2006/08/14 22:43:36  dcervelli
 * Saves kiosks and allows deletes.
 *
 * Revision 1.6  2006/08/12 21:51:04  dcervelli
 * Fixed bugs with file data sources.
 *
 * Revision 1.5  2006/08/12 00:34:44  dcervelli
 * Waits for data sources in background.
 *
 * Revision 1.4  2006/08/09 21:54:58  cervelli
 * Checks data sources and sets map visibility correctly.
 *
 * Revision 1.3  2006/08/07 22:33:26  cervelli
 * Saves monitor layout.
 *
 * Revision 1.2  2006/08/01 23:39:09  cervelli
 * Sets layout info for the chooser.
 *
 * Revision 1.1  2006/07/30 22:42:19  cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class SwarmLayout implements Comparable<SwarmLayout>
{
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
			Swarm.logger.fine("deleting file: " + fn);
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
						Swarm.getApplication().removeAllFrames();
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
				JOptionPane.showMessageDialog(Swarm.getApplication(), 
						"The data source '" + src + "' does not exist.", 
						"Error", JOptionPane.ERROR_MESSAGE);
			}
			else if (e.getID() == DataChooser.NO_CHANNEL_LIST)
			{
				JOptionPane.showMessageDialog(Swarm.getApplication(), 
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
			Swarm.getApplication().getDataChooser().processLayout(cf, cl);
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
		if (cf.getString("x") != null)
		{
			Swarm.getApplication().getMapFrame().setVisible(true);
			Swarm.getApplication().getMapFrame().processLayout(cf);
		}
		else
		{
			Swarm.getApplication().getMapFrame().setVisible(false);
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
			SeismicDataSource sds = Swarm.config.getSource(cf.getString("source"));
			if (sds != null && Swarm.getApplication().getDataChooser().isSourceOpened(sds.getName()))
			{
				MultiMonitor mm = Swarm.getApplication().getMonitor(sds);
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
			SeismicDataSource sds = Swarm.config.getSource(cf.getString("source"));
			if (sds != null)
			{
				HelicorderViewerFrame hvf = new HelicorderViewerFrame(cf);
				hvf.addLinkListeners();
				Swarm.getApplication().addInternalFrame(hvf, false);
			}
		}
	}

	private void processKiosk()
	{
		String k = config.getString("kiosk");
		if (k == null)
			k = "false";
		int x = Util.stringToInt(config.getString("kioskX"), -1);
		int y = Util.stringToInt(config.getString("kioskY"), -1);
		
		boolean kiosk = Boolean.parseBoolean(k);
		if (kiosk && x != -1 && y != -1)
		{
			Swarm.getApplication().setLocation(x, y);
		}
		Swarm.getApplication().setFullScreenMode(kiosk);
	}
	
	public int compareTo(SwarmLayout o)
	{
		return getName().compareToIgnoreCase(o.getName());
	}
}
