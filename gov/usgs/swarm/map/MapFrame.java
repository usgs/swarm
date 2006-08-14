package gov.usgs.swarm.map;

import gov.usgs.proj.GeoRange;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Kioskable;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmFrame;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.swarm.map.MapPanel.DragMode;
import gov.usgs.swarm.map.MapPanel.LabelSetting;
import gov.usgs.swarm.wave.MultiMonitor;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.swarm.wave.WaveViewSettingsToolbar;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.png.PngEncoder;
import gov.usgs.util.png.PngEncoderB;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
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
 * Revision 1.14  2006/08/12 21:52:29  dcervelli
 * New kiosk code.
 *
 * Revision 1.13  2006/08/11 21:02:17  dcervelli
 * Label buttons, changes for better CPU utilization.
 *
 * Revision 1.12  2006/08/09 05:09:46  cervelli
 * Override setMaximum to eliminate quirk when saving maximized map.
 *
 * Revision 1.11  2006/08/06 20:03:56  cervelli
 * Added drag button.
 *
 * Revision 1.10  2006/08/04 18:40:46  cervelli
 * Realtime and earth buttons.
 *
 * Revision 1.9  2006/08/02 23:34:56  cervelli
 * Layout changes.
 *
 * Revision 1.8  2006/08/01 23:45:09  cervelli
 * More development.
 *
 * Revision 1.7  2006/07/30 22:47:04  cervelli
 * Changes for layouts.
 *
 * Revision 1.6  2006/07/30 16:16:09  cervelli
 * Added ruler.
 *
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
public class MapFrame extends SwarmFrame implements Runnable, Kioskable
{
	private static final long serialVersionUID = 1L;
	private JToolBar toolbar;
	private JPanel mainPanel;
	private JButton optionsButton;
	private JButton labelButton;
	private JLabel statusLabel;
	private JToggleButton linkButton;
	private JToggleButton realtimeButton;
	private JButton compXButton;
	private JButton expXButton;
	private JButton forwardTimeButton;
	private JButton backTimeButton;
	private JButton gotoButton;
	private JButton timeHistoryButton;
	private JButton captureButton;
	private JButton clipboardButton;
	
	private JToggleButton dragButton;
	private JToggleButton dragZoomButton;
	private JToggleButton rulerButton;
	
	private WaveViewSettingsToolbar waveToolbar;
	private WaveViewPanel selected;
	
	private Thread updateThread;
	
	private MapPanel mapPanel;
	
	private Throbber throbber;
	
	private int spanIndex = 3;
	private boolean realtime = true;
	
	private long refreshInterval = 1000;
	
	private HelicorderViewPanelListener linkListener;
	
	private boolean heliLinked = true;

	private Border border;

	public MapFrame()
	{
		super("Map", true, true, true, true);
		this.setFocusable(true);
		Swarm.getApplication().touchUITime();

		createUI();
		
		updateThread = new Thread(this, "Map Update");
		updateThread.start();
	}
	
	public void saveLayout(ConfigFile cf, String prefix)
	{
		super.saveLayout(cf, prefix);
		mapPanel.saveLayout(cf, prefix + ".panel");
	}
	
	public void processLayout(ConfigFile cf)
	{
		processStandardLayout(cf);
		mapPanel.processLayout(cf.getSubConfig("panel"));
		LabelSetting ls = mapPanel.getLabelSetting();
		labelButton.setIcon(ls.getIcon());
	}
	
	private void createUI()
	{
		setFrameIcon(Images.getIcon("earth"));
		setSize(Swarm.config.mapWidth, Swarm.config.mapHeight);
		setLocation(Swarm.config.mapX, Swarm.config.mapY);
		setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

		mainPanel = new JPanel(new BorderLayout());

		createToolbar();
		
		mainPanel.add(toolbar, BorderLayout.NORTH);
		
		mapPanel = new MapPanel(this);
		border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 3), 
				LineBorder.createGrayLineBorder());
		mapPanel.setBorder(border);
		mainPanel.add(mapPanel, BorderLayout.CENTER);
		statusLabel = new JLabel(" ");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
		mainPanel.add(statusLabel, BorderLayout.SOUTH);
		setContentPane(mainPanel);
		
		this.addComponentListener(new ComponentAdapter()
				{
					public void componentShown(ComponentEvent e)
					{
						if (!mapPanel.imageValid())
							mapPanel.resetImage();
					}
				});
		
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
							
							setRealtime(false);
							mapPanel.setTimes(st, et, true);
						}
					}
				};
		
//		mainPanel.addKeyListener(mapPanel.getKeyListener());		
		setVisible(true);
	}

	public void setMaximum(boolean max) throws PropertyVetoException
	{
		if (max)
		{
			Swarm.config.mapX = getX();
			Swarm.config.mapY = getY();
		}
		super.setMaximum(max);
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
						MapSettingsDialog msd = MapSettingsDialog.getInstance(MapFrame.this);
						msd.setVisible(true);
					}
				});
		toolbar.add(optionsButton);
		
		labelButton = SwarmUtil.createToolBarButton(
				Images.getIcon("label_some"),
				"Change label settings",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						LabelSetting ls = mapPanel.getLabelSetting().next();
						labelButton.setIcon(ls.getIcon());
						mapPanel.setLabelSetting(ls);
					}
				});
