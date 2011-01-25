package gov.usgs.swarm.wave;

import gov.usgs.swarm.Images;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.wave.WaveViewSettings.ViewType;
import gov.usgs.util.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2007/05/21 02:43:20  dcervelli
 * Added an exception check for  1.6 code run on 1.5 JVM.
 *
 * Revision 1.3  2007/04/29 21:25:35  dcervelli
 * Multiselect code support.
 *
 * Revision 1.2  2006/10/26 00:57:43  dcervelli
 * Key mapping for manual zooming.
 *
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.6  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.5  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.4  2005/08/30 00:35:06  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:34:46  tparker
 * Tidy code, no functional changes.
 *
 * Revision 1.2  2005/08/26 23:26:33  tparker
 * Create image path constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class WaveViewSettingsToolbar
{
	private JButton waveSet;
	private JToggleButton waveToggle;
	private JToggleButton spectraToggle;
	private JToggleButton spectrogramToggle;
	private ButtonGroup waveTypes;
	
	private Set<WaveViewSettings> settingsSet;
//	private WaveViewSettings settings;
	
	public WaveViewSettingsToolbar(WaveViewSettings s, JToolBar dest, JComponent keyComp)
	{
		settingsSet = new HashSet<WaveViewSettings>();
		createUI(dest, keyComp);
		setSettings(s);
	}
	
	public void createUI(JToolBar dest, JComponent keyComp)
	{
		waveSet = SwarmUtil.createToolBarButton(
				Images.getIcon("wavesettings"),
				"Wave view settings (?)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						WaveViewSettings s = new WaveViewSettings();
						WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(s, settingsSet.size());
						wvsd.setVisible(true);
						for (WaveViewSettings settings : settingsSet)
						{
							settings.copy(s);
							settings.notifyView();
						}
//						if (settings != null)
//						{
//						    WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(settings);
//							wvsd.setVisible(true);
//						}
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
		dest.add(waveSet);
		
		waveToggle = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("wave"),
				"Wave view (W or ,)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.setType(ViewType.WAVE);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", waveToggle);
		Util.mapKeyStrokeToButton(keyComp, "W", "wave2", waveToggle);
//		waveToggle.setSelected(true);
		dest.add(waveToggle);
		
		spectraToggle = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("spectra"),
				"Spectra view (S or .)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.setType(ViewType.SPECTRA);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", spectraToggle);
		Util.mapKeyStrokeToButton(keyComp, "S", "spectra2", spectraToggle);
		dest.add(spectraToggle);
		
		spectrogramToggle = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("spectrogram"),
				"Spectrogram view (G or /)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)							
							settings.setType(ViewType.SPECTROGRAM);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "SLASH", "spectrogram1", spectrogramToggle);
		Util.mapKeyStrokeToButton(keyComp, "G", "spectrogram2", spectrogramToggle);
		dest.add(spectrogramToggle);
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("L"), "cycleLogSettings");
		keyComp.getActionMap().put("cycleLogSettings", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.cycleLogSettings();
					}	
				});
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F"), "toggleFilter");
		keyComp.getActionMap().put("toggleFilter", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.toggleFilter();
					}	
				});				
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("R"), "resetAutoScale");
		keyComp.getActionMap().put("resetAutoScale", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.resetAutoScaleMemory();
					}	
				});
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("OPEN_BRACKET"), "yScaleIn");
		keyComp.getActionMap().put("yScaleIn", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.adjustScale(1.0 / 1.25);
					}	
				});
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("CLOSE_BRACKET"), "yScaleOut");
		keyComp.getActionMap().put("yScaleOut", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
//						if (settings != null)
						for (WaveViewSettings settings : settingsSet)
							settings.adjustScale(1.25);
					}	
				});
				
		waveTypes = new ButtonGroup();
		waveTypes.add(waveToggle);
		waveTypes.add(spectraToggle);
		waveTypes.add(spectrogramToggle);
	}
	
	public void clearSettingsSet()
	{
		settingsSet.clear();
	}
	
	public void addSettings(WaveViewSettings s)
	{
		if (s != null)
		{
			settingsSet.add(s);
			s.toolbar = this;
			settingsChanged();
		}
//		System.out.println("addSettings " + s + " " + settingsSet.size());
	}
	
	public void removeSettings(WaveViewSettings s)
	{
		settingsSet.remove(s);
		if (s != null)
		{
			s.toolbar = null;
			settingsChanged();
		}
//		System.out.println("removeSettings " + s + " " + settingsSet.size());
	}
	
	public void setSettings(WaveViewSettings s)
	{
		clearSettingsSet();
		addSettings(s);
//		settingsSet.add(s);
//		settings = s;
	}
	
	public void settingsChanged()
	{
		boolean w = false;
		boolean s = false;
		boolean sg = false;
		for (WaveViewSettings set : settingsSet)
		{
			if (set.viewType == ViewType.WAVE)
				w = true;
			if (set.viewType == ViewType.SPECTRA)
				s = true;
			if (set.viewType == ViewType.SPECTROGRAM)
				sg = true;
		}
		// TODO: fix for Java 1.5, clearSelection was added in 1.6
		// try { waveTypes.clearSelection(); } catch (Throwable e) {}
		waveToggle.setSelected(w && !s && !sg);	
		spectraToggle.setSelected(!w && s && !sg);
		spectrogramToggle.setSelected(!w && !s && sg);
//		waveToggle.setSelected(settings.viewType == ViewType.WAVE);	
//		spectraToggle.setSelected(settings.viewType == ViewType.SPECTRA);
//		spectrogramToggle.setSelected(settings.viewType == ViewType.SPECTROGRAM);
	}
}