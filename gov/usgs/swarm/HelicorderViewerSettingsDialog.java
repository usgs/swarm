package gov.usgs.swarm;

import gov.usgs.util.GridBagHelper;
import gov.usgs.util.Util;
import gov.usgs.util.ui.BaseDialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.3  2005/04/13 00:49:04  cervelli
 * Replaced show() with overriden setVisible().
 *
 * @author Dan Cervelli
 */
public class HelicorderViewerSettingsDialog extends BaseDialog
{
	public static final long serialVersionUID = -1;
	
	private static final int WIDTH = 240;
	private static final int HEIGHT = 500;	
	
	private HelicorderViewerSettings settings;
	private WaveViewSettings waveSettings;
	
	private JPanel dialogPanel;
	
	private JComboBox chunkList;
	private JComboBox spanList;
	private JTextField bottomTime;
	private JComboBox zoomList;
	private JTextField refreshInterval;
	private JTextField scrollSize;
	private DateFormat dateFormat;
	private JTextField clipBars;
	private JCheckBox removeDrift;
	private JCheckBox showWiggler;
	
	private JCheckBox showClip;
	private JTextField clipValue;
	private JCheckBox autoScale;
	private JTextField barRange;
	
	private static final String IMAGE_WAVESETTINGS = "images/wavesettings.png";
	
	private static HelicorderViewerSettingsDialog dialog;
	
	private HelicorderViewerSettingsDialog() 
	{
		super(Swarm.getParentFrame(), "Helicorder View Settings", true, WIDTH, HEIGHT);
		dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//		settings = s;
//		waveSettings = s2;
		createSettingsUI();	
	}
	
	public static HelicorderViewerSettingsDialog getInstance(HelicorderViewerSettings s, WaveViewSettings s2)
	{
		if (dialog == null)
			dialog = new HelicorderViewerSettingsDialog();

		dialog.setSettings(s, s2);
		dialog.setToCurrent();
		return dialog;
	}
	
	public void setVisible(boolean b)
	{
		if (b)
			this.getRootPane().setDefaultButton(okButton);
		super.setVisible(b);
	}
	
	public void setSettings(HelicorderViewerSettings s, WaveViewSettings s2)
	{
		settings = s;
		waveSettings = s2;
		setToCurrent();
	}
	
