package org.clas.fcmon.cc;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.ui.RangeSlider;
import org.jlab.utils.groups.IndexedTable;

public class CCCalibrationApp extends FCApplication implements CalibrationConstantsListener,ChangeListener {
    
    JPanel                    engineView = new JPanel();
    JSplitPane                enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
    JButton                    tableSave = null;
    JButton                    tableRead = null;
    JButton                       hvSave = null;
    CalibrationConstantsView      ccview = new CalibrationConstantsView("");
    ConstantsManager                ccdb = new ConstantsManager();
    ArrayList<CalibrationConstants> list = new ArrayList<CalibrationConstants>();

    public CCCalibrationEngine[] engines = {
            new CCGainEventListener(),
            new CCStatusEventListener(),
            new CCTimingEventListener(),
            new CCHVEventListener(),
    };

    public final int   GAIN = 0;
    public final int STATUS = 1;
    public final int TIMING = 2;
    public final int     HV = 3;
    
    String[] names = {"/calibration/ltcc/gain",
                      "/calibration/ltcc/status",
                      "/calibration/ltcc/timing_offset",
                      "/calibration/ltcc/hv",
                      };
    
    String selectedDir = names[HV];
       
    int selectedSector = 1;
    int  selectedLayer = 1;
    int selectedPaddle = 1;
    
    int calrun = 1;
    
    public CCCalibrationApp(String name , CCPixels ccPix) {
        super(name, ccPix);       
     } 
    
    public void init() {        
        for (int i=0; i<engines.length; i++) engines[i].init(CCConstants.IS1,CCConstants.IS2); 
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
        tableSave  = new JButton("Write Table");
        tableSave.addActionListener(this);
        tableSave.setActionCommand("WRITE");
        tableRead  = new JButton("Restore Table");
        tableRead.addActionListener(this);
        tableRead.setActionCommand("RESTORE");        
        hvSave  = new JButton("Load HVnew");
        hvSave.addActionListener(this);
        hvSave.setActionCommand("LOADHV");
        buttonPane.add(tableSave);
        buttonPane.add(tableRead);
        buttonPane.add(hvSave);
        return buttonPane;
    }  
    
