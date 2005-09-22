package gov.usgs.swarm;

import gov.usgs.util.Util;
import gov.usgs.util.ui.ExtensionFileFilter;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * The wave clipboard internal frame.
 * 
 * TODO: refactor, clean up dialog boxes.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2005/08/30 00:34:26  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:31:20  tparker
 * Tidied code, no functional changes.
 *
 * Revision 1.2  2005/08/26 23:49:04  tparker
 * Create image path constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.3  2005/03/27 22:03:46  cervelli
 * Added Save All button.  Moved the Save/Save All/Open ActionListeners into separate
 * internal non-anonymous classes.  Open now opens directories.
 *
 * Revision 1.2  2005/03/24 20:48:31  cervelli
 * Support for opening and saving SAC files.
 *
 * @author Dan Cervelli
 */
public class WaveClipboardFrame extends JInternalFrame
{
	public static final long serialVersionUID = -1;
		
	private JScrollPane scrollPane;
	private Box waveBox;
	private List<ClipboardWaveViewPanel> waves;
	private ClipboardWaveViewPanel selected;
	private JToolBar toolbar;
	private JPanel mainPanel;
	private JLabel statusLabel;
	private JButton syncButton;
	private JButton removeButton;
	private JButton saveButton;
	private JButton saveAllButton;
	private JButton openButton;
	private DateFormat saveAllDateFormat;

