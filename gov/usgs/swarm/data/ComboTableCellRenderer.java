package gov.usgs.swarm.data;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class ComboTableCellRenderer implements ListCellRenderer, TableCellRenderer {
  DefaultListCellRenderer listRenderer = new DefaultListCellRenderer();

  DefaultTableCellRenderer tableRenderer = new DefaultTableCellRenderer();

  private void configureRenderer(JLabel renderer, Object value) {
    /*if ((value != null) && (value instanceof Color)) {
      renderer.setText(value.toString());
      //renderer.setBackground((Color)value);
    } else {
      renderer.setText((String) value);
    }*/
	renderer.setText((String) value);
  }

  public Component getListCellRendererComponent(JList list, Object value, int index,
      boolean isSelected, boolean cellHasFocus) {
    listRenderer = (DefaultListCellRenderer) listRenderer.getListCellRendererComponent(list, value,
        index, isSelected, cellHasFocus);
    configureRenderer(listRenderer, value);
    return listRenderer;
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    tableRenderer = (DefaultTableCellRenderer) tableRenderer.getTableCellRendererComponent(table,
        value, isSelected, hasFocus, row, column);
    configureRenderer(tableRenderer, value);
    return tableRenderer;
  }
}