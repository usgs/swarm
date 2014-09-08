package gov.usgs.swarm.database.view;

import gov.usgs.swarm.DataRecord;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmMenu;
import gov.usgs.swarm.database.managers.DataSearchManager;
import gov.usgs.swarm.database.model.Attempt;
import gov.usgs.swarm.database.model.Event;
import gov.usgs.swarm.database.model.HypoResults;
import gov.usgs.swarm.database.model.Marker;
import gov.usgs.swarm.database.model.ResultSetModel;
import gov.usgs.swarm.map.HypoOuputMapFrame;
import gov.usgs.swarm.map.MapPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Panel for the results of a search for events/attempts.
 * 
 * @author Chirag Patel
 */
public class DataSearchResultPanel extends JPanel {
    private JTable dataSearchResultTable;
    private boolean selectRow = false;
    private JButton showAttemptOnMap;
    private JButton showHypoOutput;
    private JButton deleteAttempt;
    private JButton editAttempt;
    private JButton createAttempt;
    public boolean isEventList = false;

    public DataSearchResultPanel() {
        super.setLayout(new BorderLayout());
        super.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Search result"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        this.initDataSearchResultTable();
        JScrollPane scrollPane = new JScrollPane(dataSearchResultTable);
        scrollPane.setBorder(null);
        // scrollPane.setViewportView(dataSearchResultTable);
        super.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanels = new JPanel();
        buttonPanels.add(createShowAttemptOnMapButton());
        buttonPanels.add(createDeleteButton());
        buttonPanels.add(createLoadButton());
        buttonPanels.add(createCreateButton());
        buttonPanels.add(createShowHypoButton());
        super.add(buttonPanels, BorderLayout.SOUTH);
    }

