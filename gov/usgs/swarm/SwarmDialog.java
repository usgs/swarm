package gov.usgs.swarm;

import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.factories.ButtonBarFactory;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class SwarmDialog extends JDialog 
{
	private static final long serialVersionUID = -1;
	
	protected JButton okButton;
	protected JButton cancelButton;
	protected JPanel buttonPanel;
	
	protected JPanel mainPanel;
	
	protected JFrame parent;
	
	private boolean okClicked;
	
	protected SwarmDialog(JFrame parent, String title, boolean modal)
	{
		super(parent, title, modal);
		setResizable(false);
		this.parent = parent;
		createUI();
	}
	
	protected SwarmDialog(JFrame parent, String title, boolean modal, boolean flag)
	{
		super(parent, title, modal);
		setResizable(false);
		this.parent = parent;
		createFileTypeUI();
	}
	
	protected void setSizeAndLocation()
	{
		Dimension d = mainPanel.getPreferredSize();
		setSize(d.width + 10, d.height + 30);
		Dimension parentSize = parent.getSize();
		Point parentLoc = parent.getLocation();
		this.setLocation(parentLoc.x + (parentSize.width / 2 - d.width / 2),
				parentLoc.y + (parentSize.height / 2 - d.height / 2));
	}
	
	protected void createUI()
	{
		mainPanel = new JPanel(new BorderLayout());
		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowOK())
						{
							dispose();
							okClicked = true;
							wasOK();
						}
					}
				});
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowCancel())
						{
							dispose();
							wasCancelled();
						}
					}
				});
		Util.mapKeyStrokeToButton(mainPanel, "ESCAPE", "cancel1", cancelButton);
		this.addWindowListener(new WindowAdapter() 
				{
		            public void windowOpened(WindowEvent e)
		            {
		            	okButton.requestFocus();
		                JRootPane root = SwingUtilities.getRootPane(okButton);
		                if (root != null) 
		                    root.setDefaultButton(okButton);
		            }
		            
		            public void windowClosing(WindowEvent e)
		            {
		            	if (!okClicked)
		            		wasCancelled();
		            }
				});

		buttonPanel = ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		this.setContentPane(mainPanel);
	}
	
	protected void createFileTypeUI()
	{
		mainPanel = new JPanel(new BorderLayout());
		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowOK())
						{
							dispose();
							okClicked = true;
							wasOK();
						}
					}
				});
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowCancel())
						{
							dispose();
							wasCancelled();
							Swarm.cancelProcess = true;
						}
					}
				});
		Util.mapKeyStrokeToButton(mainPanel, "ESCAPE", "cancel1", cancelButton);
		this.addWindowListener(new WindowAdapter() 
				{
		            public void windowOpened(WindowEvent e)
		            {
		            	okButton.requestFocus();
		                JRootPane root = SwingUtilities.getRootPane(okButton);
		                if (root != null) 
		                    root.setDefaultButton(okButton);
		            }
		            
		            public void windowClosing(WindowEvent e)
		            {
		            	if (!okClicked)
		            		wasCancelled();
		            }
				});

		buttonPanel = ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		this.setContentPane(mainPanel);
	}
	
	protected void createUITimeZoneDialog()
	{
		mainPanel = new JPanel(new BorderLayout());
		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowOK())
						{
							dispose();
							okClicked = true;
//							Swarm.isCancelled = true;
							wasOKTZ();
						}
					}
				});
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowCancel())
						{
							dispose();
							wasCancelledTZ();
						}
					}
				});
		Util.mapKeyStrokeToButton(mainPanel, "ESCAPE", "cancel1", cancelButton);
		this.addWindowListener(new WindowAdapter() 
				{
		            public void windowOpened(WindowEvent e)
		            {
		            	okButton.requestFocus();
		                JRootPane root = SwingUtilities.getRootPane(okButton);
		                if (root != null) 
		                    root.setDefaultButton(okButton);
		            }
		            
		            public void windowClosing(WindowEvent e)
		            {
		            	if (!okClicked)
		            		wasCancelledTZ();
		            }
				});

		buttonPanel = ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		this.setContentPane(mainPanel);
	}
	
	protected void createUIWaveDialog()
	{
		mainPanel = new JPanel(new BorderLayout());
		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowOK())
						{
							dispose();
							okClicked = true;
//							Swarm.isCancelled = true;
							wasOK();
						}
					}
				});
		cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (allowCancel())
						{
							dispose();
							wasCancelled();
							Swarm.isCancelled = true;
						}
					}
				});
		Util.mapKeyStrokeToButton(mainPanel, "ESCAPE", "cancel1", cancelButton);
		this.addWindowListener(new WindowAdapter() 
				{
		            public void windowOpened(WindowEvent e)
		            {
		            	okButton.requestFocus();
		                JRootPane root = SwingUtilities.getRootPane(okButton);
		                if (root != null) 
		                    root.setDefaultButton(okButton);
		            }
		            
		            public void windowClosing(WindowEvent e)
		            {
		            	if (!okClicked)
		            		wasCancelled();
		            }
				});

		buttonPanel = ButtonBarFactory.buildOKCancelBar(okButton, cancelButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		this.setContentPane(mainPanel);
	}
	
	protected boolean allowOK()
	{
		return true;
	}
	
	protected boolean allowCancel()
	{
		return true;
	}
	
	protected void wasOK() {}
	protected void wasCancelled() {}
	protected void wasOKTZ() {}
	protected void wasCancelledTZ() {}
}
