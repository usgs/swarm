package gov.usgs.swarm;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.ui.BaseDialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Global application options dialog.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2004/10/28 20:17:09  cvs
 * Big red mouse cursor option.
 *
 * @author Dan Cervelli
 */
public class OptionsDialog extends BaseDialog
{
	public static final long serialVersionUID = -1;
	
	private static final int WIDTH = 240;
	private static final int HEIGHT = 280;	
	
	private JPanel dialogPanel;
	
	private JPanel timeZonePanel;
	private JTextField timeZoneAbbr;
	private JTextField timeZoneOffset;
	
	private JPanel magPanel;
	private JTextField durationA;
	private JTextField durationB;
	
	private JPanel otherPanel;
	private JCheckBox useLargeCursor;
	
	public OptionsDialog()
	{
		super(Swarm.getParentFrame(), "Options", true, WIDTH, HEIGHT);
		createOptionsUI();
	}
	
	public void createOptionsUI()
	{
		dialogPanel = new JPanel();
		BoxLayout bl = new BoxLayout(dialogPanel, BoxLayout.Y_AXIS);
		dialogPanel.setLayout(bl);
		
		timeZonePanel = new JPanel(new GridLayout(2, 2));
		timeZonePanel.setBorder(new TitledBorder(new EtchedBorder(), "Time Zone"));
		JLabel tz1 = new JLabel("Abreviation:");
		JLabel tz2 = new JLabel("Offset (hrs):");
		timeZoneAbbr = new JTextField(4);
		timeZoneOffset = new JTextField(4);
		tz1.setLabelFor(timeZoneAbbr);
		tz2.setLabelFor(timeZoneOffset);
		timeZonePanel.add(tz1);
		timeZonePanel.add(timeZoneAbbr);
		timeZonePanel.add(tz2);
		timeZonePanel.add(timeZoneOffset);
		
		dialogPanel.add(timeZonePanel);
	
		magPanel = new JPanel();
		magPanel.setBorder(new TitledBorder(new EtchedBorder(), "Duration Magnitude"));
		durationA = new JTextField(4);
		durationB = new JTextField(4);
		magPanel.add(new JLabel("Md="));
		magPanel.add(durationA);
		magPanel.add(new JLabel("* Log(t) +"));
		magPanel.add(durationB);
		dialogPanel.add(magPanel);
		
		otherPanel = new JPanel();
		otherPanel.setBorder(new TitledBorder(new EtchedBorder(), "Other"));
		useLargeCursor = new JCheckBox("Large helicorder cursor");
		otherPanel.add(useLargeCursor);
		dialogPanel.add(otherPanel);
		
		setCurrentValues();
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}
	
	public void setCurrentValues()
	{
		ConfigFile config = Swarm.getParentFrame().getConfig();
		timeZoneAbbr.setText(config.getString("timeZoneAbbr"));
		timeZoneOffset.setText(config.getString("timeZoneOffset"));
		useLargeCursor.setSelected(config.getString("useLargeCursor").equals("true"));
		durationA.setText(config.getString("durationA"));
		durationB.setText(config.getString("durationB"));
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
		ConfigFile config = Swarm.getParentFrame().getConfig();
		double tzo = 9999;
		try { tzo = Double.parseDouble(timeZoneOffset.getText().trim()); } catch (Exception e) {}
		if (tzo == 9999 || tzo > 13 || tzo < -13)
			JOptionPane.showMessageDialog(this, "Time Zone Offset has an illegal value.", "Options Error", JOptionPane.ERROR_MESSAGE);
		else
			config.put("timeZoneOffset", Double.toString(tzo), false);
			
		config.put("timeZoneAbbr", timeZoneAbbr.getText().trim(), false);
		config.put("useLargeCursor", (useLargeCursor.isSelected() ? "true" : "false"), false);
		config.put("durationA", durationA.getText().trim(), false);
		config.put("durationB", durationB.getText().trim(), false);
		
		Swarm.getParentFrame().optionsChanged();
	}
}