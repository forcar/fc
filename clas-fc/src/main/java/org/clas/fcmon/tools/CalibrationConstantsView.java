package org.clas.fcmon.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.utils.groups.IndexedTable;

/**
 *
 * @author gavalian
 */
public class CalibrationConstantsView extends JPanel implements ActionListener {
    
    JTabbedPane tabbedPane   = new JTabbedPane();
    JPanel      buttonsPanel = null;
    JButton     drawDifference = null;
    JComboBox   comboBox       = null;
    
    Map<String,CalibrationConstants>  calibrationMap = new LinkedHashMap<String,CalibrationConstants>();
//    ConstantsManager                  calibrationManager = new ConstantsManager();
    
    

    public CalibrationConstantsView(String buttonsOption){
        super();
        this.setLayout(new BorderLayout());
        this.add(tabbedPane,BorderLayout.CENTER);
        buttonsPanel = new JPanel();
        this.add(buttonsPanel,BorderLayout.PAGE_END);
        if (buttonsOption.equals("DrawDifference")) getDrawDifference();
    }
    
    public void getDrawDifference() {
        drawDifference = new JButton("Show");
        drawDifference.addActionListener(this);
        buttonsPanel.add(drawDifference);
        comboBox = new JComboBox();
        buttonsPanel.add(comboBox);                
    }
        
    public void addConstants(CalibrationConstants calib, CalibrationConstantsListener listener){
        JTable dataTable = new JTable(calib);
        dataTable.setDefaultRenderer(Object.class, new CalibrationConstants.CalibrationConstantsRenderer(calib));
        dataTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = dataTable.getSelectedRow();
                int col = dataTable.getSelectedColumn();
                listener.constantsEvent(calib, col, row);
                //System.out.println("selected row  " + row + " column "
                //+ col);
            }
        });
        JScrollPane   scrollPane = new JScrollPane(dataTable);        
        calibrationMap.put(calib.getName(), calib);        
        tabbedPane.addTab(calib.getName(), scrollPane);
    }
    
    public void addConstants(CalibrationConstants calib){
        JTable dataTable = new JTable(calib);         
        dataTable.setDefaultRenderer(Object.class, new CalibrationConstants.CalibrationConstantsRenderer(calib));
        JScrollPane   scrollPane = new JScrollPane(dataTable);        
        calibrationMap.put(calib.getName(), calib);        
        tabbedPane.addTab(calib.getName(), scrollPane);
    }
    
    public void addConstants(List<CalibrationConstants> calib){
        for(CalibrationConstants item : calib){
            this.addConstants(item);
        }
    }
    
    public JTabbedPane  getTabbedPane(){
        return this.tabbedPane;
    }
        
    private void updateComboBox(){
        int index = this.tabbedPane.getSelectedIndex();
        String name = tabbedPane.getTitleAt(index);
        CalibrationConstants  calib = this.calibrationMap.get(name);
        int ncolumns = calib.getColumnCount();
        String[] columns = new String[ncolumns];
        for(int i = 0; i < ncolumns; i++){
            columns[i] = calib.getColumnName(i);
            System.out.println("adding column : " + columns[i]);
        }
        DefaultComboBoxModel model = (DefaultComboBoxModel) comboBox.getModel();
        model.removeAllElements();
        for(String item : columns){
            model.addElement(item);
        }
        comboBox.setModel(model);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().compareTo("Show")==0){
            System.out.println("Showing difference");
            this.updateComboBox();
        }
    }
    
    
    
    public static void main(String[] args){
        JFrame frame = new JFrame();
        CalibrationConstantsView view = new CalibrationConstantsView("DrawDifference");
        
        CalibrationConstants gain = new CalibrationConstants(3,"Mean/F:Error/I:Sigma/F:Serror/F");
        for(int i = 0; i < 23; i++){
            gain.addEntry(1,1,i+1);
        }
        gain.addConstraint(3, 0.2, 1.0);
        gain.setDoubleValue(0.2, "Mean", 1,1,1);
        gain.setDoubleValue(0.3, "Mean", 1,1,2);
        gain.setDoubleValue(0.4, "Mean", 1,1,3);
        gain.setDoubleValue(0.5, "Mean", 1,1,4);
        gain.setDoubleValue(0.6, "Mean", 1,1,5);
        
        gain.setIntValue(4, "Error", 1,1,4);
        view.addConstants(gain);
        frame.add(view);
        frame.pack();
        frame.setVisible(true);
    }

    
}
