package gov.usgs.swarm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class SwarmMenu extends JMenuBar
{
	private static final long serialVersionUID = 1L;
	private JMenu fileMenu;
	private JMenuItem exit;
	
	private JMenu editMenu;
	private JMenuItem options;
	
	private JMenu windowMenu;
	private JMenuItem tileWaves;
	private JMenuItem tileHelicorders;
	private JMenuItem fullScreen;
	private JCheckBoxMenuItem clipboard;
	private JCheckBoxMenuItem chooser;
	
	private JMenu helpMenu;
	private JMenuItem about;
	
	private AboutDialog aboutDialog;
	
	public SwarmMenu()
	{
		super();
		
		createFileMenu();
		createEditMenu();
		createWindowMenu();
		createHelpMenu();
	}
	
	private void createFileMenu()
	{
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().closeApp();
					}
				});
		fileMenu.add(exit);
		add(fileMenu);
	}
	
	private void createEditMenu()
	{
		editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		options = new JMenuItem("Options...");
		options.setMnemonic('O');
		options.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						OptionsDialog od = new OptionsDialog();
						od.setVisible(true);
					}
				});
		editMenu.add(options);
		add(editMenu);
	}
	
	private void createWindowMenu()
	{
		windowMenu = new JMenu("Window");
		windowMenu.setMnemonic('W');
		
		chooser = new JCheckBoxMenuItem("Data Chooser");
		chooser.setMnemonic('D');
		chooser.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setChooserVisible(!Swarm.getApplication().isChooserVisible());
					}
				});
		chooser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
		windowMenu.add(chooser);
		
		
		clipboard = new JCheckBoxMenuItem("Wave Clipboard");
		clipboard.setMnemonic('W');
		clipboard.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setClipboardVisible(!Swarm.getApplication().isClipboardVisible());
					}
				});
		clipboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
		windowMenu.add(clipboard);
		
		windowMenu.addSeparator();
		
		tileHelicorders = new JMenuItem("Tile Helicorders");
		tileHelicorders.setMnemonic('H');
		tileHelicorders.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().tileHelicorders();
					}
				});
		windowMenu.add(tileHelicorders);
		
		tileWaves = new JMenuItem("Tile Waves");
		tileWaves.setMnemonic('v');
		tileWaves.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().tileWaves();
					}
				});
		windowMenu.add(tileWaves);
		
		windowMenu.addSeparator();
		
		fullScreen = new JMenuItem("Kiosk Mode");
		fullScreen.setMnemonic('K');
		fullScreen.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().toggleFullScreenMode();
					}
				});
		fullScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		windowMenu.add(fullScreen);
		
		windowMenu.addMenuListener(new MenuListener()
				{
					public void menuSelected(MenuEvent e)
					{
						clipboard.setSelected(Swarm.getApplication().isClipboardVisible());
						chooser.setSelected(Swarm.getApplication().isChooserVisible());
					}

					public void menuDeselected(MenuEvent e)
					{}
					
					public void menuCanceled(MenuEvent e)
					{}
				});
		
		add(windowMenu);
	}
	
	private void createHelpMenu()
	{
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		about = new JMenuItem("About...");
		about.setMnemonic('A');
		aboutDialog = new AboutDialog();
		about.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						aboutDialog.update();
						aboutDialog.setVisible(true);
					}
				});
				
		helpMenu.add(about);
		add(helpMenu);
	}
}
