
package org.clas.fcmon.ec;
 
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
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
    
    DetectorCollection<H2F> dc2a = null;
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    
    String otab[][]={{" U PMT "," V PMT "," W PMT "},
            {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
            {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};    
    
   int ics[] = {1,1,1};
   int la,ilm ;
	
   public ECPedestalApp(String name, ECPixels[] ecPix) {
      super(name, ecPix);		
   }
   
   public void updateCanvas(DetectorDescriptor dd) {
	   
       this.ilm = ilmap;
       this.getDetIndices(dd);
       this.la = lay;

       this.dc2a = ecPix[ilm].strips.hmap2.get("H2_Peds_Hist");        

       if (app.isMC) return;
       
       if(la<4) stripCanvas();
       if(la>3) pixCanvas();
   }
   
   public void stripCanvas() {

       F1D f1 = new F1D("p0","[a]",-10.,10.); 
       F1D f2 = new F1D("p0","[a]",-10.,10.); 
       f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(2);
       f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(2);       
       
       H1F h1;  H2F h2,h2b;    
		      
       c.divide(3,3);
       c.setAxisFontSize(14);
     
      for(int il=1;il<4;il++){
         h2 = dc2a.get(is,il,0); h2.setTitleY("Sector "+is+otab[ilm][il-1]); h2.setTitleX("PED (Ref-Measured)") ;       
         canvasConfig(c,il-1,-10.,10.,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2); 		
         if(la==il) {c.draw(f1,"same"); c.draw(f2,"same");}     
         if(dc2a.hasEntry(is, il, 1)) {
            h2b = dc2a.get(is,il,1); h2b.setTitleY("Sector "+is+otab[ilm][il-1]); h2b.setTitleX("RMS") ;       
            canvasConfig(c,il+2,0.,10.,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2b); 		
            if(la==il) {c.draw(f1,"same"); c.draw(f2,"same");}  
         }
         c.cd(il+5); 
         h1=h2.sliceY(ics[il-1]); h1.setOptStat(Integer.parseInt("1001100")); h1.setFillColor(4); h1.setTitle("") ;
         h1.setTitleX("Sector "+is+otab[ilm][il-1]+(ics[il-1]+1)); c.draw(h1);
         if(la==il) {h1=h2.sliceY(ic); h1.setOptStat(Integer.parseInt("1001100")); h1.setFillColor(2); h1.setTitle("") ;
         h1.setTitleX("Sector "+is+otab[ilm][il-1]+(ic+1)) ; c.draw(h1);}
      }
      
      c.repaint();
      ics[la-1] = ic;
      
   }	
   
   public void pixCanvas() {

       H1F h1 = new H1F(); h1.setFillColor(0);  
       H2F h2 = new H2F();

       c.divide(3,2);
       c.setAxisFontSize(14);
       c.setStatBoxFontSize(12);
                         
       for (int il=1; il<4; il++) {
           h2 = dc2a.get(is,il,0); h2.setTitleY("Sector "+is+otab[ilm][il-1]) ; h2.setTitleX("PED (Ref-Measured)");
           canvasConfig(c,il-1,-10.,10.,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2);
           int strip = ecPix[ilm].pixels.getStrip(il,ic+1);
           F1D f1 = new F1D("p0","[a]",-10.,10.); f1.setLineColor(2); f1.setLineWidth(1); f1.setParameter(0,strip);
           F1D f2 = new F1D("p0","[a]",-10.,10.); f2.setLineColor(2); f2.setLineWidth(1); f2.setParameter(0,strip+1);
           c.draw(f1,"same");
           c.draw(f2,"same");  
           
           c.cd(il+2); 
           h1=h2.sliceY(strip-1); h1.setOptStat(Integer.parseInt("1001100")); h1.setFillColor(2); h1.setTitle("") ;
           h1.setTitleX("Sector "+is+otab[ilm][il-1]+strip); c.draw(h1);         
       }       
       c.repaint();        
   }
}
