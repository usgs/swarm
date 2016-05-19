package gov.usgs.volcanoes.swarm.map;

import gov.usgs.plot.map.GeoLabel;
import gov.usgs.plot.render.DataPointRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: ClickableGeoLabel.java,v 1.1 2007-05-21 02:51:56 dcervelli Exp $
 */
abstract public class ClickableGeoLabel extends GeoLabel
{
	abstract public boolean mouseClicked(MouseEvent e);
	abstract public Rectangle getClickBox();
	
	public ClickableGeoLabel()
	{
		super();
	}
	
	public void draw(Graphics2D g2)
	{
		((DataPointRenderer)marker).renderAtOrigin(g2);
		g2.setColor(Color.WHITE);
		g2.drawString(text, 8, -8);
	}
}
