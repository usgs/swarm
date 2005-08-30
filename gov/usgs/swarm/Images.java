package gov.usgs.swarm;

import java.util.HashMap;

/**
 * Class to return image strings
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/30 00:34:01  tparker
 * Initial commit of Images class
 *
 *
 * @author Tom Parker
 */
public class Images {
		
	static HashMap<String, String> images = new HashMap<String, String>() 
		{{
			put("back", "images/back.png");
			put("clipboard", "images/clipboard.png");
			put("down", "images/down.png");
			put("clock", "images/clock.png");
			put("close", "images/close.png");
			put("delete", "images/delete.png");
			put("gototime", "images/gototime.png");
			put("left", "images/left.png");
			put("maximize","images/maximize.png"); 
			put("minimize", "images/minimize.png"); 
			put("open", "images/open.png");
			put("right", "images/right.png");
			put("save", "images/save.png");
			put("saveall", "images/saveall.png");
			put("settings", "images/settings.png");
			put("spectra", "images/spectra.png");
			put("spectrogram", "images/spectrogram.png");
			put("up", "images/up.png");
			put("wave", "images/wave.png");
			put("wavesettings", "images/wavesettings.png"); 
			put("xminus", "images/xminus.png");
			put("xplus", "images/xplus.png");
			put("yminus", "images/yminus.png");
			put("yplus", "images/yplus.png");
			put("zoomminus", "images/zoomminus.png");
			put("zoomplus", "images/zoomplus.png");
		}};
	
	public static String get(String key)
	{
		return images.get(key);
	}
}
