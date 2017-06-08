package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.clas.detector.CalorimeterResponse;
import org.jlab.clas.detector.DetectorResponse;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.service.eb.EventBuilder;
import org.jlab.service.ec.ECPart;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F; 

public class ECEngineApp extends FCApplication implements ActionListener {

    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    strips = new EmbeddedCanvasTabbed("Strips");
    EmbeddedCanvasTabbed     peaks = new EmbeddedCanvasTabbed("Peaks");
    EmbeddedCanvasTabbed  clusters = new EmbeddedCanvasTabbed("Clusters");
    EmbeddedCanvasTabbed        mc = new EmbeddedCanvasTabbed("MC");
    EmbeddedCanvas               c = new EmbeddedCanvas();
    
    JTextField                  sf = new JTextField(4);  
    JComboBox                   cb = null;
    JCheckBox             debugBtn = null;
    JCheckBox               engBtn = null;
    JCheckBox               crtBtn = null;
    ButtonGroup                bG1 = null;
    
    DetectorType[] detNames = {DetectorType.PCAL, DetectorType.ECIN, DetectorType.ECOUT};
    double pcx,pcy,pcz;
    double refE=0,refP=0,refTH=25;

   public ECEngineApp(String name, ECPixels[] ecPix) {
      super(name,ecPix);
      initCanvas();
      createPopupMenu();
      is1 = ECConstants.IS1;
      is2 = ECConstants.IS2;
   }
   