    public void actionPerformed(ActionEvent e) {

        CCCalibrationEngine engine = getSelectedEngine();
       if (e.getActionCommand().compareTo("WRITE")==0) {           
            String outputFileName = engine.getFileName(app.runNumber);
            engine.calib.save(outputFileName);
            JOptionPane.showMessageDialog(new JPanel(),
                    engine.calib.getName() + " table written to "+outputFileName);
        }        
        if (e.getActionCommand().compareTo("RESTORE")==0) {           
            String outputFileName = engines[0].getFileName(app.runNumber);
            engines[0].updateTable();
            JOptionPane.showMessageDialog(new JPanel(),
                   "Restoring table " + engine.calib.getName() + " written to "+outputFileName);
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
    
    public CCCalibrationEngine getSelectedEngine() {
        
        CCCalibrationEngine engine = engines[HV];

        if (selectedDir == names[HV]) {
            engine = engines[HV];
        } else if (selectedDir == names[GAIN]) {
            engine = engines[GAIN];
        } else if (selectedDir == names[STATUS]) {
            engine = engines[STATUS];
        } else if (selectedDir == names[TIMING]) {
            engine = engines[TIMING];
        } 
        return engine;
    }

    public void analyze(int is1, int is2, int il1, int il2) {            
        if (il1>3) return;
        CCCalibrationEngine engine = getSelectedEngine();
        engine.analyze(is1,is2,il1,il2,1,19);
    }   
    
    public void analyzeAllEngines(int is1, int is2, int il1, int il2) {
        for (int i=0; i<engines.length; i++) {
//            engines[i].analyze(is1,is2,il1,il2,1,19); 
        }
    }
   
    public void updateDetectorView(DetectorShape2D shape) {
        
        CCCalibrationEngine engine = getSelectedEngine();
        DetectorDescriptor dd = shape.getDescriptor();
        this.getDetIndices(dd);
        layer = lay;
        if (app.omap==3) {
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
        
        CCCalibrationEngine engine = getSelectedEngine();
        this.getDetIndices(dd);
        engine.drawPlots(is,lay,ic);        
    }
    
    public void stateChanged(ChangeEvent e) {
        int i = ccview.getTabbedPane().getSelectedIndex();
        String tabTitle = ccview.getTabbedPane().getTitleAt(i);
        if (tabTitle != selectedDir) {            
            selectedDir = tabTitle;
        }
        CCCalibrationEngine engine = getSelectedEngine(); 
        engine.setCalibPane(); 
        
    }  
    
    public class CCHVEventListener extends CCCalibrationEngine {
        
        EmbeddedCanvasTabbed  CCHv = new EmbeddedCanvasTabbed("HV");
        public final double PARAM3 = 1.0;
        public final int    PARAM6 = 0;
        public final double[] GAIN = {0.5,0.5};
        public final int[]     DHV = {100,100};
        
        IndexedTable        status = null; 
        
        CCHVEventListener(){};
        
        public void init(int is1, int is2) {
            
            System.out.println("CCCalibrationApp:CCHvEventListener.init");
            
            fileNamePrefix = "LTCC_CALIB_HV";
            filePath       = app.hvPath;
            
            initCalibPane();
            setCalibPane();
            
            calib = new CalibrationConstants(3,"Gain/F:HVold/F:HVnew/F:DHV/F");
            calib.setName(names[HV]);
            calib.setPrecision(3);
            
            for (int il=1; il<3; il++) {
                calib.addConstraint(3, PARAM3-GAIN[il-1], 
                                       PARAM3+GAIN[il-1], 1, il);
                calib.addConstraint(6, PARAM6-DHV[il-1], 
                                       PARAM6+DHV[il-1], 1, il);
           }
            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setDoubleValue(1.0,"Gain",is,il,ip);
                        calib.setDoubleValue(0., "HVold", is, il, ip);
                        calib.setDoubleValue(0., "HVnew", is, il, ip);
                        calib.setDoubleValue(0., "DHV",   is, il, ip);                        
                    }
                }
            }
            list.add(calib);
        }
        
        public void initCalibPane() {
            System.out.println("initCalibPane:CCHv");            
        }  
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:CCHv");
            enginePane.setTopComponent(CCHv);         
        } 
        
