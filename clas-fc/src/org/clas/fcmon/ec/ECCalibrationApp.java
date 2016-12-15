package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.CalibrationData;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
//import org.jlab.clasrec.utils.DatabaseConstantProvider;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.utils.groups.IndexedTable;

public class ECCalibrationApp extends FCApplication implements CalibrationConstantsListener,ChangeListener {
    
    JPanel                    engineView = new JPanel();
    JSplitPane                enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
    CalibrationConstantsView      ccview = new CalibrationConstantsView();
    ConstantsManager                ccdb = new ConstantsManager();
    ArrayList<CalibrationConstants> list = new ArrayList<CalibrationConstants>();

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
        for (int i=0; i<engines.length; i++) engines[i].init(ECConstants.IS1,ECConstants.IS2); 
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
        return engineView;       
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
        engine.analyze(idet,is1,is2,il1,il2);
    }   
    
    public void analyzeAllEngines(int is1, int is2, int il1, int il2) {
        for (int i=0; i<engines.length; i++) {
            for (int idet=0; idet<ecPix.length; idet++) engines[i].analyze(idet,is1,is2,il1,il2); 
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
    }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
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
        ECCalibrationEngine engine = getSelectedEngine(); engine.setCalibPane();
    }

    // Calibration Apps
    
    private class ECHvEventListener extends ECCalibrationEngine {
        
        EmbeddedCanvasTabbed  ECHv = new EmbeddedCanvasTabbed("HV");
        public final double PARAM3 = 1.0;
        public final int    PARAM6 = 0;
        public final double[] GAIN = {0.2,0.2,0.2,0.5,0.5,0.5,0.5,0.5,0.5};
        public final int[]    DHV = {20,20,20,100,100,100,100,100,100};
        IndexedTable        status = null; 
        
        ECHvEventListener(){}
        
        public void init(int is1, int is2){
            
            System.out.println("ECCalibrationApp:ECHvEventListener.init");
            
            initCalibPane();
            setCalibPane();
            
            status = ccdb.getConstants(calrun, names[STATUS]);
            calib = new CalibrationConstants(3,"Gain/F:HVold/F:HVnew/F:DHV/F");
            calib.setName(names[HV]);
            calib.setPrecision(3);
            
            for (int i=0; i<9; i++) {
                calib.addConstraint(3, PARAM3-GAIN[i], 
                                       PARAM3+GAIN[i], 1, i+1);
                calib.addConstraint(6, PARAM6-DHV[i], 
                                       PARAM6+DHV[i], 1, i+1);
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
        public void analyze(int idet, int is1, int is2, int il1, int il2) {
            for (int is=is1; is<is2; is++) {
                for (int il=il1; il<il2; il++) {
                    for (int ip=1; ip<ecPix[idet].ec_nstr[il-1]+1; ip++) { 
                        int sl = 3*idet+il;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                        calib.addEntry(is, sl, ip);
                        calib.setDoubleValue(app.fifo1.get(is, sl, ip).getLast(), "HVold", is, sl, ip);                        
                    }
                }
            }
            CalibrationConstants gains = engines[0].calib;
            
            for (int is=is1; is<is2; is++) {
                for (int il=il1; il<il2; il++) {
                    for (int ip=1; ip<ecPix[idet].ec_nstr[il-1]+1; ip++) { 
                        int sl = 3*idet+il;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                        double  gain = gains.getDoubleValue("A", is,sl,ip)+gains.getDoubleValue("C", is,sl,ip);
                        double hvold = calib.getDoubleValue("HVold", is,sl,ip);
                        calib.setDoubleValue(gain,"Gain", is, sl, ip);                        
                        if (gain<0.5||gain>2.0) gain=0.0;
                        double ratio=Math.pow(gain, 1./11.);
                        double hvnew = (ratio>0.5) ? hvold/ratio:hvold;
                        calib.setDoubleValue(hvnew,"HVnew", is, sl, ip);                        
                        calib.setDoubleValue(hvnew-hvold,"DHV", is, sl, ip);                        
                    }
                }
            }
            
            calib.fireTableDataChanged();     
        }
         
        public void drawPlots(int is, int il, int ic) {
            if (il>3) return;
            if (app.getInProcess()<2) analyze(ilmap,is,is+1,il,il+1);
            EmbeddedCanvas  c = new EmbeddedCanvas(); 
            int nstr = ecPix[ilmap].ec_nstr[il-1];
            H1F hvold = new H1F(" ",nstr,1.,nstr+1);
            H1F hvnew = new H1F(" ",nstr,1.,nstr+1);
            H1F   dhv = new H1F(" ",nstr,1.,nstr+1);
            hvold.setFillColor(33);
            hvnew.setFillColor(32);
              dhv.setFillColor(32);
            for (int ip=1; ip<nstr+1; ip++) hvold.fill(ip,app.fifo1.get(is, 3*ilmap+il, ip).getLast());
            for (int ip=1; ip<nstr+1; ip++) hvnew.fill(ip,calib.getDoubleValue("HVnew", is,3*ilmap+il,ip));
            for (int ip=1; ip<nstr+1; ip++)   dhv.fill(ip,calib.getDoubleValue("DHV",   is,3*ilmap+il,ip));
             
            c = ECHv.getCanvas("HV"); 
            c.divide(2,2);
            c.getPad(0).getAxisY().setRange((double)getMin(hvnew.getData())-10,(double)getMax(hvnew.getData())+10);
            c.cd(0); c.draw(hvold); c.draw(hvnew,"same");
            c.getPad(1).getAxisY().setRange((double)getMin(dhv.getData())-10,(double)getMax(dhv.getData())+10);
            c.cd(1); c.draw(dhv);
            if (app.getInProcess()>0)  {
                DetectorCollection<CalibrationData> fit = ecPix[ilmap].collection;
//                System.out.println("ilmap,is,il="+ilmap+" "+is+" "+il);
                if (app.getInProcess()==1&&app.getIsRunning()) engines[0].analyze(ilmap,is,is+1,il,il+1);
                if (fit.hasEntry(is, il, ic+1)) {
                    c.getPad(2).getAxisX().setRange(0.,400.); c.getPad(2).getAxisY().setRange(0.,300.);
                    if(fit.get(is,il,ic+1).getFitGraph(0).getDataSize(0)>0) {
                        c.getPad(2).getAxisX().setRange(0.,400.); c.getPad(2).getAxisY().setRange(0.,300.);
                        c.cd(2); c.draw(fit.get(is,il,ic+1).getFitGraph(0));
                    }
                }
            }
            c.repaint();                                    
        }
    }  
  
    public class ECAttenEventListener extends ECCalibrationEngine {
        
        JSplitPane              hPane = null; 
        EmbeddedCanvasTabbed   fitPix = new EmbeddedCanvasTabbed("Pixel Fits");
        EmbeddedCanvasTabbed   fitADC = new EmbeddedCanvasTabbed("ADC");
        EmbeddedCanvasTabbed   fitCh2 = new EmbeddedCanvasTabbed("Chi^2");
        EmbeddedCanvasTabbed   fitCof = new EmbeddedCanvasTabbed("COEF");
        IndexedTable            atten = null; 
        
        public final double[][] PARAM = {{1,1,1,1,1,1,1,1,1},
                                         {360,360,360,360,360,360,360,360,360},
                                         {0.25,0.25,0.25,0,0,0,0,0,0}};
        public final double[][] DELTA = {{0.25,0.25,0.25,0.2,0.2,0.2,0.2,0.2,0.2},
                                         {160,160,160,160,160,160,160,160,160},
                                         {0.25,0.25,0.25,0,0,0,0,0,0}};
        public final        int[] MIP = {100,100,100,100,100,100,160,160,160};
        
        int is1,is2;
        int nstr=68;
        
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
            
            initCalibPane();
            setCalibPane();
            
            this.is1=is1;
            this.is2=is2;
            
            atten = ccdb.getConstants(calrun, names[ATTEN]);
            calib = new CalibrationConstants(3,"A/F:Aerr/F:B/F:Berr/F:C/F:Cerr/F");
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
                    for (int il=0; il<3 ; il++) {
                        int layer = il+idet*3;
                        for(int ip = 0; ip < ecPix[idet].ec_nstr[il]; ip++) {
                            calib.addEntry(is, layer+1, ip+1);
                            calib.setDoubleValue(0., "A",    is, layer+1, ip+1);
                            calib.setDoubleValue(0., "Aerr", is, layer+1, ip+1);
                            calib.setDoubleValue(0., "B",    is, layer+1, ip+1);
                            calib.setDoubleValue(0., "Berr", is, layer+1, ip+1);
                            calib.setDoubleValue(0., "C",    is, layer+1, ip+1);
                            calib.setDoubleValue(0., "Cerr", is, layer+1, ip+1);
                        }
                    }
                }
            }
            
            list.add(calib);         
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
        public void analyze(int idet, int is1, int is2, int il1, int il2) {
            
            TreeMap<Integer, Object> map;
            boolean doCalibration=false;
            int npix = ecPix[idet].pixels.getNumPixels();
            double  meanerr[] = new double[npix];
            boolean  status[] = new boolean[npix];
                      
            for (int is=is1 ; is<is2 ; is++) {
               for (int il=il1 ; il<il2 ; il++) { 
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
                      
                  for (int ip=1 ; ip<ecPix[idet].ec_nstr[il-1]+1 ; ip++) {
                     if (doCalibration) {
                        fits = new CalibrationData(is,il,ip);
                        fits.getDescriptor().setType(DetectorType.EC);
                        fits.addGraph(ecPix[idet].strips.getpixels(il,ip,cnts),
                                      ecPix[idet].strips.getpixels(il,ip,distmap),
                                      ecPix[idet].strips.getpixels(il,ip,meanmap),
                                      ecPix[idet].strips.getpixels(il,ip,meanerr),
                                      ecPix[idet].strips.getpixels(il,ip,status));
                        fits.analyze(idet);
                        int sl = il+idet*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                        calib.addEntry(is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(0).value()/MIP[sl-1], "A",    is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(1).value(),           "B",    is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(2).value()/MIP[sl-1], "C",    is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(0).error()/MIP[sl-1], "Aerr", is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(1).error(),           "Berr", is, sl, ip);
                        calib.setDoubleValue(fits.getFunc(0).parameter(2).error()/MIP[sl-1], "Cerr", is, sl, ip);
                      
                        ecPix[idet].collection.add(fits.getDescriptor(),fits);
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
        public void drawPlots(int is, int layer, int ic) {
            
            DetectorCollection<CalibrationData> fit = ecPix[ilmap].collection;
            DetectorCollection<H2F>            dc2a = ecPix[ilmap].strips.hmap2.get("H2_a_Hist");    
            EmbeddedCanvas                        c = new EmbeddedCanvas();    
            Boolean   isPix = false;
            Boolean   isStr = false;
            H1F      pixADC = null;
            int          il = 0;
            int    pixStrip = 0;
            int        nstr = ecPix[0].ec_nstr[0];
             
            String otab[][]={{" U PMT "," V PMT "," W PMT "},
                    {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
                    {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
                      
            isPix = layer > 10;
            isStr = layer <  7;  
            il    = layer;
                                        
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
                     if (app.getInProcess()==1&&app.getIsRunning())  {analyze(ilmap,is,is+1,il,il+1);}
                     if (fit.hasEntry(is, il, pixStrip)) {
                     int sl = il+ilmap*3;  // Superlayer PCAL:1-3 ECinner: 4-6 ECouter: 7-9
                                                                   
                     for (int ip=0; ip<nstr ; ip++) {
                         xp[ip]    = ip+1;     
                         xpe[ip]   = 0.; 
                         parA[ip]  = fit.get(is,il,ip+1).getFunc(0).parameter(0).value()/MIP[sl-1];
                         parAe[ip] = fit.get(is,il,ip+1).getFunc(0).parameter(0).error()/MIP[sl-1];
                         parB[ip]  = fit.get(is,il,ip+1).getFunc(0).parameter(1).value();
                         parBe[ip] = fit.get(is,il,ip+1).getFunc(0).parameter(1).error();
                         parC[ip]  = fit.get(is,il,ip+1).getFunc(0).parameter(2).value()/MIP[sl-1];
                         parCe[ip] = fit.get(is,il,ip+1).getFunc(0).parameter(2).error()/MIP[sl-1];
                         parAC[ip] = parA[ip]+parC[ip];
                         parACe[ip]= Math.sqrt(parAe[ip]*parAe[ip]+parCe[ip]*parCe[ip]);
                         vchi2[ip] = Math.min(4, fit.get(is,il,ip+1).getChi2(0)); 
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
                     GStyle.getGraphErrorsAttributes().setMarkerStyle(1);
                     GStyle.getGraphErrorsAttributes().setMarkerColor(2);
                     GStyle.getGraphErrorsAttributes().setMarkerSize(3);
                     GStyle.getGraphErrorsAttributes().setLineColor(2);
                     GStyle.getGraphErrorsAttributes().setLineWidth(1);
                     GStyle.getGraphErrorsAttributes().setFillStyle(1);                     
                     GraphErrors  parAGraph = new GraphErrors("A",xp,parA,xpe,parAe); 
                     GraphErrors  parBGraph = new GraphErrors("B",xp,parB,xpe,parBe); 
                     GraphErrors  parCGraph = new GraphErrors("C",xp,parC,xpe,parCe); 
                     GraphErrors parACGraph = new GraphErrors("A+C",xp,parAC,xpe,parACe); 
                     GraphErrors   pixGraph = new GraphErrors("pix",xpix,ypix,xerr,yerr);
                     GStyle.getGraphErrorsAttributes().setMarkerColor(1);
                     GStyle.getGraphErrorsAttributes().setFillStyle(0);
                     GraphErrors ccdbAGraph = new GraphErrors("dbA",xp,ccdbA,xpe,ccdbAe); 
                     GraphErrors ccdbBGraph = new GraphErrors("dbB",xp,ccdbB,xpe,ccdbBe); 
                     GraphErrors ccdbCGraph = new GraphErrors("dbC",xp,ccdbC,xpe,ccdbCe); 
                     GraphErrors ccdbACGraph = new GraphErrors("dbAC",xp,ccdbAC,xpe,ccdbACe); 
                     GStyle.getGraphErrorsAttributes().setMarkerColor(2);
                     GStyle.getGraphErrorsAttributes().setFillStyle(2);
                     GraphErrors  chi2Graph = new GraphErrors("chi2",xp,vchi2,xpe,vchi2e);
                       
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
                      
                     F1D f1 = new F1D("p0","[a]",0,nstr+1); f1.setParameter(0,MIP[sl-1]); f1.setLineWidth(1);
                               
                     double ymax=2.; 
                     
                     c = fitPix.getCanvas("Pixel Fits"); 
                     
                     c.getPad(0).getAxisX().setRange(0.,400.);c.getPad(0).getAxisY().setRange(0.,300.); 
                     c.draw(fit.get(is,il,pixStrip).getRawGraph(0));  
                     if(fit.get(is,il,pixStrip).getFitGraph(0).getDataSize(0)>0) {
                         c.draw(fit.get(is,il,pixStrip).getFitGraph(0),"same");                                                 
                     }
                     if (isPix) c.draw(pixGraph,"same");
                     c.repaint();

                     c = fitADC.getCanvas("ADC"); 
                     
                     c.cd(0); pixADC.setOptStat(Integer.parseInt("110")); pixADC.setTitle(""); c.draw(pixADC);                    
                     c.repaint();
                     
                     c = fitCh2.getCanvas("Chi^2"); 
                     
                     double xmax = ecPix[ilmap].ec_nstr[il-1]+1;
                     
                     c.getPad(0).getAxisX().setRange(0.,xmax);c.getPad(0).getAxisY().setRange(0.,4.);
                     c.draw(chi2Graph) ;
                     c.repaint();
                     
                     c = fitCof.getCanvas("COEF"); c.divide(2,2);
                     
                     c.cd(0); c.getPad(0).getAxisX().setRange(0.,xmax);c.getPad(0).getAxisY().setRange(0.,2.);
                     c.draw(ccdbAGraph) ; c.draw(parAGraph,"same"); 
                     c.cd(1); c.getPad(1).getAxisX().setRange(0.,xmax);c.getPad(1).getAxisY().setRange(0.,600.);
                     c.draw(ccdbBGraph); c.draw(parBGraph,"same");                       
                     c.cd(2); c.getPad(2).getAxisX().setRange(0.,xmax);c.getPad(2).getAxisY().setRange(0.,2.);
                     c.draw(ccdbCGraph) ; c.draw(parCGraph,"same"); 
                     c.cd(3); c.getPad(3).getAxisX().setRange(0.,xmax);c.getPad(3).getAxisY().setRange(0.,2.);
                     c.draw(ccdbACGraph) ; c.draw(parACGraph,"same"); 
                     c.repaint();
                      
                  }
               }
            }
        }

    }
    
    public class ECGainEventListener extends ECCalibrationEngine {
        
        public final int[]      PARAM = {100,100,100,100,100,100,160,160,160};
        public final int        DELTA = 10;        
        IndexedTable             gain = null; 
        
        int is1,is2;
        
        ECGainEventListener(){};
        
        public void init(int is1, int is2) {
            
            System.out.println("ECCalibrationApp:ECGainEventListener.init");
            this.is1=is1;
            this.is2=is2;
            
            gain = ccdb.getConstants(calrun, names[GAIN]);
            calib = new CalibrationConstants(3,"gain/F");
            calib.setName(names[GAIN]);
            calib.setPrecision(3);

            for (int i=0; i<9; i++) { 
                calib.addConstraint(3, PARAM[i]-DELTA, 
                                       PARAM[i]+DELTA, 1, i+1);
            }
            
            list.add(calib);         
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
            return (getTestChannel(sector,layer,paddle) >=PARAM[layer-1]-DELTA  &&
                    getTestChannel(sector,layer,paddle) <=PARAM[layer-1]+DELTA );
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