   public JPanel getPanel() {        
       engineView.setLayout(new BorderLayout());
       engineView.add(getCanvasPane(),BorderLayout.CENTER);
       engineView.add(getButtonPane(),BorderLayout.PAGE_END);
       strips.addCanvas("Ns");
       peaks.addCanvas("Np");
       clusters.addCanvas("Nc");
       mc.addCanvas("Resid");
       mc.addCanvas("SF");
       mc.addCanvas("PI0");
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
   
   public JPanel getButtonPane() {
       
       buttonPane = new JPanel();
       buttonPane.setLayout(new FlowLayout());
       
       buttonPane.add(new JLabel("Trigger:"));
       
       bG1 = new ButtonGroup();
       JRadioButton bc = new JRadioButton("Cluster"); buttonPane.add(bc); bc.setActionCommand("0"); bc.addActionListener(this);
       JRadioButton bp = new JRadioButton("Pixel");   buttonPane.add(bp); bp.setActionCommand("1"); bp.addActionListener(this); 
       bG1.add(bc); bG1.add(bp); bp.setSelected(true);
       
       buttonPane.add(new JLabel("Config:"));
       
       cb = new JComboBox();
       DefaultComboBoxModel model = (DefaultComboBoxModel) cb.getModel();
       model.addElement("photon");
       model.addElement("electron");
       model.addElement("muon");
       model.addElement("pizero");
       cb.setModel(model);
       cb.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               String s = (String) cb.getSelectedItem();
               switch (s) {
               case   "photon": app.config="phot"; break;
               case "electron": app.config="elec"; break;
               case     "muon": app.config="muon"; break;
               case   "pizero": app.config="pi0";
               }
               mon.initEngine();
           }
       });

       buttonPane.add(cb);
       
       buttonPane.add(new JLabel("SF:"));
       sf.setActionCommand("SF"); sf.addActionListener(this); sf.setText(app.geom);  
       buttonPane.add(sf); 
       
       debugBtn = new JCheckBox("Debug");
       debugBtn.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
               if(e.getStateChange() == ItemEvent.SELECTED) {
                   app.debug = true;
               } else {
                   app.debug = false;
               };
           }
       }); 
       debugBtn.setSelected(false);
       buttonPane.add(debugBtn);
       
       engBtn = new JCheckBox("ECEngine");
       engBtn.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
               if(e.getStateChange() == ItemEvent.SELECTED) {
                   app.doEng = true;
               } else {
                   app.doEng = false;
               };
           }
       });           
       engBtn.setSelected(false);
       buttonPane.add(engBtn);
       
       crtBtn = new JCheckBox("CRT");
       crtBtn.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
               if(e.getStateChange() == ItemEvent.SELECTED) {
                   app.isCRT = true;
               } else {
                   app.isCRT = false;
               };
           }
       });         
       crtBtn.setSelected(false);
       buttonPane.add(crtBtn);
       
       return buttonPane;
       
   }

   
   public void actionPerformed(ActionEvent e) {
       if(e.getActionCommand().compareTo("SF")==0) app.geom = sf.getText();
       app.trigger = Integer.parseInt(bG1.getSelection().getActionCommand());
   }      
   
   private void createPopupMenu(){
       strips.popup = new JPopupMenu();
       JMenuItem itemCopy = new JMenuItem("Copy Canvas");
       itemCopy.addActionListener(strips.getCanvas("Strips"));
       strips.popup.add(itemCopy);
   } 
   
   public void initCanvas() {
       c.setAxisFontSize(14);
       c.setStatBoxFontSize(12);       
   }
   
   public int getDet(int layer) {
       int[] il = {0,0,0,1,1,1,2,2,2}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
       return il[layer-1];
    }
   
   public int getLay(int layer) {
       int[] il = {1,2,3,1,2,3,1,2,3}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
       return il[layer-1];
    }
      
   public void addEvent(DataEvent event) {
       DataBank mcData,genData = null;
       double tmax = 30;
       
       if(event.hasBank("MC::Particle")==true) {
           genData = event.getBank("MC::Particle");
           int   pid = genData.getInt("pid", 0);
           float ppx = genData.getFloat("px",0);
           float ppy = genData.getFloat("py",0);
           float ppz = genData.getFloat("pz",0);
           double  rm = 0.;
           if (pid==111) rm=0.1349764; // pizero mass               
           refP  = Math.sqrt(ppx*ppx+ppy*ppy+ppz*ppz);  
           refE  = Math.sqrt(refP*refP+rm*rm);    
           refTH = Math.acos(ppz/refP)*180/3.14159;
       }
           
       if(event.hasBank("ECAL::true")==true) {
           mcData = event.getBank("ECAL::true");
           for(int i=0; i < mcData.rows(); i++) {
               float pcX = mcData.getFloat("avgX",i);
               float pcY = mcData.getFloat("avgY",i);
               float pcZ = mcData.getFloat("avgZ",i);
               float pcT = mcData.getFloat("avgT",i);
               if(pcT<tmax){pcx=pcX; pcy=pcY; pcz=pcZ ; tmax = pcT;}
           }
       }
       
       fillHistos(event);
   }   
   
   public void fillHistos(DataEvent event) {
       
       double[] esum = {0,0,0,0,0,0};
       int[][] nesum = {{0,0,0,0,0,0},{0,0,0,0,0,0},{0,0,0,0,0,0}};
       int[]   iidet = {1,4,7};
       int       ipp = 0;
       
       if (app.isSingleEvent()) {
           for (int i=0; i<3; i++) app.getDetectorView().getView().removeLayer("L"+i);
           for (int i=0; i<3; i++) app.getDetectorView().getView().addLayer("L"+i);
           for (int is=is1; is<is2; is++ ) {
               for (int ilm=0; ilm<3; ilm++) {
                   ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).reset();
                   ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,5,0).reset();
                   ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,6,0).reset();
                   ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).reset();
               }
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,3).reset(); //Sampling fraction vs energy
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2).reset(); //E1*E2 vs opening angle
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).reset(); //Cluster X,Y,X - MC
               ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).reset(); //Photon 1,2, errors
               ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0).reset(); //Pizero energy error
               ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1).reset(); //Pizero theta error
               ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2).reset(); //X:(E1-E2)/(E1+E2)
           }
       }
       
       //Monitor EC peak data
       
      if(event.hasBank("ECAL::peaks")){
         DataBank bank = event.getBank("ECAL::peaks");
         for(int i=0; i < bank.rows(); i++) {
            int    is = bank.getByte("sector",i);
            int    il = bank.getByte("layer",i);
            float  en = bank.getFloat("energy",i);
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
                DetectorShape2D shape = new DetectorShape2D(detNames[getDet(il)],is,ipp++,0); 
                shape.getShapePath().addPoint(orig.x(),orig.y(),0.);
                shape.getShapePath().addPoint(end.x(),end.y(),0.);
                app.getDetectorView().getView().addShape("L"+getDet(il), shape);
                double[] dum = {orig.x(),orig.y(),end.x(),end.y()};
                ecPix[getDet(il)].peakXY.get(is).add(dum);
//                System.out.println("sector,layer="+is+" "+il);  
//                System.out.println(orig.x()+" "+orig.y()+" "+orig.z());
//                System.out.println(end.x()+" "+end.y()+" "+end.z());
//                System.out.println(" ");
            }
           
//             double[] dum = {orig.x(),-orig.y(),orig.z(),end.x(),-end.y(),end.z()};
//            if (app.isSingleEvent()) ecPix[getDet(il)].peakXY.get(is).add(dum);
//
//            System.out.println("sector,layer="+is+" "+il);  
//            System.out.println("Xo,Yo,Zo= "+xo+" "+yo+" "+zo);
//           System.out.println("Xe,Ye,Ze= "+xe+" "+ye+" "+ze);
//            System.out.println("energy="+en);  
//            System.out.println(" ");
         }
        
      } 
      
      // Monitor EC cluster data
            
      ECPart                   part = new ECPart();      
      EventBuilder               eb = new EventBuilder();
      List<List<CalorimeterResponse>>   res = new ArrayList<List<CalorimeterResponse>>();      
      part.setGeom(app.geom);  part.setConfig(app.config);      
      List<CalorimeterResponse>  ecClusters = part.readEC(event);   
      
      if (ecClusters.size()>0) {
          
      for (int idet=0; idet<3; idet++) {
          res.add(eb.getUnmatchedResponses(ecClusters, DetectorType.EC,iidet[idet]));
          for(int i = 0; i < res.get(idet).size(); i++){
              int        is = res.get(idet).get(i).getDescriptor().getSector();
              double energy = res.get(idet).get(i).getEnergy();
              double      X = res.get(idet).get(i).getPosition().x();
              double      Y = res.get(idet).get(i).getPosition().y();
              double      Z = res.get(idet).get(i).getPosition().z();
              double    pcR = Math.sqrt(X*X+Y*Y+Z*Z);
              double pcThet = Math.asin(Math.sqrt(X*X+Y*Y)/pcR)*180/3.14159;
              double    mcR = Math.sqrt(pcx*pcx+pcy*pcy+pcz*pcz);
              double mcThet = Math.asin(Math.sqrt(pcx*pcx+pcy*pcy)/mcR)*180/3.14159;
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
              }
              ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,9,0).fill(refTH-mcThet,1.);
              ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,9,0).fill(pcThet-mcThet,2.);
              //Transform X,Y,Z from CLAS into tilted for detector view
              Point3D xyz = new Point3D(-X,Y,Z);
              xyz.rotateZ(Math.toRadians(60*(is-1)));
              xyz.rotateY(Math.toRadians(25));
              xyz.translateXYZ(-333.1042, 0.0, 0.0);
              xyz.rotateZ(Math.toRadians(-60*(is-1)));
              double[] dum  = {xyz.x(),-xyz.y()}; 
