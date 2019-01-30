package org.clas.fcmon.ec;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class ECAdcApp extends FCApplication {

    DetectorCollection<H2F> dc2a = null;
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    
    String otab[][]={{" U PMT "," V PMT "," W PMT "},
            {" U Inner PMT "," V Inner PMT "," W Inner PMT "},
            {" U Outer PMT "," V Outer PMT "," W Outer PMT "}};
    
    int ics[][] = new int[3][10];
    int la,ilm ;
    double amax=ecPix[ilm].amax[0];
    
    public ECAdcApp(String name, ECPixels[] ecPix) {
        super(name,ecPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
   
        this.ilm = ilmap;
        this.getDetIndices(dd);
        this.la = lay;

        this.dc2a = ecPix[ilm].strips.hmap2.get("H2_a_Hist");        

        if(la<4) stripCanvas();
        if(la>3) pixCanvas();
    }
    
    public void stripCanvas() {
       
        F1D f1 = new F1D("p0","[a]",0.,amax); 
        F1D f2 = new F1D("p0","[a]",0.,amax); 
        f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(2);
        f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(2); 

        H1F copy1=null;
        H1F h1; H2F h2;
       
        c.divide(3,4);       
        c.setAxisFontSize(14);
        
        for (int il=1; il<4; il++) {
            h2 = dc2a.get(is,il,3); h2.setTitleY("Sector "+is+otab[ilm][il-1]) ; h2.setTitleX("ADC");
            canvasConfig(c,il-1,0.,40.,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2);
            if (la==il) {c.draw(f1,"same"); c.draw(f2,"same");}
            h2 = dc2a.get(is,il,0); h2.setTitleY("Sector "+is+otab[ilm][il-1]) ; h2.setTitleX("ADC");
            canvasConfig(c,il+2,0.,amax,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2); 
            if (la==il) {c.draw(f1,"same"); c.draw(f2,"same");}
            h1 = dc2a.get(is,il,0).projectionY(); h1.setTitleX("Sector "+is+otab[ilm][il-1]); h1.setFillColor(0); 
            if (la==il) {h1.setFillColor(4); copy1=h1.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1.getBinContent(ic)); copy1.setFillColor(2);}
            c.cd(il+5); h1.setOptStat(Integer.parseInt("1000000")); c.draw(h1);  if (il==la) c.draw(copy1,"same");            
//            if(app.isIPC) {h1.setUniqueID(160000+is*1000+ilm*100+il*10+5); app.ipc.sendData(h1);}
            h1 = dc2a.get(is,il,0).sliceY(ics[ilm][il-1]); h1.setTitleX("Sector "+is+otab[ilm][il-1]+(ics[ilm][il-1]+1)+" ADC"); h1.setFillColor(0);  
            c.cd(il+8); h1.setOptStat(Integer.parseInt("1000100")); h1.setTitle(""); c.draw(h1);
            if (la==il) {h1=dc2a.get(is,il,0).sliceY(ic) ; h1.setOptStat(Integer.parseInt("1000100")); h1.setFillColor(2); 
            h1.setTitleX("Sector "+is+otab[ilm][il-1]+(ic+1)+" ADC"); c.draw(h1);}
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
            h2 = dc2a.get(is,il,1); h2.setTitleY("Sector "+is+otab[ilm][il-1]) ; h2.setTitleX("ADC");
            canvasConfig(c,il-1,0.,amax,1.,ecPix[ilm].ec_nstr[il-1]+1.,true).draw(h2);
            int strip = ecPix[ilm].pixels.getStrip(il,ic+1);
            F1D f1 = new F1D("p0","[a]",0.,amax); f1.setLineColor(2); f1.setLineWidth(1); f1.setParameter(0,strip);
            F1D f2 = new F1D("p0","[a]",0.,amax); f2.setLineColor(2); f2.setLineWidth(1); f2.setParameter(0,strip+1);
            c.draw(f1,"same");
            c.draw(f2,"same");  
            h1 = dc2a.get(is,il,1).sliceY(strip-1); h1.setTitleX("Sector "+is+otab[ilm][il-1]+strip+" ADC");  
            if (la>10&&la-10==il) h1.setFillColor(2);
            c.cd(il+2); c.getPad(il+2).getAxisY().setAutoScale(true);
            h1.setOptStat(Integer.parseInt("1000000")); h1.setTitle(""); c.draw(h1); 
            h1 = dc2a.get(is,il,2).sliceY(ic); h1.setTitleX("Sector "+is+otab[ilm][il-1]+strip+" Pixel "+(ic+1)+" ADC");
            if (la>10&&la-10==il) h1.setFillColor(2);
            c.cd(il+5); h1.setOptStat(Integer.parseInt("1000100")); h1.setTitle(""); c.draw(h1);
        }       
        c.repaint();        
    }
        
}
