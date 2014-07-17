package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2006/08/16 20:59:36  cervelli
 * Changed default timeout to 5.0 for WWS
 *
 * Revision 1.2  2006/08/09 03:43:42  cervelli
 * Uses FormLayout.
 *
 * Revision 1.1  2006/08/01 23:43:13  cervelli
 * Moved package and new data source panel system.
 *
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
		super("wws", "Winston Wave Server");
	}

	private void createFields()
	{
		wwsHost = new JTextField();
		wwsPort = new JTextField();
		wwsTimeout = new JTextField();
		wwsCompress = new JCheckBox();
		String h = "";
		String p = "16022";
		String t = "15.0";
		boolean wscomp = true;
		if (source != null && source.indexOf(";wws:") != -1)
		{
			String[] ss = source.substring(source.indexOf(";wws:") + 5).split(":");
			h = ss[0];
			p = ss[1];
			t = String.format("%.1f", Integer.parseInt(ss[2]) / 1000.0);
			wscomp = ss[3].equals("1");
		}
		wwsHost.setText(h);
		wwsPort.setText(p);
		wwsTimeout.setText(t);
		wwsCompress.setSelected(wscomp);
	}
	
	protected void createPanel()
	{
		createFields();
		FormLayout layout = new FormLayout(
				"right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 126dlu", 
				"");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(new JLabel("Use this data source to connect to a Winston Wave Server (WWS)."), 5);
		builder.nextLine();
		builder.appendSeparator();
		builder.append("IP Address or Host Name:");
		builder.append(wwsHost, 3);
		builder.nextLine();
		builder.append("Port:");
		builder.append(wwsPort);
		builder.append(" Winston default: 16022");
		builder.nextLine();

		builder.append("Timeout:");
		builder.append(wwsTimeout);
		builder.append(" seconds");
		builder.nextLine();
		
		builder.append("Use Compression:");
		builder.append(wwsCompress);
		builder.nextLine();
		
		panel = builder.getPanel();
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
			JOptionPane.showMessageDialog(applicationFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else
			return true;	
	}

	public String wasOK()
	{	
		int timeout = (int)(Double.parseDouble(wwsTimeout.getText()) * 1000);
		String result = String.format("wws:%s:%s:%d:%s",
				wwsHost.getText(), wwsPort.getText(), timeout, 
				(wwsCompress.isSelected() ? "1" : "0"));
		return result;
	}

}
