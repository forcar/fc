package org.clas.fcmon.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedList;
import java.util.TreeMap;
 
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorPane2D;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.utils.groups.IndexedTable;

/*
 * @author gavalian
 * Revised by L. C. Smith in Sep-Nov 2015 for development of ECMon.java.
 */

@SuppressWarnings("serial")
public class MonitorApp extends JFrame implements ActionListener {
    
    DetectorPane2D       detectorView;  	
    JTabbedPane      canvasTabbedPane;
    JSplitPane             vSplitPane; 
    JSplitPane	           hSplitPane;
	
    JPanel  detectorPane;
    JPanel  infoPane;
    JLabel statusLabel = null;
    JPanel  canvasPane = null;
    JPanel  buttonPane = null;
    JTextField   runno = new JTextField(4);
    JCheckBox    mcBtn = null;
    
    TreeMap<String,EmbeddedCanvas>  paneCanvas = new TreeMap<String,EmbeddedCanvas>();
	
    JPanel  controlsPanel0 = null;        
    JPanel  controlsPanel1 = null;
    JPanel  controlsPanel2 = null;
    JPanel  controlsPanel3 = null;
	
    EventControl              eventControl = null;    
    public DisplayControl   displayControl = null;	
    public Mode7Emulation   mode7Emulation = null;
    
    int      selectedTabIndex = 0;  
    String   selectedTabName  = " ";  
    public String currentView = null;
    public int   currentCrate = 1;
    public int   currentSlot  = 3;
    public int   currentChan  = 0;
    public int  detectorIndex = 0;
    public int      viewIndex = 1;
    public boolean    doEpics = false;
    public String     appName = null;
    public String    rootPath = ".";
    public String    hipoPath = null;
    public String   calibPath = null;
    public String      hvPath = null;
    public String    xMsgHost = null;
    public String   runNumber = "100";
    public boolean      debug = false;
    public boolean       isMC = false;
    public boolean      isCRT = false;
    public boolean      doEng = false;
    public String        geom = "0.27";
    public String      config = "muon";
    
    public FTHashCollection rtt = null;
    
    public DetectorCollection<LinkedList<Double>> fifo1 = new DetectorCollection<LinkedList<Double>>();
    public DetectorCollection<LinkedList<Double>> fifo2 = new DetectorCollection<LinkedList<Double>>();
    public DetectorCollection<LinkedList<Double>> fifo3 = new DetectorCollection<LinkedList<Double>>();
    public DetectorCollection<LinkedList<Double>> fifo4 = new DetectorCollection<LinkedList<Double>>();
    public DetectorCollection<LinkedList<Double>> fifo5 = new DetectorCollection<LinkedList<Double>>();
    public DetectorCollection<LinkedList<Double>> fifo6 = new DetectorCollection<LinkedList<Double>>();
        
    DetectorMonitor   monitoringClass = null;
    
    public MonitorApp(String name, int xsize, int ysize) {
        super(name);
        this.setPreferredSize(new Dimension(xsize, ysize));
    }
    
    public void init(){
        this.addChangeListener();
        this.pack();
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.displayControl.setFPS(2);
        eventControl.setXmsgHost(this.xMsgHost);
        this.setSelectedTab(2);
    }
    
    public void setPluginClass(DetectorMonitor mon) {
    	this.monitoringClass = mon;
    }
    
    public void setAppName(String name) {
        this.appName = name;
    }
    
    public void getReverseTT(ConstantsManager ccdb) {
        System.out.println("monitor.getReverseTT()"); 
        IndexedTable tt = ccdb.getConstants(10,  "/daq/tt/ec");
        rtt = new FTHashCollection<int[]>(4);
        for(int ic=1; ic<35; ic++) {
            for (int sl=3; sl<19; sl++) {
                int chmax=16;
                if (sl==6||sl==16) chmax=128;
                for (int ch=0; ch<chmax; ch++){
                    if (tt.hasEntry(ic,sl,ch)) {
                        int[] dum = {ic,sl,ch}; rtt.add(dum,tt.getIntValue("sector",    ic,sl,ch),
                                                            tt.getIntValue("layer",     ic,sl,ch),
                                                            tt.getIntValue("component", ic,sl,ch),
                                                            tt.getIntValue("order",     ic,sl,ch));
                    };
                }
            }
        }
    } 
    
