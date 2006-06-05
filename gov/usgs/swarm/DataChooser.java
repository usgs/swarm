package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Pair;

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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
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
 * Revision 1.2  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.1  2006/04/15 15:53:25  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class DataChooser extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final String OPENING_MESSAGE = Messages.getString("DataChooser.treeOpening"); //$NON-NLS-1$
	
	private static final int MAX_CHANNELS_AT_ONCE = 500;
	public static final Color LINE_COLOR = new Color(0xac, 0xa8, 0x99);
	
	private JTree dataTree;
	private DefaultMutableTreeNode rootNode;
	
	private JToolBar toolBar;
	
	private JButton editButton;
	private JButton newButton;
	private JButton closeButton;
	private JButton collapseButton;
	private JButton deleteButton;
	
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
		
		addServers(Swarm.config.sources);
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
											SeismicDataSource source = SeismicDataSource.getDataSource(nds);
											if (source != null)
											{
												insertServer(source);
											}
										}
									}
								});
					}	
				});
		toolBar.add(newButton);
		
		editButton = SwarmUtil.createToolBarButton(
				Images.getIcon("edit_server"), //$NON-NLS-1$
				Messages.getString("DataChooser.editSourceToolTip"), //$NON-NLS-1$
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						List<String> servers = getSelectedServers();
//						if (servers != null)
//						{
//							String selected = servers.get(0).substring(1);
//							
//							EditDataSourceDialog d = new EditDataSourceDialog(Swarm.config.getServer(selected));
//							d.setVisible(true);
//							String eds = d.getResult();
//							if (eds != null)
//							{
////								((DefaultListModel)servers.getModel()).removeElementAt(i);
////								Swarm.swarmConfig.removeServer(Swarm.swarmConfig.getServer(selected));
////								((DefaultListModel)servers.getModel()).add(i, eds.substring(0, eds.indexOf(";")));
////								Swarm.swarmConfig.addServer(eds);
////								servers.setSelectedIndex(i);
//							}
//						}
					}	
				});
		toolBar.add(editButton);
		
		collapseButton = SwarmUtil.createToolBarButton(
				Images.getIcon("collapse"), //$NON-NLS-1$
				Messages.getString("DataChooser.collapseToolTip"), //$NON-NLS-1$		
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						collapseTree(dataTree);
					}
				});
		toolBar.add(collapseButton);
		
		deleteButton = SwarmUtil.createToolBarButton(
				Images.getIcon("new_delete"), //$NON-NLS-1$
				Messages.getString("DataChooser.removeSourceToolTip"), //$NON-NLS-1$
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						List<ServerNode> servers = getSelectedServers();
						if (servers != null)
						{
							for (ServerNode server : servers)
								removeServer(server);
						}
					}
				});
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

		heliButton = new JButton(Images.getIcon("heli")); //$NON-NLS-1$
		heliButton.setToolTipText(Messages.getString("DataChooser.heliButtonToolTip")); //$NON-NLS-1$
		heliButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<Pair<ServerNode, ChannelNode>> channels = getSelections();
										if (channels != null)
										{
											for (Pair<ServerNode, ChannelNode> pair : channels)
											{
												Swarm.getApplication().openHelicorder(pair.item1.getSource(), pair.item2.getChannel());
											}
										}
										return null;
									}
								};
						worker.start();
					}
				});
		
		realtimeButton = new JButton(Images.getIcon("wave")); //$NON-NLS-1$
		realtimeButton.setToolTipText(Messages.getString("DataChooser.waveButtonToolTip")); //$NON-NLS-1$
		realtimeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<Pair<ServerNode, ChannelNode>> channels = getSelections();
										if (channels != null)
										{
											for (Pair<ServerNode, ChannelNode> pair : channels)
											{
												Swarm.getApplication().openRealtimeWave(pair.item1.getSource(), pair.item2.getChannel());
											}
										}
										return null;
									}
								};
						worker.start();
					}
				});
		
		clipboardButton = new JButton(Images.getIcon("clipboard")); //$NON-NLS-1$
		clipboardButton.setToolTipText(Messages.getString("DataChooser.clipboardButtonToolTip")); //$NON-NLS-1$
		clipboardButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<Pair<ServerNode, ChannelNode>> channels = getSelections();
										if (channels != null)
										{
											for (Pair<ServerNode, ChannelNode> pair : channels)
											{
												Swarm.getApplication().loadClipboardWave(pair.item1.getSource(), pair.item2.getChannel());
											}
										}
										return null;
									}
								};
						worker.start();
					}
				});
		
		monitorButton = new JButton(Images.getIcon("monitor")); //$NON-NLS-1$
		monitorButton.setToolTipText(Messages.getString("DataChooser.monitorButtonToolTip")); //$NON-NLS-1$
		monitorButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
						{
							public Object construct()
							{
								List<Pair<ServerNode, ChannelNode>> channels = getSelections();
								if (channels != null)
								{
									for (Pair<ServerNode, ChannelNode> pair : channels)
									{
										Swarm.getApplication().monitorChannelSelected(pair.item1.getSource(), pair.item2.getChannel());
									}
								}
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
	
	private List<ServerNode> getSelectedServers()
	{
		TreePath[] paths = dataTree.getSelectionPaths();
		List<ServerNode> servers = new ArrayList<ServerNode>();
		if (paths != null)
		{
			for (TreePath path : paths)
			{
				if (path.getPathCount() == 2)
				{
					ServerNode node = (ServerNode)path.getLastPathComponent();
					servers.add(node);
				}
			}
		}
		return servers;
	}
	
	private List<Pair<ServerNode, ChannelNode>> getSelections()
	{
		TreePath[] paths = dataTree.getSelectionPaths();
		return getSelectedLeaves(paths);
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
	
	private boolean isOpened(ChooserNode node)
	{
		ChooserNode child = (ChooserNode)node.getChildAt(0);
		if (!(child instanceof MessageNode))
			return true;
		if (((MessageNode)child).getLabel().equals(OPENING_MESSAGE))
			return false;
		return true;
	}
	
	private class ExpansionListener implements TreeExpansionListener
	{
		public void treeExpanded(TreeExpansionEvent event)
		{
			TreePath path = event.getPath();
			if (path.getPathCount() == 2)
			{
				ServerNode node = (ServerNode)path.getLastPathComponent();
				if (!isOpened(node))
					dataSourceSelected(node);
			}
		}

		public void treeCollapsed(TreeExpansionEvent event)
		{}
	}
	
	private void dataSourceSelected(final ServerNode source)
	{
		final SwingWorker worker = new SwingWorker()
				{
					private List<String> channels;
					
					public Object construct()
					{
						SeismicDataSource sds = source.getSource();
						channels = null;
						try
						{
							sds.establish();
							channels = sds.getChannels();
							sds.close();
						} 
						catch (Exception e)
						{}
						return null;	
					}			
					
					public void finished()
					{
						if (channels != null)
						{
							source.setBroken(false);
							((DefaultTreeModel)dataTree.getModel()).reload(source);
							populateServer(source, channels);
						}
						else
						{
							source.setBroken(true);
							((DefaultTreeModel)dataTree.getModel()).reload(source);
							dataTree.collapsePath(new TreePath(source.getPath()));
						}
					}
				};
		worker.start();
	}
	
	public void removeServer(final ServerNode node)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						Swarm.config.removeSource(node.getSource().getName());
						((DefaultTreeModel)dataTree.getModel()).removeNodeFromParent(node);
					}
				});
	}
	
	public void insertServer(final SeismicDataSource source)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						Swarm.config.addSource(source);
						
						String ns = source.getName();
						int i = 0;
						for (i = 0; i < rootNode.getChildCount(); i++)
						{
							String s = ((ServerNode)rootNode.getChildAt(i)).getSource().getName();
							if (ns.compareToIgnoreCase(s) <= 0)
								break;
						}
						ServerNode node = new ServerNode(source);
						node.add(new MessageNode(OPENING_MESSAGE));

						((DefaultTreeModel)dataTree.getModel()).insertNodeInto(node, rootNode, i);
					}
				});
	}
	
	public void addServers(final Map<String, SeismicDataSource> servers)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						for (String key : servers.keySet())
						{
							SeismicDataSource sds = servers.get(key);
							ServerNode node = new ServerNode(sds);
							node.add(new MessageNode(OPENING_MESSAGE));
							rootNode.add(node);
						}
						((DefaultTreeModel)dataTree.getModel()).reload();
					}
				});
	}
	
