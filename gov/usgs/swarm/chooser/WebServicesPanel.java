package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.data.DataSelectReader;
import gov.usgs.swarm.data.fdsnWs.WebServiceStationTextClient;
import gov.usgs.swarm.data.fdsnWs.WebServiceUtils;
import gov.usgs.swarm.data.fdsnWs.WebServicesSource;
import gov.usgs.util.ResourceReader;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The Web Services panel is a data source panel for Web Services.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class WebServicesPanel extends DataSourcePanel
{
	private static final String WS_NETWORK_FILE = "IRIS_networks.txt";
	private static final String codeText = ";"
			+ WebServicesSource.typeString + ":";
	private boolean showUrlFieldsFlag = false;
	private JComboBox network;
	private JTextField station;
	private JTextField location;
	private JTextField channel;
	private JTextField gulperSize;
	private JTextField gulperDelay;
	private JTextField wsDataselectUrlField;
	private JTextField wsStationUrlField;

	/**
	 * Create the Web Services server panel.
	 */
	public WebServicesPanel()
	{
		super(WebServicesSource.typeString,
				WebServicesSource.TAB_TITLE);
	}

	/**
	 * Determines if the OK should be allowed.
	 * 
	 * @return true if allowed, false otherwise.
	 */
	public boolean allowOK(boolean edit)
	{
		String message = null;
		double gs = -1;
		try
		{
			gs = Double.parseDouble(gulperSize.getText());
		}
		catch (Exception e)
		{
		}
		if (gs <= 0)
			message = "The gulper size must be greater than 0 minutes.";

		double gd = -1;
		try
		{
			gd = Double.parseDouble(gulperDelay.getText());
		}
		catch (Exception e)
		{
		}
		if (gd < 0)
			message = "The gulper delay must be greater than or equal to 0 seconds.";

		if (message != null)
		{
			JOptionPane.showMessageDialog(Swarm.getApplication(), message,
					"Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else
			return true;
	}

	/**
	 * Create fields.
	 */
	protected void createFields()
	{
		network = new JComboBox();
		network.setEditable(true);
		station = new JTextField();
		location = new JTextField();
		channel = new JTextField();
		gulperSize = new JTextField();
		gulperDelay = new JTextField();
		wsDataselectUrlField = new JTextField();
		wsStationUrlField = new JTextField();
		String net = "IU";
		String sta = "";
		String loc = "";
		String chan = "";
		String gs = "60";
		String gd = "1.0";
		String wsDataSelectUrl = getDefaultText(wsDataselectUrlField);
		String wsStationUrl = getDefaultText(wsStationUrlField);
		int index;
		if (source != null && (index = source.indexOf(codeText)) != -1)
		{
			String[] ss = source.substring(index + codeText.length()).split(
					WebServicesSource.PARAM_SPLIT_TEXT);
			int ssIndex = 0;
			net = ss[ssIndex++];
			sta = ss[ssIndex++];
			loc = ss[ssIndex++];
			chan = ss[ssIndex++];
			gs = String.format("%.0f", Integer.parseInt(ss[ssIndex++]) / 60.0);
			gd = String
					.format("%.1f", Integer.parseInt(ss[ssIndex++]) / 1000.0);
			wsDataSelectUrl = ss[ssIndex++];
			wsStationUrl = ss[ssIndex++];
		}
		initWebServicesNetworks();
		selectNetwork(net);
		station.setText(sta);
		location.setText(loc);
		channel.setText(chan);
		gulperSize.setText(gs);
		gulperDelay.setText(gd);
		wsDataselectUrlField.setText(wsDataSelectUrl);
		wsStationUrlField.setText(wsStationUrl);
	}

	/**
	 * Create panels.
	 */
	protected void createPanel()
	{
		createFields();
		FormLayout layout = new FormLayout(
				"right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 45dlu, 3dlu, right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 45dlu",
				"");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(new JLabel("Use this data source to connect to "
				+ WebServicesSource.DESCRIPTION + "."), 11);
		builder.nextLine();
		builder.appendSeparator();

		JLabel scnlLabel = new JLabel(
				"<HTML>Station, Channel, Network and Location filters or empty for all. Use "
						+ WebServiceUtils.EMPTY_LOC_CODE
						+ " for empty location code."
						+ " Both wildcards (\"?\" for any single character and \"*\" for zero or more characters)"
						+ " and comma-separated lists are accepted. If all Networks channels will not be displayed on the map.</HTML>");
		builder.append(scnlLabel, 11);
		builder.nextLine();
		builder.append("Station:");
		builder.append(station, 3);
		builder.append("Channel:");
		builder.append(channel, 3);
		builder.nextLine();
		builder.append("Network:");
		builder.append(network, 3);
		builder.append("Location:");
		builder.append(location, 3);
		builder.nextLine();
		builder.append("Gulp size:");
		builder.append(gulperSize);
		builder.append(" minutes");
		builder.append("Gulp delay:");
		builder.append(gulperDelay);
		builder.append(" seconds");
		if (showUrlFieldsFlag)
		{
			builder.nextLine();
			builder.append("Dataselect URL");
			builder.append(wsDataselectUrlField, 9);
			builder.nextLine();
			builder.append("Station URL");
			builder.append(wsStationUrlField, 9);
		}
		panel = builder.getPanel();
	}

	/**
	 * Get the default text for the specified component.
	 * 
	 * @param component the component.
	 * @return the default text.
	 */
	private String getDefaultText(Component component)
	{
		String s;
		if (component == wsDataselectUrlField)
			s = DataSelectReader.DEFAULT_WS_URL;
		else if (component == wsStationUrlField)
			s = WebServiceStationTextClient.DEFAULT_WS_URL;
		else
			s = "";
		return s;
	}

	/**
	 * Get the network combination box text.
	 * 
	 * @param s the network text.
	 * @return the network combination box text.
	 */
	protected String getNetworkText(String s)
	{
		return s.replaceFirst("\\s*,.*", "").trim();
	}

	/**
	 * Get the current text for the specified component.
	 * 
	 * @param component the component.
	 * @return the current text.
	 */
	private String getText(Component component)
	{
		Object value;
		String s;
		if (component instanceof JComboBox)
		{
			value = ((JComboBox) component).getSelectedItem();
		}
		else if (component instanceof JTextComponent)
		{
			value = ((JTextComponent) component).getText();
		}
		else
		{
			value = component.toString();
		}
		if (value == null)
		{
			s = getDefaultText(component);
		}
		else
		{
			s = getText(value);
		}
		if (component == network)
		{
			s = getNetworkText(s);
		}
		return s;
	}

	/**
	 * Get the text for the specified value.
	 * 
	 * @param value the value or null if none.
	 * @return the text or null if none.
	 */
	private String getText(Object value)
	{
		if (value != null)
		{
			return value.toString().trim();
		}
		return null;
	}

	/**
	 * Get web services resource reader.
	 * 
	 * @return the web services resource reader or null if none.
	 */
	protected ResourceReader getWsNetworResourceReader()
	{
		ResourceReader rr = getResourceReader(WS_NETWORK_FILE);
		if (rr == null)
		{
			WebServiceUtils.warning(WS_NETWORK_FILE + " is missing.");
		}
		return rr;
	}

	/**
	 * Initialize the web service networks in the network selection.
	 */
	protected void initWebServicesNetworks()
	{
		ResourceReader rr = getWsNetworResourceReader();
		if (rr != null)
		{
			String s;
			while ((s = rr.nextLine()) != null)
			{
				s = s.trim();
				if (s.length() > 1 && !s.startsWith("#"))
				{
					network.addItem(s);
				}
			}
		}
	}

	/**
	 * Select the network.
	 * 
	 * @param net the network.
	 */
	protected void selectNetwork(String net)
	{
		if (net != null && net.length() != 0)
		{
			boolean found = false;
			for (int i = 0; i < network.getItemCount(); i++)
			{
				String item = (String) network.getItemAt(i);
				if (item.startsWith(net))
				{
					network.setSelectedIndex(i);
					found = true;
					break;
				}
			}

			if (!found)
			{
				network.insertItemAt(net, 0);
				network.setSelectedIndex(0);
			}
		}
		else
		{
			network.setSelectedItem(null);
		}
	}

	/**
	 * Process the OK.
	 */
	public String wasOK()
	{
		int gs = (int) (Double.parseDouble(gulperSize.getText()) * 60);
		int gd = (int) (Double.parseDouble(gulperDelay.getText()) * 1000);
		String result = String.format(getCode() + ":"
				+ WebServicesSource.PARAM_FMT_TEXT, getText(network),
				getText(station), getText(location), getText(channel), gs, gd,
				getText(wsDataselectUrlField), getText(wsStationUrlField));
		return result;
	}
}
