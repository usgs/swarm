package gov.usgs.swarm;

import gov.usgs.util.GridBagHelper;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
 * Revision 1.13  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.12  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.11  2006/03/04 23:03:45  cervelli
 * Added alias feature. More thoroughly incorporated calibrations.  Got rid of 'waves' tab and combined all functionality under a 'channels' tab.
 *
 * Revision 1.10  2006/01/26 18:06:33  tparker
 * Correct spelling
 *
 * Revision 1.9  2006/01/26 00:29:05  tparker
 * Correct logic for clipping enabled visibility, with a bit of code cleanup.
 *
 * Revision 1.8  2006/01/25 21:52:50  tparker
 * Cleanup imports
 *
 * Revision 1.7  2006/01/25 21:45:54  tparker
 * Move clipping alert into the heli renderer.
 *
 * Revision 1.6  2006/01/21 11:04:11  tparker
 * Apply alertClip settings
 *
 * Revision 1.5  2006/01/21 01:29:20  tparker
 * First swipe at adding voice alerting of clipping. A work in progress...
 *
 * Revision 1.4  2005/08/30 00:33:43  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:28:59  tparker
 * Tidy code, no functional changes.
 *
 * Revision 1.2  2005/08/27 00:07:30  tparker
 * Create image constant
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.3  2005/04/13 00:49:04  cervelli
 * Replaced show() with overriden setVisible().
 *
 * @author Dan Cervelli
 */
public class HelicorderViewerSettingsDialog extends SwarmDialog
{
	public static final long serialVersionUID = -1;
	
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
	private JCheckBox removeDrift;
	
	private JCheckBox showClip;
	private JCheckBox alertClip;
	private JTextField alertClipTimeout;
	private JLabel alertClipTimeoutLabel;
	private JTextField clipValue;
	private JCheckBox autoScale;
	private JTextField barRange;
		
	private static HelicorderViewerSettingsDialog dialog;
	
	private HelicorderViewerSettingsDialog() 
	{
//		super(Swarm.getApplication(), "Helicorder View Settings", true, WIDTH, HEIGHT);
		super(Swarm.getApplication(), "Helicorder View Settings", true);
		dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		createUI();	
		setSizeAndLocation();
	}
	
