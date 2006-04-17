package gov.usgs.swarm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * A class that shows an about dialog with some extra controls.
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2004/10/12 23:43:15  cvs
 * Added log info and some comments.
 *
 * @author Dan Cervelli
 */
public class AboutDialog extends JDialog
{
	private static final long serialVersionUID = -1;
	private static final int WIDTH = 240;
	private static final int HEIGHT = 271;
	private JPanel mainPanel;
	private JLabel freeMemory;
	private JLabel totalMemory;
	private JLabel usedMemory;
	private JLabel maxMemory;
	private JLabel cacheMemory;
	private JButton gcButton;
	private ImageIcon background;
	private JButton okButton;
	
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
				
		background = Images.getIcon("honeycomb");
		JPanel bp = new JPanel(new BorderLayout());
		bp.add(new JLabel(background), BorderLayout.CENTER);
		bp.setSize(237, 245);
		bp.setLocation(0,0);
		this.getLayeredPane().add(bp);
		this.getLayeredPane().setLayer(bp, JLayeredPane.DEFAULT_LAYER.intValue());
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(LineBorder.createBlackLineBorder());
		JPanel panel = new JPanel(new GridLayout(5, 2));
		panel.setBorder(new EmptyBorder(3, 6, 3, 6));
		panel.add(new JLabel("Free memory: "));
		freeMemory = new JLabel();
		panel.add(freeMemory);
		panel.add(new JLabel("Total memory: "));
		totalMemory = new JLabel();
		panel.add(totalMemory);
		panel.add(new JLabel("Used memory: "));
		usedMemory = new JLabel();
		panel.add(usedMemory);
		panel.add(new JLabel("Max memory: "));
		maxMemory = new JLabel();
		panel.add(maxMemory);
		panel.add(new JLabel("Cache size: "));
		cacheMemory = new JLabel();
		panel.add(cacheMemory);
		
		mainPanel.add(panel, BorderLayout.CENTER);
		mainPanel.setSize(224, 100);
		mainPanel.setLocation(5, 94);
		panel.setBackground(new Color(255, 255, 0, 210));
		mainPanel.setBackground(new Color(0, 0, 0, 0));
		this.getLayeredPane().add(mainPanel);
		this.getLayeredPane().setLayer(mainPanel, JLayeredPane.PALETTE_LAYER.intValue());
		
		JPanel title = new JPanel(new GridLayout(4, 1));
		title.setBackground(new Color(255, 255, 0, 210));
		title.setSize(224, 80);
		title.setLocation(5, 5);
		title.setBorder(LineBorder.createBlackLineBorder());
		JLabel titleLabel = new JLabel("SWARM:", JLabel.CENTER);
		titleLabel.setForeground(Color.red);
		title.add(titleLabel);
		JLabel l2 = new JLabel("Seismic Wave Analysis/", JLabel.CENTER);
		l2.setForeground(Color.blue);
		title.add(l2);
		JLabel l3 = new JLabel("Real-time Monitoring", JLabel.CENTER);
		l3.setForeground(Color.blue);
		title.add(l3);
		title.add(new JLabel("Version: " + Swarm.getVersion(), JLabel.CENTER));
		this.getLayeredPane().add(title);
		this.getLayeredPane().setLayer(title, JLayeredPane.PALETTE_LAYER.intValue());
		
		okButton = new JButton("OK");
		okButton.setSize(60, 30);
		okButton.setLocation(170, 205);
		okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						dispose();
					}
				});
		
		gcButton = new JButton("Run GC");
		gcButton.setSize(90, 30);
		gcButton.setLocation(70, 205);
		gcButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						System.gc();
						update();
						repaint();
					}
				});
		
		this.getLayeredPane().add(okButton);
		this.getLayeredPane().add(gcButton);		
		
		//okButton.setBackground(new Color(255, 255, 255, 128));
		this.getLayeredPane().setLayer(okButton, JLayeredPane.PALETTE_LAYER.intValue());
		this.getLayeredPane().setLayer(gcButton, JLayeredPane.PALETTE_LAYER.intValue());
	}
	
	/** 
	 * Formats a long as a number of bytes.
	 * @param bytes the number of bytes
	 * @return a formatted string
	 */
	public String toByteString(long bytes)
	{
		NumberFormat nf = new DecimalFormat("#.##");
		return nf.format(bytes / 1000000.0) + " MB";
		//return Long.toString(bytes);
	}
	
	/**
	 * Updates the memory information displayed in the dialog.
	 */
	public void update()
	{
		Runtime r = Runtime.getRuntime();
		freeMemory.setText(toByteString(r.freeMemory()));
		totalMemory.setText(toByteString(r.totalMemory()));
		usedMemory.setText(toByteString(r.totalMemory() - r.freeMemory()));
		maxMemory.setText(toByteString(r.maxMemory()));
		cacheMemory.setText(toByteString(Swarm.getCache().getSize()));
	}
}