	public WaveClipboardFrame()
	{
		super("Wave Clipboard", true, false, true, true);
		saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		waves = new ArrayList<ClipboardWaveViewPanel>();
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						createUI();
					}
				});
				
		this.addComponentListener(new ComponentAdapter()
				{
					public void componentResized(ComponentEvent e)
					{
						resizeWaves();
					}
				});
		
	}
	 
	public void createUI()
	{
		this.setSize(600, 700);
		this.setLocation(0, 0);
		mainPanel = new JPanel(new BorderLayout());

		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setRollover(true);
		
		openButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("open"))));
		openButton.setEnabled(true);
		openButton.setMargin(new Insets(0,0,0,0));
		openButton.setToolTipText("Open a saved wave");
		openButton.addActionListener(new OpenActionListener());
		toolbar.add(openButton);
		
		saveButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("save"))));
		saveButton.setEnabled(false);
		saveButton.setMargin(new Insets(0,0,0,0));
		saveButton.setToolTipText("Save selected wave");
		saveButton.addActionListener(new SaveActionListener());
		toolbar.add(saveButton);

		saveAllButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("saveall"))));
		saveAllButton.setEnabled(false);
		saveAllButton.setMargin(new Insets(0,0,0,0));
		saveAllButton.setToolTipText("Save all waves");
		saveAllButton.addActionListener(new SaveAllActionListener());
		toolbar.add(saveAllButton);
		
		toolbar.addSeparator();
		syncButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("clock"))));
		syncButton.setEnabled(false);
		syncButton.setMargin(new Insets(0,0,0,0));
		syncButton.setToolTipText("Synchronize times with selected wave");
		syncButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected == null)
							return;
						
						syncChannels();
					}
				});
		toolbar.add(syncButton);
		toolbar.addSeparator();
		
		removeButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("delete"))));
		removeButton.setEnabled(false);
		removeButton.setMargin(new Insets(0,0,0,0));
		removeButton.setToolTipText("Remove all waves from clipboard");
		removeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						removeWaves();
					}
				});
		toolbar.add(removeButton);
		mainPanel.add(toolbar, BorderLayout.NORTH);
		
		/*
		JButton particleButton = new JButton("Particle Test");
		particleButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						ClipboardWaveViewPanel w1 = (ClipboardWaveViewPanel)waves.elementAt(0);
						ClipboardWaveViewPanel w2 = (ClipboardWaveViewPanel)waves.elementAt(1);
						
						JFrame tf = new JFrame("Particle Test");
						tf.setSize(600, 1000);
						ParticleView pv = new ParticleView();
						pv.setWaves(w1.getWaveViewPanel().getWave(), w2.getWaveViewPanel().getWave(), null);
						
						tf.setContentPane(pv);
						tf.setVisible(true);
					}
				});
		toolbar.add(particleButton);
		*/
		
		waveBox = new Box(BoxLayout.Y_AXIS);
		scrollPane = new JScrollPane(waveBox);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(40);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		statusLabel = new JLabel(" ", JLabel.LEFT);
		mainPanel.add(statusLabel, BorderLayout.SOUTH);
		this.setContentPane(mainPanel);
		
		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameActivated(InternalFrameEvent e)
					{
						if (selected != null)
							selected.setSelected(true);							
					}
					
					public void internalFrameDeiconified(InternalFrameEvent e)
					{
						resizeWaves();	
					}
				});
		this.setVisible(true);
	}
	
	private class OpenActionListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e)
		{
			JFileChooser chooser = Swarm.getParentFrame().getFileChooser();
			chooser.resetChoosableFileFilters();
			ExtensionFileFilter txtExt = new ExtensionFileFilter(".txt", "Matlab-readable text files");
			ExtensionFileFilter sacExt = new ExtensionFileFilter(".sac", "SAC files");
			chooser.addChoosableFileFilter(txtExt);
			chooser.addChoosableFileFilter(sacExt);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			File lastPath = new File(Swarm.getParentFrame().getConfig().getString("lastPath"));
			chooser.setCurrentDirectory(lastPath);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(true);
			int result = chooser.showOpenDialog(Swarm.getParentFrame());
			if (result == JFileChooser.APPROVE_OPTION) 
			{						            
				File[] fs = chooser.getSelectedFiles();

				for (int i = 0; i < fs.length; i++)
				{
				    if (fs[i].isDirectory())
				    {
				        File[] dfs = fs[i].listFiles();
				        for (int j = 0; j < dfs.length; j++)
					        openFile(dfs[j]);
					    Swarm.getParentFrame().getConfig().put("lastPath", fs[i].getParent(), false);
				    }
				    else
				    {
				        openFile(fs[i]);
				        
					    Swarm.getParentFrame().getConfig().put("lastPath", fs[i].getParent(), false);
				    }
				}
			}
		}
	}
	
	private class SaveActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (selected == null)
				return;
			
			WaveViewPanel wvp = selected.getWaveViewPanel();
			
			JFileChooser chooser = Swarm.getParentFrame().getFileChooser();
			chooser.resetChoosableFileFilters();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(false);
			
			ExtensionFileFilter txtExt = new ExtensionFileFilter(".txt", "Matlab-readable text files");
			ExtensionFileFilter sacExt = new ExtensionFileFilter(".sac", "SAC files");
			chooser.addChoosableFileFilter(txtExt);
			chooser.addChoosableFileFilter(sacExt);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			
			File lastPath = new File(Swarm.getParentFrame().getConfig().getString("lastPath"));
			chooser.setCurrentDirectory(lastPath);
			chooser.setSelectedFile(new File(wvp.getChannel() + ".txt"));
			int result = chooser.showSaveDialog(Swarm.getParentFrame());
			if (result == JFileChooser.APPROVE_OPTION) 
			{						            
				File f = chooser.getSelectedFile();
				String path = f.getPath();
				if (!(path.endsWith(".txt") || path.endsWith(".sac")))
				{
				    if (chooser.getFileFilter() == sacExt)
				        f = new File(path + ".sac");
				    else
				        f = new File(path + ".txt");
				}
				boolean confirm = true;
				if (f.exists())
				{
				    if (f.isDirectory())
				    {
				        JOptionPane.showMessageDialog(Swarm.getParentFrame(), "You can not select an existing directory.", "Error", JOptionPane.ERROR_MESSAGE);
					    return;
				    }
					confirm = false;
					int choice = JOptionPane.showConfirmDialog(Swarm.getParentFrame(), "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION)
						confirm = true;
				}
				
				if (confirm)
				{
					Swarm.getParentFrame().getConfig().put("lastPath", f.getParent(), false);
					String fn = f.getPath().toLowerCase();
					if (fn.endsWith(".sac"))
					{
					    SAC sac = wvp.getWave().toSAC();
					    String[] scn = wvp.getChannel().split(" ");
					    sac.kstnm = scn[0];
					    sac.kcmpnm = scn[1];
					    sac.knetwk = scn[2];
					    try
					    {
					        sac.write(f);
					    }
					    catch (IOException ex)
					    {
					        ex.printStackTrace();
					    }
					}
					else
					    wvp.getWave().exportToText(f.getPath());
				}
			}
		}
	}
	
	private class SaveAllActionListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e)
		{
			if (waves.size() <= 0)
				return;
			
			JFileChooser chooser = Swarm.getParentFrame().getFileChooser();
			chooser.resetChoosableFileFilters();
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			File lastPath = new File(Swarm.getParentFrame().getConfig().getString("lastPath"));
			chooser.setCurrentDirectory(lastPath);
			int result = chooser.showSaveDialog(Swarm.getParentFrame());
			File f = chooser.getSelectedFile();
			if (f == null)
			{
			    JOptionPane.showMessageDialog(Swarm.getParentFrame(), "You must select a directory.", "Error", JOptionPane.ERROR_MESSAGE);
			    return;
			}
			if (result == JFileChooser.APPROVE_OPTION) 
			{	
			    if (f.exists() && !f.isDirectory())
			        return;
			    if (!f.exists())
			        f.mkdir();
			    for (ClipboardWaveViewPanel wave : waves)
			    {
			    	WaveViewPanel wvp = wave.getWaveViewPanel();
			        Wave sw = wvp.getWave();
			        if (sw != null)
			        {
			            String date = saveAllDateFormat.format(Util.j2KToDate(sw.getStartTime()));
			            File dir = new File(f.getPath() + File.separatorChar + date);
			            if (!dir.exists())
			                dir.mkdir();
			            
			            SAC sac = sw.toSAC();
					    String[] scn = wvp.getChannel().split(" ");
					    sac.kstnm = scn[0];
					    sac.kcmpnm = scn[1];
					    sac.knetwk = scn[2];
					    try
					    {
					        sac.write(new File(dir.getPath() + File.separatorChar + wvp.getChannel() + ".sac"));
					    }
					    catch (IOException ex)
					    {
					        ex.printStackTrace();
					    }
			        }
			    }
			    Swarm.getParentFrame().getConfig().put("lastPath", f.getPath(), false);
			}
		}
	}
	
	private enum FileType 
	{ 
		TEXT, SAC, UNKNOWN;
		
		public static FileType fromFile(File f)
		{
			if (f.getPath().endsWith(".sac"))
				return SAC;
			else if( f.getPath().endsWith(".txt"))
				return TEXT;
			else 
				return UNKNOWN;
		}
	}
	
	private SAC readSAC(File f)
	{
		SAC sac = new SAC();
	    try
	    {
	        sac.read(f.getPath());
	    }
	    catch (Exception ex)
	    {
	    	sac = null;
	    }
	    return sac;
	}
	
	public void openFile(File f)
	{
		SAC sac = null;
	    Wave sw = null;
	    String channel = f.getName();
	    FileType ft = FileType.fromFile(f);
	    switch (ft)
	    {
		    case SAC:
		    	sac = readSAC(f);
		    	
		    	break;
		    case TEXT:
		    	sw = Wave.importFromText(f.getPath());
		    	break;
		    case UNKNOWN:
		    	// try SAC
		    	sac = readSAC(f);
		    	// try text
		    	if (sac == null)
		    		sw = Wave.importFromText(f.getPath());
		    	break;
	    }
	    
	    if (sac != null)
    	{
    		sw = sac.toWave();
    		channel = sac.getWinstonChannel().replace('$', ' ');
    	}
	    
		if (sw != null)
		{
			WaveViewPanel wvp = new WaveViewPanel();
			wvp.setChannel(channel);
			Swarm.getCache().putWave(f.getName(), sw);
			wvp.setDataSource(Swarm.getCache());
			wvp.setWave(sw, sw.getStartTime(), sw.getEndTime());
			WaveClipboardFrame.this.addWave(new ClipboardWaveViewPanel(wvp));
		}
		else
			JOptionPane.showMessageDialog(Swarm.getParentFrame(), "There was an error opening the file, '" + f.getName() + "'.", "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	private void doButtonEnables()
	{
		boolean enable = (waves == null || waves.size() == 0);
		saveButton.setEnabled(!enable);
		saveAllButton.setEnabled(!enable);
		syncButton.setEnabled(!enable);
		removeButton.setEnabled(!enable);
	}
	
	public synchronized void syncChannels()
	{
		final double st = selected.getWaveViewPanel().getStartTime();
		final double et = selected.getWaveViewPanel().getEndTime();
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						for (ClipboardWaveViewPanel wave : waves)
						{
							if (wave != selected)
							{
								WaveViewPanel wvp = wave.getWaveViewPanel();
								if (wvp.getDataSource() != null)
								{
									Wave sw = wvp.getDataSource().getWave(wvp.getChannel(), st, et);
									wvp.setWave(sw, st, et);
								}
							}
						}
						return null;
					}
					
					public void finished()
					{
						repaint();	
					}
				};
		worker.start();	
	}
	
	public ClipboardWaveViewPanel getSelected()
	{
		return selected;
	}
	
	public synchronized void addWave(ClipboardWaveViewPanel p)
	{
		p.setClipboard(this);
		p.getWaveViewPanel().setStatusLabel(statusLabel);
		p.getWaveViewPanel().setAllowDragging(true);
		waveBox.add(p);	
		waves.add(p);
		resizeWaves();
		
		if (waves.size() == 1)
		{
			selected = p;
			selected.setSelected(true);
		}
		doButtonEnables();
	}
	
	public synchronized void select(ClipboardWaveViewPanel p)
	{
		if (selected != null)
			selected.setSelected(false);
		selected = p;
		selected.setSelected(true);
		repaint();
	}
	
	public synchronized void remove(ClipboardWaveViewPanel p)
	{
		if (selected == p)
			selected = null;
			
		p.close();
		waveBox.remove(p);
		waves.remove(p);
		if (selected == null && waves.size() > 0)
		{
			selected = waves.get(0);
			selected.setSelected(true);	
		}
		doButtonEnables();
		resizeWaves();
	}
	
	public synchronized void moveDown(ClipboardWaveViewPanel p)
	{
		int i = waves.indexOf(p);
		if (i == waves.size() - 1)
			return;
			
		waves.remove(i);
		waves.add(i + 1, p);
		waveBox.remove(p);
		waveBox.add(p, i + 1);
		waveBox.validate();
		if (selected != null)
			selected.requestFocus();
		repaint();
		
	}
	
	public synchronized void moveUp(ClipboardWaveViewPanel p)
	{
		int i = waves.indexOf(p);
		if (i == 0)
			return;
			
		waves.remove(i);
		waves.add(i - 1, p);
		waveBox.remove(p);
		waveBox.add(p, i - 1);
		waveBox.validate();
		if (selected != null)
			selected.requestFocus();
		repaint();
	}
	
	public void resizeWaves()
	{
		waveBox.validate();
		for (ClipboardWaveViewPanel wave : waves)
		{
			wave.setSize(waveBox.getSize().width, 200);
		}
		scrollPane.validate();
		for (ClipboardWaveViewPanel wave : waves)
		{
			wave.setSize(scrollPane.getViewport().getSize().width, 200);
		}
		this.validate();
		repaint();
	}
	 
	public void removeWaves()
	{
		for (ClipboardWaveViewPanel wave : waves)
		{
			wave.close();
			waveBox.remove(wave);
		}
		selected = null;
		waves.clear();
		waveBox.validate();
		scrollPane.validate();
		doButtonEnables();
		repaint();
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		if (waves.size() == 0)
		{
			Dimension dim = this.getSize();
			g.setColor(Color.black);
			g.drawString("Clipboard empty.", dim.width / 2 - 40, dim.height / 2);	
		}	
	}
}