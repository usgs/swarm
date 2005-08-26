package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * @author Dan Cervelli
 */
public class WaveViewerFrame extends JInternalFrame implements Runnable
{
	public static final long serialVersionUID = -1;
	private long interval = 2000;
//	private int frames = 30;
	private final static int[] SPANS = new int[] {15, 30, 60, 120, 180, 240, 300};
	private int spanIndex;
//	private Swarm swarm;
	private SeismicDataSource dataSource;
	private String channel;
	private Thread updateThread;
	private boolean kill;
	private JToolBar toolbar;
	private JButton showToolbar;
	
	private WaveViewSettings settings;
	private WaveViewPanel waveViewPanel;
	
//	private WaveComponent waveComponent;
//	private WaveViewChooser waveChooser;
//	private WaveModel model;
	
	private JPanel mainPanel;
	private JPanel wavePanel;
	
	private double now;
	
	public WaveViewerFrame(Swarm sw, SeismicDataSource sds, String ch)
	{
		super("[" + sds + "]: " + ch, true, true, false, true);
//		swarm = sw;
		dataSource = sds;
		channel = ch;
		settings = new WaveViewSettings();
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						createUI();
					}
				});
		spanIndex = 3;
		kill = false;
		updateThread = new Thread(this, "Update Thread: " + sds + " " + ch);
	}
	
	public void createUI()
	{
		mainPanel = new JPanel(new BorderLayout());
		waveViewPanel = new WaveViewPanel(settings);
		wavePanel = new JPanel(new BorderLayout());
		wavePanel.setBorder(LineBorder.createGrayLineBorder());
		wavePanel.add(waveViewPanel, BorderLayout.CENTER);
//		waveComponent = new Waveform();
//		waveComponent = new Spectra();
//		model = new WaveModel();
//		waveComponent.setModel(model);
//		waveChooser = new WaveViewChooser();
//		waveChooser.setModel(model);
//		waveChooser.setViewCallback(new ActionListener()
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						wavePanel.remove(waveComponent);
//						waveComponent = waveChooser.getWaveComponent();
//						waveComponent.setViewTimes(now - SPANS[spanIndex], now);
//						wavePanel.add(waveComponent);
//						validate();
//					}
//				});
//		waveComponent = waveChooser.getWaveComponent();
//		wavePanel.add(waveComponent, BorderLayout.CENTER);
		mainPanel.add(wavePanel, BorderLayout.CENTER);
		
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		JButton hideTB = new JButton(new ImageIcon("images/minimize.png"));
		hideTB.setToolTipText("Hide toolbar");
		hideTB.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						showToolbar.setVisible(true);
						mainPanel.remove(toolbar);
						mainPanel.doLayout();
						repaint();
					}
				});
		hideTB.setMargin(new Insets(0,0,0,0));
		toolbar.add(hideTB);
		toolbar.addSeparator();
		toolbar.setRollover(true);
		
		JButton compXButton = new JButton(new ImageIcon("images/xminus.png"));
		compXButton.setMargin(new Insets(0,0,0,0));
		compXButton.setToolTipText("Shrink time axis (Alt-left arrow)");
		compXButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex != 0)
							spanIndex--;
					}
				});
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		toolbar.add(compXButton);
		
		JButton expXButton = new JButton(new ImageIcon("images/xplus.png"));
		expXButton.setMargin(new Insets(0,0,0,0));
		expXButton.setToolTipText("Expand time axis (Alt-right arrow)");
		expXButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex < SPANS.length - 1)
							spanIndex++;
					}
				});
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);				
		toolbar.add(expXButton);
		toolbar.addSeparator();

//		waveChooser.addButtonsToToolbar(toolbar);
		new WaveViewSettingsToolbar(settings, toolbar, this);
		JButton clipboard = new JButton(new ImageIcon("images/clipboard.png"));
		toolbar.add(clipboard);
		clipboard.setMargin(new Insets(0,0,0,0));
		clipboard.setToolTipText("Copy wave to clipboard (C or Ctrl-C)");
		clipboard.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (waveViewPanel != null)
							Swarm.getParentFrame().getWaveClipboard().addWave(new ClipboardWaveViewPanel(waveViewPanel));
					}
				});
		Util.mapKeyStrokeToButton(this, "C", "clipboard1", clipboard);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard2", clipboard);
		toolbar.add(clipboard);
		toolbar.addSeparator();
		
		mainPanel.add(toolbar, BorderLayout.NORTH);		
		showToolbar = new JButton(new ImageIcon("images/maximize.png"));
		showToolbar.setMargin(new Insets(0, 0, 0, 0));
		showToolbar.setSize(24, 24);
		showToolbar.setLocation(0, 0);
		showToolbar.setVisible(false);
		showToolbar.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						showToolbar.setVisible(false);
						mainPanel.add(toolbar, BorderLayout.PAGE_START);	
						mainPanel.doLayout();
					}
				});
		this.getLayeredPane().setLayer(showToolbar, JLayeredPane.PALETTE_LAYER.intValue());
		this.getLayeredPane().add(showToolbar);
		
		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameClosing(InternalFrameEvent e)
					{
						kill();	
						Swarm.getParentFrame().removeInternalFrame(WaveViewerFrame.this);
						dataSource.close();
					}
				});
				
		//this.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
		this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		this.setContentPane(mainPanel);
		this.setSize(750, 280);
		this.setVisible(true);
		
		updateThread.start();
	}
	
//	private boolean busy;
	public void getWave()
	{
//		if (busy)
//			return;
		//double now = Util.nowJ2K();
		/*
		final SwingWorker worker = new SwingWorker()
		{ 
			public Object construct()
			{
				busy = true;
				now = CurrentTime.nowJ2K();
				Wave sw = dataSource.getWave(channel, now - SPANS[spanIndex]-20, now);
				model.setWave(new Wave(sw));
				return null;
			}
			
			public void finished()
			{
				busy = false;
			}
		};
		 
		worker.start();
		*/
		now = CurrentTime.nowJ2K();
		Wave sw = dataSource.getWave(channel, now - SPANS[spanIndex], now);
		waveViewPanel.setWorking(true);
		waveViewPanel.setWave(sw, now - SPANS[spanIndex], now);
		waveViewPanel.setChannel(channel);
		waveViewPanel.setDataSource(dataSource);
		waveViewPanel.setWorking(false);
//		model.setWave(sw);
//		waveComponent.setViewTimes(now - SPANS[spanIndex], now);
//		Wave sw = dataSource.getWave(channel, now - SPANS[spanIndex]-20, now);
//		model.setWave(new Wave(sw));
	}
	
	public void kill()
	{
		kill = true;
		updateThread.interrupt();
	}
	
	public void run()
	{
		Swarm.getParentFrame().incThreadCount();
//		int count = 0;
		while (!kill)
		{
			try
			{
//				now = CurrentTime.nowJ2K();
//				waveComponent.setViewTimes(now - SPANS[spanIndex]- 10, now-10);
//				if (count % frames == 0)
				getWave();
//				waveComponent.doCompleteRepaint();
//				count++;
				Thread.sleep(interval);
			}
			catch (InterruptedException e) {}
		}
		Swarm.getParentFrame().decThreadCount();
		dataSource.close();
		System.out.println(updateThread.getName() + " killed");
		
	}
}