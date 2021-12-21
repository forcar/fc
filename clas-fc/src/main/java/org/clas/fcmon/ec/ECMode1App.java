package org.clas.fcmon.ec;


import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
//groot
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.Axis;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;


public class ECMode1App extends FCApplication  {
	
   JPanel              engineView = new JPanel();
   EmbeddedCanvasTabbed    mode1  = new EmbeddedCanvasTabbed("PMT");
   EmbeddedCanvas               c = this.getCanvas(this.getName()); 
   
   int is,la,ic,idet,nstr;
   int ics[][] = new int[3][10];
   double amax=ecPix[idet].amax[1], amax2=ecPix[idet].amax[2], tmax=ecPix[idet].tmax[1];
   
   String  det[] = {" PCAL "," ECIN "," ECOU "};
   String otab[][]={{" U PMT "," V PMT "," W PMT "},
           {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
           {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
   
   public ECMode1App(String name, ECPixels[] ecPix) {
      super(name,ecPix);	
   }
   
   public JPanel getPanel() {
       engineView.setLayout(new BorderLayout());
       engineView.add(getCanvasPane(),BorderLayout.CENTER);
       return engineView;
   }
   
   public EmbeddedCanvasTabbed getCanvasPane() {
       mode1.addCanvas("UVW");
       mode1.addCanvas("TDIF");
       mode1.addCanvas("AvsT");
       mode1.addCanvas("SYNC");  
       mode1.addCanvas("OVFL");  
       return mode1;
   }
   
   public void updateCanvas(DetectorDescriptor dd) {
		
      this.is = dd.getSector();
      this.la = dd.getLayer();
      this.ic = dd.getComponent();   
      this.idet = ilmap;	  
      
      if (la>3) return;
      
      this.nstr = ecPix[idet].ec_nstr[la-1];
      
      PCMon_zmin = 100;
      PCMon_zmax = 4000;        
      
      switch (mode1.selectedCanvas) {
      case  "PMT": updateEvent(); break;
      case "TDIF": updateTDIF();  break;
      case "AvsT": updateAvsT();  break;
      case  "UVW": updateUVW();   break;
      case "SYNC": updateSync();  break;
      case "OVFL": updateOVF();
      }
      
   }
      
   public void updateEvent() {   
   
      int min=0, max=nstr;
      c = mode1.getCanvas("PMT");
      
      switch (idet) {
      case 0: c.divide(4,6); max=24 ; if (ic>23) {min=24; max=48;} if (ic>47) {min=48; max=nstr;} break;
      case 1: c.divide(4,3); max=12 ; if (ic>11) {min=12; max=24;} if (ic>23) {min=24; max=nstr;} break;
      case 2: c.divide(4,3); max=12 ; if (ic>11) {min=12; max=24;} if (ic>23) {min=24; max=nstr;}   
      }   
      
      c.setAxisFontSize(14);
      
//      System.out.println("cr,sl,ch="+app.currentCrate+" "+app.currentSlot+" "+app.currentChan);
//      app.mode7Emulation.configMode7(app.currentCrate, app.currentSlot, app.currentChan);
//      app.mode7Emulation.updateGUI();
      
      int tet = app.mode7Emulation.tet;
      
      if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
      if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
      
      F1D f1 = new F1D("p0","[a]",0.,100.); f1.setParameter(0,tet);
      f1.setLineColor(2);
      F1D f2 = new F1D("p0","[a]",0.,100.); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
      f2.setLineColor(4); f2.setLineStyle(2);	
		
      H1F h ;
      
      c.clear();
      
      for(int ip=min;ip<max;ip++){
          c.cd(ip-min); 
          c.getPad(ip-min).setOptStat(Integer.parseInt("0"));
          c.getPad(ip-min).getAxisX().setRange(0.,100.);
          c.getPad(ip-min).getAxisY().setRange(-100.,4000*app.displayControl.pixMax);
          h = ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,la,0).sliceY(ip); 
          h.setTitleX("Sector "+is+otab[idet][la-1]+(ip+1)+"  (4 ns/ch)"); h.setTitleY("Counts");
          h.setFillColor(4); c.draw(h);
          h = ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,la,1).sliceY(ip); 
          h.setFillColor(2); c.draw(h,"same");
          c.draw(f1,"same"); c.draw(f2,"same");
          }  

      c.repaint();
   }
   
   public void updateUVW() {
       
      DetectorCollection<H2F> dc2a = ecPix[idet].strips.hmap2.get("H2_Mode1_Hist");        
      DetectorCollection<H2F> dc2t = ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd");        
      H1F h1; H2F h2;
     
      F1D f1 = new F1D("p0","[a]",0.,100.); 
      F1D f2 = new F1D("p0","[a]",0.,100.); 
      f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(0);
      f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(0);
             
      c = mode1.getCanvas("UVW");  c.clear(); c.divide(3,2);       
          
      for (int il=1; il<4 ; il++) {
//          h2 = H2F.divide(dc2a.get(is,il,1),dc2a.get(is,il,0)); 
          h2 = dc2a.get(is,il,0);
          h2.setTitleY("Sector "+is+otab[idet][il-1]) ; h2.setTitleX("SAMPLES (4 ns/ch)");         
          canvasConfig(c,il-1,0.,100.,1.,nstr+1.,true).draw(h2);          
          if (la==il) {c.draw(f1,"same"); c.draw(f2,"same");}          
          h1 = dc2a.get(is,il,0).sliceY(ic); h1.setOptStat(Integer.parseInt("1000100"));
          h1.setTitleX("Sector "+is+otab[idet][il-1]+(ic+1)+" (4 ns/ch)"); h1.setFillColor(0);
          c.cd(il+2); h1.setTitle(" "); c.draw(h1);
          if (la==il) {h1=dc2a.get(is,il,0).sliceY(ic) ; h1.setFillColor(2); h1.setOptStat(Integer.parseInt("1000100"));
          h1.setTitleX("Sector "+is+otab[idet][il-1]+(ic+1)+" (4 ns/ch)"); c.draw(h1);}
          if (app.isSingleEvent()) {h1=dc2t.get(is,il,2).sliceY(ic) ; h1.setFillColor(4); c.draw(h1,"same");}
         
      }
      
      c.repaint();
//      ics[idet][la-1]=ic;
      
   }
   
   public void updateTDIF() {
       
       DetectorCollection<H2F> dc2a = ecPix[idet].strips.hmap2.get("H2_Tdif_Hist");        
       
       H1F h1; H2F h2;
       
       F1D f1 = new F1D("p0","[a]",-30.,30.); 
       F1D f2 = new F1D("p0","[a]",-30.,30.); 
       f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(0);
       f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(0);
              
       c = mode1.getCanvas("TDIF");  c.clear(); c.divide(3,2);       
           
       for (int il=1; il<4 ; il++) {
           h2 = dc2a.get(is,il,0); h2.setTitleY("Sector "+is+otab[idet][il-1]) ; h2.setTitleX("TDC-FADC (NSEC)");
           canvasConfig(c,il-1,-30.,30.,1.,nstr+1.,true).draw(h2);
           if (la==il) {c.draw(f1,"same"); c.draw(f2,"same");}
           h1 = dc2a.get(is,il,0).sliceY(ic); h1.setOptStat(Integer.parseInt("1000100"));
           h1.setTitleX("Sector "+is+otab[idet][il-1]+(ic+1)+" (NSEC)"); h1.setFillColor(0);
           c.cd(il+2); h1.setTitle(" "); c.draw(h1);
           if (la==il) {
               h1=dc2a.get(is,il,0).sliceY(ic) ; h1.setFillColor(2);h1.setOptStat(Integer.parseInt("1000100"));
               h1.setTitleX("Sector "+is+otab[idet][il-1]+(ic+1)+" (NSEC)"); c.draw(h1);
           }            
       }
       
       c.repaint();
       
       ics[idet][la-1]=ic;
       
   } 
   
   public void updateAvsT() {
       
       DetectorCollection<H2F> dc2a = ecPix[idet].strips.hmap2.get("H2_a_Hist");       
               
       H2F h2;
       GraphErrors geff;
       
       c = mode1.getCanvas("AvsT");  c.clear(); c.divide(3,4);       
      
       for (int il=1; il<4; il++) {
           h2=dc2a.get(is,il,4); h2.setTitleY("Sector "+is+otab[idet][il-1]+" TDC") ; h2.setTitleX("Sector "+is+otab[idet][il-1]+" FADC");
           canvasConfig(c,il-1,0.,amax,0.,tmax,true).draw(h2);            
           h2=dc2a.get(is,il,5); h2.setTitleY("Sector "+is+otab[idet][il-1]+" TDC") ; h2.setTitleX("Sector "+is+otab[idet][il-1]+" FADC");
           canvasConfig(c,il-1+3,0.,amax2,0.,tmax,true).draw(h2);     
           geff = getEff(h2,dc2a.get(is,il,3)); geff.setTitleY("DSC Efficiency");   geff.setTitleX("Sector "+is+otab[idet][il-1]+" FADC");
           canvasConfig(c,il-1+6,0.,amax2,0,1.05,false).draw(geff); 
           h2=dc2a.get(is,il,3); h2.setTitleY("Sector "+is+otab[idet][il-1]) ;        h2.setTitleX("Sector "+is+otab[idet][il-1]+" FADC");
           canvasConfig(c,il-1+9,0.,amax2,0,0,true).draw(h2);     
       }
       
       c.repaint();
       
   }
   
   public H1F projectionX(H2F h2, float ymin, float ymax ) {
       String name = "X Projection";
       Axis xAxis = h2.getXAxis(), yAxis = h2.getYAxis();
       double xMin = xAxis.min();
       double xMax = xAxis.max();
       int xNum = xAxis.getNBins();
       H1F projX = new H1F(name, xNum, xMin, xMax);
       int ybinlo = yAxis.getBin(ymin), ybinhi = yAxis.getBin(ymax);
       double height = 0.0;
       for (int x = 0; x < xAxis.getNBins(); x++) {
           height = 0.0;
           for (int y = ybinlo; y < ybinhi; y++) {
               height += h2.getBinContent(x, y);
           }
           projX.setBinContent(x, height);
       }
       
       return projX;
   }
   
   public GraphErrors getEff(H2F h1, H2F h2) {	  	
	   GraphErrors  gout = H1F.divide(projectionX(h1,120,200), h2.projectionX()).getGraph();
	   gout.setMarkerColor(1); gout.setLineColor(2);
	   return gout;
   }
   
   public void updateSync() {
       
       DetectorCollection<H2F> dc2t = ecPix[1].strips.hmap2.get("H2_t_Hist"); 
       
       H2F h2;
       
       c = mode1.getCanvas("SYNC");  c.clear(); c.divide(3,4); 
       
       for (int is=1; is<7; is++) {
           h2 = dc2t.get(is,3,3) ;  h2.setTitleY("PHASE") ; h2.setTitleX("Sector "+is+" W Inner TDC-FADC (ns)");   
           canvasConfig(c,is-1,-40.,40.,0.,6.,true).draw(h2);
       }
       for (int is=1; is<7; is++) {
//           h2 = dc2t.get(is,3,4) ;  h2.setTitleY("PHASE") ; h2.setTitleX("Sector "+is+" W Inner TDC (ns)");   
//           canvasConfig(c,is-1+6,0.,tmax,0.,6.,true).draw(h2);
           H1F h1 = dc2t.get(is,3,4).projectionX() ;  h1.setTitleY("COUNTS") ; h1.setTitleX("Sector "+is+" W Inner TDC (ns)");   
           canvasConfig(c,is-1+6,0.,tmax,0.,0.,false).draw(h1);
       }
       
       c.repaint();   
   }
   
   public void updateOVF() {
	   
       DetectorCollection<H2F> dc2a = ecPix[idet].strips.hmap2.get("H2_PCa_Stat"); 
       
	   PCMon_zmin = 0.;
	   PCMon_zmax = 1.;        
       
       H2F hd1=null; H2F hd2=null; 
       int n=0;

       c = mode1.getCanvas("OVFL"); c.clear(); c.divide(1,2);
            
       H2F h2 = dc2a.get(is, 0, 2); H2F h0 = dc2a.get(is, 0, 0); H2F h5 = dc2a.get(is, 0, 5);
       hd1=hd1.divide(h2,h0); hd2=hd2.divide(h5,h2);  
       String tit = "Sector "+is+det[idet];
       hd1.setTitleY("UVW"); hd1.setTitleX("PMT"); hd2.setTitleY("UVW"); hd2.setTitleX("PMT");
       hd1.setTitle("Fraction of hits with FADC>4090 - "+tit); hd2.setTitle("FADC overflow multiplicity - "+tit);
       PCMon_zmin = 0.; PCMon_zmax = 0.2; canvasConfig(c,n++,0,0,0,0,false).draw(hd1);
       PCMon_zmin = 0.; PCMon_zmax = 5.0; canvasConfig(c,n++,0,0,0,0,false).draw(hd2);
             
       c.repaint();	       
   }   
   
}
