package gov.usgs.swarm.map;

import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmDialog;
import gov.usgs.swarm.map.MapPanel.LabelSetting;

import java.awt.BorderLayout;
import java.awt.geom.Point2D;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/08/14 22:46:04  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class MapSettingsDialog extends SwarmDialog
{
public static final long serialVersionUID = -1;
	
	private MapFrame map;
	
	private JPanel dialogPanel;

	private JTextField scale;
	private JTextField longitude;
	private JTextField latitude;
	
	private JTextField refreshInterval;
	private ButtonGroup labelGroup;
	private JRadioButton someLabels;
	private JRadioButton allLabels;
	private JRadioButton noLabels;
	
	private JTextField labelSource;
	
	private static MapSettingsDialog dialog;
	
	private MapSettingsDialog()
	{
		super(Swarm.getApplication(), "Map Settings", true);
		createUI();
		setSizeAndLocation();
	}
	
	private void createFields()
	{
		latitude = new JTextField();
		longitude = new JTextField();
		scale = new JTextField();
		refreshInterval = new JTextField();
		labelGroup = new ButtonGroup();
		someLabels = new JRadioButton("Some");
		allLabels = new JRadioButton("All");
		noLabels = new JRadioButton("None");
		labelGroup.add(someLabels);
		labelGroup.add(allLabels);
		labelGroup.add(noLabels);
		labelSource = new JTextField();
	}
	
	protected void createUI()
	{
		super.createUI();
		createFields();
		
		FormLayout layout = new FormLayout(
				"right:max(30dlu;pref), 3dlu, 50dlu, 3dlu, 30dlu", 
				"");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();

		builder.appendSeparator("Location");
		builder.append("Longitude:");
		builder.append(longitude);
		builder.append("degrees");
		builder.nextLine();
		builder.append("Latitude:");
		builder.append(latitude);
		builder.append("degrees");
		builder.nextLine();
		builder.append("Scale:");
		builder.append(scale);
		builder.append("m/pixel");
		builder.nextLine();
		
		builder.appendSeparator("Options");
		builder.append("Refresh Interval:");
		builder.append(refreshInterval);
		builder.append(" seconds");
		builder.nextLine();
		builder.append("Channel Labels:");
		builder.append(noLabels);
		builder.nextLine();
		builder.append(" ");
		builder.append(someLabels);
		builder.nextLine();
		builder.append(" ");
		builder.append(allLabels);
		builder.nextLine();
		builder.append("Click Labels:");
		builder.append(labelSource, 3);
		
		dialogPanel = builder.getPanel();
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}
	
	public static MapSettingsDialog getInstance(MapFrame mf)
	{
		if (dialog == null)
			dialog = new MapSettingsDialog();

		dialog.setMapFrame(mf);
		dialog.setToCurrent();
		return dialog;
	}
	
	public void setVisible(boolean b)
	{
		if (b)
			this.getRootPane().setDefaultButton(okButton);
		super.setVisible(b);
	}
	
	public void setMapFrame(MapFrame mf)
	{
		map = mf;
		setToCurrent();
	}
	
	public void setToCurrent()
	{
		MapPanel panel = map.getMapPanel();
		longitude.setText(String.format("%.4f", panel.getCenter().x));
		latitude.setText(String.format("%.4f", panel.getCenter().y));
		scale.setText(String.format("%.1f", panel.getScale()));
		refreshInterval.setText(String.format("%.2f", map.getRefreshInterval() / 1000.0));
		LabelSetting ls = panel.getLabelSetting();
		switch (ls)
		{
			case ALL:
				allLabels.setSelected(true);
				break;
			case SOME:
				someLabels.setSelected(true);
				break;
			case NONE:
				noLabels.setSelected(true);
				break;
		}
		labelSource.setText(Swarm.config.labelSource);
	}

	protected void wasOK()
	{
		try
		{
			Swarm.config.labelSource = labelSource.getText();
			MapPanel panel = map.getMapPanel();
			LabelSetting ls = LabelSetting.ALL;
			if (someLabels.isSelected())
				ls = LabelSetting.SOME;
			else if (noLabels.isSelected())
				ls = LabelSetting.NONE;
			panel.setLabelSetting(ls);
			Point2D.Double center = new Point2D.Double();
			center.x = Double.parseDouble(longitude.getText());
			center.y = Double.parseDouble(latitude.getText());
			double sc = Double.parseDouble(scale.getText());
			panel.setCenterAndScale(center, sc);
			map.setRefreshInterval(Math.round(Double.parseDouble(refreshInterval.getText()) * 1000));
			panel.loadLabels();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// don't do anything here since all validation should occur in 
			// allowOK() -- this is just worst case.
		}
	}
	
	protected boolean allowOK()
	{
		String message = null;
		try
		{
			message = "Invalid refresh interval; legal values are between 0 and 3600, 0 to refresh continuously.";
			double ri = Double.parseDouble(refreshInterval.getText());
			if (ri < 0 || ri > 3600)
				throw new NumberFormatException();
			
			message = "Invalid longitude; legal values are between -180 and 180.";
			double lon = Double.parseDouble(longitude.getText());
			if (lon < -180 || lon > 180)
				throw new NumberFormatException();
			
			message = "Invalid latitude; legal values are between -90 and 90.";
			double lat = Double.parseDouble(latitude.getText());
			if (lat < -90 || lat > 90)
				throw new NumberFormatException();
			
			message = "Invalid scale; legal values are greater than 0.";
			double sc = Double.parseDouble(scale.getText());
			if (sc <= 0)
				throw new NumberFormatException();
			
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
}
