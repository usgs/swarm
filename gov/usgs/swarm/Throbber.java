package gov.usgs.swarm;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Throbber extends JLabel implements Runnable
{
	private static final long serialVersionUID = 1L;
	private static int instances = 0;

	private static ImageIcon offIcon;
	private static ImageIcon[] onIcons;  

	private int onCount = 0;
	private Thread thread;
	private boolean quit;
	private boolean off;
	
	private int cycle = 0;
	
	public Throbber()
	{
		super();
		if (offIcon == null)
		{
			offIcon = Images.getIcon("throbber_off");
			onIcons = new ImageIcon[8];
			for (int i = 0; i < 8; i++)
				onIcons[i] = Images.getIcon("throbber_" + i);
		}
		setIcon(offIcon);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
		thread = new Thread(this, "Throbber-" + instances++);
		thread.start();
	}

	public void increment()
	{
		onCount++;
	}
	
	public void decrement()
	{
		onCount--;
		if (onCount <= 0)
			off = true;
	}
	
	public void close()
	{
		quit = true;
	}
	
	private void setIcon(final ImageIcon icon)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						Throbber.super.setIcon(icon);
						repaint();
					}
				});
	}
	
	public void run()
	{
		while (!quit)
		{
			try
			{
				Thread.sleep(1000 / 8);
				if (onCount > 0)
					setIcon(onIcons[cycle++ % 8]);
				
				if (off)
				{
					setIcon(offIcon);
					off = false;
					cycle = 0;
				}
			}
			catch (Exception e)
			{}
		}
	}
}
