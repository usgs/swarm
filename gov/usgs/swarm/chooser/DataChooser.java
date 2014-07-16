package gov.usgs.swarm.chooser;

import gov.usgs.proj.GeoRange;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Messages;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmConfig;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.data.DataSourceType;
import gov.usgs.swarm.data.FileDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.data.SeismicDataSourceListener;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Pair;
import gov.usgs.util.Time;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
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
 * TODO: tooltip over data source describes data source TODO: refresh data
 * source TODO: confirm box on remove source
 * 
 * @author Dan Cervelli
 */
public class DataChooser extends JPanel {
    public static final int NO_CHANNEL_LIST = -2;
    public static final int NO_DATA_SOURCE = -1;
    public static final int OK = 0;

    private static final long serialVersionUID = 1L;
    private static final String OPENING_MESSAGE = Messages.getString("DataChooser.treeOpening"); //$NON-NLS-1$

    private static final String[] TIME_VALUES = new String[] { "Now" }; // "Today (Local)",
                                                                        // "Today (UTC)",
    // "Yesterday (Local)", "Yesterday (UTC)" };

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

    private JComboBox timeBox;
    private JButton heliButton;
    private JButton clipboardButton;
    private JButton monitorButton;
    private JButton realtimeButton;
    private JButton mapButton;

    private Map<String, TreePath> nearestPaths;

    private Set<String> openedSources;

    private ServerNode filesNode;
    private boolean filesNodeInTree = false;

    private DefaultTreeModel model;

    public DataChooser() {
        super(new BorderLayout());

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

        addServers(SwarmConfig.getInstance().sources);
    }

    private class FileSourceListener implements SeismicDataSourceListener {
        Map<String, ProgressNode> progressNodes;

        public FileSourceListener() {
            progressNodes = new HashMap<String, ProgressNode>();
        }

        public synchronized void channelsUpdated() {
            List<String> ch = filesNode.getSource().getChannels();
            if (ch == null && filesNodeInTree) {
                filesNode.removeAllChildren();
                removeServer(filesNode);
                filesNodeInTree = false;
            } else if (ch != null) {
                if (!filesNodeInTree) {
                    model.insertNodeInto(filesNode, rootNode, 0);
                }

                populateServer(filesNode, ch, true, true);

                filesNodeInTree = true;
            }
        }

        public synchronized void channelsProgress(String id, double p) {
            ProgressNode pn = progressNodes.get(id);
            boolean ins = false;
            if (pn == null) {
                pn = new ProgressNode();
                progressNodes.put(id, pn);
                ins = true;
            }
            if (!filesNodeInTree) {
                model.insertNodeInto(filesNode, rootNode, 0);
                filesNodeInTree = true;
                ins = true;
            }
            pn.setProgress(p);
            if (ins) {
                model.insertNodeInto(pn, filesNode, 0);
                dataTree.expandPath(new TreePath(filesNode.getPath()));
            }
            if (p == 1) {
                progressNodes.remove(id);
                filesNode.remove(pn);
            }
            dataTree.repaint();
        }

        public void helicorderProgress(String channel, double progress) {
        }
    }

    public void saveLayout(ConfigFile cf, String prefix) {
        for (String src : openedSources)
            cf.put(prefix + ".source", src);
    }

    public void processLayout(ConfigFile cf, ActionListener listener) {
        List<String> srcs = cf.getList("source");
        for (String src : srcs) {
            if (!isSourceOpened(src)) {
                ServerNode node = getServerNode(src);
                if (node == null)
                    listener.actionPerformed(new ActionEvent(this, NO_DATA_SOURCE, src));
                else
                    dataSourceSelected(node, listener);
            } else
                listener.actionPerformed(new ActionEvent(this, OK, src));
        }
    }

