package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
import org.jlab.groot.ui.RangeSlider;
import org.jlab.utils.groups.IndexedTable;

public class ECCalibrationApp extends FCApplication implements CalibrationConstantsListener,ChangeListener {
    
    JPanel                    engineView = new JPanel();
    JSplitPane                enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
    JButton                   tableWrite = null;
    JButton                    tableSave = null;
    JButton                    tableRead = null;
    JButton                       hvSave = null;
    CalibrationConstantsView      ccview = new CalibrationConstantsView("");
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
        enginePane.setResizeWeight(0.9);
        engineView.add(enginePane);
        engineView.add(getButtonPane(),BorderLayout.PAGE_END);
        return engineView;       
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
   
    public void updateDetectorView(DetectorShape2D shape) {
        
        ECCalibrationEngine engine = getSelectedEngine();
        DetectorDescriptor dd = shape.getDescriptor();
        this.getDetIndices(dd);
        layer = lay;
        if (this.omap==3) {
           if(engine.isGoodPaddle(is, layer-1, ic)) {
               shape.setColor(101, 200, 59);
           } else {
               shape.setColor(225, 75, 60);
           }
        }
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
        
        DetectorDescriptor dd = new DetectorDescriptor();
        dd.setSectorLayerComponent(selectedSector,selectedLayer,selectedPaddle-1);
        app.setFPS(0);
        updateCanvas(dd);
        
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
            
            status = ccdb.getConstants(calrun, names[STATUS]);
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
            
            if(isUseTable) return;
            
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
                        double ratio=Math.pow(gain, 1./11.);
                        double hvnew = (ratio>0.5) ? hvold/ratio:hvold;
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
                    dum = Double.parseDouble(lineValues[3]);  calib.setDoubleValue(dum,"Gain",  is,il,ip);
                    dum = Double.parseDouble(lineValues[4]);  calib.setDoubleValue(dum,"HVold", is,il,ip);
                    dum = Double.parseDouble(lineValues[5]);  calib.setDoubleValue(dum,"HVnew", is,il,ip);
                    app.fifo6.get(is, il, ip).add(dum);
                    dum = Double.parseDouble(lineValues[6]);  calib.setDoubleValue(dum,"DHV",   is,il,ip);
                    
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
        
        double xSliderMin = 0.0;
        double xSliderMax = 100.0;
        double currentRangeMin = 0.0;
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
            
            atten = ccdb.getConstants(calrun, names[ATTEN]);

            makeNewTable(is1,is2);

        }
        
