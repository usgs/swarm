package gov.usgs.swarm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import nl.knmi.orfeus.SLClient;
import edu.sc.seis.seisFile.BuildVersion;
import gov.usgs.swarm.data.CachedDataSource;

/**
 * A class that shows an about dialog with some extra controls.
 *
 * TODO: use SwarmDialog
 * 
 * @author Dan Cervelli
 */
public class AboutDialog extends JDialog implements Runnable
{
	private static final long serialVersionUID = -1;
	private final static int TITLE_LOC_Y = 5;
	private final static int TITLE_HEIGHT =  190;
	private final static int MAIN_PAD_HEIGHT = 9;
	private final static int MAIN_LOC_Y = TITLE_LOC_Y + TITLE_HEIGHT + MAIN_PAD_HEIGHT;
	private final static int MAIN_HEIGHT = 100;
	private final static int BUTTON_PAD_HEIGHT = 11;
	private final static int BUTTON_LOC_Y = MAIN_LOC_Y + MAIN_HEIGHT + BUTTON_PAD_HEIGHT;
	private final static int BUTTON_HEIGHT = 30;
	private final static int BG_WIDTH = 237;
	private final static int BG_HEIGHT = BUTTON_LOC_Y + BUTTON_HEIGHT + 10;
	private static final int WIDTH = BG_WIDTH + 3;
	private static final int HEIGHT = BG_HEIGHT + 26;
	private final JPanel mainPanel;
	private final MemoryPanel memoryPanel;
	private final JLabel freeMemory;
	private final JLabel totalMemory;
	private final JLabel usedMemory;
	private final JLabel maxMemory;
	private final JLabel cacheMemory;
	private final JButton gcButton;
	private final ImageIcon background;
	private final JButton okButton;
	
	private Thread updateThread;
	
	private boolean kill = false;
	
