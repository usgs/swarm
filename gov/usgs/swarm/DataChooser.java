package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * 
 * TODO: edit source
 * TODO: toolbar
 * TODO: tooltip over data source describes data source
 * TODO: refresh data source
 * TODO: error box on failed open
 * TODO: confirm box on remove source
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/04/15 15:53:25  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class DataChooser extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final String OPENING_MESSAGE = Messages.getString("DataChooser.treeOpening"); //$NON-NLS-1$
	private static final char SERVER_CHAR = '!';
	private static final char CHANNEL_CHAR = '@';
	private static final char GROUP_CHAR = '#';
	private static final char BROKEN_SERVER_CHAR = '$';
	private static final char MESSAGE_CHAR = '%';
	
	private static final int MAX_CHANNELS_AT_ONCE = 500;
	public static final Color LINE_COLOR = new Color(0xac, 0xa8, 0x99);
	
	private JTree dataTree;
	private DefaultMutableTreeNode rootNode;
	
	private JToolBar toolBar;
	
	private JButton newButton;
	private JButton closeButton;
	
	private JButton heliButton;
	private JButton clipboardButton;
	private JButton monitorButton;
	private JButton realtimeButton;
	
	
	private ConfigFile groupFile;
	
	public DataChooser()
	{
		super(new BorderLayout());

		groupFile = new ConfigFile(Swarm.config.groupConfigFile);
		
		createToolBar();
		createTree();
		createActionBar();

		setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		
		addServers(Swarm.config.servers);
	}
	
	private void createToolBar()
	{
		toolBar = SwarmUtil.createToolBar();
		
		newButton = SwarmUtil.createToolBarButton(
				Images.getIcon("new_server"), //$NON-NLS-1$
				Messages.getString("DataChooser.newSourceToolTip"), //$NON-NLS-1$
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										EditDataSourceDialog d = new EditDataSourceDialog(null);
										d.setVisible(true);
										String nds = d.getResult();
										if (nds != null)
										{
											Swarm.config.addServer(nds);
											insertServer(nds);
										}
									}
								});
					}	
				});
		toolBar.add(newButton);
		
		JButton editButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("edit_server")))); //$NON-NLS-1$
		editButton.setFocusable(false);
//		editButton.setMargin(new Insets(0,0,0,0));
		editButton.setToolTipText(Messages.getString("DataChooser.editSourceToolTip")); //$NON-NLS-1$
		editButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						List<String> servers = getSelectedServers();
						if (servers != null)
						{
							String selected = servers.get(0).substring(1);
							
							EditDataSourceDialog d = new EditDataSourceDialog(Swarm.config.getServer(selected));
							d.setVisible(true);
							String eds = d.getResult();
							if (eds != null)
							{
//								((DefaultListModel)servers.getModel()).removeElementAt(i);
//								Swarm.swarmConfig.removeServer(Swarm.swarmConfig.getServer(selected));
//								((DefaultListModel)servers.getModel()).add(i, eds.substring(0, eds.indexOf(";")));
//								Swarm.swarmConfig.addServer(eds);
//								servers.setSelectedIndex(i);
							}
						}
					}	
				});
		toolBar.add(editButton);
		
		JButton collapseButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("collapse")))); //$NON-NLS-1$
		collapseButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						collapseTree(dataTree);
					}
				});
		collapseButton.setFocusable(false);
//		collapseButton.setMargin(new Insets(0,0,0,0));
		collapseButton.setToolTipText(Messages.getString("DataChooser.collapseToolTip")); //$NON-NLS-1$
		collapseButton.setFocusable(false);
		toolBar.add(collapseButton);
		
		JButton deleteButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("new_delete")))); //$NON-NLS-1$
		deleteButton.setFocusable(false);
