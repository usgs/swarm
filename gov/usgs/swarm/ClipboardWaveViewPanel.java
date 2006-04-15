package gov.usgs.swarm;

import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.LineBorder;

/**
 * This is a panel that holds a <code>WaveViewPanel</code> on the
 * Wave Clipboard.
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2006/04/08 01:00:44  dcervelli
 * Fix in fetchNewWave() for bug #84.
 *
 * Revision 1.4  2005/08/30 00:33:19  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:27:12  tparker
 * Tidy code. No functional changes.
 *
 * Revision 1.2  2005/08/27 00:02:55  tparker
 * Create image constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2004/10/12 23:43:15  cvs
 * Added log info and some comments.
 *
 * @author Dan Cervelli
 */
public class ClipboardWaveViewPanel extends JPanel
{
	private static final long serialVersionUID = -1;

	private WaveViewPanel waveViewPanel;
	private JLayeredPane mainPane;
	private JPanel mainPanel;
	private JToolBar toolbar;
	private JButton showToolbar;
	private JButton upButton;
	private JButton downButton;
	private JButton removeButton;
	private JButton compXButton;
	private JButton expXButton;
	private JButton copyButton;
	private JButton forwardButton;
	private JButton backButton;
	private JButton gotoButton;
	private DateFormat dateFormat;
	
	private boolean selected;
	private Stack<double[]> history;
	
	private WaveClipboardFrame clipboard;
	private static final Color selectColor = new Color(204, 204, 255);
	private static final Color backgroundColor = new Color(0xf7, 0xf7, 0xf7);
	