	/**
	 * Construct an about dialog.
	 */
	public AboutDialog()
	{
		super(Swarm.getApplication(), "About", true);
		this.setSize(WIDTH, HEIGHT);
		Dimension parentSize = Swarm.getApplication().getSize();
		Point parentLoc = Swarm.getApplication().getLocation();
		this.setLocation(parentLoc.x + (parentSize.width / 2 - WIDTH / 2),
				parentLoc.y + (parentSize.height / 2 - HEIGHT / 2));
				
		this.setResizable(false);
				
		background = Icons.honeycomb;
		if (background.getIconWidth() != BG_WIDTH || background.getIconHeight() != BG_HEIGHT)
		{
			// resize the background image
			Image image = background.getImage();
			image = image.getScaledInstance(BG_WIDTH, BG_HEIGHT, Image.SCALE_FAST);
			background.setImage(image);
		}
		JPanel bp = new JPanel(new BorderLayout());
		bp.add(new JLabel(background), BorderLayout.CENTER);
		bp.setSize(BG_WIDTH, BG_HEIGHT);
		bp.setLocation(0,0);
		this.getLayeredPane().add(bp);
		this.getLayeredPane().setLayer(bp, JLayeredPane.DEFAULT_LAYER.intValue());
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(LineBorder.createBlackLineBorder());
		memoryPanel = new MemoryPanel(new GridLayout(5, 2));
		memoryPanel.setBorder(new EmptyBorder(3, 6, 3, 6));
		memoryPanel.add(new JLabel("Free memory: "));
		freeMemory = new JLabel();
		memoryPanel.add(freeMemory);
		memoryPanel.add(new JLabel("Total memory: "));
		totalMemory = new JLabel();
		memoryPanel.add(totalMemory);
		memoryPanel.add(new JLabel("Used memory: "));
		usedMemory = new JLabel();
		memoryPanel.add(usedMemory);
		memoryPanel.add(new JLabel("Max memory: "));
		maxMemory = new JLabel();
		memoryPanel.add(maxMemory);
		memoryPanel.add(new JLabel("Cache size: "));
		cacheMemory = new JLabel();
		memoryPanel.add(cacheMemory);
		
		mainPanel.add(memoryPanel, BorderLayout.CENTER);
		mainPanel.setSize(224, MAIN_HEIGHT);
		mainPanel.setLocation(5, MAIN_LOC_Y);
		memoryPanel.setBackground(new Color(255, 255, 0));
		mainPanel.setBackground(new Color(0, 0, 0, 0));
		this.getLayeredPane().add(mainPanel);
		this.getLayeredPane().setLayer(mainPanel, JLayeredPane.PALETTE_LAYER.intValue());
		
		JTextPane title = new JTextPane();
		title.setFont(freeMemory.getFont()); // use label font for title
		title.setEditable(false);           //make title not modifiable
		title.setBackground(new Color(255, 255, 0, 210));
		title.setSize(224, TITLE_HEIGHT);
		title.setLocation(5, TITLE_LOC_Y);
		title.setBorder(LineBorder.createBlackLineBorder());
		title.setContentType("text/html");
		title.setText("<HTML><DIV style=\"text-align: center;\">"
				+ "<SPAN style=\"color: red;\">SWARM:</SPAN><BR>"
				+ "<SPAN style=\"color: blue;\">Seismic Wave Analysis/</SPAN><BR>"
				+ "<SPAN style=\"color: blue;\">Real-time Monitoring/</SPAN><BR>"
				+ "Version: " + Swarm.getVersion() + "<BR>"
				+ "<HR>"
				+ "<B>Funded by:</B><BR>"
				+ "USGS  http://www.usgs.gov<BR>"
				+ "IRIS  http://www.iris.edu<BR>"
				+ "ISTI  http://www.isti.com<BR>"
				+ "<HR>"
				+ "<B>Library versions:</B><BR>"
				+ BuildVersion.getName() + " " + BuildVersion.getVersion() + "<BR>"
				+ SLClient.PROGRAM_NAME + "<BR>"
				+ "</DIV></HTML>");
		this.getLayeredPane().add(title);
		this.getLayeredPane().setLayer(title, JLayeredPane.PALETTE_LAYER.intValue());
		
		okButton = new JButton("OK");
		okButton.setSize(60, BUTTON_HEIGHT);
		okButton.setLocation(170, BUTTON_LOC_Y);
		okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						dispose();
						kill = true;
					}
				});
		
		gcButton = new JButton("Run GC");
		gcButton.setSize(90, BUTTON_HEIGHT);
		gcButton.setLocation(70, BUTTON_LOC_Y);
		gcButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						System.gc();
						// update();
						// no need to update since it will be done by thread
					}
				});
		
		this.getLayeredPane().add(okButton);
		this.getLayeredPane().add(gcButton);		
		
		this.getLayeredPane().setLayer(okButton, JLayeredPane.PALETTE_LAYER.intValue());
		this.getLayeredPane().setLayer(gcButton, JLayeredPane.PALETTE_LAYER.intValue());
	}
	
	public void setVisible(boolean v)
	{
		updateThread = new Thread(this, "About");
		updateThread.start();
		super.setVisible(v);
	}
	
	public void run()
	{
		while (!kill)
		{
			try
			{
				update();
				Thread.sleep(500);
			}
			catch (Exception e) {}
		}
		kill = false;
	}

	/**
	 * The memory panel overrides the paint method to update the values.
	 */
	private class MemoryPanel extends JPanel
	{
		/** serial version UID */
		private static final long serialVersionUID = 1L;

		/** re-use the number format safely on the Event Dispatch Thread. */
		private final NumberFormat nf = new DecimalFormat("#.##");
		
		public MemoryPanel(LayoutManager layout)
		{
			super(layout);
		}

		/**
		 * Invoked by Swing to draw components. This method should not be called
		 * directly, use the repaint method.
		 * 
		 * @param g the graphics.
		 */
		public void paint(Graphics g)
		{
			update();
			super.paint(g);
		}
		
		/** 
		 * Formats a long as a number of bytes.
		 * @param bytes the number of bytes
		 * @return a formatted string
		 */
		private String toByteString(long bytes)
		{
			return nf.format(bytes / 1000000.0) + " MB";
		}
		
		/**
		 * Updates the memory information displayed in the dialog.
		 * This should only be called on the Event Dispatch Thread.
		 */
		private void update()
		{
			Runtime r = Runtime.getRuntime();
			freeMemory.setText(toByteString(r.freeMemory()));
			totalMemory.setText(toByteString(r.totalMemory()));
			usedMemory.setText(toByteString(r.totalMemory() - r.freeMemory()));
			maxMemory.setText(toByteString(r.maxMemory()));
			
			CachedDataSource cache = CachedDataSource.getInstance();
			cacheMemory.setText(toByteString(cache.getSize()));
		}
	}

	/**
	 * Updates the memory information displayed in the dialog.
	 */
	public void update()
	{
		//this will cause paint to be called on the Event Dispatch Thread
		memoryPanel.repaint();
	}
}