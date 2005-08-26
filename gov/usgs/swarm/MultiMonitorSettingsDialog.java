package gov.usgs.swarm;

import gov.usgs.util.GridBagHelper;
import gov.usgs.util.ui.BaseDialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * A dialog for Monitor Mode Settings.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2005/04/15 04:36:40  cervelli
 * More JDK 1.5 deprecation stuff.
 *
 * Revision 1.2  2005/04/11 00:23:05  cervelli
 * Removed JDK 1.5 deprecated Dialog.show().
 *
 * Revision 1.1  2004/10/28 20:23:36  cvs
 * Initial revision.
 *
 * @author Dan Cervelli
 */
public class MultiMonitorSettingsDialog extends BaseDialog
{
	public static final long serialVersionUID = -1;
	
	private static final int WIDTH = 280;
	private static final int HEIGHT = 160;	
	
	private MultiMonitor monitor;
	
	private JPanel dialogPanel;
	
	private JComboBox spanList;
	private JTextField refreshInterval;
	
	private static MultiMonitorSettingsDialog dialog;
	
	private MultiMonitorSettingsDialog()
	{
		super(Swarm.getParentFrame(), "Monitor Settings", true, WIDTH, HEIGHT);
		createSettingsUI();	
	}
	
	public static MultiMonitorSettingsDialog getInstance(MultiMonitor mm)
	{
		if (dialog == null)
			dialog = new MultiMonitorSettingsDialog();

		dialog.setMonitor(mm);
		dialog.setToCurrent();
		return dialog;
	}
	
	public void setVisible(boolean b)
	{
		if (b)
			this.getRootPane().setDefaultButton(okButton);
		super.setVisible(b);
	}
	
	public void setMonitor(MultiMonitor mm)
	{
		monitor = mm;
		setToCurrent();
	}
	
	private void createSettingsUI()
	{
		dialogPanel = new JPanel(new GridBagLayout());

		int[] values = MultiMonitor.SPANS;
		String[] spans = new String[values.length];
		for (int i = 0; i < spans.length; i++)
			spans[i] = Integer.toString(values[i]);
		
		spanList = new JComboBox(spans);
		JLabel spanLabel = new JLabel("Span, seconds: ");
		
		refreshInterval = new JTextField();
		JLabel intervalLabel = new JLabel("Refresh interval, seconds:");
		
		GridBagConstraints c = new GridBagConstraints();
		dialogPanel.setBorder(new TitledBorder(new EtchedBorder(), "Options"));
		dialogPanel.add(spanLabel, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;a=w;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
		dialogPanel.add(spanList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
		dialogPanel.add(intervalLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
		dialogPanel.add(refreshInterval, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
		
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}
	
	public void setToCurrent()
	{
		refreshInterval.setText(Long.toString(monitor.getRefreshInterval() / 1000));
		String span = Integer.toString((int)(monitor.getSpan()));
		spanList.setSelectedItem(span);
	}

	protected void wasOK()
	{
		try
		{
			monitor.setSpan(Integer.parseInt(spanList.getSelectedItem().toString()));
			monitor.setRefreshInterval(Integer.parseInt(refreshInterval.getText()) * 1000);
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
			int ri = Integer.parseInt(refreshInterval.getText());
			if (ri < 0 || ri > 3600)
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
