package org.clas.fcmon.cc;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12
import org.jlab.detector.base.DetectorCollection;

//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.io.base.DataEvent;

public class CCMon extends DetectorMonitor {
	
    static MonitorApp           app = new MonitorApp("LTCCMon",1800,950);
    
    CCPixels                  ccPix = new CCPixels();
    ConstantsManager           ccdb = new ConstantsManager();  
    FTHashCollection            rtt = null;    
    CCDetector                ccDet = null;  
    
    CCReconstructionApp     ccRecon = null;
    CCMode1App              ccMode1 = null;
    CCOccupancyApp      ccOccupancy = null;
    CCPedestalApp        ccPedestal = null;
    CCSpeApp                  ccSpe = null;    
    CCCalibrationApp        ccCalib = null;
    CCScalersApp          ccScalers = null;
    CCHvApp                    ccHv = null;
    
    public int               calRun = 2;
    int                       detID = 0;
    int                         is1 = 1 ;
    int                         is2 = 2 ; 
    int    nsa,nsb,tet,p1,p2,pedref = 0;
    double               PCMon_zmin = 0;
    double               PCMon_zmax = 0;
    
    String                   mondet = "LTCC";
    
    DetectorCollection<H1F> H1_CCa_Sevd = new DetectorCollection<H1F>();
    DetectorCollection<H1F> H1_CCt_Sevd = new DetectorCollection<H1F>();
    DetectorCollection<H2F> H2_CCa_Hist = new DetectorCollection<H2F>();
    DetectorCollection<H2F> H2_CCt_Hist = new DetectorCollection<H2F>();
    DetectorCollection<H2F> H2_CCa_Sevd = new DetectorCollection<H2F>();
		
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public CCMon(String det) {
        super("CCMON", "1.0", "lcsmith");
        mondet = det;
    }

    public static void main(String[] args){		
        String det = "LTCC";
        CCMon monitor = new CCMon(det);	
        if (args.length != 0) {
            monitor.is1=Integer.parseInt(args[0]); 
            monitor.is2=Integer.parseInt(args[1]);    
         }
        app.setPluginClass(monitor);
        app.makeGUI();
        app.getEnv();
        monitor.initConstants();
        monitor.initCCDB();
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.initEPICS();
        monitor.init();
        monitor.initDetector();
        app.init();
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(1); 
        app.setIsMC(false);
        monitor.ccDet.initButtons();
    }
    
