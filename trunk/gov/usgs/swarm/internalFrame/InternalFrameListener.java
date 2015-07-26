package gov.usgs.swarm.internalFrame;

import java.util.EventListener;

import javax.swing.JInternalFrame;

/**
 * 
 * @author Tom Parker
 */
public interface InternalFrameListener extends EventListener
{
    public void internalFrameAdded(final JInternalFrame f);
    public void internalFrameRemoved(final JInternalFrame f);
}
