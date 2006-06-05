package gov.usgs.swarm;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class OptionsDialog extends SwarmDialog
{
	private static final long serialVersionUID = 1L;

	private JPanel dialogPanel;
	
	private JCheckBox durationEnabled;
	private JTextField durationA;
	private JTextField durationB;
	private JTextField timeZoneOffset;
	private JTextField timeZoneAbbr;
	private JCheckBox useLargeCursor;
	
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
		durationB = new JTextField();;
		timeZoneOffset = new JTextField();;
		timeZoneAbbr = new JTextField();;
		useLargeCursor = new JCheckBox("Large Helicorder Cursor");
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
		builder.append("Abbreviation:", timeZoneAbbr);
		builder.nextLine();
		
		builder.append("Offset (hrs):", timeZoneOffset);
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
		timeZoneAbbr.setText(Swarm.config.timeZoneAbbr);
		timeZoneOffset.setText(Double.toString(Swarm.config.timeZoneOffset));
		useLargeCursor.setSelected(Swarm.config.useLargeCursor);
		durationA.setText(Double.toString(Swarm.config.durationA));
		durationB.setText(Double.toString(Swarm.config.durationB));
		durationEnabled.setSelected(Swarm.config.durationEnabled);
	}
	
	public boolean allowOK()
	{
		String message = null;
		try
		{
			
			message = "The time zone abbreviation must be between 1 and 4 characters.";
			String tz = timeZoneAbbr.getText();
			if (tz == null || tz.length() > 4 || tz.length() < 1)
				throw new IllegalArgumentException();
		
			message = "The time zone offset must be between -13 and 13.";
			double tzo = Double.parseDouble(timeZoneOffset.getText().trim());
			if (tzo > 13 || tzo < -13)
				throw new IllegalArgumentException();
			
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
		double tzo = 9999;
		try { tzo = Double.parseDouble(timeZoneOffset.getText().trim()); } catch (Exception e) {}
		if (tzo == 9999 || tzo > 13 || tzo < -13)
			JOptionPane.showMessageDialog(this, "Time Zone Offset has an illegal value.", "Options Error", JOptionPane.ERROR_MESSAGE);
		else
			Swarm.config.timeZoneOffset = tzo;
			
		Swarm.config.timeZoneAbbr = timeZoneAbbr.getText().trim();
		Swarm.config.useLargeCursor = useLargeCursor.isSelected();
		Swarm.config.durationEnabled = durationEnabled.isSelected();
		Swarm.config.durationA = Double.parseDouble(durationA.getText().trim());
		Swarm.config.durationB = Double.parseDouble(durationB.getText().trim());
		
		Swarm.getApplication().optionsChanged();
	}
}