//	private DefaultMutableTreeNode getServerNode(String server)
//	{
//		for (int i = 0; i < rootNode.getChildCount(); i++)
//		{
//			DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
//			if (node.toString().substring(1).equals(server.substring(1)))
//				return node;
//		}
//		return null;
//	}
	
	private void createTree()
	{
//		rootNode = new DefaultMutableTreeNode("root"); //$NON-NLS-1$
		rootNode = new RootNode(); //$NON-NLS-1$
		dataTree = new JTree(rootNode);
		dataTree.setRootVisible(false);
		dataTree.setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 0));
		
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
		dataTree.setModel(treeModel);
		
		JScrollPane scrollPane = new JScrollPane(dataTree);
		
		dataTree.addTreeSelectionListener(new MakeVisibileTSL());
		dataTree.addTreeExpansionListener(new ExpansionListener());
		dataTree.setCellRenderer(new CellRenderer2());
		
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
	
	private void populateServer(final ServerNode node, final List<String> channels)
	{
		if (channels == null)
			return;
		
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						TreeMap<String, GroupNode> rootMap = new TreeMap<String, GroupNode>();
						
//						DefaultMutableTreeNode allNode = new DefaultMutableTreeNode(GROUP_CHAR + Messages.getString("DataChooser.allGroup")); //$NON-NLS-1$
						GroupNode allNode = new GroupNode(Messages.getString("DataChooser.allGroup")); //$NON-NLS-1$
//						DefaultMutableTreeNode rootNode = getServerNode(server);
//						DefaultMutableTreeNode rootNode = getServerNode(server);
						ChooserNode rootNode = node;
						rootNode.removeAllChildren();
						rootNode.add(allNode);
						for (String channel : channels)
						{
//							DefaultMutableTreeNode node = new DefaultMutableTreeNode(CHANNEL_CHAR + channel);
							ChannelNode node = new ChannelNode(channel);
							allNode.add(node);
							
							if (groupFile != null)
							{
								List<String> groups = groupFile.getList(channel);
								if (groups != null)
								{
									for (String g : groups)
									{
										GroupNode gn = rootMap.get(g);
//										DefaultMutableTreeNode rn = (DefaultMutableTreeNode)rootMap.get(g);
										if (gn == null)
										{
											gn = new GroupNode(g);
											rootMap.put(g, gn);
										}
										ChannelNode ln = new ChannelNode(channel);
										gn.add(ln);
									}
								}
							}
						}
						
						for (String key : rootMap.keySet())
						{
							GroupNode n = rootMap.get(key);
							rootNode.add(n);
						}
						
						((DefaultTreeModel)dataTree.getModel()).reload(rootNode);
					}
				});	
	}
	
