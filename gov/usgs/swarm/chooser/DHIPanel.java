package gov.usgs.swarm.chooser;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class DHIPanel extends DataSourcePanel
{
	private JTextField network;
	
	public DHIPanel()
	{
		super("dhi", "DMC");
	}
	
	public boolean allowOK(boolean edit)
	{
		return true;
	}

	protected void createPanel()
	{
		panel = new JPanel();
		JLabel info = new JLabel("<html>Use this type of data source to connect to the IRIS DMC/BUD.<br>This panel needs work.</html>");
		panel.add(info);
		panel.add(new JLabel("Network:"));
		network = new JTextField(4);
		panel.add(network);
	}

	public String wasOK()
	{
		return "dhi:" + network.getText();
	}

}
