package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.clas.detector.CalorimeterResponse;
import org.jlab.clas.detector.DetectorParticle;
import org.jlab.clas.detector.DetectorResponse;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.Vector3;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.eb.SamplingFractions;

import org.clas.service.ec.ECPeak;

import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F; 
import org.clas.fcmon.ftof.TOFPaddle;
import org.jlab.utils.groups.IndexedList.IndexGenerator;

import org.clas.tools.EBMCEngine;


public class ECEngineApp extends FCApplication implements CalibrationConstantsListener,ActionListener {

    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    strips = new EmbeddedCanvasTabbed("Strips");
    EmbeddedCanvasTabbed     peaks = new EmbeddedCanvasTabbed("Peaks");
    EmbeddedCanvasTabbed  clusters = new EmbeddedCanvasTabbed("Clusters");
    EmbeddedCanvasTabbed        mc = new EmbeddedCanvasTabbed("GAMMA");
    EmbeddedCanvas               c = new EmbeddedCanvas();
    
    IndexedTable    pcalAlignment, ecAlignment;
//    HTCCReconstructionService engineHTCC = new HTCCReconstructionService();

    List<TOFPaddle>     paddleList = null;
    
    EBMCEngine               ebmce = new EBMCEngine();
    ConstantsManager          ccdb = new ConstantsManager();    
	List<Float>                GEN = new ArrayList<Float>();
	List<Float>                REC = new ArrayList<Float>();  
    
    List<List<DetectorResponse>>   res = new ArrayList<List<DetectorResponse>>();    
	List<DetectorParticle>          np = new ArrayList<DetectorParticle>();    
	
    DetectorType[] detNames = {DetectorType.ECAL, DetectorType.ECIN, DetectorType.ECOUT};
    int ccdbrun=10;
    double pcx,pcy,pcz;
    double refE=0,refP=0,refTH=15;
    String otab[]={"U ","V ","W "};             
    String dtab[]={"PCAL ","ECIN ","ECOU "};
    
    H1F[][] eff = new H1F[3][6];
	static int trSEC=5, trPID=-211, mcSEC=2, mcPID=22;

    public ECEngineApp(String name, ECPixels[] ecPix) {
    	super(name,ecPix);
        initCanvas();
        createPopupMenu();
        is1 = ECConstants.IS1;
        is2 = ECConstants.IS2;
        mcSEC = is1;
        ebmce.getCCDB(10);
        ebmce.setMCpid(mcPID); 
        ebmce.setGeom("2.5");
        initHist();
    }
    
    public void setConstantsManager(ConstantsManager ccdb, int run) {
    	this.ccdb = ccdb;
        ccdbrun = run;
        eng.setGeomVariation(app.geomVariation);
    }
   
    public void initHist() {
	    float xmin[] = {1.8f, 1, 0};
	    float xmax[] = {12,   8, 1};
	    for (int i=0; i<3; i++) {
		   for (int j=0; j<6; j++) {
			   eff[i][j] = new H1F("Efficiency"+i+j,50,xmin[i],xmax[i]);
		   }
	    }
    }    
   
    public void resetEffHist() {
	   for (int i=0; i<3; i++) {
		   for (int j=0; j<6; j++) {
			   eff[i][j].reset();
		   }
	   }	   
    }
   
    public JPanel getPanel() {        
       engineView.setLayout(new BorderLayout());
       engineView.add(getCanvasPane(),BorderLayout.CENTER);
       engineView.add(eng.getECEnginePane(),BorderLayout.PAGE_END);
       strips.addCanvas("Ns");
       peaks.addCanvas("Np");
       clusters.addCanvas("Nc"); 
       mc.addCanvas("N=1");
       mc.addCanvas("N>1"); 
       mc.addCanvas("N=2"); 
       mc.addCanvas("Eff");
       mc.addCanvas("Resid");
       mc.addCanvas("SF");
       mc.addCanvas("PI0");
       mc.addCanvas("Sectors");
       mc.addCanvas("Map1");
       mc.addCanvas("Map2");
       mc.addCanvas("Map3");
       return engineView;       
    }  
   
    public JSplitPane getCanvasPane() {
       
       JSplitPane    hPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
       JSplitPane   vPaneL = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
       JSplitPane   vPaneR = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
       hPane.setLeftComponent(vPaneL);
       hPane.setRightComponent(vPaneR);
       vPaneL.setTopComponent(strips);
       vPaneL.setBottomComponent(peaks);
       vPaneR.setTopComponent(clusters);
       vPaneR.setBottomComponent(mc);
       hPane.setResizeWeight(0.5);
       vPaneL.setResizeWeight(0.5);
       vPaneR.setResizeWeight(0.5);      
       return hPane;
    }
   
    private void createPopupMenu(){
       strips.popup = new JPopupMenu();
       JMenuItem itemCopy = new JMenuItem("Copy Canvas");
       itemCopy.addActionListener(strips.getCanvas("Strips"));
       strips.popup.add(itemCopy);
   } 
   
   public int getDet(int layer) {
       int[] il = {0,0,0,1,1,1,2,2,2}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
       return il[layer-1];
    }
   
   public int getLay(int layer) {
       int[] il = {1,2,3,1,2,3,1,2,3}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
       return il[layer-1];
    }
   
   public List<Float> getkin(List<Particle> list) {
   	List<Float> out = new ArrayList<Float>();
   	int n=0;
   	for (Particle p : list) {
		    out.add(n++,(float) p.e());
		    out.add(n++,(float) Math.toDegrees(p.theta())); 
		    out.add(n++,(float) Math.toDegrees(p.phi()));
   	}
		return out;
   }
      
   public int getZone(int layer, int u, int v, int w){
   	if (layer>0) return 0;
       if (u<53&&v>15&&w>15) return 0;
       if (u>52&&v>15&&w>15) return 1;
       if (v<16)             return 2;
       if (w<16)             return 3;
       return 0;
   }
   
   public void addEvent(DataEvent event) {              
       if(event.hasBank("ECAL::clusters")) fillHistos(event);       
   }
   
   public int getEventNumber(DataEvent event) {
       return event.hasBank("RUN::config") ? event.getBank("RUN::config").getInt("event",0):0;
   }
   
   public void fillHistos(DataEvent de) {
	   fillSPCHistos(de);
	   fillEBMCHistos(de);
   }
   
