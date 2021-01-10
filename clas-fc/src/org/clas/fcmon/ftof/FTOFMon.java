package org.clas.fcmon.ftof;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.io.base.DataEvent;

public class FTOFMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("FTOFMon",1800,950);	
    
    FTOFPixels              ftofPix[] = new FTOFPixels[3];
    ConstantsManager             ccdb = new ConstantsManager();
    FTHashCollection              rtt = null;      
    FTOFDet                   ftofDet = null;  
    
    FTOFReconstructionApp   ftofRecon = null;
    FTOFMode1App            ftofMode1 = null;
    FTOFAdcApp                ftofAdc = null;
    FTOFTdcApp                ftofTdc = null;
    FTOFPedestalApp      ftofPedestal = null;
    FTOFMipApp                ftofMip = null;    
    FTOFCalibrationApp      ftofCalib = null;
    FTOFScalersApp        ftofScalers = null;
    FTOFHvApp                  ftofHv = null;
       
    public int                 calRun = 12;
    int                         detID = 0;
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
    boolean                firstevent = true;
    
    static int                    is1 = 1;    //All sectors: is1=1 is2=7  Single sector: is1=s is2=s+1
    static int                    is2 = 7;       
    static int                   fmax = 100; //fadc samples (4ns/sample)
    static int                   tmax = 300; //tmax (ns)    
    
    String mondet                     = "FTOF";
    static String             appname = "FTOFMON";
	
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public FTOFMon(String det) {
        super(appname, "1.0", "lcsmith");
        mondet = det;
        ftofPix[0] = new FTOFPixels("PANEL1A");
        ftofPix[1] = new FTOFPixels("PANEL1B");
        ftofPix[2] = new FTOFPixels("PANEL2");
    }

    public static void main(String[] args){		
        String det = "FTOF";
        FTOFMon monitor = new FTOFMon(det);	
        if (args.length != 0) {
            is1=Integer.parseInt(args[0]); 
            is2=Integer.parseInt(args[1]);    
        	fmax = Integer.parseInt(args[2]);
        	tmax = Integer.parseInt(args[3]); 	
        }
        app.setPluginClass(monitor);
        app.setAppName(appname);
        app.makeGUI();
        app.getEnv();
        monitor.initConstants();
        monitor.initCCDB(10);
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.init();
        monitor.initDetector();
        app.init();
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(2); 
        app.setTDCOffset(435);
        monitor.ftofDet.initButtons();
    }
    
    public void initConstants() {
        FTOFConstants.setSectorRange(is1,is2);
    }   
    
    public void initCCDB(int runno) {
        System.out.println(appname+".initCCDB("+runno+")"); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ftof",
                "/daq/tt/ftof",
                "/calibration/ftof/attenuation",
                "/calibration/ftof/gain_balance",
                "/calibration/ftof/status"}));
        app.getReverseTT(ccdb,runno,"/daq/tt/ftof"); 
        app.mode7Emulation.init(ccdb,runno,"/daq/fadc/ftof", 3,3,1);        
    } 
    
    public void initDetector() {
        System.out.println(appname+".initDetector()"); 
        ftofDet = new FTOFDet("FTOFDet",ftofPix);
        ftofDet.setMonitoringClass(this);
        ftofDet.setApplicationClass(app);
        ftofDet.init();
    }
	
    public void makeApps() {
        System.out.println(appname+".makeApps()"); 
        ftofRecon = new FTOFReconstructionApp("FTOFREC",ftofPix);        
        ftofRecon.setMonitoringClass(this);
        ftofRecon.setApplicationClass(app);	
        
        ftofMode1 = new FTOFMode1App("Mode1",ftofPix);        
        ftofMode1.setMonitoringClass(this);
        ftofMode1.setApplicationClass(app);   
        
        ftofAdc = new FTOFAdcApp("ADC",ftofPix);        
        ftofAdc.setMonitoringClass(this);
        ftofAdc.setApplicationClass(app);     
        
        ftofTdc = new FTOFTdcApp("TDC",ftofPix);        
        ftofTdc.setMonitoringClass(this);
        ftofTdc.setApplicationClass(app);           
        
        ftofPedestal = new FTOFPedestalApp("Pedestal",ftofPix);        
        ftofPedestal.setMonitoringClass(this);
        ftofPedestal.setApplicationClass(app);       
        
        ftofMip = new FTOFMipApp("MIP",ftofPix);        
        ftofMip.setMonitoringClass(this);
        ftofMip.setApplicationClass(app);  
        
        ftofCalib = new FTOFCalibrationApp("Calibration", ftofPix);
        ftofCalib.setMonitoringClass(this);
        ftofCalib.setApplicationClass(app);  
        ftofCalib.init(is1,is2);
        
        ftofHv = new FTOFHvApp("HV","FTOF");
        ftofHv.setMonitoringClass(this);
        ftofHv.setApplicationClass(app);  
        ftofHv.init();
        
        ftofScalers = new FTOFScalersApp("Scalers","FTOF");
        ftofScalers.setMonitoringClass(this);
        ftofScalers.setApplicationClass(app); 
        ftofScalers.init();
        
        if(app.xMsgHost=="localhost") app.startEpics();
    }
	
    public void addCanvas() {
        System.out.println(appname+".addCanvas()"); 
        app.addFrame(ftofMode1.getName(),          ftofMode1.getPanel());
        app.addCanvas(ftofAdc.getName(),             ftofAdc.getCanvas());          
        app.addCanvas(ftofTdc.getName(),             ftofTdc.getCanvas());          
        app.addCanvas(ftofPedestal.getName(),   ftofPedestal.getCanvas());
        app.addCanvas(ftofMip.getName(),             ftofMip.getCanvas()); 
        app.addFrame(ftofCalib.getName(),          ftofCalib.getCalibPane());
        app.addFrame(ftofHv.getName(),                ftofHv.getPanel());
        app.addFrame(ftofScalers.getName(),      ftofScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println(appname+".init()");   
        app.setInProcess(0);
        initApps();
        for (int i=0; i<ftofPix.length; i++) ftofPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println(appname+".initApps()");
        firstevent = true;
        for (int i=0; i<ftofPix.length; i++)   ftofPix[i].init();
        ftofRecon.init();
    }
    
    public void initGlob() {
        System.out.println(appname+".initGlob()");
        putGlob("detID", detID);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("ccdb", ccdb);
        putGlob("zmin", PCMon_zmin);
        putGlob("zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",FTOFConstants.IS1);
        putGlob("is2",FTOFConstants.IS2);
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
        ftofRecon.clearHistograms();
    }
	
    @Override
    public void dataEventAction(DataEvent de) {
        if (firstevent&&app.getEventNumber()>2) {
    	    System.out.println(appname+".dataEventAction: First Event");
   	        initCCDB(app.getEventNumber());
   	        firstevent=false;
         }
        ftofRecon.addEvent(de);	
    }

    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: 
            for (int idet=0; idet<ftofPix.length; idet++) ftofRecon.makeMaps(idet); 
            break;
        case 2:
            for (int idet=0; idet<ftofPix.length; idet++) ftofRecon.makeMaps(idet); 
            System.out.println("End of run");                 
            ftofCalib.engines[0].analyze();
            app.setInProcess(3);
        }
    }
    
    @Override
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        ftofDet.update(shape);
        if (app.getSelectedTabName().equals("Scalers")) ftofScalers.updateDetectorView(shape);
        if (app.getSelectedTabName().equals("HV"))           ftofHv.updateDetectorView(shape);