//              System.out.println("sector,layer="+is+" "+il);  
//            System.out.println("Cluster: "+dum[0]+" "+dum[1]+" "+xyz.z());
//            System.out.println("Cluster: "+X+" "+Y+" "+Z);
              if (app.isSingleEvent()) ecPix[idet].clusterXY.get(is).add(dum);
              ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(energy*1e3,4,1.);          // Layer Cluster Energy
              ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,4,1).fill(refE*1e-3,energy/refE,1.); // Layer Cluster Normalized Energy
              if(energy*1e3>10) {esum[is-1]=esum[is-1]+energy*1e3; nesum[idet][is-1]++;}
          }
      }
          
      for (int is=is1; is<is2; is++) {
          
          if (isGoodSector(is)) {
          if(nesum[0][is-1]==1) { 
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],8,1.);                         // Total Single Cluster Energy PC=1                     
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,3).fill(1e-3*esum[is-1],1e-3*esum[is-1]/refE,1.); // S.F. vs. meas.photon energy  
              if(nesum[1][is-1]==1) ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],5,1.);   // Total Single Cluster Energy PC=1.EC=1 
          }
                    
          if (app.config=="pi0") {
              
              double invmass = 1e3*Math.sqrt(part.getTwoPhoton(ecClusters,is));
              double     opa = Math.acos(part.cth)*180/3.14159;
              ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill((float)invmass,6,1.); // Two-photon invariant mass
              
              if(nesum[0][is-1]>1 && nesum[1][is-1]>0) {
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).fill(esum[is-1],7,1.);          // Total Cluster Energy            
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,2).fill(part.e1,part.SF1,1.);      // S.F. vs. meas. photon energy            
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2).fill(opa,part.e1c*part.e2c,1.); // E1*E2 vs opening angle            
              }            
                        
              if (invmass>95 && invmass<200) {
                  ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0).fill((float)(1e3*(Math.sqrt(part.tpi2)-refE))); // Pizero total energy error
                  ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1).fill(Math.acos(part.cpi0)*180/3.14159-refTH);   // Pizero theta angle error
                  ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2).fill((float)part.X);                            // Pizero energy asymmetry
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,4).fill(opa,(float)part.X);      
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).fill(part.distance11,1,1.); // Pizero photon 1 PCAL-ECinner cluster error
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).fill(part.distance12,2,1.); // Pizero photon 2 PCAL-ECinner cluster error
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).fill(part.distance21,3,1.); // Pizero photon 1 PCAL-ECouter cluster error
                  ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).fill(part.distance22,4,1.); // Pizero photon 2 PCAL-ECouter cluster error     
                  if (app.debug) System.out.println(part.distance11+" "+part.distance12+" "+part.distance21+" "+part.distance22);
              }
          }
          }
      }
      
      }
      
      if(event.hasBank("ECAL::calib")){
          double raw[] = new double[3];
          double rec[] = new double[3];
          DataBank bank = event.getBank("ECAL::calib");
          for(int i=0; i < bank.rows(); i++) {
             int is = bank.getByte("sector",i);
             int il = bank.getByte("layer",i);
             raw[0] = bank.getFloat("rawEU",i);
             raw[1] = bank.getFloat("rawEV",i);
             raw[2] = bank.getFloat("rawEW",i);
             rec[0] = bank.getFloat("recEU",i);
             rec[1] = bank.getFloat("recEV",i);
             rec[2] = bank.getFloat("recEW",i);
             
             for (int k=1; k<4; k++) ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,5,0).fill(raw[k-1]*1e3,k,1.);        // raw peak energies          
             for (int k=1; k<4; k++) ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,6,0).fill(rec[k-1]*1e3,k,1.);        // reconstructed peak energies          
             for (int k=1; k<4; k++) ecPix[getDet(il)].strips.hmap2.get("H2_a_Hist").get(is,6,k).fill(1e-3*refE,rec[k-1]/refE);  // sampling fraction vs. energy          
