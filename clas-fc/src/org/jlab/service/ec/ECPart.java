package org.jlab.service.ec;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.jlab.clas.detector.CalorimeterResponse;
import org.jlab.clas.detector.DetectorParticle;
import org.jlab.clas.detector.DetectorResponse;
import org.jlab.clas.physics.GenericKinematicFitter;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.clas.physics.Vector3;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Line3D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioSource;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.service.eb.EBConstants;
import org.jlab.service.eb.EventBuilder;
import org.jlab.utils.groups.IndexedList;


public class ECPart {
	
    EventBuilder                                  eb = new EventBuilder();
    List<CalorimeterResponse>                    rEC = new ArrayList<CalorimeterResponse>();
    List<CalorimeterResponse>                   rEC2 = new ArrayList<CalorimeterResponse>();
    List<List<CalorimeterResponse>> neutralResponses = new ArrayList<List<CalorimeterResponse>>(); 
    IndexedList<List<CalorimeterResponse>>  singleNeutrals = new IndexedList<List<CalorimeterResponse>>(1);
    List<DetectorParticle>                 particles = new ArrayList<DetectorParticle>();    
    CalorimeterResponse                         rPC1 = new CalorimeterResponse();
    CalorimeterResponse                         rPC2 = new CalorimeterResponse();
    DetectorParticle                              p1 = new DetectorParticle();
    DetectorParticle                              p2 = new DetectorParticle();
    Vector3                                       n1 = new Vector3();
    Vector3                                       n2 = new Vector3();
    
    public static double distance11,distance12,distance21,distance22;
    public static double e1,e2,e1c,e2c,cth,cth1,cth2,X,tpi2,cpi0,refE,refP,refTH;
    public static double x1,y1,x2,y2;
    public static int ip1,ip2,is1,is2;
    public static double mpi0 = 0.1349764;
    public static String geom = "2.4";
    public static String config = null;
    public static double SF1 = 0.27;
    public static double SF2 = 0.27;
  
    public int n2hit=0;
    public int n2rec=0;
    public int[] mip = {0,0,0,0,0,0};
    
    public static void readMC(DataEvent event) {
        int pid=0;
        double ppx=0,ppy=0,ppz=0;
        Boolean isEvio = event instanceof EvioDataEvent;        
        if(isEvio&&event.hasBank("GenPart::true")) {
            EvioDataBank bank = (EvioDataBank) event.getBank("GenPart::true");
            ppx = bank.getDouble("px",0);
            ppy = bank.getDouble("py",0);
            ppz = bank.getDouble("pz",0);
            pid = bank.getInt("pid",0);
        }
        if(!isEvio&&event.hasBank("MC::Particle")) {
            DataBank bank = event.getBank("MC::Particle");
            ppx = bank.getFloat("px",0);
            ppy = bank.getFloat("py",0);
            ppz = bank.getFloat("pz",0);
            pid = bank.getInt("pid",0);                
        }
        double  rm = 0.;
        if (pid==111) rm=mpi0;                   
        refP  = Math.sqrt(ppx*ppx+ppy*ppy+ppz*ppz);  
        refE  = Math.sqrt(refP*refP+rm*rm);            
        refTH = Math.acos(ppz/refP)*180/3.14159;        
    }
    
    public static List<CalorimeterResponse> readEvioEvent(DataEvent event, String bankName, DetectorType type) {
        List<CalorimeterResponse> responseList = new ArrayList<CalorimeterResponse>();
        if(event.hasBank(bankName)==true){
            EvioDataBank bank = (EvioDataBank) event.getBank(bankName);
            int nrows = bank.rows();
            for(int row = 0; row < nrows; row++){
                int sector = bank.getInt("sector", row);
                int  layer = bank.getInt("layer",  row);
                CalorimeterResponse  response = new CalorimeterResponse(sector,layer,0);
                response.getDescriptor().setType(type);
                double x = bank.getDouble("X", row);
                double y = bank.getDouble("Y", row);
                double z = bank.getDouble("Z", row);
                response.setPosition(x, y, z);
                response.setEnergy(bank.getDouble("energy", row));
                response.setTime(bank.getDouble("time", row));
                responseList.add(response);
            }
        }
        return responseList;                      
    }
    
