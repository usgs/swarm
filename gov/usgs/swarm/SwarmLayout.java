package gov.usgs.swarm;

import gov.usgs.swarm.heli.HelicorderViewerFrame;
import gov.usgs.util.ConfigFile;

import java.io.File;
import java.util.List;

/**
 * 
 * $Log: not supported by cvs2svn $
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
		Swarm.getApplication().removeAllFrames();
		processChooser();
		processMap();
		processWaves();
		processHelicorders();
		processMonitors();
	}
	
	private void processChooser()
	{
		ConfigFile cf = config.getSubConfig("chooser");
		Swarm.getApplication().getDataChooser().processLayout(cf);
	}
	
	private void processWaves()
	{
		
	}
	
	private void processMap()
	{
		ConfigFile cf = config.getSubConfig("map");
		Swarm.getApplication().getMapFrame().processLayout(cf);
	}
	
	private void processMonitors()
	{
		
	}
	
	private void processHelicorders()
	{
		List<String> helis = config.getList("helicorder");
		if (helis == null)
			return;
		
		for (String heli : helis)
		{
			ConfigFile cf = config.getSubConfig(heli);
			// check source now
			HelicorderViewerFrame hvf = new HelicorderViewerFrame(cf);
			hvf.addLinkListeners();
			Swarm.getApplication().addInternalFrame(hvf, false);
		}
	}

	public int compareTo(SwarmLayout o)
	{
		return getName().compareToIgnoreCase(o.getName());
	}
}