    public void getEnv() {        
        String   ostype = System.getenv("OSTYPE").toLowerCase();    
        if (ostype!=null&&ostype.equals("linux")) {
            String hostname = System.getenv("HOST");
            if(hostname.substring(0,4).equals("clon")) {
              System.out.println("Running on "+hostname);
              doEpics = true;
              setIsMC(false);
              rootPath = "/home/clasrun/"+appName;              
              xMsgHost = "129.57.167.227"; //clondaq4
           }
        } else {
            System.out.println("Running on "+ostype);
            doEpics = false;
            setIsMC(true);
            rootPath  = "/Users/colesmith/"+appName;
            xMsgHost = "localhost";
        }
        hipoPath  = rootPath+"/HIPO/";
        calibPath = rootPath+"/CALIB/";
           hvPath = rootPath+"/HV/";
    }    
    
    public void makeGUI(){

// Setup containers        
        
        this.setLayout(new BorderLayout());   
    	
        this.detectorView       = new DetectorPane2D();
        this.infoPane           = new JPanel();
        this.detectorPane       = new JPanel();
        
        detectorPane.setLayout(new BorderLayout());
        this.detectorPane.add(detectorView,BorderLayout.CENTER);
        this.detectorPane.add(infoPane,BorderLayout.PAGE_END);
        
        this.canvasTabbedPane   = new JTabbedPane();    
        this.buttonPane         = new JPanel();
        this.canvasPane         = new JPanel();
        
        canvasPane.setLayout(new BorderLayout());
        this.canvasPane.add(canvasTabbedPane,BorderLayout.CENTER);
        this.canvasPane.add(buttonPane,BorderLayout.PAGE_END);

        
// InfoPane label
        
        infoPane.setLayout(new FlowLayout());
        statusLabel = new JLabel(" ");         
        infoPane.add(statusLabel);

// Canvas buttons
		
        mcBtn = new JCheckBox("MC");
        mcBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    isMC = true;
                } else {
                    isMC = false;
                };
            }
        });         
        mcBtn.setSelected(true);
        buttonPane.add(mcBtn);
        
        JButton resetBtn = new JButton("Clear Histos");
        resetBtn.addActionListener(this);
        buttonPane.add(resetBtn);   
        
        JButton saveBtn = new JButton("Write Histos");
        saveBtn.addActionListener(this);
        buttonPane.add(saveBtn);	
        
        JButton loadBtn = new JButton("Read Histos");
        loadBtn.addActionListener(this);
        buttonPane.add(loadBtn); 
        
        buttonPane.add(new JLabel("Run:"));
        runno.setActionCommand("RUN"); runno.addActionListener(this); runno.setText(runNumber);  
        buttonPane.add(runno); 
        
// Control Panels
		
        this.controlsPanel0 = new JPanel(new GridBagLayout());
		
        this.controlsPanel1 = new JPanel();
        this.controlsPanel1.setBorder(BorderFactory.createTitledBorder("Event Control"));
		
        this.controlsPanel2 = new JPanel();
        this.controlsPanel2.setBorder(BorderFactory.createTitledBorder("Display Control"));
		
        this.controlsPanel3 = new JPanel();
        this.controlsPanel3.setBorder(BorderFactory.createTitledBorder("Mode 7 Emulation"));

        eventControl   = new EventControl();   this.controlsPanel1.add(eventControl);
        displayControl = new DisplayControl(); this.controlsPanel2.add(displayControl);
        mode7Emulation = new Mode7Emulation(); this.controlsPanel3.add(mode7Emulation);
      
        eventControl.setPluginClass(this.monitoringClass,this.detectorView);
        displayControl.setPluginClass(this.detectorView);
        mode7Emulation.setPluginClass(this.detectorView);
        
    	this.setJMenuBar(new FCMenuBar(eventControl));
		
        this.controlsPanel0.setBackground(Color.LIGHT_GRAY);
        this.controlsPanel1.setBackground(Color.LIGHT_GRAY);
        this.controlsPanel2.setBackground(Color.LIGHT_GRAY);
        this.controlsPanel3.setBackground(Color.LIGHT_GRAY);
		
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 0.5;
		
        c.gridx=0 ; c.gridy=0 ; this.controlsPanel0.add(this.controlsPanel1,c);
        c.gridx=0 ; c.gridy=1 ; this.controlsPanel0.add(this.controlsPanel2,c);
        c.gridx=0 ; c.gridy=2 ; this.controlsPanel0.add(this.controlsPanel3,c);
        		
