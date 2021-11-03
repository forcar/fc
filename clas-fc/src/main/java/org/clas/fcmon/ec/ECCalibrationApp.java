package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.CalibrationConstantsView;
import org.clas.fcmon.tools.DisplayControl;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.clas.detector.DetectorEvent;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
//import org.jlab.clasrec.utils.DatabaseConstantProvider;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
//import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Axis;
import org.jlab.groot.ui.RangeSlider;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.utils.groups.IndexedTable;

public class ECCalibrationApp extends FCApplication implements CalibrationConstantsListener,ChangeListener {
    
    JPanel                    engineView = new JPanel();
    JSplitPane                enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
    CalibrationConstantsView      ccview = new CalibrationConstantsView("");
    
    JButton                   tableWrite = null;
    JButton                    tableSave = null;
    JButton                    tableRead = null;
    JButton                       hvSave = null;
    ConstantsManager                ccdb = new ConstantsManager();
    ArrayList<CalibrationConstants> list = new ArrayList<CalibrationConstants>(); 
    ECConstants                      ecc = new ECConstants();

    public ECCalibrationEngine[] engines = {
            new ECAttenEventListener(),
            new ECGainEventListener(),
            new ECStatusEventListener(),
            new ECHvEventListener()
    };

    public final int  ATTEN  = 0;
    public final int   GAIN  = 1;
    public final int STATUS  = 2;
    public final int     HV  = 3;
    
    String[] names = {"/calibration/ec/attenuation","/calibration/ec/gain","/calibration/ec/status","/calibration/ec/hv"};

    String selectedDir = names[HV];
       
    int selectedSector = 1;
    int selectedLayer = 1;
    int selectedPaddle = 1;
    
    boolean debug = false;
    
    Boolean[]   doIDET = {false,false,false};
    Boolean[] doSector = {false,false,false,false,false,false};
    int calrun = 1;
    
    public ECCalibrationApp(String name , ECPixels[] ecPix) {
        super(name, ecPix);       
     } 
    
    public void init() {        
        for (int i=0; i<engines.length; i++) engines[i].init(ecc.IS1,ecc.IS2); 
    }   
    
    public JPanel getPanel() {        
        engineView.setLayout(new BorderLayout());
        ccview.getTabbedPane().addChangeListener(this);  
        for (int i=0; i < engines.length; i++) {
            ccview.addConstants(engines[i].getCalibrationConstants().get(0),this);
        }   
        enginePane.setBottomComponent(ccview);       
        enginePane.setResizeWeight(0.95);
        engineView.add(enginePane);
        engineView.add(getButtonPane(),BorderLayout.PAGE_END);
        return engineView;       
    }  
    