//             System.out.println("sector,layer ="+is+" "+il);  
//             System.out.println("X,Y,Z,energy="+X+" "+Y+" "+Z+" "+energy);  
//             System.out.println(" ");
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
  
   public static double getSF(String geom, double e) {
       switch (geom) {
       case "2.4": return 0.268*(1.0151  - 0.0104/e - 0.00008/e/e); 
       case "2.5": return 0.250*(1.0286  - 0.0150/e + 0.00012/e/e);
       }
       return Double.parseDouble(geom);
   }   
   
   public void updateCanvas(DetectorDescriptor dd) {
       
      if (!app.doEng) return;
	  
      H1F h,h1,h2;
      H2F h2f;
      
      String otab[]={"U ","V ","W "};             
      String dtab[]={"PCAL ","EC Inner ","EC Outer "};
      
      int ilm = ilmap;
        
      double   zmax = (double) mon.getGlob().get("PCMon_zmax");
      DetectorCollection<H1F> ecEngHist = (DetectorCollection<H1F>) mon.getGlob().get("ecEng");
      
      this.getDetIndices(dd);
            
      // Single event strip hit plots for PCAL, ECinner, ECouter
      c = strips.getCanvas("Strips"); c.divide(3,3); 
      
      int ii=0;
            
	  for(ilm=0; ilm<3; ilm++) {
      for(int il=1;il<4;il++) {
         F1D f1 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f1.setParameter(0,0.1*ecPix[ilm].getStripThr(app.config,il));
         f1.setLineColor(4);
         F1D f2 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f2.setParameter(0,0.1*ecPix[ilm].getPeakThr(app.config,il));
         f2.setLineColor(2);
         h1 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,1); h1.setFillColor(1);
         h2 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,0); h2.setFillColor(4); 
         h1.setTitleX(dtab[ilm]+otab[il-1]+"PMT"); h1.setTitleY(" ");
         if (il==1) h1.setTitleY("Strip Energy (MeV)"); 
         c.cd(ii); 
         c.getPad(ii).getAxisX().setRange(0.,ecPix[ilm].ec_nstr[il-1]+1);
         c.getPad(ii).getAxisY().setRange(0.,5*zmax*app.displayControl.pixMax); ii++;
         c.draw(h1);
         c.draw(h2,"same"); 
         c.draw(f1,"same");
         c.draw(f2,"same");
      }
	  }
	  
	  c.repaint();
	  
	  if (!app.doEng) return;
	  
	  double xmx1=40.,xmx2=100.;
	  switch (app.config) {
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
          h  = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(il-1) ;  h.setFillColor(0);
          h1 = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,5,0).sliceY(il-1) ; h1.setFillColor(34);
          h2 = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,6,0).sliceY(il-1) ; h2.setFillColor(32);
          h2.setOptStat(Integer.parseInt("11001100"));
          h.setTitleX(dtab[ilm]+otab[il-1]+"Peak Energy (MeV)");          
          h2.setTitleX(dtab[ilm]+otab[il-1]+"Peak Energy (MeV)");          
          c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,xmx1); ii++;
          if(!app.isSingleEvent()){c.draw(h); c.draw(h1,"same"); c.draw(h2,"same");}
          if( app.isSingleEvent()) c.draw(h2);
       }
	  }
      
	  c.repaint();
	  
	  // Cluster error PCAL, ECinner, ECouter
      c = clusters.getCanvas("Clusters"); c.divide(3,3); 
      
	  ii=0;
	  
	  for(ilm=0; ilm<3; ilm++) {
          h1=ecEngHist.get(is,ilm+1,0); ; h1.setTitleX(dtab[ilm]+"Cluster Error (cm)"); h1.setFillColor(0);
          h2=ecEngHist.get(is,ilm+1,1); ; h2.setFillColor(2); 
          h2.setOptStat(Integer.parseInt("1100"));
          c.cd(ii); c.getPad(ii).getAxisX().setRange(-100.,100.); ii++;
          c.draw(h1); c.draw(h2,"same");
	  }
	  
      // Cluster energy PCAL, ECinner, ECouter
	  for(ilm=0; ilm<3; ilm++) {
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)3) ; h.setTitleX(dtab[ilm]+"Cluster Energy (MeV)"); h.setFillColor(2);          
          h.setOptStat(Integer.parseInt("11001100")); 
          c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,xmx2); ii++;
          c.draw(h); 
	  }
	  
	  // Single Cluster total energy     
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)4) ; h.setTitleX("Total Single Cluster Energy (MeV)"); h.setFillColor(2);          
//      h.setOptStat(Integer.parseInt("11001100")); 
      h.setOptStat(Integer.parseInt("1000100")); 
      c.cd(ii);  c.getPad(ii).getAxisX().setRange(0.,xmx2*2.2); ii++;
      c.draw(h);      
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)7) ; h.setFillColor(32);          
      h.setOptStat(Integer.parseInt("1000000")); 
      c.draw(h,"same");
      
      // Cluster total energy
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)6) ; h.setTitleX("Total Cluster Energy (MeV)"); h.setFillColor(2);          
      h.setOptStat(Integer.parseInt("11001100")); 
      c.cd(ii);  c.getPad(ii).getAxisX().setRange(0.,xmx2*2.2); ii++;
      c.draw(h);
      
      c.repaint();
      
      // JTabbedPane plots: mc1=TRUE-DGTZ residuals  resid=distance residuals pi0=Pi-zero plots   
      
      c = mc.getCanvas("MC"); c.divide(3,4); 
      
	  ii=0;
	  
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(0) ; h.setTitleX("PCAL Cluster X - GEMC X (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(1) ; h.setTitleX("PCAL Cluster Y - GEMC Y (cm)"); h.setFillColor(2);
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(2) ; h.setTitleX("PCAL Cluster Z - GEMC Z (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
	  
      for(ilm=0; ilm<3; ilm++) {
          c.cd(ii); 
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(0) ; h.setTitleX("Generated Theta-"+dtab[ilm]+" Theta"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
          c.cd(ii+3); ii++;
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(1) ; h.setTitleX(dtab[ilm]+" Theta-GEMC Theta"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      }
      
      ii=ii+3;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0) ; h.setTitleX("Pizero Energy Error (MeV)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1) ; h.setTitleX("Pizero Theta Error (deg)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;
     
      c.repaint();
      
      c = mc.getCanvas("Resid"); c.divide(2,2); 
      String lab[] = {"Photon 1 - ECin (cm)","Photon 1 - ECout (cm)","Photon 2 - ECin (cm)","Photon 2 - ECout (cm)"};
      for(ii=0; ii<4; ii++){
          c.cd(ii); c.getPad(ii).getAxisX().setRange(-2.,40.);
          h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).sliceY(ii) ; h.setTitleX(lab[ii]); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("11001100")); c.draw(h);
      }
      
      c.repaint();
      
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
      
      ii=0;
      
      c = mc.getCanvas("PI0"); c.divide(3,2);      
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)5) ; h.setTitleX("Pizero Invariant Mass (MeV)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("11001100")); 
      c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,400.); c.draw(h); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2) ; h.setTitleX("X:(E1-E2)/(E1+E2)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("11001100")); 
      c.cd(ii); c.draw(h); ii++;
      
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2) ; 
      h2f.setTitleX("Two Photon Opening Angle (deg)"); h2f.setTitleY("E1*E2 (GeV^2)");      
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true);
      c.getPad(ii).getAxisZ().setAutoScale(true);
      if(app.isSingleEvent()) c.getPad(ii).getAxisZ().setRange(0.,3.2);
      c.draw(h2f);
      F1D f1 = new F1D("E1*E2/2/(1-COS(x))","0.13495*0.13495/2/(1-cos(x*3.14159/180.))",3.6,20.); f1.setLineColor(1); f1.setLineWidth(2);
      F1D f2 = new F1D("E1*E2/2/(1-COS(x))","0.12495*0.12495/2/(1-cos(x*3.14159/180.))",3.4,20.); f2.setLineColor(5); f2.setLineWidth(2);
      F1D f3 = new F1D("E1*E2/2/(1-COS(x))","0.14495*0.14495/2/(1-cos(x*3.14159/180.))",3.8,20.); f3.setLineColor(5); f3.setLineWidth(2);
      c.draw(f1,"same"); c.draw(f2,"same"); c.draw(f3,"same"); ii++;
      
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,4) ; 
      h2f.setTitleX("Two Photon Opening Angle (deg)"); h2f.setTitleY("X:(E1-E2)/(E1+E2)");      
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true);
      c.getPad(ii).getAxisZ().setAutoScale(true);
      c.draw(h2f);
       
      c.repaint();
      
   }
   
}
