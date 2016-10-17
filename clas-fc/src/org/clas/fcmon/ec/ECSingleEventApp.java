package org.clas.fcmon.ec;

import java.awt.BorderLayout;

import javax.swing.JPanel;
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

    JPanel                     engineView = new JPanel();
    EmbeddedCanvas                      l = new EmbeddedCanvas();
    EmbeddedCanvas                     ru = new EmbeddedCanvas();  
    EmbeddedCanvas                    mc1 = new EmbeddedCanvas();  
    EmbeddedCanvas                  resid = new EmbeddedCanvas();  
    EmbeddedCanvas                    pi0 = new EmbeddedCanvas();  
    JTabbedPane                canvasView = new JTabbedPane();

   public ECSingleEventApp(String name, ECPixels[] ecPix) {
      super(name,ecPix);		
   }
   
   public JPanel getCalibPane() {        
       engineView.setLayout(new BorderLayout());
       JSplitPane   hPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
       JSplitPane   vPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
       vPane.setTopComponent(ru);
       vPane.setBottomComponent(canvasView);
       hPane.setLeftComponent(l);
       hPane.setRightComponent(vPane);
       hPane.setResizeWeight(0.5);
       vPane.setResizeWeight(0.5);
       engineView.add(hPane);
       canvasView.addTab("MC1", mc1);
       canvasView.addTab("Resid", resid);
       canvasView.addTab("PI0", pi0);
       return engineView;       
   }  
   
   public void addEvent(DataEvent event) {
       
   }
   
   public void fillHistos() {
       
   }
   
   public void updateCanvas(DetectorDescriptor dd) {
	       
      H1F h,h1,h2;
      
      String otab[]={"U ","V ","W "};             
      String dtab[]={"PCAL ","EC Inner ","EC Outer "};
      
      int ilm = ilmap;
        
      double   zmax = (double) mon.getGlob().get("PCMon_zmax");
      String config = (String) mon.getGlob().get("config");
      DetectorCollection<H1F> ecEngHist = (DetectorCollection<H1F>) mon.getGlob().get("ecEng");
      
      this.getDetIndices(dd);
		
      l.divide(3,6); ru.divide(3,3); mc1.divide(3,3); pi0.divide(1,1);
      l.setAxisFontSize(14);
      l.setStatBoxFontSize(12);
      
      int ii=0;
      int jj=0;
      int kk=0;
      
      // Single event strip hit plots for PCAL, ECinner, ECouter
      
	  for(ilm=0; ilm<3; ilm++) {
      for(int il=1;il<4;il++) {
         F1D f1 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f1.setParameter(0,0.1*ecPix[ilm].getStripThr(config,il));
         f1.setLineColor(4);
         F1D f2 = new F1D("p0","[a]",0.,ecPix[ilm].ec_nstr[il-1]+1); 
         f2.setParameter(0,0.1*ecPix[ilm].getPeakThr(config,il));
         f2.setLineColor(2);
         l.cd(ii); 
         l.getPad(ii).getAxisX().setRange(0.,ecPix[ilm].ec_nstr[il-1]+1);
         l.getPad(ii).getAxisY().setRange(0.,1.2*zmax*app.displayControl.pixMax);
         h1 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,1); h1.setFillColor(0);
         h2 = ecPix[ilm].strips.hmap1.get("H1_Stra_Sevd").get(is,il,0); h2.setFillColor(4); 
         h1.setTitleX(dtab[ilm]+otab[il-1]+"PMT"); h1.setTitleY(" ");
         if (il==1) h1.setTitleY("Strip Energy (MeV)"); 
         l.draw(h1);
         l.draw(h2,"same"); 
         l.draw(f1,"same");
         l.draw(f2,"same");
         ii++;
      }
	  }
	  
	  double xmx1=40.,xmx2=100.;
	  switch (config) {
	  case "muon": xmx1=40. ; xmx2=100.; break;
	  case "phot": xmx1=200.; xmx2=500.; break;
	  case "elec": xmx1=100.; xmx2=400.;
	  }

	  // Peak energy plots for PCAL, ECinner, ECouter
	  
	  for(ilm=0; ilm<3; ilm++) {
      for(int il=1;il<4; il++) {
          l.cd(ii); 
          l.getPad(ii).getAxisX().setRange(0.,xmx1);
          h  = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY(il-1) ;  h.setFillColor(0);
          h1 = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,5,0).sliceY(il-1) ; h1.setFillColor(34);
          h2 = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,6,0).sliceY(il-1) ; h2.setFillColor(32);
          h.setTitleX(dtab[ilm]+otab[il-1]+"Peak Energy (MeV)");          
          l.draw(h); l.draw(h1,"same"); l.draw(h2,"same");
          ii++;
      }
	  }
      
	  // Cluster error PCAL, ECinner, ECouter
	  ii=0;
	  
	  for(ilm=0; ilm<3; ilm++) {
          ru.cd(ii); ru.getPad(ii).getAxisX().setRange(-100.,100.);
          h=ecEngHist.get(is,ilm+1,0); ; h.setTitleX(dtab[ilm]+"Cluster Error (cm)"); h.setFillColor(0);
          h.setOptStat(Integer.parseInt("1100")); ru.draw(h);
          h=ecEngHist.get(is,ilm+1,1); ; h.setFillColor(2); ru.draw(h,"same");
	      ii++;
	  }
	  
      // Cluster energy PCAL, ECinner, ECouter
	  for(ilm=0; ilm<3; ilm++) {
          ru.cd(ii); ru.getPad(ii).getAxisX().setRange(0.,xmx2);
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)3) ; h.setTitleX(dtab[ilm]+"Cluster Energy (MeV)"); h.setFillColor(2);          
          h.setOptStat(Integer.parseInt("1100")); ru.draw(h);
          ii++;   
	  }
	  
	  // Cluster total energy
	  ru.cd(ii);  ru.getPad(ii).getAxisX().setRange(0.,xmx2*2); 
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)4) ; h.setTitleX("Total Cluster Energy (MeV)"); h.setFillColor(2);          
      h.setOptStat(Integer.parseInt("1100")); ru.draw(h);
      
      // JTabbedPane plots: mc1=TRUE-DGTZ residuals  resid=distance residuals pi0=Pi-zero plots     
	  ii=0;
	  
      mc1.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(0) ; h.setTitleX("PCAL Cluster X - GEMC X (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); mc1.draw(h);
      mc1.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(1) ; h.setTitleX("PCAL Cluster Y - GEMC Y (cm)"); h.setFillColor(2);
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); mc1.draw(h);
      mc1.cd(ii); ii++; h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,8,0).sliceY(2) ; h.setTitleX("PCAL Cluster Z - GEMC Z (cm)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); mc1.draw(h);
	  
      for(ilm=0; ilm<3; ilm++) {
          mc1.cd(ii); 
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(0) ; h.setTitleX("25-"+dtab[ilm]+"mcThet"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); mc1.draw(h);
          mc1.cd(ii+3);ii++;
          h = ecPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,9,0).sliceY(1) ; h.setTitleX(dtab[ilm]+"Theta-mcThet"); h.setFillColor(2); 
          h.setOptStat(Integer.parseInt("1100")); h.setTitle(" "); mc1.draw(h);
      }
      
      ii=0;
      pi0.cd(ii); pi0.getPad(ii).getAxisX().setRange(0.,200.);
      h = ecPix[0].strips.hmap2.get("H2_a_Hist").get(is,4,0).sliceY((int)5) ; h.setTitleX("Pizero Invariant Mass (MeV)"); h.setFillColor(2); 
      h.setOptStat(Integer.parseInt("1100")); pi0.draw(h);
            
      l.repaint(); ru.repaint(); mc1.repaint(); pi0.repaint();
   }
   
}
