package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/08/01 23:43:13  cervelli
 * Moved package and new data source panel system.
 *
 * @author Dan Cervelli
 */
public class WaveServerPanel extends DataSourcePanel
{
	private JTextField wsHost;
	private JTextField wsPort;
	private JTextField wsTimeout;
	private JTextField gulperSize;
	private JTextField gulperDelay;
	
	public WaveServerPanel()
	{
		super("ws", "Earthworm Wave Server");
	}
	
	private void createFields()
	{
		wsHost = new JTextField();
		wsPort = new JTextField();
		wsTimeout = new JTextField();
		gulperSize = new JTextField();
		gulperDelay = new JTextField();
		String h = "";
		String p = "16022";
		String t = "2.0";
		String gs = "30";
		String gd = "1.0";
		if (source != null && source.indexOf(";ws:") != -1)
		{
			String[] ss = source.substring(source.indexOf(";ws:") + 4).split(":");
			h = ss[0];
			p = ss[1];
			t = String.format("%.1f", Integer.parseInt(ss[2]) / 1000.0);
			gs = String.format("%.0f", Integer.parseInt(ss[3]) / 60.0);
			gd = String.format("%.1f", Integer.parseInt(ss[4]) / 1000.0);
		}
		wsHost.setText(h);
		wsPort.setText(p);
		wsTimeout.setText(t);
		gulperSize.setText(gs);
		gulperDelay.setText(gd);
	}
	
	protected void createPanel()
	{
		createFields();
		FormLayout layout = new FormLayout(
				"right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 126dlu", 
				"");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(new JLabel("Use this data source to connect to an Earthworm Wave Server (EWS)."), 5);
		builder.nextLine();
		builder.appendSeparator();
		builder.append("IP Address or Host Name:");
		builder.append(wsHost, 3);
		builder.nextLine();
		builder.append("Port:");
		builder.append(wsPort);
		builder.append(" Earthworm default: 16022");
		builder.nextLine();

		builder.append("Timeout:");
		builder.append(wsTimeout);
		builder.append(" seconds");
		builder.nextLine();
		
		builder.append("Gulp size:");
		builder.append(gulperSize);
		builder.append(" minutes");
		builder.nextLine();
		
		builder.append("Gulp delay:");
		builder.append(gulperDelay);
		builder.append(" seconds");
		panel = builder.getPanel();
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
		
		double gs = -1;
		try { gs = Double.parseDouble(gulperSize.getText()); } catch (Exception e) {}
		if (gs <= 0)
			message = "The gulper size must be greater than 0 minutes.";
		
		double gd = -1;
		try { gd = Double.parseDouble(gulperDelay.getText()); } catch (Exception e) {}
		if (gd < 0)
			message = "The gulper delay must be greater than or equal to 0 seconds.";
		
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
		int gs = (int)(Double.parseDouble(gulperSize.getText()) * 60);
		int gd = (int)(Double.parseDouble(gulperDelay.getText()) * 1000);
		String result = String.format("ws:%s:%s:%d:%d:%d",
				wsHost.getText(), wsPort.getText(),
				timeout, gs, gd);
		return result;
	}
	
}
