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

public class ECTdcApp extends FCApplication {

    DetectorCollection<H2F> dc2a = null;
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    
    String otab[][]={{" U PMT "," V PMT "," W PMT "},
            {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
            {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
    
    int ics[][] = new int[3][10];
    int is,la,ic,ilm ;
    double tmin=450,tmax=850;
    
    public ECTdcApp(String name, ECPixels[] ecPix) {
        super(name,ecPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        this.is  = dd.getSector();
        this.la  = dd.getLayer();
        this.ic  = dd.getComponent();   
        this.ilm = ilmap;

        this.dc2a = ecPix[ilm].strips.hmap2.get("H2_t_Hist");        

        if(la<4) stripCanvas();
        if(la>3) pixCanvas();
    }
    
    public void stripCanvas() {
       
        F1D f1 = new F1D("p0","[a]",tmin,tmax); 
        F1D f2 = new F1D("p0","[a]",tmin,tmax); 
        f1.setParameter(0,ic+1); f1.setLineColor(2);
        f2.setParameter(0,ic+2); f2.setLineColor(2);

        H1F copy1=null;
        H1F h1; H2F h2;
          
        c.divide(3,3);       
        c.setAxisFontSize(14);
        
        for (int il=1; il<4; il++) {
            h2 = dc2a.get(is,il,0); h2.setTitleY("Sector "+is+otab[ilm][il-1]) ; h2.setTitleX("TDC");
            canvasConfig(c,il-1,tmin,tmax,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2);
        }
       
        c.cd(la-1); c.draw(f1,"same"); c.draw(f2,"same");
        
        for (int il=1; il<4; il++) {
            h1 = dc2a.get(is,il,0).projectionY(); h1.setTitleX("Sector "+is+otab[ilm][il-1]); h1.setFillColor(0); 
            if (la==il) {h1.setFillColor(4); copy1=h1.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1.getBinContent(ic)); copy1.setFillColor(2);}
            c.cd(il+2); h1.setOptStat(Integer.parseInt("1000000")); c.draw(h1);  if (il==la) c.draw(copy1,"same");            
            h1 = dc2a.get(is,il,0).sliceY(ics[ilm][il-1]); h1.setTitleX("Sector "+is+otab[ilm][il-1]+(ics[ilm][il-1]+1)+" TDC"); h1.setFillColor(0);  
            c.cd(il+5); h1.setOptStat(Integer.parseInt("1000100")); h1.setTitle(""); c.draw(h1); 
            if (la==il) {h1=dc2a.get(is,il,0).sliceY(ic) ; h1.setOptStat(Integer.parseInt("1000100")); h1.setFillColor(2); 
            h1.setTitleX("Sector "+is+otab[ilm][il-1]+(ic+1)+" TDC"); c.draw(h1);}
        }           
        c.repaint();        
        ics[ilm][la-1]=ic;
        
    }
    
    public void pixCanvas() {

        H1F h1 = new H1F(); h1.setFillColor(0);  
        H2F h2 = new H2F();

        c.divide(3,3);
        c.setAxisFontSize(14);
        c.setStatBoxFontSize(12);
                  
        for (int il=1; il<4; il++) {
            h2 = dc2a.get(is,il,1); h2.setTitleY("Sector "+is+otab[ilm][il-1]) ; h2.setTitleX("TDC");
            canvasConfig(c,il-1,tmin,tmax,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2);
            int strip = ecPix[ilm].pixels.getStrip(il,ic+1);
            F1D f1 = new F1D("p0","[a]",tmin,tmax); f1.setLineColor(2); f1.setParameter(0,strip);
            F1D f2 = new F1D("p0","[a]",tmin,tmax); f2.setLineColor(2); f2.setParameter(0,strip+1);
            c.draw(f1,"same");
            c.draw(f2,"same");  
            h1 = dc2a.get(is,il,1).sliceY(strip-1); h1.setTitleX("Sector "+is+otab[ilm][il-1]+strip+" TDC");  
            c.cd(il+2); h1.setOptStat(Integer.parseInt("1000100")); h1.setTitle(""); c.draw(h1); 
            h1 = dc2a.get(is,il,2).sliceY(ic); h1.setTitleX("Sector "+is+otab[ilm][il-1]+strip+" Pixel "+(ic+1)+" TDC");
            c.cd(il+5); h1.setOptStat(Integer.parseInt("1000100")); h1.setTitle(""); c.draw(h1);
        }       
        c.repaint();        
    }
    

}
