package org.clas.fcmon.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.band.BANDConstants;
import org.clas.fcmon.detector.view.DetectorPane2D;
//import org.jlab.clas.mq.IpcReceiver;
//import org.jlab.clas.mq.IpcServer;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.base.GStyle;
//import org.jlab.detector.decode.CLASDecoder;
//import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.groot.graphics.EmbeddedCanvas;
//import org.jlab.io.hipo3.Hipo3DataSync;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.utils.groups.IndexedTable;

/*
 * @author gavalian
 * Revised by L. C. Smith in Sep-Nov 2015 for development of ECMon.java.
 */

@SuppressWarnings("serial")
public class MonitorApp extends JFrame implements ActionListener {
    
//	public IpcServer          ipc = null;
	
	DetectorPane2D       detectorView;  	
    JTabbedPane      canvasTabbedPane;
    JSplitPane             vSplitPane; 
    JSplitPane	           hSplitPane;
    JScrollPane            scrollPane;

    JPanel    detectorPane = null;
    JPanel        infoPane = null;
    JLabel     statusLabel = null;
    JPanel      canvasPane = null;
    JPanel      buttonPane = null;
    JPanel  controlsPanel0 = null;        
    JPanel  controlsPanel1 = null;
    JPanel  controlsPanel2 = null;
    JPanel  controlsPanel3 = null;
    JTextField       runno = new JTextField(4);
    JCheckBox        mcBtn = null;
    JCheckBox       mcbBtn = null;
    JCheckBox    filterBtn = null;
    JCheckBox       ipcBtn = null;
    JCheckBox     epicsBtn = null;    
    JButton       openBtn  = null;
    JButton       closeBtn = null;
    
    TreeMap<String,EmbeddedCanvas>  paneCanvas = new TreeMap<String,EmbeddedCanvas>();
	
    EventControl              eventControl = null;    
    
    public FCMenuBar               menuBar = null;
    public CodaEventDecoder    codadecoder = new CodaEventDecoder();
    public FCCLASDecoder3           decoder = new FCCLASDecoder3();
    public FCCLASDecoder4          decoder4 = new FCCLASDecoder4();
//    public Hipo3DataSync           writer3 = null;
    public HipoDataSync             writer = null;
    public DisplayControl   displayControl = null;	
    public Mode7Emulation   mode7Emulation = null;
    
    int      selectedTabIndex = 0;  
    String   selectedTabName  = " ";  
    public String currentView = null;
    public int   currentCrate = 1;
    public int   currentSlot  = 3;
    public int   currentChan  = 0;
    public int   currentEvent = 0;
    public int  detectorIndex = 0;
    public String detectorAlias = "";
    public int      viewIndex = 1;
   
    public boolean    doEpics = false;
    public String     appName = null;
    public String    rootPath = ".";
    public String    hipoPath = null;
    public String   calibPath = null;
    public String      hvPath = null;
    public String  scalerPath = null;
    public String    evioPath = null;
    public String    xMsgHost = "localhost";
    public String   runNumber = "100";
    public boolean      debug = false;
    public boolean       isMC = false;
    public boolean      isMCB = false;
    public boolean       isTB = false;
    public boolean   isFilter = false;
    public boolean  isTrigger = false;
    public boolean      isIPC = false;
    public boolean      isCRT = false;
    public boolean      doEng = false;
    public boolean     doGain = false;
    
    public String    HipoFileName = null;
    public boolean isHipoFileOpen = false;   
    public String       variation = "default";
    public String            geom = "2.5";
    public String          config = "muon";
    public int            trigger = 1;        //0=cluster 1=pixel
    public int               omap = 0;
    public int                run = 0;
    public int        triggerMask = 0; // Set in FCMenuBar
    public long       triggerWord = 0;    
    public int              evtno = 0;
    public int             bitsec = 0;
    public long         timestamp = 0;
    public float            phase = 0;
    public float  phaseCorrection = 0;
    public boolean  correctPhase  = true;
    public int            tcpPort = 11111;

    public int tet,nsa,nsb,pedref;
    public int maxEvents=100000000;
    public int tdcOffset=0;
    public int phaseOffset=3;
    
    public boolean goodFilterEvent = false;
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
    
