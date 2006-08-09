package gov.usgs.swarm.wave;

import gov.usgs.swarm.Images;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/08/09 05:09:18  cervelli
 * Closes throbber to eliminate memory leak.
 *
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.9  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.8  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.7  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.6  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.5  2005/09/02 16:40:17  dcervelli
 * CurrentTime changes.
 *
 * Revision 1.4  2005/08/30 00:34:40  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:32:40  tparker
 * Tidy code, no functional changes.
 *
 * Revision 1.2  2005/08/27 00:20:25  tparker
 * Create image constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * @author Dan Cervelli
 */
public class WaveViewerFrame extends JInternalFrame implements Runnable
{
	public static final long serialVersionUID = -1;
	
	private long interval = 2000;
	private final static int[] SPANS = new int[] {15, 30, 60, 120, 180, 240, 300};
	private int spanIndex;
	private SeismicDataSource dataSource;
	private String channel;
	private Thread updateThread;
	private boolean kill;
	private JToolBar toolBar;
	
	private WaveViewSettings settings;
	private WaveViewPanel waveViewPanel;
	
	private JPanel mainPanel;
	private JPanel wavePanel;
	
	private Throbber throbber;
	
	public WaveViewerFrame(SeismicDataSource sds, String ch)
	{
		super(ch + ", [" + sds + "]", true, true, false, true);
		dataSource = sds;
		channel = ch;
		settings = new WaveViewSettings();
		spanIndex = 3;
		kill = false;
		updateThread = new Thread(this, "WaveViewerFrame-" + sds + "-" + ch);
		createUI();
	}
	
	public void createUI()
	{
		this.setFrameIcon(Images.getIcon("wave"));
		mainPanel = new JPanel(new BorderLayout());
		waveViewPanel = new WaveViewPanel(settings);
		wavePanel = new JPanel(new BorderLayout());
		wavePanel.add(waveViewPanel, BorderLayout.CENTER);
		Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 3, 3), 
				LineBorder.createGrayLineBorder());
		wavePanel.setBorder(border);

		mainPanel.add(wavePanel, BorderLayout.CENTER);
		
		toolBar = SwarmUtil.createToolBar();
		
		JButton compXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xminus"),
				"Shrink time axis (Alt-left arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex != 0)
							spanIndex--;
					}
				});
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		toolBar.add(compXButton);
		
		JButton expXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xplus"),
				"Expand time axis (Alt-right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex < SPANS.length - 1)
							spanIndex++;
					}
				});
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);				
		toolBar.add(expXButton);
		
		toolBar.addSeparator();

		new WaveViewSettingsToolbar(settings, toolBar, this);
		
		JButton clipboard = SwarmUtil.createToolBarButton(
				Images.getIcon("clipboard"),
				"Copy wave to clipboard (C or Ctrl-C)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (waveViewPanel != null)
						{
							WaveClipboardFrame cb = Swarm.getApplication().getWaveClipboard();
							cb.setVisible(true);
							cb.addWave(new WaveViewPanel(waveViewPanel));
						}
					}
				});
		Util.mapKeyStrokeToButton(this, "C", "clipboard1", clipboard);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard2", clipboard);
		toolBar.add(clipboard);
		
		toolBar.addSeparator();
		
		toolBar.add(Box.createHorizontalGlue());
		
		throbber = new Throbber();
		toolBar.add(throbber);
		
		mainPanel.add(toolBar, BorderLayout.NORTH);
		
		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameActivated(InternalFrameEvent e)
					{
						if (channel != null)
							Swarm.getApplication().getDataChooser().setNearest(channel);
					}
					
					public void internalFrameClosing(InternalFrameEvent e)
					{
						throbber.close();
						kill();	
						Swarm.getApplication().removeInternalFrame(WaveViewerFrame.this);
						dataSource.close();
					}
				});
				
		this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		this.setContentPane(mainPanel);
		this.setSize(750, 280);
		this.setVisible(true);
		
		updateThread.start();
	}
	
	public void getWave()
	{
		throbber.increment();
		double now = CurrentTime.getInstance().nowJ2K();
		Wave sw = dataSource.getWave(channel, now - SPANS[spanIndex], now);
//		System.out.println(sw);
		waveViewPanel.setWorking(true);
		waveViewPanel.setWave(sw, now - SPANS[spanIndex], now);
		waveViewPanel.setChannel(channel);
		waveViewPanel.setDataSource(dataSource);
		waveViewPanel.setWorking(false);
		waveViewPanel.repaint();
		throbber.decrement();
	}
	
	public void kill()
	{
		kill = true;
		updateThread.interrupt();
	}
	
	public void run()
	{
		while (!kill)
		{
			try
			{
				getWave();
				Thread.sleep(interval);
			}
			catch (InterruptedException e) {}
		}
		dataSource.close();
	}
}