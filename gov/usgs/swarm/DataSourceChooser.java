package gov.usgs.swarm;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * This is a UI element for choosing which data source to use.
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2005/04/11 00:22:41  cervelli
 * Removed JDK 1.5 deprecated Dialog.show().
 *
 * Revision 1.2  2004/10/12 23:43:15  cvs
 * Added log info and some comments.
 *
 * @author Dan Cervelli
 */
public class DataSourceChooser extends JPanel
{
	private static final long serialVersionUID = -1;
	private Swarm swarm;
	
	private JList servers;
	private JButton newButton;
	private JButton goButton;
	private JButton removeButton;
	private JButton editButton;
	private JScrollPane scrollPane;
	
	/**
	 * Constructs a <code>DataSourceChooser</code>.
	 * @param sw the application parent
	 */
	public DataSourceChooser(Swarm sw)
	{
		swarm = sw;
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						createUI();
					}
				});
	}
	
	/**
	 * Creates the user interface.
	 */
	private void createUI()
	{
		this.setLayout(new BorderLayout());
		servers = new JList();
		servers.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (goButton.isEnabled() && e.getClickCount() == 2 && servers.getSelectedIndex() != -1)
						{
							goButton.setEnabled(false);
							swarm.dataSourceSelected((String)servers.getModel().getElementAt(servers.getSelectedIndex()));
						}
					}
				});
		
		scrollPane = new JScrollPane(servers);
		servers.setVisibleRowCount(8);
		newButton = new JButton("New");
		goButton = new JButton("Go");
		removeButton = new JButton("Remove");
		editButton = new JButton("Edit");
		//removeButton.setMargin(new Insets(0,0,0,0));
		removeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (servers.getSelectedIndex() != -1)
						{
							swarm.getConfig().remove("server", swarm.findSource((String)servers.getModel().getElementAt(servers.getSelectedIndex())));
							((DefaultListModel)servers.getModel()).removeElementAt(servers.getSelectedIndex());
						}
					}
				});
		
		goButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (servers.getSelectedIndex() != -1)
						{
							goButton.setEnabled(false);
							swarm.dataSourceSelected((String)servers.getModel().getElementAt(servers.getSelectedIndex()));
						}
					}	
				});
		newButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						EditDataSourceDialog d = new EditDataSourceDialog(null);
						d.setVisible(true);
						String nds = d.getResult();//getNewDataSource();
						if (nds != null)
						{
							((DefaultListModel)servers.getModel()).addElement(nds.substring(0, nds.indexOf(";")));
							swarm.getConfig().put("server", nds, true);
						}
					}	
				});
		editButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						int i = servers.getSelectedIndex(); 
						if (i != -1)
						{
							
							String selected = (String)servers.getModel().getElementAt(i);
							
							EditDataSourceDialog d = new EditDataSourceDialog(Swarm.getParentFrame().findSource(selected));
							d.setVisible(true);
							String eds = d.getResult();
							if (eds != null)
							{
								((DefaultListModel)servers.getModel()).removeElementAt(i);
								swarm.getConfig().remove("server", swarm.findSource(selected));
								((DefaultListModel)servers.getModel()).add(i, eds.substring(0, eds.indexOf(";")));
								swarm.getConfig().put("server", eds, true);
								servers.setSelectedIndex(i);
							}
						}
					}	
				});
		
		this.add(scrollPane, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel(new GridLayout(2, 2));
		buttonPanel.add(goButton);
		buttonPanel.add(editButton);
		buttonPanel.add(newButton);
		buttonPanel.add(removeButton);
		this.add(buttonPanel, BorderLayout.SOUTH);
		this.setBorder(new TitledBorder(new EtchedBorder(), "Data Sources"));
		
		java.util.List<String> sl = swarm.getConfig().getList("server");
		Collections.sort(sl);
		DefaultListModel model = new DefaultListModel();
		if (sl != null)
		{
			Iterator it = sl.iterator();
			while (it.hasNext())
			{
				String server = (String)it.next();
				model.addElement(server.substring(0, server.indexOf(";")));
			}
		}
		servers.setModel(model);
	}
	
	/**
	 * Enables the "Go" button in the chooser. 
	 */
	public void enableGoButton()
	{
		
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						goButton.setEnabled(true);
					}	
				});
	}
}