   public void fillSPCHistos(DataEvent de) {
       
      int ip = 0;  
      
      if (app.isSingleEvent()) {
          resetEffHist();
          for (int i=0; i<3; i++) app.getDetectorView().getView().removeLayer("L"+i);
          for (int i=0; i<3; i++) app.getDetectorView().getView().addLayer("L"+i);
          for (int is=is1; is<is2; is++ ) {
              for (int ilm=0; ilm<3; ilm++) {
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).reset();
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,5,0).reset();
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,6,0).reset();
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).reset();
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,10).reset(); //Residuals
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,11).reset(); //Residuals
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,20).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,21).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,22).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,23).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,30).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,31).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,32).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,33).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,40).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,41).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,42).reset(); 
                  ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,43).reset();               
                  ecPix[ilm].strips.hmap1.get("H1_Clus_Mult").get(is,0,0).reset(); //Cluster multiplicity
                  ecPix[ilm].strips.hmap2.get("H2_Clus_Mult").get(is,0,0).reset(); //Cluster multiplicity
                  for (int il=1; il<4; il++)  ecPix[ilm].strips.hmap2.get("H2_Clus_Mult").get(is,il,0).reset(); //splitRatio vs cluster mult.                                   
              }
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,4).reset(); //X vs OPA
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,3).reset(); //Sampling fraction vs energy
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2).reset(); //E1*E2 vs opening angle
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).reset(); //Cluster X,Y,X - MC
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).reset(); //Photon 1,2, errors
              ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0).reset(); //Pizero energy error
              ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1).reset(); //Pizero theta error
              ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2).reset(); //X:(E1-E2)/(E1+E2)
              ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,11,is).reset(); //IVM
          }
      }
       
      if(de.hasBank("ECAL::peaks")){
         DataBank bank = de.getBank("ECAL::peaks");
         for(int i=0; i < bank.rows(); i++) {
            int    is = bank.getByte("sector",i);
            int    il = bank.getByte("layer",i);
            float  en = bank.getFloat("energy",i);             
            if (isGoodSector(is)) {
            ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(en*1e3,getLay(il),1.);  
            if (app.isSingleEvent()) {  //Draw reconstructed peak lines on detector view
                float xo = bank.getFloat("xo",i);
                float yo = bank.getFloat("yo",i);
                float zo = bank.getFloat("zo",i);
                float xe = bank.getFloat("xe",i);
                float ye = bank.getFloat("ye",i);
                float ze = bank.getFloat("ze",i);
                Line3D xyz = new Line3D(-xo,yo,zo,-xe,ye,ze);
                xyz.rotateZ(Math.toRadians(60*(is-1)));
                xyz.rotateY(Math.toRadians(25));                
                xyz.translateXYZ(-333.1042, 0.0, 0.0);
                xyz.rotateZ(Math.toRadians(-60*(is-1)));
                Point3D orig = xyz.origin();
                Point3D  end = xyz.end();
                orig.setY(-orig.y()); end.setY(-end.y());                        
                DetectorShape2D shape = new DetectorShape2D(detNames[getDet(il)],is,ip++,0,0); 
                shape.getShapePath().addPoint(orig.x(),orig.y(),0.);
                shape.getShapePath().addPoint(end.x(),end.y(),0.);
                app.getDetectorView().getView().addShape("L"+getDet(il), shape);
                double[] dum = {orig.x(),orig.y(),end.x(),end.y()};
                ecPix[getDet(il)].peakXY.get(is).add(dum);
            }           
            }
        }        
      }
            
      if(de.hasBank("ECAL::calib")) {
          DataBank bank = de.getBank("ECAL::calib");
          for(int i=0; i < bank.rows(); i++) {
             int is = bank.getByte("sector",i);
             int il = bank.getByte("layer",i);
             if(isGoodSector(is)) {
            	 ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,5,0).fill(1e3*(float)bank.getFloat("rawEU",i),1,1.);
            	 ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,5,0).fill(1e3*(float)bank.getFloat("rawEV",i),2,1.); 
            	 ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,5,0).fill(1e3*(float)bank.getFloat("rawEW",i),3,1.); 
            	 ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,6,0).fill(1e3*(float)bank.getFloat("recEU",i),1,1.); 
            	 ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,6,0).fill(1e3*(float)bank.getFloat("recEV",i),2,1.); 
            	 ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,6,0).fill(1e3*(float)bank.getFloat("recEW",i),3,1.); 	 
             }
          }
      }
      
      if(de.hasBank("ECAL::clusters")) {
    	  int nclus[][] = new int[6][3]; 
    	  IndexedList<List<Integer>> stat = new IndexedList<List<Integer>>(2);
    	  DataBank bank = de.getBank("ECAL::clusters");
    	  for(int i=0; i<bank.rows(); i++) {
    		  int  is = bank.getByte("sector",i);
    		  int  il = getDet(bank.getByte("layer",i));
    		  float e = bank.getFloat("energy", i);
    		  float x = bank.getFloat("x",i);
    		  float y = bank.getFloat("y",i);
    		  float z = bank.getFloat("z",i);
    		  int  st = bank.getInt("status", i);
              int  iU = (bank.getInt("coordU",i)-4)/8+1; //strip number of peak U
              int  iV = (bank.getInt("coordV",i)-4)/8+1; //strip number of peak V
              int  iW = (bank.getInt("coordW",i)-4)/8+1; //strip number of peak W  
              if(!stat.hasItem(is,il)) stat.add(new ArrayList<Integer>(),is,il);
    		  stat.getItem(is,il).add(st);
    		  //Transform X,Y,Z from CLAS into tilted for detector view
    		  Point3D xyz = new Point3D(-x,y,z);
    		  xyz.rotateZ(Math.toRadians(60*(is-1)));
    		  xyz.rotateY(Math.toRadians(25));
    		  xyz.translateXYZ(-333.1042, 0.0, 0.0);
    		  xyz.rotateZ(Math.toRadians(-60*(is-1)));
    		  double[] dum  = {xyz.x(),-xyz.y()}; 
    		  nclus[is-1][il]++;    		  
    		  if (app.isSingleEvent()) ecPix[il].clusterXY.get(is).add(dum);  
    		  ecPix[il].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(e*1e3,4,1.);          // Layer Cluster Energy
    	  }

    	  for (int is=1; is<7; is++) {
    		  for (int il=0; il<3; il++) {
    			  ecPix[il].strips.hmap1.get("H1_Clus_Mult").get(is,0,0).fill(nclus[is-1][il]);
    		  }
    	  }   
    	  
  	      IndexGenerator ig = new IndexGenerator();                
  	      for (Map.Entry<Long,List<Integer>>  entry : stat.getMap().entrySet()){
	           int is = ig.getIndex(entry.getKey(), 0);  
  	           int il = ig.getIndex(entry.getKey(), 1);
  	           for (Integer p : entry.getValue()) ecPix[il].strips.hmap2.get("H2_Clus_Mult").get(is,0,0).fill(nclus[is-1][il],p);	           
  	      }
  	      
  	      List<ECPeak> peaks = eng.engine.getPeaks();
  	      
          for(int p = 0; p < peaks.size(); p++){
             int is = peaks.get(p).getDescriptor().getSector();             
             int il = peaks.get(p).getDescriptor().getLayer();
             int ilm = getDet(il), ill=getLay(il);
             double splitRatio = peaks.get(p).getSplitRatio();
             ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,2);
             ecPix[ilm].strips.hmap2.get("H2_Clus_Mult").get(is,ill,0).fill(nclus[is-1][ilm],splitRatio);
          }
      }
  	        	      
   }
      
   void fillEBMCHistos(DataEvent de) { // EBMCEngine
      
  	  List<Float>           GEN   = new ArrayList<Float>();
      List<Float>           REC   = new ArrayList<Float>();   
  	  List<Float>           GENPZ = new ArrayList<Float>();
  	  List<Float>           RECPZ = new ArrayList<Float>(); 
   	
  	  DetectorParticle p1 = new DetectorParticle();
  	  DetectorParticle p2 = new DetectorParticle();
  	  
  	  int npart=0; 	  
  	  boolean correct=false; //cluster merging correction
  	  
      float opa=0, x=0;
      
	  boolean goodev = ebmce.readMC(de) && ebmce.pmc.size()==2;
		
	  if (!goodev) return;
      	
      GEN   = getkin(ebmce.pmc);	
      GENPZ = ebmce.getPizeroKinematics(ebmce.pmc); opa = GENPZ.get(3); x = GENPZ.get(4);	    		      
	    
      if(!ebmce.processDataEvent(de)) return;
      	
      float stt = ebmce.starttime;
      	
      List<DetectorParticle> par = ebmce.eb.getEvent().getParticles();  
      List<DetectorResponse> cal = ebmce.eb.getEvent().getCalorimeterResponseList(); 
     
      int trsec = -1;
      for (DetectorParticle dp: par) { //find sector of trpid
      	int pid = dp.getPid(), sec = dp.getSector(DetectorType.ECAL);
      	if(trsec==-1 && sec==trSEC && pid==trPID) trsec=sec;
      }
      
      if(trsec==-1) return;
      
      //gamma-gamma phi angle    
      Point3D point1 = new Point3D((float)ebmce.pmv.get(0).x(),(float)ebmce.pmv.get(0).y(),(float)ebmce.pmv.get(0).z());
      Point3D point2 = new Point3D((float)ebmce.pmv.get(1).x(),(float)ebmce.pmv.get(1).y(),(float)ebmce.pmv.get(1).z());
      point1.rotateZ(Math.toRadians(-60*(mcSEC-1))); point2.rotateZ(Math.toRadians(-60*(mcSEC-1)));
      point1.rotateY(Math.toRadians(-25));           point2.rotateY(Math.toRadians(-25));      
      Vector3 vv1 = new Vector3(point1.x(),point1.y(),point1.z()); Vector3 vv2 = new Vector3(point2.x(),point2.y(),point2.z());
      Vector3 vv12 = vv1.cross(vv2);
      double ggp = Math.toDegrees(Math.atan2(vv12.y(),vv12.x()));
      if(ggp<0) ggp=-ggp;
      ggp=ggp-90;
      if(ggp<0) ggp=ggp+180;      

      if (app.debug) {
      	System.out.println(" ");
      	System.out.println(getEventNumber(de)+" "+cal.size()+" "+par.size());
      	
      	for (DetectorResponse drr : cal) {
      		CalorimeterResponse dr = (CalorimeterResponse) drr;
      		System.out.println("Response "+dr.getAssociation()+" "+dr.getDescriptor().getType()+" "+dr.getDescriptor().getSector()+" "+dr.getDescriptor().getLayer()+" "
      	                      +par.get(dr.getAssociation()).getPid());
      	}
      	
      	int nnn=0;
      	for (DetectorParticle dp : par) {
      		System.out.println("Particle "+nnn+"  "+dp.getSector(DetectorType.ECAL)+" "+dp.getEnergy(DetectorType.ECAL));nnn++;
      		for (DetectorResponse dr : dp.getDetectorResponses()) {
            	  System.out.println(dr.getAssociation()+" "+dr.getDescriptor().getType()+" "+dr.getDescriptor().getLayer());       			
      		}
      	}
      } //debug
      
      List<Particle> plist = new ArrayList<Particle>(); 
      	
      for (DetectorParticle dp : par) { // make list of neutral Particle objects 
      	if(app.debug) {
    		System.out.println("Plist "+trsec+" "+dp.getSector(DetectorType.ECAL)+" "+ebmce.hasTriggerPID+" "+dp.getPid()+" "+dp.getBeta()+" "+dp.getEnergy(DetectorType.ECAL));
    	}
      	  int mcsec = dp.getSector(DetectorType.ECAL);
    	  if(mcsec!=trSEC && mcsec==mcSEC && dp.getPid()==mcPID) { npart++;
		    	if(!ebmce.hasTriggerPID && dp.getPid()==2112) {// this repairs zero momentum neutrons from non-PCAL seeded neutrals
	 				double e = dp.getEnergy(DetectorType.ECAL)/ebmce.getSF(dp); 		
			    	Vector3D vec = new Vector3D() ; vec.copy(dp.getHit(DetectorType.ECAL).getPosition()); vec.unit(); 			    		
			    	dp.vector().add(new Vector3(e*vec.x(),e*vec.y(),e*vec.z())); //track energy for neutrals in DetectorParticle
			    	dp.setPid(mcPID);
			    }
		    	//SF corrected Particle energy from DetectorParticle
			    Particle p = dp.getPhysicsParticle(mcPID); p.setProperty("beta",dp.getBeta()); plist.add(p);
		    }		    
      }
      
	  double X=0,tpi2=0;
	  
//get pizero kinematics 
	  
    	  Particle g1 = ebmce.pmc.get(0), g2 = ebmce.pmc.get(1);
    	  double mpi0 = 0.13495;
          double  e1c = g1.e(), e2c = g2.e();
          double cth1 = Math.cos(g1.theta());
          double cth2 = Math.cos(g2.theta());
          double  cth = g1.cosTheta(g2);
                    X = Math.abs((e1c-e2c)/(e1c+e2c));
                 tpi2 = Math.sqrt(2*mpi0*mpi0/(1-cth)/(1-X*X));
          
                         eff[0][0].fill(opa); eff[1][0].fill(tpi2); eff[2][0].fill(X);
          if (npart>=2) {eff[0][1].fill(opa); eff[1][1].fill(tpi2); eff[2][1].fill(X);}                        
          if (npart==2) {eff[0][2].fill(opa); eff[1][2].fill(tpi2); eff[2][2].fill(X);}           
              	
          double dist=0, du=0;
   		  int[]     npc = new int[50];       int[] neci = new int[50];       int[] neco = new int[50];
   		  int[]     spc = new int[50];       int[]  sci = new int[50];       int[]  sco = new int[50]; 
          double[]  epc = new double[50]; double[]  eci = new double[50]; double[]  eco = new double[50];  					
		  double[]  bpc = new double[50]; double[]  bci = new double[50]; double[]  bco = new double[50];
		  double[]  dpc = new double[50]; double[]  dci = new double[50]; double[]  dco = new double[50];
					
		  Vector3D[] r1 = new Vector3D[50]; Vector3[] c1 = new Vector3[50]; Vector3D[] d1 = new Vector3D[50];
		  Vector3D[] r4 = new Vector3D[50]; Vector3[] c4 = new Vector3[50]; Vector3D[] d4 = new Vector3D[50];
		  Vector3D[] r7 = new Vector3D[50]; Vector3[] c7 = new Vector3[50]; Vector3D[] d7 = new Vector3D[50]; 
              			    	
		  int npp = 0, ipp = 0;
		  for (DetectorParticle dp : par) {
			  if(dp.getSector(DetectorType.ECAL)!=trsec && dp.getPid()==mcPID) {
			    if(npp==0) p1 = dp; //Photon 1
			    if(npp==1) p2 = dp; //Photon 2
			    for(int iresp = 0; iresp < cal.size(); iresp++){
			        CalorimeterResponse dr = (CalorimeterResponse)cal.get(iresp); 			              
			    	int lay = dr.getDescriptor().getLayer(); 		    		  
			    	if (dr.getAssociation(0)==ipp && dr.getDescriptor().getType()==DetectorType.ECAL) {
			    		double dre=dr.getEnergy(),drt = dr.getTime()-stt, drb=dr.getPath()/drt/29.97; int drs=dr.getStatus(); 
			    		double drd=dr.getMatchedDistance();
			    		Vector3D drp=dr.getPosition(); Vector3D drm=dr.getMatchedPosition(); Vector3 drc=dr.getCoordUVW(); 
			            if(lay==1) { npc[0]++ ; npc[npp+1]++;epc[npp+1]=dre;r1[npp+1]=drp;c1[npp+1]=drc;d1[npp+1]=drm;spc[npp+1]=drs;bpc[npp+1]=drb;dpc[npp+1]=drd;}    					
			            if(lay==4) {neci[0]++ ;neci[npp+1]++;eci[npp+1]=dre;r4[npp+1]=drp;c4[npp+1]=drc;d4[npp+1]=drm;sci[npp+1]=drs;bci[npp+1]=drb;dci[npp+1]=drd; }    					
			            if(lay==7) {neco[0]++ ;neco[npp+1]++;eco[npp+1]=dre;r7[npp+1]=drp;c7[npp+1]=drc;d7[npp+1]=drm;sco[npp+1]=drs;bco[npp+1]=drb;dco[npp+1]=drd; }
			        }
			    } 			    		   
			    npp++;
		 	  }
			 ipp++;
		}
		  
	 	REC   = getkin(plist);
	 	
 		if(npc[0]==1) {
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,40).fill(GEN.get(1),REC.get(0)/GEN.get(0));
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,41).fill(GEN.get(0),REC.get(0)/GEN.get(0));
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,42).fill(GEN.get(1),REC.get(1)-GEN.get(1));
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,43).fill(GEN.get(0),REC.get(1)-GEN.get(1));
 		} 
	 	
		if (npart>=2) {
	 	
	 	double nopa = Math.toDegrees(Math.acos(plist.get(0).cosTheta(plist.get(1))));
	 	double dopa = opa-nopa;  //GEN-REC 	 
	 	
 	    //Truth matching based on fixed angle of gamma 1
  		Boolean swap = Math.abs(GEN.get(1)- REC.get(1))<0.17 ? false:true;

 		double  delE1 = REC.get(swap?3:0)/GEN.get(0);
 		double delTH1 = REC.get(swap?4:1)-GEN.get(1);
 		double delPH1 = REC.get(swap?5:2)-GEN.get(2);
 		double  delE2 = REC.get(swap?0:3)/GEN.get(3);
 		double delTH2 = REC.get(swap?1:4)-GEN.get(4);
 		double delPH2 = REC.get(swap?2:5)-GEN.get(5);
 		
 		float e1 = (float) (p1.getEnergy(DetectorType.ECAL)/ebmce.getSF(p1)), b1 = (float) p1.getBeta();
		float e2 = (float) (p2.getEnergy(DetectorType.ECAL)/ebmce.getSF(p2)), b2 = (float) p2.getBeta();	
        
 		if(app.debug && npc[0]>1 && neci[0]>1 && opa>2.5) {
				System.out.println(" "); int scaf = 2;
				System.out.println(getEventNumber(de));
				     				
	            System.out.println(p1.getEnergy(DetectorType.ECAL)+" "+epc[1]+" "+eci[1]+" "+eco[1]);
	            System.out.println(p2.getEnergy(DetectorType.ECAL)+" "+epc[2]+" "+eci[2]+" "+eco[2]);
				
				System.out.println(p1.getEnergy(DetectorType.ECAL)+" "+(epc[1]+eci[1]/scaf+eco[1])/ebmce.getSF(p1));
				System.out.println(p2.getEnergy(DetectorType.ECAL)+" "+(epc[2]+eci[1]/scaf+eco[2])/ebmce.getSF(p2));
				
	    	    System.out.println(npart+" "+npc[0]+" "+neci[0]+" "+neco[0]);
			    System.out.println("Photon 1: "+p1.getMass()+" "+e1+" "+npc[1]+" "+neci[1]+" "+b1+" "+bpc[1]); 
			    System.out.println("Photon 2: "+p2.getMass()+" "+e2+" "+npc[2]+" "+neci[2]+" "+b2+" "+bpc[2]); 
			    
				System.out.println("GEN,REC EN1,EN2 "+GEN.get(0)+" "+ REC.get(swap?3:0)+" "+GEN.get(3)+" "+ REC.get(swap?0:3));
				System.out.println("GEN,REC TH1,TH2 "+GEN.get(1)+" "+ REC.get(swap?4:1)+" "+GEN.get(4)+" "+ REC.get(swap?1:4));
				System.out.println("GEN,REC PH1,PH2 "+GEN.get(2)+" "+ REC.get(swap?5:2)+" "+GEN.get(5)+" "+ REC.get(swap?2:5));
		    	System.out.println("GEN,REC opa "+opa+" "+nopa + " "+dist);
	
			    System.out.println("Phot 1 "+swap+" "+delE1+" "+delTH1+" "+delPH1);
				System.out.println("Phot 2 "+swap+" "+delE2+" "+delTH2+" "+delPH2);   
		} 
 		
        if (npc[0]==2)  {eff[0][3].fill(opa); eff[1][3].fill(tpi2); eff[2][3].fill(X);}                        
        if (neci[0]==2) {eff[0][4].fill(opa); eff[1][4].fill(tpi2); eff[2][4].fill(X);}                        
        if (neco[0]==2) {eff[0][5].fill(opa); eff[1][5].fill(tpi2); eff[2][5].fill(X);}                        
           		
 		if(npc[0]>1 && neci[0]>1) {
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,20).fill(opa,delE1);
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,21).fill(opa,delE2);
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,22).fill(opa,delTH1);
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,23).fill(opa,delTH2);
 		}
 		
 		if(npc[0]==2 && neci[0]==2) {
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,30).fill(opa,delE1);
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,31).fill(opa,delE2);
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,32).fill(opa,delTH1);
 		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,33).fill(opa,delTH2);
 		} 		

		if(npc[1]==1&&neci[1]==1) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,10).fill(dci[1],1);
	    if(npc[2]==1&&neci[2]==1) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,10).fill(dci[2],2);
	    if(npc[1]==1&&neco[1]==1) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,10).fill(dco[1],3);
	    if(npc[2]==1&&neco[2]==1) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,10).fill(dco[2],4);
	    
	    if(npc[1]==1 && npc[2]==1) {Vector3D d11=d1[1].sub(r1[1]), d12=d1[2].sub(r1[2]);
		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d11.x(),1);
	    ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d11.y(),2);
		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d12.x(),3);
	    ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d12.y(),4);
	    }
	    
	    if(neci[1]==1 && neci[2]==1) {Vector3D d41=d4[1].sub(r4[1]), d42=d4[2].sub(r4[2]);
		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d41.x(),5);
	    ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d41.y(),6);
		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d42.x(),7);
	    ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d42.y(),8);
	    }
	    
	    if(neco[1]==1 && neco[2]==1) {Vector3D d71=d7[1].sub(r7[1]), d72=d7[2].sub(r7[2]);		 	   	
		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d71.x(),9);
	    ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d71.y(),10);
		ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d72.x(),11);
	    ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).fill(d72.y(),12);
	    }
   	  }
   }
   
   private class toLocal {
      
      void toLocal(int is, Line3D line) {
          line.rotateZ(Math.toRadians(60*(is-1)));
          line.rotateY(Math.toRadians(25));
          line.translateXYZ(-333.1042, 0.0, 0.0);
          line.rotateZ(Math.toRadians(-60*(is-1)));           
      }
      
      void toLocal(int is, Point3D point) {
          point.rotateZ(Math.toRadians(60*(is-1)));
          point.rotateY(Math.toRadians(25));
          point.translateXYZ(-333.1042, 0.0, 0.0);
          point.rotateZ(Math.toRadians(-60*(is-1)));                      
      }
      
   }
  
   public double getSF(DetectorParticle dp) {
 		return SamplingFractions.getMean(22, dp, ebmce.ccdb);
   }  
   
   public static double getSF(String geom, double e) {
       switch (geom) {
       case "2.4": return 0.268*(1.0151  - 0.0104/e - 0.00008/e/e); 
       case "2.5": return 0.250*(1.0286  - 0.0150/e + 0.00012/e/e);
       }
       return Double.parseDouble(geom);
   } 
   
   public void updateCanvas(DetectorDescriptor dd) {
	   if (!eng.doEng) return;		  
	   updateSPC(dd);
	   updateGAMMA(dd);
   }

   public void updateSPC(DetectorDescriptor dd) {
       
      H1F h,h1,h2;
      H2F h2f;
      F1D f1,f2,f3;
      
      int ilm = ilmap;
        
      double   zmax = (double) mon.getGlob().get("PCMon_zmax");
      DetectorCollection<H1F> ecEngHist  = eng.engine.getHist(); 
      DetectorCollection<H2F> ecEngHist2 = eng.engine.getHist2();  
      
      this.getDetIndices(dd);
            
      // Single event strip hit plots for PCAL, ECinner, ECouter
      c = strips.getCanvas("Strips"); c.divide(3,3); 
      
      int ii=0; int[] thr1 = eng.getStripThresholds(); int[] thr2 = eng.getPeakThresholds();
            
	  for(ilm=0; ilm<3; ilm++) {		
      for(int il=1; il<4; il++) {
         f1 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f1.setParameter(0,0.1*thr1[ilm]);
         f1.setLineColor(4);
         f2 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f2.setParameter(0,0.1*thr2[ilm]);
         f2.setLineColor(2);
         h1 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,1); h1.setFillColor(1); // all hits
         h2 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,0); h2.setFillColor(4); // hits > strip threshold
         h1.setTitleX(dtab[ilm]+otab[il-1]+"PMT"); h1.setTitleY(" ");
         if (il==1) h1.setTitleY("Strip Energy (MeV)"); 
         c.cd(ii); 
         c.getPad(ii).getAxisY().setLog(app.displayControl.pixMax==1?true:false); // set logY at max end of slider range
         c.getPad(ii).getAxisX().setRange(0.,ecPix[ilm].ec_nstr[il-1]+1);
         c.getPad(ii).getAxisY().setRange(0.,5*zmax*app.displayControl.pixMax); ii++;
         c.draw(h1);
         c.draw(h2,"same"); 
         c.draw(f1,"same");
         c.draw(f2,"same");
      }
	  }
	  
	  c.repaint();
	  
	  c = strips.getCanvas("Ns"); c.divide(3, 3);
      ii=0;
	  
	  for(ilm=0; ilm<3; ilm++) {
	      for(int il=1;il<4;il++) {	  
	          h1 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,2); h1.setFillColor(1); // all hits	    	  
	          h1.setTitleX(dtab[ilm]+otab[il-1]+"PMT"); h1.setTitleY(" ");	    	  
	          c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,ecPix[ilm].ec_nstr[il-1]+1); c.draw(h1); ii++;
	      }
	  }
	      	      
	  c.repaint();
	  
	  double xmx1=40.,xmx2=100.;
	  switch (eng.config) {
	  case "muon": xmx1=40. ; xmx2=100.; break;
      case "phot": xmx1=200.; xmx2=500.; break;
      case "pi0" : xmx1=200.; xmx2=500.; break;
	  case "elec": xmx1=100.; xmx2=400.;
	  }

	  // Peak energy plots for PCAL, ECinner, ECouter
      c = peaks.getCanvas("Peaks"); c.divide(3,3); 
      
      ii=0;
	  
	  for(ilm=0; ilm<3; ilm++) {
      for(int il=1;il<4; il++) {
//          h  = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(il-1) ;  h.setFillColor(0);
          h1 = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,5,0).sliceY(il-1) ; h1.setFillColor(34);
          h2 = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,6,0).sliceY(il-1) ; h2.setFillColor(32);
          h2.setOptStat(Integer.parseInt("1000100"));
