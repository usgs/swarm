package gov.usgs.swarm;

import java.awt.BorderLayout;
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
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * @author Dan Cervelli
 */
public class OptionsDialog extends SwarmDialog
{
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
	
	public OptionsDialog()
	{
		super(Swarm.getApplication(), "Options", true);
		createUI();
		setCurrentValues();
		setSizeAndLocation();
	}
	
	private void createFields()
	{
		durationEnabled = new JCheckBox("Enabled");
		durationA = new JTextField();
		durationB = new JTextField();
		useLargeCursor = new JCheckBox("Large Helicorder Cursor");
		tzInstrument = new JCheckBox("Use instrument time zone if available");
		tzLocal = new JRadioButton("Use local machine time zone:");
		tzSpecific = new JRadioButton("Use specific time zone:");
		String[] tzs = TimeZone.getAvailableIDs();
		Arrays.sort(tzs);
		timeZones = new JComboBox(tzs);
	}
	
	protected void createUI()
	{
		super.createUI();
		createFields();
		
		FormLayout layout = new FormLayout(
				"right:max(30dlu;pref), 3dlu, 30dlu, 3dlu, right:max(30dlu;pref), 3dlu, 30dlu", 
				"");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		builder.appendSeparator("Time Zone");
		
		builder.append(tzInstrument, 7);
		builder.nextLine();
		TimeZone local = TimeZone.getDefault();
		ButtonGroup tzGroup = new ButtonGroup();
		tzGroup.add(tzLocal);
		builder.append(tzLocal, 7);
		builder.append("   ");
		builder.append(new JLabel(local.getID()), 5);
		builder.nextLine();
		
		tzGroup.add(tzSpecific);
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
		
		builder.appendSeparator("Other");
		builder.append(useLargeCursor, 7);
		builder.nextLine();
		
		dialogPanel = builder.getPanel();
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}
	
	public void setCurrentValues()
	{
		useLargeCursor.setSelected(Swarm.config.useLargeCursor);
		durationA.setText(Double.toString(Swarm.config.durationA));
		durationB.setText(Double.toString(Swarm.config.durationB));
		durationEnabled.setSelected(Swarm.config.durationEnabled);
		tzInstrument.setSelected(Swarm.config.useInstrumentTimeZone);
		if (Swarm.config.useLocalTimeZone)
			tzLocal.setSelected(true);
		else
			tzSpecific.setSelected(true);
		timeZones.setSelectedItem(Swarm.config.specificTimeZone.getID());
	}
	
	public boolean allowOK()
	{
		String message = null;
		try
		{
			message = "The duration magnitude constants must be numbers.";
			Double.parseDouble(durationA.getText().trim());
			Double.parseDouble(durationB.getText().trim());
			
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;	
	}
	
	public void wasOK()
	{
		Swarm.config.useLargeCursor = useLargeCursor.isSelected();
		Swarm.config.durationEnabled = durationEnabled.isSelected();
		Swarm.config.durationA = Double.parseDouble(durationA.getText().trim());
		Swarm.config.durationB = Double.parseDouble(durationB.getText().trim());
		Swarm.config.useInstrumentTimeZone = tzInstrument.isSelected();
		Swarm.config.useLocalTimeZone = tzLocal.isSelected();
		Swarm.config.specificTimeZone = TimeZone.getTimeZone((String)timeZones.getSelectedItem());
		
		Swarm.getApplication().optionsChanged();
	}
}
