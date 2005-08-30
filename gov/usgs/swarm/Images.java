package gov.usgs.swarm;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to return image strings
 *
 * $Log: not supported by cvs2svn $
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
		images.put("back", "images/back.png");
		images.put("clipboard", "images/clipboard.png");
		images.put("down", "images/down.png");
		images.put("clock", "images/clock.png");
		images.put("close", "images/close.png");
		images.put("delete", "images/delete.png");
		images.put("gototime", "images/gototime.png");
		images.put("left", "images/left.png");
		images.put("maximize","images/maximize.png"); 
		images.put("minimize", "images/minimize.png"); 
		images.put("open", "images/open.png");
		images.put("right", "images/right.png");
		images.put("save", "images/save.png");
		images.put("saveall", "images/saveall.png");
		images.put("settings", "images/settings.png");
		images.put("spectra", "images/spectra.png");
		images.put("spectrogram", "images/spectrogram.png");
		images.put("up", "images/up.png");
		images.put("wave", "images/wave.png");
		images.put("wavesettings", "images/wavesettings.png"); 
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