    public List<CalorimeterResponse>  readEC(DataEvent event){
        rEC.clear();
        Boolean isEvio = event instanceof EvioDataEvent;                  
        if (isEvio) rEC =                     readEvioEvent(event, "ECDetector::clusters", DetectorType.EC); 
        if(!isEvio) rEC = CalorimeterResponse.readHipoEvent(event, "ECAL::clusters", DetectorType.EC);
        eb.addCalorimeterResponses(rEC);
        return rEC;
    } 
    
    public void getNeutralResponses(List<CalorimeterResponse> response) {        
        neutralResponses.clear();
        neutralResponses.add(eb.getUnmatchedResponses(response, DetectorType.EC,1));
        neutralResponses.add(eb.getUnmatchedResponses(response, DetectorType.EC,4));
        neutralResponses.add(eb.getUnmatchedResponses(response, DetectorType.EC,7));
        getSingleNeutralResponses();
    }
    
    public void getSingleNeutralResponses() {
        singleNeutrals.clear();
        for (int is=1; is<7; is++) {
            rEC = CalorimeterResponse.getListBySector(neutralResponses.get(0),  DetectorType.EC, is);
            if(rEC.size()==1&&mip[is-1]!=1) singleNeutrals.add(rEC,is);
        } 
    }
    
    public double getTwoPhotonInvMass(int sector){
        is1=0;is2=-1;
        return processTwoPhotons(doHitMatching(getNeutralParticles(sector)));
    }   
     
    public List<DetectorParticle> getNeutralParticles(int sector) {
        
        particles.clear();
        
        rEC = CalorimeterResponse.getListBySector(neutralResponses.get(0), DetectorType.EC, sector);
        
        switch (rEC.size()) {
        case 1: rEC2 = findSecondPhoton(sector);
                if (rEC2.size()>0) {
                   particles.add(DetectorParticle.createNeutral(rEC.get(0)));
                   particles.add(DetectorParticle.createNeutral(rEC2.get(0))); return particles;
                }
                break;
        case 2: particles.add(DetectorParticle.createNeutral(rEC.get(0)));                
                particles.add(DetectorParticle.createNeutral(rEC.get(1))); return particles;
        }
       return particles;
    }
    
    public List<CalorimeterResponse> findSecondPhoton(int sector) {
        int neut=0, isave=0;
        rEC2.clear();
        for (int is=sector+1; is<7; is++) {
            if(singleNeutrals.hasItem(is)) {neut++; isave=is;}
        }
        return (neut==1) ? singleNeutrals.getItem(isave):rEC2;
    }
    
    public double doHitMatch(DetectorParticle p, String io) {
        
        int index=0;
        double distance = -10;
        
        int is = p.getCalorimeterResponse().get(0).getDescriptor().getSector();
        
        switch (io) {       
        case "Inner": rEC = CalorimeterResponse.getListBySector(neutralResponses.get(1), DetectorType.EC, is);
                      index  = p.getCalorimeterHit(rEC,DetectorType.EC,4,EBConstants.ECIN_MATCHING);
                      if(index>=0){p.addResponse(rEC.get(index),true); rEC.get(index).setAssociation(0);
                      distance = p.getDistance(rEC.get(index)).length();}
        case "Outer": rEC = CalorimeterResponse.getListBySector(neutralResponses.get(2), DetectorType.EC, is); 
                      index  = p.getCalorimeterHit(rEC,DetectorType.EC,7,EBConstants.ECOUT_MATCHING);
                      if(index>=0){p.addResponse(rEC.get(index),true); rEC.get(index).setAssociation(0);
                      distance = p.getDistance(rEC.get(index)).length();}
        }
        
        return distance;        
    }
        
