package gov.usgs.swarm.chooser;

import gov.usgs.proj.GeoRange;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Messages;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.data.FileDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.data.SeismicDataSourceListener;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Pair;
import gov.usgs.util.Util;

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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
 * TODO: tooltip over data source describes data source
 * TODO: refresh data source
 * TODO: error box on failed open
 * TODO: confirm box on remove source
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2006/08/08 22:20:12  cervelli
 * Fixes -180/180 map bug.
 *
 * Revision 1.4  2006/08/07 22:35:10  cervelli
 * File source.
 *
 * Revision 1.3  2006/08/05 22:22:33  cervelli
 * New group system.
 *
 * Revision 1.2  2006/08/04 18:37:40  cervelli
 * New group system and single click to change nearest dialog.
 *
 * Revision 1.1  2006/08/01 23:42:54  cervelli
 * Moved package and changes for layouts.
 *
 * Revision 1.9  2006/07/30 16:14:35  cervelli
 * New icons for locked sources.
 *
 * Revision 1.8  2006/07/28 14:49:32  cervelli
 * Map button makes sure that the map frame is visible.
 *
 * Revision 1.7  2006/07/26 22:39:31  cervelli
 * Now sorts case insensitive.
 *
 * Revision 1.6  2006/07/23 04:33:31  cervelli
 * Changed map icon, fixed bug on new data source.
 *
 * Revision 1.5  2006/07/22 20:22:30  cervelli
 * Many changes for map integration.
 *
 * Revision 1.4  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.3  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
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
	private JScrollPane treeScrollPane;
	private JLabel nearestLabel;
	private JList nearestList;
	private JScrollPane nearestScrollPane;
	private JSplitPane split;
	private JPanel nearestPanel;
	private String lastNearest;
	
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
	private JButton mapButton;
	
	private Map<String, TreePath> nearestPaths;
	
	private Set<String> openedSources;
	
	private ServerNode filesNode;
	private boolean filesNodeInTree = false;
	
	public DataChooser()
	{
		super(new BorderLayout());

		filesNode = new ServerNode(Swarm.getFileSource());
		filesNode.getSource().addListener(new FileSourceListener());
		
		nearestPaths = new HashMap<String, TreePath>();
		openedSources = new HashSet<String>();
		
		createToolBar();
		createTree();
		createNearest();
		split = SwarmUtil.createStrippedSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, nearestPanel);
		split.setDividerSize(4);
		add(split, BorderLayout.CENTER);
		createActionBar();

		setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		
		addServers(Swarm.config.sources);
	}
	
	private class FileSourceListener implements SeismicDataSourceListener
	{
		public void channelsUpdated()
		{
			List<String> ch = filesNode.getSource().getChannels();
			if (ch == null && filesNodeInTree)
			{
				removeServer(filesNode);
				filesNodeInTree = false;
			}
			else if (ch != null)
			{
				if (!filesNodeInTree)
					rootNode.insert(filesNode, 0);
				
				((DefaultTreeModel)dataTree.getModel()).reload();
				populateServer(filesNode, ch, true);
				
//				dataTree.expandPath(new TreePath(filesNode.getFirstChild()));
				
				filesNodeInTree = true;	
			}
		}
		
		public void channelsProgress(double p)
		{}
	}
	
	public void saveLayout(ConfigFile cf, String prefix)
	{
		for (String src : openedSources)
			cf.put(prefix + ".source", src);
	}
	
	public void processLayout(ConfigFile cf)
	{
		List<String> srcs = cf.getList("source");
		for (String src : srcs)
		{
//			System.out.println(src);
			if (!isSourceOpened(src))
			{
				List<String> chs = openSource(Swarm.config.getSource(src));
				if (chs == null)
				{
					JOptionPane.showMessageDialog(Swarm.getApplication(), 
							"The data source '" + src + "' could not be opened.", 
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
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
						List<ServerNode> servers = getSelectedServers();
						if (servers != null && servers.size() > 0)
						{
							SeismicDataSource sds = servers.get(0).getSource();
							if (sds.isStoreInUserConfig())
							{
								String selected = servers.get(0).getSource().toConfigString();
//								EditDataSourceDialog d = new EditDataSourceDialog(selected);
								EditDataSourceDialog d = new EditDataSourceDialog(selected);
								d.setVisible(true);
								String eds = d.getResult();
								if (eds != null)
								{
									SeismicDataSource newSource = SeismicDataSource.getDataSource(eds);
									if (newSource == null)
										return;
									removeServer(servers.get(0));
									String svn = eds.substring(0, eds.indexOf(";"));
									Swarm.config.removeSource(svn);
									Swarm.config.addSource(newSource);
									insertServer(newSource);
								}
							}
						}
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
							{
								if (server.getSource().isStoreInUserConfig())
									removeServer(server);
							}
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
		heliButton.setFocusable(false);
		heliButton.setToolTipText(Messages.getString("DataChooser.heliButtonToolTip")); //$NON-NLS-1$
		heliButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<Pair<ServerNode, String>> channels = getSelections();
										if (channels != null)
										{
//											Pair<ServerNode, ChannelNode> ch1 = channels.get(0);
//											if (ch1 != null)
//												setNearest(ch1.item2.getChannel());
											for (Pair<ServerNode, String> pair : channels)
											{
												Swarm.getApplication().openHelicorder(pair.item1.getSource(), pair.item2);
											}
										}
										return null;
									}
								};
						worker.start();
					}
				});
		
		realtimeButton = new JButton(Images.getIcon("wave")); //$NON-NLS-1$
		realtimeButton.setFocusable(false);
		realtimeButton.setToolTipText(Messages.getString("DataChooser.waveButtonToolTip")); //$NON-NLS-1$
		realtimeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<Pair<ServerNode, String>> channels = getSelections();
										if (channels != null)
										{
											for (Pair<ServerNode, String> pair : channels)
											{
												Swarm.getApplication().openRealtimeWave(pair.item1.getSource(), pair.item2);
											}
										}
										return null;
									}
								};
						worker.start();
					}
				});
		
		clipboardButton = new JButton(Images.getIcon("clipboard")); //$NON-NLS-1$
		clipboardButton.setFocusable(false);
		clipboardButton.setToolTipText(Messages.getString("DataChooser.clipboardButtonToolTip")); //$NON-NLS-1$
		clipboardButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
								{
									public Object construct()
									{
										List<Pair<ServerNode, String>> channels = getSelections();
										if (channels != null)
										{
											for (Pair<ServerNode, String> pair : channels)
											{
												Swarm.getApplication().loadClipboardWave(pair.item1.getSource(), pair.item2);
											}
										}
										Swarm.getApplication().getWaveClipboard().requestFocusInWindow();
										return null;
									}
								};
						worker.start();
					}
				});
		
		monitorButton = new JButton(Images.getIcon("monitor")); //$NON-NLS-1$
		monitorButton.setFocusable(false);
		monitorButton.setToolTipText(Messages.getString("DataChooser.monitorButtonToolTip")); //$NON-NLS-1$
		monitorButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
						{
							public Object construct()
							{
								List<Pair<ServerNode, String>> channels = getSelections();
								if (channels != null)
								{
									for (Pair<ServerNode, String> pair : channels)
									{
										Swarm.getApplication().monitorChannelSelected(pair.item1.getSource(), pair.item2);
									}
								}
								return null;
							}
						};
						worker.start();
					}
				});
		
		mapButton = new JButton(Images.getIcon("earth"));
		mapButton.setFocusable(false);
		mapButton.setToolTipText("Open map interface"); 
		mapButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						SwingWorker worker = new SwingWorker()
						{
							public Object construct()
							{
								List<Pair<ServerNode, String>> channels = getSelections();
								GeoRange gr = new GeoRange();
								int nc = 0;
								for (Pair<ServerNode, String> pair : channels)
								{
									Metadata md = Swarm.config.getMetadata(pair.item2);
									Point2D.Double pt = md.getLonLat();
									if (pt != null && !Double.isNaN(pt.x) && !Double.isNaN(pt.y))
									{
										nc++;
										gr.includePoint(pt, 0.0001);
									}
								}
								if (nc == 1)
									gr.pad(0.1275, 0.1275);
								else
									gr.padPercent(0.4, 0.4);
								if (gr.isValid())
								{
//									System.out.println(gr);
									Swarm.getApplication().setMapVisible(true);
									Swarm.getApplication().getMapFrame().setView(gr);
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
		actionPanel.add(mapButton);
		
		add(actionPanel, BorderLayout.SOUTH);
	}
	
	public int getDividerLocation()
	{
		return split.getDividerLocation();
	}
	
	public void setDividerLocation(int dl)
	{
		split.setDividerLocation(dl);
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
	
	private List<Pair<ServerNode, String>> getSelections()
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
//				if (paths.length = 2)
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
	
	public boolean isSourceOpened(String src)
	{
		return openedSources.contains(src);
	}
	
	// TODO: use dataSourceSelected
	private List<String> openSource(SeismicDataSource sds)
	{
		List<String> channels = null;
		try
		{
			sds.establish();
			channels = sds.getChannels();
			Swarm.getApplication().getMapFrame().reset(false);
			sds.close();
			openedSources.add(sds.getName());
		} 
		catch (Exception e)
		{
//			e.printStackTrace();
		}	
		return channels;
	}
	
	private void dataSourceSelected(final ServerNode source)
	{
		final SwingWorker worker = new SwingWorker()
				{
					private List<String> channels;
					
					public Object construct()
					{
						SeismicDataSource sds = source.getSource();
						channels = openSource(sds);
						return null;	
					}			
					
					public void finished()
					{
						if (channels != null)
						{
							source.setBroken(false);
							((DefaultTreeModel)dataTree.getModel()).reload(source);
							populateServer(source, channels, false);
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
						((DefaultTreeModel)dataTree.getModel()).reload();
					}
				});
	}
	
	public void addServers(final Map<String, SeismicDataSource> servers)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						List<String> list = Collections.list(Collections.enumeration(servers.keySet()));
						Collections.sort(list, Util.getIgnoreCaseStringComparator());
						for (String key : list)
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
		
		treeScrollPane = new JScrollPane(dataTree);
		
		dataTree.addTreeSelectionListener(new MakeVisibileTSL());
		dataTree.addTreeExpansionListener(new ExpansionListener());
		dataTree.setCellRenderer(new CellRenderer());
		
		dataTree.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 1)
						{
							TreePath path = dataTree.getSelectionPath();
							if (path != null)
							{
								DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
								if (node instanceof ChannelNode)
								{
									ChannelNode cn = (ChannelNode)node;
									setNearest(cn.getChannel());
								}
							}
						}
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
		
	}

	public void setNearest(final String channel)
	{
		if (channel == null || channel.equals(lastNearest))
			return;
		
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						List<Pair<Double, String>> nrst = Metadata.findNearest(Swarm.config.getMetadata(), channel);
						if (nrst == null)
							return;
						lastNearest = channel;
						nearestLabel.setText("Nearest to " + channel);
						DefaultListModel model = (DefaultListModel)nearestList.getModel();
						model.removeAllElements();
						for (Pair<Double, String> item : nrst)
							model.addElement(String.format("%s (%.1f km)", item.item2, item.item1));
					}
				});
	}
	
	private void createNearest()
	{
		nearestList = new JList(new DefaultListModel());
		nearestScrollPane = new JScrollPane(nearestList);
		nearestPanel = new JPanel(new BorderLayout());
		nearestPanel.add(nearestScrollPane, BorderLayout.CENTER);
		nearestLabel = new JLabel("Nearest");
		nearestPanel.add(nearestLabel, BorderLayout.NORTH);
		nearestList.setCellRenderer(new ListCellRenderer());
		
		nearestList.addListSelectionListener(new ListSelectionListener()
				{
					public void valueChanged(ListSelectionEvent e)
					{
						if (!e.getValueIsAdjusting())
						{
							dataTree.clearSelection();
							Object[] sels = nearestList.getSelectedValues();
							for (Object o : sels)
							{
								String ch = (String)o;
								ch = ch.substring(0, ch.indexOf("(")).trim();
								TreePath tp = nearestPaths.get(ch);
								dataTree.addSelectionPath(tp);
							}
						}
//						dataTree.addSelectionPath(new TreePath());
//						System.out.println(e);
					}
				});
		
	}
	
	/**
	 * @param node
	 * @param channels
	 */
	private void populateServer(final ServerNode node, final List<String> channels, final boolean expandAll)
	{
		if (channels == null)
			return;
		
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						TreeMap<String, GroupNode> rootMap = new TreeMap<String, GroupNode>();
						HashMap<String, GroupNode> groupMap = new HashMap<String, GroupNode>();
						HashSet<GroupNode> openGroups = new HashSet<GroupNode>();
						
						GroupNode allNode = new GroupNode(Messages.getString("DataChooser.allGroup")); //$NON-NLS-1$
						ChooserNode rootNode = node;
						rootNode.removeAllChildren();
						rootNode.add(allNode);
						for (String channel : channels)
						{
							ChannelNode newNode = new ChannelNode(channel);
							allNode.add(newNode);
							
							Metadata md = Swarm.config.getMetadata(channel);
							if (md != null && md.getGroups() != null)
							{
								Set<String> groups = md.getGroups();
								for (String g : groups)
								{
									boolean forceOpen = false;
									String[] ss = g.split("\\^");
									if (ss[0].endsWith("!"))
									{
										ss[0] = ss[0].substring(0, ss[0].length() - 1);
										forceOpen = true;
									}
									GroupNode gn = rootMap.get(ss[0]);
									if (gn == null)
									{
										gn = new GroupNode(ss[0]);
										rootMap.put(ss[0], gn);
									}
									if (forceOpen)
										openGroups.add(gn);
									GroupNode cn = gn;
									String cs = ss[0];
									for (int i = 1; i < ss.length; i++)
									{
										boolean fo = false;
										if (ss[i].endsWith("!"))
										{
											ss[i] = ss[i].substring(0, ss[i].length() - 1);
											fo = true;
										}
										cs += "^" + ss[i];
										GroupNode nn = groupMap.get(cs);
										if (nn == null)
										{
											nn = new GroupNode(ss[i]);
											groupMap.put(cs, nn);
											int j = 0;
											for (j = 0; j < cn.getChildCount(); j++)
											{
												if (cn.getChildAt(j) instanceof GroupNode)
												{
													GroupNode ogn = (GroupNode)cn.getChildAt(j);
													if (nn.name.compareToIgnoreCase(ogn.name) <= 0)
														break;
												}
											}
											if (j >= cn.getChildCount())
												cn.add(nn);
											else
												cn.insert(nn, j);
										}
										if (fo)
											openGroups.add(nn);
										
										cn = nn;
									}
									ChannelNode ln = new ChannelNode(channel);
									cn.add(ln);
								}
							}
//							else
//							{
								nearestPaths.put(channel, new TreePath(newNode.getPath()));
//							}
						}
						
						for (String key : rootMap.keySet())
						{
							GroupNode n = rootMap.get(key);
							rootNode.add(n);
//							for (int i = 0; i < n.getChildCount(); i++)
//							{
//								ChannelNode cn = (ChannelNode)n.getChildAt(i);
//								nearestPaths.put(cn.getChannel(), new TreePath(cn.getPath()));
//							}
						}
						
						((DefaultTreeModel)dataTree.getModel()).reload(rootNode);
						
						for (GroupNode gn : openGroups)
						{
							dataTree.expandPath(new TreePath(gn.getPath()));
						}
						if (expandAll)
						{
							dataTree.expandPath(new TreePath(allNode.getPath()));
						}
						nearestList.repaint();
					}
				});	
	}
	
