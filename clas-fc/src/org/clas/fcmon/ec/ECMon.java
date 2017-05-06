package org.clas.fcmon.ec;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.detector.decode.CLASDecoder;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioDataSync;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.service.ec.*;
import org.jlab.utils.groups.IndexedTable;

import java.util.Arrays;
import java.util.TreeMap;

public class ECMon extends DetectorMonitor {
	
    static MonitorApp           app = new MonitorApp("ECMon",1800,950);	
    
    ECPixels                ecPix[] = new ECPixels[3];
    ConstantsManager           ccdb = new ConstantsManager();
    FTHashCollection            rtt = null;
    ECDet                     ecDet = null;
    
    ECReconstructionApp     ecRecon = null;
    ECMode1App              ecMode1 = null;
    ECEngineApp               ecEng = null;
    ECAdcApp                  ecAdc = null;
    ECTdcApp                  ecTdc = null;
    ECCalibrationApp        ecCalib = null;
    ECPedestalApp        ecPedestal = null;
    ECPixelsApp            ecPixels = null;
    ECGainsApp              ecGains = null;
    ECScalersApp          ecScalers = null;
    ECHvApp                    ecHv = null;   
    
    ECEngine               ecEngine = null;
    EvioDataSync             writer = null;
    Boolean                saveFile = false;
   
    public static int        calRun = 760;
    int                       detID = 0;
    int                         is1 = 1;
    int                         is2 = 7;  
    int    nsa,nsb,tet,p1,p2,pedref = 0;
    double               PCMon_zmin = 0;
    double               PCMon_zmax = 0;
   
    String                   mondet = "EC";
    static String           appname = "ECMON";
    String                 detnam[] = {"PCAL","ECin","ECout"};
        
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
   
    public ECMon(String det) {
        super(appname,"1.0","lcsmith");
        mondet = det;
        ECDetector ecdet  = new ECFactory().createDetectorTilted(DataBaseLoader.getGeometryConstants(DetectorType.EC, 10, "default"));
        ecPix[0] = new ECPixels("PCAL",ecdet);
        ecPix[1] = new ECPixels("ECin",ecdet);
        ecPix[2] = new ECPixels("ECout",ecdet);
    }
	
    public static void main(String[] args){
        String det = "PCAL";
        ECMon monitor = new ECMon(det);
        if (args.length != 0) {
           monitor.is1=Integer.parseInt(args[0]); 
           monitor.is2=Integer.parseInt(args[1]);    
        }
        app.setPluginClass(monitor);
        app.setAppName(appname);
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
        app.setSelectedTab(2); 
        monitor.ecDet.initButtons();
    }
    
    public void initConstants() {
        ECConstants.setSectorRange(is1,is2);
    }
    
