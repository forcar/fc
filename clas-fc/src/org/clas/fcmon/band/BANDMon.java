package org.clas.fcmon.band;

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

public class BANDMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("BANDMon",1800,950);	
    
    BANDPixels              bandPix[] = new BANDPixels[5];
    ConstantsManager             ccdb = new ConstantsManager();
    FTHashCollection              rtt = null;      
    BANDDet                   bandDet = null;  
    
    BANDReconstructionApp   bandRecon = null;
    BANDMode1App            bandMode1 = null;
    BANDAdcApp                bandAdc = null;
    BANDTdcApp                bandTdc = null;
    BANDPedestalApp      bandPedestal = null;
    BANDMipApp                bandMip = null;    
    BANDCalibrationApp      bandCalib = null;
    BANDScalersApp       bandScalers = null;
    BANDHvApp                  bandHv = null;
       
    public int                 calRun = 12;
    int                         detID = 0;
    int                           is1 = 1;    //All sectors: is1=1 is2=7  Single sector: is1=s is2=s+1
    int                           is2 = 6; 
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
    boolean                firstevent = true;
    
    String mondet                     = "BAND";
    static String             appname = "BANDMON";
	
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public BANDMon(String det) {
        super(appname, "1.0", "lcsmith");
        mondet = det;
        bandPix[0] = new BANDPixels("LAYER1");
        bandPix[1] = new BANDPixels("LAYER2");
        bandPix[2] = new BANDPixels("LAYER3");
        bandPix[3] = new BANDPixels("LAYER4");
        bandPix[4] = new BANDPixels("LAYER5");
    }

    public static void main(String[] args){		
        String det = "BAND";
        BANDMon monitor = new BANDMon(det);	
        if (args.length != 0) {
            monitor.is1=Integer.parseInt(args[0]); 
            monitor.is2=Integer.parseInt(args[1]);    
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
        app.setTDCOffset(1200);
        monitor.bandDet.initButtons();
    }
    
    public void initConstants() {
        BANDConstants.setSectorRange(is1,is2);
    }   
    
    public void initCCDB(int runno) {
        System.out.println(appname+".initCCDB()"); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/band",
                "/daq/tt/band"
                 }));
        app.getReverseTT(ccdb,runno,"/daq/tt/band"); 
        app.mode7Emulation.init(ccdb,runno,"/daq/fadc/band", 66,3,1);        
    } 
    
    public void initDetector() {
        System.out.println(appname=".initDetector()"); 
        bandDet = new BANDDet("BANDDet",bandPix);
        bandDet.setMonitoringClass(this);
        bandDet.setApplicationClass(app);
        bandDet.init();
    }
	
    public void makeApps() {
        System.out.println(appname+".makeApps()"); 
        bandRecon = new BANDReconstructionApp("BANDREC",bandPix);        
        bandRecon.setMonitoringClass(this);
        bandRecon.setApplicationClass(app);	
        
        bandMode1 = new BANDMode1App("Mode1",bandPix);        
        bandMode1.setMonitoringClass(this);
        bandMode1.setApplicationClass(app);   
        
        bandAdc = new BANDAdcApp("ADC",bandPix);        
        bandAdc.setMonitoringClass(this);
        bandAdc.setApplicationClass(app);     
        
        bandTdc = new BANDTdcApp("TDC",bandPix);        
        bandTdc.setMonitoringClass(this);
        bandTdc.setApplicationClass(app);           
        
        bandPedestal = new BANDPedestalApp("Pedestal",bandPix);        
        bandPedestal.setMonitoringClass(this);
        bandPedestal.setApplicationClass(app);       
        
        bandMip = new BANDMipApp("MIP",bandPix);        
        bandMip.setMonitoringClass(this);
        bandMip.setApplicationClass(app);  
        
        bandCalib = new BANDCalibrationApp("Calibration", bandPix);
        bandCalib.setMonitoringClass(this);
        bandCalib.setApplicationClass(app);  
        bandCalib.init(is1,is2);
        
        bandHv = new BANDHvApp("HV",mondet);
        bandHv.setMonitoringClass(this);
        bandHv.setApplicationClass(app);  
        bandHv.init();
        
        bandScalers = new BANDScalersApp("Scalers",mondet);
        bandScalers.setMonitoringClass(this);
        bandScalers.setApplicationClass(app); 
        bandScalers.init();
        
        if(app.xMsgHost=="localhost") app.startEpics();
    }
	
    public void addCanvas() {
        System.out.println(appname+".addCanvas()"); 
        app.addFrame(bandMode1.getName(),          bandMode1.getPanel());
        app.addCanvas(bandAdc.getName(),             bandAdc.getCanvas());          
        app.addCanvas(bandTdc.getName(),             bandTdc.getCanvas());          
        app.addCanvas(bandPedestal.getName(),   bandPedestal.getCanvas());
        app.addCanvas(bandMip.getName(),             bandMip.getCanvas()); 
        app.addFrame(bandCalib.getName(),          bandCalib.getCalibPane());
        app.addFrame(bandHv.getName(),                bandHv.getPanel());
        app.addFrame(bandScalers.getName(),      bandScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println(appname+".init()");   
        app.setInProcess(0);
        initApps();
        for (int i=0; i<bandPix.length; i++) bandPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println(appname+".initApps()");
        firstevent = true;
        for (int i=0; i<bandPix.length; i++)   bandPix[i].init();
        bandRecon.init();
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
        putGlob("is1",BANDConstants.IS1);
        putGlob("is2",BANDConstants.IS2);
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
        bandRecon.clearHistograms();
    }
	
    @Override
    public void dataEventAction(DataEvent de) {
        if (firstevent&&app.getEventNumber()>2) {
    	        System.out.println(appname+".dataEventAction: First Event");
   	        initCCDB(app.run);
   	        firstevent=false;
         }
        bandRecon.addEvent(de);	
    }

    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: 
            for (int idet=0; idet<bandPix.length; idet++) bandRecon.makeMaps(idet); 
            break;
        case 2:
            for (int idet=0; idet<bandPix.length; idet++) bandRecon.makeMaps(idet); 
            System.out.println("End of run");                 
//            bandCalib.engines[0].analyze();
            app.setInProcess(3);
        }
    }
    
    @Override
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        bandDet.update(shape);
        if (app.getSelectedTabName().equals("Scalers")) bandScalers.updateDetectorView(shape);
        if (app.getSelectedTabName().equals("HV"))           bandHv.updateDetectorView(shape);