//        ftofCalib.updateDetectorView(shape);
    }
        
    @Override
    public void processShape(DetectorShape2D shape) { 
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze();  // Refresh color maps      
        switch (app.getSelectedTabName()) {
        case "Mode1":                        ftofMode1.updateCanvas(dd); break;
        case "ADC":                            ftofAdc.updateCanvas(dd); break;
        case "TDC":                            ftofTdc.updateCanvas(dd); break;
        case "Pedestal":                  ftofPedestal.updateCanvas(dd); break;
        case "MIP":                            ftofMip.updateCanvas(dd); break; 
        case "HV":                              ftofHv.updateCanvas(dd); break;
        case "Scalers":                    ftofScalers.updateCanvas(dd);
       } 
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ftofHv.loadHV(is1,is2,il1,il2);
    }  
    
    @Override
    public void resetEventListener() {
    }

    @Override
    public void timerUpdate() {
    }
    
    @Override
    public void readHipoFile() {        
        System.out.println(appname+".readHipoFile()");
        for (int idet=0; idet<3; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Reading Histograms from "+hipoFileName);
          ftofPix[idet].initHistograms(hipoFileName);
        }
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println(appname+".writeHipoFile()");
        for (int idet=0; idet<3; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Writing Histograms to "+hipoFileName);
          HipoFile histofile = new HipoFile(hipoFileName);
          histofile.addToMap("H2_a_Hist",ftofPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",ftofPix[idet].strips.hmap2.get("H2_t_Hist"));
          histofile.writeHipoFile(hipoFileName);
        }
    }
    
    @Override
    public void close() {
        System.out.println(appname+".close()");
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

    @Override
    public void initEpics(Boolean doEpics) {
        // TODO Auto-generated method stub
        System.out.println("monitor.initEpics():Initializing EPICS Channel Access");
        if (app.xMsgHost=="localhost") {ftofHv.online=false ; ftofScalers.online=false;}
        if ( doEpics) {ftofHv.startEPICS(); ftofScalers.startEPICS();}
        if (!doEpics) {ftofHv.stopEPICS();  ftofScalers.stopEPICS();}
    }
    
}
