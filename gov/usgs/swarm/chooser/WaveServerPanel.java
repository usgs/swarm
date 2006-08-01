package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.GridBagHelper;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
public class WaveServerPanel extends DataSourcePanel
{
	private JTextField wsHost;
	private JTextField wsPort;
	private JTextField wsTimeout;
	
	public WaveServerPanel()
	{
		super("ws", "Wave Server");
	}
	
	protected void createPanel()
	{
		String wsh = "";
		String wsp = "16022";
		String wsto = "2";
		if (source != null && source.indexOf(";ws:") != -1)
		{
			String s = source.substring(source.indexOf(";ws:") + 4);
//			String[] ss = Util.splitString(s, ":");
			String[] ss = s.split(":");
//			wsh = source.substring(source.indexOf(";ws:") + 4, source.indexOf(':', source.indexOf("ws:") + 4));
			wsh = ss[0];
			wsp = ss[1];
			wsto = Double.toString(Double.parseDouble(ss[2]) / 1000);
		}
		panel = new JPanel(new GridBagLayout());
		panel.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
		GridBagConstraints c = new GridBagConstraints();
		
		wsHost = new JTextField(20);
//		wsHost.setMinimumSize(new Dimension(180, 20));
		wsHost.setText(wsh);
		wsPort = new JTextField("16022", 5);
//		wsPort.setMinimumSize(new Dimension(50, 20));
		wsPort.setText(wsp);
		
		wsTimeout = new JTextField("2", 5);
//		wsTimeout.setMinimumSize(new Dimension(50, 20));
		wsTimeout.setText(wsto);
		
		JLabel info = new JLabel("<html>Use this type of data source to connect to a Earthworm Wave Server.<br>The default Wave Server port is 16022.</html>");
		panel.add(info, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;ix=4;iy=4;wy=0.0;f=b"));
		
		panel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=2;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		JLabel ipLabel = new JLabel("IP address or host name:");
		ipLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(ipLabel, GridBagHelper.set(c, "x=0;y=2;a=e;w=1;h=1;wx=0;wy=0;f=h;i=0,0,0,8"));
		panel.add(wsHost, GridBagHelper.set(c, "x=1;y=2;a=w;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
		
		JLabel portLabel = new JLabel("Port:");
		portLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(portLabel, GridBagHelper.set(c, "x=0;y=3;a=ne;w=1;h=1;wx=0;wy=0.0;i=0,0,0,8"));
		panel.add(wsPort, GridBagHelper.set(c, "x=1;y=3;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
		
		JLabel toLabel = new JLabel("Timeout, seconds:");
		toLabel.setHorizontalAlignment(JLabel.RIGHT);
		panel.add(toLabel, GridBagHelper.set(c, "x=0;y=4;a=ne;w=1;h=1;wx=0;wy=1;i=0,0,0,8"));
		panel.add(wsTimeout, GridBagHelper.set(c, "x=1;y=4;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
	}
	
	public boolean allowOK(boolean edit)
	{
		String message = null;
		String host = wsHost.getText();
		if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') !=-1)
			message = "There is an error with the Wave Server IP address or host name.";
		int ip = -1;
		try { ip = Integer.parseInt(wsPort.getText()); } catch (Exception e) {}
		if (ip < 0 || ip > 65535)
			message = "There is an error with the Wave Server port.";
		
		double to = -1;
		try { to = Double.parseDouble(wsTimeout.getText()); } catch (Exception e) {}
		if (to <= 0)
			message = "There is an error with the Wave Server time out (must be > 0).";
		
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
		int timeout = (int)(Double.parseDouble(wsTimeout.getText()) * 1000);
		return "ws:" + wsHost.getText() + ":" + wsPort.getText() + ":" + timeout;
	}
	
}
