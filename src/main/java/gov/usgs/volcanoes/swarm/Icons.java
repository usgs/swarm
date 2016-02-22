package gov.usgs.volcanoes.swarm;

import javax.swing.ImageIcon;

/**
 * Class to return icons
 * 
 * @author Tom Parker
 */
public class Icons {
    public static final ImageIcon swarm = getIcon("images/swarm.gif");
    public static final ImageIcon camera = getIcon("images/camera.gif");
    public static final ImageIcon back = getIcon("images/back.gif");
    public static final ImageIcon geoback = getIcon("images/geoback.gif");
    public static final ImageIcon timeback = getIcon("images/timeback.gif");
    public static final ImageIcon clipboard = getIcon("images/clipboard.gif");
    public static final ImageIcon picker = getIcon("images/clipboard.gif");
    public static final ImageIcon down = getIcon("images/down.gif");
    public static final ImageIcon clock = getIcon("images/date.gif");
    public static final ImageIcon close = getIcon("images/close.png");
    public static final ImageIcon crosshair = getIcon("images/crosshair.gif");
    public static final ImageIcon close_view = getIcon("images/closeview.gif");
    public static final ImageIcon delete = getIcon("images/delete.gif");
    public static final ImageIcon deleteall = getIcon("images/deleteall.gif");
    public static final ImageIcon new_delete = getIcon("images/delete.gif");
    public static final ImageIcon gototime = getIcon("images/gototime.gif");
    public static final ImageIcon heli = getIcon("images/helicorder.gif");
    public static final ImageIcon left = getIcon("images/left.gif");
    public static final ImageIcon maximize = getIcon("images/maximize.png");
    public static final ImageIcon minimize = getIcon("images/minimize.png");
    public static final ImageIcon monitor = getIcon("images/monitor.gif");
    public static final ImageIcon open = getIcon("images/wave_folder.gif");
    public static final ImageIcon right = getIcon("images/right.gif");
    public static final ImageIcon save = getIcon("images/save.gif");
    public static final ImageIcon saveall = getIcon("images/saveall.gif");
    public static final ImageIcon settings = getIcon("images/settings.gif");
    public static final ImageIcon spectra = getIcon("images/spectra.gif");
    public static final ImageIcon spectrogram = getIcon("images/spectrogram.png");
    public static final ImageIcon up = getIcon("images/up.gif");
    public static final ImageIcon wave = getIcon("images/wave.gif");
    public static final ImageIcon waveclip = getIcon("images/waveclip.gif");
    public static final ImageIcon wavesettings = getIcon("images/wavesettings.gif");
    public static final ImageIcon wavezoom = getIcon("images/wavezoom.gif");
    public static final ImageIcon xminus = getIcon("images/xminus.gif");
    public static final ImageIcon xplus = getIcon("images/xplus.gif");
    public static final ImageIcon yminus = getIcon("images/yminus.gif");
    public static final ImageIcon yplus = getIcon("images/yplus.gif");
    public static final ImageIcon zoomminus = getIcon("images/zoomminus.gif");
    public static final ImageIcon zoomplus = getIcon("images/zoomplus.gif");
    public static final ImageIcon server = getIcon("images/server.gif");
    public static final ImageIcon new_server = getIcon("images/new_server.gif");
    public static final ImageIcon collapse = getIcon("images/collapse.gif");
    public static final ImageIcon wave_folder = getIcon("images/wave_folder.gif");
    public static final ImageIcon warning = getIcon("images/warning.gif");
    public static final ImageIcon broken_server = getIcon("images/broken_server.gif");
    public static final ImageIcon edit_server = getIcon("images/edit_server.gif");
    public static final ImageIcon bullet = getIcon("images/bullet.gif");
    public static final ImageIcon honeycomb = getIcon("images/honeycomb.jpg");
    public static final ImageIcon colorbar = getIcon("images/colorbar.png");
    public static final ImageIcon redbullet = getIcon("images/redbullet.gif");
    public static final ImageIcon helilink = getIcon("images/helicorderlink.gif");
    public static final ImageIcon geosort = getIcon("images/geosort.gif");
    public static final ImageIcon earth = getIcon("images/earth.gif");
    public static final ImageIcon locked_server = getIcon("images/locked_server.gif");
    public static final ImageIcon broken_locked_server = getIcon("images/broken_locked_server.gif");
    public static final ImageIcon pin = getIcon("images/pin.gif");
    public static final ImageIcon ruler = getIcon("images/ruler.gif");
    public static final ImageIcon dragbox = getIcon("images/dragbox.gif");
    public static final ImageIcon drag = getIcon("images/drag.gif");
    public static final ImageIcon alarm = getIcon("images/alarm.gif");
    public static final ImageIcon resize = getIcon("images/resize.gif");
    public static final ImageIcon label_some = getIcon("images/label_some.gif");
    public static final ImageIcon label_all = getIcon("images/label_all.gif");
    public static final ImageIcon label_none = getIcon("images/label_none.gif");
    public static final ImageIcon bluebullet = getIcon("images/bluebullet.gif");
    public static final ImageIcon graybullet = getIcon("images/graybullet.gif");
    public static final ImageIcon pause = getIcon("images/pause.gif");
    public static final ImageIcon throbber_off = getIcon("images/throbber_off.gif");
    public static final ImageIcon throbber_0 = getIcon("images/throbber_0.gif");
    public static final ImageIcon throbber_1 = getIcon("images/throbber_1.gif");
    public static final ImageIcon throbber_2 = getIcon("images/throbber_2.gif");
    public static final ImageIcon throbber_3 = getIcon("images/throbber_3.gif");
    public static final ImageIcon throbber_4 = getIcon("images/throbber_4.gif");
    public static final ImageIcon throbber_5 = getIcon("images/throbber_5.gif");
    public static final ImageIcon throbber_6 = getIcon("images/throbber_6.gif");
    public static final ImageIcon throbber_7 = getIcon("images/throbber_7.gif");
    public static final ImageIcon rsam_values = getIcon("images/rsam_values.png");
    public static final ImageIcon rsam_counts = getIcon("images/rsam_counts.png");
    public static final ImageIcon particle_motion = getIcon("images/bee.png");

    private static ImageIcon getIcon(String key) {
        return new ImageIcon(ClassLoader.getSystemResource(key));
    }
}
