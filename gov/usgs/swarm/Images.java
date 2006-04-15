package gov.usgs.swarm;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Class to return image strings
 *
 * TODO: replace 'new ImageIcon(getClass().getClassLoader().getResource(Images.get("minimize")))' with 'Images.getImage("minimize")'
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.8  2006/04/02 17:14:24  cervelli
 * Made everything nice and alphabetical
 *
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
	private static Images images;
	private Map<String, String> imageMap;

	private Images()
	{
		imageMap = new HashMap<String, String>();
		imageMap.put("camera", "images/camera.png");
		imageMap.put("back", "images/back.png");
		imageMap.put("clipboard", "images/clipboard.png");
		imageMap.put("down", "images/down.png");
		imageMap.put("clock", "images/clock.png");
		imageMap.put("close", "images/close.png");
		imageMap.put("delete", "images/delete.png");
		imageMap.put("new_delete", "images/new_delete.gif");
		imageMap.put("gototime", "images/gototime.png");
		imageMap.put("heli", "images/heli.png"); 
		imageMap.put("left", "images/left.png");
		imageMap.put("maximize","images/maximize.png"); 
		imageMap.put("minimize", "images/minimize.png");
		imageMap.put("monitor", "images/monitor.png");
		imageMap.put("open", "images/open.png");
		imageMap.put("right", "images/right.png");
		imageMap.put("save", "images/save.png");
		imageMap.put("saveall", "images/saveall.png");
		imageMap.put("settings", "images/settings.png");
		imageMap.put("spectra", "images/spectra.png");
		imageMap.put("spectrogram", "images/spectrogram.png");
		imageMap.put("up", "images/up.png");
		imageMap.put("wave", "images/wave.png");
		imageMap.put("waveclip", "images/waveclip.png");
		imageMap.put("wavesettings", "images/wavesettings.png"); 
		imageMap.put("wavezoom", "images/wavezoom.png"); 
		imageMap.put("xminus", "images/xminus.png");
		imageMap.put("xplus", "images/xplus.png");
		imageMap.put("yminus", "images/yminus.png");
		imageMap.put("yplus", "images/yplus.png");
		imageMap.put("zoomminus", "images/zoomminus.png");
		imageMap.put("zoomplus", "images/zoomplus.png");
		imageMap.put("server", "images/server.gif");
		imageMap.put("new_server", "images/new_server.gif");
		imageMap.put("collapse", "images/collapse.gif");
		imageMap.put("wave_folder", "images/wave_folder.gif");
		imageMap.put("warning", "images/warning.gif");
		imageMap.put("broken_server", "images/broken_server.gif");
		imageMap.put("edit_server", "images/edit_server.gif");
		imageMap.put("bullet", "images/bullet.gif");
	}
	
	public static Icon getIcon(String key)
	{
		if (images == null)
			images = new Images();
		return new ImageIcon(images.getClass().getClassLoader().getResource(images.imageMap.get(key)));
	}
	
	public static String get(String key)
	{
		if (images == null)
			images = new Images();
		return images.imageMap.get(key);
	}
}