        @Override
        public synchronized void analyze(int is1, int is2, int il1, int il2, int ip1, int ip2) {
            for (int is=is1; is<is2; is++) {
                for (int il=il1; il<il2; il++) {
                    for (int ip=ip1; ip<ip2; ip++) { 
                        calib.addEntry(is, il, ip);
                        calib.setDoubleValue(app.fifo1.get(is, il, ip).getLast(), "HVold", is, il, ip);                        
                    }
                }
            }
            CalibrationConstants gains = engines[0].calib;
            
            for (int is=is1; is<is2; is++) {
                for (int il=il1; il<il2; il++) {
                    for (int ip=ip1; ip<ip2; ip++) { 
                        double  gain = gains.getDoubleValue("Gain", is,il,ip);
                        double hvold = calib.getDoubleValue("HVold", is,il,ip);
                        calib.setDoubleValue(gain,"Gain", is, il, ip);                        
                        if (gain<0.3||gain>2.5) gain=0.0;
                        double ratio=Math.pow(gain, 1./11.);
                        double hvnew = (ratio>0.5) ? hvold/ratio:hvold;
                        calib.setDoubleValue(hvnew,"HVnew", is, il, ip);                           
                        calib.setDoubleValue(hvnew-hvold,"DHV", is, il, ip);  
                        app.fifo6.get(is, il, ip).add(hvnew);
                    }
                }
            }
            
            calib.fireTableDataChanged();     
        }  
        public synchronized void drawPlots(int is, int il, int ic) {
            if (il>3) return;
            int nstr = ccPix.nstr[0];
            if (app.getInProcess()<2) analyze(is,is+1,il,il+1,1,nstr+1);
            EmbeddedCanvas  c = new EmbeddedCanvas(); 
            int   sl = il;
            H1F hvold = new H1F(" ",nstr,1.,nstr+1);
            H1F hvnew = new H1F(" ",nstr,1.,nstr+1);
            H1F   dhv = new H1F(" ",nstr,1.,nstr+1);
            hvold.setFillColor(33);
            hvnew.setFillColor(32);
              dhv.setFillColor(32);
            for (int ip=1; ip<nstr+1; ip++) hvold.fill(ip,app.fifo1.get(is, sl, ip).getLast());
            for (int ip=1; ip<nstr+1; ip++) hvnew.fill(ip,calib.getDoubleValue("HVnew", is,sl,ip));
            for (int ip=1; ip<nstr+1; ip++)   dhv.fill(ip,calib.getDoubleValue("DHV",   is,sl,ip));
             
            c = CCHv.getCanvas("HV"); 
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
                if (app.getInProcess()==1&&app.getIsRunning()) engines[0].analyze(is,is+1,il,il+1,1,nstr+1);
                if (engines[0].collection.hasEntry(is, sl, ic+1)) {
                    if(engines[0].collection.get(is,sl,ic+1).getFitGraph(0).getDataSize(0)>0) {
                        c.getPad(2).getAxisX().setRange(0.,ccPix.amax); 
                        c.cd(2); c.draw(engines[0].collection.get(is,sl,ic+1).getFitGraph(0));
                    }
                }
            }
            c.repaint();                                    
        }
        
        @Override
        public boolean isGoodPaddle(int sector, int layer, int paddle) {
            int rowCount = (sector-1)*36+layer*18+paddle;
            return calib.isValid(rowCount, 3);
        }
    }
    
    private class CCGainEventListener extends CCCalibrationEngine implements ActionListener {
        
        JSplitPane              hPane = null; 
        EmbeddedCanvasTabbed   fitAdc = new EmbeddedCanvasTabbed("ADC Fit");
        EmbeddedCanvasTabbed   fitSpe = new EmbeddedCanvasTabbed("SPE Fit");
        EmbeddedCanvasTabbed   fitCh2 = new EmbeddedCanvasTabbed("Chi^2");
        EmbeddedCanvasTabbed   fitCof = new EmbeddedCanvasTabbed("COEF");        
        EmbeddedCanvas              c = new EmbeddedCanvas();    
        IndexedTable             gain = null; 
        RangeSlider            slider = null;
        
        public final double[]  GAIN_MEAN  = {1,1};
        public final double[]  GAIN_RANGE = {0.2,0.2};
        public final double[]     SPE_REF = {350,350};
        
        int is1,is2;
        int npmt = ccPix.nstr[0];
        
        double      xSliderMin = 0.0;
        double      xSliderMax = 100.0;
        double currentRangeMin = 0.0;
        double currentRangeMax = 100.0;
        
        String sliderMode = "Strip";   
        
        double[] xp     = new double[npmt];
        double[] xpe    = new double[npmt];
        double[] parB   = new double[npmt];
        double[] parBe  = new double[npmt];
        double[] parC   = new double[npmt];
        double[] parCe  = new double[npmt];
        double[] vchi2  = new double[npmt];
        double[] vchi2e = new double[npmt]; 
        double[] xpix   = new double[1];
        double[] ypix   = new double[1];
        double[] xerr   = new double[1];
        double[] yerr   = new double[1];
        
        CCGainEventListener(){}
        
        public void init(int is1, int is2) {
            
            System.out.println("CCCalibrationApp:CCGainEventListener.init");
            
            fileNamePrefix = "CC_CALIB_GAIN";
            filePath       = app.calibPath;
            
            getButtonGroup();
            getSliderPane();
            initCalibPane();
            setCalibPane();       
            
            collection.clear();
            
            this.is1=is1;
            this.is2=is2; 
            
            gain = ccdb.getConstants(calrun, names[GAIN]);
            calib = new CalibrationConstants(3,"Gain/F:GainErr/F:FitMin/F:FitMax/F");
            calib.setName(names[GAIN]);
            calib.setPrecision(3);    
            
            for (int il=1 ; il<3; il++) {
                calib.addConstraint(3, GAIN_MEAN[il-1]-GAIN_RANGE[il-1],
                                       GAIN_MEAN[il-1]+GAIN_RANGE[il-1]);    
            }

            for(int is=is1; is<is2; is++) {
                for (int sl=1; sl<3 ; sl++) {
                    for(int ip = 1; ip < ccPix.nstr[0]+1; ip++) {
                        calib.addEntry(is,sl,ip);
                        calib.setDoubleValue(0.00, "Gain",   is,sl,ip);
                        calib.setDoubleValue(0.00, "GainErr",is,sl,ip);
                        calib.setDoubleValue(0.04, "FitMin", is,sl,ip);
                        calib.setDoubleValue(0.95, "FitMax", is,sl,ip);
                    }
                }
            }
            
            list.add(calib);  
        }
        
        public void updateTable(){
            
            String inputFile = getFileName(app.runNumber);
            
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
                    dum = Double.parseDouble(lineValues[3]); calib.setDoubleValue(dum,"Gain",   is,il,ip);
                    dum = Double.parseDouble(lineValues[4]); calib.setDoubleValue(dum,"GainErr",is,il,ip);
                    dum = Double.parseDouble(lineValues[5]); calib.setDoubleValue(dum,"FitMin", is,il,ip);
                    dum = Double.parseDouble(lineValues[6]); calib.setDoubleValue(dum,"FitMax", is,il,ip);
                    
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
            System.out.println("initCalibPane:CCGain");
            hPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
            JSplitPane   vPaneL = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
            JSplitPane   vPaneR = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
            hPane.setLeftComponent(vPaneL);
            hPane.setRightComponent(vPaneR);
            vPaneL.setTopComponent(fitAdc);
            vPaneL.setBottomComponent(fitSpe);
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
            
            fitAdc.actionPanel.add(bGv);
            fitAdc.actionPanel.add(bGs);
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
           fitAdc.actionPanel.add(xLabel);
           fitAdc.actionPanel.add(rangeSliderValue1);
           fitAdc.actionPanel.add(slider);
           fitAdc.actionPanel.add(rangeSliderValue2);           
           slider.addChangeListener(new ChangeListener() {
               public void stateChanged(ChangeEvent e) {
                   RangeSlider slider = (RangeSlider) e.getSource();
                   currentRangeMin = slider.getValue();
                   currentRangeMax = slider.getUpperValue();
                   rangeSliderValue1.setText(String.valueOf("" + String.format("%4.1f", currentRangeMin)));
                   rangeSliderValue2.setText(String.valueOf("" + String.format("%4.1f", currentRangeMax)));
                   int il =  lay; 
                   int ip1=1,ip2=1;
                   if (sliderMode=="Strip") {ip1=ic+1; ip2=ic+2;}
                   if (sliderMode=="View")  {ip1=1;  ip2=ccPix.nstr[0]+1;}
                   for (int ip=ip1; ip<ip2; ip++) {
                      calib.setDoubleValue(currentRangeMin*0.01,"FitMin",is, il, ip);
                      calib.setDoubleValue(currentRangeMax*0.01,"FitMax",is, il, ip);
                   }
                   calib.fireTableDataChanged();
                   analyze(is,is+1,il,il+1,ip1,ip2);
               }
           });   
       }
        
        @Override
        public void setCalibPane() {
            System.out.println("setCalibPane:CCGain");
            enginePane.setTopComponent(hPane);                        
        }    
        
        @Override
        public synchronized void analyze(int is1, int is2, int il1, int il2, int ip1, int ip2) {
            for (int is=is1 ; is<is2 ; is++) {
                for (int il=il1 ; il<il2 ; il++) { 
                    for (int ip=ip1 ; ip<ip2; ip++) {                      
                        CalibrationData fits = new CalibrationData(is, il, ip);
                        fits.getDescriptor().setType(DetectorType.LTCC);
                        fits.addGraph(ccPix.strips.hmap2.get("H2_a_Hist").get(is,il,0).sliceY(ip-1).getGraph());
                        fits.setFitLimits(calib.getDoubleValue("FitMin",is,il,ip),
                                          calib.getDoubleValue("FitMax",is,il,ip));
                        fits.analyze(0);
                        calib.setDoubleValue(fits.getFunc(0).parameter(1).value()/SPE_REF[il-1], "Gain",    is, il, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(1).error()/SPE_REF[il-1], "GainErr", is, il, ip);
                        collection.add(fits.getDescriptor(),fits);
//                        loadDataGroup(is,il,ip);                     
                    }
                }
            }
            calib.fireTableDataChanged();              
       }
            
        public void loadDataGroup(int is, int il, int ip) {
            DataGroup dg = new DataGroup(2,1);
            dg.addDataSet(ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,0).sliceY(ip-1).getGraph(), 0);
            
        }
        @Override
        public synchronized void drawPlots(int is, int il, int ic) {
            String[] otab  = {"LEFT PMT", "RIGHT PMT"};
            double mean,meane,sig,sige;
            
            GStyle.getGraphErrorsAttributes().setMarkerStyle(1);
            GStyle.getGraphErrorsAttributes().setMarkerColor(2);
            GStyle.getGraphErrorsAttributes().setMarkerSize(4);
            GStyle.getGraphErrorsAttributes().setLineColor(2);
            GStyle.getGraphErrorsAttributes().setLineWidth(1);
            GStyle.getGraphErrorsAttributes().setFillStyle(1);  
            
            if (app.getInProcess()>0) {
                int npmt = ccPix.nstr[0];
                if (app.getInProcess()==1&&app.getIsRunning())  {analyze(is,is+1,il,il+1,1,npmt+1);}
                if (collection.hasEntry(is, il, ic+1)) {
                    for (int ip=0; ip<npmt ; ip++) {
                    // Fill Arrays
                          xp[ip] = ip+1;     
                         xpe[ip] = 0.; 
                            mean = collection.get(is,il,ip+1).getFunc(0).parameter(1).value();
                           meane = collection.get(is,il,ip+1).getFunc(0).parameter(1).error();
                             sig = collection.get(is,il,ip+1).getFunc(0).parameter(2).value();
                            sige = collection.get(is,il,ip+1).getFunc(0).parameter(2).error();
                        parB[ip] = mean/SPE_REF[il-1]; 
                       parBe[ip] = meane/SPE_REF[il-1];
                        parC[ip] = Math.min(2.5, sig/mean);           
                       parCe[ip] = parC[ip]*Math.sqrt((meane/mean)*(meane/mean)+(sige/sig)*(sige/sig));                        
                     double chi2 = collection.get(is,il,ip+1).getFunc(0).getChiSquare()/
                                   collection.get(is,il,ip+1).getFunc(0).getNDF();
                       vchi2[ip] = Math.min(4, chi2); 
                      vchi2e[ip] = 0.;
                    }
                    
                    // Identify current mouseover PMT
                    GStyle.getGraphErrorsAttributes().setLineColor(3);
                    GStyle.getGraphErrorsAttributes().setLineWidth(2);
                    GStyle.getGraphErrorsAttributes().setMarkerColor(3);
                    xpix[0]=xp[ic]; ypix[0]=vchi2[ic]; yerr[0]=vchi2e[ic];
                    GraphErrors  chi2Graphi = new GraphErrors("chi",xpix,ypix,xerr,yerr);
                    xpix[0]=xp[ic]; ypix[0]=parB[ic]; yerr[0]=parBe[ic];
                    GraphErrors  parBGraphi = new GraphErrors("Bi",xpix,ypix,xerr,yerr);
                    xpix[0]=xp[ic]; ypix[0]=parC[ic]; yerr[0]=parCe[ic];
                    GraphErrors  parCGraphi = new GraphErrors("Ci",xpix,ypix,xerr,yerr);
                    
                    // Setup summary plots                    
                    GStyle.getGraphErrorsAttributes().setLineColor(2);
                    GStyle.getGraphErrorsAttributes().setLineWidth(1);
                    GStyle.getGraphErrorsAttributes().setMarkerColor(2);
                    GraphErrors  chi2Graph = new GraphErrors("chi2",xp,vchi2,xpe,vchi2e);
                    GraphErrors  parBGraph = new GraphErrors("B",xp,parB,xpe,parBe); 
                    GraphErrors  parCGraph = new GraphErrors("C",xp,parC,xpe,parCe); 
                    
                    parBGraph.getAttributes().setTitleX(otab[il-1]) ; 
                    parBGraph.getAttributes().setTitleY("B / SPE") ;               
                    parBGraph.getAttributes().setTitle(" ");
                    parCGraph.getAttributes().setTitleX(otab[il-1]) ; 
                    parCGraph.getAttributes().setTitleY("C / B") ;                
                    parCGraph.getAttributes().setTitle(" ");
                    chi2Graph.getAttributes().setTitleX(otab[il-1]) ;  
                    chi2Graph.getAttributes().setTitleY("REDUCED CHI^2"); 
                    chi2Graph.getAttributes().setTitle(" ");                                       
                    
                    c = fitAdc.getCanvas("ADC Fit");  
                    if(collection.get(is,il,ic+1).getFitGraph(0).getDataSize(0)>0) {
                        c.getPad(0).getAxisX().setRange(0.,ccPix.amax); 
                        c.draw(collection.get(is,il,ic+1).getFitGraph(0));                                                 
                    }
                    c.repaint();  
                    
                    c = fitCh2.getCanvas("Chi^2"); 
                    
                    c.getPad(0).getAxisY().setRange(0.,4.);
                    c.draw(chi2Graph) ; c.draw(chi2Graphi,"same");
                    c.repaint();
                    
                    c = fitCof.getCanvas("COEF"); c.divide(1,2);
                    
                    c.cd(0); c.getPad(0).getAxisY().setRange(0.,2.0);
                    c.draw(parBGraph); c.draw(parBGraphi,"same");                       
                    c.cd(1); c.getPad(1).getAxisY().setRange(0.,2.5);
                    c.draw(parCGraph); c.draw(parCGraphi,"same");
                    
                    c.repaint();
                    
                    
                }
            }
        }
        
        @Override
        public void fit(int sector, int layer, int paddle, double minRange, double maxRange){ 
           double mean = ccPix.strips.hmap2.get("H2_a_Hist").get(sector,layer,0).sliceY(paddle-1).getMean();
           calib.addEntry(sector, layer, paddle);
           calib.setDoubleValue(mean, "gain", sector, layer, paddle);
        } 
        
        @Override
        public void actionPerformed(ActionEvent e) {
          sliderMode = e.getActionCommand();
        }       
        
    }
    
    private class CCStatusEventListener extends CCCalibrationEngine {
        
        public final int[]    EXPECTED_STATUS = {0,0};
        public final int  ALLOWED_STATUS_DIFF = 1;
        
        CCStatusEventListener(){};
        
        public void init(int is1, int is2){
            calib = new CalibrationConstants(3,"status/I");
            calib.setName("/calibration/ltcc/status");
            calib.setPrecision(3);
            
            for (int i=0 ; i<2; i++) {
                calib.addConstraint(3, EXPECTED_STATUS[i]-ALLOWED_STATUS_DIFF,
                                       EXPECTED_STATUS[i]+ALLOWED_STATUS_DIFF);
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setIntValue(0,"status",is,il,ip);
                    }
                }
            }
            list.add(calib);
        }
        
    }
    
    private class CCTimingEventListener extends CCCalibrationEngine {
        
        public final int[]    EXPECTED_TIMING = {0,0};
        public final int  ALLOWED_TIMING_DIFF = 1;
        
        CCTimingEventListener(){};
        
        public void init(int is1, int is2) {
            calib = new CalibrationConstants(3,"offset/F");
            calib.setName("/calibration/ltcc/timing_offset");
            calib.setPrecision(3);
            
            for (int i=0 ; i<2; i++) {
                calib.addConstraint(3, EXPECTED_TIMING[i]-ALLOWED_TIMING_DIFF,
                                       EXPECTED_TIMING[i]+ALLOWED_TIMING_DIFF);
            }
            
            for(int is=is1; is<is2; is++) {                
                for(int il=1; il<3; il++) {
                    for(int ip = 1; ip < 19; ip++) {
                        calib.addEntry(is,il,ip);
                        calib.setDoubleValue(0.0,"offset",is,il,ip);
                    }
                }
            }
            list.add(calib);
        }
                
    }
 
}