	private void createSettingsUI()
	{
		dialogPanel = new JPanel(new BorderLayout());

		JPanel centerPanel = new JPanel();
		BoxLayout bl = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
		centerPanel.setLayout(bl);
		
		int[] values = HelicorderViewerFrame.chunkValues;
		String[] chunks = new String[values.length];
		for (int i = 0; i < chunks.length; i++)
			chunks[i] = Integer.toString(values[i] / 60);
			
//		chunkList = new JComboBox(new String[] {"10", "15", "20", "30", "60", "120", "180", "360"});
		chunkList = new JComboBox(chunks);
		
		values = HelicorderViewerFrame.spanValues;
		String[] spans = new String[values.length];
		for (int i = 0; i < spans.length; i++)
			spans[i] = Integer.toString(values[i] / 60);
		
//		spanList = new JComboBox(new String[] {"2", "4", "6", "12",	"24", "48", "72", "168"});
		spanList = new JComboBox(spans);
		JPanel axisPanel = new JPanel(new GridLayout(3, 2));
		axisPanel.setBorder(new TitledBorder(new EtchedBorder(), "Axes"));
		JLabel chunkLabel = new JLabel("X, minutes:");
		axisPanel.add(chunkLabel);
		chunkLabel.setLabelFor(chunkList);
		axisPanel.add(chunkList);
		JLabel spanLabel = new JLabel("Y, hours:");
		axisPanel.add(spanLabel);
		spanLabel.setLabelFor(spanList);
		axisPanel.add(spanList);
		bottomTime = new JTextField();
		bottomTime.setToolTipText("Format: YYYYMMDDhhmm");
		JLabel bottomLabel = new JLabel("View time:");
		bottomLabel.setLabelFor(bottomTime);
		bottomLabel.setToolTipText("Format: YYYYMMDDhhmm");
		axisPanel.add(bottomLabel);
		axisPanel.add(bottomTime);
		
		JPanel zoomPanel = new JPanel(new GridLayout(2, 2));
		zoomPanel.setBorder(new TitledBorder(new EtchedBorder(), "Zoom"));
		
		values = HelicorderViewerFrame.zoomValues;
		String[] zooms= new String[values.length];
		for (int i = 0; i < zooms.length; i++)
			zooms[i] = Integer.toString(values[i]);
		
		zoomList = new JComboBox(zooms);
		JLabel zoomLabel = new JLabel("Zoom, seconds:");
		zoomLabel.setLabelFor(zoomList);
		zoomPanel.add(zoomLabel);
		zoomPanel.add(zoomList);
		//JButton waveSettingsButton = new JButton("Wave Settings", new ImageIcon("images/wavesettings.png"));
		JButton waveSettingsButton = new JButton("Wave Settings", new ImageIcon(getClass().getClassLoader().getResource(IMAGE_WAVESETTINGS)));
		zoomPanel.add(waveSettingsButton);
		waveSettingsButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(waveSettings);//new WaveViewSettingsDialog(waveSettings);
						wvsd.setVisible(true);
					}
				});
		refreshInterval = new JTextField(4);
		JLabel refreshLabel = new JLabel("Refresh, seconds: ");
		refreshLabel.setLabelFor(refreshInterval);
		JLabel scrollLabel = new JLabel("Scroll size, rows: ");
		scrollSize = new JTextField(4);
		scrollLabel.setLabelFor(scrollSize);
		JLabel clipLabel = new JLabel("Clip, rows:");
		clipBars = new JTextField(4);
		clipLabel.setLabelFor(clipBars);
		
		showClip = new JCheckBox("Show clip");
		JLabel cvLabel = new JLabel("Clip threshold:");
		clipValue = new JTextField();
		cvLabel.setLabelFor(clipValue);
		
		autoScale = new JCheckBox("Auto-scale");
		autoScale.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						setAutoScaleEnabled(autoScale.isSelected());
					}
				});
		JLabel obrLabel = new JLabel("One bar range:");
		barRange= new JTextField();
		obrLabel.setLabelFor(barRange);

		axisPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		axisPanel.setBorder(new TitledBorder(new EtchedBorder(), "Axes"));
		axisPanel.add(chunkLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		axisPanel.add(chunkList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		axisPanel.add(spanLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
		axisPanel.add(spanList, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
		axisPanel.add(bottomLabel, GridBagHelper.set(c, "x=0;y=2;w=1;h=1;wx=0;a=w;f=n"));
		axisPanel.add(bottomTime, GridBagHelper.set(c, "x=1;y=2;w=2;h=1;f=h;wx=1;a=e"));

		zoomPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		zoomPanel.setBorder(new TitledBorder(new EtchedBorder(), "Zoom"));
		zoomPanel.add(zoomLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		zoomPanel.add(zoomList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		zoomPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=3;h=1;f=h;i=5,0,5,0"));
		zoomPanel.add(waveSettingsButton, GridBagHelper.set(c, "x=0;y=2;w=3;h=1;a=e;f=n;i=0,4,0,4"));
		
		JPanel otherPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		otherPanel.setBorder(new TitledBorder(new EtchedBorder(), "Other"));
		otherPanel.add(refreshLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		otherPanel.add(refreshInterval, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		otherPanel.add(scrollLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
		otherPanel.add(scrollSize, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
		//otherPanel.add(clipLabel, GridBagHelper.set(c, "x=0;y=2;w=2;h=1;wx=1;a=w;f=n"));
		//otherPanel.add(clipBars, GridBagHelper.set(c, "x=2;y=2;w=1;h=1;f=h;wx=0;a=e"));
		removeDrift = new JCheckBox("Force Center");
		otherPanel.add(removeDrift, GridBagHelper.set(c, "x=0;y=2;w=3;h=1;f=h;wx=0;a=w"));

		otherPanel.add(showClip, GridBagHelper.set(c, "x=0;y=3;w=3;h=1;f=h;wx=0;a=w"));
		
		otherPanel.add(autoScale, GridBagHelper.set(c, "x=0;y=4;w=3;h=1;f=h;wx=0;a=w"));
		otherPanel.add(obrLabel, GridBagHelper.set(c, "x=0;y=5;w=2;h=1;wx=1;a=w;f=n"));
		otherPanel.add(barRange, GridBagHelper.set(c, "x=2;y=5;w=1;h=1;f=h;wx=0;a=e"));
		
		otherPanel.add(cvLabel, GridBagHelper.set(c, "x=0;y=6;w=2;h=1;wx=1;a=w;f=n"));
		otherPanel.add(clipValue, GridBagHelper.set(c, "x=2;y=6;w=1;h=1;f=h;wx=0;a=e"));
		
		showWiggler = new JCheckBox("Show Wiggler");
		//otherPanel.add(showWiggler, GridBagHelper.set(c, "x=0;y=4;w=3;h=1;f=h;wx=0;a=w"));
		
		centerPanel.add(axisPanel);
		centerPanel.add(zoomPanel);		
		centerPanel.add(otherPanel);
		
		dialogPanel.add(centerPanel, BorderLayout.CENTER);
		
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
		//setToCurrent();
	}
	
	private void setAutoScaleEnabled(final boolean b)
	{
		SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						clipValue.setEnabled(!b);
						barRange.setEnabled(!b);
						return null;
					}
					
					public void finished() {}
				};
		worker.start();
	}
	
	public void setToCurrent()
	{
		if (settings == null || waveSettings == null)
			return;
		
		String tc = Integer.toString((int)(settings.timeChunk / 60.0));
		chunkList.setSelectedItem(tc);
		
		String span = Integer.toString((int)(settings.span / 60));
		spanList.setSelectedItem(span);
		
		String wzo = Integer.toString(settings.waveZoomOffset);
		zoomList.setSelectedItem(wzo);
		
		double bt = settings.getBottomTime();
		if (Double.isNaN(bt))
			bottomTime.setText("Now");
		else
		{
			double tzo = Double.parseDouble(Swarm.getParentFrame().getConfig().getString("timeZoneOffset"));
			bottomTime.setText(dateFormat.format(Util.j2KToDate(bt + tzo * 3600)));
		}
		
		refreshInterval.setText(Integer.toString(settings.refreshInterval));
		scrollSize.setText(Integer.toString(settings.scrollSize));
		
		removeDrift.setSelected(settings.forceCenter);
		
		clipBars.setText(Integer.toString(settings.clipBars));
		showWiggler.setSelected(settings.showWiggler);
		
		clipValue.setText(Integer.toString(settings.clipValue));
		barRange.setText(Integer.toString(settings.barRange));
		autoScale.setSelected(settings.autoScale);
		showClip.setSelected(settings.showClip);
		
		setAutoScaleEnabled(settings.autoScale);
	}

	protected void wasOK()
	{
		try
		{
			settings.timeChunk = Integer.parseInt(chunkList.getSelectedItem().toString()) * 60;
			settings.span = Integer.parseInt(spanList.getSelectedItem().toString()) * 60;
			settings.waveZoomOffset = Integer.parseInt(zoomList.getSelectedItem().toString());
			settings.refreshInterval = Integer.parseInt(refreshInterval.getText());
			settings.scrollSize = Integer.parseInt(scrollSize.getText());;
			if (bottomTime.getText().toLowerCase().equals("now"))
				settings.setBottomTime(Double.NaN);
			else
			{
				String t = bottomTime.getText();
				if (t.length() == 8)
					t = t + "2359";
				Date bt = dateFormat.parse(t);
				double tzo = Double.parseDouble(Swarm.getParentFrame().getConfig().getString("timeZoneOffset"));
				settings.setBottomTime(Util.dateToJ2K(bt) - tzo * 3600);
			}
			settings.forceCenter = removeDrift.isSelected();
			settings.clipBars = Integer.parseInt(clipBars.getText());
			settings.showWiggler = showWiggler.isSelected();
			settings.showClip = showClip.isSelected();
			settings.clipValue = Integer.parseInt(clipValue.getText());
			settings.autoScale = autoScale.isSelected();
			settings.barRange = Integer.parseInt(barRange.getText());
			settings.notifyView();
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
			message = "Invalid time; legal format is 'YYYYMMDD' or 'YYYYMMDDhhmm' or 'Now'.";
			// no bottom time
			if (!bottomTime.getText().toLowerCase().equals("now"))
			{
				Date bt = null;
				String t = bottomTime.getText();
				if (t.length() == 8)
					t = t + "2359";
				bt = dateFormat.parse(t);
				if (bt == null)
					throw new ParseException(null, 0);
			}
			
			// validate
			message = "Invalid refresh interval; legal values are between 0 and 3600, 0 for no refresh.";
			int ri = Integer.parseInt(refreshInterval.getText());
			if (ri < 0 || ri > 3600)
				throw new NumberFormatException();
			
			message = "Invalid scroll size; legal values are between 1 and 48.";
			int ss = Integer.parseInt(scrollSize.getText());
			if (ss <= 0 || ss >= 48)
				throw new NumberFormatException();
				
			message = "Invalid clip size; legal values must be 0 or above.";
			int c = Integer.parseInt(clipBars.getText());
			if (c < 0)
				throw new NumberFormatException();
			
			message = "Invalid clip value.";
			Integer.parseInt(clipValue.getText());
			
			message = "Invalid one bar range.";
			Integer.parseInt(barRange.getText());
			
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
}