    public List<DetectorParticle> doHitMatching(List<DetectorParticle>  particles) {
                       
        if (particles.size()==0) return particles;
        
        p1 = particles.get(0);  //PCAL Photon 1
        p2 = particles.get(1);  //PCAL Photon 2       
        
        rPC1 = p1.getCalorimeterResponse().get(0) ;
        rPC2 = p2.getCalorimeterResponse().get(0) ;
        
        ip1 = rPC1.getHitIndex();
        ip2 = rPC2.getHitIndex();
        
        is1 = rPC1.getDescriptor().getSector();
        is2 = rPC2.getDescriptor().getSector();

        x1 = rPC1.getPosition().x();
        y1 = rPC1.getPosition().y();
        x2 = rPC2.getPosition().x();
        y2 = rPC2.getPosition().y();
        
        distance11 = doHitMatch(p1,"Inner");
        distance12 = doHitMatch(p1,"Outer");
        distance21 = doHitMatch(p2,"Inner");
        distance22 = doHitMatch(p2,"Outer");
                
        return particles;
    }
       
    public double processTwoPhotons(List<DetectorParticle> particles) {
        
        if (particles.size()==0) return 0.0;
        
        p1 = particles.get(0);  //Photon 1
        p2 = particles.get(1);  //Photon 2
        
        n1 = p1.vector(); n1.unit();
        n2 = p2.vector(); n2.unit();
                
        e1 = p1.getEnergy(DetectorType.EC);
        e2 = p2.getEnergy(DetectorType.EC);

        SF1 = getSF(geom,e1); e1c = e1/SF1;
        Particle g1 = new Particle(22,
                n1.x()*e1c,
                n1.y()*e1c,
                n1.z()*e1c
        );
        
        SF2 = getSF(geom,e2); e2c = e2/SF2;
        Particle g2 = new Particle(22,
                n2.x()*e2c,
                n2.y()*e2c,
                n2.z()*e2c
        );
        
        X    =  1e3;
        tpi2 =  1e9;
        cpi0 =  -1;
        cth  =  -1;
        cth1 = Math.cos(g1.theta());
        cth2 = Math.cos(g2.theta());
         cth = g1.cosTheta(g2);
         
        // Require 2 photons in PCAL and ECinner
        
        n2hit++;
        if((myCalorimeterResponse(p1,DetectorType.EC, 1)!=null  &&
            myCalorimeterResponse(p1,DetectorType.EC, 4)!=null) &&
           (myCalorimeterResponse(p2,DetectorType.EC, 1)!=null && 
            myCalorimeterResponse(p2,DetectorType.EC, 4)!=null)) {                
              X = (e1c-e2c)/(e1c+e2c);
           tpi2 = 2*mpi0*mpi0/(1-cth)/(1-X*X);
           cpi0 = (e1c*cth1+e2c*cth2)/Math.sqrt(e1c*e1c+e2c*e2c+2*e1c*e2c*cth);
           g1.combine(g2, +1);
           n2rec++;
           return g1.mass2();
        }
        
        return  0.0;

        //System.out.println("  ENERGIES = " + (e1/0.27) + "  " + (e2/0.27));
        //System.out.println(particles.get(0));
        //System.out.println(particles.get(1));
        //System.out.println(gen);
    }

    public CalorimeterResponse  myCalorimeterResponse(DetectorParticle p, DetectorType type, int layer){
        List<CalorimeterResponse> calorimeterStore = p.getCalorimeterResponses();
        for(CalorimeterResponse res : calorimeterStore){
            if(res.getDescriptor().getType()==type&&res.getDescriptor().getLayer()==layer) return res;    
        }
        return null;
    }   
    
    public void setGeom(String geom) {
        this.geom = geom;
    }
    