    public void initGraphics() {
        GStyle.getAxisAttributesX().setTitleFontSize(14);
        GStyle.getAxisAttributesX().setLabelFontSize(14);
        GStyle.getAxisAttributesY().setTitleFontSize(14);
        GStyle.getAxisAttributesY().setLabelFontSize(14);
        GStyle.getAxisAttributesZ().setLabelFontSize(14); 
        GStyle.getAxisAttributesX().setAxisGrid(false);
        GStyle.getAxisAttributesY().setAxisGrid(false);
        GStyle.getAxisAttributesX().setLabelFontName("Avenir");
        GStyle.getAxisAttributesY().setLabelFontName("Avenir");
        GStyle.getAxisAttributesZ().setLabelFontName("Avenir");
        GStyle.getAxisAttributesX().setTitleFontName("Avenir");
        GStyle.getAxisAttributesY().setTitleFontName("Avenir");
        GStyle.getAxisAttributesZ().setTitleFontName("Avenir");
    }
    
    public void initFCMenu() {
    	menuBar.initMenu();
    }
    
    public void setPluginClass(DetectorMonitor mon) {
    	this.monitoringClass = mon;
    	initGraphics();
    }
    
    public void setAppName(String name) {
        this.appName = name;
    }
    
    public void setVariation(String variation) {
        this.variation = variation;
    }
    
    public int getRunNumber() {
    	return Integer.parseInt(runNumber);
    }
    
    public void getReverseTT(ConstantsManager ccdb, int run, String table) {
        System.out.println("monitor.getReverseTT()"); 
        IndexedTable tt = ccdb.getConstants(run, table);
        rtt = new FTHashCollection<int[]>(4);
        for(int ic=1; ic<74; ic++) {
            for (int sl=3; sl<21; sl++) {
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
        String ostype = System.getenv("OSTYPE"); 
        System.out.println("monitor.getEnv(): OSTYPE = "+ostype);
        
        if (ostype!=null&&(ostype.equals("linux")||ostype.equals("Linux"))) {
            String hostname = System.getenv("HOST");
            if(hostname.substring(0,4).equals("clon")) {
              System.out.println("monitor.getEnv(): Running on "+hostname);
              doEpics = false;
              setIsMC(false);
              rootPath = "/home/clasrun/"+appName;              
              xMsgHost = "129.57.167.227"; //clondaq4
            }
        }
        
        if (ostype!=null&&ostype.equals("darwin")) {
            System.out.println("monitor.getEnv(): Running on "+ostype);
            doEpics = false;
            setIsMC(false);
            rootPath  = "/Users/colesmith/"+appName;
            xMsgHost = "localhost";
        }
        
        hipoPath  = rootPath+"/HIPO/";
        calibPath = rootPath+"/CALIB/";
           hvPath = rootPath+"/HV/";
       scalerPath = rootPath+"/SCALERS/";
         evioPath = rootPath+"/EVIO/";
    }    
    
    public void makeGUI(){
    	
// Setup containers        
        
        this.setLayout(new BorderLayout());   
    	
        this.detectorPane       = new JPanel();
        this.detectorView       = new DetectorPane2D();
        this.infoPane           = new JPanel();
        
        detectorPane.setLayout(new BorderLayout());
        this.detectorPane.add(detectorView,BorderLayout.CENTER);
        this.detectorPane.add(infoPane,BorderLayout.PAGE_END);
        
        this.canvasPane         = new JPanel();
        this.canvasTabbedPane   = new JTabbedPane();    
        this.buttonPane         = new JPanel();
        
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
        mcBtn.setSelected(false);        
        buttonPane.add(mcBtn);
        
/*
        mcbBtn = new JCheckBox("MCB");
        mcbBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    isMCB = true;
                } else {
                    isMCB = false;
                };
            }
        });         
        mcbBtn.setSelected(false);        
        buttonPane.add(mcbBtn);  
*/        
        epicsBtn = new JCheckBox("EPICS");
        epicsBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    doEpics = true;
                    monitoringClass.initEpics(doEpics);
                } else {
                    doEpics = false;
                    monitoringClass.initEpics(doEpics);
                };
            }
        });         
        epicsBtn.setSelected(false);         
        buttonPane.add(epicsBtn);

        filterBtn = new JCheckBox("Filter");
        filterBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    isFilter = true;
                } else {
                    isFilter = false;
                };
            }
        });  
        
        filterBtn.setSelected(false);              
        buttonPane.add(filterBtn);