    private void createToolBar() {
        toolBar = SwarmUtil.createToolBar();

        newButton = SwarmUtil.createToolBarButton(Icons.new_server, //$NON-NLS-1$
                Messages.getString("DataChooser.newSourceToolTip"), //$NON-NLS-1$
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                EditDataSourceDialog d = new EditDataSourceDialog(null);
                                d.setVisible(true);
                                String nds = d.getResult();
                                if (nds != null) {
                                    SeismicDataSource source = DataSourceType.parseConfig(nds);
                                    if (source != null)
                                        insertServer(source);
                                }
                            }
                        });
                    }
                });
        toolBar.add(newButton);

        editButton = SwarmUtil.createToolBarButton(Icons.edit_server, //$NON-NLS-1$
                Messages.getString("DataChooser.editSourceToolTip"), //$NON-NLS-1$
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        List<ServerNode> servers = getSelectedServers();
                        if (servers != null && servers.size() > 0) {
                            SeismicDataSource sds = servers.get(0).getSource();
                            if (sds.isStoreInUserConfig()) {
                                String selected = servers.get(0).getSource().toConfigString();
                                EditDataSourceDialog d = new EditDataSourceDialog(selected);
                                d.setVisible(true);
                                String eds = d.getResult();
                                if (eds != null) {
                                    SeismicDataSource newSource = DataSourceType.parseConfig(eds);

                                    if (newSource == null)
                                        return;
                                    removeServer(servers.get(0));
                                    String svn = eds.substring(0, eds.indexOf(";"));
                                    SwarmConfig.getInstance().removeSource(svn);
                                    SwarmConfig.getInstance().addSource(newSource);
                                    insertServer(newSource);
                                }
                            }
                        }
                    }
                });
        toolBar.add(editButton);

        collapseButton = SwarmUtil.createToolBarButton(Icons.collapse, //$NON-NLS-1$
                Messages.getString("DataChooser.collapseToolTip"), //$NON-NLS-1$		
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        collapseTree(dataTree);
                    }
                });
        toolBar.add(collapseButton);

        deleteButton = SwarmUtil.createToolBarButton(Icons.new_delete, //$NON-NLS-1$
                Messages.getString("DataChooser.removeSourceToolTip"), //$NON-NLS-1$
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        List<ServerNode> servers = getSelectedServers();
                        if (servers != null) {
                            for (ServerNode server : servers) {
                                if (server.getSource().isStoreInUserConfig())
                                    removeServer(server);
                            }
                        }
                    }
                });
        toolBar.add(deleteButton);

        toolBar.add(Box.createHorizontalGlue());

        closeButton = SwarmUtil.createToolBarButton(Icons.close_view, "Close data chooser", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Swarm.getApplication().setChooserVisible(false);
                closeButton.getModel().setRollover(false);
            }
        });
        toolBar.add(closeButton);

        this.add(toolBar, BorderLayout.NORTH);
    }

    private void addTimeToBox(String t) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) timeBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(t)) {
                model.removeElementAt(i);
                break;
            }
        }
        model.insertElementAt(t, 1);
        timeBox.setSelectedIndex(1);
    }

    public String[] getUserTimes() {
        DefaultComboBoxModel model = (DefaultComboBoxModel) timeBox.getModel();
        String[] result = new String[model.getSize() - 1];
        for (int i = 1; i < model.getSize(); i++)
            result[i - 1] = (String) model.getElementAt(i);
        return result;
    }

    private double getTime() {
        double j2k = Double.NaN;
        String t0 = ((JTextField) timeBox.getEditor().getEditorComponent()).getText();
        if (!t0.equals(TIME_VALUES[0])) {
            String t = t0;
            // custom time
            if (t.length() == 8)
                t += "2359";

            try {
                j2k = Time.parseEx("yyyyMMddHHmm", t);
                addTimeToBox(t0);
            } catch (ParseException e) {
                String message = "Invalid time; legal format is 'YYYYMMDD' or 'YYYYMMDDhhmm', using 'Now' instead.";
                JOptionPane.showMessageDialog(Swarm.getApplication(), message, "Time Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return j2k;
    }

    private void createActionBar() {
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        JPanel actionPanel = new JPanel(new GridLayout(1, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        heliButton = new JButton(Icons.heli); //$NON-NLS-1$
        heliButton.setFocusable(false);
        heliButton.setToolTipText(Messages.getString("DataChooser.heliButtonToolTip")); //$NON-NLS-1$
        heliButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        List<Pair<ServerNode, String>> channels = getSelections();
                        if (channels != null) {
                            double j2k = getTime();
                            for (Pair<ServerNode, String> pair : channels) {
                                Swarm.getApplication().openHelicorder(pair.item1.getSource(), pair.item2, j2k);
                            }
                        }
                        return null;
                    }
                };
                worker.start();
            }
        });

        realtimeButton = new JButton(Icons.wave); //$NON-NLS-1$
        realtimeButton.setFocusable(false);
        realtimeButton.setToolTipText(Messages.getString("DataChooser.waveButtonToolTip")); //$NON-NLS-1$
        realtimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        List<Pair<ServerNode, String>> channels = getSelections();
                        if (channels != null) {
                            for (Pair<ServerNode, String> pair : channels) {
                                Swarm.getApplication().openRealtimeWave(pair.item1.getSource(), pair.item2);
                            }
                        }
                        return null;
                    }
                };
                worker.start();
            }
        });

        clipboardButton = new JButton(Icons.clipboard); //$NON-NLS-1$
        clipboardButton.setFocusable(false);
        clipboardButton.setToolTipText(Messages.getString("DataChooser.clipboardButtonToolTip")); //$NON-NLS-1$
        clipboardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        List<Pair<ServerNode, String>> channels = getSelections();
                        Collections.sort(channels, new Comparator<Pair<ServerNode, String>>() {
                            public int compare(Pair<ServerNode, String> o1, Pair<ServerNode, String> o2) {
                                return o1.item2.compareTo(o2.item2);
                            }
                        });
                        if (channels != null) {
                            for (Pair<ServerNode, String> pair : channels) {
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

        monitorButton = new JButton(Icons.monitor); //$NON-NLS-1$
        monitorButton.setFocusable(false);
        monitorButton.setToolTipText(Messages.getString("DataChooser.monitorButtonToolTip")); //$NON-NLS-1$
        monitorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        List<Pair<ServerNode, String>> channels = getSelections();
                        if (channels != null) {
                            for (Pair<ServerNode, String> pair : channels) {
                                Swarm.getApplication().monitorChannelSelected(pair.item1.getSource(), pair.item2);
                            }
                        }
                        return null;
                    }
                };
                worker.start();
            }
        });

        mapButton = new JButton(Icons.earth);
        mapButton.setFocusable(false);
        mapButton.setToolTipText("Open map interface");
        mapButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        List<Pair<ServerNode, String>> channels = getSelections();
                        GeoRange gr = new GeoRange();
                        int nc = 0;
                        for (Pair<ServerNode, String> pair : channels) {
                            Metadata md = SwarmConfig.getInstance().getMetadata(pair.item2);
                            Point2D.Double pt = md.getLonLat();
                            if (pt != null && !Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                                nc++;
                                gr.includePoint(pt, 0.0001);
                            }
                        }
                        if (nc == 1)
                            gr.pad(0.1275, 0.1275);
                        else
                            gr.padPercent(1.2, 1.2);
                        if (gr.isValid()) {
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

        JPanel timePanel = new JPanel(new BorderLayout());
        timeBox = new JComboBox(TIME_VALUES);
        for (String ut : SwarmConfig.getInstance().userTimes) {
            if (ut.length() > 0)
                timeBox.addItem(ut);
        }
        timeBox.setEditable(true);
        timeBox.getEditor().getEditorComponent().addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                timeBox.getEditor().selectAll();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        timePanel.add(new JLabel("Open to: "), BorderLayout.WEST);
        timePanel.add(timeBox, BorderLayout.CENTER);
        bottomPanel.add(timePanel);
        bottomPanel.add(actionPanel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public int getDividerLocation() {
        return split.getDividerLocation();
    }

    public void setDividerLocation(int dl) {
        split.setDividerLocation(dl);
    }

    private ServerNode getServerNode(String svr) {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            ServerNode node = (ServerNode) rootNode.getChildAt(i);
            if (node.getSource().getName().equals(svr)) {
                return node;
            }
        }
        return null;
    }

    private List<ServerNode> getSelectedServers() {
        TreePath[] paths = dataTree.getSelectionPaths();
        List<ServerNode> servers = new ArrayList<ServerNode>();
        if (paths != null) {
            for (TreePath path : paths) {
                if (path.getPathCount() == 2) {
                    ServerNode node = (ServerNode) path.getLastPathComponent();
                    servers.add(node);
                }
            }
        }
        return servers;
    }

    private List<Pair<ServerNode, String>> getSelections() {
        TreePath[] paths = dataTree.getSelectionPaths();
        return getSelectedLeaves(paths);
    }

    class MakeVisibileTSL implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            if (e.isAddedPath()) {
                TreePath[] paths = e.getPaths();
                ((JTree) e.getSource()).scrollPathToVisible(paths[0]);
            }
        }
    }

    private void collapseTree(JTree tree) {
        model.reload();
    }

    private boolean isOpened(ChooserNode node) {
        ChooserNode child = (ChooserNode) node.getChildAt(0);
        if (!(child instanceof MessageNode))
            return true;
        if (((MessageNode) child).getLabel().equals(OPENING_MESSAGE))
            return false;
        return true;
    }

    private class ExpansionListener implements TreeExpansionListener {
        public void treeExpanded(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            if (path.getPathCount() == 2) {
                ServerNode node = (ServerNode) path.getLastPathComponent();
                if (!isOpened(node))
                    dataSourceSelected(node, null);
            }
        }

        public void treeCollapsed(TreeExpansionEvent event) {
        }
    }

    public boolean isSourceOpened(String src) {
        return openedSources.contains(src);
    }

    private List<String> openSource(SeismicDataSource sds) {
        List<String> channels = null;
        try {
            sds.establish();
            channels = sds.getChannels();
            Swarm.getApplication().getMapFrame().reset(false);
            sds.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return channels;
    }

    private class DataSourceOpener extends SwingWorker {
        private List<String> channels;
        private ServerNode source;
        private ActionListener finishListener;

        public DataSourceOpener(ServerNode src, ActionListener fl) {
            source = src;
            finishListener = fl;
        }

        private SeismicDataSourceListener listener = new SeismicDataSourceListener() {
            public void channelsProgress(String id, final double progress) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) source.getFirstChild();
                        if (node instanceof MessageNode) {
                            model.removeNodeFromParent(node);
                            ProgressNode pn = new ProgressNode();
                            source.insert(pn, 0);
                            model.insertNodeInto(pn, source, 0);
                            dataTree.expandPath(new TreePath(source.getPath()));
                            dataTree.repaint();
                        } else if (node instanceof ProgressNode) {
                            ProgressNode pn = (ProgressNode) node;
                            pn.setProgress(progress);
                            dataTree.repaint();
                        }
                    }
                });
            }

            public void channelsUpdated() {
            }

            public void helicorderProgress(String channel, double progress) {
            }
        };

        public Object construct() {
            SeismicDataSource sds = source.getSource();
            sds.addListener(listener);
            channels = openSource(sds);
            return null;
        }

        public void finished() {
            int id = OK;
            if (channels != null) {
                source.setBroken(false);
                model.reload(source);
                populateServer(source, channels, false, false);
                id = OK;
                openedSources.add(source.getSource().getName());
            } else {
                source.setBroken(true);
                model.reload(source);
                dataTree.collapsePath(new TreePath(source.getPath()));
                id = NO_CHANNEL_LIST;
            }
            source.getSource().removeListener(listener);
            if (finishListener != null)
                finishListener.actionPerformed(new ActionEvent(DataChooser.this, id, source.getSource().getName()));
        }
    }

    private void dataSourceSelected(final ServerNode source, ActionListener listener) {
        DataSourceOpener opener = new DataSourceOpener(source, listener);
        opener.start();
    }

    public void removeServer(final ServerNode node) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwarmConfig.getInstance().removeSource(node.getSource().getName());
                model.removeNodeFromParent(node);
            }
        });
    }

    public void insertServer(final SeismicDataSource source) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwarmConfig.getInstance().addSource(source);

                String ns = source.getName();
                int i = 0;
                for (i = 0; i < rootNode.getChildCount(); i++) {
                    String s = ((ServerNode) rootNode.getChildAt(i)).getSource().getName();
                    if (ns.compareToIgnoreCase(s) <= 0)
                        break;
                }

                ServerNode node = new ServerNode(source);
                node.add(new MessageNode(OPENING_MESSAGE));
                model.insertNodeInto(node, rootNode, i);
                model.reload();
            }
        });
    }

    public void addServers(final Map<String, SeismicDataSource> servers) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                List<String> list = Collections.list(Collections.enumeration(servers.keySet()));
                Collections.sort(list, Util.getIgnoreCaseStringComparator());
                for (String key : list) {
                    SeismicDataSource sds = servers.get(key);
                    ServerNode node = new ServerNode(sds);
                    node.add(new MessageNode(OPENING_MESSAGE));
                    rootNode.add(node);
                }
                model.reload();
            }
        });
    }

    private void createTree() {
        rootNode = new RootNode(); //$NON-NLS-1$
        dataTree = new JTree(rootNode);
        dataTree.setRootVisible(false);
        dataTree.setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 0));

        model = new DefaultTreeModel(rootNode);
        dataTree.setModel(model);

        treeScrollPane = new JScrollPane(dataTree);

        dataTree.addTreeSelectionListener(new MakeVisibileTSL());
        dataTree.addTreeExpansionListener(new ExpansionListener());
        dataTree.setCellRenderer(new CellRenderer());

        dataTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    TreePath path = dataTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node instanceof ChannelNode) {
                            ChannelNode cn = (ChannelNode) node;
                            setNearest(cn.getChannel());
                        }
                    }
                }
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = dataTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node != null && node.isLeaf()) {
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

        dataTree.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == 0x0a)
                    heliButton.doClick();
            }
        });

    }

    public void setNearest(final String channel) {
        if (channel == null || channel.equals(lastNearest))
            return;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                List<Pair<Double, String>> nrst = Metadata
                        .findNearest(SwarmConfig.getInstance().getMetadata(), channel);
                if (nrst == null)
                    return;
                lastNearest = channel;
                nearestLabel.setText("Distance to " + channel);
                DefaultListModel model = (DefaultListModel) nearestList.getModel();
                model.removeAllElements();
                for (Pair<Double, String> item : nrst)
                    model.addElement(String.format("%s (%.1f km)", item.item2, item.item1 / 1000));
            }
        });
    }

    private void createNearest() {
        nearestList = new JList(new DefaultListModel());
        nearestScrollPane = new JScrollPane(nearestList);
        nearestPanel = new JPanel(new BorderLayout());
        nearestPanel.add(nearestScrollPane, BorderLayout.CENTER);
        nearestLabel = new JLabel("Distance");
        nearestPanel.add(nearestLabel, BorderLayout.NORTH);
        nearestList.setCellRenderer(new ListCellRenderer());

        nearestList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Object[] sels = nearestList.getSelectedValues();
                    if (sels.length > 0)
                        dataTree.clearSelection();
                    for (Object o : sels) {
                        String ch = (String) o;
                        ch = ch.substring(0, ch.indexOf("(")).trim();
                        TreePath tp = nearestPaths.get(ch);
                        dataTree.addSelectionPath(tp);
                    }
                }
            }
        });

    }

    /**
     * @param node
     * @param channels
     */
    private void populateServer(final ServerNode node, final List<String> channels, final boolean expandAll,
            final boolean saveProgress) {
        if (channels == null)
            return;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TreeMap<String, GroupNode> rootMap = new TreeMap<String, GroupNode>();
                HashMap<String, GroupNode> groupMap = new HashMap<String, GroupNode>();
                HashSet<GroupNode> openGroups = new HashSet<GroupNode>();

                GroupNode allNode = new GroupNode(Messages.getString("DataChooser.allGroup")); //$NON-NLS-1$
                ChooserNode rootNode = node;
                if (!saveProgress)
                    rootNode.removeAllChildren();
                else {
                    for (int i = 0; i < rootNode.getChildCount(); i++) {
                        if (!(rootNode.getChildAt(i) instanceof ProgressNode)) {
                            rootNode.remove(i);
                            i--;
                        }
                    }
                }
                rootNode.add(allNode);
                for (String channel : channels) {
                    ChannelNode newNode = new ChannelNode(channel);
                    allNode.add(newNode);

                    Metadata md = SwarmConfig.getInstance().getMetadata(channel);
                    if (md != null && md.getGroups() != null) {
                        Set<String> groups = md.getGroups();
                        for (String g : groups) {
                            boolean forceOpen = false;
                            String[] ss = g.split("\\^");
                            if (ss[0].endsWith("!")) {
                                ss[0] = ss[0].substring(0, ss[0].length() - 1);
                                forceOpen = true;
                            }
                            GroupNode gn = rootMap.get(ss[0]);
                            if (gn == null) {
                                gn = new GroupNode(ss[0]);
                                rootMap.put(ss[0], gn);
                            }
                            if (forceOpen)
                                openGroups.add(gn);
                            GroupNode cn = gn;
                            String cs = ss[0];
                            for (int i = 1; i < ss.length; i++) {
                                boolean fo = false;
                                if (ss[i].endsWith("!")) {
                                    ss[i] = ss[i].substring(0, ss[i].length() - 1);
                                    fo = true;
                                }
                                cs += "^" + ss[i];
                                GroupNode nn = groupMap.get(cs);
                                if (nn == null) {
                                    nn = new GroupNode(ss[i]);
                                    groupMap.put(cs, nn);
                                    int j = 0;
                                    for (j = 0; j < cn.getChildCount(); j++) {
                                        if (cn.getChildAt(j) instanceof GroupNode) {
                                            GroupNode ogn = (GroupNode) cn.getChildAt(j);
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
                    nearestPaths.put(channel, new TreePath(newNode.getPath()));
                }

                for (String key : rootMap.keySet()) {
                    GroupNode n = rootMap.get(key);
                    rootNode.add(n);
                }

                model.reload(rootNode);

                for (GroupNode gn : openGroups) {
                    dataTree.expandPath(new TreePath(gn.getPath()));
                }
                if (expandAll) {
                    dataTree.expandPath(new TreePath(allNode.getPath()));
                }
                nearestList.repaint();
            }
        });
    }

    private Set<String> getGroupChannels(GroupNode gn) {
        HashSet<String> channels = new HashSet<String>();
        for (Enumeration<?> e = gn.children(); e.hasMoreElements();) {
            ChooserNode n = (ChooserNode) e.nextElement();
            if (n instanceof ChannelNode)
                channels.add(((ChannelNode) n).channel);
            else if (n instanceof GroupNode)
                channels.addAll(getGroupChannels((GroupNode) n));
        }

        return channels;
    }

    private List<Pair<ServerNode, String>> getSelectedLeaves(TreePath[] paths) {
        if (paths == null)
            return null;

        boolean countExceeded = false;
        List<Pair<ServerNode, String>> selections = new ArrayList<Pair<ServerNode, String>>();
        for (int i = 0; i < paths.length; i++) {
            TreePath path = paths[i];
            if (path.getPathCount() <= 2)
                continue;

            ServerNode serverNode = (ServerNode) path.getPathComponent(1);

            ChooserNode node = (ChooserNode) path.getLastPathComponent();
            if (node.isLeaf() && node instanceof ChannelNode) {
                selections.add(new Pair<ServerNode, String>(serverNode, ((ChannelNode) node).channel));
            } else if (!node.isLeaf()) {
                Set<String> channels = getGroupChannels((GroupNode) node);
                for (String ch : channels)
                    selections.add(new Pair<ServerNode, String>(serverNode, ch));
            }

        }

        if (countExceeded) {
            JOptionPane.showMessageDialog(Swarm.getApplication(),
                    Messages.getString("DataChooser.maxChannelsAtOnceError") + MAX_CHANNELS_AT_ONCE, //$NON-NLS-1$ 
                    Messages.getString("DataChooser.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
        }

        return selections;
    }

    abstract private class ChooserNode extends DefaultMutableTreeNode {
        private static final long serialVersionUID = 1L;

        abstract public Icon getIcon();

        abstract public String getLabel();

        public String toString() {
            return "chooserNode";
        }
    }

    private class RootNode extends ChooserNode {
        private static final long serialVersionUID = 1L;

        public Icon getIcon() {
            return null;
        }

        public String getLabel() {
            return null;
        }
    }

    private class ServerNode extends ChooserNode {
        private static final long serialVersionUID = 1L;
        private boolean broken;
        private SeismicDataSource source;

        public ServerNode(SeismicDataSource sds) {
            source = sds;
        }

        public void setBroken(boolean b) {
            broken = b;
        }

        public Icon getIcon() {
            if (source instanceof FileDataSource) {
                return Icons.wave_folder;
            } else {
                if (!broken) {
                    if (source.isStoreInUserConfig())
                        return Icons.server;
                    else
                        return Icons.locked_server;
                } else {
                    if (source.isStoreInUserConfig())
                        return Icons.broken_server;
                    else
                        return Icons.broken_locked_server;
                }
            }
        }

        public String getLabel() {
            return source.getName();
        }

        public SeismicDataSource getSource() {
            return source;
        }
    }

    private class ChannelNode extends ChooserNode {
        private static final long serialVersionUID = 1L;
        private String channel;

        public ChannelNode(String c) {
            channel = c;
            setToolTipText(channel + "<br>2nd line: value");
        }

        public Icon getIcon() {
            Metadata md = SwarmConfig.getInstance().getMetadata(channel);
            if (md == null || !md.isTouched())
                return Icons.graybullet;
            else if (md.hasLonLat())
                return Icons.bluebullet;
            else
                return Icons.bullet;
        }

        public String getLabel() {
            return channel;
        }

        public String getChannel() {
            return channel;
        }
    }

    private class MessageNode extends ChooserNode {
        private static final long serialVersionUID = 1L;
        private String message;

        public MessageNode(String m) {
            message = m;
        }

        public Icon getIcon() {
            return Icons.warning;
        }

        public String getLabel() {
            return message;
        }

        public String toString() {
            return message;
        }
    }

    private class GroupNode extends ChooserNode {
        private static final long serialVersionUID = 1L;
        private String name;

        public GroupNode(String n) {
            name = n;
        }

        public Icon getIcon() {
            return Icons.wave_folder;
        }

        public String getLabel() {
            return name;
        }
    }

    private class ProgressNode extends ChooserNode {
        private static final long serialVersionUID = 1L;
        private JProgressBar progressBar;

        public ProgressNode() {
            progressBar = new JProgressBar(0, 100);
            progressBar.setPreferredSize(new Dimension(80, 10));
        }

        public Icon getIcon() {
            return Icons.warning;
        }

        public void setProgress(double p) {
            progressBar.setValue((int) Math.round(p * 100));
        }

        public JProgressBar getProgressBar() {
            return progressBar;
        }

        public String getLabel() {
            return "progress";
        }
    }

    private class ListCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean hasFocus) {
            String ch = (String) value;
            ch = ch.substring(0, ch.indexOf("(")).trim();
            Icon icon = nearestPaths.containsKey(ch) ? Icons.bullet : Icons.redbullet;
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            setIcon(icon);
            return this;
        }
    }

    private class CellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf,
                int row, boolean focus) {
            if (value instanceof ProgressNode) {
                ProgressNode node = (ProgressNode) value;
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                panel.setOpaque(false);
                panel.setBorder(null);
                panel.setBackground(Color.WHITE);
                panel.add(new JLabel(node.getIcon()));
                panel.add(node.getProgressBar());
                return panel;
            } else if (value instanceof ChooserNode) {
                ChooserNode node = (ChooserNode) value;
                Icon icon = node.getIcon();
                super.getTreeCellRendererComponent(tree, node.getLabel(), sel, exp, leaf, row, focus);
                setIcon(icon);
                return this;
            } else {
                super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);
                return this;
            }
        }
    }
}