    public void setConfig(String config) {
        this.config = config;
    }
    
    public static double getSF(String geom, double e) {
        switch (geom) {
        case "2.4": return 0.268*(1.0510 - 0.0104/e - 0.00008/e/e); 
        case "2.5": return 0.250*(1.0286 - 0.0150/e + 0.00012/e/e);
        }
        return Double.parseDouble(geom);
    }    
    
    public static void main(String[] args){
        
        ECEngine       engine = new ECEngine();
        HipoDataSource reader = new HipoDataSource();
        ECPart           part = new ECPart();
        
        String evioPath = "/Users/colesmith/clas12/gemc/pizero/hipo/";
        // GEMC file: 10k 2.0 GeV pizeros thrown at 25 deg into Sector 2 using GEMC 2.4 geometry
        // JLAB: evioPath = "/lustre/expphy/work/hallb/clas12/lcsmith/clas12/forcar/gemc/evio/";
        
        if (args.length == 0) { 
//            reader.open(evioPath+"fc-pizero-10k-s2-25deg-oldgeom.evio");
            reader.open(evioPath+"fc-pizero-10k-s2-newgeom.hipo");
        } else {
            String inputFile = args[0];
            reader.open(inputFile);
        }
                
        engine.init();
        engine.isMC = true;
        engine.setVariation("clas6");
        engine.setCalRun(2);
        engine.setStripThresholds(10,9,8);
//        engine.setStripThresholds(5,5,5);
        engine.setPeakThresholds(18,20,15);
//        engine.setPeakThresholds(5,5,5);
        engine.setClusterCuts(7,15,20);
        part.setGeom("2.5");
        
        H1F h1 = new H1F("Invariant Mass",50,10.,200);         
        h1.setOptStat("11010000111"); h1.setTitleX("Pizero Invariant Mass (MeV)");
        H1F h2 = new H1F("Energy Asymmetry",50,-1.0,1.0);      
        h2.setOptStat(Integer.parseInt("1100")); h2.setTitleX("X:(E1-E2)/(E1+E2)");
        H1F h3 = new H1F("Pizero Energy Error",50,-500.,500.); 
        h3.setOptStat(Integer.parseInt("1100")); h3.setTitleX("Pizero Energy Error (MeV)");
        H1F h4 = new H1F("Pizero Theta Error",50,-1.,1.);      
        h4.setOptStat(Integer.parseInt("1100")); h4.setTitleX("Pizero Theta Error (deg)");
        
        int nimcut = 0;
        
        while(reader.hasEvent()){
            DataEvent event = reader.getNextEvent();
            part.readMC(event);
            engine.processDataEvent(event);   
            part.getNeutralResponses(part.readEC(event));
            double invmass = 1e3*Math.sqrt(part.getTwoPhotonInvMass(2));
            
            h1.fill((float)invmass,1.);                            //Two-photon invariant mass
            
            if (invmass>80 && invmass<200) {
                h2.fill((float)part.X);                            //Pizero energy asymmetry
                h3.fill((float)(1e3*(Math.sqrt(part.tpi2)-refE))); //Pizero total energy error
                h4.fill(Math.acos(part.cpi0)*180/3.14159-refTH);   //Pizero theta angle error
                nimcut++;
            }
        }
        
        System.out.println("n2hit,n2rec,nimcut="+part.n2hit+" "+part.n2rec+" "+nimcut);
        System.out.println("Eff1= "+(float)part.n2rec/(float)part.n2hit+" Eff2= "+(float)nimcut/(float)part.n2hit);
        
        JFrame frame = new JFrame("Pizero Reconstruction");
        frame.setSize(800,800);
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        canvas.divide(2,2);
        canvas.cd(0); canvas.draw(h1);
        canvas.cd(1); canvas.draw(h2);
        canvas.cd(2); canvas.draw(h3);
        canvas.cd(3); canvas.draw(h4);
        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);     
    }
}