    public JCheckBox getPCButton() {
        JCheckBox button = new JCheckBox("PC");
        button.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    doIDET[0]=true;
                } else {
                    doIDET[0]=false;
                };
            }
        });           
        button.setSelected(false);
        
        return button;
    	
    }
    
    public JCheckBox getECiButton() {
        JCheckBox button = new JCheckBox("ECi");
        button.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    doIDET[1] = true;  
                } else {
                    doIDET[1] = false;  
                };
            }
        });           
        button.setSelected(false);
        
        return button;    	
    }    
    public JCheckBox getECoButton() {
        JCheckBox button = new JCheckBox("ECo");
        button.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    doIDET[2]=true;
                } else {
                    doIDET[2]=false;
                };
            }
        });           
        button.setSelected(false);
        
        return button;    	
    }     
    public JCheckBox getSectorButton(int s) {
        JCheckBox button = new JCheckBox("S"+s);
        button.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    doSector[s-1] = true; 
                } else {
                    doSector[s-1] = false; 
                };
            }
        });           
        button.setSelected(false);
        
        return button;    	
    }     
    
    
    public JPanel getButtonPane(){
        buttonPane = new JPanel();
        tableWrite = new JButton("Write Table");
        tableWrite.addActionListener(this);
        tableWrite.setActionCommand("WRITE");
        tableSave  = new JButton("Save Table");
        tableSave.addActionListener(this);
        tableSave.setActionCommand("SAVE");
        tableRead  = new JButton("Restore Table");
        tableRead.addActionListener(this);
        tableRead.setActionCommand("RESTORE");        
        hvSave  = new JButton("Load HVnew");
        hvSave.addActionListener(this);
        hvSave.setActionCommand("LOADHV");
        for (int s=1; s<7 ; s++) buttonPane.add(getSectorButton(s));
        buttonPane.add(getPCButton());
        buttonPane.add(getECiButton());
        buttonPane.add(getECoButton());
        buttonPane.add(tableWrite);
        buttonPane.add(tableSave);
        buttonPane.add(tableRead);
        buttonPane.add(hvSave);
        return buttonPane;
    }  
    
    public void actionPerformed(ActionEvent e) {

       ECCalibrationEngine engine = getSelectedEngine();
       
       if (e.getActionCommand().compareTo("WRITE")==0) {           
           String outputFileName = engine.getFileName(app.runNumber);
           engine.writeDefaultTables(app.runNumber);
           JOptionPane.showMessageDialog(new JPanel(),
                   "Writing table " + engine.calib.getName() + " to "+outputFileName);
       }        
       if (e.getActionCommand().compareTo("SAVE")==0) {           
            String outputFileName = engine.getFileName(app.runNumber);
            engine.calib.save(outputFileName);
            JOptionPane.showMessageDialog(new JPanel(),
                    "Saving table " + engine.calib.getName() + " to "+outputFileName);
        }        
        if (e.getActionCommand().compareTo("RESTORE")==0) {           
            String outputFileName = engine.getFileName(app.runNumber);
            engine.updateTable(outputFileName);
            JOptionPane.showMessageDialog(new JPanel(),
                   "Restoring table " + engine.calib.getName() + " from "+outputFileName);
        }
        if (e.getActionCommand().compareTo("LOADHV")==0) {             
            int is1=sectorSelected;         int is2=is1+1;
            int il1=layerSelected+ilmap*3;  int il2=il1+1;
            mon.loadHV(is1,is2,il1,il2);
            JOptionPane.showMessageDialog(new JPanel(),
                    "Loading HV from "+engines[3].outputFileName+" to CAEN MAINFRAME");
        }        
    }
    
    public void setConstantsManager(ConstantsManager ccdb, int run) {
        this.ccdb = ccdb;
        this.calrun = run;
    }
    
    public ECCalibrationEngine getSelectedEngine() {
        
        ECCalibrationEngine engine = engines[HV];
        
        if (selectedDir == names[ATTEN]) {
            engine = engines[ATTEN];  
        } else if (selectedDir == names[GAIN]) {
            engine = engines[GAIN];  
        } else if (selectedDir == names[STATUS]) {
            engine = engines[STATUS];  
        } else if (selectedDir == names[HV]) {
            engine = engines[HV];  
        } 
        return engine;
    }
    
    public void analyze(int idet, int is1, int is2, int il1, int il2) {            
        if (il1>3) return;
        ECCalibrationEngine engine = getSelectedEngine();
        engine.analyze(idet,is1,is2,il1,il2,1,69);
    }   
    
    public void analyzeAllEngines(int is1, int is2, int il1, int il2) {
        for (int i=0; i<engines.length; i++) {
            for (int idet=0; idet<ecPix.length; idet++) engines[i].analyze(idet,is1,is2,il1,il2,1,69); 
        }
    }
    
    public void processAllEngines(DataEvent event) {
        for (int i=0; i<engines.length; i++) {
            for (int idet=0; idet<ecPix.length; idet++) engines[i].processEvent(event); 
        }    	
    }
   
    public void updateDetectorView(DetectorShape2D shape) {
        
        ECCalibrationEngine engine = getSelectedEngine();
        DetectorDescriptor dd = shape.getDescriptor();
        this.getDetIndices(dd);
        if (app.omap==3) {
           if(engine.isGoodPaddle(is, lay+ilmap*3, ic+1)) {
               shape.setColor(101, 200, 59);
           } else {
               shape.setColor(225, 75, 60);
           }
        }
    }
    
    public int getLay(int index) {
        int[] il = {1,2,3,1,2,3,1,2,3}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
        return il[index-1];
     } 
    
    public int getDet(int index) {
        int[] il = {1,1,1,2,2,2,3,3,3}; 
        return il[index-1];
     } 
    
    public String getLayName(int index) {
        String[] il = {"U","V","W"};  
        return il[index-1];
     } 
    
    public String getDetName(int index) {
        String[] il = {"PC","ECi","ECo"};  
        return il[index-1];
     }    
    
    public String getMapName(int index) {
         String[] il = {"U PIX","V PIX","W PIX"};  
         return il[index-1];
    } 
    
    public void constantsEvent(CalibrationConstants cc, int col, int row) {

        String str_sector    = (String) cc.getValueAt(row, 0);
        String str_layer     = (String) cc.getValueAt(row, 1);
        String str_component = (String) cc.getValueAt(row, 2);
        
        if (cc.getName() != selectedDir) {
            selectedDir = cc.getName();
        }
            
        selectedSector = Integer.parseInt(str_sector);
        selectedLayer  = Integer.parseInt(str_layer);
        selectedPaddle = Integer.parseInt(str_component);
        
//        DetectorDescriptor dd = new DetectorDescriptor();
//        dd.setSectorLayerComponent(selectedSector,getLay(selectedLayer),selectedPaddle-1);
        app.setFPS(0);
        
        app.getDetectorView().selectViewButton("DET", getDetName(getDet(selectedLayer)));
        
        if (app.getDetectorView().checkViewButton("LAY","PIX")) {
            app.getDetectorView().selectMapButton("PIX", getMapName(getLay(selectedLayer)));
        } else {
            app.getDetectorView().selectViewButton("LAY", getLayName(getLay(selectedLayer)));            
        }
        
        ECCalibrationEngine engine = getSelectedEngine();
        engine.drawPlots(selectedSector,getLay(selectedLayer),selectedPaddle-1);        
        
    }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
         sectorSelected = dd.getSector();
          layerSelected = dd.getLayer();
        channelSelected = dd.getComponent(); 
        
        ECCalibrationEngine engine = getSelectedEngine();
        this.getDetIndices(dd);
        engine.drawPlots(is,lay,ic);        
    }
        
        /*
        public void updateCanvas() {

            IndexedList<DataGroup> group = getSelectedEngine().getDataGroup();
            
            if(group.hasItem(selectedSector,selectedLayer,selectedPaddle)==true){
                DataGroup dataGroup = group.getItem(selectedSector,selectedLayer,selectedPaddle);
                this.canvas.draw(dataGroup);
                this.canvas.update();
            } else {
                System.out.println(" ERROR: can not find the data group");
            }
       
        }   
*/   
    public void stateChanged(ChangeEvent e) {
        int i = ccview.getTabbedPane().getSelectedIndex();
        String tabTitle = ccview.getTabbedPane().getTitleAt(i);
        if (tabTitle != selectedDir) {            
            selectedDir = tabTitle;
        }
        ECCalibrationEngine engine = getSelectedEngine(); 
        engine.setCalibPane(); 
        
    }

    // Calibration Apps
    
    private class ECHvEventListener extends ECCalibrationEngine {
        
        EmbeddedCanvasTabbed  ECHv = new EmbeddedCanvasTabbed("HV");
        public final double PARAM3 = 1.0;
        public final int    PARAM6 = 0;
        public final double[] GAIN = {0.2,0.2,0.2,0.5,0.5,0.5,0.5,0.5,0.5};
        public final int[]     DHV = {20,20,20,150,150,150,150,150,150};
        public final float[] HVEXP = {12,11,11};
        IndexedTable        status = null; 
        Boolean         isUseTable = false;
        
        ECHvEventListener(){}
        
        public void init(int is1, int is2){
            
            System.out.println("ECCalibrationApp:ECHvEventListener.init");
            
            fileNamePrefix = "EC_CALIB_HV";
            filePath       = app.hvPath;

            collection.clear();
            
            initCalibPane();
            setCalibPane();
            
            calib = new CalibrationConstants(3,"Gain/F:HVold/F:HVnew/F:DHV/F");
            calib.setName(names[HV]);
            calib.setPrecision(3);
            
            for (int i=0; i<9; i++) {
//                calib.addConstraint(3, PARAM3-GAIN[i], 
//                                       PARAM3+GAIN[i], 1, i+1);
//                calib.addConstraint(6, PARAM6-DHV[i], 
//                                       PARAM6+DHV[i], 1, i+1);
           }
 
            for(int is=is1; is<is2; is++) {                
                for(int idet=0; idet<ecPix.length; idet++) {
                    for (int il=0; il<3 ; il++) {
                        int layer = il+idet*3;
                        for(int ip = 0; ip < ecPix[idet].ec_nstr[il]; ip++) {
                            calib.addEntry(is, layer+1, ip+1);
                            calib.setDoubleValue(1., "Gain",  is, layer+1, ip+1);
                            calib.setDoubleValue(0., "HVold", is, layer+1, ip+1);
                            calib.setDoubleValue(0., "HVnew", is, layer+1, ip+1);
                            calib.setDoubleValue(0., "DHV",   is, layer+1, ip+1);
                        }
                    }
                }
            }            
            
            list.add(calib);
        }
        
        public void initCalibPane() {
            System.out.println("initCalibPane:ECHv");            
        }
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:ECHv");
            enginePane.setTopComponent(ECHv);         
        }
        
        @Override
        public synchronized void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
            
            if(isUseTable||!app.doEpics) return;
            
            for (int is=is1; is<is2; is++) {
                for (int il=il1; il<il2; il++) {
                    int iptst = ecPix[idet].ec_nstr[il-1]+1;
                    int ipmax = ip2>iptst ? iptst:ip2;
                    for (int ip=ip1; ip<ipmax; ip++) { 
                        int sl = 3*idet+il;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                        calib.addEntry(is, sl, ip);
                        calib.setDoubleValue(app.fifo1.get(is, sl, ip).getLast(), "HVold", is, sl, ip);                        
                    }
                }
            }
            
            
            CalibrationConstants gains = engines[0].calib;
            
            for (int is=is1; is<is2; is++) {
                for (int il=il1; il<il2; il++) {
                    int iptst = ecPix[idet].ec_nstr[il-1]+1;
                    int ipmax = ip2>iptst ? iptst:ip2;
                    for (int ip=ip1; ip<ipmax; ip++) { 
                        int sl = 3*idet+il;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                        double  gain = gains.getDoubleValue("A", is,sl,ip)+gains.getDoubleValue("C", is,sl,ip);
                        double hvold = calib.getDoubleValue("HVold", is,sl,ip);
                        calib.setDoubleValue(gain,"Gain", is, sl, ip);                        
                        if (gain<0.3||gain>12.0) gain=1.0;
                        double ratio=Math.pow(gain, 1./HVEXP[idet]);
                        double hvnew = (ratio>0.5) ? hvold/ratio:hvold;
                        if((hvnew-hvold)>100) {gain=1.0; hvnew=hvold;}
                        calib.setDoubleValue(hvnew,"HVnew", is, sl, ip);                           
                        calib.setDoubleValue(hvnew-hvold,"DHV", is, sl, ip);  
                        app.fifo6.get(is, sl, ip).add(hvnew);
                    }
                }
            }
            
            calib.fireTableDataChanged();     
        }
        
        @Override
        public void updateTable(String inputFile){
            
            System.out.println("ECHVListener:updateTable()");
            if(inputFile==null) inputFile = getFileName(app.runNumber);
            
            isUseTable = true; //disable analysis method
            
            int is,il,ip;
            double dum;
            
            try {                 
                FileInputStream fstream = new FileInputStream(inputFile);
                BufferedReader       br = new BufferedReader(new InputStreamReader(fstream));

                String line = br.readLine();
                
                while (line != null) {

                    String[] lineValues = line.trim().split("\\s+");
                    
                    is  = Integer.parseInt(lineValues[0]);
                    il  = Integer.parseInt(lineValues[1]);
                    ip  = Integer.parseInt(lineValues[2]);
                    if (calib.hasEntry(is,il,ip)) {
                      dum = Double.parseDouble(lineValues[3]);  calib.setDoubleValue(dum,"Gain",  is,il,ip);
                      dum = Double.parseDouble(lineValues[4]);  calib.setDoubleValue(dum,"HVold", is,il,ip);
                      dum = Double.parseDouble(lineValues[5]);  calib.setDoubleValue(dum,"HVnew", is,il,ip);
                      app.fifo6.get(is, il, ip).add(dum);
                      dum = Double.parseDouble(lineValues[6]);  calib.setDoubleValue(dum,"DHV",   is,il,ip);
                    }
                    line = br.readLine();                    
                }
                br.close();            
            }
            catch(FileNotFoundException ex) {
                ex.printStackTrace();                
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }            
            
        }
        
        public synchronized void drawPlots(int is, int il, int ic) {
            if (il>3) return;
            if (app.getInProcess()<2) analyze(ilmap,is,is+1,il,il+1,1,69);
            EmbeddedCanvas  c = new EmbeddedCanvas(); 
            int nstr = ecPix[ilmap].ec_nstr[il-1];
            int   sl = il+ilmap*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9            
            H1F hvold = new H1F(" ",nstr,1.,nstr+1);
            H1F hvnew = new H1F(" ",nstr,1.,nstr+1);
            H1F   dhv = new H1F(" ",nstr,1.,nstr+1);
            hvold.setFillColor(33);
            hvnew.setFillColor(32);
              dhv.setFillColor(32);
            for (int ip=1; ip<nstr+1; ip++) hvold.fill(ip,app.fifo1.get(is, sl, ip).getLast());
            for (int ip=1; ip<nstr+1; ip++) hvnew.fill(ip,calib.getDoubleValue("HVnew", is, sl,ip));
            for (int ip=1; ip<nstr+1; ip++)   dhv.fill(ip,calib.getDoubleValue("DHV",   is, sl,ip));
             
            c = ECHv.getCanvas("HV"); 
            c.divide(2,2);
            c.getPad(0).getAxisY().setRange((double)getMin(hvnew.getData())-10,(double)getMax(hvnew.getData())+10);
            hvold.getAttributes().setTitleX("PMT"); hvold.getAttributes().setTitleY("HV");
            c.cd(0); c.draw(hvold); c.draw(hvnew,"same");
            c.getPad(1).getAxisY().setRange((double)getMin(dhv.getData())-10,(double)getMax(dhv.getData())+10);
            dhv.getAttributes().setTitleX("PMT"); dhv.getAttributes().setTitleY("Delta HV");
            c.cd(1); c.draw(dhv);
            
            H1F h = null;
            h = hvnew.histClone("Copy"); h.reset() ; h.setBinContent(ic, 2000);
            h.setFillColor(2);  c.cd(0); c.draw(h,"same");
            
            h = dhv.histClone("Copy"); h.reset() ; h.setBinContent(ic, 2000);
            h.setFillColor(2);  c.cd(1); c.draw(h,"same");
            
            if (app.getInProcess()>0)  {
                if (app.getInProcess()==1&&app.getIsRunning()) engines[0].analyze(ilmap,is,is+1,il,il+1,1,69);
                if (engines[0].collection.hasEntry(is, sl, ic+1)) {
                    c.getPad(2).getAxisX().setRange(0.,400.); c.getPad(2).getAxisY().setRange(0.,300.);
                    if(engines[0].collection.get(is,sl,ic+1).getFitGraph(0).getDataSize(0)>0) {
                        c.getPad(2).getAxisX().setRange(0.,400.); c.getPad(2).getAxisY().setRange(0.,300.);
                        c.cd(2); c.draw(engines[0].collection.get(is,sl,ic+1).getFitGraph(0));
                    }
                }
            }
            c.repaint();                                    
        }

    }  
  
    public class ECAttenEventListener extends ECCalibrationEngine implements ActionListener {
        
        JSplitPane              hPane = null; 
        EmbeddedCanvasTabbed   fitPix = new EmbeddedCanvasTabbed("Pixel Fits");
        EmbeddedCanvasTabbed   fitADC = new EmbeddedCanvasTabbed("ADC");
        EmbeddedCanvasTabbed   fitCh2 = new EmbeddedCanvasTabbed("Chi^2");
        EmbeddedCanvasTabbed   fitCof = new EmbeddedCanvasTabbed("COEF");
        CalibrationConstants    cctab = null;
        IndexedTable            atten = null; 
        IndexedTable             gain = null; 
        RangeSlider            slider = null;
        
        public final double[][] PARAM = {{1,1,1,1,1,1,1,1,1},
                                         {360,360,360,360,360,360,360,360,360},
                                         {0.25,0.25,0.25,0,0,0,0,0,0}};
        public final double[][] DELTA = {{0.25,0.25,0.25,0.2,0.2,0.2,0.2,0.2,0.2},
                                         {160,160,160,160,160,160,160,160,160},
                                         {0.25,0.25,0.25,0,0,0,0,0,0}};
        
        int is1,is2;
        int nstr=68;
        
        Boolean   isPix = false;
        Boolean   isStr = false;
        int    pixStrip = 1;
        
        double      xSliderMin =   0.0;
        double      xSliderMax = 100.0;
        double currentRangeMin =   0.0;
        double currentRangeMax = 100.0;
        
        String sliderMode = "Strip";
        
        double[] xp     = new double[nstr];
        double[] xpe    = new double[nstr];
        double[] parA   = new double[nstr];
        double[] parAe  = new double[nstr];
        double[] parB   = new double[nstr];
        double[] parBe  = new double[nstr];
        double[] parC   = new double[nstr];
        double[] parCe  = new double[nstr];
        double[] parAC  = new double[nstr];
        double[] parACe = new double[nstr];
        double[] vchi2  = new double[nstr];
        double[] vchi2e = new double[nstr]; 
        double[] ccdbA  = new double[nstr];
        double[] ccdbB  = new double[nstr];
        double[] ccdbC  = new double[nstr];
        double[] ccdbAC = new double[nstr];
        double[] ccdbAe = new double[nstr];
        double[] ccdbBe = new double[nstr];
        double[] ccdbCe = new double[nstr];
        double[] ccdbACe= new double[nstr];
        double[] ccdbGAIN  = new double[nstr];
        double[] ccdbGAINe = new double[nstr];
        
        double[] vgain  = new double[nstr];
        double[] vgaine = new double[nstr]; 
        double[] vatt   = new double[nstr];
        double[] vatte  = new double[nstr]; 
        double[] vattdb = new double[nstr];
        double[] vattdbe= new double[nstr];
        double[] xpix   = new double[1];
        double[] ypix   = new double[1];
        double[] xerr   = new double[1];
        double[] yerr   = new double[1];
        
        ECAttenEventListener(){}
        
        public void init(int is1, int is2) {
            System.out.println("ECCalibrationApp:ECAttenEventListener.init");
            
            fileNamePrefix ="EC_CALIB_ATTEN";
            filePath= app.calibPath;
            
            getButtonGroup();
            getSliderPane();
            initCalibPane();
            setCalibPane();

            collection.clear();
            
            this.is1=is1;
            this.is2=is2;
            
            atten   = ccdb.getConstants(calrun, names[ATTEN]);
            gain    = ccdb.getConstants(calrun, names[GAIN]);

            makeNewTable(is1,is2);

        }
               
        public void initCalibPane() {
            System.out.println("initCalibPane:ECAtten");
            hPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
            JSplitPane   vPaneL = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
            JSplitPane   vPaneR = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
            hPane.setLeftComponent(vPaneL);
            hPane.setRightComponent(vPaneR);
            vPaneL.setTopComponent(fitPix);
            vPaneL.setBottomComponent(fitADC);
            vPaneR.setTopComponent(fitCh2);
            vPaneR.setBottomComponent(fitCof);
            hPane.setResizeWeight(0.5);
            vPaneL.setResizeWeight(0.7);
            vPaneR.setResizeWeight(0.25);
        }
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:ECAtten");
            enginePane.setTopComponent(hPane);                        
        }
        
        public void getButtonGroup() {
            JRadioButton bGv = new JRadioButton("View"); 
            bGv.setActionCommand("View");
            
            JRadioButton bGs = new JRadioButton("Strip");
            bGs.setActionCommand("Strip"); 
            bGs.setSelected(true); 
            
            ButtonGroup  bG  = new ButtonGroup();
            bG.add(bGv);
            bG.add(bGs);
            
            bGv.addActionListener(this);  
            bGs.addActionListener(this);   
            
            fitPix.actionPanel.add(bGv);
            fitPix.actionPanel.add(bGs);
        }
        
        public void getSliderPane() {
           JLabel xLabel = new JLabel("X:");
           slider = new RangeSlider();
           slider.setMinimum((int) xSliderMin);
           slider.setMaximum((int) xSliderMax);
           slider.setValue((int) xSliderMin);
           slider.setUpperValue((int) xSliderMax);            
           currentRangeMin = slider.getValue();
           currentRangeMax = slider.getUpperValue();
           JLabel rangeSliderValue1 = new JLabel("" + String.format("%4.1f", currentRangeMin));
           JLabel rangeSliderValue2 = new JLabel("" + String.format("%4.1f", currentRangeMax));
           fitPix.actionPanel.add(xLabel);
           fitPix.actionPanel.add(rangeSliderValue1);
           fitPix.actionPanel.add(slider);
           fitPix.actionPanel.add(rangeSliderValue2);           
           slider.addChangeListener(new ChangeListener() {
               public void stateChanged(ChangeEvent e) {
                   RangeSlider slider = (RangeSlider) e.getSource();
                   currentRangeMin = slider.getValue();
                   currentRangeMax = slider.getUpperValue();
                   rangeSliderValue1.setText(String.valueOf("" + String.format("%4.1f", currentRangeMin)));
                   rangeSliderValue2.setText(String.valueOf("" + String.format("%4.1f", currentRangeMax)));
                   int il = isPix ? lay-10:lay;
                   int ip1=1,ip2=1;
                   if (sliderMode=="Strip") {ip1=pixStrip; ip2=pixStrip+1;}
                   if (sliderMode=="View")  {ip1=1;        ip2=ecPix[ilmap].ec_nstr[il-1];}
                   for (int ip=ip1; ip<ip2; ip++) {
                      calib.setDoubleValue(currentRangeMin*0.01,"FitMin",is, 3*ilmap+il, ip);
                      calib.setDoubleValue(currentRangeMax*0.01,"FitMax",is, 3*ilmap+il, ip);
                   }
                   
                  
                   calib.fireTableDataChanged();       
                  
                   analyze(ilmap,sectorSelected,sectorSelected+1,il,il+1,ip1,ip2); 
                                   
                   drawPlots(sectorSelected,layerSelected,channelSelected);
                   
               }
           });   
       } 
        
        public void makeNewTable(int is1, int is2) {
            
            calib = new CalibrationConstants(3,"A/F:Aerr/F:B/F:Berr/F:C/F:Cerr/F:FitMin/F:FitMax/F");
            calib.setName(names[ATTEN]);
            calib.setPrecision(3);
            
            int kk=0;
            for (int k=0; k<1; k++) {
            for (int i=0; i<9; i++) {  
                calib.addConstraint(kk+3,PARAM[k][i]-DELTA[k][i], 
                                         PARAM[k][i]+DELTA[k][i], 1, i+1);
            }
            kk=kk+2;
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int idet=0; idet<ecPix.length; idet++) {
                    for (int il=1; il<4 ; il++) {
                        int sl = il+idet*3;
                        for(int ip = 1; ip < ecPix[idet].ec_nstr[il-1]+1; ip++) {
                            calib.addEntry(is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("A",    is,sl,ip), "A",     is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("Aerr", is,sl,ip), "Aerr",  is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("B",    is,sl,ip), "B",     is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("Berr", is,sl,ip), "Berr",  is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("C",    is,sl,ip), "C",     is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("Cerr", is,sl,ip), "Cerr",  is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("FitMin", is,sl,ip), "FitMin",is, sl, ip);
                            calib.setDoubleValue(atten.getDoubleValue("FitMax", is,sl,ip), "FitMax",is, sl, ip);
                        }
                    }
                }
            }
            list.add(calib);  
        }
        
        @Override
        public void writeDefaultTables(String runno) {
          System.out.println("Creating EC_CALIB_ATTEN tables for "+runno);
          makeNewTable(1,7);         
          updateTable(app.calibPath+"EC_CALIB_ATTEN_s0_"+runno);   
          calib.save(app.calibPath+"EC_CALIB_ATTEN_r"+runno);   
        }
        
        public void createDefaultTable(int run, double att)  {
            
            makeNewTable(1,7);
            
            for(int is=1; is<7; is++) {                
                for(int idet=0; idet<ecPix.length; idet++) {
                    for (int il=1; il<4 ; il++) {
                        int sl = il+idet*3;
                        for(int ip = 1; ip < ecPix[idet].ec_nstr[il-1]+1; ip++) {                   
                            calib.setDoubleValue(1.00, "A",     is, sl, ip);
                            calib.setDoubleValue(att,  "B",     is, sl, ip);
                            calib.setDoubleValue(0.00, "C",     is, sl, ip);                            
                            calib.setDoubleValue(0.00, "Aerr",  is, sl, ip);
                            calib.setDoubleValue(0.00, "Berr",  is, sl, ip);
                            calib.setDoubleValue(0.00, "Cerr",  is, sl, ip);
                            calib.setDoubleValue(0.04, "FitMin",is, sl, ip);
                            calib.setDoubleValue(0.95, "FitMax",is, sl, ip);
                        }
                    }
                }
            }  
            calib.save(app.calibPath+"EC_CALIB_ATTEN_r"+run);
        }
        
        @Override
        public void updateTable(String inputFile){
            
            System.out.println("ECCalibrationApp:ECAttenEventListener.updateTable");
            
            if(inputFile==null) inputFile = getFileName(app.runNumber);
             
            int is,il,ip;
            double dum;
            
            try {                 
                FileInputStream fstream = new FileInputStream(inputFile);
                BufferedReader       br = new BufferedReader(new InputStreamReader(fstream));

                String line = br.readLine();
                
                while (line != null) {

                	String[] lineValues = line.trim().split("\\s+");
                    
                    is  = Integer.parseInt(lineValues[0]);
                    il  = Integer.parseInt(lineValues[1]);
                    ip  = Integer.parseInt(lineValues[2]);
                    
                    if (calib.hasEntry(is,il,ip)&&doIDET[getDet(il)-1]) {
                        double A = Double.parseDouble(lineValues[3]);
                        double C = Double.parseDouble(lineValues[7]);
                        if (A==0) A=1.0;
                        double sca = A+C;
                        calib.setDoubleValue(A/sca,"A",     is,il,ip);
                        dum = Double.parseDouble(lineValues[4]);  calib.setDoubleValue(dum/sca,"Aerr",  is,il,ip);
                        dum = Double.parseDouble(lineValues[5]);  calib.setDoubleValue(dum,"B",         is,il,ip);
                        dum = Double.parseDouble(lineValues[6]);  calib.setDoubleValue(dum,"Berr",      is,il,ip);
                        calib.setDoubleValue(C/sca,"C",     is,il,ip);
                        dum = Double.parseDouble(lineValues[8]);  calib.setDoubleValue(dum/sca,"Cerr",  is,il,ip);
                        dum = Double.parseDouble(lineValues[9]);  calib.setDoubleValue(dum,"FitMin",    is,il,ip);
                        dum = Double.parseDouble(lineValues[10]); calib.setDoubleValue(dum,"FitMax",    is,il,ip);
                    }
                    
                    line = br.readLine();                    
                }
                br.close();            
            }
            catch(FileNotFoundException ex) {
                ex.printStackTrace();                
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }            
            
        }
        
        public CalibrationConstants getCalibTable() {
            return calib;
        }

        
        @Override
        public void analyze() {
            for (int sector = is1; sector < is2; sector++) {
                for (int layer = 1; layer < 4; layer++) {
                    for (int paddle = 1; paddle<NUM_PADDLES[layer-1]+1; paddle++) {
                        fit(sector, layer, paddle, 0.0, 0.0);
                    }
                }
            }
            calib.fireTableDataChanged();
        }
        
        @Override
        public void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
            
