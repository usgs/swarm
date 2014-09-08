package gov.usgs.swarm.database.view;

import gov.usgs.swarm.SwarmMenu;
import gov.usgs.swarm.database.model.Attempt;
import gov.usgs.swarm.database.model.Event;
import gov.usgs.swarm.database.model.Marker;
import gov.usgs.swarm.database.model.SearchQueryCriteria;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 * This dialog that embeds on the top search dialog and on the bottom table that includes results of search in database.
 * All components in search dialog can be traversed by Tab. This dialog allows :
 * <ul>
 * <li>
 * As soon as focus is lost on text field its text is validated and if it is invalid text field background color is
 * changed to red</li>
 * <li>
 * At any moment user can press ESCAPE button to hide the dialog. If user again wants to show dialog, Ctrl+S should be
 * pressed. Changes to search text fields are not lost.</li>
 * <li>
 * Press Enter or Alt+S to perform search after search data was entered.</li>
 * <li>
 * Press Ctrl+D to maximize search dialog. If divider is on top and previously it was not on bottom then it is needed to
 * press Ctrl+D twice.</li>
 * <li>
 * Press Ctrl+U to maximize search result dialog. If divider is on bottom and previously it was not on top then it is
 * needed to press Ctrl+U twice.</li>
 * <li>
 * Press Alt+Enter to resize whole dialog to full screen. To escape from full screen press Alt+Enter again</li>
 * <li>
 * Press Ctrl+J to enter attempt_id column's value which will be selected in search result table. It means that you
 * press Ctrl+J and then you type attempt_id value and you get corresponding preferred column selected. No need to touch
 * mouse at all.</li>
 * </ul>
 * 
 * @author Chirag Patel
 */
public class DataSearchDialog extends JFrame {
    private static final long serialVersionUID = 1L;

    private JCheckBox searchByEventCheckBox;
    private JLabel eventLabel;
    private DataSearchTextField<String> eventTypeField;
    private JLabel timestampLabel;
    private JLabel timestammpToLabel;
    private DataSearchTextField<Timestamp> timestampStartField;
    private DataSearchTextField<Timestamp> timestampEndField;
    // private JLabel durationMagnitudeLabel;
    // private DataSearchTextField<Long> durationMagnitudeMinField;
    // private JLabel durationMagnitudeToLabel;
    // private DataSearchTextField<Long> durationMagnitudeMaxField;
    private DataSearchResultPanel dataSearchResultPanel = null;
    private Dimension sizeBeforeFullScreen;
    private Point locationBeforeFullScreen;
    private KeyStroke maximizeMinimizeStroke;
    final String datePattern = "yyyy-MM-dd  HH:mm:ss";