//		deleteButton.setMargin(new Insets(0,0,0,0));
		deleteButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						List<String> servers = getSelectedServers();
						if (servers != null)
						{
							for (String server : servers)
							{
								Swarm.config.removeServer(Swarm.config.getServer(server.substring(1)));
								removeServer(server);
							}
						}
					}
				});
		deleteButton.setToolTipText(Messages.getString("DataChooser.removeSourceToolTip")); //$NON-NLS-1$
		toolBar.add(deleteButton);
		
		toolBar.add(Box.createHorizontalGlue());
		
		closeButton = SwarmUtil.createToolBarButton(
				Images.getIcon("close_view"),
				"Close data chooser",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setChooserVisible(false);
						closeButton.getModel().setRollover(false);
					}
				});
		toolBar.add(closeButton);
		
		this.add(toolBar, BorderLayout.NORTH);
	}
	
	private void createActionBar()
	{
		JPanel actionPanel = new JPanel(new GridLayout(1, 4));
		actionPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

		heliButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("heli")))); //$NON-NLS-1$
		heliButton.setToolTipText(Messages.getString("DataChooser.heliButtonToolTip")); //$NON-NLS-1$
		heliButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<String[]> channels = getSelections();
										if (channels != null)
											for (String[] channel : channels)
												Swarm.getApplication().helicorderChannelSelected(channel[0], channel[1]);
										return null;
									}
								};
						worker.start();
					}
				});
		
		realtimeButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("wave")))); //$NON-NLS-1$
		realtimeButton.setToolTipText(Messages.getString("DataChooser.waveButtonToolTip")); //$NON-NLS-1$
		realtimeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<String[]> channels = getSelections();
										if (channels != null)
											for (String[] channel : channels)
												Swarm.getApplication().waveChannelSelected(channel[0], channel[1]);
										return null;
									}
								};
						worker.start();
					}
				});
		
		clipboardButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("clipboard")))); //$NON-NLS-1$
		clipboardButton.setToolTipText(Messages.getString("DataChooser.clipboardButtonToolTip")); //$NON-NLS-1$
		clipboardButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<String[]> channels = getSelections();
										if (channels != null)
											for (String[] channel : channels)
												Swarm.getApplication().clipboardWaveChannelSelected(channel[0], channel[1]);
										return null;
									}
								};
						worker.start();
					}
				});
		
		monitorButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("monitor")))); //$NON-NLS-1$
		monitorButton.setToolTipText(Messages.getString("DataChooser.monitorButtonToolTip")); //$NON-NLS-1$
		monitorButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
						{
							public Object construct()
							{
								List<String[]> channels = getSelections();
								if (channels != null)
									for (String[] channel : channels)
										Swarm.getApplication().monitorChannelSelected(channel[0], channel[1]);
								return null;
							}
						};
						worker.start();
					}
				});
		
		actionPanel.add(heliButton);
		actionPanel.add(clipboardButton);
		actionPanel.add(monitorButton);
		actionPanel.add(realtimeButton);
		
		add(actionPanel, BorderLayout.SOUTH);
	}
	
	private List<String> getSelectedServers()
	{
		TreePath[] paths = dataTree.getSelectionPaths();
		List<String> servers = new ArrayList<String>();
		if (paths != null)
		{
			for (TreePath path : paths)
			{
				if (path.getPathCount() == 2)
				{
					String server = path.getLastPathComponent().toString();
					servers.add(server);
				}
			}
		}
		return servers;
	}
	
	private List<String[]> getSelections()
	{
		TreePath[] paths = dataTree.getSelectionPaths();
		Set<String> channels = getSelectedLeaves(paths);
		if (channels == null || channels.size() == 0)
			return null;
		
		List<String[]> list = new ArrayList<String[]>(channels.size());
		for (String channel : channels)
		{
			String[] sc = channel.split(";"); //$NON-NLS-1$
			list.add(new String[] { sc[0].substring(1), sc[1].substring(1) });
		}
		return list;
	}
	
	class MakeVisibileTSL implements TreeSelectionListener
	{
		public void valueChanged(TreeSelectionEvent e) 
		{
			if (e.isAddedPath())
			{
				TreePath[] paths = e.getPaths();
				if (paths.length == 2)
					((JTree)e.getSource()).scrollPathToVisible(paths[0]);
			}
		}
	}
	
	private void collapseTree(JTree tree)
	{
		((DefaultTreeModel)dataTree.getModel()).reload();
	}
	
	private boolean isOpened(DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(0);
		return !((String)child.getUserObject()).equals(MESSAGE_CHAR + OPENING_MESSAGE);
	}
	
	private class ExpansionListener implements TreeExpansionListener
	{
		public void treeExpanded(TreeExpansionEvent event)
		{
			TreePath path = event.getPath();
			if (path.getPathCount() == 2)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				if (!isOpened(node))
					dataSourceSelected((String)node.getUserObject());
			}
		}

		public void treeCollapsed(TreeExpansionEvent event)
		{}
	}
	
	private class CellRenderer extends DefaultTreeCellRenderer
	{
		private static final long serialVersionUID = 1L;

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean exp, boolean leaf, int row, boolean focus)
		{
			Icon icon = null;
			String val = (String)value.toString();
			String output = val.substring(1);
			switch (val.charAt(0))
			{
				case MESSAGE_CHAR:
					icon = Images.getIcon("warning"); //$NON-NLS-1$
					break;
				case SERVER_CHAR:
					icon = Images.getIcon("server"); //$NON-NLS-1$
					break;
				case CHANNEL_CHAR:
					icon = Images.getIcon("bullet"); //$NON-NLS-1$
					break;
				case GROUP_CHAR:
					icon = Images.getIcon("wave_folder"); //$NON-NLS-1$
					break;
				case BROKEN_SERVER_CHAR:
					icon = Images.getIcon("broken_server"); //$NON-NLS-1$
					break;
				default:
					output = val;
			}
			super.getTreeCellRendererComponent(tree, output, sel, exp, leaf, row, focus);
			setIcon(icon);
			return this;
		}
	}
	
	private void dataSourceSelected(final String source)
	{
		final SwingWorker worker = new SwingWorker()
				{
					private List<String> channels;
					
					public Object construct()
					{
						channels = null;
						try
						{
							String s = Swarm.config.getServer(source.substring(1));
							SeismicDataSource sds = SeismicDataSource.getDataSource(s);
							
							if (sds != null)
							{
								channels = sds.getChannels();
								sds.close();
							}
						} 
						catch (Exception e)
						{}
						return null;	
					}			
					
					public void finished()
					{
						DefaultMutableTreeNode node = getServerNode(source);
						if (channels != null)
						{
							node.setUserObject(SERVER_CHAR + node.toString().substring(1));
							((DefaultTreeModel)dataTree.getModel()).reload(node);
							populateServer(source, channels);
						}
						else
						{
							node.setUserObject(BROKEN_SERVER_CHAR + node.toString().substring(1));
							((DefaultTreeModel)dataTree.getModel()).reload(node);
							dataTree.collapsePath(new TreePath(node.getPath()));
						}
					}
				};
		worker.start();
	}
	
	public void removeServer(final String server)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						((DefaultTreeModel)dataTree.getModel()).removeNodeFromParent(getServerNode(server));
					}
				});
	}
	
	public void insertServer(final String server)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						String ns = server.split(";")[0]; //$NON-NLS-1$
						DefaultMutableTreeNode node = null;
						int i = 0;
						for (i = 0; i < rootNode.getChildCount(); i++)
						{
							String s = rootNode.getChildAt(i).toString().substring(1);
							if (ns.compareToIgnoreCase(s) <= 0)
								break;
						}
						node = new DefaultMutableTreeNode(SERVER_CHAR + ns);
						node.add(new DefaultMutableTreeNode(MESSAGE_CHAR + OPENING_MESSAGE));

						((DefaultTreeModel)dataTree.getModel()).insertNodeInto(node, rootNode, i);
					}
				});
	}
	
	public void addServers(final List<String> servers)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						for (String server : servers)
						{
							String[] ss = server.split(";"); //$NON-NLS-1$
							DefaultMutableTreeNode node = new DefaultMutableTreeNode(SERVER_CHAR + ss[0]);
							node.add(new DefaultMutableTreeNode(MESSAGE_CHAR + OPENING_MESSAGE));
							rootNode.add(node);
						}
						((DefaultTreeModel)dataTree.getModel()).reload();
					}
				});
	}
	
	private DefaultMutableTreeNode getServerNode(String server)
	{
		for (int i = 0; i < rootNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
			if (node.toString().substring(1).equals(server.substring(1)))
				return node;
		}
		return null;
	}
	
	private void createTree()
	{
		rootNode = new DefaultMutableTreeNode("root"); //$NON-NLS-1$
		dataTree = new JTree(rootNode);
		dataTree.setRootVisible(false);
		dataTree.setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 0));
		
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		dataTree.setModel(treeModel);
		
		JScrollPane scrollPane = new JScrollPane(dataTree);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xac, 0xa8, 0x99)));

		dataTree.addTreeSelectionListener(new MakeVisibileTSL());
		dataTree.addTreeExpansionListener(new ExpansionListener());
		dataTree.setCellRenderer(new CellRenderer());
		
		dataTree.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
						{
							TreePath path = dataTree.getSelectionPath();
							if (path != null)
							{
								DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
								if (node != null && node.isLeaf())
								{
									if (e.isShiftDown())
										realtimeButton.doClick();
									else if (e.isControlDown())
										clipboardButton.doClick();
									else
										heliButton.doClick();
								}
							}
						}	
					}
				});
				
		dataTree.addKeyListener(new KeyAdapter()
				{
					public void keyTyped(KeyEvent e) 
					{
						if (e.getKeyChar() == 0x0a)
							heliButton.doClick();
					}
				});
		
		add(scrollPane, BorderLayout.CENTER);
	}
	
	private void populateServer(final String server, final List<String> channels)
	{
		if (channels == null)
			return;
		
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						TreeMap<String, TreeNode> rootMap = new TreeMap<String, TreeNode>();
						
						DefaultMutableTreeNode allNode = new DefaultMutableTreeNode(GROUP_CHAR + Messages.getString("DataChooser.allGroup")); //$NON-NLS-1$
						DefaultMutableTreeNode rootNode = getServerNode(server);
						rootNode.removeAllChildren();
						rootNode.add(allNode);
						for (String channel : channels)
						{
							DefaultMutableTreeNode node = new DefaultMutableTreeNode(CHANNEL_CHAR + channel);
							allNode.add(node);
							
							if (groupFile != null)
							{
								List<String> groups = groupFile.getList(channel);
								if (groups != null)
								{
									for (String g : groups)
									{
										DefaultMutableTreeNode rn = (DefaultMutableTreeNode)rootMap.get(g);
										if (rn == null)
										{
											rn = new DefaultMutableTreeNode(GROUP_CHAR + g);
											rootMap.put(g, rn);
										}
										DefaultMutableTreeNode ln = new DefaultMutableTreeNode(CHANNEL_CHAR + channel);
										rn.add(ln);
									}
								}
							}
						}
						
						for (String key : rootMap.keySet())
						{
							DefaultMutableTreeNode n = (DefaultMutableTreeNode)rootMap.get(key);
							rootNode.add(n);
						}
						
						((DefaultTreeModel)dataTree.getModel()).reload(rootNode);
					}
				});	
	}
	
	private Set<String> getSelectedLeaves(TreePath[] paths)
	{
		if (paths == null)
			return null;
			
		boolean countExceeded = false;
		Set<String> channels = new HashSet<String>();
		for (int i = 0; i < paths.length; i++)
		{
			TreePath path = paths[i];
			if (path.getPathCount() <= 2)
				continue;
			
			DefaultMutableTreeNode serverNode = (DefaultMutableTreeNode)path.getPathComponent(1);
			String server = serverNode.toString();
			
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			if (node.isLeaf())
			{
				channels.add(server + ";" + node.toString()); //$NON-NLS-1$
			}
			else
			{
				for (Enumeration e = node.children() ; e.hasMoreElements() ;) 
				{
					DefaultMutableTreeNode node2 = (DefaultMutableTreeNode)e.nextElement();
					if (node2.isLeaf())
					{
						if (channels.size() <= MAX_CHANNELS_AT_ONCE)
							channels.add(server + ";" + node2.toString()); //$NON-NLS-1$
						else
						{
							countExceeded = true;
							break;
						}
					}
				}
			}
			
			if (countExceeded)
				break;
		}
		
		if (countExceeded)
			JOptionPane.showMessageDialog(Swarm.getApplication(), 
					Messages.getString("DataChooser.maxChannelsAtOnceError") + MAX_CHANNELS_AT_ONCE, //$NON-NLS-1$ 
					Messages.getString("DataChooser.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		
		return channels;
	}
	
}
