package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.util.Log;
import gov.usgs.util.ResourceReader;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2007/03/09 00:06:44  dcervelli
 * Doesn't crash the panel if the network file is missing.
 *
 * Revision 1.4  2007/03/08 23:56:23  dcervelli
 * Added IRIS network list.
 *
 * Revision 1.3  2006/08/09 03:44:18  cervelli
 * Added description and separator.
 *
 * Revision 1.2  2006/08/08 22:20:29  cervelli
 * First good working version of this panel.
 *
 * Revision 1.1  2006/08/01 23:43:13  cervelli
 * Moved package and new data source panel system.
 *
 * @author Dan Cervelli
 * @version $Id: DHIPanel.java,v 1.6 2007-05-21 02:51:00 dcervelli Exp $
 */
public class DHIPanel extends DataSourcePanel
{
	private static final String IRIS_NETWORK_FILE = "IRIS_networks.txt";
	private JComboBox netDC;
	private JComboBox netDNS;
	private JComboBox seisDC;
	private JComboBox seisDNS;
	private JComboBox network;
	private JTextField gulperSize;
	private JTextField gulperDelay;
	private JButton nwButton;
	private JButton dcButton;
	
	public DHIPanel()
	{
		super("dhi", "DMC");
	}
	
	private String checkComboBox(JComboBox box, String name)
	{
		String val = (String)box.getSelectedItem();
		if (val == null || val.length() == 0)
			return "There is an error with the " + name + ".";
		else
			return null;
	}
	
	public boolean allowOK(boolean edit)
	{
		String message = null;
		message = checkComboBox(netDC, "Network DC");
		if (message == null)
			message = checkComboBox(netDNS, "Network DNS");
		if (message == null)
			message = checkComboBox(seisDC, "Seismogram DC");
		if (message == null)
			message = checkComboBox(seisDNS, "Seismogram DNS");
		if (message == null)
			message = checkComboBox(network, "Network");
		
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
	
	private void createFields()
	{
		netDC = new JComboBox();
		netDC.setEditable(true);
		netDNS = new JComboBox();
		netDNS.setEditable(true);
		seisDC = new JComboBox();
		seisDC.setEditable(true);
		seisDNS = new JComboBox();
		seisDNS.setEditable(true);
		network = new JComboBox();
		network.setEditable(true);
		
		ResourceReader rr = getResourceReader(IRIS_NETWORK_FILE);
		if (rr != null)
		{
			String s;
			while ((s = rr.nextLine()) != null)
			{
				s = s.trim();
				if (s.length() > 1 && !s.startsWith("#"))
					network.addItem(s);
			}
		}
		else
			Log.getLogger("gov.usgs.swarm").warning(IRIS_NETWORK_FILE + " is missing.");
		gulperSize = new JTextField();
		gulperDelay = new JTextField();
		dcButton = new JButton("Query for DCs");
		dcButton.setEnabled(false);
		nwButton = new JButton("Query for Networks");
		nwButton.setEnabled(false);
		
		String ndns = "edu/iris/dmc";
		String ndc = "IRIS_NetworkDC";
		String sdns = "edu/iris/dmc";
		String sdc = "IRIS_BudDataCenter";
		String nw = null;
		String gs = "60";
		String gd = "1.0";
		if (source != null && source.indexOf(";dhi:") != -1)
		{
			String[] ss = source.substring(source.indexOf(";dhi:") + 5).split(":");
			ndns = ss[0];
			ndc = ss[1];
			sdns = ss[2];
			sdc = ss[3];
			nw = ss[4];
			gs = String.format("%.0f", Integer.parseInt(ss[5]) / 60.0);
			gd = String.format("%.1f", Integer.parseInt(ss[6]) / 1000.0);
		}
		netDC.addItem(ndc);
		netDNS.addItem(ndns);
		seisDC.addItem(sdc);
		seisDNS.addItem(sdns);
		if (nw != null)
		{
			boolean found = false;
			for (int i = 0; i < network.getItemCount(); i++)
			{
				String item = (String)network.getItemAt(i);
				if (item.startsWith(nw))
				{
					network.setSelectedIndex(i);
					found = true;
					break;
				}
			}
		
			if (!found)
			{
				network.insertItemAt(nw, 0);
				network.setSelectedIndex(0);
			}
		}
		
		gulperSize.setText(gs);
		gulperDelay.setText(gd);
	}
	
	protected void createPanel()
	{
		createFields();
		FormLayout layout = new FormLayout(
				"right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 45dlu, 3dlu, right:max(20dlu;pref), 3dlu, 85dlu", 
				"");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(new JLabel("Use this data source to connect to a Data Management Center (DMC)."), 9);
		builder.nextLine();
		builder.appendSeparator();
		builder.append("Network DC:");
		builder.append(netDC, 3);
		builder.append("DNS:");
		builder.append(netDNS);
		builder.nextLine();

		builder.append("Seismogram DC:");
		builder.append(seisDC, 3);
		builder.append("DNS:");
		builder.append(seisDNS);
		builder.nextLine();
		
		builder.append("Network:");
		builder.append(network, 7);
		builder.nextLine();
		
		builder.append("Gulp size:");
		builder.append(gulperSize);
		builder.append(" minutes");
		builder.append(" ");
		builder.append(dcButton);
		builder.nextLine();
		
		builder.append("Gulp delay:");
		builder.append(gulperDelay);
		builder.append(" seconds");
		builder.append(" ");
		builder.append(nwButton);
		
		panel = builder.getPanel();
	}

	public String wasOK()
	{
		int gs = (int)(Double.parseDouble(gulperSize.getText()) * 60);
		int gd = (int)(Double.parseDouble(gulperDelay.getText()) * 1000);
		String nw = (String)network.getSelectedItem();
		if (nw.indexOf(",") != -1)
			nw = nw.substring(0, nw.indexOf(","));
		String result = String.format("dhi:%s:%s:%s:%s:%s:%d:%d",
				netDNS.getSelectedItem(), netDC.getSelectedItem(),
				seisDNS.getSelectedItem(), seisDC.getSelectedItem(),
				nw,
				gs, gd);
		return result;
	}

}
