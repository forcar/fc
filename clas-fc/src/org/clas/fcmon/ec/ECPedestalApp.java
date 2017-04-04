
package org.clas.fcmon.ec;
 
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;

//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
//import org.root.histogram.H2D;

//groot
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class ECPedestalApp extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    int ics[] = {1,1,1};
	
   public ECPedestalApp(String name, ECPixels[] ecPix) {
      super(name, ecPix);		
   }

   public void updateCanvas(DetectorDescriptor dd) {

      int  is = dd.getSector();
      int  la = dd.getLayer();
      int  ic = dd.getComponent();   
      int ilm = ilmap;
      
      if (la>3) return;
		
      if (app.isMC) return;
		
      H1F h;      
      String otab[][]={{" U PMT "," V PMT "," W PMT "},
                       {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
                       {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
		      
      c.divide(3,2);
      c.setAxisFontSize(14);
     
      for(int il=0;il<3;il++){
         H2F hpix = ecPix[ilm].strips.hmap2.get("H2_Peds_Hist").get(is,il+1,0);
         hpix.setTitleX("PED (Ref-Measured)") ; hpix.setTitleY(otab[ilm][il]);        
         c.cd(il); c.getPad(il).getAxisZ().setLog(true); c.draw(hpix);   		
         if(la==il+1) {
            F1D f1 = new F1D("p0","[a]",-10.,10.); f1.setParameter(0,ic+1);
            F1D f2 = new F1D("p0","[a]",-10.,10.); f2.setParameter(0,ic+2);
            f1.setLineColor(2); c.draw(f1,"same"); 
            f2.setLineColor(2); c.draw(f2,"same");
         }
         
         c.cd(il+3); 
         h=hpix.sliceY(ics[il]); h.setOptStat(Integer.parseInt("1001100")); h.setFillColor(4); h.setTitle("") ;
         h.setTitleX("Sector "+is+otab[ilm][il]+(ics[il]+1)); c.draw(h);
         if(la==il+1) {h=hpix.sliceY(ic); h.setOptStat(Integer.parseInt("1001100")); h.setFillColor(2); h.setTitle("") ;
         h.setTitleX("Sector "+is+otab[ilm][il]+(ic+1)) ; c.draw(h);}
      }
      
      c.repaint();
      ics[la-1] = ic;
      
   }	
}
