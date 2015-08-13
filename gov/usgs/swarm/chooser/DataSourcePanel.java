package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.ResourceReader;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
abstract public class DataSourcePanel
{
	protected JPanel panel;
	protected String code;
	protected String name;
	protected String source;
    protected static final JFrame applicationFrame = Swarm.getApplicationFrame();

	
	public DataSourcePanel(String c, String n)
	{
		code = c;
		name = n;
	}
	
	public void setSource(String s)
	{
		source = s;
	}

       public void resetSource(String s)
       {
       }

	
	public String getCode()
	{
		return code;
	}
	
	public String getName()
	{
		return name;
	}

	public JPanel getPanel()
	{
		if (panel == null)
			createPanel();
		return panel;
	}
	
	/**
	 * Creates a resource reader for the given resource.  If the resource has
	 * has a local filename then it is read otherwise the class loader is used.
	 * 
	 * @param name the resource name
	 * @return resource reader
	 */
	protected ResourceReader getResourceReader(String name)
	{
		return ResourceReader.getResourceReader(getClass(), name);
	}

	abstract protected void createPanel();
	abstract public boolean allowOK(boolean edit);
	abstract public String wasOK();
}
