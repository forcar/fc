package org.clas.fcmon.cc;

import java.util.TreeMap;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.FADCConfigLoader;
import org.jlab.clasrec.utils.DatabaseConstantProvider;


//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.base.DataEvent;

public class CCMon extends DetectorMonitor {
	
    static MonitorApp           app = new MonitorApp("LTCCMon",1800,950);
    
    CCPixels                  ccPix = new CCPixels();
    FADCConfigLoader           fadc = new FADCConfigLoader();
  
    CCDetector                ccDet = null;  
    
    CCReconstructionApp     ccRecon = null;
    CCMode1App              ccMode1 = null;
    CCOccupancyApp      ccOccupancy = null;
    CCPedestalApp        ccPedestal = null;
    CCSpeApp                  ccSpe = null;    
    CCCalibrationApp        ccCalib = null;
    CCScalersApp          ccScalers = null;
    CCHvApp                    ccHv = null;
    
    DatabaseConstantProvider   ccdb = null;
        
    public boolean             inMC = false; //true=MC false=DATA
    public int               calRun = 2;
    public int            inProcess = 0;     //0=init 1=processing 2=end-of-run 3=post-run
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
        ccdb = new DatabaseConstantProvider(calRun,"default");
        ccdb.loadTable("/calibration/ltcc/gain");
        ccdb.disconnect();
    }

    public static void main(String[] args){		
        String det = "LTCC";
        CCMon monitor = new CCMon(det);	
        app.setPluginClass(monitor);
        app.getEnv();
        app.makeGUI();
        app.mode7Emulation.init("/daq/fadc/ltcc",1, 18, 12);
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.init();
        monitor.initDetector();
        app.init();
        monitor.ccDet.initButtons();
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
        ccCalib.init(is1,is2);
        
        ccHv = new CCHvApp("HV","LTCC");
        ccHv.setMonitoringClass(this);
        ccHv.setApplicationClass(app);  
        
        ccScalers = new CCScalersApp("Scalers","LTCC");
        ccScalers.setMonitoringClass(this);
        ccScalers.setApplicationClass(app);  
    }
	
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addCanvas(ccMode1.getName(),         ccMode1.getCanvas());
        app.addCanvas(ccOccupancy.getName(), ccOccupancy.getCanvas());          
        app.addCanvas(ccPedestal.getName(),   ccPedestal.getCanvas());
        app.addCanvas(ccSpe.getName(),             ccSpe.getCanvas()); 
        app.addFrame(ccCalib.getName(),          ccCalib.getCalibPane());
        app.addFrame(ccHv.getName(),                ccHv.getScalerPane());
        app.addFrame(ccScalers.getName(),      ccScalers.getScalerPane());
    }
    
    public void init( ) {       
        System.out.println("monitor.init()");   
        inProcess = 0; putGlob("inProcess", inProcess);
        initApps();
        ccPix.initHistograms(" ");
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        ccPix.init();
        ccRecon.init();
        if (!app.doEpics) {
            ccHv.init(is1,is2);        
            ccScalers.init(is1,is2); 
        }
    }
    
    public void initGlob() {
        System.out.println("monitor.initGlob()");
        putGlob("inProcess", inProcess);
        putGlob("detID", detID);
        putGlob("inMC", inMC);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("ccdb", ccdb);
        putGlob("PCMon_zmin", PCMon_zmin);
        putGlob("PCMon_zmax", PCMon_zmax);
        putGlob("fadc",fadc);
        putGlob("mondet",mondet);
        putGlob("is1",is1);
        putGlob("is2",is2);
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
    public void close() {	
    }	
	
    @Override
    public void dataEventAction(DataEvent de) {
        ccRecon.addEvent(de);	
    }

    @Override
    public void update(DetectorShape2D shape) {
        putGlob("inProcess", inProcess);
        ccDet.update(shape);
        ccCalib.updateDetectorView(shape);
    }
		
    @Override
    public void analyze(int process) {
        this.inProcess = process; glob.put("inProcess", process);
        if (process==1||process==2) {
            ccRecon.makeMaps();	
            ccCalib.engines[0].analyze();
        }
    }

    @Override
    public void processShape(DetectorShape2D shape) {       
        DetectorDescriptor dd = shape.getDescriptor();
        this.analyze(inProcess);        
        switch (app.getSelectedTabName()) {
        case "Mode1":                ccMode1.updateCanvas(dd); break;
        case "Occupancy":        ccOccupancy.updateCanvas(dd); break;
        case "Pedestal":          ccPedestal.updateCanvas(dd); break;
        case "SPE":                    ccSpe.updateCanvas(dd); break; 
        case "HV":      if(app.doEpics) ccHv.updateCanvas(dd); break;
        case "Scalers": if(app.doEpics) ccScalers.updateCanvas(dd);
        }                       
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
        String hipoFileName = app.hipoPath+"/"+mondet+"_"+app.calibRun+".hipo";
        System.out.println("Loading Histograms from "+hipoFileName);
        ccPix.initHistograms(hipoFileName);
        ccOccupancy.analyze();
        inProcess = 2;          
    }
    
    @Override
    public void saveToFile() {
        System.out.println("monitor.saveToFile()");
        String hipoFileName = app.hipoPath+"/"+mondet+"_"+app.calibRun+".hipo";
        System.out.println("Saving Histograms to "+hipoFileName);
        HipoFile histofile = new HipoFile(hipoFileName);
        histofile.addToMap("H2_CCa_Hist", this.H2_CCa_Hist);
        histofile.addToMap("H2_CCt_Hist", this.H2_CCt_Hist);
        histofile.writeHipoFile(hipoFileName);
    }
    
}
