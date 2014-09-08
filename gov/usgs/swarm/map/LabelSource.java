package gov.usgs.swarm.map;

import java.util.List;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: LabelSource.java,v 1.1 2007-05-21 02:51:56 dcervelli Exp $
 */
public interface LabelSource
{
	public List<? extends ClickableGeoLabel> getLabels();
}
