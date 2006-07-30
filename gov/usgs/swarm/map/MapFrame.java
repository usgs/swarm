package gov.usgs.swarm.map;

import gov.usgs.proj.GeoRange;
import gov.usgs.swarm.HelicorderViewPanelListener;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.MultiMonitor;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.map.MapPanel.DragMode;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.util.png.PngEncoder;
import gov.usgs.util.png.PngEncoderB;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2006/07/28 14:51:55  cervelli
 * Changes for moved GeoRange.
 *
 * Revision 1.4  2006/07/26 22:41:00  cervelli
 * Bunch more development for 2.0.
 *
 * Revision 1.3  2006/07/26 00:39:09  cervelli
 * Changed refresh interval.
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
	private JToggleButton linkButton;
	private JButton compXButton;
	private JButton expXButton;
	private JButton forwardTimeButton;
	private JButton backTimeButton;
	private JButton gotoButton;
	private JButton timeHistoryButton;
	private JButton captureButton;
	
	private JToggleButton dragButton;
	private JToggleButton rulerButton;
	
	private Thread updateThread;
	
	private MapPanel mapPanel;
	
	private Throbber throbber;
	
	private int spanIndex = 3;
	private boolean realtime = true;
//	private double endTime = Double.NaN;
//	private double startTime = Double.NaN;
	
	private int refreshInterval = 1000;
	
	private HelicorderViewPanelListener linkListener;
	
	private boolean heliLinked = true;
	
	public MapFrame()
	{
		super("Map", true, true, true, true);
		this.setFocusable(true);
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

		createToolbar();
		
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
					if (!realtime)
						mapPanel.timePush();
					
					realtime = false;
					mapPanel.setTimes(st, et);
				}
			}
		};
		
//		mainPanel.addKeyListener(mapPanel.getKeyListener());		
		setVisible(true);
	}
	
	private void createToolbar()
	{
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
				"Synchronize times with helicorder wave (H)",
				new ActionListener()
				{
						public void actionPerformed(ActionEvent e)
						{
							heliLinked = linkButton.isSelected();
							if (heliLinked == false)
							{
								realtime = true;
							}
						}
				});
		Util.mapKeyStrokeToButton(this, "H", "helilink", linkButton);
		linkButton.setSelected(heliLinked);
		toolbar.add(linkButton);
		
		toolbar.addSeparator();
		
		dragButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("dragbox"),
				"Zoom into box (B)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.setDragMode(DragMode.BOX);
					}
				});		
		Util.mapKeyStrokeToButton(this, "B", "box", dragButton);
		dragButton.setSelected(true);
		toolbar.add(dragButton);
		
		rulerButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("ruler"),
				"Measure distances (R)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.setDragMode(DragMode.RULER);
					}
				});		
		Util.mapKeyStrokeToButton(this, "M", "measure", rulerButton);
		toolbar.add(rulerButton);
		toolbar.addSeparator();
		
		ButtonGroup group = new ButtonGroup();
		group.add(dragButton);
		group.add(rulerButton);
		
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
		Util.mapKeyStrokeToButton(this, "MINUS", "zoomout1", zoomOut);
		toolbar.add(zoomOut);
		
		JButton backButton = SwarmUtil.createToolBarButton(
				Images.getIcon("geoback"), 
				"Last map view (Ctrl-backspace)", 
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.mapPop();
					}
				});
		Util.mapKeyStrokeToButton(this, "ctrl BACK_SPACE", "mapback1", backButton);
		toolbar.add(backButton);
		
		toolbar.addSeparator();
		
		backTimeButton = SwarmUtil.createToolBarButton(
				Images.getIcon("left"),
				"Scroll back time 20% (Left arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						realtime = false;
						mapPanel.shiftTime(-0.20);
//						if (selected != null)
//							shiftTime(selected, -0.20);						
					}
				});
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backTimeButton);
		toolbar.add(backTimeButton);
		
		forwardTimeButton = SwarmUtil.createToolBarButton(
				Images.getIcon("right"),
				"Scroll forward time 20% (Right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						realtime = false;
						mapPanel.shiftTime(0.20);
//						if (selected != null)
//							shiftTime(selected, 0.20);
					}
				});
		toolbar.add(forwardTimeButton);
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardTimeButton);
		
		gotoButton = SwarmUtil.createToolBarButton(
				Images.getIcon("gototime"),
				"Go to time (Ctrl-G)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						String t = JOptionPane.showInputDialog(Swarm.getApplication(), "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time", JOptionPane.PLAIN_MESSAGE);
//						if (selected != null && t != null)
//							gotoTime(selected, t);
					}
				});
		toolbar.add(gotoButton);
		Util.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);
		
		compXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xminus"),
				"Shrink time axis (Alt-left arrow",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (realtime)
						{
							if (spanIndex != 0)
								spanIndex--;
						}
						else
						{
							mapPanel.scaleTime(0.20);
						}
					}
				});
		toolbar.add(compXButton);
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		
		expXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xplus"),
				"Expand time axis (Alt-right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (realtime)
						{
							if (spanIndex < MultiMonitor.SPANS.length - 1)
								spanIndex++;
						}
						else
						{
							mapPanel.scaleTime(-0.20);
						}
					}
				});
		toolbar.add(expXButton);
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
		
		timeHistoryButton = SwarmUtil.createToolBarButton(
				Images.getIcon("timeback"),
				"Last time settings (Backspace)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						realtime = false;
						mapPanel.timePop();
					}
				});		
		Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", timeHistoryButton);
		toolbar.add(timeHistoryButton);
		toolbar.addSeparator();
		
		captureButton = SwarmUtil.createToolBarButton(
				Images.getIcon("camera"),
				"Save map image (P)",
				new CaptureActionListener());
		Util.mapKeyStrokeToButton(this, "P", "capture", captureButton);
		toolbar.add(captureButton);
		
		toolbar.addSeparator();
		
		toolbar.add(Box.createHorizontalGlue());
		throbber = new Throbber();
		toolbar.add(throbber);
	}
	
	class CaptureActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			chooser.setSelectedFile(new File("map.png"));
			int result = chooser.showSaveDialog(Swarm.getApplication());
			File f = null;
			if (result == JFileChooser.APPROVE_OPTION) 
			{						 
				f = chooser.getSelectedFile();

				if (f.exists()) 
				{
					int choice = JOptionPane.showConfirmDialog(Swarm.getApplication(), "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
					if (choice != JOptionPane.YES_OPTION) 
						return;
			    }
				Swarm.config.lastPath = f.getParent();
			}
			if (f == null)
				return;
			
			Insets i = mapPanel.getInsets();
			BufferedImage image = new BufferedImage(mapPanel.getWidth() - i.left - i.right, mapPanel.getHeight() - i.top - i.bottom, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = image.getGraphics();
			g.translate(-i.left, -i.top);
			mapPanel.paint(g);
			try
	        {
	            PngEncoderB png = new PngEncoderB(image, false, PngEncoder.FILTER_NONE, 7);
	            FileOutputStream out = new FileOutputStream(f);
	            byte[] bytes = png.pngEncode();
	            out.write(bytes);
	            out.close();
	        }
	        catch (Exception ex)
	        {
	            ex.printStackTrace();
	        }
		}
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
				if (this.isVisible() && realtime)
				{
					double end = CurrentTime.getInstance().nowJ2K();
					double start = end - MultiMonitor.SPANS[spanIndex];
					mapPanel.setTimes(start, end);
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