//          h.setTitleX(dtab[ilm]+otab[il-1]+"Peak Energy (MeV)");  h.setTitle("");
          h1.setTitle(""); h2.setTitle("");    
          h2.setTitleX(dtab[ilm]+otab[il-1]+"Peak Energy (MeV)");          
          c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,5*zmax*app.displayControl.pixMax); ii++;
          if(!app.isSingleEvent()){c.draw(h1); c.draw(h2,"same");} //raw and rec
          if( app.isSingleEvent()) c.draw(h2); // rec only
       }
	  }
      
	  c.repaint();
	  
	  c = peaks.getCanvas("Np"); c.divide(3, 3);
	  
	  ii=0;
	  
	  for(ilm=0; ilm<3; ilm++) {
	  for(int il=1;il<4; il++) {
		  H2F hh = ecPix[ilm].strips.hmap2.get("H2_Clus_Mult").get(is,il,0); 
		  hh.setTitleY(dtab[ilm]+otab[il-1]+" Split Ratio"); hh.setTitleX(dtab[ilm]+" Clusters");
		  c.cd(ii); ii++;
		  c.draw(hh);
	  }
	  }
	  
	  c.repaint();
	  
	  // Cluster Size PCAL, ECinner, ECouter
      c = clusters.getCanvas("Clusters"); c.divide(3,3); 
      
	  ii=0;
	  
	  for(ilm=0; ilm<3; ilm++) {
          h1=ecEngHist.get(is,ilm+1,0); ; h1.setTitleX(dtab[ilm]+"Cluster Size (cm)"); h1.setFillColor(0);
          h2=ecEngHist.get(is,ilm+1,1); ; h2.setFillColor(2); 
          h2.setOptStat(Integer.parseInt("1000100"));
          c.cd(ii); ii++;
          c.draw(h1); c.draw(h2,"same");
	  }
	  
	  ilm=0;
      h1=ecEngHist.get(is,ilm+1,10); ; h1.setTitleX(dtab[ilm]+"Zone 0 Cluster Size (cm)"); h1.setFillColor(4);
      c.cd(ii); ii++; c.draw(h1);  
      h1=ecEngHist.get(is,ilm+1,11); ; h1.setTitleX(dtab[ilm]+"Zone 1 Cluster Size (cm)"); h1.setFillColor(4);
      c.cd(ii); ii++; c.draw(h1);  
      h1=ecEngHist.get(is,ilm+1,12); ; h1.setTitleX(dtab[ilm]+"Zone 23 Cluster Size (cm)"); h1.setFillColor(4);
      c.cd(ii); ii++; c.draw(h1);  
  
      // Cluster energy PCAL, ECinner, ECouter
	  for(ilm=0; ilm<3; ilm++) {
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(3) ; 
          h.setTitle(" "); h.setTitleX(dtab[ilm]+"Cluster Energy (MeV)"); h.setFillColor(2);          
          h.setOptStat(Integer.parseInt("1000100")); 
          c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,10*zmax*app.displayControl.pixMax); ii++;
          c.draw(h); 
	  }
	  
	  /*
	  // Single Cluster total energy     
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(7) ; h.setTitleX("Total Single Cluster Energy (MeV)"); h.setFillColor(2);           
      h.setOptStat(Integer.parseInt("1000100")); 
      c.cd(ii);  c.getPad(ii).getAxisX().setRange(0.,xmx2*2.2); ii++;
      c.draw(h);      
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(4) ; h.setFillColor(4);          
      h.setOptStat(Integer.parseInt("1000000")); 
      c.draw(h,"same");
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(6) ; h.setFillColor(35);          
      h.setOptStat(Integer.parseInt("1000000")); 
      c.draw(h,"same");
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(5) ; h.setFillColor(65);          
      h.setOptStat(Integer.parseInt("1000000")); 
      c.draw(h,"same");
      
      // Cluster total energy
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(6) ; h.setTitleX("Total Cluster Energy (MeV)"); h.setFillColor(2);          
      h.setOptStat(Integer.parseInt("1000100")); 
      c.cd(ii);  c.getPad(ii).getAxisX().setRange(0.,xmx2*2.2); ii++;
      c.draw(h);
      
      c.repaint();
      */
	  
	  //NC TAB
	  
      ii=0;
      c = clusters.getCanvas("Nc"); c.divide(3,3);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap1.get("H1_Clus_Mult").get(is,0,0) ; h.setTitleX("PCAL Clusters"); h.setFillColor(2); h.setTitle(""); 
      h.setOptStat(Integer.parseInt("1000100"));c.draw(h);
      c.cd(ii); ii++; h = ecPix[1].strips.hmap1.get("H1_Clus_Mult").get(is,0,0) ; h.setTitleX("ECIN Clusters"); h.setFillColor(2); h.setTitle(""); 
      h.setOptStat(Integer.parseInt("1000100"));c.draw(h);
      c.cd(ii); ii++; h = ecPix[2].strips.hmap1.get("H1_Clus_Mult").get(is,0,0) ; h.setTitleX("ECOU Clusters"); h.setFillColor(2); h.setTitle(""); 
      h.setOptStat(Integer.parseInt("1000100"));c.draw(h);
      
      h2f=ecEngHist2.get(is,1,1); ; h2f.setTitleX("PCAL Clusters"); h2f.setTitleY("Cluster Size (cm)"); 
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.draw(h2f); ii++;
      h2f=ecEngHist2.get(is,2,1); ; h2f.setTitleX("ECIN Clusters"); h2f.setTitleY("Cluster Size (cm)"); 
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.draw(h2f); ii++;
      h2f=ecEngHist2.get(is,3,1); ; h2f.setTitleX("ECOU Clusters"); h2f.setTitleY("Cluster Size (cm)"); 
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.draw(h2f); ii++;
	  
      h2f=ecPix[0].strips.hmap2.get("H2_Clus_Mult").get(is, 0, 0); ; h2f.setTitleX("PCAL Clusters"); h2f.setTitleY("Cluster Status"); 
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.draw(h2f); ii++;
      h2f=ecPix[1].strips.hmap2.get("H2_Clus_Mult").get(is, 0, 0); ; h2f.setTitleX("ECIN Clusters"); h2f.setTitleY("Cluster Status"); 
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.draw(h2f); ii++;
      h2f=ecPix[2].strips.hmap2.get("H2_Clus_Mult").get(is, 0, 0); ; h2f.setTitleX("ECOU Clusters"); h2f.setTitleY("Cluster Status");       
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.draw(h2f); ii++;
      
      c.repaint();  
      
   }
   
   void updateGAMMA(DetectorDescriptor dd) {
	   
	  H1F h;
	  H2F h2f;
	  F1D f1,f2,f3,f4;

      // MC TAB 
	  
      this.getDetIndices(dd);
      
	  int ii=0,ilm=0;	  
      c = mc.getCanvas("GAMMA"); c.divide(3,5); 	  
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(0) ; h.setTitleX("PCAL Cluster X - GEMC X (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(1) ; h.setTitleX("PCAL Cluster Y - GEMC Y (cm)"); h.setFillColor(2);
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(2) ; h.setTitleX("PCAL Cluster Z - GEMC Z (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
	  
      c.cd(ii); ii++; h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,1) ; 
      h2f.setTitleX("PCAL Cluster X - GEMC X (cm)");  h2f.setTitleY("True Theta (deg)"); c.draw(h2f);
      c.cd(ii); ii++; h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,2) ; 
      h2f.setTitleX("PCAL Cluster Y - GEMC Y (cm)");  h2f.setTitleY("True Theta (deg)"); c.draw(h2f);
      c.cd(ii); ii++; h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,3) ; 
      h2f.setTitleX("PCAL Cluster Z - GEMC Z (cm)");  h2f.setTitleY("True Theta (deg)"); c.draw(h2f);
            
      for(ilm=0; ilm<3; ilm++) {
          c.cd(ii); ii++;
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(1) ; h.setTitleX(dtab[ilm]+"Theta-True Theta"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      }
      
      for(ilm=0; ilm<3; ilm++) {
          c.cd(ii); ii++;
          h2f = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,ilm+1) ; 
          h2f.setTitleX(dtab[ilm]+"Theta-True Theta"); h2f.setTitleY("True Theta (deg)"); 
          h2f.setTitle(" "); c.getPad(ii-1).getAxisZ().setLog(true); c.draw(h2f);
      }
     
      c.cd(ii); ii++;
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(0) ; h.setTitleX("True Theta-GEMC Theta"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.getPad(ii-1).getAxisY().setLog(true);  c.draw(h);
      
      c.cd(ii); ii++; h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,4) ; 
      h2f.setTitleX("PCAL Cluster R - GEMC R (cm)");  h2f.setTitleY("True Theta (deg)"); c.draw(h2f);

      c.repaint();
      
      f1 = new F1D("p0","[a]",5,30); f1.setParameter(0, 1); f1.setLineColor(1); 
      f2 = new F1D("p0","[a]",2,10); f2.setParameter(0, 1); f2.setLineColor(1);
      f3 = new F1D("p0","[a]",5,30); f3.setParameter(0, 0); f3.setLineColor(1); 
      f4 = new F1D("p0","[a]",2,10); f4.setParameter(0, 0); f4.setLineColor(1);
      
      //N=1 TAB      
      ii=0;
      c = mc.getCanvas("N=1"); c.divide(2,2);  
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,40); 
      h2f.setTitleX("GEN #theta (deg)"); h2f.setTitleY("REC/GEN E");      c.cd(ii++);c.draw(h2f);c.draw(f1,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,41); 
      h2f.setTitleX("GEN E (GeV)");      h2f.setTitleY("REC/GEN E");      c.cd(ii++);c.draw(h2f);c.draw(f2,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,42); 
      h2f.setTitleX("GEN #theta (deg)"); h2f.setTitleY("REC-GEN #Theta"); c.cd(ii++);c.draw(h2f);c.draw(f3,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,43); 
      h2f.setTitleX("GEN E (GeV)");      h2f.setTitleY("REC-GEN #Theta"); c.cd(ii++);c.draw(h2f);c.draw(f4,"same");
      c.repaint(); 
      
      f1 = new F1D("p0","[a]",0.,5.2); f1.setParameter(0, 1); f1.setLineColor(1); 
      f2 = new F1D("p0","[a]",0.,5.2); f2.setParameter(0, 0); f2.setLineColor(1);
      
      //N>1 TAB      
      ii=0;
      c = mc.getCanvas("N>1"); c.divide(2,2);  
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,20); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC/GEN E #gamma 1");      c.cd(ii++);c.draw(h2f);c.draw(f1,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,21); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC/GEN E #gamma 2");      c.cd(ii++);c.draw(h2f);c.draw(f1,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,22); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC-GEN #Theta #gamma 1"); c.cd(ii++);c.draw(h2f);c.draw(f2,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,23); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC-GEN #Theta #gamma 2"); c.cd(ii++);c.draw(h2f);c.draw(f2,"same");
      c.repaint();
      
      //N=2 TAB
      ii=0;
      c = mc.getCanvas("N=2"); c.divide(2,2); 	  
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,30); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC/GEN E #gamma 1");      c.cd(ii++);c.draw(h2f);c.draw(f1,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,31); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC/GEN E #gamma 2");      c.cd(ii++);c.draw(h2f);c.draw(f1,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,32); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC-GEN #Theta #gamma 1"); c.cd(ii++);c.draw(h2f);c.draw(f2,"same");
      h2f=ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,33); 
      h2f.setTitleX("Opening Angle (deg)"); h2f.setTitleY("REC-GEN #Theta #gamma 2"); c.cd(ii++);c.draw(h2f);c.draw(f2,"same"); 
      c.repaint();  
      
      //EFF TAB 
      GraphErrors gg; F1D ff;
      c = mc.getCanvas("Eff"); c.divide(3, 2); 
      double x1a = eff[0][0].getXaxis().min(), x1b = eff[0][0].getXaxis().max();
      double x2a = eff[1][0].getXaxis().min(), x2b = eff[1][0].getXaxis().max();
      double x3a = eff[2][0].getXaxis().min(), x3b = eff[2][0].getXaxis().max();

      c.cd(0); c.getPad().setAxisRange(x1a, x1b, 0, 1.1); 
      ff = new F1D("eff","[a]",x1a,x1b); ff.setParameter(0,1); ff.setLineColor(1);
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[0][1], eff[0][0]).getGraph());
      gg.setMarkerColor(1); gg.setLineColor(1); gg.setTitleX("OPENING ANGLE (DEG)"); gg.setTitleY("EFFICIENCY");
      gg.setTitle("BLK: n#gamma>1 RED: n#gamma=2"); c.draw(gg); c.draw(ff,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[0][2], eff[0][0]).getGraph());
      gg.setMarkerColor(2); gg.setLineColor(2); c.draw(gg,"same");
      
      c.cd(1); c.getPad().setAxisRange(x2a, x2b, 0, 1.1); 
      ff = new F1D("eff","[a]",x2a, x2b); ff.setParameter(0,1); ff.setLineColor(1);
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[1][1], eff[1][0]).getGraph());
      gg.setMarkerColor(1); gg.setLineColor(1); gg.setTitleX("PIZERO ENERGY (GEV)"); gg.setTitleY("EFFICIENCY");
      gg.setTitle("BLK: n#gamma>1 RED: n#gamma=2"); c.draw(gg); c.draw(ff,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[1][2], eff[1][0]).getGraph());
      gg.setMarkerColor(2); gg.setLineColor(2); c.draw(gg,"same");
    
      c.cd(2); c.getPad().setAxisRange(x3a, x3b, 0, 1.1); 
      ff = new F1D("eff","[a]",x3a, x3b); ff.setParameter(0,1); ff.setLineColor(1);
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[2][1], eff[2][0]).getGraph());
      gg.setMarkerColor(1); gg.setLineColor(1); gg.setTitleX("ENERGY ASYMMETRY X"); gg.setTitleY("EFFICIENCY");
      gg.setTitle("BLK: n#gamma>1 RED: n#gamma=2"); c.draw(gg); c.draw(ff,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[2][2], eff[2][0]).getGraph());
      gg.setMarkerColor(2); gg.setLineColor(2); c.draw(gg,"same");

      c.cd(3); c.getPad().setAxisRange(x1a, x1b, 0, 1.1); 
      ff = new F1D("eff","[a]",x1a, x1b); ff.setParameter(0,1); ff.setLineColor(1);
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[0][3], eff[0][0]).getGraph());
      gg.setMarkerColor(2); gg.setLineColor(2); gg.setTitleX("OPENING ANGLE (DEG)"); gg.setTitleY("EFFICIENCY");
      gg.setTitle("RED: NPC=2 GRN: NECI=2 BLU: NECO=2"); c.draw(gg); c.draw(ff,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[0][4], eff[0][0]).getGraph());
      gg.setMarkerColor(3); gg.setLineColor(3); c.draw(gg,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[0][5], eff[0][0]).getGraph());
      gg.setMarkerColor(4); gg.setLineColor(4); c.draw(gg,"same");
      
      c.cd(4); c.getPad().setAxisRange(x2a, x2b, 0, 1.1); 
      ff = new F1D("eff","[a]",x2a, x2b); ff.setParameter(0,1); ff.setLineColor(1);
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[1][3], eff[1][0]).getGraph());
      gg.setMarkerColor(2); gg.setLineColor(2); gg.setTitleX("PIZERO ENERGY (GEV)"); gg.setTitleY("EFFICIENCY");
      gg.setTitle("RED: NPC=2 GRN: NECI=2 BLU: NECO=2"); c.draw(gg); c.draw(ff,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[1][4], eff[1][0]).getGraph());
      gg.setMarkerColor(3); gg.setLineColor(3); c.draw(gg,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[1][5], eff[1][0]).getGraph());
      gg.setMarkerColor(4); gg.setLineColor(4); c.draw(gg,"same");
    
      c.cd(5); c.getPad().setAxisRange(x3a, x3b, 0, 1.1); 
      ff = new F1D("eff","[a]",x3a, x3b); ff.setParameter(0,1); ff.setLineColor(1);
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[2][3], eff[2][0]).getGraph());
      gg.setMarkerColor(2); gg.setLineColor(2); gg.setTitleX("ENERGY ASYMMETRY X"); gg.setTitleY("EFFICIENCY");
      gg.setTitle("RED: NPC=2 GRN: NECI=2 BLU: NECO=2"); c.draw(gg); c.draw(ff,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[2][4], eff[2][0]).getGraph());
      gg.setMarkerColor(3); gg.setLineColor(3); c.draw(gg,"same");
      gg = new GraphErrors(); gg.copy(H1F.divide(eff[2][5], eff[2][0]).getGraph());
      gg.setMarkerColor(4); gg.setLineColor(4); c.draw(gg,"same");

      c.repaint();
    
      // RESID TAB
      
      c = mc.getCanvas("Resid"); c.divide(4,4); 
      
      String lab1[] = {"#gamma1 PCAL-ECIN  (cm)",   "#gamma2 PCAL-ECIN (cm)",    "#gamma1 PCAL-ECOU (cm)",    "#gamma2 PCAL-ECOU (cm)"};
	  String lab2[] = {"#gamma1 PCAL #Delta X (cm)","#gamma1 PCAL #Delta Y (cm)","#gamma2 PCAL #Delta X (cm)","#gamma2 PCAL #Delta Y (cm)",
			           "#gamma1 ECIN #Delta X (cm)","#gamma1 ECIN #Delta Y (cm)","#gamma2 ECIN #Delta X (cm)","#gamma2 ECIN #Delta Y (cm)",
			           "#gamma1 ECOU #Delta X (cm)","#gamma1 ECOU #Delta Y (cm)","#gamma2 ECOU #Delta X (cm)","#gamma2 ECOU #Delta Y (cm)"};
			          
      for(ii=0; ii<4; ii++){
          c.cd(ii); 
          h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,10).sliceY(ii) ; h.setTitleX(lab1[ii]); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("11001100")); h.setTitle(""); c.draw(h);
      }
      for(ii=4; ii<16; ii++){
          c.cd(ii); 
          h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,11).sliceY(ii-4) ; h.setTitleX(lab2[ii-4]); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("11001100")); h.setTitle(""); c.draw(h);
      }
      
      c.repaint();
      
      // SF TAB
      
      ii=0;      
      c = mc.getCanvas("SF"); c.divide(2,1); 
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,3);
      h2f.setTitleX("Measured Photon Energy (GeV)"); h2f.setTitleY("Sampling Fraction");  
      c.cd(ii); c.draw(h2f); ii++;
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,2);
      h2f.setTitleX("Measured Photon Energy (GeV)"); h2f.setTitleY("Sampling Fraction");  
      c.cd(ii); c.draw(h2f); ii++;
