package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmConfig;
import gov.usgs.swarm.data.fdsnWs.WebServiceStationXmlClient;
import gov.usgs.swarm.data.fdsnWs.WebServiceUtils;
import gov.usgs.swarm.data.fdsnWs.WebServicesSource;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
       private static final String codeText = ";" + WebServicesSource.typeString + ":";
       private JComboBox<String> network;

	private JTextField station;
	private JTextField location;
	private JTextField channel;
	private JTextField gulperSize;
	private JTextField gulperDelay;
	private JTextField wsDataselectUrlField;
	private JTextField wsStationUrlField;
       private JButton updateNetworkList;
       private String currentStationURL = "";

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
			JOptionPane.showMessageDialog(applicationFrame, message,
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
               network.addItem("---Need Update---");
		station = new JTextField();
		location = new JTextField();
		channel = new JTextField();
		gulperSize = new JTextField();
		gulperDelay = new JTextField();
		wsDataselectUrlField = new JTextField();
		wsStationUrlField = new JTextField();
               // Listen for changes in the text
               wsStationUrlField.getDocument().addDocumentListener(new DocumentListener() {
                       public void changedUpdate(DocumentEvent e) {
                           String s = getText(wsStationUrlField);
                           if(currentStationURL.compareTo(s) != 0) {
                               showNeedUpdate();
                           }
                       }
                       public void removeUpdate(DocumentEvent e) {
                           String s = getText(wsStationUrlField);
                           if(currentStationURL.compareTo(s) != 0) {
                               showNeedUpdate();
                           }
                       }
                       public void insertUpdate(DocumentEvent e) {
                           String s = getText(wsStationUrlField);
                           if(currentStationURL.compareTo(s) != 0) {
                               showNeedUpdate();
                           }
                       }
                   });
               String net = "";
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
               selectNetwork(net);
               station.setText(sta);
               location.setText(loc);
               channel.setText(chan);
               gulperSize.setText(gs);
               gulperDelay.setText(gd);
               wsDataselectUrlField.setText(wsDataSelectUrl);
               wsStationUrlField.setText(wsStationUrl);
               updateNetworkList = new JButton("Update");
               updateNetworkList.addActionListener(new ActionListener() {
                       public void actionPerformed(ActionEvent e)
                       {
                           getWebServicesNetworks();
                       }
                   });
       }

       public void resetSource(String src)
       {
           if(src != null && (source == null || src.compareTo(source) != 0)) {
               source = src;
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
		selectNetwork(net);
		station.setText(sta);
		location.setText(loc);
		channel.setText(chan);
		gulperSize.setText(gs);
		gulperDelay.setText(gd);
		wsDataselectUrlField.setText(wsDataSelectUrl);
		wsStationUrlField.setText(wsStationUrl);
		}
	}

       private class WSNetworkClient extends WebServiceStationXmlClient
       {
           public WSNetworkClient(String baseUrlText) {
               super(baseUrlText);
           }
           protected String getUrlTextWithTime() {
               return getBaseUrlText() + "net=*&level=network&format=xml&includeavailability=false";
           }
           protected void fetch(URL url) throws Exception {
               fetchNetworks(url);
           }
           public List<String> getNetworkList() {
               try {
                   fetch();
               }
               catch(Exception e) {
                   StringBuilder msg = new StringBuilder();
                   if(error != null && !error.toString().isEmpty()) {
                       msg.append(error.toString());
                   }
                   if(e != null && !e.toString().isEmpty()) {
                       if( msg.length() > 0 ) msg.append("\n");
                       msg.append(e.toString());
                   }
                   if( msg.length() == 0 ) msg.append("Error getting network list.");

                   JOptionPane.showMessageDialog(Swarm.getApplication(), msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                   return null;
               }
               return super.getNetworkList();
           }
       }


	/**
	 * Create panels.
	 */
	protected void createPanel()
	{
		createFields();
		FormLayout layout = new FormLayout(
                       "right:max(10dlu;pref), 3dlu, right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 45dlu, 3dlu, right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 40dlu",

				"");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(new JLabel("Use this data source to connect to "
				+ WebServicesSource.DESCRIPTION + "."), 11);

		builder.nextLine();
               builder.append("Dataselect URL");
               builder.append(wsDataselectUrlField, 9);
               builder.nextLine();
               builder.append("Station URL");
               builder.append(wsStationUrlField, 9);

               builder.nextLine();

		builder.appendSeparator();

		JLabel scnlLabel = new JLabel(
                       "<HTML>Enter Station, Channel, Network and Location. An empty field is the same as '*'. Use "
                       + WebServiceUtils.EMPTY_LOC_CODE
                       + " for an empty location code."
                       + " Wildcards (\"?\" for any single character and \"*\" for zero or more characters)"
                       + " and comma-separated lists are accepted. All Networks channels will not be displayed on the map.</HTML>");

		builder.append(scnlLabel, 11);
		builder.nextLine();
               builder.append(updateNetworkList);
               builder.append("Network");
               builder.append(network, 9);
		builder.nextLine();
               builder.append("");
               builder.append("Station");
               builder.append(station, 1);
               builder.append("");
               builder.append("Gulp size");

		builder.append(gulperSize);
		builder.append(" minutes");

               builder.nextLine();
               builder.append("");
               builder.append("Channel");
               builder.append(channel, 1);
               builder.append("");
               builder.append("Gulp delay");
		
		builder.append(gulperDelay);
		builder.append(" seconds");
               builder.nextLine();
               builder.append("");
               builder.append("Location");
               builder.append(location, 1);
               // add some space
               builder.nextLine();
               builder.append(" ");
               builder.nextLine();
               builder.append(" ");

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
                       s = SwarmConfig.getInstance().fdsnDataselectURL;

		else if (component == wsStationUrlField)
                       s = SwarmConfig.getInstance().fdsnStationURL;

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
	 * Initialize the web service networks in the network selection.
	 */
       protected void getWebServicesNetworks()

	{
           network.removeAllItems();
           network.addItem("---Updating List---");
           currentStationURL = getText(wsStationUrlField);
           WSNetworkClient wsc = new WSNetworkClient(currentStationURL);
           List<String> nets = wsc.getNetworkList();
           if(nets != null) {
               network.removeAllItems();
               Iterator<String> iterator = nets.iterator();
               while(iterator.hasNext()) {
                   network.addItem(iterator.next());

		}
	}
	}
       protected void showNeedUpdate() {
           if(network.getItemCount() > 0 && network.getSelectedItem() != null) {
               String value = network.getSelectedItem().toString();
               if(value.compareTo("---Need Update---") == 0) return;
           }
           network.removeAllItems();
           network.addItem("---Need Update---");
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
               SwarmConfig.getInstance().fdsnDataselectURL = getText(wsDataselectUrlField);
               SwarmConfig.getInstance().fdsnStationURL = getText(wsStationUrlField);

		return result;
	}
}
