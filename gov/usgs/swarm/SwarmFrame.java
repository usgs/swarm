package gov.usgs.swarm;

import gov.usgs.util.ConfigFile;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JInternalFrame;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/08/09 21:54:22  cervelli
 * Added maximized variable to layout information.
 *
 * Revision 1.1  2006/07/30 22:42:19  cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class SwarmFrame extends JInternalFrame
{
	private static final long serialVersionUID = 1L;
	
	public SwarmFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable)
	{
		super(title, resizable, closable, maximizable, iconifiable);
		setOpaque(false);
		setBackground(new Color(255, 255, 255, 0));
	}
	
	protected void processStandardLayout(ConfigFile cf)
	{
		int x = Integer.parseInt(cf.getString("x"));
		int y = Integer.parseInt(cf.getString("y"));
		setLocation(x, y);
		int w = Integer.parseInt(cf.getString("w"));
		int h = Integer.parseInt(cf.getString("h"));
		setSize(w, h);
		
		boolean m = Boolean.parseBoolean(cf.getString("maximized"));
		try { setMaximum(m); } catch (Exception e) {}
	}
	
	public void saveLayout(ConfigFile cf, String prefix)
	{
		Point pt = getLocation();
		cf.put(prefix + ".x", Integer.toString(pt.x));
		cf.put(prefix + ".y", Integer.toString(pt.y));
		
		Dimension d = getSize();
		cf.put(prefix + ".w", Integer.toString(d.width));
		cf.put(prefix + ".h", Integer.toString(d.height));
		
		cf.put(prefix + ".maximized", Boolean.toString(isMaximum()));
	}
}