    public FTHashCollection getReverseTT(ConstantsManager ccdb) {
        System.out.println("monitor.getReverseTT()"); 
        IndexedTable tt = ccdb.getConstants(10,  "/daq/tt/ltcc");
        FTHashCollection rtt = new FTHashCollection<int[]>(4);
        for(int ic=1; ic<35; ic++) {
            for (int sl=16; sl<21; sl++) {
                int chmax=16;
                if (sl==16) chmax=128;
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
        return rtt;
    }  
    
    public void initConstants() {
        CCConstants.setSectors(is1,is2);
    }

    public void initCCDB() {
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ltcc",
                "/daq/tt/ltcc",
                "/calibration/ltcc/gain",
                "/calibration/ltcc/timing_offset",
                "/calibration/ltcc/status"}));
        rtt = getReverseTT(ccdb);
        app.mode7Emulation.init(ccdb, calRun, "/daq/fadc/ltcc", 1,18,12);        
    }
    
    public void initDetector() {
        System.out.println("monitor.initDetector()"); 
        ccDet = new CCDetector("CCDet",ccPix);
        ccDet.setMonitoringClass(this);
        ccDet.setApplicationClass(app);
        ccDet.init(is1,is2);
    }
	
    public void makeApps() {
        
        System.out.println("monitor.makeApps()");  
        
        ccRecon = new CCReconstructionApp("CCREC",ccPix);        
        ccRecon.setMonitoringClass(this);
        ccRecon.setApplicationClass(app);	
        
        ccMode1 = new CCMode1App("Mode1",ccPix);        
        ccMode1.setMonitoringClass(this);
        ccMode1.setApplicationClass(app);   
        
        ccOccupancy = new CCOccupancyApp("Occupancy",ccPix);        
        ccOccupancy.setMonitoringClass(this);
        ccOccupancy.setApplicationClass(app);           
        
        ccPedestal = new CCPedestalApp("Pedestal",ccPix);        
        ccPedestal.setMonitoringClass(this);
        ccPedestal.setApplicationClass(app);       
        
        ccSpe = new CCSpeApp("SPE",ccPix);        
        ccSpe.setMonitoringClass(this);
        ccSpe.setApplicationClass(app);  
        
        ccCalib = new CCCalibrationApp("Calibration", ccPix);
        ccCalib.setMonitoringClass(this);
        ccCalib.setApplicationClass(app);  
        ccCalib.setConstantsManager(ccdb,calRun);
        ccCalib.init();
        
        ccHv = new CCHvApp("HV","LTCC");
        ccHv.setMonitoringClass(this);
        ccHv.setApplicationClass(app);  
        
        ccScalers = new CCScalersApp("Scalers","LTCC");
        ccScalers.setMonitoringClass(this);
        ccScalers.setApplicationClass(app);  
    }
	
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addFrame(ccMode1.getName(),         ccMode1.getPanel());
        app.addCanvas(ccOccupancy.getName(), ccOccupancy.getCanvas());          
        app.addCanvas(ccPedestal.getName(),   ccPedestal.getCanvas());
        app.addCanvas(ccSpe.getName(),             ccSpe.getCanvas()); 
        app.addFrame(ccCalib.getName(),          ccCalib.getPanel());
        app.addFrame(ccHv.getName(),                ccHv.getPanel());
        app.addFrame(ccScalers.getName(),      ccScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println("monitor.init()");   
        app.setInProcess(0); 
        initApps();
        ccPix.initHistograms(" ");
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        ccPix.init();
        ccRecon.init();
    }
    
    public void initEPICS() {
        System.out.println("monitor.initScalers():Initializing EPICS Channel Access");
        ccHv.init(app.doEpics);        
        ccScalers.init(app.doEpics);         
    }
    
    public void initGlob() {
        System.out.println("monitor.initGlob()");
        putGlob("detID", detID);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("PCMon_zmin", PCMon_zmin);
        putGlob("PCMon_zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",CCConstants.IS1);
        putGlob("is2",CCConstants.IS2);
        putGlob("calRun",calRun);
   }
    
    @Override
    public TreeMap<String,Object> getGlob(){
        return this.glob;
    }	
    
    @Override
    public void putGlob(String name, Object obj){
        glob.put(name,obj);
    }  
    
    @Override
    public void reset() {
        ccRecon.clearHistograms();
    }	
	
    @Override
    public void dataEventAction(DataEvent de) {
        ccRecon.addEvent(de);	
    }

    @Override
    public void update(DetectorShape2D shape) {
        ccDet.update(shape);
        //ccCalib.updateDetectorView(shape);
    }
		
    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: ccRecon.makeMaps();  break;
        case 2: ccRecon.makeMaps();
                ccCalib.analyzeAllEngines(is1,is2,1,3);   
                app.setInProcess(3); 
        }
    }

    @Override
    public void processShape(DetectorShape2D shape) {       
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatus(getStatusString(dd));
        this.analyze();        
        switch (app.getSelectedTabName()) {
        case "Mode1":                ccMode1.updateCanvas(dd); break;
        case "Occupancy":        ccOccupancy.updateCanvas(dd); break;
        case "Pedestal":          ccPedestal.updateCanvas(dd); break;
        case "SPE":                    ccSpe.updateCanvas(dd); break; 
        case "Calibration":          ccCalib.updateCanvas(dd); break; 
        case "HV":                      ccHv.updateCanvas(dd); break;
        case "Scalers":            ccScalers.updateCanvas(dd);
        }                       
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ccHv.loadHV(is1,is2,il1,il2);
    }
    
    public String getStatusString(DetectorDescriptor dd) {
        
        String comp=(dd.getLayer()==4) ? "  Pixel:":"  PMT:";  
      
        int is = dd.getSector();
        int lr = dd.getLayer();
        int ic = dd.getComponent()+1;
        int or = 0;
        int cr = 0;
        int sl = 0;
        int ch = 0;
        if (app.getSelectedTabName()=="TDC") or=2;
        if (rtt.hasItem(is,lr,ic,or)) {
            int[] dum = (int[]) rtt.getItem(is,lr,ic,or);
            cr = dum[0];
            sl = dum[1];
            ch = dum[2];
        }   
        return " Sector:"+is+"  Layer:"+lr+comp+ic+" "+" Crate:"+cr+" Slot:"+sl+" Chan:"+ch;
    } 
    
    @Override
    public void resetEventListener() {
    }

    @Override
    public void timerUpdate() {
    }
    
    @Override
    public void readHipoFile() {        
        System.out.println("monitor.readHipoFile()");
        String hipoFileName = app.hipoPath+mondet+"_"+app.runNumber+".hipo";
        System.out.println("Reading Histograms from "+hipoFileName);
        ccPix.initHistograms(hipoFileName);
        ccOccupancy.analyze();
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println("monitor.writeHipoFile()");
        String hipoFileName = app.hipoPath+mondet+"_"+app.runNumber+".hipo";
        System.out.println("Saving Histograms to "+hipoFileName);
        HipoFile histofile = new HipoFile(hipoFileName);
        histofile.addToMap("H2_CCa_Hist", this.H2_CCa_Hist);
        histofile.addToMap("H2_CCt_Hist", this.H2_CCt_Hist);
        histofile.writeHipoFile(hipoFileName);
    }
    
    @Override
    public void close() {
        app.displayControl.setFPS(1);
    }
    
    @Override
    public void pause() {
        app.displayControl.setFPS(1);
        
    }

    @Override
    public void go() {
        app.displayControl.setFPS(10);
        
    }
 
    @Override
    public void initEngine() {
        // TODO Auto-generated method stub
        
    }
    
}