        public void makeNewTable(int is1, int is2) {
            
            calib = new CalibrationConstants(3,"A/F:Aerr/F:B/F:Berr/F:C/F:Cerr/F:FitMin/F:FitMax/F");
            calib.setName(names[ATTEN]);
            calib.setPrecision(3);
            
            int kk=0;
            for (int k=0; k<3; k++) {
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
                            calib.setDoubleValue(1.00, "A",     is, sl, ip);
                            calib.setDoubleValue(0.00, "Aerr",  is, sl, ip);
                            calib.setDoubleValue(0.00, "B",     is, sl, ip);
                            calib.setDoubleValue(0.00, "Berr",  is, sl, ip);
                            calib.setDoubleValue(0.00, "C",     is, sl, ip);
                            calib.setDoubleValue(0.00, "Cerr",  is, sl, ip);
                            calib.setDoubleValue(0.04, "FitMin",is, sl, ip);
                            calib.setDoubleValue(0.95, "FitMax",is, sl, ip);
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
          updateTable(app.calibPath+"EC_CALIB_ATTEN_r9");          
          updateTable(app.calibPath+"EC_CALIB_ATTEN_s1_r"+runno);   
          updateTable(app.calibPath+"EC_CALIB_ATTEN_s2_r"+runno);   
          updateTable(app.calibPath+"EC_CALIB_ATTEN_s3_r"+runno);   
//        updateTable(app.calibPath+"EC_CALIB_ATTEN_s4_r"+runno);   
          updateTable(app.calibPath+"EC_CALIB_ATTEN_s5_r"+runno);   
          updateTable(app.calibPath+"EC_CALIB_ATTEN_s6_r"+runno);   
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
                   int il =  lay; if (isPix) il=lay-10;
                   int ip1=1,ip2=1;
                   if (sliderMode=="Strip") {ip1=pixStrip; ip2=pixStrip+1;}
                   if (sliderMode=="View")  {ip1=1;        ip2=ecPix[ilmap].ec_nstr[il-1];}
                   for (int ip=ip1; ip<ip2; ip++) {
                      calib.setDoubleValue(currentRangeMin*0.01,"FitMin",is, 3*ilmap+il, ip);
                      calib.setDoubleValue(currentRangeMax*0.01,"FitMax",is, 3*ilmap+il, ip);
                   }
                   calib.fireTableDataChanged();
                   analyze(ilmap,is,is+1,il,il+1,ip1,ip2);
                   drawPlots(sectorSelected,layerSelected,channelSelected);
               }
           });   
       }
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:ECAtten");
            enginePane.setTopComponent(hPane);                        
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
        public synchronized void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
            
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
                        fits.getDescriptor().setType(DetectorType.EC);
                        fits.addGraph(ecPix[idet].strips.getpixels(il,ip,cnts),
                                      ecPix[idet].strips.getpixels(il,ip,distmap),
                                      ecPix[idet].strips.getpixels(il,ip,meanmap),
                                      ecPix[idet].strips.getpixels(il,ip,meanerr),
                                      ecPix[idet].strips.getpixels(il,ip,status));
                        fits.setFitLimits(calib.getDoubleValue("FitMin",is,sl,ip),
                                          calib.getDoubleValue("FitMax",is,sl,ip));
                        fits.analyze(idet,scale);
                        
                        calib.setDoubleValue(fits.getFunc(0).parameter(0).value()/scale, "A",    is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(1).value(),       "B",    is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(2).value()/scale, "C",    is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(0).error()/scale, "Aerr", is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(1).error(),       "Berr", is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(2).error()/scale, "Cerr", is, sl, ip);
                        
                        collection.add(fits.getDescriptor(),fits);
                     }
                  }
               }
            }
            calib.fireTableDataChanged();              
         } 
        
        public double getTestChannel(int sector, int layer, int paddle) {
            return calib.getDoubleValue("B", sector, layer, paddle);
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {

            return (getTestChannel(sector,layer,paddle) >=PARAM[1][layer-1]-DELTA[1][layer-1]  &&
                    getTestChannel(sector,layer,paddle) <=PARAM[1][layer-1]+DELTA[1][layer-1] );

        }    
              
        @Override
        public synchronized void drawPlots(int is, int layer, int ic) {
            
            DetectorCollection<H2F>            dc2a = ecPix[ilmap].strips.hmap2.get("H2_a_Hist");    
            EmbeddedCanvas                        c = new EmbeddedCanvas();    
            H1F                              pixADC = null;
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
               float meanmap[] = (float[]) ecPix[ilmap].Lmap_a.get(is, layer, 0).get(1);
               il = layer-10;
               xpix[0] = ecPix[ilmap].pixels.getDist(il, ic+1);
               ypix[0] = meanmap[ic];
               xerr[0] = 0.;
               yerr[0] = 0.;
               pixStrip = ecPix[ilmap].pixels.getStrip(il,ic+1);
                 pixADC = dc2a.get(is,il,2).sliceY(ic) ; pixADC.setFillColor(2);
                 pixADC.setTitleX("Sector "+is+otab[ilmap][il-1]+pixStrip+" Pixel "+(ic+1)+" ADC");
            }
            
            if (isStr) {
                pixStrip = ic+1;
                pixADC = dc2a.get(is,il,0).sliceY(ic) ; pixADC.setFillColor(2);                
                pixADC.setTitleX("Sector "+is+otab[ilmap][il-1]+pixStrip+" Strip "+(ic+1)+" ADC");
            }           
                         
            if (isStr||isPix) {
               if (app.getInProcess()>0) {
                     nstr = ecPix[ilmap].ec_nstr[il-1];
                     if (app.getInProcess()==1&&app.getIsRunning())  {analyze(ilmap,is,is+1,il,il+1,1,69);}
                     int sl = il+ilmap*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                     if (collection.hasEntry(is, sl, pixStrip)) {
//                     slider.setValue(     (int) (fit.get(is,il,pixStrip).fitLimits[0]*100));
//                     slider.setUpperValue((int) (fit.get(is,il,pixStrip).fitLimits[1]*100));            

                     double scale = (app.isMC) ? ecc.MIP[sl-1]:ecc.REF[sl-1]; 
                     
                     for (int ip=0; ip<nstr ; ip++) {
                         xp[ip]    = ip+1;     
                         xpe[ip]   = 0.; 
                         
                         parA[ip]  = collection.get(is,sl,ip+1).getFunc(0).parameter(0).value()/scale;
                         parAe[ip] = collection.get(is,sl,ip+1).getFunc(0).parameter(0).error()/scale;
                         parB[ip]  = collection.get(is,sl,ip+1).getFunc(0).parameter(1).value();
                         parBe[ip] = collection.get(is,sl,ip+1).getFunc(0).parameter(1).error();
                         parC[ip]  = collection.get(is,sl,ip+1).getFunc(0).parameter(2).value()/scale;
                         parCe[ip] = collection.get(is,sl,ip+1).getFunc(0).parameter(2).error()/scale;
                         parAC[ip] = parA[ip]+parC[ip];
                         parACe[ip]= Math.sqrt(parAe[ip]*parAe[ip]+parCe[ip]*parCe[ip]);
                         double chi2 = collection.get(is,sl,ip+1).getFunc(0).getChiSquare()/
                                       collection.get(is,sl,ip+1).getFunc(0).getNDF();
                         vchi2[ip] = Math.min(4, chi2); 
                         vchi2e[ip]= 0.;                                                          
                         ccdbA[ip] = atten.getDoubleValue("A",is,sl,ip+1);
                         ccdbB[ip] = atten.getDoubleValue("B",is,sl,ip+1);
                         ccdbC[ip] = atten.getDoubleValue("C",is,sl,ip+1);
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
                     GraphErrors ccdbACGraph = new GraphErrors("dbAC",xp,ccdbAC,xpe,ccdbACe); 
                     GStyle.getGraphErrorsAttributes().setMarkerColor(2);
                     GStyle.getGraphErrorsAttributes().setFillStyle(2);
                       
                     parAGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ;  ccdbAGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ; 
                     parAGraph.getAttributes().setTitleY("A")  ;               ccdbAGraph.getAttributes().setTitleY("A")  ;
                     parAGraph.getAttributes().setTitle(" ");
                     parBGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ; ccdbBGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ;
                     parBGraph.getAttributes().setTitleY("B") ;               ccdbBGraph.getAttributes().setTitleY("B")  ;
                     parBGraph.getAttributes().setTitle(" ");
                     parCGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ; ccdbCGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ;
                     parCGraph.getAttributes().setTitleY("C") ;               ccdbCGraph.getAttributes().setTitleY("C")  ;
                     parCGraph.getAttributes().setTitle(" ");
                     parACGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ; ccdbACGraph.getAttributes().setTitleX(otab[ilmap][il-1]) ;
                     parACGraph.getAttributes().setTitleY("A+C") ;             ccdbACGraph.getAttributes().setTitleY("A+C")  ;
                     parACGraph.getAttributes().setTitle(" ");
                     chi2Graph.getAttributes().setTitleX(otab[ilmap][il-1]) ;  
                     chi2Graph.getAttributes().setTitleY("REDUCED CHI^2"); 
                     chi2Graph.getAttributes().setTitle(" ");
                      
                     F1D f1 = new F1D("p0","[a]",0,nstr+1); f1.setParameter(0,scale); f1.setLineWidth(1);
                               
                     double ymax=2.; 
                     
                     c = fitPix.getCanvas("Pixel Fits"); 
                     
                     c.getPad(0).getAxisX().setRange(0.,400.);c.getPad(0).getAxisY().setRange(0.,300.); 
                     c.draw(collection.get(is,sl,pixStrip).getRawGraph(0));  
                     if(collection.get(is,sl,pixStrip).getFitGraph(0).getDataSize(0)>0) {
                         c.draw(collection.get(is,sl,pixStrip).getFitGraph(0),"same");                                                 
                     }
                     if (isPix) c.draw(pixGraph,"same");
                     c.repaint();

                     c = fitADC.getCanvas("ADC"); 
                     
                     c.cd(0); pixADC.setOptStat(Integer.parseInt("110")); pixADC.setTitle(""); c.draw(pixADC);                    
                     c.repaint();
                     
                     c = fitCh2.getCanvas("Chi^2"); 
                     
                     double xmax = ecPix[ilmap].ec_nstr[il-1]+1;
                     
                     c.getPad(0).getAxisX().setRange(0.,xmax);c.getPad(0).getAxisY().setRange(0.,4.);
                     c.draw(chi2Graph) ; c.draw(chi2Graphi,"same");
                     c.repaint();
                     
                     c = fitCof.getCanvas("COEF"); c.divide(2,2);
                     
                     c.cd(0); c.getPad(0).getAxisX().setRange(0.,xmax);c.getPad(0).getAxisY().setRange(0.,2.);
                     c.draw(ccdbAGraph) ; c.draw(parAGraph,"same"); c.draw(parAGraphi,"same"); 
                     c.cd(1); c.getPad(1).getAxisX().setRange(0.,xmax);c.getPad(1).getAxisY().setRange(0.,600.);
                     c.draw(ccdbBGraph); c.draw(parBGraph,"same"); c.draw(parBGraphi,"same");                       
                     c.cd(2); c.getPad(2).getAxisX().setRange(0.,xmax);c.getPad(2).getAxisY().setRange(0.,2.);
                     c.draw(ccdbCGraph) ; c.draw(parCGraph,"same"); c.draw(parCGraphi,"same");
                     c.cd(3); c.getPad(3).getAxisX().setRange(0.,xmax);c.getPad(3).getAxisY().setRange(0.,2.);
                     c.draw(ccdbACGraph) ; c.draw(parACGraph,"same"); c.draw(parACGraphi,"same"); 
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
        
        public final double[]   PARAM = {0.067,0.067,0.067,0.1,0.1,0.1,0.1,0.1,0.1};        
        IndexedTable             gain = null; 
        
        int is1,is2;
        
        ECGainEventListener(){};
        
        public void init(int is1, int is2) {
            
            System.out.println("ECCalibrationApp:ECGainEventListener.init");
            
            fileNamePrefix ="EC_CALIB_GAIN";
            filePath       = app.calibPath;
            
            collection.clear();
            
            this.is1=is1;
            this.is2=is2;
            
            gain = ccdb.getConstants(calrun, names[GAIN]);
            
            makeNewTable(is1,is2);
            
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
                            double scale = (is==5)?ecc.SCALE5[sl-1]:ecc.SCALE[sl-1];
                            double gain = ecc.MIP[sl-1]/ecc.REF[sl-1]/scale;
                            calib.setDoubleValue(gain, "gain",     is, sl, ip);
                            calib.setDoubleValue(0.00, "gainErr",  is, sl, ip);
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
//          updateTable(app.calibPath+"EC_CALIB_GAIN_s4_r"+runno);   
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
                    double  gain = Double.parseDouble(lineValues[3]);
                    double gaine = Double.parseDouble(lineValues[4]);
                    calib.setDoubleValue(gain, "gain",   is,il,ip);
                    calib.setDoubleValue(gaine,"gainErr",is,il,ip);
                    
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
                            calib.setDoubleValue(FADC2ENER/gain,                 "gain", is, sl, ip);
                            calib.setDoubleValue(FADC2ENER*gaine/(gain*gain), "gainErr", is, sl, ip);
                        }
                    }
                }
            }           
            calib.fireTableDataChanged();
        }
        
        @Override
        public void fit(int sector, int layer, int paddle, double minRange, double maxRange){ 
           double mean = ftofPix[layer-1].strips.hmap2.get("H2_a_Hist").get(sector,0,0).sliceY(paddle-1).getMean();
           calib.addEntry(sector, layer, paddle);
           calib.setDoubleValue(mean, "mipa_left", sector, layer, paddle);
           calib.setDoubleValue(mean, "mipa_right", sector, layer, paddle);
        }
        
        public double getTestChannel(int sector, int layer, int paddle) {
            return calib.getDoubleValue("mipa_left", sector, layer, paddle);
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {
            return (getTestChannel(sector,layer,paddle) >=PARAM[layer-1]*0.9  &&
                    getTestChannel(sector,layer,paddle) <=PARAM[layer-1]*1.1 );
        }   
        
        public void drawPlots(int is, int il, int ic) {
            
        }

    }
    
    private class ECStatusEventListener extends ECCalibrationEngine {
        
        public final int[]   PARAM = {0,0,0,0,0,0,0,0,0};
        public final int     DELTA = 1;
        IndexedTable        status = null; 
        
        ECStatusEventListener(){};
        
        public void init(int is1, int is2){
            
            System.out.println("ECCalibrationApp:ECStatusEventListener.init");
            
            fileNamePrefix = "EC_CALIB_STATUS";
            filePath       = app.calibPath;
            
            collection.clear();
            
            status = ccdb.getConstants(calrun, names[STATUS]);
            calib = new CalibrationConstants(3,"status/I");
            calib.setName(names[STATUS]);
            calib.setPrecision(3);
            
            for (int i=0; i<9; i++) {
                calib.addConstraint(3, PARAM[i]-DELTA, 
                                       PARAM[i]+DELTA, 1, i+1);
            }
 
/*            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setIntValue(0,"status",is,il,ip);
                    }
                }
            }
            */
            list.add(calib);
        }
        
        public void drawPlots(int is, int il, int ic) {
            
        }
    }

 
}