//        ftofCalib.updateDetectorView(shape);
    }
        
    @Override
    public void processShape(DetectorShape2D shape) { 
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze();  // Refresh color maps      
        switch (app.getSelectedTabName()) {
        case "Mode1":                        bandMode1.updateCanvas(dd); break;
        case "ADC":                            bandAdc.updateCanvas(dd); break;
        case "TDC":                            bandTdc.updateCanvas(dd); break;
        case "Pedestal":                  bandPedestal.updateCanvas(dd); break;
        case "MIP":                            bandMip.updateCanvas(dd); break; 
//        case "HV":                              bandHv.updateCanvas(dd); break;
//        case "Scalers":                    bandScalers.updateCanvas(dd);
       } 
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        bandHv.loadHV(is1,is2,il1,il2);
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
          bandPix[idet].initHistograms(hipoFileName);
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
          histofile.addToMap("H2_a_Hist",bandPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",bandPix[idet].strips.hmap2.get("H2_t_Hist"));
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
        if (app.xMsgHost=="localhost") {bandHv.online=false ; bandScalers.online=false;}
        if ( doEpics) {bandHv.startEPICS(); bandScalers.startEPICS();}
        if (!doEpics) {bandHv.stopEPICS();  bandScalers.stopEPICS();}
    }
    
}