//		Util.mapKeyStrokeToButton(this, "L", "label", labelButton);
		toolbar.add(labelButton);
		
		toolbar.addSeparator();
		
		realtimeButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("clock"),
				"Realtime mode (N)",
				new ActionListener()
				{
						public void actionPerformed(ActionEvent e)
						{
							setRealtime(realtimeButton.isSelected());
						}
				});
		Util.mapKeyStrokeToButton(this, "N", "realtime", realtimeButton);
		realtimeButton.setSelected(realtime);
		toolbar.add(realtimeButton);
		
		linkButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("helilink"),
				"Synchronize times with helicorder wave (H)",
				new ActionListener()
				{
						public void actionPerformed(ActionEvent e)
						{
							heliLinked = linkButton.isSelected();
						}
				});
		Util.mapKeyStrokeToButton(this, "H", "helilink", linkButton);
		linkButton.setSelected(heliLinked);
		toolbar.add(linkButton);
		
		toolbar.addSeparator();

		JButton earthButton = SwarmUtil.createToolBarButton(
				Images.getIcon("earth"),
				"Zoom out to full scale (Home)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Point2D.Double c = new Point2D.Double(mapPanel.getCenter().x, 0);
						mapPanel.setCenterAndScale(c, 100000);
//						mapPanel.setDragMode(DragMode.BOX);
					}
				});		
		Util.mapKeyStrokeToButton(this, "HOME", "home", earthButton);
		toolbar.add(earthButton);
		
		dragButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("drag"),
				"Drag map (D)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.setDragMode(DragMode.DRAG_MAP);
					}
				});		
		Util.mapKeyStrokeToButton(this, "D", "drag", dragButton);
		dragButton.setSelected(true);
		toolbar.add(dragButton);
		
		dragZoomButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("dragbox"),
				"Zoom into box (B)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.setDragMode(DragMode.BOX);
					}
				});		
		Util.mapKeyStrokeToButton(this, "B", "box", dragZoomButton);
		dragZoomButton.setSelected(false);
		toolbar.add(dragZoomButton);
		
		rulerButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("ruler"),
				"Measure distances (M)",
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
		group.add(dragZoomButton);
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
						setRealtime(false);
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
						setRealtime(false);
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
						String t = JOptionPane.showInputDialog(Swarm.getApplication(), "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time", JOptionPane.PLAIN_MESSAGE);
						if (t != null)
						{
							if (t.length() == 12)
								t = t + "30";
							
							double j2k = Time.parse("yyyyMMddHHmmss", t);
							setRealtime(false);
							mapPanel.gotoTime(j2k);
							
						}
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
						if (mapPanel.timePop())
							setRealtime(false);
					}
				});		
		Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", timeHistoryButton);
		toolbar.add(timeHistoryButton);
		toolbar.addSeparator();
		
		waveToolbar = new WaveViewSettingsToolbar(null, toolbar, this);

		clipboardButton = SwarmUtil.createToolBarButton(
				Images.getIcon("clipboard"),
				"Copy inset to clipboard (C or Ctrl-C)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						mapPanel.wavesToClipboard();
					}
				});
//		clipboardButton.setEnabled(false);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard1", clipboardButton);
		Util.mapKeyStrokeToButton(this, "C", "clipboard2", clipboardButton);
		toolbar.add(clipboardButton);
		
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
	
	public void setRealtime(boolean b)
	{
		realtime = b;
		realtimeButton.setSelected(realtime);
	}
	
	public Throbber getThrobber()
	{
		return throbber;
	}
	
	public void setRefreshInterval(long r)
	{
		refreshInterval = r;
	}
	
	public long getRefreshInterval()
	{
		return refreshInterval;
	}
	
	public MapPanel getMapPanel()
	{
		return mapPanel;
	}
	
	public void setSelectedWave(WaveViewPanel wvp)
	{
		selected = wvp;
		waveToolbar.setSettings(selected.getSettings());
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
						if (mapPanel.getMissing() == 1)
							missing = "(" + mapPanel.getMissing() + " channel hidden) ";
						else if (mapPanel.getMissing() > 1)
							missing = "(" + mapPanel.getMissing() + " channels hidden) ";
						statusLabel.setText(missing + t);
						statusLabel.repaint();
					}
				});
	}
	
	public void reset(boolean doMap)
	{
		mapPanel.resetImage(doMap);
	}
	
	public HelicorderViewPanelListener getLinkListener()
	{
		return linkListener;
	}
	
	public void setKioskMode(boolean b)
	{
		setDefaultKioskMode(b);
		if (fullScreen)
		{
			mainPanel.remove(toolbar);
			mapPanel.setBorder(null);
		}
		else
		{
			mainPanel.add(toolbar, BorderLayout.NORTH);
			mapPanel.setBorder(border);
		}
//		mapPanel.requestFocusInWindow();
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
					mapPanel.setTimes(start, end, false);
				}
				
				Thread.sleep(refreshInterval);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
}