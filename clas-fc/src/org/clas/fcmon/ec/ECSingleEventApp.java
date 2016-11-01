package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F; 

public class ECSingleEventApp extends FCApplication {

    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    strips = new EmbeddedCanvasTabbed("Strips");
    EmbeddedCanvasTabbed     peaks = new EmbeddedCanvasTabbed("Peaks");
    EmbeddedCanvasTabbed  clusters = new EmbeddedCanvasTabbed("Clusters");
    EmbeddedCanvasTabbed        mc = new EmbeddedCanvasTabbed("MC");
    EmbeddedCanvas               c = new EmbeddedCanvas();

   public ECSingleEventApp(String name, ECPixels[] ecPix) {
      super(name,ecPix);
      initCanvas();
      createPopupMenu();
   }
   
   public JPanel getCalibPane() {        
       engineView.setLayout(new BorderLayout());
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
       engineView.add(hPane);
       mc.addCanvas("Resid");
       mc.addCanvas("SF");
       mc.addCanvas("PI0");
       return engineView;       
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
   
   public void addEvent(DataEvent event) {
       
   }
   
   public void fillHistos() {
       
   }
   
   public static double getSF(String geom, double e) {
       switch (geom) {
       case "2.4": return 0.268*(1.0151  - 0.0104/e - 0.00008/e/e); 
       case "2.5": return 0.250*(1.0286  - 0.0150/e + 0.00012/e/e);
       }
       return Double.parseDouble(geom);
   }   
   
   public void updateCanvas(DetectorDescriptor dd) {
	  
      H1F h,h1,h2;
      H2F h2f;
      
      String otab[]={"U ","V ","W "};             
      String dtab[]={"PCAL ","EC Inner ","EC Outer "};
      
      int ilm = ilmap;
        
      double   zmax = (double) mon.getGlob().get("PCMon_zmax");
      String config = (String) mon.getGlob().get("config");
      DetectorCollection<H1F> ecEngHist = (DetectorCollection<H1F>) mon.getGlob().get("ecEng");
      Boolean                      doEng =                (Boolean) mon.getGlob().get("doEng");
      
      this.getDetIndices(dd);
            
      // Single event strip hit plots for PCAL, ECinner, ECouter
      c = strips.getCanvas("Strips"); c.divide(3,3); 
      
      int ii=0;
            
	  for(ilm=0; ilm<3; ilm++) {
      for(int il=1;il<4;il++) {
         F1D f1 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f1.setParameter(0,0.1*ecPix[ilm].getStripThr(config,il));
         f1.setLineColor(4);
         F1D f2 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f2.setParameter(0,0.1*ecPix[ilm].getPeakThr(config,il));
         f2.setLineColor(2);
         h1 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,1); h1.setFillColor(0);
         h2 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,0); h2.setFillColor(4); 
         h1.setTitleX(dtab[ilm]+otab[il-1]+"PMT"); h1.setTitleY(" ");
         if (il==1) h1.setTitleY("Strip Energy (MeV)"); 
         c.cd(ii); 
         c.getPad(ii).getAxisX().setRange(0.,ecPix[ilm].ec_nstr[il-1]+1);
         c.getPad(ii).getAxisY().setRange(0.,1.2*zmax*app.displayControl.pixMax); ii++;
         c.draw(h1);
         c.draw(h2,"same"); 
         c.draw(f1,"same");
         c.draw(f2,"same");
      }
	  }
	  
	  c.repaint();
	  
	  if (!doEng) return;
	  
	  double xmx1=40.,xmx2=100.;
	  switch (config) {
	  case "muon": xmx1=40. ; xmx2=100.; break;
	  case "phot": xmx1=200.; xmx2=500.; break;
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
          h2.setOptStat(Integer.parseInt("1100"));
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
          h.setOptStat(Integer.parseInt("1110")); 
          c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,xmx2); ii++;
          c.draw(h); 
	  }
	  
	  // Single Cluster total energy
     
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)4) ; h.setTitleX("Total Single Cluster Energy (MeV)"); h.setFillColor(2);          
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii);  c.getPad(ii).getAxisX().setRange(0.,xmx2*2); ii++;
      c.draw(h);      
      
      // Cluster total energy
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)6) ; h.setTitleX("Total Cluster Energy (MeV)"); h.setFillColor(2);          
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii);  c.getPad(ii).getAxisX().setRange(0.,xmx2*2); ii++;
      c.draw(h);
      
      c.repaint();
      
      // JTabbedPane plots: mc1=TRUE-DGTZ residuals  resid=distance residuals pi0=Pi-zero plots     
      c = mc.getCanvas("MC"); c.divide(3,3); 
      
	  ii=0;
	  
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(0) ; h.setTitleX("PCAL Cluster X - GEMC X (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(1) ; h.setTitleX("PCAL Cluster Y - GEMC Y (cm)"); h.setFillColor(2);
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      c.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(2) ; h.setTitleX("PCAL Cluster Z - GEMC Z (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
	  
      for(ilm=0; ilm<3; ilm++) {
          c.cd(ii); 
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(0) ; h.setTitleX("25-"+dtab[ilm]+"mcThet"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
          c.cd(ii+3); ii++;
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(1) ; h.setTitleX(dtab[ilm]+"Theta-mcThet"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); c.draw(h);
      }
      
      c.repaint();
      
      c = mc.getCanvas("Resid"); c.divide(2,2); 
      String lab[] = {"Photon 1 - ECin (cm)","Photon 1 - ECout (cm)","Photon 2 - ECin (cm)","Photon 2 - ECout (cm)"};
      for(ii=0; ii<4; ii++){
          c.cd(ii); c.getPad(ii).getAxisX().setRange(-2.,40.);
          h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,9,1).sliceY(ii) ; h.setTitleX(lab[ii]); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); c.draw(h);
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
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.getPad(ii).getAxisX().setRange(0.,200.); c.draw(h); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,2) ; h.setTitleX("X:(E1-E2)/(E1+E2)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;
      
      h2f = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,7,2) ; 
      h2f.setTitleX("Two Photon Opening Angle (deg)"); h2f.setTitleY("E1*E2 (GeV^2)");      
      c.cd(ii); c.getPad(ii).getAxisZ().setLog(true);
      c.getPad(ii).getAxisZ().setAutoScale(true);
      if(app.isSingleEvent()) c.getPad(ii).getAxisZ().setRange(0.,3.2);
      c.draw(h2f);
      F1D f1 = new F1D("E1*E2/2/(1-COS(x))","0.13495*0.13495/2/(1-cos(x*3.14159/180.))",7.,20.); f1.setLineColor(1); f1.setLineWidth(2);
      F1D f2 = new F1D("E1*E2/2/(1-COS(x))","0.12495*0.12495/2/(1-cos(x*3.14159/180.))",7.,20.); f2.setLineColor(5); f2.setLineWidth(1);
      F1D f3 = new F1D("E1*E2/2/(1-COS(x))","0.14495*0.14495/2/(1-cos(x*3.14159/180.))",7.,20.); f3.setLineColor(5); f3.setLineWidth(2);
      c.draw(f1,"same"); c.draw(f2,"same"); c.draw(f3,"same"); ii++;
 
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,0) ; h.setTitleX("Pizero Energy Error (MeV)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;
      
      h = ecPix[0].strips.hmap1.get("H1_a_Hist").get(is,4,1) ; h.setTitleX("Pizero Theta Error (deg)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); 
      c.cd(ii); c.draw(h); ii++;
      
      
      c.repaint();
      
   }
   
}