//	private Set<String> getSelectedLeaves(TreePath[] paths)
	private List<Pair<ServerNode, ChannelNode>> getSelectedLeaves(TreePath[] paths)
	{
		if (paths == null)
			return null;
			
		boolean countExceeded = false;
		List<Pair<ServerNode, ChannelNode>> selections = new ArrayList<Pair<ServerNode, ChannelNode>>();
		for (int i = 0; i < paths.length; i++)
		{
			TreePath path = paths[i];
			if (path.getPathCount() <= 2)
				continue;
			
			ServerNode serverNode = (ServerNode)path.getPathComponent(1);
//			String server = serverNode.toString();
			
			ChooserNode node = (ChooserNode)path.getLastPathComponent();
			if (node.isLeaf() && node instanceof ChannelNode)
			{
				selections.add(new Pair<ServerNode, ChannelNode>(serverNode, (ChannelNode)node));
			}
			else if (!node.isLeaf())
			{
				for (Enumeration e = node.children() ; e.hasMoreElements() ;) 
				{
					ChooserNode node2 = (ChooserNode)e.nextElement();
					if (node2.isLeaf() && node2 instanceof ChannelNode)
					{
						selections.add(new Pair<ServerNode, ChannelNode>(serverNode, (ChannelNode)node2));
//						if (channels.size() <= MAX_CHANNELS_AT_ONCE)
//							channels.add(server + ";" + node2.toString()); //$NON-NLS-1$
//						else
//						{
//							countExceeded = true;
//							break;
//						}
					}
				}
			}
			
//			if (countExceeded)
//				break;
		}
		
		if (countExceeded)
			JOptionPane.showMessageDialog(Swarm.getApplication(), 
					Messages.getString("DataChooser.maxChannelsAtOnceError") + MAX_CHANNELS_AT_ONCE, //$NON-NLS-1$ 
					Messages.getString("DataChooser.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		
		return selections;
	}
	
	abstract private class ChooserNode extends DefaultMutableTreeNode
	{
		private static final long serialVersionUID = 1L;

		abstract public Icon getIcon();
		abstract public String getLabel();
		
		public String toString()
		{
			return "chooserNode";
		}
	}
	
	private class RootNode extends ChooserNode
	{
		private static final long serialVersionUID = 1L;
		public Icon getIcon() { return null; }
		public String getLabel() { return null; }
	}
	
	private class ServerNode extends ChooserNode
	{
		private static final long serialVersionUID = 1L;
		private boolean broken;
		private SeismicDataSource source;
		
		public ServerNode(SeismicDataSource sds)
		{
			source = sds;
		}
		
		public void setBroken(boolean b)
		{
			broken = b;
		}
		
		public Icon getIcon()
		{
			return broken ? Images.getIcon("broken_server") : Images.getIcon("server");
		}
		
		public String getLabel()
		{
			return source.getName();
		}
		
		public SeismicDataSource getSource()
		{
			return source;
		}
	}
	
	private class ChannelNode extends ChooserNode
	{
		private static final long serialVersionUID = 1L;
		private String channel;
		
		public ChannelNode(String c)
		{
			channel = c;
		}
		
		public Icon getIcon()
		{
			return Images.getIcon("bullet");
		}
		
		public String getLabel()
		{
			return channel;
		}
		
		public String getChannel()
		{
			return channel;
		}
	}
	
	private class MessageNode extends ChooserNode
	{
		private static final long serialVersionUID = 1L;
		private String message;
		
		public MessageNode(String m)
		{
			message = m;
		}
		
		public Icon getIcon()
		{
			return Images.getIcon("warning");
		}
		
		public String getLabel()
		{
			return message;
		}
	}
	
	private class GroupNode extends ChooserNode
	{
		private static final long serialVersionUID = 1L;
		private String name;
		
		public GroupNode(String n)
		{
			name = n;
		}
		
		public Icon getIcon()
		{
			return Images.getIcon("wave_folder");
		}
		
		public String getLabel()
		{
			return name;
		}
	}
	
	private class CellRenderer2 extends DefaultTreeCellRenderer
	{
		private static final long serialVersionUID = 1L;

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean exp, boolean leaf, int row, boolean focus)
		{
			ChooserNode node = (ChooserNode)value; 
			Icon icon = node.getIcon();
			super.getTreeCellRendererComponent(tree, node.getLabel(), sel, exp, leaf, row, focus);
			setIcon(icon);
			return this;
		}
	}
	
}
