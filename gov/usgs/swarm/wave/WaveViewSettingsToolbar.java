package gov.usgs.swarm.wave;

import gov.usgs.swarm.Images;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.wave.WaveViewSettings.ViewType;
import gov.usgs.util.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
	
	private WaveViewSettings settings;
	
	public WaveViewSettingsToolbar(WaveViewSettings s, JToolBar dest, JComponent keyComp)
	{
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
						if (settings != null)
						{
						    WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(settings);
							wvsd.setVisible(true);
						}
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
						if (settings != null)
							settings.setType(ViewType.WAVE);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", waveToggle);
		Util.mapKeyStrokeToButton(keyComp, "W", "wave2", waveToggle);
		waveToggle.setSelected(true);
		dest.add(waveToggle);
		
		spectraToggle = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("spectra"),
				"Spectra view (S or .)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (settings != null)
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
						if (settings != null)
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
						if (settings != null)
							settings.cycleLogSettings();
					}	
				});
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F"), "toggleFilter");
		keyComp.getActionMap().put("toggleFilter", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						if (settings != null)
							settings.toggleFilter();
					}	
				});				
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("R"), "resetAutoScale");
		keyComp.getActionMap().put("resetAutoScale", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						if (settings != null)
							settings.resetAutoScaleMemory();
					}	
				});
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("OPEN_BRACKET"), "yScaleIn");
		keyComp.getActionMap().put("yScaleIn", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						if (settings != null)
							settings.adjustScale(1.0 / 1.25);
						System.out.println("scale in");
					}	
				});
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("CLOSE_BRACKET"), "yScaleOut");
		keyComp.getActionMap().put("yScaleOut", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						if (settings != null)
							settings.adjustScale(1.25);
						System.out.println("scale out");
					}	
				});
				
		ButtonGroup waveTypes = new ButtonGroup();
		waveTypes.add(waveToggle);
		waveTypes.add(spectraToggle);
		waveTypes.add(spectrogramToggle);
	}
	
	public void setSettings(WaveViewSettings s)
	{
		settings = s;
		if (settings != null)
		{
			settings.toolbar = this;
			settingsChanged();
		}
	}
	
	public void settingsChanged()
	{
		waveToggle.setSelected(settings.viewType == ViewType.WAVE);	
		spectraToggle.setSelected(settings.viewType == ViewType.SPECTRA);
		spectrogramToggle.setSelected(settings.viewType == ViewType.SPECTROGRAM);
	}
}