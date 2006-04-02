package gov.usgs.swarm;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to return image strings
 *
 * TODO: replace 'new ImageIcon(getClass().getClassLoader().getResource(Images.get("minimize")))' with 'Images.getImage("minimize")'
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.7  2006/03/04 23:03:45  cervelli
 * Added alias feature. More thoroughly incorporated calibrations.  Got rid of 'waves' tab and combined all functionality under a 'channels' tab.
 *
 * Revision 1.6  2005/09/22 20:49:57  dcervelli
 * Got rid of stupid warning.
 *
 * Revision 1.5  2005/09/22 20:48:54  dcervelli
 * Added camera icon.
 *
 * Revision 1.4  2005/09/08 18:47:07  tparker
 * Add icons for the autoscale slider
 *
 * Revision 1.3  2005/08/30 17:44:02  dcervelli
 * Doesn't use anonymous class.
 *
 * Revision 1.2  2005/08/30 00:42:21  tparker
 * Correct formatting, no functional changes.
 *
 * Revision 1.1  2005/08/30 00:34:01  tparker
 * Initial commit of Images class
 *
 * @author Tom Parker
 */
public class Images 
{
	private static Map<String, String> images;

	static
	{
		images = new HashMap<String, String>();
		images.put("camera", "images/camera.png");
		images.put("back", "images/back.png");
		images.put("clipboard", "images/clipboard.png");
		images.put("down", "images/down.png");
		images.put("clock", "images/clock.png");
		images.put("close", "images/close.png");
		images.put("delete", "images/delete.png");
		images.put("gototime", "images/gototime.png");
		images.put("heli", "images/heli.png"); 
		images.put("left", "images/left.png");
		images.put("maximize","images/maximize.png"); 
		images.put("minimize", "images/minimize.png");
		images.put("monitor", "images/monitor.png");
		images.put("open", "images/open.png");
		images.put("right", "images/right.png");
		images.put("save", "images/save.png");
		images.put("saveall", "images/saveall.png");
		images.put("settings", "images/settings.png");
		images.put("spectra", "images/spectra.png");
		images.put("spectrogram", "images/spectrogram.png");
		images.put("up", "images/up.png");
		images.put("wave", "images/wave.png");
		images.put("waveclip", "images/waveclip.png");
		images.put("wavesettings", "images/wavesettings.png"); 
		images.put("wavezoom", "images/wavezoom.png"); 
		images.put("xminus", "images/xminus.png");
		images.put("xplus", "images/xplus.png");
		images.put("yminus", "images/yminus.png");
		images.put("yplus", "images/yplus.png");
		images.put("zoomminus", "images/zoomminus.png");
		images.put("zoomplus", "images/zoomplus.png");		
	}
	
	public static String get(String key)
	{
		return images.get(key);
	}
}