//        	String arg = idet+" "+is1+" "+is2+" "+il1+" "+il2+" "+ip1+" "+ip1;
//            System.out.println("ECCalibrationApp:ECAttenEventListener.analyze"+arg);
            
            TreeMap<Integer, Object> map;
            boolean doCalibration=false;
            int npix = ecPix[idet].pixels.getNumPixels();
            double  meanerr[] = new double[npix];
            boolean  status[] = new boolean[npix];
            int a = layer;     
            
            for (int is=is1 ; is<is2 ; is++) {
               for (int il=il1 ; il<il2 ; il++) { //Extract pixel maps for each U,V,W layer
                  //Extract raw arrays for error bar calculation
                  float  cnts[] = ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is,il,4).getData();                
                  float   adc[] = ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is,il,1).getData();
                  float adcsq[] = ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is,il,3).getData();
                  
                  doCalibration = false;
                      
                  for (int ipix=0 ; ipix<npix ; ipix++) {
                     meanerr[ipix]=0;
                     if (cnts[ipix]>1) {
                        meanerr[ipix]=Math.sqrt((adcsq[ipix]-adc[ipix]*adc[ipix]-8.3)/(cnts[ipix]-1)); //Sheppard's correction: c^2/12 c=10
                        doCalibration = true;
                     }                
                     if (cnts[ipix]==1) {
                        meanerr[ipix]=8.3;
                        doCalibration = true;
                     }
                        status[ipix] = ecPix[idet].pixels.getPixelStatus(ipix+1);
                  }
                      
                  map = (TreeMap<Integer, Object>) ecPix[idet].Lmap_a.get(is,il+10,0);
                  float  meanmap[] = (float[]) map.get(1);
                  double distmap[] = (double[]) ecPix[idet].pixels.getDist(il);
                  int    sl = il+idet*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                  int iptst = ecPix[idet].ec_nstr[il-1]+1;
                  int ipmax = ip2>iptst ? iptst:ip2;
                  
                  double scale = (app.isMC) ? ecc.MIP[sl-1]:ecc.REF[sl-1]; 
                  
                  for (int ip=ip1 ; ip<ipmax ; ip++) { //Loop over strips
                     if (doCalibration) {
                         
                        CalibrationData fits = new CalibrationData(is, sl, ip);
                        fits.getDescriptor().setType(DetectorType.ECAL);
                        if(ip==36||ip==68||ip==62) fits.ignorePixelStatus();
                        if(ip<4)                   fits.ignoreLoPixelStatus();
                        fits.addGraph(ecPix[idet].strips.getpixels(il,ip,cnts),
                                      ecPix[idet].strips.getpixels(il,ip,distmap),
                                      ecPix[idet].strips.getpixels(il,ip,meanmap),
                                      ecPix[idet].strips.getpixels(il,ip,meanerr),
                                      ecPix[idet].strips.getpixels(il,ip,status));
                        fits.setFitLimits(calib.getDoubleValue("FitMin",is,sl,ip),
                                          calib.getDoubleValue("FitMax",is,sl,ip));
                        
                        if(ip<8) fits.fitGainsOnly(true);
                        
                        fits.analyze(idet,scale);
                        
                        if(doIDET[idet]&&doSector[is-1]) {
                            calib.setDoubleValue(fits.getFunc(0).parameter(0).value()/scale, "A",    is, sl, ip);
                            calib.setDoubleValue(fits.getFunc(0).parameter(1).value(),       "B",    is, sl, ip);
                            calib.setDoubleValue(fits.getFunc(0).parameter(2).value()/scale, "C",    is, sl, ip);
                            calib.setDoubleValue(fits.getFunc(0).parameter(0).error()/scale, "Aerr", is, sl, ip);
                            calib.setDoubleValue(fits.getFunc(0).parameter(1).error(),       "Berr", is, sl, ip);
                            calib.setDoubleValue(fits.getFunc(0).parameter(2).error()/scale, "Cerr", is, sl, ip);
                        }
                     
                        collection.add(fits.getDescriptor(),fits);
                     }
                  }
               }
            }
            calib.fireTableDataChanged();              
         } 
        
        public double getTestChannel(int sector, int layer, int paddle) {
            double A = calib.getDoubleValue("A", sector, layer, paddle);
            double C = calib.getDoubleValue("C", sector, layer, paddle);            
            return A+C;
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {

            return (getTestChannel(sector,layer,paddle) >=PARAM[0][layer-1]-DELTA[0][layer-1]  &&
                    getTestChannel(sector,layer,paddle) <=PARAM[0][layer-1]+DELTA[0][layer-1] );

        }    
              
        @Override
        public void drawPlots(int iss, int layer, int ic) {
            
            DetectorCollection<H2F>            dc2a = ecPix[ilmap].strips.hmap2.get("H2_a_Hist");    
            EmbeddedCanvas                        c = new EmbeddedCanvas();    
            H1F                              pixADC = null;
            H2F                              pixATT = null;
            int                                  il = 0;
            int                                nstr = ecPix[0].ec_nstr[0];
             
            String otab[][]={{" U PMT "," V PMT "," W PMT "},
                    {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
                    {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
                      
            isPix = layer > 10;
            isStr = layer <  7;  
            il    = layer;
                                        
            GStyle.getGraphErrorsAttributes().setMarkerStyle(1);
            GStyle.getGraphErrorsAttributes().setMarkerColor(2);
            GStyle.getGraphErrorsAttributes().setMarkerSize(3);
            GStyle.getGraphErrorsAttributes().setLineColor(2);
            GStyle.getGraphErrorsAttributes().setLineWidth(1);
            GStyle.getGraphErrorsAttributes().setFillStyle(1);  
            
            if (isPix) {
               float meanmap[] = (float[]) ecPix[ilmap].Lmap_a.get(iss, layer, 0).get(1);
               il = layer-10;
               xpix[0] = ecPix[ilmap].pixels.getDist(il, ic+1);
               ypix[0] = meanmap[ic];
               xerr[0] = 0.;
               yerr[0] = 0.;
               pixStrip = ecPix[ilmap].pixels.getStrip(il,ic+1);
                 pixADC = dc2a.get(iss,il,2).sliceY(ic) ; pixADC.setFillColor(2);
                 pixADC.setTitleX("Sector "+iss+otab[ilmap][il-1]+pixStrip+" Pixel "+(ic+1)+" ADC");
            }
            
            if (isStr) {
                pixStrip = ic+1;
                pixADC = dc2a.get(iss,il,0).sliceY(ic) ; pixADC.setFillColor(2);                
                pixADC.setTitleX("Sector "+iss+otab[ilmap][il-1]+pixStrip+" Strip "+(ic+1)+" ADC");
            }           
                        
            if (isStr||isPix) {
               if (app.getInProcess()>0) {
                   
                     pixATT = ecPix[ilmap].strips.getpixels(il, pixStrip, dc2a.get(iss,il,2));
                     pixATT.setTitleX("Sector "+iss+otab[ilmap][il-1]+pixStrip+" Pixel No.");
                     pixATT.setTitleY("ADC");                            
                     nstr = ecPix[ilmap].ec_nstr[il-1];
                     if (app.getInProcess()==1&&app.getIsRunning())  {analyze(ilmap,iss,iss+1,il,il+1,1,69);}
                     int sl = il+ilmap*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                     if (collection.hasEntry(iss, sl, pixStrip)) {
//                     slider.setValue(     (int) (fit.get(is,il,pixStrip).fitLimits[0]*100));
//                     slider.setUpperValue((int) (fit.get(is,il,pixStrip).fitLimits[1]*100));            

                     double scale = (app.isMC) ? ecc.MIP[sl-1]:ecc.REF[sl-1]; 
                     
                     for (int ip=0; ip<nstr ; ip++) {
                         xp[ip]    = ip+1;     
                         xpe[ip]   = 0.; 
                         
                         parA[ip]  = collection.get(iss,sl,ip+1).getFunc(0).parameter(0).value()/scale;
                         parAe[ip] = collection.get(iss,sl,ip+1).getFunc(0).parameter(0).error()/scale;
                         parB[ip]  = collection.get(iss,sl,ip+1).getFunc(0).parameter(1).value();
                         parBe[ip] = collection.get(iss,sl,ip+1).getFunc(0).parameter(1).error();
                         parC[ip]  = collection.get(iss,sl,ip+1).getFunc(0).parameter(2).value()/scale;
                         parCe[ip] = collection.get(iss,sl,ip+1).getFunc(0).parameter(2).error()/scale;
                         parAC[ip] = parA[ip]+parC[ip];
                         parACe[ip]= Math.sqrt(parAe[ip]*parAe[ip]+parCe[ip]*parCe[ip]);
                         double chi2 = collection.get(iss,sl,ip+1).getFunc(0).getChiSquare()/
                                       collection.get(iss,sl,ip+1).getFunc(0).getNDF();
                         vchi2[ip] = Math.min(4, chi2); 
                         vchi2e[ip]= 0.;                                                          
                         ccdbA[ip] = atten.getDoubleValue("A",iss,sl,ip+1);
                         ccdbB[ip] = atten.getDoubleValue("B",iss,sl,ip+1);
                         ccdbC[ip] = atten.getDoubleValue("C",iss,sl,ip+1);
                         int gsca = 1;
                         if (ilmap==0) gsca = 15;
                         if (ilmap>0)  gsca = (iss==5) ? 5:10;
                         ccdbGAIN[ip]  = 1./(gain.getDoubleValue("gain",iss,sl,ip+1)*gsca); //2019: CLAS12ANA.ECmip
                         ccdbGAINe[ip] = gain.getDoubleValue("gainErr",iss,sl,ip+1)*gsca/(ccdbGAIN[ip]*ccdbGAIN[ip]);
                         ccdbAC[ip]= ccdbA[ip]+ccdbC[ip];
                         ccdbAe[ip]  = 0.;
                         ccdbBe[ip]  = 0.;
                         ccdbCe[ip]  = 0.;
                         ccdbACe[ip] = 0.;
                     }
                     
                     GraphErrors   pixGraph = new GraphErrors("pix",xpix,ypix,xerr,yerr);
                     
                     GStyle.getGraphErrorsAttributes().setLineColor(3);
                     GStyle.getGraphErrorsAttributes().setLineWidth(2);
                     GStyle.getGraphErrorsAttributes().setMarkerColor(3);
                     int icc = pixStrip-1;
                     xpix[0]=xp[icc]; ypix[0]=vchi2[icc]; yerr[0]=vchi2e[icc];
                     GraphErrors  chi2Graphi = new GraphErrors("chi",xpix,ypix,xerr,yerr);
                     xpix[0]=xp[icc]; ypix[0]=parA[icc]; yerr[0]=parAe[icc];
                     GraphErrors  parAGraphi = new GraphErrors("Ai",xpix,ypix,xerr,yerr);
                     xpix[0]=xp[icc]; ypix[0]=parB[icc]; yerr[0]=parBe[icc];
                     GraphErrors  parBGraphi = new GraphErrors("Bi",xpix,ypix,xerr,yerr);
                     xpix[0]=xp[icc]; ypix[0]=parC[icc]; yerr[0]=parCe[icc];
                     GraphErrors  parCGraphi = new GraphErrors("Ci",xpix,ypix,xerr,yerr);
                     xpix[0]=xp[icc]; ypix[0]=parAC[icc]; yerr[0]=parACe[icc];
                     GraphErrors  parACGraphi = new GraphErrors("A+Ci",xpix,ypix,xerr,yerr);
                     
                     GStyle.getGraphErrorsAttributes().setLineColor(2);
                     GStyle.getGraphErrorsAttributes().setLineWidth(1);
                     GStyle.getGraphErrorsAttributes().setMarkerColor(2);
                     GraphErrors  chi2Graph = new GraphErrors("chi2",xp,vchi2,xpe,vchi2e);
                     GraphErrors  parAGraph = new GraphErrors("A",xp,parA,xpe,parAe); 
                     GraphErrors  parBGraph = new GraphErrors("B",xp,parB,xpe,parBe); 
                     GraphErrors  parCGraph = new GraphErrors("C",xp,parC,xpe,parCe); 
                     GraphErrors parACGraph = new GraphErrors("A+C",xp,parAC,xpe,parACe); 
                     GStyle.getGraphErrorsAttributes().setMarkerColor(1);
                     GStyle.getGraphErrorsAttributes().setFillStyle(0);
                     GraphErrors ccdbAGraph = new GraphErrors("dbA",xp,ccdbA,xpe,ccdbAe); 
                     GraphErrors ccdbBGraph = new GraphErrors("dbB",xp,ccdbB,xpe,ccdbBe); 
                     GraphErrors ccdbCGraph = new GraphErrors("dbC",xp,ccdbC,xpe,ccdbCe); 
                     GStyle.getGraphErrorsAttributes().setLineColor(1);
                     GraphErrors ccdbGAINGraph = new GraphErrors("dbGAIN",xp,ccdbGAIN,xpe,ccdbGAINe);
                     GStyle.getGraphErrorsAttributes().setLineColor(2);
                     GStyle.getGraphErrorsAttributes().setMarkerColor(2);
                     GStyle.getGraphErrorsAttributes().setFillStyle(2);
                       
                     ccdbAGraph.setTitleX(otab[ilmap][il-1])    ; ccdbAGraph.setTitleY("A")  ; //2019: ECmon
                     ccdbBGraph.setTitleX(otab[ilmap][il-1])    ; ccdbBGraph.setTitleY("B")  ; //2019: ECmon
                     ccdbCGraph.setTitleX(otab[ilmap][il-1])    ; ccdbCGraph.setTitleY("C")  ; //2019: ECmon
                     ccdbGAINGraph.setTitleX(otab[ilmap][il-1]) ; ccdbGAINGraph.setTitleY("A+C")  ; //2019: CLAS12ANA.ECmip
                     
                     chi2Graph.setTitleX(otab[ilmap][il-1]) ;  
                     chi2Graph.setTitleY("REDUCED CHI^2"); 
                     chi2Graph.setTitle(" ");
                      
                     F1D f1 = new F1D("p0","[a]",0,nstr+1); f1.setParameter(0,scale); f1.setLineWidth(1);
                               
                     // Start plotting results
                     
                     c = fitPix.getCanvas("Pixel Fits"); 
                     
                     c.getPad(0).getAxisX().setRange(0.,400.);c.getPad(0).getAxisY().setRange(0.,300.); 
                     c.draw(collection.get(iss,sl,pixStrip).getRawGraph(0));  
                     if(collection.get(iss,sl,pixStrip).getFitGraph(0).getDataSize(0)>0) {
                         c.draw(collection.get(iss,sl,pixStrip).getFitGraph(0),"same");                                                 
                     }
                     if (isPix) c.draw(pixGraph,"same");
                     c.repaint();

                     c = fitADC.getCanvas("ADC");  c.divide(2,1);
                     
                     c.cd(0); pixADC.setOptStat(Integer.parseInt("1000100")); pixADC.setTitle(""); c.draw(pixADC); 
                     c.cd(1); c.getPad(1).getAxisZ().setLog(true); c.draw(pixATT);
                     c.repaint();
                     
                     c = fitCh2.getCanvas("Chi^2"); 
                     
                     double xmax = ecPix[ilmap].ec_nstr[il-1]+1;
                     
                     c.getPad(0).getAxisX().setRange(0.,xmax);c.getPad(0).getAxisY().setRange(0.,4.);
                     c.draw(chi2Graph) ; c.draw(chi2Graphi,"same");
                     c.repaint();
                     
                     c = fitCof.getCanvas("COEF"); c.divide(2,2);
                     
                     F1D f2 = new F1D("p0","[a]",0,nstr+1); f2.setParameter(0,1); f2.setLineWidth(1); f2.setLineColor(1);
                     
                     c.cd(0); c.getPad(0).getAxisX().setRange(0.,xmax);c.getPad(0).getAxisY().setRange(0.,2.);
                     c.draw(ccdbAGraph) ; c.draw(parAGraph,"same"); c.draw(parAGraphi,"same"); 
                     c.cd(1); c.getPad(1).getAxisX().setRange(0.,xmax);c.getPad(1).getAxisY().setRange(0.5,600.);
                     c.draw(ccdbBGraph); c.draw(parBGraph,"same"); c.draw(parBGraphi,"same");                       
                     c.cd(2); c.getPad(2).getAxisX().setRange(0.,xmax);c.getPad(2).getAxisY().setRange(0.,2.);
                     c.draw(ccdbCGraph) ; c.draw(parCGraph,"same"); c.draw(parCGraphi,"same");
                     c.cd(3); c.getPad(3).getAxisX().setRange(0.,xmax);c.getPad(3).getAxisY().setRange(0.5,1.5);
                     c.draw(ccdbGAINGraph) ; c.draw(parACGraph,"same"); c.draw(parACGraphi,"same"); 
                     c.draw(f2,"same");
                     
                     c.repaint();
                      
                  }
               }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
          sliderMode = e.getActionCommand();
        }

    }
    
    public class ECGainEventListener extends ECCalibrationEngine {
        
        EmbeddedCanvasTabbed  ECGain = new EmbeddedCanvasTabbed("Gain");
        EmbeddedCanvas               c = new EmbeddedCanvas(); 

        public final double[]   PARAM = {0.067,0.067,0.067,0.1,0.1,0.1,0.1,0.1,0.1};        
        IndexedTable             gain = null; 
        
        int is1,is2;
        
        ECGainEventListener(){};
        
        public void init(int is1, int is2) {
            
            System.out.println("ECCalibrationApp:ECGainEventListener.init");
            
            fileNamePrefix ="EC_CALIB_GAIN";
            filePath       = app.calibPath;
            
            initCalibPane();
            setCalibPane();

            collection.clear();
            
            this.is1=is1;
            this.is2=is2;
            
            gain = ccdb.getConstants(calrun, names[GAIN]);
            
            makeNewTable(is1,is2);
            
        } 
        
        public void initCalibPane() {
            System.out.println("initCalibPane:ECGain");   
        }
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:ECGain");
            enginePane.setTopComponent(ECGain);             
            enginePane.setDividerLocation(0.8);
    
        }  
        
        public void makeNewTable(int is1, int is2) {
            
            calib = new CalibrationConstants(3,"gain/F:gainErr/F");
            calib.setName(names[GAIN]);
            calib.setPrecision(3);

            for (int i=0; i<9; i++) { 
                calib.addConstraint(3, PARAM[i]*0.9, 
                                       PARAM[i]*1.1, 1, i+1);
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int idet=0; idet<ecPix.length; idet++) {
                    for(int il=1; il<4 ; il++) {
                        int sl = il+idet*3;
                        for(int ip = 1; ip < ecPix[idet].ec_nstr[il-1]+1; ip++) {
                            calib.addEntry(is, sl, ip);
//                            double scale = (is==5)?ecc.SCALE5[sl-1]:ecc.SCALE[sl-1];
//                            double gain = ecc.MIP[sl-1]/ecc.REF[sl-1]/scale;
                            calib.setDoubleValue(gain.getDoubleValue("gain",   is,sl,ip), "gain",     is, sl, ip);
                            calib.setDoubleValue(gain.getDoubleValue("gainErr",is,sl,ip), "gainErr",  is, sl, ip);
                        }
                    }
                }
            }     
            
            list.add(calib);
            
        }
        @Override
        public void writeDefaultTables(String runno) {
            System.out.println("Creating EC_CALIB_GAIN tables for run "+runno);
            makeNewTable(1,7);
            updateTable(app.calibPath+"EC_CALIB_GAIN_r1");          
            updateTable(app.calibPath+"EC_CALIB_GAIN_s1_r"+runno);   
            updateTable(app.calibPath+"EC_CALIB_GAIN_s2_r"+runno);   
            updateTable(app.calibPath+"EC_CALIB_GAIN_s3_r"+runno);   
            updateTable(app.calibPath+"EC_CALIB_GAIN_s4_r"+runno);   
            updateTable(app.calibPath+"EC_CALIB_GAIN_s5_r"+runno);   
            updateTable(app.calibPath+"EC_CALIB_GAIN_s6_r"+runno);   
            calib.save(app.calibPath+"EC_CALIB_GAIN_r"+runno);   
        }        
        
        public void createDefaultTable(int run)  {             
            for(int is=1; is<7; is++) {                
                for(int idet=0; idet<ecPix.length; idet++) {
                    for(int il=1; il<4 ; il++) {
                        int sl = il+idet*3;
                        for(int ip = 1; ip < ecPix[idet].ec_nstr[il-1]+1; ip++) {
                            double scale = (is==5)?ecc.SCALE5[sl-1]:ecc.SCALE[sl-1];
                            double gain = ecc.MIP[sl-1]/ecc.REF[sl-1]/scale;
                            calib.setDoubleValue(gain, "gain",     is, sl, ip);
                            calib.setDoubleValue(0.00, "gainErr",  is, sl, ip);
                        }
                    }
                }
            }                                 
            calib.save(app.calibPath+"EC_CALIB_GAIN_r"+run);
        }

        
        @Override        
        public void updateTable(String inputFile){
            
            if(inputFile==null) inputFile = getFileName(app.runNumber);
            
            int is,il,ip;
            double dum;
            
            try {                 
                FileInputStream fstream = new FileInputStream(inputFile);
                BufferedReader       br = new BufferedReader(new InputStreamReader(fstream));

                String line = br.readLine();
                
                while (line != null) {

                    String[] lineValues = line.trim().split("\\s+");
                    
                    is  = Integer.parseInt(lineValues[0]);
                    il  = Integer.parseInt(lineValues[1]);
                    ip  = Integer.parseInt(lineValues[2]);
                    if (calib.hasEntry(is,il,ip)&&doIDET[getDet(il)-1]) {
                        double  gain = Double.parseDouble(lineValues[3]);
                        double gaine = Double.parseDouble(lineValues[4]);
                        calib.setDoubleValue(gain, "gain",   is,il,ip);
                        calib.setDoubleValue(gaine,"gainErr",is,il,ip);
                    }
                    
                    line = br.readLine();                    
                }
                br.close();            
            }
            catch(FileNotFoundException ex) {
                ex.printStackTrace();                
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }            
            
        }    
        
        @Override
        public void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
            for(int is=is1; is<is2; is++) {
                for (int il=1; il<4 ; il++) {
                    int    sl = il+idet*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                    int iptst = ecPix[idet].ec_nstr[il-1]+1;
                    int ipmax = ip2>iptst ? iptst:ip2;
                    for(int ip = 1; ip < ipmax; ip++) {
                        if(engines[0].collection.hasEntry(is, sl, ip)) {
                            double  ref = ECConstants.REF[sl-1];
                            double    A = engines[0].collection.get(is,sl,ip).getFunc(0).parameter(0).value()/ref;
                            double    C = engines[0].collection.get(is,sl,ip).getFunc(0).parameter(2).value()/ref;
                            double Aerr = engines[0].collection.get(is,sl,ip).getFunc(0).parameter(0).error()/ref;
                            double Cerr = engines[0].collection.get(is,sl,ip).getFunc(0).parameter(2).error()/ref;                                    
                            double  gain = A + C;
                            double gaine = Math.sqrt(Aerr*Aerr+Cerr*Cerr); 
                            double scale = (is==5)?ECConstants.SCALE5[sl-1]:ECConstants.SCALE[sl-1];
                            double FADC2ENER = ECConstants.MIP[sl-1]/ECConstants.REF[sl-1]/scale;
                            if (doIDET[idet]&&doSector[is-1]) {
                                calib.setDoubleValue(FADC2ENER/gain,                 "gain", is, sl, ip);
                                calib.setDoubleValue(FADC2ENER*gaine/(gain*gain), "gainErr", is, sl, ip);
                            }
                        }
                    }
                }
            }           
            calib.fireTableDataChanged();
        }
        
        public double getTestChannel(int sector, int layer, int paddle) {
            return calib.getDoubleValue("gain", sector, layer, paddle);
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {
            return (getTestChannel(sector,layer,paddle) >=PARAM[layer-1]*0.9  &&
                    getTestChannel(sector,layer,paddle) <=PARAM[layer-1]*1.1 );
        }   
        
        public void drawPlots(int is, int il, int ic) {
            EmbeddedCanvas  c = new EmbeddedCanvas(); 
            c.repaint();                        
        }

    }
    
    public class ECStatusEventListener extends ECCalibrationEngine {
        
        EmbeddedCanvasTabbed  ECStatus = new EmbeddedCanvasTabbed("Status");
        EmbeddedCanvas               c = new EmbeddedCanvas(); 
        
        H1F h1af,h1a,h1t;
        double aYL,aYS,aYR,tYL,tYS,tYR;
        
        public DetectorCollection<H2F> H2_STAT = new DetectorCollection<H2F>();
        DetectorCollection<Float>         asum = new DetectorCollection<Float>();
        DetectorCollection<Float>         tsum = new DetectorCollection<Float>();
        IndexedTable                    status = null; 
        
        int is1,is2;
       
        ECStatusEventListener(){};
        
        public void init(int is1, int is2){
            
            System.out.println("ECCalibrationApp:ECStatusEventListener.init");
            
            fileNamePrefix = "EC_CALIB_STATUS";
            filePath       = app.calibPath;
            
            initCalibPane();
            setCalibPane();
            
            collection.clear();
            
            this.is1=is1;
            this.is2=is2;   
            
            makeNewTable(is1,is2);
            initHistos(is1,is2);

        }
        
        public void initCalibPane() {
            System.out.println("initCalibPane:ECStatus");   
            ECStatus.addCanvas("Background");
        }
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:ECStatus");
            enginePane.setTopComponent(ECStatus);             
            enginePane.setDividerLocation(0.8);    
        } 
        
        public void makeNewTable(int is1, int is2) {
            
            calib = new CalibrationConstants(3,"status/I");
            calib.setName(names[STATUS]);
//            calib.setPrecision(3);
            
            for (int i=0; i<9; i++) { 
                calib.addConstraint(3,   0,   0, 1, i+1);
                calib.addConstraint(4, 0.0, 0.1, 1, i+1);
                                       
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int idet=0; idet<ecPix.length; idet++) {
                    for (int il=1; il<4 ; il++) {
                        int sl = il+idet*3;
                        for(int ip = 1; ip < ecPix[idet].ec_nstr[il-1]+1; ip++) {
                            calib.addEntry(is, sl, ip);
                        }
                    }
                }
            }
            list.add(calib);  
        }   
        
        public void initHistos(int is1, int is2) {  
            
            for (int idet=0; idet<3; idet++) {
                int nb = ecPix[idet].ec_nstr[0] ; double nend = nb+1;  
                for (int is=is1; is<is2 ; is++) {
                    H2_STAT.add(is, 0, idet, new H2F("STATUS"+is,nb,1,nend,3,1,4));                
                    H2_STAT.add(is, 1, idet, new H2F("BACKGR"+is,nb,1,nend,3,1,4));                
                }
            }
        } 
        
        public double integral(H1F h, double xmin, double xmax) {
            
            float[] histogramData = h.getData(); 
            Axis xAxis = h.getxAxis();
            double integral = 0.0;
            for(int i = 0; i < xAxis.getNBins(); i++) {
                if(xAxis.getBinCenter(i)>xmin && xAxis.getBinCenter(i)<xmax) integral += histogramData[i];
            }
            return integral;            
        }
        
/*        public Integer getStatus() {   
            int t = app.trigger;  //0=cluster 1=pixel
            aYL=integral(h1af,ecc.AL[t][0],ecc.AL[t][1]);
            aYS=integral(h1af,ecc.AS[t][0],ecc.AS[t][1]);
            aYR=integral(h1af,ecc.AR[t][0],ecc.AR[t][1]);
            tYL=integral(h1t,ecc.TL[t][0],ecc.TL[t][1]);
            tYS=integral(h1t,ecc.TS[t][0],ecc.TS[t][1]);
            tYR=integral(h1t,ecc.TR[t][0],ecc.TR[t][1]);
            
            if (badAT())     return 3;  
            if (badT())      return 2;  
            if (badA())      return 1;
            return 0;            
        }
*/        
        public Integer getStatus(int is, int sl, int ip) {
        	float Asum = asum.get(7, sl, ip), A = asum.get(is, sl, ip);
        	float Tsum = tsum.get(7, sl, ip), T = tsum.get(is, sl, ip);
        	float aAsym = (A-Asum)/(A+Asum), tAsym = (T-Tsum)/(T+Tsum);
        	Boolean   badA = A==0 && T>0;   //dead ADC good TDC
            Boolean   badT = T==0 && A>0;   //dead TDC good ADC
            Boolean  badAT = A==0 && T==0;  //dead PMT or HV
        	Boolean nnbadA = aAsym < -0.85; //dead but noisy
        	Boolean nnbadT = tAsym < -0.85; //dead but noisy
       	    Boolean  nbadA = aAsym < -0.30; //low gain, bad cable, high threshold
        	Boolean  nbadT = tAsym < -0.30; //low gain, bad cable, high threshold
       	    Boolean  pbadA = aAsym >  0.30; //noisy, light leak
        	Boolean  pbadT = tAsym >  0.30; //noisy, light leak
        	if (badA && !badT) return 1;
        	if (badT && !badA) return 2;
        	if (badAT)         return 3;
       	    if (nnbadA)        return 1;
        	if (nnbadT)        return 2;
        	if (nbadA)         return 4;
        	if (nbadT)         return 5;
        	if (pbadA)         return 6;
        	if (pbadT)         return 7;
            return 0;
        }
        
        public double getBackground() {
            return getLL();
        }
          
        public double getPlotStatus(int stat) {            
            switch (stat) 
            {
            case 0: return 0.0;  
            case 1: return 0.60; 
            case 2: return 0.48;  
            case 3: return 0.02;  
            case 4: return 0.75;
            case 5: return 0.85; 
            case 6: return 0.99;
            case 7: return 1.10; 
            }
        return 0.48;
            
        }
        
        public Boolean goodA() {return aYS>10000;}
        public Boolean goodT() {return tYS>200;}
        public Boolean badA()  {return (aYS<10000)&&(tYS>200);}
        public Boolean badT()  {return (tYS<200)&&(aYS>10000);}
        public Boolean badAT() {return (tYS<200)&&(aYS<10000);}
        public double  getLL() {return (goodT()) ? tYL/tYS:0.0;}
       
        
        @Override
        public void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
            
            DetectorCollection<H2F> dc2af = ecPix[idet].strips.hmap2.get("H2_Mode1_Hist"); 
            DetectorCollection<H2F> dc2a  = ecPix[idet].strips.hmap2.get("H2_a_Hist"); 
            DetectorCollection<H2F> dc2t  = ecPix[idet].strips.hmap2.get("H2_t_Hist");
            
            asum.clear(); tsum.clear();
            
            for (int il=1; il<4 ; il++) {
        		int iptst = ecPix[idet].ec_nstr[il-1]+1;
                int ipmax = ip2>iptst ? iptst:ip2;
                int    sl = il+idet*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                for(int ip = 1; ip < ipmax; ip++) {
                    float aint = 0, tint = 0; int acnt=0, tcnt=0;
                	for (int is=is1; is<is2; is++) {
                        asum.add(is,sl,ip,(float)dc2a.get(is,il,0).sliceY(ip-1).integral());
                        tsum.add(is,sl,ip,(float)dc2t.get(is,il,0).sliceY(ip-1).integral());
                        acnt+=(asum.get(is, sl, ip)>0?1:0);
                        tcnt+=(tsum.get(is, sl, ip)>0?1:0);
                		aint+=(asum.get(is, sl, ip)>0?asum.get(is, sl, ip):0);
                		tint+=(tsum.get(is, sl, ip)>0?tsum.get(is, sl, ip):0); 
                    }
                    asum.add(7,sl,ip,aint/acnt);
                    tsum.add(7,sl,ip,tint/tcnt);
                }
            }

            for(int is=is1; is<is2; is++) {
                H2_STAT.get(is, 0, idet).reset(); H2_STAT.get(is, 1, idet).reset(); 
                for (int il=1; il<4 ; il++) {
                    int    sl = il+idet*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                    int iptst = ecPix[idet].ec_nstr[il-1]+1;
                    int ipmax = ip2>iptst ? iptst:ip2;
                    for(int ip = 1; ip < ipmax; ip++) {
                        h1af = dc2af.get(is,il,0).sliceY(ip-1);
                        h1a  = dc2a.get(is,il,0).sliceY(ip-1);
                        h1t  = dc2t.get(is,il,0).sliceY(ip-1);
                        Integer status = getStatus(is,sl,ip);
                        calib.setIntValue(status,"status", is, sl, ip);   
                        H2_STAT.get(is, 0, idet).fill((float)ip, (float)il, getPlotStatus(status));
                        H2_STAT.get(is, 1, idet).fill((float)ip, (float)il, getBackground());
                    }
                }   
            }
          
            calib.fireTableDataChanged();  
            writeFile(getFileName(app.runNumber),1,7,1,10); //because CalibrationConstants.write does not work for integer tables
        }
        
        @Override
        public void drawPlots(int is, int il, int ic) {
            switch (ECStatus.selectedCanvas) {
            case     "Status": drawStatus(); break;
            case "Background": drawBackground();
            }
        }
        
        public void drawStatus() {           
            c = ECStatus.getCanvas("Status"); 
            c.divide(3, 6);
            int n=0;
            for (int is=1; is<7; is++) {
                for (int idet=0; idet<3; idet++) {      
                    if(is==1) H2_STAT.get(is, 0, idet).setTitle(ecPix[idet].detName);
                    H2_STAT.get(is, 0, idet).setTitleX("SECTOR "+is);
                    c.getPad(n).getAxisZ().setRange(0.0, 1.0); 
                    c.getPad(n).getAxisZ().setLog(false);
                    c.cd(n); c.draw(H2_STAT.get(is, 0, idet)); n++;                   
                }
            }            
        }
        
        public void drawBackground() {           
            c = ECStatus.getCanvas("Background"); 
            c.divide(3, 6);
            int n=0;
            for (int is=1; is<7; is++) {
                for (int idet=0; idet<3; idet++) {
                    if(is==1) H2_STAT.get(is, 1, idet).setTitle(ecPix[idet].detName);
                    H2_STAT.get(is, 1, idet).setTitleX("SECTOR "+is);
                    c.getPad(n).getAxisZ().setRange(0.01,app.displayControl.pixMax);                                      
                    c.cd(n); c.draw(H2_STAT.get(is, 1, idet)); n++;
                }
            }
            c.repaint();                        
        }
        
        public double getTestChannel(int sector, int layer, int paddle) {
            return calib.getDoubleValue("status", sector, layer, paddle);
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {
            return (getTestChannel(sector,layer,paddle) == 0);
        }   
        
    	public void writeFile(String file, int is1, int is2, int il1, int il2) {
    		
    		String line = new String();
    		int[] npmt = {68,62,62,36,36,36,36,36,36};    
    		
    		try { 
    			File outputFile = new File(file);
    			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
    			BufferedWriter outputBw = new BufferedWriter(outputFw);
    			
    			for (int is=is1; is<is2; is++) {
    				for (int il=il1; il<il2; il++ ) {
    					for (int ip=0; ip<npmt[il-1]; ip++) {
    						    line = is+" "+il+" "+(ip+1)+" "+calib.getIntValue("status",is,il,ip+1);
    						    System.out.println(line);
    						    outputBw.write(line);
    						    outputBw.newLine();
    					}
    				}
    			}

    			outputBw.close();
    			outputFw.close();
    		}
    		catch(IOException ex) {
    			System.out.println("Error writing file '" );                   
    			ex.printStackTrace();
    		}

    	}
        
    }
    