	public static HelicorderViewerSettingsDialog getInstance(HelicorderViewerSettings s, WaveViewSettings s2)
	{
		if (dialog == null)
			dialog = new HelicorderViewerSettingsDialog();

		dialog.setSettings(s, s2);
//		dialog.setToCurrent();
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
	
	private void createComponents()
	{
		int[] values = HelicorderViewerFrame.chunkValues;
		String[] chunks = new String[values.length];
		for (int i = 0; i < chunks.length; i++)
			chunks[i] = Integer.toString(values[i] / 60);
			
		chunkList = new JComboBox(chunks);
		
		values = HelicorderViewerFrame.spanValues;
		String[] spans = new String[values.length];
		for (int i = 0; i < spans.length; i++)
			spans[i] = Integer.toString(values[i] / 60);
		
		spanList = new JComboBox(spans);
	}
	
	protected void createUI()
	{
		super.createUI();
		createComponents();
		
		// AXIS PANEL
		int[] values = HelicorderViewerFrame.chunkValues;
		String[] chunks = new String[values.length];
		for (int i = 0; i < chunks.length; i++)
			chunks[i] = Integer.toString(values[i] / 60);
			
		chunkList = new JComboBox(chunks);
		
		values = HelicorderViewerFrame.spanValues;
		String[] spans = new String[values.length];
		for (int i = 0; i < spans.length; i++)
			spans[i] = Integer.toString(values[i] / 60);
		
		spanList = new JComboBox(spans);
		
		JLabel chunkLabel = new JLabel("X, minutes:");
		chunkLabel.setLabelFor(chunkList);
		JLabel spanLabel = new JLabel("Y, hours:");
		spanLabel.setLabelFor(spanList);
		bottomTime = new JTextField();
		bottomTime.setToolTipText("Format: YYYYMMDDhhmm");
		JLabel bottomLabel = new JLabel("View time:");
		bottomLabel.setLabelFor(bottomTime);
		bottomLabel.setToolTipText("Format: YYYYMMDDhhmm");
		
		JPanel axisPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		axisPanel.setBorder(new TitledBorder(new EtchedBorder(), "Axes"));
		axisPanel.add(chunkLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		axisPanel.add(chunkList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		axisPanel.add(spanLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
		axisPanel.add(spanList, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
		axisPanel.add(bottomLabel, GridBagHelper.set(c, "x=0;y=2;w=1;h=1;wx=0;a=w;f=n"));
		axisPanel.add(bottomTime, GridBagHelper.set(c, "x=1;y=2;w=2;h=1;f=h;wx=1;a=e"));
		
		
		// ZOOM PANEL
		values = HelicorderViewerFrame.zoomValues;
		String[] zooms= new String[values.length];
		for (int i = 0; i < zooms.length; i++)
			zooms[i] = Integer.toString(values[i]);
		
		zoomList = new JComboBox(zooms);
		JLabel zoomLabel = new JLabel("Zoom, seconds:");
		zoomLabel.setLabelFor(zoomList);
		JButton waveSettingsButton = new JButton("Wave Settings", new ImageIcon(getClass().getClassLoader().getResource(Images.get("wavesettings"))));
		waveSettingsButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(waveSettings);//new WaveViewSettingsDialog(waveSettings);
						wvsd.setVisible(true);
					}
				});
		
		JPanel zoomPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		zoomPanel.setBorder(new TitledBorder(new EtchedBorder(), "Zoom"));
		zoomPanel.add(zoomLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		zoomPanel.add(zoomList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		zoomPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=3;h=1;f=h;i=5,0,5,0"));
		zoomPanel.add(waveSettingsButton, GridBagHelper.set(c, "x=0;y=2;w=3;h=1;a=e;f=n;i=0,4,0,4"));

		
		// CLIPPING PANEL
		showClip = new JCheckBox("Show clip");
		showClip.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						setShowClipEnabled(showClip.isSelected());
					}
				});
		alertClip = new JCheckBox("Audible clipping");
		alertClip.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						setAlertClipEnabled(alertClip.isSelected());
					}
				});

		alertClipTimeoutLabel = new JLabel("Alert frequency, minutes: ");
		alertClipTimeout = new JTextField(4);
		alertClipTimeoutLabel.setLabelFor(alertClipTimeout);
		
		JPanel clippingPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		clippingPanel.setBorder(new TitledBorder(new EtchedBorder(), "Clipping"));
		clippingPanel.add(showClip, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		clippingPanel.add(alertClip, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		clippingPanel.add(alertClipTimeoutLabel, GridBagHelper.set(c, "x=0;y=2;w=1;h=1;f=h;wx=0;a=w"));
		clippingPanel.add(alertClipTimeout, GridBagHelper.set(c, "x=1;y=2;w=1;h=1;f=h;wx=1;a=e"));
				

		// OTHER PANEL
		JLabel cvLabel = new JLabel("Clip threshold:");
		clipValue = new JTextField();
		cvLabel.setLabelFor(clipValue);
				
		refreshInterval = new JTextField(4);
		JLabel refreshLabel = new JLabel("Refresh, seconds: ");
		refreshLabel.setLabelFor(refreshInterval);
		JLabel scrollLabel = new JLabel("Scroll size, rows: ");
		scrollSize = new JTextField(4);
		scrollLabel.setLabelFor(scrollSize);
		
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

		JPanel otherPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		otherPanel.setBorder(new TitledBorder(new EtchedBorder(), "Other"));
		
		otherPanel.add(refreshLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
		otherPanel.add(refreshInterval, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		otherPanel.add(scrollLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
		otherPanel.add(scrollSize, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
		removeDrift = new JCheckBox("Force Center");
		otherPanel.add(removeDrift, GridBagHelper.set(c, "x=0;y=2;w=3;h=1;f=h;wx=0;a=w"));

		otherPanel.add(autoScale, GridBagHelper.set(c, "x=0;y=6;w=3;h=1;f=h;wx=0;a=w"));
		otherPanel.add(obrLabel, GridBagHelper.set(c, "x=0;y=7;w=2;h=1;wx=1;a=w;f=n"));
		otherPanel.add(barRange, GridBagHelper.set(c, "x=2;y=7;w=1;h=1;f=h;wx=0;a=e"));
		
		otherPanel.add(cvLabel, GridBagHelper.set(c, "x=0;y=8;w=2;h=1;wx=1;a=w;f=n"));
		otherPanel.add(clipValue, GridBagHelper.set(c, "x=2;y=8;w=1;h=1;f=h;wx=0;a=e"));
		
		
		// CENTER PANEL
		JPanel centerPanel = new JPanel();
		BoxLayout bl = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
		centerPanel.setLayout(bl);

		centerPanel.add(axisPanel);
		centerPanel.add(zoomPanel);	
		centerPanel.add(clippingPanel);
		centerPanel.add(otherPanel);

		// DIALOG PANEL
		dialogPanel = new JPanel(new BorderLayout());
		dialogPanel.add(centerPanel, BorderLayout.CENTER);
		
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}
	
	private void setShowClipEnabled(final boolean b)
	{
		SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						alertClip.setEnabled(b);
						setAlertClipEnabled(b);
						return null;
					}
					
					public void finished() {}
				};
		worker.start();
	}

	private void setAlertClipEnabled(final boolean b)
	{
		SwingWorker worker = new SwingWorker()
		{
			public Object construct()
			{
				boolean state = showClip.isSelected() && alertClip.isSelected();
				alertClipTimeout.setEnabled(state);
				alertClipTimeoutLabel.setEnabled(state);
				return null;
			}
			
			public void finished() {}
		};
		worker.start();
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
		
		String span = Integer.toString((int)(settings.span / 60.0));
		spanList.setSelectedItem(span);
		
		String wzo = Integer.toString(settings.waveZoomOffset);
		zoomList.setSelectedItem(wzo);
		
		double bt = settings.getBottomTime();
		if (Double.isNaN(bt))
			bottomTime.setText("Now");
		else
		{
			double tzo = Swarm.config.timeZoneOffset;
			bottomTime.setText(dateFormat.format(Util.j2KToDate(bt + tzo * 3600)));
		}
		
		refreshInterval.setText(Integer.toString(settings.refreshInterval));
		scrollSize.setText(Integer.toString(settings.scrollSize));
		
		removeDrift.setSelected(settings.forceCenter);
		
		clipValue.setText(Integer.toString(settings.clipValue));
		barRange.setText(Integer.toString(settings.barRange));
		autoScale.setSelected(settings.autoScale);
		showClip.setSelected(settings.showClip);
		alertClip.setSelected(settings.alertClip);
		
		String alertTO = Integer.toString(settings.alertClipTimeout / 60);
		alertClipTimeout.setText(alertTO);
		
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
				double tzo = Swarm.config.timeZoneOffset;
				settings.setBottomTime(Util.dateToJ2K(bt) - tzo * 3600);
			}
			settings.forceCenter = removeDrift.isSelected();
			settings.showClip = showClip.isSelected();
			settings.alertClip = alertClip.isSelected();
			settings.alertClipTimeout = Integer.parseInt(alertClipTimeout.getText()) * 60;
			
//			Calibration cb = Swarm.getParentFrame().getCalibration(channel);

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
			if (ss < 1 || ss > 48)
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