	public ClipboardWaveViewPanel(WaveViewPanel p)
	{
		super(new BorderLayout());
		dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		history = new Stack<double[]>();
		
		waveViewPanel = new WaveViewPanel(p);
		//waveViewPanel.setDataSource(waveViewPanel.getDataSource().getCopy());
		//waveViewPanel.setStackMode(true);
		waveViewPanel.setDisplayTitle(true);
		waveViewPanel.setClipboardPanel(this);
		mainPane = new JLayeredPane();
		
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		JButton hideTB = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("minimize"))));
		hideTB.setToolTipText("Hide toolbar");
		hideTB.setMargin(new Insets(0,0,0,0));
		hideTB.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						showToolbar.setVisible(true);
						mainPanel.remove(toolbar);
						mainPanel.validate();
						repaint();
						waveViewPanel.requestFocus();
					}
				});
		
		toolbar.add(hideTB);
		toolbar.addSeparator();
		
		showToolbar = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("maximize"))));
		showToolbar.setToolTipText("Show toolbar");
		showToolbar.setMargin(new Insets(0, 0, 0, 0));
		showToolbar.setSize(24, 24);
		showToolbar.setLocation(0, 0);
		showToolbar.setVisible(false);
		showToolbar.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						showToolbar.setVisible(false);
						mainPanel.add(toolbar, BorderLayout.NORTH);	
						mainPanel.doLayout();
						waveViewPanel.requestFocus();
					}
				});
		mainPane.add(showToolbar);
		mainPane.setLayer(showToolbar, JLayeredPane.PALETTE_LAYER.intValue());
		
		backButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("left"))));
		backButton.setMargin(new Insets(0,0,0,0));
		backButton.setToolTipText("Scroll back time 20% (Left arrow)");
		backButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						shiftTime(-0.20);						
					}
				});
		toolbar.add(backButton);
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
		
		forwardButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("right"))));
		forwardButton.setMargin(new Insets(0,0,0,0));
		forwardButton.setToolTipText("Scroll forward time 20% (Right arrow)");
		forwardButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						shiftTime(0.20);
					}
				});
		toolbar.add(forwardButton);
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);
		
		gotoButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("gototime"))));
		gotoButton.setMargin(new Insets(0,0,0,0));
		gotoButton.setToolTipText("Go to time (Ctrl-G)");
		gotoButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						String t = JOptionPane.showInputDialog(Swarm.getApplication(), "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time", JOptionPane.PLAIN_MESSAGE);
						if (t != null)
							gotoTime(t);
					}
				});
		toolbar.add(gotoButton);
		Util.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);
		
		compXButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("xminus"))));
		compXButton.setMargin(new Insets(0,0,0,0));
		compXButton.setToolTipText("Shrink sample time 20% (Alt-left arrow)");
		compXButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						scaleTime(0.20);
					}
				});
		toolbar.add(compXButton);
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		
		expXButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("xplus"))));
		expXButton.setMargin(new Insets(0,0,0,0));
		expXButton.setToolTipText("Expand sample time 20% (Alt-right arrow)");
		expXButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						scaleTime(-0.20);
					}
				});
		toolbar.add(expXButton);
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);

		JButton histButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("back"))));
		histButton.setMargin(new Insets(0,0,0,0));
		histButton.setToolTipText("Last time settings (Backspace)");
		histButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						back();
					}
				});		
		Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", histButton);
		toolbar.add(histButton);
		toolbar.addSeparator();

		new WaveViewSettingsToolbar(waveViewPanel.getWaveViewSettings(), toolbar, this);
		
		copyButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("clipboard"))));
		copyButton.setMargin(new Insets(0,0,0,0));
		copyButton.setToolTipText("Place another copy of wave on clipboard (C or Ctrl-C)");
		copyButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						clipboard.addWave(new ClipboardWaveViewPanel(waveViewPanel));
					}
				});
		Util.mapKeyStrokeToButton(this, "C", "clipboard1", copyButton);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard2", copyButton);
		
		toolbar.add(copyButton);
		toolbar.addSeparator();
		
		upButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("up"))));
		upButton.setMargin(new Insets(0,0,0,0));
		upButton.setToolTipText("Move wave up in clipboard (Up arrow)");
		upButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						clipboard.moveUp(ClipboardWaveViewPanel.this);
					}
				});
		Util.mapKeyStrokeToButton(this, "UP", "up", upButton);
		toolbar.add(upButton);
		
		
		downButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("down"))));
		downButton.setMargin(new Insets(0,0,0,0));
		downButton.setToolTipText("Move wave down in clipboard (Down arrow)");
		downButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						clipboard.moveDown(ClipboardWaveViewPanel.this);
					}
				});
		Util.mapKeyStrokeToButton(this, "DOWN", "down", downButton);
		toolbar.add(downButton);
		
		removeButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("delete"))));
		removeButton.setMargin(new Insets(0,0,0,0));
		removeButton.setToolTipText("Remove wave from clipboard (Delete)");
		removeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						clipboard.remove(ClipboardWaveViewPanel.this);
					}
				});
		Util.mapKeyStrokeToButton(this, "DELETE", "remove", removeButton);
		toolbar.add(removeButton);
		
		toolbar.setRollover(true);
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(toolbar, BorderLayout.NORTH);
		JPanel wp = new JPanel(new BorderLayout());
		wp.setBorder(LineBorder.createGrayLineBorder());
		wp.add(waveViewPanel, BorderLayout.CENTER);
		mainPanel.add(wp, BorderLayout.CENTER);
		this.setBorder(LineBorder.createBlackLineBorder());
		
		mainPane.add(mainPanel);
		mainPane.setLayer(mainPanel, JLayeredPane.DEFAULT_LAYER.intValue());
		this.setRequestFocusEnabled(true);
		this.add(mainPane, BorderLayout.CENTER);
	}

	public void close()
	{
		waveViewPanel.getDataSource().close();
	}
	
	private void disableNavigationButtons()
	{
		compXButton.setEnabled(false);
		expXButton.setEnabled(false);
		forwardButton.setEnabled(false);
		backButton.setEnabled(false);
		gotoButton.setEnabled(false);
		
	}
	
	private void enableNavigationButtons()
	{
		compXButton.setEnabled(true);
		expXButton.setEnabled(true);
		forwardButton.setEnabled(true);
		backButton.setEnabled(true);
		gotoButton.setEnabled(true);
	}

	public void select()
	{
		if (!selected)
			clipboard.select(ClipboardWaveViewPanel.this);
			
		requestFocus();
	}

	public WaveViewPanel getWaveViewPanel()
	{
		return waveViewPanel;	
	}
	
	public void setSelected(boolean b)
	{
		selected = b;
		waveViewPanel.invalidateImage();
		requestFocus();
	}
	
	public void setClipboard(WaveClipboardFrame b)
	{
		clipboard = b;	
	}
	
	public void paint(Graphics g)
	{
		if (selected)
			waveViewPanel.setBackgroundColor(selectColor);
		else
			waveViewPanel.setBackgroundColor(backgroundColor);
		
		super.paint(g);
	}
	 
	public void setSize(int x, int y)
	{
		super.setSize(x, y);
		waveViewPanel.setSize(x, y - 25);
		mainPane.setSize(x, y);
		mainPanel.setSize(x, y);
	}
	
	public Dimension getPreferredSize()
	{
		return getSize();	
	}
	
	public Dimension getMinimumSize()
	{
		return getSize();	
	}
	
	public Dimension getMaximumSize()
	{
		return getSize();	
	}

	public void back()
	{
		if (history.empty())
			return;
			
		final double[] t = history.pop();
		fetchNewWave(t[0], t[1]);
	}

	public void didZoom(double st, double et)
	{
		double[] t = new double[] {st, et};
		history.push(t);
	}

	public void shiftTime(double pct)
	{
		double st = waveViewPanel.getStartTime();	
		double et = waveViewPanel.getEndTime();
		double[] t = new double[] {st, et};
		history.push(t);
		double dt = (et - st) * pct;
		double nst = st + dt;
		double net = et + dt;
		fetchNewWave(nst, net);
	}
	
	public void scaleTime(double pct)
	{
		double st = waveViewPanel.getStartTime();	
		double et = waveViewPanel.getEndTime();
		double[] t = new double[] {st, et};
		history.push(t);
		double dt = (et - st) * (1 - pct);
		double mt = (et - st) / 2 + st;
		double nst = mt - dt / 2;
		double net = mt + dt / 2;
		fetchNewWave(nst, net);
	}
	
	public void gotoTime(String t)
	{
		Date d = null;
		try
		{
			if (t.length() == 12)
				t = t + "30";
				
			d = dateFormat.parse(t);
		}	
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(Swarm.getApplication(), "Illegal time value.", "Error", JOptionPane.ERROR_MESSAGE);
		}	
		
		if (d != null)
		{
			double dt = 60;
			if (waveViewPanel.getWave() != null)
			{
				double st = waveViewPanel.getStartTime();	
				double et = waveViewPanel.getEndTime();
				double[] ts = new double[] {st, et};
				history.push(ts);
				dt = (et - st);	
			}
			
			double tzo = Swarm.config.timeZoneOffset;
			double nst = Util.dateToJ2K(d) - tzo * 3600 - dt / 2;
			double net = nst + dt;

			fetchNewWave(nst, net);
		}	
	}
	
	private void fetchNewWave(final double nst, final double net)
	{
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						disableNavigationButtons();
						Swarm.getApplication().incThreadCount();
						System.out.println(waveViewPanel.getDataSource().getClass());
						SeismicDataSource sds = waveViewPanel.getDataSource();
						// Hacky fix for bug #84
						Wave sw = null;
						if (sds instanceof CachedDataSource)
							sw = ((CachedDataSource)sds).getBestWave(waveViewPanel.getChannel(), nst, net);
						else
							sw = sds.getWave(waveViewPanel.getChannel(), nst, net);
						waveViewPanel.setWave(sw, nst, net);
						return null;
					}
					
					public void finished()
					{
						Swarm.getApplication().decThreadCount();
						repaint();	
						enableNavigationButtons();
					}
				};
		worker.start();	
	}
}