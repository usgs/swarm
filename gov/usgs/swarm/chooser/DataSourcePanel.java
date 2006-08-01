package gov.usgs.swarm.chooser;

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
	
	public DataSourcePanel(String c, String n)
	{
		code = c;
		name = n;
	}
	
	public void setSource(String s)
	{
		source = s;
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
	
	abstract protected void createPanel();
	abstract public boolean allowOK(boolean edit);
	abstract public String wasOK();
}
