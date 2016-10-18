package org.clas.fcmon.cc;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;

public class CCOccupancyApp extends FCApplication {
    
    public CCOccupancyApp(String name, CCPixels ccPix) {
        super(name,ccPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        EmbeddedCanvas c = this.getCanvas(this.getName()); 
        this.getDetIndices(dd); 
        int lr = layer;
    
        int col0=0,col1=4,col2=2;
        
        H1F h1;  
        String otab[]={" Left "," Right "};
        String lab4[]={" ADC"," TDC"};      
        String xlab,ylab;
        
        c.divide(2,3);
        c.setAxisFontSize(14);
        c.setStatBoxFontSize(12);
        
        double nstr = ccPix.cc_nstr[0];
        
        H2F h2a = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,1,0); h2a.setTitleY("Sector "+is+otab[0]+"PMTs") ; h2a.setTitleX("Sector "+is+otab[0]+"PMT"+lab4[0]);
        H2F h2b = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,2,0); h2b.setTitleY("Sector "+is+otab[1]+"PMTs") ; h2b.setTitleX("Sector "+is+otab[1]+"PMT"+lab4[0]);
        canvasConfig(c,0,0.,2000.,1.,nstr+1,true); c.draw(h2a);
        canvasConfig(c,1,0.,2000.,1.,nstr+1,true); c.draw(h2b);
        
        c.cd(lr-1);
        
        F1D f1 = new F1D("p0","[a]",0.,2000.); f1.setParameter(0,ic+1);
        F1D f2 = new F1D("p0","[a]",0.,2000.); f2.setParameter(0,ic+2);
        f1.setLineColor(2); c.draw(f1,"same"); 
        f2.setLineColor(2); c.draw(f2,"same");
        
        for(int il=1;il<3;il++){
            xlab = "Sector "+is+otab[il-1]+"PMTs";
            c.cd(il+1); h1 = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,0).projectionY(); h1.setTitleX(xlab); h1.setFillColor(col0); c.draw(h1);
            }   
        
        c.cd(lr+1); h1 = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,lr,0).projectionY(); h1.setFillColor(col1); c.draw(h1,"same");
        H1F copy = h1.histClone("Copy"); copy.reset() ; 
        copy.setBinContent(ic, h1.getBinContent(ic)); copy.setFillColor(col2); c.draw(copy,"same");
        
        for(int il=1;il<3;il++) {
            String alab = "Sector "+is+otab[il-1]+"PMT "+11+lab4[0]; String tlab = otab[il-1]+(ic+1)+lab4[1];
            if(lr!=il) {c.cd(il+3); h1 = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,0).sliceY(11); h1.setTitleX(alab); h1.setTitle(""); h1.setFillColor(col0); c.draw(h1,"S");}
            //if(lr!=il) {canvas.cd(il+3); h = H2_CCt_Hist.get(is+1,il,0).sliceY(22); h.setXTitle(tlab); h.setTitle(""); h.setFillColor(col0); canvas.draw(h);}
        }
        String alab = "Sector "+is+otab[lr-1]+"PMT "+(ic+1)+lab4[0]; String tlab = otab[lr-1]+(ic+1)+lab4[1];
        c.cd(lr+3); h1 = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,lr,0).sliceY(ic);h1.setTitleX(alab); h1.setTitle(""); h1.setFillColor(col2); c.draw(h1,"S");
        //canvas.cd(lr+3); h = H2_CCt_Hist.get(is+1,lr,0).sliceY(ip+1);h.setXTitle(tlab); h.setTitle(""); h.setFillColor(col2); canvas.draw(h);   
        
        c.repaint();
    }


}