/*        
        ipcBtn = new JCheckBox("IPC");
        ipcBtn.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    isIPC = true;
                    ipc = new IpcServer("tcp://clondb3.jlab.org:61616","clasrun.*.*.*");
                } else {
                    isIPC = false;
                };
            }
        });         
        ipcBtn.setSelected(false);        
        buttonPane.add(ipcBtn);  
*/        
        openBtn = new JButton("Open HIPO");
        openBtn.addActionListener(this);
        buttonPane.add(openBtn);  
        
        closeBtn = new JButton("Close HIPO");
        closeBtn.addActionListener(this);
        buttonPane.add(closeBtn);   
        
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
        
        this.controlsPanel0.setBackground(Color.LIGHT_GRAY);
        this.controlsPanel1.setBackground(Color.LIGHT_GRAY);
        this.controlsPanel2.setBackground(Color.LIGHT_GRAY);
        this.controlsPanel3.setBackground(Color.LIGHT_GRAY);
		
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 0.5;
		
        c.gridx=0 ; c.gridy=0 ; this.controlsPanel0.add(this.controlsPanel1,c);
        c.gridx=0 ; c.gridy=1 ; this.controlsPanel0.add(this.controlsPanel2,c);
        c.gridx=0 ; c.gridy=2 ; this.controlsPanel0.add(this.controlsPanel3,c);
        
// Menu Bar  
        
        menuBar = new FCMenuBar();
        menuBar.setApplicationClass(this);
        this.setJMenuBar(menuBar);	
        		