//	private Set<String> getSelectedLeaves(TreePath[] paths)
	private Set<String> getGroupChannels(GroupNode gn)
	{
		HashSet<String> channels = new HashSet<String>();
		for (Enumeration e = gn.children() ; e.hasMoreElements() ;) 
		{
			ChooserNode n = (ChooserNode)e.nextElement();
			if (n instanceof ChannelNode)
				channels.add(((ChannelNode)n).channel);
			else if (n instanceof GroupNode)
				channels.addAll(getGroupChannels((GroupNode)n));
		}
		
		return channels;
	}
	
	private List<Pair<ServerNode, String>> getSelectedLeaves(TreePath[] paths)
	{
		if (paths == null)
			return null;
			
		boolean countExceeded = false;
		List<Pair<ServerNode, String>> selections = new ArrayList<Pair<ServerNode, String>>();
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
				selections.add(new Pair<ServerNode, String>(serverNode, ((ChannelNode)node).channel));
			}
			else if (!node.isLeaf())
			{
				Set<String> channels = getGroupChannels((GroupNode)node);
				for (String ch : channels)
					selections.add(new Pair<ServerNode, String>(serverNode, ch));
			}
			
//			if (countExceeded)
//				break;
		}
		
		if (countExceeded)
		{
			JOptionPane.showMessageDialog(Swarm.getApplication(), 
					Messages.getString("DataChooser.maxChannelsAtOnceError") + MAX_CHANNELS_AT_ONCE, //$NON-NLS-1$ 
					Messages.getString("DataChooser.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		}
		
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
			if (source instanceof FileDataSource)
			{
				return Images.getIcon("wave_folder");
			}
			else
			{
				if (!broken)
				{
					if (source.isStoreInUserConfig())
						return Images.getIcon("server");
					else
						return Images.getIcon("locked_server");
				}
				else
				{
					if (source.isStoreInUserConfig())
						return Images.getIcon("broken_server");
					else
						return Images.getIcon("broken_locked_server");
				}
			}
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
	
	private class ListCellRenderer extends DefaultListCellRenderer
	{
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean hasFocus)
		{
			String ch = (String)value;
			ch = ch.substring(0, ch.indexOf("(")).trim();
			Icon icon = nearestPaths.containsKey(ch) ? Images.getIcon("bullet") : Images.getIcon("redbullet");
			super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
			setIcon(icon);
			return this;
		}
	}
	
	private class CellRenderer extends DefaultTreeCellRenderer
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