    public DataSearchDialog() {
        super("Data search");
        JPanel contentPane = (JPanel)super.getContentPane();

        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        super.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JSplitPane splitPane = createSplitPane();

        JPanel searchPanel = createSearchPanel();
        splitPane.setDividerLocation(searchPanel.getPreferredSize().height + 5);
        splitPane.setTopComponent(searchPanel);
        splitPane.setBottomComponent(dataSearchResultPanel = new DataSearchResultPanel());

        searchPanel.setPreferredSize(new Dimension(splitPane.getPreferredSize().width + 150, 600));
        contentPane.add(splitPane);
        super.pack();

        // let's set default button for search panel
        for (Component component : searchPanel.getComponents()) {
            if (component instanceof JButton) {
                JButton defaultButton = (JButton)component;
                searchPanel.getRootPane().setDefaultButton(defaultButton);
            }
        }

        final InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = contentPane.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "actionOnEscape");
        actionMap.put("actionOnEscape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeMe();
            }
        });

        maximizeMinimizeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK);
        inputMap.put(maximizeMinimizeStroke, "toFullScreen");

        actionMap.put("toFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sizeBeforeFullScreen = DataSearchDialog.this.getSize();
                locationBeforeFullScreen = DataSearchDialog.this.getLocationOnScreen();
                resizeAndRelocate(Toolkit.getDefaultToolkit().getScreenSize(), new Point(0, 0));
                dataSearchResultPanel.setTableAutoResizeOn();
                inputMap.put(maximizeMinimizeStroke, "revertFullScreen");
            }
        });
        actionMap.put("revertFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resizeAndRelocate(sizeBeforeFullScreen, locationBeforeFullScreen);
                dataSearchResultPanel.setTableAutoResizeOFF();
                inputMap.put(maximizeMinimizeStroke, "toFullScreen");
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_DOWN_MASK), "selectTableRow");
        actionMap.put("selectTableRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataSearchResultPanel.focusDataSearchResultTable();
            }
        });

        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeMe();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                eventTypeField.requestFocusInWindow();
            }
        });

        setAlwaysOnTop(false);
    }

    private JSplitPane createSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(10);
        splitPane.setOneTouchExpandable(true);
        final SplitPaneUI ui = splitPane.getUI();
        if (ui instanceof BasicSplitPaneUI) {
            ((BasicSplitPaneUI)ui).getDivider().setBorder(null);
            splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), "jumpToTop");
            splitPane.getActionMap().put("jumpToTop", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JButton)((BasicSplitPaneUI)ui).getDivider().getComponent(0)).doClick();
                }
            });
            splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "jumpToBottom");
            splitPane.getActionMap().put("jumpToBottom", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JButton)((BasicSplitPaneUI)ui).getDivider().getComponent(1)).doClick();
                }
            });
        }
        return splitPane;
    }

    private JPanel createSearchPanel() {
        searchByEventCheckBox = new JCheckBox("Do not search by attempts");
        eventLabel = new JLabel("Event Type : ", SwingConstants.LEFT);
        eventTypeField =
                this.createDataSearchTextField(new DataSearchTextField<String>(String.class, "event label", 30,
                        Color.GRAY));
        timestampLabel = new JLabel("Timestamp : ");
        timestammpToLabel = new JLabel("to");
        timestampStartField =
                this.createDataSearchTextField(new DataSearchTextField<Timestamp>(Timestamp.class, datePattern, 15,
                        Color.GRAY));
        timestampEndField =
                this.createDataSearchTextField(new DataSearchTextField<Timestamp>(Timestamp.class, datePattern, 15,
                        Color.GRAY));
        // durationMagnitudeLabel = new JLabel("Duration Magnitude : ");
        // durationMagnitudeMinField = this.createDataSearchTextField(new DataSearchTextField<Long>(Long.class,
        // "min duration", 15, Color.GRAY));
        // durationMagnitudeToLabel = new JLabel("to");
        // durationMagnitudeMaxField = this.createDataSearchTextField(new DataSearchTextField<Long>(Long.class,
        // "max duration", 15, Color.GRAY));
        return layoutSearchPanel(initSearchPanel());
    }

    private JPanel initSearchPanel() {
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setMinimumSize(new Dimension(0, 0));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Search query"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return searchPanel;
    }

    private JPanel layoutSearchPanel(JPanel searchPanel) {
        GridBagConstraints gbc =
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
                        new Insets(5, 2, 5, 3), 0, 0);

        searchPanel.add(eventLabel, updateGridBagConstraints(gbc, 0, 0, 1, 1, 0f, 0f));
        searchPanel.add(eventTypeField, updateGridBagConstraints(gbc, 1, 0, 3, 1, 0f, 0f));
        searchPanel.add(timestampLabel, updateGridBagConstraints(gbc, 0, 1, 1, 1, 0f, 0f));
        searchPanel.add(timestampStartField, updateGridBagConstraints(gbc, 1, 1, 1, 1, 0f, 0f));
        searchPanel.add(timestammpToLabel, updateGridBagConstraints(gbc, 2, 1, 1, 1, 0f, 0f));
        searchPanel.add(timestampEndField, updateGridBagConstraints(gbc, 3, 1, 1, 1, 0f, 0f));
        // searchPanel.add(durationMagnitudeLabel, updateGridBagConstraints(gbc, 0, 2, 1, 1, 0f, 0f));
        // searchPanel.add(durationMagnitudeMinField, updateGridBagConstraints(gbc, 1, 2, 1, 1, 0f, 0f));
        // searchPanel.add(durationMagnitudeToLabel, updateGridBagConstraints(gbc, 2, 2, 1, 1, 0f, 0f));
        // searchPanel.add(durationMagnitudeMaxField, updateGridBagConstraints(gbc, 3, 2, 1, 1, 0f, 0f));
        searchPanel.add(searchByEventCheckBox);
        gbc = updateGridBagConstraints(gbc, 0, 4, 4, 1, 0f, 0f);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.ipadx = 4;
        gbc.ipady = 2;
        gbc.insets = new Insets(15, 0, 0, 0);


        searchPanel.add(createSearchButton(), gbc);
        gbc.insets = new Insets(15, 205, 0, 0);
        // searchPanel.add(createSearchLocationButton(),gbc);

        return searchPanel;
    }

    private JButton createSearchButton() {
        JButton searchButton = new JButton("Search");
        searchButton.setMnemonic(KeyEvent.VK_S);
        searchButton.setToolTipText("Press to search for attempts by enetered criterias");
        searchButton.setMultiClickThreshhold(800);
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (eventTypeField.isValueValid() && timestampStartField.isValueValid() &&
                        timestampEndField.isValueValid())
                    // && durationMagnitudeMinField.isValueValid() && durationMagnitudeMaxField.isValueValid())
                        doSearch(new SearchQueryCriteria(eventTypeField.validValue(), timestampStartField.validValue(),
                                timestampEndField.validValue(), Long.MIN_VALUE, Long.MAX_VALUE));
                    // durationMagnitudeMinField.validValue(), durationMagnitudeMaxField.validValue()));
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        return searchButton;
    }


    // private JButton createSearchLocationButton()
    // {
    // JButton searchButton = new JButton("Search By location");
    // searchButton.setMnemonic(KeyEvent.VK_S);
    // searchButton.setToolTipText("Press to search for attempts by location");
    // searchButton.setMultiClickThreshhold(800);
    // searchButton.addActionListener(new ActionListener()
    // {
    // @Override
    // public void actionPerformed(ActionEvent e)
    // {
    //
    // if(Swarm.getApplication().getLocationMapFrame() == null){
    // Swarm.getApplication().setLocationMapFrame(new LocationPickerMapFrame());
    // }
    // Swarm.getApplication().getLocationMapFrame().setVisible(true);
    //
    // if(!Swarm.getApplication().getLocationMapFrame().isVisible()){
    // Point2D.Double longLat = Swarm.getApplication().getLocationMapFrame().getLongLat();
    // Double minD = Swarm.getApplication().getLocationMapFrame().getMinD();
    // Double maxD = Swarm.getApplication().getLocationMapFrame().getMaxD();
    //
    //
    // dataSearchResultPanel.isEventList = false;
    // List<Attempt> attempts = Marker.getAttemptsByPMarkerAndLocationAndDepth(minD, maxD, longLat);
    // dataSearchResultPanel.updateDataSearchTableModel(attempts);
    //
    // }
    // }
    // });
    // return searchButton;
    // }

    private <T> DataSearchTextField<T> createDataSearchTextField(final DataSearchTextField<T> textField) {
        textField.setMinimumSize(textField.getPreferredSize());
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.isInitialText()) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                } else textField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                textField.validateInput();
            }
        });
        return textField;
    }

    private void doSearch(SearchQueryCriteria searchQueryCriteria) throws SQLException {
        if (searchByEventCheckBox.isSelected()) {
            dataSearchResultPanel.isEventList = true;
            List<Event> events = Event.listByType(eventTypeField.validValue());
            dataSearchResultPanel.updateDataSearchTableModelByEvent(events);
        } else {
            dataSearchResultPanel.isEventList = false;
            List<Attempt> attempts =
                    Marker.getAttemptsByPMarker(eventTypeField.validValue(), timestampStartField.validValue(),
                            timestampEndField.validValue());
            attempts.addAll(Marker.getAttemptsBySMarker(eventTypeField.validValue(), timestampStartField.validValue(),
                    timestampEndField.validValue()));
            attempts.addAll(Marker.getAttemptsByCODAMarker(eventTypeField.validValue(),
                    timestampStartField.validValue(), timestampEndField.validValue()));
            attempts.addAll(Marker.getAttemptsByAZIMUTHMarker(eventTypeField.validValue(),
                    timestampStartField.validValue(), timestampEndField.validValue()));
            attempts.addAll(Marker.getAttemptsBySMarker(eventTypeField.validValue(), timestampStartField.validValue(),
                    timestampEndField.validValue()));
            dataSearchResultPanel.updateDataSearchTableModel(attempts);
        }
        dataSearchResultPanel.setDefaultCursor();
        dataSearchResultPanel.disableButtons();
    }

    private GridBagConstraints updateGridBagConstraints(GridBagConstraints gbc, int gridx, int gridy, int gridwidth,
            int gridheight, float weightx, float weighty) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        return gbc;
    }

    public void centerOnScreen() {
        if (locationBeforeFullScreen == null) {
            super.setLocationRelativeTo(null);
        }
    }

    private void resizeAndRelocate(Dimension size, Point location) {
        super.setSize(size);
        super.setLocation(location);
    }

    private void closeMe() {
        super.setVisible(false);
        SwarmMenu.setDataQueryState(false);
    }


    private class DataSearchTextField<T> extends JTextField {
        private final Color errorColor = new Color(0xFFbbbb);
        private T validValue;
        private String initialText = null;
        private Class<T> valueClass;

        DataSearchTextField(Class<T> valueClass, String initialText, int columns, Color foreground) {
            super(initialText, columns);
            this.valueClass = valueClass;
            super.setForeground(foreground);
            this.initialText = initialText;
        }

        T validValue() {
            return validValue;
        }

        boolean isValueValid() {
            return validateInput();
        }

        @SuppressWarnings("unchecked")
        boolean validateInput() {
            String text = super.getText();
            if (text.isEmpty()) {
                validValue = null;
                super.setBackground(Color.WHITE);
                return true;
            } else if (valueClass == Timestamp.class) {
                if (validateTimestamp(text)) {
                    super.setBackground(Color.WHITE);
                    return true;
                }
            } else if (valueClass == Long.class) {
                if (validateLong(text)) {
                    super.setBackground(Color.WHITE);
                    return true;
                }
            } else {
                validValue = (T)text;
                return true;
            }
            super.setBackground(errorColor);
            return false;
        }

        @SuppressWarnings("unchecked")
        private boolean validateTimestamp(String text) {
            try {
                validValue = (T)Timestamp.valueOf(text);
                return true;
            } catch (IllegalArgumentException e) {
                validValue = null;
                return isInitialText();
            }
        }

        @SuppressWarnings("unchecked")
        private boolean validateLong(String text) {
            try {
                validValue = (T)Long.valueOf(text);
                return true;
            } catch (NumberFormatException e) {
                validValue = null;
                return isInitialText();
            }
        }

        boolean isInitialText() {
            return initialText.equals(super.getText());
        }
    }
}