package gov.usgs.volcanoes.swarm.internalframe;

import java.util.EventListener;
import javax.swing.JInternalFrame;

/**
 * InternalFrameListner.
 * 
 * @author Tom Parker
 */
public interface InternalFrameListener extends EventListener {
  public void internalFrameAdded(final JInternalFrame f);

  public void internalFrameRemoved(final JInternalFrame f);
}