    public void initCCDB() {
        System.out.println("monitor.initCCDB()"); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ec",
                "/daq/tt/ec",
                "/calibration/ec/attenuation",
                "/calibration/ec/gain",
                "/calibration/ec/status"}));
        app.getReverseTT(ccdb,"/daq/tt/ec");
        app.mode7Emulation.init(ccdb,calRun,"/daq/fadc/ec", 3,3,1);        
    }	
    
    public void initDetector() {
        System.out.println("monitor.initDetector()"); 
        ecDet = new ECDet("ECDet",ecPix);
        ecDet.setMonitoringClass(this);
        ecDet.setApplicationClass(app);
        ecDet.init();
    }
    
    public void makeApps() {
        System.out.println("monitor.makeApps()");   
        
        ecEngine   = new ECEngine();
        
        ecRecon = new ECReconstructionApp("ECREC",ecPix);        
        ecRecon.setMonitoringClass(this);
        ecRecon.setApplicationClass(app);
        
        ecMode1 = new ECMode1App("Mode1",ecPix);
        ecMode1.setMonitoringClass(this);
        ecMode1.setApplicationClass(app);
        
        ecAdc = new ECAdcApp("ADC",ecPix);        
        ecAdc.setMonitoringClass(this);
        ecAdc.setApplicationClass(app);     
               
        ecTdc = new ECTdcApp("TDC",ecPix);        
        ecTdc.setMonitoringClass(this);
        ecTdc.setApplicationClass(app); 
        
        ecPixels = new ECPixelsApp("Pixels",ecPix);       
        ecPixels.setMonitoringClass(this);
        ecPixels.setApplicationClass(app); 
        
        ecPedestal = new ECPedestalApp("Pedestal",ecPix);       
        ecPedestal.setMonitoringClass(this);
        ecPedestal.setApplicationClass(app);  

        ecCalib = new ECCalibrationApp("Calibration", ecPix);
        ecCalib.setMonitoringClass(this);
        ecCalib.setApplicationClass(app);
        ecCalib.setConstantsManager(ccdb,calRun);
        ecCalib.init(); 
        
        ecEng = new ECEngineApp("ECEngine",ecPix);
        ecEng.setMonitoringClass(this);
        ecEng.setApplicationClass(app);
        
        ecGains = new ECGainsApp("Gains");
        ecGains.setMonitoringClass(this);
        ecGains.setApplicationClass(app);
        ecGains.init(); 
                
        ecHv = new ECHvApp("HV","EC");
        ecHv.setMonitoringClass(this);
        ecHv.setApplicationClass(app);  
        
        ecScalers = new ECScalersApp("Scalers","EC");
        ecScalers.setMonitoringClass(this);
        ecScalers.setApplicationClass(app);     
    }
    
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addFrame(ecMode1.getName(),             ecMode1.getPanel());
        app.addFrame(ecEng.getName(),                 ecEng.getPanel());
        app.addCanvas(ecAdc.getName(),                ecAdc.getCanvas());          
        app.addCanvas(ecTdc.getName(),                ecTdc.getCanvas());          
        app.addCanvas(ecPedestal.getName(),      ecPedestal.getCanvas());         
        app.addCanvas(ecPixels.getName(),          ecPixels.getCanvas());         
        app.addFrame(ecCalib.getName(),             ecCalib.getPanel());
        app.addFrame(ecGains.getName(),             ecGains.getPanel());
        app.addFrame(ecHv.getName(),                   ecHv.getPanel());
        app.addFrame(ecScalers.getName(),         ecScalers.getPanel());        
    }
	
    public void init( ) {	    
        System.out.println("monitor.init()");	
        app.setInProcess(0);  
        initApps();
        for (int i=0; i<ecPix.length; i++) ecPix[i].initHistograms(" ");
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        for (int i=0; i<ecPix.length; i++)   ecPix[i].init();
        initEngine();
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_a.add(0,0,0, ecRecon.toTreeMap(ecPix[i].ec_cmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_t.add(0,0,0, ecRecon.toTreeMap(ecPix[i].ec_cmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_a.add(0,0,1, ecRecon.toTreeMap(ecPix[i].ec_zmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].getLmapMinMax(0,1,0,0); 

    }
    
    public void initEngine() {
        System.out.println("monitor.initEngine():Initializing ecEngine");
        System.out.println("Configuration: "+app.config);   
        
        if(saveFile) {
            writer = new EvioDataSync();
            writer.open("/Users/colesmith/ECMON/EVIO/test.evio");
        }

        ecRecon.init(); 
        ecEngine.init();
        ecEngine.setVariation("clas6");
       
        ecEngine.setStripThresholds(ecPix[0].getStripThr(app.config, 1),
                                    ecPix[1].getStripThr(app.config, 1),
                                    ecPix[2].getStripThr(app.config, 1));  
        ecEngine.setPeakThresholds(ecPix[0].getPeakThr(app.config, 1),
                                   ecPix[1].getPeakThr(app.config, 1),
                                   ecPix[2].getPeakThr(app.config, 1));  
        ecEngine.setClusterCuts(ecPix[0].getClusterErr(app.config),
                                ecPix[1].getClusterErr(app.config),
                                ecPix[2].getClusterErr(app.config));
        putGlob("ecEng",ecEngine.getHist());
        
    }
    
    public void initEPICS() {
        System.out.println("monitor.initScalers():Initializing EPICS Channel Access");
        ecHv.init(app.doEpics);        
        ecScalers.init(app.doEpics);         
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
        putGlob("is1",ECConstants.IS1);
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
		ecRecon.clearHistograms();
    } 

    @Override
    public void dataEventAction(DataEvent de) { 

      ecRecon.addEvent(de);
      
      if(app.doEng) {
          ecEngine.singleEvent = app.isSingleEvent() ; 
          ecEngine.debug       = app.debug; 
          ecEngine.isMC        = app.isMC;
          ecEngine.processDataEvent(de);     
          ecEng.addEvent(de);
          ecGains.addEvent(de);
          if(de instanceof EvioDataEvent&&saveFile) writer.writeEvent(de);
      }
    }

	@Override
	public void analyze() {		
	
		switch (app.getInProcess()) {
			case 1: 
			    for (int idet=0; idet<ecPix.length; idet++) ecRecon.makeMaps(idet); 
			    break;
			case 2: 
                // Final analysis of full detector at end of run
			    for (int idet=0; idet<ecPix.length; idet++) ecRecon.makeMaps(idet);
		        System.out.println("End of run");
				ecCalib.analyzeAllEngines(is1,is2,1,4);	
				if (app.doEng) ecGains.analyze();
		        app.setInProcess(3); 
		}
	}
    
    @Override    
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        ecDet.update(shape); 
//      ecCalib.updateDetectorView(shape); //For status maps
    }	
    
    @Override
    public void processShape(DetectorShape2D shape) {	
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze(); // Refresh color maps 
        switch (app.getSelectedTabName()) {
        case "Mode1":                       ecMode1.updateCanvas(dd); break;
        case "ADC":                           ecAdc.updateCanvas(dd); break;
        case "TDC":                           ecTdc.updateCanvas(dd); break;
        case "Pedestal":                 ecPedestal.updateCanvas(dd); break;
        case "Pixels":                     ecPixels.updateCanvas(dd); break;
        case "Calibration":                 ecCalib.updateCanvas(dd); break;
        case "Gains":                       ecGains.updateCanvas(dd); break;
        case "ECEngine":                      ecEng.updateCanvas(dd); break;
        case "HV":                             ecHv.updateCanvas(dd); break;
        case "Scalers":                   ecScalers.updateCanvas(dd);
        }				
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ecHv.loadHV(is1,is2,il1,il2);
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
        for (int idet=0; idet<3; idet++) {
            String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
            System.out.println("Reading Histograms from "+hipoFileName);
            ecPix[idet].initHistograms(hipoFileName);
          }
          app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println("monitor.writeHipoFile()");
        for (int idet=0; idet<3; idet++) {
            String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
            System.out.println("Writing Histograms to "+hipoFileName);
            HipoFile histofile = new HipoFile(hipoFileName);
            histofile.addToMap("H2_a_Hist",    ecPix[idet].strips.hmap2.get("H2_a_Hist")); 
            histofile.addToMap("H1_a_Hist",    ecPix[idet].strips.hmap1.get("H1_a_Hist")); 
            histofile.addToMap("H2_PC_Stat",   ecPix[idet].strips.hmap2.get("H2_PC_Stat"));
            histofile.addToMap("H2_Peds_Hist", ecPix[idet].strips.hmap2.get("H2_Peds_Hist"));
            histofile.addToMap("H2_Mode1_Hist",ecPix[idet].strips.hmap2.get("H2_Mode1_Hist"));
            histofile.addToMap("H2_t_Hist",    ecPix[idet].strips.hmap2.get("H2_t_Hist"));
            histofile.addToMap("H1_a_Maps",    ecPix[idet].pixels.hmap1.get("H1_a_Maps"));
            histofile.addToMap("H1_t_Maps",    ecPix[idet].pixels.hmap1.get("H1_t_Maps"));
            if (idet==0) {
            histofile.addToMap("H1_SCA", ecScalers.H1_SCA);
            histofile.addToMap("H2_SCA", ecScalers.H2_SCA);
            }
            histofile.writeHipoFile(hipoFileName);
        }
    }
    
    @Override
    public void close() {
        System.out.println("monitor.close()");
        app.displayControl.setFPS(1);
        if(saveFile) writer.close();
    }
    
    @Override
    public void pause() {
        app.displayControl.setFPS(1);
        
    }

    @Override
    public void go() {
        app.displayControl.setFPS(10);
        
    }	
}