/*
    private class ECTimingEventListener extends ECCalibrationEngine {
    	
    	    short setdetsector = 2;
    	    byte detECallayer=1;
    	    
    	    public void processEvent(DataEvent event) {
    	    	
		    if(event.hasBank("REC::Particle")     && 
               event.hasBank("REC::Calorimeter")  &&
               event.hasBank("REC::Scintillator") && 
               event.hasBank("REC::Event")        && 
               event.hasBank("ECAL::clusters")    && 
               event.hasBank("ECAL::adc")         && 
               event.hasBank("ECAL::tdc")         && 
               event.hasBank("RUN::rf")) {
		    	
			    HipoDataBank part = (HipoDataBank) event.getBank("REC::Particle");
				
				float p = 0;
				int partcount = 0;
				int eleccount = 0;
				
				for(int parti = 0; parti < part.rows(); parti++) {
					if(part.getInt("pid", parti)  == 11) {
						eleccount++;
						HipoDataBank detcount = (HipoDataBank) event.getBank("REC::Calorimeter");
						for(int detcounti = 0; detcounti < detcount.rows(); detcounti++)
						{
							short pindexcount = detcount.getShort("pindex", detcounti);
							byte detectorcount = detcount.getByte("detector", detcounti);
							byte sectorcount = detcount.getByte("sector", detcounti);
							if(pindexcount != parti && sectorcount == setdetsector && detectorcount == 7) partcount++;
						}
					}
				}
				
				HipoDataBank recev = (HipoDataBank) event.getBank("REC::Event");
				
				float vtime = -100;
				
				for(int t = 0; t < recev.rows(); t++)
				{
					float sttime = recev.getFloat("STTime", t);
					if(sttime > -100) vtime = sttime;
				}
				
				if(vtime > -100 && partcount == 0 && eleccount > 0)				
				{
					if(partcount > 1) System.out.println("partcount: " + partcount);
					for(int i = 0; i < part.rows(); i++)
					{
						int pid = part.getInt("pid", i);
						float px = part.getFloat("px", i);
						float py = part.getFloat("py", i);
						float pz = part.getFloat("pz", i);
						p = (float) Math.sqrt(px*px+py*py+pz*pz);
						if(pid == 11)
						{
							HipoDataBank det = (HipoDataBank) event.getBank("REC::Calorimeter");
							HipoDataBank detf = (HipoDataBank) event.getBank("REC::Scintillator");
							float totEdep = 0;
							float EdepECalcluster = 0;
							float Tvertex = vtime;
							float hitx = 0;
							float hity = 0;
							float hitz = 0;
							float hitpath = 0;
							float hittime = 0;
							for(int j = 0; j < det.rows(); j++)
							{
								short pindex = det.getShort("pindex", j);
								byte detector = det.getByte("detector", j);
								byte sector = det.getByte("sector", j);
								byte layer = det.getByte("layer", j);
								float x = det.getFloat("x", j);
								float y = det.getFloat("y", j);
								float z = det.getFloat("z", j);
								float path = det.getFloat("path", j);
								float time = det.getFloat("time", j);
								float energy = det.getFloat("energy", j);
								if(pindex == i && detector == 7 && sector == setdetsector) totEdep = totEdep + energy;
								if(pindex == i && detector == 7 && sector == setdetsector && layer == detECallayer)
								{
									EdepECalcluster = energy;
									hitx = x;
									hity = y;
									hitz = z;
									hitpath = path;
								}
							}
							
							HipoDataBank peak = (HipoDataBank) event.getBank("ECAL::peaks");
			                float xo = peak.getFloat("xo",i);
			                float yo = peak.getFloat("yo",i);
			                float zo = peak.getFloat("zo",i);
			                float xe = peak.getFloat("xe",i);
			                float ye = peak.getFloat("ye",i);
			                float ze = peak.getFloat("ze",i);
			                
//							float leff = (float) Math.sqrt(((xeff-PMTx)*(xeff-PMTx))+((yeff-PMTy)*(yeff-PMTy)));
							
//							float xeff = (hitx+(slope*hity)-(slope*intercept))/(1+(slope*slope));
//							float yeff = (slope*xeff)+intercept;
//							float leff = (float) Math.sqrt(((xeff-PMTx)*(xeff-PMTx))+((yeff-PMTy)*(yeff-PMTy)));
								
							byte id = -1;
							HipoDataBank clust = (HipoDataBank) event.getBank("ECAL::clusters");
							for(int k = 0; k < clust.rows(); k++)
							{
								byte sector = clust.getByte("sector", k);
								byte layer = clust.getByte("layer", k);
								float energy = clust.getFloat("energy", k);
								if(view == U) id = clust.getByte("idU", k);
								if(view == V) id = clust.getByte("idV", k);
								if(view == W) id = clust.getByte("idW", k);
								HipoDataBank hit = (HipoDataBank) event.getBank("ECAL::hits");
								byte ECalstrip = 0;
								if(sector == setdetsector && layer == detECallayer && energy == EdepECalcluster)
								{
									for(int l = 0; l < hit.rows(); l++)
									{
										byte strip = hit.getByte("strip", l);
										byte peakid = hit.getByte("peakid", l);
										byte EChitlayer = hit.getByte("layer", l);
										if(peakid == id && EChitlayer == ECallayer && strip == stripID) ECalstrip = strip;
									}
								}
								HipoDataBank adc = (HipoDataBank) event.getBank("ECAL::adc");
								int ECalstripADC = 0;
								int ECalstripref1ADC = 0;
								int ECalstripref2ADC = 0;
								if(ECalstrip == stripID)
								{
									for(int m = 0; m < adc.rows(); m++)
									{
										byte adcsector = adc.getByte("sector", m);
										byte adclayer = adc.getByte("layer", m);
										short adcstrip = adc.getShort("component", m);
										int ADC = adc.getInt("ADC", m);
										if(adcsector == setdetsector && adclayer == ECallayer && adcstrip == stripID) ECalstripADC = ADC;
										if(adcsector == setdetsector && adclayer == ECallayer && adcstrip == stripID-1) ECalstripref1ADC = ADC;
										if(adcsector == setdetsector && adclayer == ECallayer && adcstrip == stripID+1) ECalstripref2ADC = ADC;
									}
									if(ECalstripADC > ECalstripref1ADC && ECalstripADC > ECalstripref2ADC)
									{
										HipoDataBank tdc = (HipoDataBank) event.getBank("ECAL::tdc");
										int TDCcounter = 0;
										int TDCvalue = 0;
										for(int n = 0; n < tdc.rows(); n++)
										{
											byte tdcsector = tdc.getByte("sector", n);
											byte tdclayer = tdc.getByte("layer", n);
											short tdcstrip = tdc.getShort("component", n);	
											int TDC = tdc.getInt("TDC", n);
											if(tdcsector == setdetsector && tdclayer == ECallayer && tdcstrip == stripID && TDC > 0)
											{
												TDCcounter++;
												TDCvalue = TDC;
											}
										}
										if(TDCcounter == 1 && (hitpath/30)+(leff/18.1)+Tvertex > 500)
										{
											TDCi.add((float) TDCvalue);
											ADCi.add((float) ECalstripADC);
											l2i.add(leff*leff);
											l3i.add(leff*leff*leff);
											Ti.add((float) ((hitpath/30)+(leff/18.1)+Tvertex));
											Texpectedi.add((float) ((hitpath/30)+(leff/18.1)));
											HipoDataBank runrf = (HipoDataBank) event.getBank("RUN::rf");
											for(int o = 0; o < runrf.rows(); o++)
											{
												short rfid = runrf.getShort("id", o);
												float rftime = runrf.getFloat("time", o);
												if(rfid == 1) RFTi.add(rftime);		
											}
											hstrip.fill(stripID);
										}
									}
								}
							}
						}
					}
				}
			}
		    	
		    }   	
        }
    	    
    }
 */   
}