//      F1D f4 = new F1D("SF",app.geom,0.,1.0); f4.setLineColor(0) ; f4.setLineWidth(2);
//      c.cd(ii); c.draw(h2f); c.draw(f4,"same"); ii++;      
      
      c.repaint();
      
      // PI0 TAB
      
      ii=0;      
      c = mc.getCanvas("PI0"); c.divide(3,2);      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,11,is) ; h.setTitleX("Two Photon Invariant Mass (MeV)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("11001100")); 
      c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,400.); c.draw(h); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2) ; h.setTitleX("X:(E1-E2)/(E1+E2)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("11001100")); 
      c.cd(ii); c.draw(h); ii++;
      
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2) ; 
      h2f.setTitleY("Two Photon Opening Angle (deg)"); h2f.setTitleX("E1*E2 (GeV^2)");      
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true); c.getPad(ii).getAxisY().setRange(0., 12.);
      c.getPad(ii).getAxisZ().setAutoScale(true);
      if(app.isSingleEvent()) c.getPad(ii).getAxisZ().setRange(0.,3.2);
      c.draw(h2f);
      f1 = new F1D("E1*E2/2/(1-COS(x))","0.13495*0.13495/2/(1-cos(x*3.14159/180.))",2.25,20.); f1.setLineColor(1); f1.setLineWidth(2);
      f2 = new F1D("E1*E2/2/(1-COS(x))","0.12495*0.12495/2/(1-cos(x*3.14159/180.))",2.10,20.); f2.setLineColor(1); f2.setLineWidth(1);
      f3 = new F1D("E1*E2/2/(1-COS(x))","0.14495*0.14495/2/(1-cos(x*3.14159/180.))",2.40,20.); f3.setLineColor(1); f3.setLineWidth(1);
      c.draw(f1,"same"); c.draw(f2,"same"); c.draw(f3,"same"); ii++;
      
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,4) ; 
      h2f.setTitleX("Two Photon Opening Angle (deg)"); h2f.setTitleY("X:(E1-E2)/(E1+E2)");      
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true);
      c.getPad(ii).getAxisZ().setAutoScale(true);
      c.draw(h2f); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0) ; h.setTitleX("Pizero Energy Error (MeV)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1) ; h.setTitleX("Pizero Theta Error (deg)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;       
      
      c.repaint();
      
      // Sectors TAB
     
      c = mc.getCanvas("Sectors"); c.divide(3,5);  
      int[] secmap = {12,13,14,15,16,23,24,25,26,34,35,36,45,46,56};
      
      for (int i=0; i<secmap.length; i++) {
        int ss=secmap[i]; int ss1=ss/10; int ss2=ss-10*ss1;        
        h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(ss1,11,ss2) ; h.setTitleX("Sector "+ss+" Invariant Mass (MeV)"); h.setFillColor(2);
        c.cd(i); c.getPad(i).getAxisX().setRange(0.,700.); c.draw(h);
      }
      
      c.repaint();
            
      // Map1 TAB
      
      c = mc.getCanvas("Map1"); c.divide(2, 1);
      c.getPad(0).getAxisZ().setLog(false); c.getPad(0).getAxisZ().setRange(0.7, 1.3);  
      c.getPad(1).getAxisZ().setLog(false); c.getPad(1).getAxisZ().setRange(0.7, 1.3);  
      
      H2F ha,hb,hc;
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 9, 5);
      hb = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 9, 6); 
      hc = hb.divide(hb, ha); hc.setTitleX("Photon 1 X (cm)"); hc.setTitleY("Photon 1 Y (cm)"); 
      c.cd(0); c.draw(hc);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 9, 7);
      hb = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 9, 8);     
      hc = hb.divide(hb, ha); hc.setTitleX("Photon 2 X (cm)"); hc.setTitleY("Photon 2 Y (cm)"); 
      c.cd(1); c.draw(hc);
     
      c.repaint();
      
      // Map2 TAB
      
      c = mc.getCanvas("Map2"); c.divide(2, 1);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 9, 5);
      ha.setTitleX("Photon 1 X (cm)"); ha.setTitleY("Photon 1 Y (cm)"); 
      c.cd(0); c.getPad(0).getAxisZ().setLog(true); c.draw(ha);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 9, 7);
      ha.setTitleX("Photon 2 X (cm)"); ha.setTitleY("Photon 2 Y (cm)"); 
      c.cd(1); c.getPad(1).getAxisZ().setLog(true); c.draw(ha);
     
      c.repaint();
      
      // Map3 TAB
      
      c = mc.getCanvas("Map3"); c.divide(3, 2);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 10, 1);
      ha.setTitleX("Two Photon Inv. Mass"); ha.setTitleY("Photon 1 PCAL U Strips"); 
      c.cd(0);  c.getPad(0).getAxisZ().setLog(true); c.draw(ha);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 10, 2);
      ha.setTitleX("Two Photon Inv. Mass"); ha.setTitleY("Photon 1 PCAL V Strips"); 
      c.cd(1);  c.getPad(1).getAxisZ().setLog(true); c.draw(ha);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 10, 3);
      ha.setTitleX("Two Photon Inv. Mass"); ha.setTitleY("Photon 1 PCAL W Strips"); 
      c.cd(2);  c.getPad(2).getAxisZ().setLog(true); c.draw(ha);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 10, 4);
      ha.setTitleX("Two Photon Inv. Mass"); ha.setTitleY("Photon 2 PCAL U Strips"); 
      c.cd(3);  c.getPad(3).getAxisZ().setLog(true); c.draw(ha);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 10, 5);
      ha.setTitleX("Two Photon Inv. Mass"); ha.setTitleY("Photon 2 PCAL V Strips"); 
      c.cd(4);  c.getPad(4).getAxisZ().setLog(true); c.draw(ha);
      
      ha = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is, 10, 6);
      ha.setTitleX("Two Photon Inv. Mass"); ha.setTitleY("Photon 2 PCAL W Strips"); 
      c.cd(5);  c.getPad(5).getAxisZ().setLog(true); c.draw(ha);
      
      c.repaint();
      
      
   }

