package gov.usgs.swarm.rsam;

import gov.usgs.swarm.Icons;
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
 *
 * @author Dan Cervelli
 */
public class RsamViewSettingsToolbar
{
	private JButton waveSet;
//	private JToggleButton waveToggle;
//	private JToggleButton spectraToggle;
//	private JToggleButton spectrogramToggle;
	private ButtonGroup waveTypes;
	
	private Set<RsamViewSettings> settingsSet;
	
	public RsamViewSettingsToolbar(RsamViewSettings s, JToolBar dest, JComponent keyComp)
	{
		settingsSet = new HashSet<RsamViewSettings>();
		createUI(dest, keyComp);
		setSettings(s);
	}
	
	public void createUI(JToolBar dest, JComponent keyComp)
	{
		waveSet = SwarmUtil.createToolBarButton(
				Icons.wavesettings,
				"Wave view settings (?)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (settingsSet.size()==0)
							return;
						RsamViewSettings s = settingsSet.iterator().next();
						RsamViewSettingsDialog wvsd = RsamViewSettingsDialog.getInstance(s, settingsSet.size());
						wvsd.setVisible(true);
						for (RsamViewSettings settings : settingsSet)
						{
							settings.copy(s);
							settings.notifyView();
						}
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
		dest.add(waveSet);
		
//		waveToggle = SwarmUtil.createToolBarToggleButton(
//				Icons.wave,
//				"Wave view (W or ,)",
//				new ActionListener()
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						for (RsamViewSettings settings : settingsSet)
//							settings.setType(ViewType.WAVE);
//					}
//				});
//		Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", waveToggle);
//		Util.mapKeyStrokeToButton(keyComp, "W", "wave2", waveToggle);
//		dest.add(waveToggle);
		
//		spectraToggle = SwarmUtil.createToolBarToggleButton(
//				Icons.spectra,
//				"Spectra view (S or .)",
//				new ActionListener()
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						for (RsamViewSettings settings : settingsSet)
//							settings.setType(ViewType.SPECTRA);
//					}
//				});
//		Util.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", spectraToggle);
//		Util.mapKeyStrokeToButton(keyComp, "S", "spectra2", spectraToggle);
//		dest.add(spectraToggle);
		
//		spectrogramToggle = SwarmUtil.createToolBarToggleButton(
//				Icons.spectrogram,
//				"Spectrogram view (G or /)",
//				new ActionListener()
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						for (RsamViewSettings settings : settingsSet)							
//							settings.setType(ViewType.SPECTROGRAM);
//					}
//				});
//		Util.mapKeyStrokeToButton(keyComp, "SLASH", "spectrogram1", spectrogramToggle);
//		Util.mapKeyStrokeToButton(keyComp, "G", "spectrogram2", spectrogramToggle);
//		dest.add(spectrogramToggle);
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("L"), "cycleLogSettings");
//		keyComp.getActionMap().put("cycleLogSettings", new AbstractAction()
//				{
//					public static final long serialVersionUID = -1;
//					public void actionPerformed(ActionEvent e)
//					{
//						for (RsamViewSettings settings : settingsSet) {
//							if (settings.viewType == ViewType.SPECTRA)
//							settings.cycleLogSettings();
//							if (settings.viewType == ViewType.SPECTROGRAM)
//								settings.toggleLogPower();
//						}
//					}	
//				});
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F"), "toggleFilter");
		keyComp.getActionMap().put("toggleFilter", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						for (RsamViewSettings settings : settingsSet)
							settings.toggleFilter();
					}	
				});				
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("R"), "resetAutoScale");
		keyComp.getActionMap().put("resetAutoScale", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						for (RsamViewSettings settings : settingsSet)
							settings.resetAutoScaleMemory();
					}	
				});
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("OPEN_BRACKET"), "yScaleIn");
		keyComp.getActionMap().put("yScaleIn", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						for (RsamViewSettings settings : settingsSet) {
							settings.adjustScale(1.0 / 1.25);
						}
					}	
				});
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("CLOSE_BRACKET"), "yScaleOut");
		keyComp.getActionMap().put("yScaleOut", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						for (RsamViewSettings settings : settingsSet)
							settings.adjustScale(1.25);
					}	
				});
				
		waveTypes = new ButtonGroup();
//		waveTypes.add(waveToggle);
//		waveTypes.add(spectraToggle);
//		waveTypes.add(spectrogramToggle);
	}
	
	public void clearSettingsSet()
	{
		settingsSet.clear();
	}
	
	public void addSettings(RsamViewSettings s)
	{
		if (s != null)
		{
			settingsSet.add(s);
			s.toolbar = this;
			settingsChanged();
		}
	}
	
	public void removeSettings(RsamViewSettings s)
	{
		settingsSet.remove(s);
		if (s != null)
		{
			s.toolbar = null;
			settingsChanged();
		}
	}
	
	public void setSettings(RsamViewSettings s)
	{
		clearSettingsSet();
		addSettings(s);
	}
	
	public void settingsChanged()
	{
		boolean w = false;
		boolean s = false;
		boolean sg = false;
//		for (RsamViewSettings set : settingsSet)
//		{
//			if (set.viewType == ViewType.WAVE)
//				w = true;
//			if (set.viewType == ViewType.SPECTRA)
//				s = true;
//			if (set.viewType == ViewType.SPECTROGRAM)
//				sg = true;
//		}

		// fix for Java 1.5, clearSelection was added in 1.6
		try { 
			waveTypes.setSelected( waveTypes.getSelection(), false );
		} catch (Throwable e) {}
//		waveToggle.setSelected(w && !s && !sg);	
//		spectraToggle.setSelected(!w && s && !sg);
//		spectrogramToggle.setSelected(!w && !s && sg);
	}
}