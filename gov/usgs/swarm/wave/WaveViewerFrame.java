package gov.usgs.swarm.wave;

import gov.usgs.plot.data.Wave;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.chooser.DataChooser;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;

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
		this.setFrameIcon(Icons.wave);
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
				Icons.xminus,
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
				Icons.xplus,
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
				Icons.clipboard,
				"Copy wave to clipboard (C or Ctrl-C)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (waveViewPanel != null)
						{
							WaveClipboardFrame cb = WaveClipboardFrame.getInstance();
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
							DataChooser.getInstance().setNearest(channel);
					}
					
					public void internalFrameClosing(InternalFrameEvent e)
					{
						throbber.close();
						kill();	
						SwarmInternalFrames.remove(WaveViewerFrame.this);
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