@Override
public void constantsEvent(CalibrationConstants arg0, int arg1, int arg2) {
	// TODO Auto-generated method stub
	
}
   
   // For single photon MC runs estimate expected PCAL (x,y,z) impact based on earliest TOF

/*  	  
   if(de.hasBank("ECAL::true")==true) {
    	   DataBank bank = de.getBank("ECAL::true");
    	   double tmax = 30;
    	   for(int i=0; i < bank.rows(); i++) {
    		   float pcX = bank.getFloat("avgX",i);
    		   float pcY = bank.getFloat("avgY",i);
    		   float pcZ = bank.getFloat("avgZ",i);
    		   float pcT = bank.getFloat("avgT",i);
    		   if(pcT<tmax){pcx=pcX; pcy=pcY; pcz=pcZ ; tmax = pcT;}
    	   }
   }
              
   double pcalE[] = new double[6];
 	  
   double    mcR = Math.sqrt(pcx*pcx+pcy*pcy+pcz*pcz);
   double mcThet = Math.asin(Math.sqrt(pcx*pcx+pcy*pcy)/mcR)*180/Math.PI;
   
   ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,0).fill(refTH-mcThet,1.);  //refTH-mcThet
       
   for (int idet=0; idet<3; idet++) {
       res.add(eb.eb.getUnmatchedResponses(null, DetectorType.ECAL,iidet[idet]));
       for(int i = 0; i < res.get(idet).size(); i++){
           int        is = res.get(idet).get(i).getDescriptor().getSector();
           double energy = res.get(idet).get(i).getEnergy();
           double      X = res.get(idet).get(i).getPosition().x();
           double      Y = res.get(idet).get(i).getPosition().y();
           double      Z = res.get(idet).get(i).getPosition().z();
           double    pcR = Math.sqrt(X*X+Y*Y+Z*Z);
           double pcThet = Math.asin(Math.sqrt(X*X+Y*Y)/pcR)*180/Math.PI;
           if (app.debug) {
              System.out.println("Cluster: "+X+" "+Y+" "+Z);
              System.out.println("Cluster-target:"+pcR);
              System.out.println("GEMC-target:"+0.1*mcR);
              System.out.println("Cluster thet: "+pcThet);
              System.out.println("GEMC thet: "+mcThet);
              System.out.println(" ");
           }
           if(idet==0) {
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,0).fill(0.1*pcx-X,1.);
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,0).fill(0.1*pcy-Y,2.);
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,0).fill(0.1*pcz-Z,3.);
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,1).fill(0.1*pcx-X,refTH);
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,2).fill(0.1*pcy-Y,refTH);
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,3).fill(0.1*pcz-Z,refTH);
               ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,8,4).fill(0.1*mcR-pcR,refTH);
           }
           ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,9,0).fill(pcThet-refTH,2.);
           ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,9,idet+1).fill(pcThet-refTH,refTH); //pcThet-refTH
           //Transform X,Y,Z from CLAS into tilted for detector view
           Point3D xyz = new Point3D(-X,Y,Z);
           xyz.rotateZ(Math.toRadians(60*(is-1)));
           xyz.rotateY(Math.toRadians(25));
           xyz.translateXYZ(-333.1042, 0.0, 0.0);
           xyz.rotateZ(Math.toRadians(-60*(is-1)));
           double[] dum  = {xyz.x(),-xyz.y()}; 
//           System.out.println("sector,layer="+is+" "+il);  
//         System.out.println("Cluster: "+dum[0]+" "+dum[1]+" "+xyz.z());
//         System.out.println("Cluster: "+X+" "+Y+" "+Z);
           if (app.isSingleEvent()) ecPix[idet].clusterXY.get(is).add(dum);
           ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(energy*1e3,4,1.);             // Layer Cluster Energy
           ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,4,1).fill(eb.refE*1e-3,energy/refE,1.); // Layer Cluster Normalized Energy
           if(idet==0) pcalE[is-1] += energy*1e3;
           if(energy*1e3>10) {esum[is-1]+=energy*1e3; nesum[idet][is-1]++;}
       }
   }
   
   int  htcc[] = new int[6];
   if(de.hasBank("HTCC::adc")){
 	     DataBank rawbank = de.getBank("HTCC::adc");    
          for(int i=0; i < rawbank.rows(); i++) {
         	     int is = rawbank.getByte("sector", i);
         	     int ia = rawbank.getInt("ADC", i);
         	     if (ia>400)  htcc[is-1]+=ia;
          } 
             
          engineHTCC.processDataEvent(event); 
          
          if(event.hasBank("HTCC::rec")){
     	     DataBank bank = event.getBank("HTCC::rec");    
              for(int i=0; i < bank.rows(); i++) {
             	   int id = bank.getShort("id", i);
             	   int nphe = bank.getShort("nphe", i);
             	   int adc = rawbank.getInt("ADC", id);
             	   System.out.println("id,adc,nphe,);
             	   
              }
          }

   }
   
   if(de.hasBank("MIP::event")){          
       DataBank bank = de.getBank("MIP::event");
       for(int i=0; i < bank.rows(); i++) eb.mip[i]=bank.getByte("mip", i);
       
   } else if (de.hasBank("FTOF::adc")) {
       paddleList = DataProvider.getPaddleList(de);          
       double[] thresh = {500,1000,1000}; 
       for (int i=0; i<6; i++) eb.mip[i]=0;       
       if (paddleList!=null) {
       for (TOFPaddle paddle : paddleList){           
           int toflay = paddle.getDescriptor().getLayer();            
           int   isec = paddle.getDescriptor().getSector();
           eb.mip[isec-1] = (paddle.geometricMean()>thresh[toflay-1]) ? 1:0;
       }
       }
       
   } else if (de.hasBank("REC::Scintillator")) {
       double[] thresh = {7,8,8}; 
       for (int i=0; i<6; i++) eb.mip[i]=0;       
 	  DataBank bank = de.getBank("REC::Scintillator");
 	  for (int i=0; i<bank.rows(); i++) {
 		  if (bank.getByte("detector", i)==12) {
     		  int toflay = bank.getByte("layer", i);
     		  int   isec = bank.getByte("sector", i);
     		  eb.mip[isec-1] = (toflay<3&&bank.getFloat("energy",i)>thresh[toflay-1]) ? 1:0;
 		  }
 	  }      
   }  
   
   if (app.config=="pi0") eb.getNeutralResponses(1,7);

   for (int is=is1; is<is2; is++) {
       
       if (isGoodSector(is)) {
//       if (htcc[is-1]>0) System.out.println("esum,htcc = "+is+" "+esum[is-1]+" "+htcc[is-1]);
       if(nesum[0][is-1]==1) { 
           ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],8,1.);                                       // Total Single Cluster Energy PC=1                     
           ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,3).fill(1e-3*esum[is-1],1e-3*esum[is-1]/eb.refE,1.);            // S.F. vs. meas.photon energy  
           if(htcc[is-1]>1500) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],5,1.);                   // Total Cluster Energy PC>0
           if(htcc[is-1]>3000) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],7,1.);                   // Total Cluster Energy            
//           if(htcc[is-1]>3000&&pcalE[is-1]>50) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],6,1.);   // Total Cluster Energy            

       }

       if (app.config=="pi0" && ebmc.mip[is-1]!=1) {  // No FTOF MIP in sector
     	  
           DataBank ecBank = de.getBank("ECAL::clusters");              
    	  
           double invmass = Math.sqrt(ebmc.getTwoPhotonInvMass(is));
           double     opa = Math.acos(ebmc.cth)*180/3.14159;
           
           boolean badPizero = ebmc.isMC ? false:ebmc.X>0.5 && opa<8;
   
           if(eb.iis[0]>0&&eb.iis[1]>0&&!badPizero) {
         	  
           System.out.println("invmass "+invmass);
           
           ecPix[0].strips.hmap1.get("H1_a_Hist").get(eb.iis[0], 11,eb.iis[1]).fill((float)invmass*1e3); // Two-photon invariant mass
           
           if(eb.iis[0]==eb.iis[1]) {
               
           if(nesum[0][is-1]>1 && nesum[1][is-1]>0) {
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],7,1.);      // Total Cluster Energy            
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,2).fill(eb.e1,eb.SF1db,1.);    // S.F. vs. meas. photon energy            
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2).fill(opa,eb.e1c*eb.e2c,1.); // E1*E2 vs opening angle            
           }            
                     
           if (invmass>0.100 && invmass<0.180) {
               ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0).fill((float)(1e3*(Math.sqrt(eb.tpi2)-refE))); // Pizero total energy error
               ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1).fill(Math.acos(eb.cpi0)*180/3.14159-refTH);   // Pizero theta angle error
               ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2).fill((float)eb.X);                                 // Pizero energy asymmetry
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,4).fill((float)eb.X,opa);      
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,4).fill(eb.distance1[0],1,1.); // Pizero photon 1 PCAL-ECinner cluster error
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,4).fill(eb.distance1[1],2,1.); // Pizero photon 2 PCAL-ECinner cluster error
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,4).fill(eb.distance2[0],3,1.); // Pizero photon 1 PCAL-ECouter cluster error
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,4).fill(eb.distance2[1],4,1.); // Pizero photon 2 PCAL-ECouter cluster error   
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(1,9,5).fill(-eb.x[0][0], eb.y[0][0],1.);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(1,9,6).fill(-eb.x[0][0], eb.y[0][0],invmass/eb.mpi0);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(1,9,7).fill(-eb.x[1][0], eb.y[1][0],1.);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(1,9,8).fill(-eb.x[1][0], eb.y[1][0],invmass/eb.mpi0);
               float ipU,ipV,ipW;
               ipU = (ecBank.getInt("coordU", eb.iip[0][0])-4)/8;
               ipV = (ecBank.getInt("coordV", eb.iip[0][0])-4)/8;
               ipW = (ecBank.getInt("coordW", eb.iip[0][0])-4)/8;
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,10,1).fill(invmass*1e3,ipU);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,10,2).fill(invmass*1e3,ipV);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,10,3).fill(invmass*1e3,ipW);
               ipU = (ecBank.getInt("coordU", eb.iip[1][0])-4)/8;
               ipV = (ecBank.getInt("coordV", eb.iip[1][0])-4)/8;
               ipW = (ecBank.getInt("coordW", eb.iip[1][0])-4)/8;
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,10,4).fill(invmass*1e3,ipU);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,10,5).fill(invmass*1e3,ipV);
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,10,6).fill(invmass*1e3,ipW);
           }
       }
       }
       }
   
       }
   }
*/ 
   
}
