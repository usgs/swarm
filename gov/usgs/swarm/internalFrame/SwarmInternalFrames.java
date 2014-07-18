package gov.usgs.swarm.internalFrame;

import gov.usgs.swarm.heli.HelicorderViewerFrame;
import gov.usgs.swarm.wave.MultiMonitor;
import gov.usgs.swarm.wave.WaveViewerFrame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * A singleton ArrayList of internal frames which keeps a list of listeners.
 * 
 * @author Tom Parker
 *
 */
public final class SwarmInternalFrames {

    private static final EventListenerList internalFrameListeners = new EventListenerList();
    private static final ArrayList<JInternalFrame> internalFrames = new ArrayList<JInternalFrame>();

    private SwarmInternalFrames () {
    }

    public static void removeAllFrames() {
        Runnable r = new Runnable() {
            public void run() {
                Iterator<JInternalFrame> it = internalFrames.iterator();
                while (it.hasNext()) {
                    JInternalFrame frame = it.next();
                    if (frame instanceof HelicorderViewerFrame || frame instanceof WaveViewerFrame
                            || frame instanceof MultiMonitor) {
                        try {
                            frame.setClosed(true);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (Exception e) {
            }
        }
    }
    
    public static void remove(final JInternalFrame f) {
        for (InternalFrameListener listener : internalFrameListeners.getListeners(InternalFrameListener.class))
            listener.internalFrameRemoved(f);
        
        internalFrames.remove(f);
    }
    
    public static void add(final JInternalFrame f) {
        add(f, true);
    }

    public static void add(final JInternalFrame f, boolean setLoc) {
        if (setLoc)
            f.setLocation(internalFrames.size() * 24, internalFrames.size() * 24);
         
        for (InternalFrameListener listener : internalFrameListeners.getListeners(InternalFrameListener.class))
            listener.internalFrameAdded(f);
        
        internalFrames.add(f);
   }

    public static void addInternalFrameListener(InternalFrameListener tl) {
        internalFrameListeners.add(InternalFrameListener.class, tl);
    }

    public static void removeInternalFrameListener(InternalFrameListener tl) {
        internalFrameListeners.remove(InternalFrameListener.class, tl);
    }
    
    public static int frameCount() {
        return internalFrames.size();
    }
    
    public static List<JInternalFrame> getFrames() {
        return internalFrames;
    }
}
