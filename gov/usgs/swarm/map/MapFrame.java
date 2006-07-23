package gov.usgs.swarm.map;

import gov.usgs.plot.map.GeoRange;
import gov.usgs.swarm.HelicorderViewPanelListener;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.MultiMonitor;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * 
 * @author Dan Cervelli
 */
public class MapFrame extends JInternalFrame implements Runnable
{
	private static final long serialVersionUID = 1L;
	private JToolBar toolbar;
	private JPanel mainPanel;
	private JButton optionsButton;
	private JLabel statusLabel;
	private JButton compXButton;
	private JButton expXButton;
	private JToggleButton linkButton;
	
	private Thread updateThread;
	
	private MapPanel mapPanel;
	
	private Throbber throbber;
	
	private int spanIndex = 3;
	private double endTime = Double.NaN;
	private double startTime = Double.NaN;
	
	private int refreshInterval = 250;
	
	private HelicorderViewPanelListener linkListener;
	
	private boolean heliLinked = true;
	
	public MapFrame()
	{
		super("Map", true, true, true, true);
		Swarm.getApplication().touchUITime();

		createUI();
		
		updateThread = new Thread(this);
		updateThread.start();
	}
	
	private void createUI()
	{
		setFrameIcon(Images.getIcon("earth"));
		setSize(Swarm.config.mapWidth, Swarm.config.mapHeight);
		setLocation(Swarm.config.mapX, Swarm.config.mapY);
		setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		if (Swarm.config.mapMaximized)
		{
			try { setMaximum(true); } catch (Exception e) {}
		}
		mainPanel = new JPanel(new BorderLayout());

		toolbar = SwarmUtil.createToolBar();
		optionsButton = SwarmUtil.createToolBarButton(
				Images.getIcon("settings"),
				"Monitor options",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						MultiMonitorSettingsDialog mmsd = MultiMonitorSettingsDialog.getInstance(MultiMonitor.this);
//						mmsd.setVisible(true);
					}
				});
		toolbar.add(optionsButton);
		
		toolbar.addSeparator();
		
		linkButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("helilink"),
				"Synchronize times with helicorder wave",
				new ActionListener()
				{
						public void actionPerformed(ActionEvent e)
						{
							heliLinked = linkButton.isSelected();
						}
				});
		linkButton.setSelected(heliLinked);
		toolbar.add(linkButton);
		
		toolbar.addSeparator();
		
		JButton zoomIn = SwarmUtil.createToolBarButton(
				Images.getIcon("zoomplus"),
				"Zoom in (+)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.zoom(0.5);
					}
				});
		Util.mapKeyStrokeToButton(this, "EQUALS", "zoomin1", zoomIn);
		Util.mapKeyStrokeToButton(this, "shift EQUALS", "zoomin2", zoomIn);
		toolbar.add(zoomIn);
		
		JButton zoomOut = SwarmUtil.createToolBarButton(
				Images.getIcon("zoomminus"),
				"Zoom out (-)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.zoom(2);
					}
				});
		Util.mapKeyStrokeToButton(this, "EQUALS", "zoomout1", zoomIn);
		Util.mapKeyStrokeToButton(this, "shift EQUALS", "zoomout2", zoomIn);
		toolbar.add(zoomOut);
		
		JButton backButton = SwarmUtil.createToolBarButton(
				Images.getIcon("back"), 
				"Last map view (Backspace)", 
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.back();
					}
				});
		Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", backButton);
		toolbar.add(backButton);
		
		toolbar.addSeparator();
		
		compXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xminus"),
				"Shrink time axis",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex != 0)
							spanIndex--;
					}
				});
		toolbar.add(compXButton);
		
		expXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xplus"),
				"Expand time axis",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex < MultiMonitor.SPANS.length - 1)
							spanIndex++;
					}
				});
		toolbar.add(expXButton);
		
		toolbar.add(Box.createHorizontalGlue());
		throbber = new Throbber();
		toolbar.add(throbber);
		
		mainPanel.add(toolbar, BorderLayout.NORTH);
		
		mapPanel = new MapPanel(this);
		Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 3), 
				LineBorder.createGrayLineBorder());
		mapPanel.setBorder(border);
		mainPanel.add(mapPanel, BorderLayout.CENTER);
		statusLabel = new JLabel(" ");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
		mainPanel.add(statusLabel, BorderLayout.SOUTH);
		setContentPane(mainPanel);
		
		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameClosing(InternalFrameEvent e)
					{
						setVisible(false);
					}
				});
		
		linkListener = new HelicorderViewPanelListener() 
		{
			public void insetCreated(double st, double et)
			{
				if (heliLinked)
				{
					endTime = et;
					startTime = st;
//					repositionWaves(st, et);
				}
			}
		};
		
		setVisible(true);
	}
	
	public Throbber getThrobber()
	{
		return throbber;
	}
	
	public void setView(GeoRange gr)
	{
		double lr1 = gr.getLonRange();
		double lr2 = GeoRange.getLonRange(gr.getEast(), gr.getWest());
		if (lr2 < lr1)
			gr.flipEastWest();
			
		mapPanel.setCenterAndScale(gr);
	}
	
	public void setStatusText(final String t)
	{
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						String missing = "";
						if (mapPanel.getMissing() > 0)
							missing = "(" + mapPanel.getMissing() + " channels hidden) ";
						statusLabel.setText(missing + t);
					}
				});
	}
	
	public void reset()
	{
		mapPanel.resetImage();
	}
	
	public HelicorderViewPanelListener getLinkListener()
	{
		return linkListener;
	}
	
	public void run()
	{
		while (true)
		{
			try
			{
				if (this.isVisible())
				{
					double end = endTime;
					double start = startTime;
					if (Double.isNaN(end))
						end = CurrentTime.getInstance().nowJ2K();
					if (Double.isNaN(start))
						start = end - MultiMonitor.SPANS[spanIndex];
					mapPanel.refresh(start, end);
				}
				
				Thread.sleep(refreshInterval);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
