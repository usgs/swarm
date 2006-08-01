package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.GridBagHelper;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class WWSPanel extends DataSourcePanel
{
	private JTextField wwsHost;
	private JTextField wwsPort;
	private JTextField wwsTimeout;
	private JCheckBox wwsCompress;

	public WWSPanel()
	{
		super("wws", "WWS");
	}
	
	protected void createPanel()
	{
		String wsh = "";
		String wsp = "16022";
		String wsto = "10";
		boolean wscomp = false;
		if (source != null && source.indexOf(";wws:") != -1)
		{
			String s = source.substring(source.indexOf(";wws:") + 5);
			String[] ss = s.split(":");
			wsh = ss[0];
			wsp = ss[1];
			wsto = Double.toString(Double.parseDouble(ss[2]) / 1000);
			wscomp = ss[3].equals("1");
		}
		panel = new JPanel(new GridBagLayout());
		panel.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
		GridBagConstraints c = new GridBagConstraints();
		
		wwsHost = new JTextField(20);
		wwsHost.setMinimumSize(new Dimension(180, 20));
		wwsHost.setText(wsh);
		wwsPort = new JTextField("16022", 5);
		wwsPort.setMinimumSize(new Dimension(50, 20));
		wwsPort.setText(wsp);
		
		wwsTimeout = new JTextField("10", 5);
		wwsTimeout.setMinimumSize(new Dimension(50, 20));
		wwsTimeout.setText(wsto);

		wwsCompress = new JCheckBox();
		wwsCompress.setSelected(wscomp);
		
		JLabel info = new JLabel("<html>Use this type of data source to connect to a Winston Wave Server (WWS).<br>The default WWS port is 16022.</html>");
		panel.add(info, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;ix=4;iy=4;wy=0.0;f=b"));
		
		panel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=2;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		JLabel ipLabel = new JLabel("IP address or host name:");
		ipLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(ipLabel, GridBagHelper.set(c, "x=0;y=2;a=e;w=1;h=1;wx=0;wy=0;f=h;i=0,0,0,8"));
		panel.add(wwsHost, GridBagHelper.set(c, "x=1;y=2;a=w;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
		
		JLabel portLabel = new JLabel("Port:");
		portLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(portLabel, GridBagHelper.set(c, "x=0;y=3;a=ne;w=1;h=1;wx=0;wy=0.0;i=0,0,0,8"));
		panel.add(wwsPort, GridBagHelper.set(c, "x=1;y=3;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
		
		JLabel toLabel = new JLabel("Timeout, seconds:");
		toLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(toLabel, GridBagHelper.set(c, "x=0;y=4;a=ne;w=1;h=1;wx=0;wy=0;i=0,0,0,8"));
		panel.add(wwsTimeout, GridBagHelper.set(c, "x=1;y=4;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
		
		JLabel compLabel = new JLabel("Use compression:");
		compLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(compLabel, GridBagHelper.set(c, "x=0;y=5;a=ne;w=1;h=1;wx=0;wy=1;i=0,0,0,8"));
		panel.add(wwsCompress, GridBagHelper.set(c, "x=1;y=5;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
	}
	
	public boolean allowOK(boolean edit)
	{
		String host = wwsHost.getText();
		String message = null;
		
		if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') !=-1)
			message = "There is an error with the WWS IP address or host name.";
		int ip = -1;
		try { ip = Integer.parseInt(wwsPort.getText()); } catch (Exception e) {}
		if (ip < 0 || ip > 65535)
			message = "There is an error with the WWS port.";
		
		double to = -1;
		try { to = Double.parseDouble(wwsTimeout.getText()); } catch (Exception e) {}
		if (to <= 0)
			message = "There is an error with the WWS time out (must be > 0).";
		if (message != null)
		{
			JOptionPane.showMessageDialog(Swarm.getApplication(), message, "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else
			return true;	
	}

	public String wasOK()
	{	
		int timeout = (int)(Double.parseDouble(wwsTimeout.getText()) * 1000);
		return "wws:" + wwsHost.getText() + ":" + wwsPort.getText() + ":" + timeout + ":" + (wwsCompress.isSelected() ? "1" : "0");
	}

}
