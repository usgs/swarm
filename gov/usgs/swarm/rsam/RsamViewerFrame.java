package gov.usgs.swarm.rsam;

import gov.usgs.plot.data.RSAMData;
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
 * @author Tom Parker
 */
public class RsamViewerFrame extends JInternalFrame implements Runnable
{
	public static final long serialVersionUID = -1;
	private static final int H_TO_S = 60*60;
	
	private long intervalMs = 60 * 1000;
	private final static int[] SPANS_S = new int[] {1 * H_TO_S, 12 * H_TO_S, 24 * H_TO_S, 48 * H_TO_S, 168 * H_TO_S, 366 * H_TO_S, 672 * H_TO_S};
	private int spanIndex;
	private SeismicDataSource dataSource;
	private String channel;
	private Thread updateThread;
	private boolean kill;
	private JToolBar toolBar;
	
	private RsamViewSettings settings;
	private RsamViewPanel viewPanel;
	
	private JPanel mainPanel;
	private JPanel rsamPanel;
	
	private Throbber throbber;
	
	public RsamViewerFrame(SeismicDataSource sds, String ch)
	{
		super(ch + ", [" + sds + "]", true, true, false, true);
		dataSource = sds;
		channel = ch;
		settings = new RsamViewSettings();
		spanIndex = 1;
		kill = false;
		updateThread = new Thread(this, "RsamViewerFrame-" + sds + "-" + ch);
		createUI();
	}
	
	private void createUI()
	{
		this.setFrameIcon(Icons.rsam_values);
		mainPanel = new JPanel(new BorderLayout());
		viewPanel = new RsamViewPanel(settings);
		viewPanel.setChannel(channel);
		rsamPanel = new JPanel(new BorderLayout());
		rsamPanel.add(viewPanel, BorderLayout.CENTER);
		
		Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 3, 3), 
				LineBorder.createGrayLineBorder());
		rsamPanel.setBorder(border);

		mainPanel.add(rsamPanel, BorderLayout.CENTER);
		
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
						getRsam();
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
						if (spanIndex < SPANS_S.length - 1)
							spanIndex++;
						getRsam();
					}
				});
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);				
		toolBar.add(expXButton);
		
		toolBar.addSeparator();

		new RsamViewSettingsToolbar(settings, toolBar, this);
		
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
						SwarmInternalFrames.remove(RsamViewerFrame.this);
						dataSource.close();
					}
				});
				
		this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		this.setContentPane(mainPanel);
		this.setSize(750, 280);
		this.setVisible(true);
		
		updateThread.start();
	}
	
	public void getRsam()
	{
		throbber.increment();
		double now = CurrentTime.getInstance().nowJ2K();
		RSAMData data = dataSource.getRsam(channel, now - SPANS_S[spanIndex], now);
		System.out.println("ds: " + dataSource.getClass());
		System.out.println("2plotting rsam values; rows: " + data.rows() + " ; times: " + Util.j2KToDateString(data.getStartTime()) + " - " + Util.j2KToDateString(data.getEndTime()));

		viewPanel.setWorking(true);
		viewPanel.setData(data, now - SPANS_S[spanIndex], now);
		viewPanel.setChannel(channel);
		viewPanel.setDataSource(dataSource);
		viewPanel.setWorking(false);
		viewPanel.repaint();
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
				getRsam();
				Thread.sleep(intervalMs);
			}
			catch (InterruptedException e) {}
		}
		dataSource.close();
	}
}