// Basic GUI layout
        
        this.hSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,detectorPane,controlsPanel0);		
        this.hSplitPane.setDividerLocation(600);  
        this.hSplitPane.setResizeWeight(1.0);
		
        this.vSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,hSplitPane,canvasPane);			
        this.vSplitPane.setDividerLocation(600);  

        this.add(this.vSplitPane,BorderLayout.CENTER);
    }
    
 /*   
    private void initTimer(){
        updateDelay = 1000 / FPS_INIT;
        updateGUIAction action = new updateGUIAction();
        this.updateTimer = new javax.swing.Timer(updateDelay,action);  
        this.updateTimer.start();
    }  
    
    private class updateGUIAction implements ActionListener {
       public void actionPerformed(ActionEvent evt) {
          detectorView.repaint();
       }
    }   
*/  
    public void addCanvas(String name){
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        this.paneCanvas.put(name, canvas);
        this.canvasTabbedPane.addTab(name,canvas);
    }
    
    public void addCanvas(String name, EmbeddedCanvas canvas){         
        this.canvasTabbedPane.addTab(name,canvas);
    }
    
    public void addFrame(String name, JPanel frame) {
        this.canvasTabbedPane.addTab(name, frame);
    }
    
    public EmbeddedCanvas getCanvas(String name){
        return this.paneCanvas.get(name);
    }  
    
    public DetectorPane2D getDetectorView(){
        return this.detectorView;
    }  
    
    public JPanel getControlPanel(){
        return this.controlsPanel1;
    }  
    
    public void setFPS(int fps){
        this.displayControl.setFPS(fps);
    }
    
    public int getInProcess() {
        return eventControl.inProcess;
    }    
    
    public void setInProcess(int inProcess) {
        eventControl.inProcess = inProcess;
    }
    
    public Boolean getIsRunning() {
        return eventControl.isRunning;
    }
    
    public void setIsRunning(Boolean isRunning) {
         eventControl.isRunning = isRunning;
    }
    
    public void setIsMC(Boolean isMC) {
        mcBtn.setSelected(isMC);
    }
    
    public Boolean isSingleEvent(){
    	return eventControl.isSingleEvent;
    }
    
    public String getDataSource(){
        return eventControl.getDataSource();
    }

    public int getSelectedTabIndex(){
        return this.selectedTabIndex;
    }
    
    public String getSelectedTabName(){
        return this.selectedTabName;
    }
    
    public void setSelectedTab(int index) {
        this.canvasTabbedPane.setSelectedComponent(this.canvasTabbedPane.getComponent(index));
        System.out.println("Selected Tab is "+this.canvasTabbedPane.getTitleAt(index));
    }
    
    public void addChangeListener() {    
      canvasTabbedPane.addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
         if (e.getSource() instanceof JTabbedPane) {
           JTabbedPane pane = (JTabbedPane) e.getSource();
           selectedTabIndex = pane.getSelectedIndex();
           selectedTabName  = (String) pane.getTitleAt(selectedTabIndex);
         }
         }
      });
    }
    
    public String getStatusString(DetectorDescriptor dd) {
        
        String comp=(dd.getLayer()==4) ? "  Pixel:":"  PMT:";  
      
        int is = dd.getSector();
        int sp = viewIndex+3*detectorIndex;
        int ic = dd.getComponent()+1;
        int or = 0;
        int cr = 0;
        int sl = 0;
        int ch = 0;
        if (getSelectedTabName()=="TDC") or=2;
        if (rtt.hasItem(is,sp,ic,or)) {
            int[] dum = (int[]) rtt.getItem(is,sp,ic,or);
            cr = dum[0]; currentCrate = cr;
            sl = dum[1]; currentSlot  = sl;
            ch = dum[2]; currentChan  = ch;
        }   
        return " Sector:"+is+"  Layer:"+sp+comp+ic+" "+" Crate:"+cr+" Slot:"+sl+" Chan:"+ch;
    }
    
    public void updateStatusString(DetectorDescriptor dd)  {
        this.statusLabel.setText(getStatusString(dd)) ; 
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().compareTo("Clear Histos")==0) monitoringClass.reset();
        if(e.getActionCommand().compareTo("Write Histos")==0) monitoringClass.writeHipoFile();
        if(e.getActionCommand().compareTo("Read Histos")==0)  monitoringClass.readHipoFile();
        if(e.getActionCommand().compareTo("RUN")==0)           runNumber = runno.getText();
    }      
}
