package gov.usgs.swarm;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * Class to return image strings
 *
 * TODO: make images static final, don't use map 
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.19  2006/08/14 22:42:52  dcervelli
 * Added two more bullets.
 *
 * Revision 1.18  2006/08/11 20:57:36  dcervelli
 * Added label icons.
 *
 * Revision 1.17  2006/08/09 21:53:53  cervelli
 * Added resize and alarm icons.
 *
 * Revision 1.16  2006/08/06 20:02:50  cervelli
 * Added drag button.
 *
 * Revision 1.15  2006/08/04 18:30:37  cervelli
 * Replaced spectra icon.
 *
 * Revision 1.14  2006/07/30 22:41:55  cervelli
 * Added deleteall and gototime icons.
 *
 * Revision 1.13  2006/07/30 16:15:30  cervelli
 * New icons.
 *
 * Revision 1.12  2006/07/23 03:42:39  cervelli
 * Added Earth image.
 *
 * Revision 1.11  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.10  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.9  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
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
	private Map<String, ImageIcon> icons;
	private Map<String, String> imageMap;

	private Images()
	{
		icons = new HashMap<String, ImageIcon>();
		imageMap = new HashMap<String, String>();
		imageMap.put("swarm", "images/swarm.gif");
		imageMap.put("camera", "images/camera.gif");
		imageMap.put("back", "images/back.gif");
		imageMap.put("geoback", "images/geoback.gif");
		imageMap.put("timeback", "images/timeback.gif");
		imageMap.put("clipboard", "images/clipboard.gif");
		imageMap.put("down", "images/down.gif");
		imageMap.put("clock", "images/date.gif");
		imageMap.put("close", "images/close.png");
		imageMap.put("crosshair", "images/crosshair.gif");
		imageMap.put("close_view", "images/closeview.gif");
		imageMap.put("delete", "images/delete.gif");
		imageMap.put("deleteall", "images/deleteall.gif");
		imageMap.put("new_delete", "images/delete.gif");
		imageMap.put("gototime", "images/gototime.gif");
		imageMap.put("heli", "images/helicorder.gif"); 
		imageMap.put("left", "images/left.gif");
		imageMap.put("maximize","images/maximize.png"); 
		imageMap.put("minimize", "images/minimize.png");
		imageMap.put("monitor", "images/monitor.gif");
		imageMap.put("open", "images/wave_folder.gif");
		imageMap.put("right", "images/right.gif");
		imageMap.put("save", "images/save.gif");
		imageMap.put("saveall", "images/saveall.gif");
		imageMap.put("settings", "images/settings.gif");
		imageMap.put("spectra", "images/spectra.gif");
		imageMap.put("spectrogram", "images/spectrogram.png");
		imageMap.put("up", "images/up.gif");
		imageMap.put("wave", "images/wave.gif");
		imageMap.put("waveclip", "images/waveclip.gif");
		imageMap.put("wavesettings", "images/wavesettings.gif"); 
		imageMap.put("wavezoom", "images/wavezoom.gif"); 
		imageMap.put("xminus", "images/xminus.gif");
		imageMap.put("xplus", "images/xplus.gif");
		imageMap.put("yminus", "images/yminus.gif");
		imageMap.put("yplus", "images/yplus.gif");
		imageMap.put("zoomminus", "images/zoomminus.gif");
		imageMap.put("zoomplus", "images/zoomplus.gif");
		imageMap.put("server", "images/server.gif");
		imageMap.put("new_server", "images/new_server.gif");
		imageMap.put("collapse", "images/collapse.gif");
		imageMap.put("wave_folder", "images/wave_folder.gif");
		imageMap.put("warning", "images/warning.gif");
		imageMap.put("broken_server", "images/broken_server.gif");
		imageMap.put("edit_server", "images/edit_server.gif");
		imageMap.put("bullet", "images/bullet.gif");
		imageMap.put("honeycomb", "images/honeycomb.jpg");
		imageMap.put("colorbar", "images/colorbar.png");
		imageMap.put("redbullet", "images/redbullet.gif");
		imageMap.put("helilink", "images/helicorderlink.gif");
		imageMap.put("geosort", "images/geosort.gif");
		imageMap.put("earth" , "images/earth.gif");
		imageMap.put("locked_server", "images/locked_server.gif");
		imageMap.put("broken_locked_server", "images/broken_locked_server.gif");
		imageMap.put("pin", "images/pin.gif");
		imageMap.put("ruler", "images/ruler.gif");
		imageMap.put("dragbox", "images/dragbox.gif");
		imageMap.put("drag", "images/drag.gif");
		imageMap.put("alarm", "images/alarm.gif");
		imageMap.put("resize", "images/resize.gif");
		imageMap.put("label_some", "images/label_some.gif");
		imageMap.put("label_all", "images/label_all.gif");
		imageMap.put("label_none", "images/label_none.gif");
		imageMap.put("bluebullet", "images/bluebullet.gif");
		imageMap.put("graybullet", "images/graybullet.gif");
		imageMap.put("pause", "images/pause.gif");
		
		imageMap.put("throbber_off", "images/throbber_off.gif");
		imageMap.put("throbber_0", "images/throbber_0.gif");
		imageMap.put("throbber_1", "images/throbber_1.gif");
		imageMap.put("throbber_2", "images/throbber_2.gif");
		imageMap.put("throbber_3", "images/throbber_3.gif");
		imageMap.put("throbber_4", "images/throbber_4.gif");
		imageMap.put("throbber_5", "images/throbber_5.gif");
		imageMap.put("throbber_6", "images/throbber_6.gif");
		imageMap.put("throbber_7", "images/throbber_7.gif");
	}
	
	public static ImageIcon getIcon(String key)
	{
		if (images == null)
			images = new Images();
		
		ImageIcon icon = images.icons.get(key);
		if (icon == null)
		{
			icon = new ImageIcon(images.getClass().getClassLoader().getResource(images.imageMap.get(key)));
			images.icons.put(key, icon);
		}
		
		return icon;
			
	}
	
	public static String get(String key)
	{
		if (images == null)
			images = new Images();
		return images.imageMap.get(key);
	}
}