// GUI layout
        
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
    
    public void addScrollPane(String name) {
        this.canvasTabbedPane.addTab(name, this.scrollPane);     
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
    
    public int getEventNumber() {
        return eventControl.currentEvent;
    }
    
    public int getTotalEventNumber() {
    	return evtno;
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
    
    public void startEpics() {
        epicsBtn.doClick();
    }
    
    public void setMaxEvents(int num) {
    	    this.maxEvents = num;
    }
    
    public void setTDCOffset(int num) {
	    this.tdcOffset = num;
    } 
    
    public void setPhaseOffset(int num) {
	    this.phaseOffset = num;
    } 
    
    public void setSelectedTab(int index) {
        this.canvasTabbedPane.setSelectedComponent(this.canvasTabbedPane.getComponent(index));
        System.out.println("monitor.setSelectedTab: Selected Tab is "+this.canvasTabbedPane.getTitleAt(index));
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
    
    public class TextAreaOutputStream extends OutputStream {
        private javax.swing.JTextArea jTextArea1;

        /**
         * Creates a new instance of TextAreaOutputStream which writes
         * to the specified instance of javax.swing.JTextArea control.
         *
         * @param textArea   A reference to the javax.swing.JTextArea
         *                  control to which the output must be redirected to.
         */
        public TextAreaOutputStream( JTextArea textArea ) {
            this.jTextArea1 = textArea;
        }

        public void write( int b ) throws IOException {
            jTextArea1.append( String.valueOf( ( char )b ) );
            jTextArea1.setCaretPosition(jTextArea1.getDocument().getLength());
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            jTextArea1.append(new String(cbuf, off, len));
            jTextArea1.setCaretPosition(jTextArea1.getDocument().getLength());
        }
    }
    
    public int getsp(DetectorDescriptor dd) {
        switch (appName) {
        case "ECMON":   return viewIndex+3*detectorIndex; 
        case "FTOFMON": return dd.getLayer();
        case "BANDMON": return dd.getLayer();
        case "HTCCMON": return dd.getLayer();
        case "CTOFMON": return viewIndex+2*detectorIndex;
        case "LTCCMON": return dd.getLayer();
        case "CNDMON":  return dd.getComponent()+1;
        }
        return 0;
    }
    
    public int getic(DetectorDescriptor dd) {
        switch (appName) {
        case "ECMON":   return dd.getComponent()+1; 
        case "FTOFMON": return dd.getComponent()+1;  
        case "BANDMON": return dd.getComponent()+1;  
        case "HTCCMON": return dd.getComponent()+1;  
        case "CTOFMON": return dd.getComponent()+1; 
        case "LTCCMON": return dd.getComponent()+1;
        case "CNDMON":  return dd.getLayer();
        }
        return 0;
    }  
    
    public String setAlias(int s, int l, int c, int o) {
    	this.detectorAlias = getAlias(s,l,c,o);
    	return this.detectorAlias;
    }
    
    public String getAlias(int s, int l, int c, int o) {
    	switch (appName) {
    	case "BANDMON": return BANDConstants.getAlias(s, l, c, o);
    	}
    	return "";
    }
    
    public void getMode7(int cr, int sl, int ch) {    
        mode7Emulation.configMode7(cr,sl,ch);
        this.nsa    = mode7Emulation.nsa;
        this.nsb    = mode7Emulation.nsb;
        this.tet    = mode7Emulation.tet;
        this.pedref = mode7Emulation.pedref;
     }
   
    public String getStatusString(DetectorDescriptor dd) {
        
        String comp=(dd.getLayer()==4) ? "  Pixel:":"  PMT:";  
      
        int is = dd.getSector();
        int sp = getsp(dd);        
        int ic = getic(dd);
        int or = dd.getOrder();
        int cr = 0;
        int sl = 0;
        int ch = 0;
        if (getSelectedTabName()=="TDC") or=or+2;
        
//        System.out.println(is+" "+sp+" "+ic+" "+or+" "+rtt.hasItem(is,sp,ic,or));
        
        if (rtt.hasItem(is,sp,ic,or)) {
            int[] dum = (int[]) rtt.getItem(is,sp,ic,or);
            currentCrate = cr = dum[0];  
            currentSlot  = sl = dum[1];  
            currentChan  = ch = dum[2]; 
        }   
        return " Sector:"+is+"  Layer:"+sp+comp+ic+" Order: "+or+"    Crate:"+cr+" Slot:"+sl+" Chan:"+ch+"    "+setAlias(is,sp,ic,or);
    }
    
    public void updateStatusString(DetectorDescriptor dd)  {
        this.statusLabel.setText(getStatusString(dd)) ; 
    }
    
 //   public abstract class HipoDataSync {
 //   	public abstract setCompressionType(int val);
 //   	public abstract open(String val);
 //   	
 //   }
    
    public void openHipoFile(String path) {               
        HipoFileName = path+"clas_00"+runNumber+".hipo";
        System.out.println("app.openHipoFile(): Opening "+HipoFileName);
        writer = new HipoDataSync();
        writer.open(HipoFileName);
        isHipoFileOpen = true;
    }
    
    public void closeHipoFile() {
        System.out.println("app.closeHipoFile(): Closing "+HipoFileName);
        writer.close();
        isHipoFileOpen = false;
    }  
    
    public void openHIPOAction() {
        openBtn.setOpaque(true);
        openBtn.setBackground(Color.GREEN);
        openHipoFile(hipoPath);        
    }
    
    public void closeHIPOAction() {
        openBtn.setBackground(Color.RED);
        eventControl.buttonStop.doClick();
        pause(2000);
        openBtn.setOpaque(false);
        openBtn.setBackground(Color.WHITE);
        closeHipoFile();  
    }
    
    public void pause(int msec) {
        try {
            Thread.sleep(msec);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    public void   setTriggerMask(int bit) {this.triggerMask|=(1<<bit);}  
    public void clearTriggerMask(int bit) {this.triggerMask&=~(1<<bit);}  
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().compareTo("Open HIPO")==0)    openHIPOAction();
        if(e.getActionCommand().compareTo("Close HIPO")==0)   closeHIPOAction();
        if(e.getActionCommand().compareTo("Clear Histos")==0) monitoringClass.reset();
        if(e.getActionCommand().compareTo("Write Histos")==0) monitoringClass.writeHipoFile();
        if(e.getActionCommand().compareTo("Read Histos")==0)  monitoringClass.readHipoFile();
        if(e.getActionCommand().compareTo("RUN")==0)           runNumber = runno.getText();
    }      
}