    private JButton createShowAttemptOnMapButton() {
        showAttemptOnMap = new JButton("Show selected on map");
        showAttemptOnMap.setEnabled(false);
        showAttemptOnMap
                .setToolTipText("Press to show location of selected attempt on map");
        showAttemptOnMap.setMultiClickThreshhold(800);
        showAttemptOnMap.setMnemonic('M');
        showAttemptOnMap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MapPanel mapPanel = Swarm.getApplication().getMapFrame()
                        .getMapPanel();
                // DataSearchTableModel tableModel = (DataSearchTableModel)
                // dataSearchResultTable.getModel();
                DefaultTableModel tableModel = (DefaultTableModel) dataSearchResultTable
                        .getModel();
                int row = dataSearchResultTable.getSelectedRow();
                if (row != -1) {
                    Attempt attempt = Attempt.find(Integer.parseInt(tableModel
                            .getValueAt(row, 0).toString()));
                    
                    
                    if(attempt.getLatitude() == null  || attempt.getLongitude() == null){
                        JOptionPane.showMessageDialog(DataSearchResultPanel.this, "No location information for this attempt", "Info",
                                JOptionPane.ERROR_MESSAGE);
                    }else{
                        mapPanel.getMapImagePanel().showSelectedAttempt(attempt);
                        Swarm.getApplication().getMapFrame().setVisible(true);
                    }
                }
                dataSearchResultTable.requestFocusInWindow();
            }
        });
        return showAttemptOnMap;
    }

    private JButton createDeleteButton() {
        deleteAttempt = new JButton("Delete Attempt");
        deleteAttempt.setEnabled(false);
        deleteAttempt.setToolTipText("Press to delete selected attempt");
        // showAttemptOnMap.setMultiClickThreshhold(800);
        deleteAttempt.setMnemonic('M');
        deleteAttempt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = dataSearchResultTable.getSelectedRow();
                DefaultTableModel tableModel = (DefaultTableModel) dataSearchResultTable
                        .getModel();
                if (row != -1) {
                    Integer id = Integer.parseInt(tableModel.getValueAt(row, 0)
                            .toString());
                    Swarm.getApplication().getWaveClipboard()
                            .removeMarkersFromView(Marker.listByAttempt(id));

                    if (Swarm.getSelectedAttempt() != null) {
                        if (Swarm.getSelectedAttempt().getId().equals(id)) {
                            Swarm.setSelectedAttempt(null);
                            Swarm.getApplication().getWaveClipboard()
                                    .disableMarkerGeneration();
                            if (SwarmMenu.eventPropertiesDialog != null) {
                                SwarmMenu.eventPropertiesDialog
                                        .disableHypoCalculation();
                            }
                        }
                    }

                    Attempt.delete(id);
                    tableModel.removeRow(row);
                    disableButtons();
                }
                dataSearchResultTable.requestFocusInWindow();
            }
        });
        return deleteAttempt;
    }

    private JButton createLoadButton() {
        editAttempt = new JButton("Select/Edit Attempt");
        editAttempt.setEnabled(false);
        editAttempt
                .setToolTipText("Press to view/edit markers for this attempt on the wave clipbaord");
        // showAttemptOnMap.setMultiClickThreshhold(800);
        editAttempt.setMnemonic('L');
        editAttempt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = dataSearchResultTable.getSelectedRow();
                DefaultTableModel tableModel = (DefaultTableModel) dataSearchResultTable
                        .getModel();
                if (row != -1) {
                    Integer id = Integer.parseInt(tableModel.getValueAt(row, 0)
                            .toString());
                    Attempt attempt = Attempt.find(id);
                    Event event = Event.find(attempt.getEvent());

                    // setting the events and attempt to be worked on
                    Swarm.setSelectedAttempt(attempt);
                    Swarm.setSelectedEvent(event);
                    Swarm.getApplication().getSwarmMenu()
                            .enableEventProperties();
                    Swarm.getApplication().getWaveClipboard()
                            .enableMarkerGeneration();
                    if(SwarmMenu.eventPropertiesDialog == null){
                        SwarmMenu.eventPropertiesDialog = new DataRecord();
                    }
                    SwarmMenu.eventPropertiesDialog.enableHypoCalculation();
                    SwarmMenu.eventPropertiesDialog.setEventFields();
                    
                    List<Object[]> filePaths = Marker
                            .listMarkerByAttempt(id);

                    
                    for (Object[] fileName : filePaths) {
                        Swarm.getApplication().getWaveClipboard().openFile(new File(fileName[0].toString()));

                        Swarm.getApplication().getWaveClipboard()
                                .clearAndLoadMarkersForFileOnWave(fileName[0].toString(), id);
                        
                    }
                }
                dataSearchResultTable.requestFocusInWindow();
            }
        });
        return editAttempt;
    }

    private JButton createShowHypoButton() {
        showHypoOutput = new JButton("Load Hypo Results");
        showHypoOutput.setEnabled(false);
        showHypoOutput
                .setToolTipText("Press to view/edit results of hypo calculation");
        showHypoOutput.setMnemonic('H');
        showHypoOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = dataSearchResultTable.getSelectedRow();
                DefaultTableModel tableModel = (DefaultTableModel) dataSearchResultTable
                        .getModel();
                if (row != -1) {
                    Integer id = Integer.parseInt(tableModel.getValueAt(row, 0)
                            .toString());
                    Attempt attempt = Attempt.find(id);
                    byte[] hypo = attempt.getHypoResults();
                    if (hypo != null && hypo.length > 0) {
                        HypoResults hr = attempt.getHypoResultsAsObject();
                        if(Swarm.getApplication().getHypoOuputMapFrame() == null){
                            Swarm.getApplication().setHypoOuputMapFrame(new HypoOuputMapFrame());
                        }
                        Swarm.getApplication().getHypoOuputMapFrame().setResultText(hr.getPrintOutput());
                        Swarm.getApplication().getHypoOuputMapFrame().setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(null, "Hypo has not been run for this attempt", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        return showHypoOutput;
    }

    private JButton createCreateButton() {
        createAttempt = new JButton("Create/Select Attempt");
        createAttempt.setEnabled(false);
        createAttempt
                .setToolTipText("Press to create new attempt for the selected event");
        createAttempt.setMnemonic('C');
        createAttempt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = dataSearchResultTable.getSelectedRow();
                DefaultTableModel tableModel = (DefaultTableModel) dataSearchResultTable
                        .getModel();
                if (row != -1) {
                    Integer id = Integer.parseInt(tableModel.getValueAt(row, 0)
                            .toString());
                    Event event = null;
                    if (isEventList) {
                        event = Event.find(id);
                    } else {
                        Attempt attempt = Attempt.find(id);
                        event = Event.find(attempt.getEvent());
                    }
                    Attempt attempt = new Attempt();
                    attempt.setEvent(event.getId());
                    attempt.persist();
                    Swarm.setSelectedEvent(event);
                    Swarm.setSelectedAttempt(attempt);
                    Swarm.getApplication().getSwarmMenu()
                            .enableEventProperties();
                    Swarm.getApplication().getWaveClipboard()
                            .enableMarkerGeneration();

                    if (SwarmMenu.eventPropertiesDialog != null) {
                        SwarmMenu.eventPropertiesDialog.enableHypoCalculation();
                        SwarmMenu.eventPropertiesDialog.setEventFields();
                    }
                }
                dataSearchResultTable.requestFocusInWindow();
            }
        });
        return createAttempt;
    }

    private void initDataSearchResultTable() {
        dataSearchResultTable = new JTable() {
            private Border redBorder = BorderFactory.createMatteBorder(1, 1, 1,
                    1, Color.RED);
            private Border unselectedBorder = BorderFactory.createMatteBorder(
                    1, 1, 1, 1, Color.LIGHT_GRAY);

            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }

            @Override
            public Component prepareRenderer(TableCellRenderer renderer,
                    int row, int column) {
                Object value = getValueAt(row, column);

                boolean isSelected = false;
                boolean hasFocus = false;

                // Only indicate the selection and focused cell if not printing
                if (!isPaintingForPrint()) {
                    isSelected = isCellSelected(row, column);

                    boolean rowIsLead = (selectionModel.getLeadSelectionIndex() == row);
                    boolean colIsLead = (columnModel.getSelectionModel()
                            .getLeadSelectionIndex() == column);

                    hasFocus = (rowIsLead && colIsLead) && isFocusOwner();
                }
                // let's set cell border depending on focus
                JComponent cellRenderer = (JComponent) renderer
                        .getTableCellRendererComponent(this, value, isSelected,
                                hasFocus, row, column);
                if (isSelected && hasFocus) {
                    cellRenderer.setBorder(redBorder);
                } else {
                    cellRenderer.setBorder(unselectedBorder);
                }

                // let's set up striped background
                // 0 means false
                if ((row & 1) == 0) {
                    cellRenderer.setBackground(new Color(0xccccff));
                } else {
                    cellRenderer.setBackground(Color.WHITE);
                }
                return cellRenderer;
            }
        };
        dataSearchResultTable.addKeyListener(new KeyAdapter() {
            private String attemptId = new String();

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getModifiers() == KeyEvent.CTRL_MASK
                        && (int) e.getKeyChar() == 10) {
                    selectRow = true;
                    attemptId = new String();
                } else if (selectRow) {
                    char keyChar = e.getKeyChar();
                    if (keyChar >= 48 && keyChar <= 57) {
                        attemptId += String.valueOf(keyChar);
                        selectRow(Integer.valueOf(attemptId).intValue());
                    } else {
                        selectRow = false;
                        attemptId = new String();
                    }
                }
            }
        });
        dataSearchResultTable.setSelectionBackground(new Color(0xbbccdd));
        dataSearchResultTable.setSelectionForeground(new Color(0x333300));
        dataSearchResultTable.setFillsViewportHeight(true);
        // dataSearchResultTable.setRowSelectionAllowed(false);
        dataSearchResultTable
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataSearchResultTable.setAutoCreateRowSorter(true);
        dataSearchResultTable.setRowHeight(30);
        dataSearchResultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        dataSearchResultTable.getColumnModel().setColumnMargin(0);
        dataSearchResultTable
                .setPreferredScrollableViewportSize(dataSearchResultTable
                        .getPreferredSize());
        dataSearchResultTable.setDefaultRenderer(Timestamp.class,
                new DateTimeRenderer());
        // dataSearchResultTable.putClientProperty("terminateEditOnFocusLost",
        // true);

        dataSearchResultTable.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (dataSearchResultTable.getSelectedRow() == -1) {
                    disableButtons();
                    createAttempt.setEnabled(false);
                } else {
                    if (isEventList) {
                        disableButtons();
                    } else {
                        enableButtons();
                    }
                    createAttempt.setEnabled(true);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub

            }

        });
    }

    int updateDataSearchTableModel(ResultSetModel resultSetModel,
            DataSearchManager dataSearchManager) {
        TableModel tableModel = dataSearchResultTable.getModel();
        TableColumnModel columnModel = dataSearchResultTable.getColumnModel();
        if (tableModel instanceof DataSearchTableModel) {
            ((DataSearchTableModel) tableModel).updateModel(resultSetModel,
                    true);
        } else {
            dataSearchResultTable
                    .setModel(tableModel = new DataSearchTableModel(
                            resultSetModel, dataSearchManager));
        }
        this.initColumnWidths(tableModel, columnModel);
        dataSearchResultTable.setCellSelectionEnabled(true);
        return tableModel.getRowCount();
    }

    int updateDataSearchTableModel(List<Attempt> attempts) {

//      Object[][] dd = new Object[attempts.size()][4];
//      Object[] col = { "Attempt ID", "Event", "Event Type" };
        List<Integer> ids = new ArrayList<Integer>();
        ArrayList<Object[]> data =   new ArrayList<Object[]>();
        
        for (int i = 0; i < attempts.size(); i++) {
            if(!ids.contains(attempts.get(i).getId())){
                ids.add(attempts.get(i).getId());
                Object[] rowD = new Object[3];
                rowD[0] = attempts.get(i).getId();
                Event e = Event.find(attempts.get(i).getEvent());
                rowD[1] = e.getEventLabel();
                rowD[2] = e.getEventType();
                data.add(rowD);
            }
            
//          dd[i][0] = attempts.get(i).getId();
//          Event e = Event.find(attempts.get(i).getEvent());
//          dd[i][1] = e.getEventLabel();
//          dd[i][2] = e.getEventType();

        }
//      DefaultTableModel dtm = new DefaultTableModel(dd, col);
        
        DefaultTableModel dtm = new DefaultTableModel();
        dtm.addColumn("Attempt ID");
        dtm.addColumn("Event");
        dtm.addColumn("Event Type");
        for(Object[] r : data){
            dtm.addRow(r);
        }
    
        dataSearchResultTable.setModel(dtm);
        return dtm.getRowCount();
    }

    int updateDataSearchTableModelByEvent(List<Event> events) {

        Object[][] dd = new Object[events.size()][4];
        Object[] col = { "Event ID", "Event", "Event Type" };
        for (int i = 0; i < events.size(); i++) {
            dd[i][0] = events.get(i).getId();
            dd[i][1] = events.get(i).getEventLabel();
            dd[i][2] = events.get(i).getEventType();

        }
        DefaultTableModel dtm = new DefaultTableModel(dd, col);
        dataSearchResultTable.setModel(dtm);
        return dtm.getRowCount();
    }

    private void initColumnWidths(TableModel tableModel,
            TableColumnModel columnModel) {
        TableColumn column = null;
        Component comp = null;
        int cellWidth = 0;
        int headerWidth = 0;
        TableCellRenderer headerRenderer = dataSearchResultTable
                .getTableHeader().getDefaultRenderer();

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            column = columnModel.getColumn(i);
            comp = headerRenderer.getTableCellRendererComponent(null,
                    column.getHeaderValue(), false, false, -1, 0);
            headerWidth = comp.getPreferredSize().width;
            Class<?> columnClass = tableModel.getColumnClass(i);
            for (int j = 0; j < tableModel.getRowCount(); j++) {
                comp = dataSearchResultTable
                        .getDefaultRenderer(columnClass)
                        .getTableCellRendererComponent(dataSearchResultTable,
                                tableModel.getValueAt(j, i), false, false, j, i);
                int width = comp.getPreferredSize().width;
                // we cache width of first column. And compare widths of next
                // columns with width of first.
                // If some column has greater width it becomes width of whole
                // column(unless header has greater width)
                if (cellWidth < width || j == 0) {
                    cellWidth = width;
                }
            }
            TableCellRenderer centeredRenderer = dataSearchResultTable
                    .getDefaultRenderer(columnClass);
            if (centeredRenderer instanceof DefaultTableCellRenderer) {
                ((DefaultTableCellRenderer) centeredRenderer)
                        .setHorizontalAlignment(SwingConstants.CENTER);
                column.setCellRenderer(centeredRenderer);
            }
            if (headerWidth > cellWidth)
                column.setPreferredWidth(headerWidth + 5);
            else
                column.setPreferredWidth(cellWidth + 5);
        }
    }

    public void selectRow(int attemptId) {
        DataSearchTableModel tableModel = (DataSearchTableModel) dataSearchResultTable
                .getModel();
        TableColumnModel columnModel = dataSearchResultTable.getColumnModel();
        int row = -1;
        if (attemptId >= 0
                && (row = getRowNumberByAttemptId(attemptId, tableModel,
                        columnModel)) != -1) {
            dataSearchResultTable.setRowSelectionInterval(row, row);
            dataSearchResultTable.setColumnSelectionInterval(
                    tableModel.getPreferredIndex(),
                    tableModel.getPreferredIndex());
            dataSearchResultTable.editCellAt(row,
                    tableModel.getPreferredIndex());
            dataSearchResultTable.requestFocusInWindow();
        }
    }

    private Integer getRowNumberByAttemptId(int attemptId,
            DataSearchTableModel tableModel, TableColumnModel columnModel) {
        if (columnModel.getColumnCount() > 0) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, tableModel.getAttemptIdColIndex())
                        .equals(attemptId)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setWaitCursor() {
        super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void setDefaultCursor() {
        super.setCursor(Cursor.getDefaultCursor());
    }

    public void setTableAutoResizeOn() {
        dataSearchResultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    public void setTableAutoResizeOFF() {
        dataSearchResultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    public void focusDataSearchResultTable() {
        selectRow = true;
        dataSearchResultTable.requestFocusInWindow();
    }

    public void disableButtons() {
        showAttemptOnMap.setEnabled(false);
        deleteAttempt.setEnabled(false);
        editAttempt.setEnabled(false);
        createAttempt.setEnabled(false);
        showHypoOutput.setEnabled(false);
    }

    public void enableButtons() {
        showAttemptOnMap.setEnabled(true);
        deleteAttempt.setEnabled(true);
        editAttempt.setEnabled(true);
        showHypoOutput.setEnabled(true);
    }

    static class DateTimeRenderer extends DefaultTableCellRenderer.UIResource {
        private DateFormat formatter;

        public DateTimeRenderer() {
            super();
        }

        public void setValue(Object value) {
            if (formatter == null) {
                formatter = DateFormat.getDateTimeInstance();
            }
            setText((value == null) ? "" : formatter.format(value));
        }
    }
}