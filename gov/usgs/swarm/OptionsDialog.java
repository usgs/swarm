package gov.usgs.swarm;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * 
 * @author Dan Cervelli
 * @version $Id: OptionsDialog.java,v 1.7 2007-05-21 02:38:41 dcervelli Exp $
 */
public class OptionsDialog extends SwarmDialog {
	private static final long serialVersionUID = 1L;

	private JPanel dialogPanel;

	private JCheckBox durationEnabled;
	private JTextField durationA;
	private JTextField durationB;
	private JCheckBox useLargeCursor;

	private JCheckBox tzInstrument;
	private JRadioButton tzLocal;
	private JRadioButton tzSpecific;
	private JComboBox timeZones;

	private JRadioButton useMapPacks;
	private JRadioButton useWMS;
	private JTextField wmsServer;
	private JTextField wmsLayer;
	private JTextField wmsStyles;
	private JLabel wmsServerLabel;
	private JLabel wmsLayerLabel;
	private JLabel wmsStylesLabel;

	public OptionsDialog() {
		super(Swarm.getApplication(), "Options", true);
		createUI();
		setCurrentValues();
		setSizeAndLocation();
	}

	private void createFields() {
		durationEnabled = new JCheckBox("Enabled");
		durationA = new JTextField();
		durationB = new JTextField();
		useLargeCursor = new JCheckBox("Large Helicorder Cursor");
		tzInstrument = new JCheckBox("Use instrument time zone if available");
		tzLocal = new JRadioButton("Use local machine time zone:");
		tzSpecific = new JRadioButton("Use specific time zone:");
		ButtonGroup tzGroup = new ButtonGroup();
		tzGroup.add(tzLocal);
		tzGroup.add(tzSpecific);
		String[] tzs = TimeZone.getAvailableIDs();
		Arrays.sort(tzs);
		timeZones = new JComboBox(tzs);

		useMapPacks = new JRadioButton("Use local MapPacks");
		useWMS = new JRadioButton("Use WMS");
		ButtonGroup mapGroup = new ButtonGroup();
		mapGroup.add(useMapPacks);
		mapGroup.add(useWMS);
		wmsLayer = new JTextField();
		wmsServer = new JTextField();
		wmsStyles = new JTextField();
	}

	protected void createUI() {
		super.createUI();
		createFields();

		FormLayout layout = new FormLayout(
				"right:max(30dlu;pref), 3dlu, 40dlu, 3dlu, right:max(40dlu;pref), 3dlu, 40dlu", "");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();

		builder.appendSeparator("Time Zone");

		builder.append(tzInstrument, 7);
		builder.nextLine();
		TimeZone local = TimeZone.getDefault();

		builder.append(tzLocal, 7);
		builder.append("   ");
		builder.append(new JLabel(local.getID()), 5);
		builder.nextLine();

		builder.append(tzSpecific, 7);
		builder.nextLine();

		builder.append("   ");
		builder.append(timeZones, 5);
		builder.nextLine();

		builder.appendSeparator("Duration Magnitude");
		builder.append(durationEnabled, 7);
		builder.nextLine();
		builder.append("Md=", durationA);
		builder.append("* Log(t) +", durationB);

		builder.appendSeparator("Maps");
		builder.append(useMapPacks, 7);
		builder.nextLine();
		builder.append(useWMS, 7);
		builder.nextLine();
		wmsServerLabel = new JLabel("Server:");
		wmsServerLabel.setLabelFor(wmsServer);
		builder.append(wmsServerLabel);
		builder.append(wmsServer, 5);
		builder.nextLine();
		wmsLayerLabel = new JLabel("Layer:");
		wmsLayerLabel.setLabelFor(wmsLayer);
		builder.append(wmsLayerLabel);
		builder.append(wmsLayer, 5);
		builder.nextLine();
		wmsStylesLabel = new JLabel("Styles:");
		wmsStylesLabel.setLabelFor(wmsStyles);
		builder.append(wmsStylesLabel);
		builder.append(wmsStyles, 5);
		builder.nextLine();

		useMapPacks.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				doEnables();
			}
		});

		builder.appendSeparator("Other");
		builder.append(useLargeCursor, 7);
		builder.nextLine();

		dialogPanel = builder.getPanel();
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}

	public void doEnables() {
		boolean state = useMapPacks.isSelected();
		wmsServer.setEnabled(!state);
		wmsLayer.setEnabled(!state);
		wmsStyles.setEnabled(!state);
		wmsServerLabel.setEnabled(!state);
		wmsLayerLabel.setEnabled(!state);
		wmsStylesLabel.setEnabled(!state);
	}

	public void setCurrentValues() {
		useLargeCursor.setSelected(swarmConfig.useLargeCursor);
		durationA.setText(Double.toString(swarmConfig.durationA));
		durationB.setText(Double.toString(swarmConfig.durationB));
		durationEnabled.setSelected(swarmConfig.durationEnabled);
		tzInstrument.setSelected(swarmConfig.useInstrumentTimeZone);
		if (swarmConfig.useLocalTimeZone)
			tzLocal.setSelected(true);
		else
			tzSpecific.setSelected(true);
		timeZones.setSelectedItem(swarmConfig.specificTimeZone.getID());

		useMapPacks.setSelected(!swarmConfig.useWMS);
		useWMS.setSelected(swarmConfig.useWMS);
		wmsServer.setText(swarmConfig.wmsServer);
		wmsLayer.setText(swarmConfig.wmsLayer);
		wmsStyles.setText(swarmConfig.wmsStyles);
		doEnables();
	}

	public boolean allowOK() {
		String message = null;
		try {
			message = "The duration magnitude constants must be numbers.";
			Double.parseDouble(durationA.getText().trim());
			Double.parseDouble(durationB.getText().trim());

			return true;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}

	public void wasOK() {
		swarmConfig.useLargeCursor = useLargeCursor.isSelected();
		swarmConfig.durationEnabled = durationEnabled.isSelected();
		swarmConfig.durationA = Double.parseDouble(durationA.getText().trim());
		swarmConfig.durationB = Double.parseDouble(durationB.getText().trim());
		swarmConfig.useInstrumentTimeZone = tzInstrument.isSelected();
		swarmConfig.useLocalTimeZone = tzLocal.isSelected();
		swarmConfig.specificTimeZone = TimeZone.getTimeZone((String) timeZones.getSelectedItem());
		swarmConfig.useWMS = useWMS.isSelected();
		swarmConfig.wmsServer = wmsServer.getText();
		swarmConfig.wmsLayer = wmsLayer.getText();
		swarmConfig.wmsStyles = wmsStyles.getText();

		Swarm.getApplication().optionsChanged();
	}
}
