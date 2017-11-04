package org.clas.fcmon.ctof;

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

public class CTOFTdcApp extends FCApplication {

    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    int ics;
    
    public CTOFTdcApp(String name, CTOFPixels[] ctofPix) {
        super(name,ctofPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd); 
        int lr = dd.getOrder()+1;
    
        int col0=0,col1=4,col2=2;
        
        H1F h1a,h1b,h1c,copy1=null,copy2=null;  
         
        c.divide(3,3);
        c.setAxisFontSize(14);
        
//        canvas.setAxisTitleFontSize(14);
//        canvas.setTitleFontSize(14);
//        canvas.setStatBoxFontSize(12);
        
        int ilm = ilmap;
        double nstr = ctofPix[ilm].nstr;
        
        H2F h2a = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,1,0); h2a.setTitleY("Sector "+is+" UP PMT") ; h2a.setTitleX("UP PMT TDC");
        H2F h2b = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,2,0); h2b.setTitleY("Sector "+is+" DN PMT") ; h2b.setTitleX("DN PMT TDC");
        H2F h2c = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,0,0); h2c.setTitleY("Sector "+is+" PADDLE") ; h2c.setTitleX("TDIF");
        canvasConfig(c,0,450.,850.,1.,nstr+1.,true).draw(h2a);
        canvasConfig(c,1,450.,850.,1.,nstr+1.,true).draw(h2b);
        canvasConfig(c,2, -35.,  35.,1.,nstr+1.,true).draw(h2c);
        
        F1D f1 = new F1D("p0","[a]",450.,850.); f1.setParameter(0,ic+1);
        F1D f2 = new F1D("p0","[a]",450.,850.); f2.setParameter(0,ic+2);
        c.cd(lr-1);        
        f1.setLineColor(2); c.draw(f1,"same"); 
        f2.setLineColor(2); c.draw(f2,"same");
        f1 = new F1D("p0","[a]",-35.,35.); f1.setParameter(0,ic+1);
        f2 = new F1D("p0","[a]",-35.,35.); f2.setParameter(0,ic+2);
        c.cd(2);        
        f1.setLineColor(2); c.draw(f1,"same"); 
        f2.setLineColor(2); c.draw(f2,"same");
        
        h1a = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,1,0).projectionY(); h1a.setTitleX("Sector "+is+" UP PMT");   h1a.setFillColor(col0);  
        h1b = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,2,0).projectionY(); h1b.setTitleX("Sector "+is+" DN PMT" ); h1b.setFillColor(col0);  
        h1c = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,0,0).projectionY(); h1c.setTitleX("Sector "+is+" PADDLE" );    h1c.setFillColor(col1);  

        if (lr==1) {h1a.setFillColor(col1); copy1=h1a.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1a.getBinContent(ic)); copy1.setFillColor(col2);}
        if (lr==2) {h1b.setFillColor(col1); copy1=h1b.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1b.getBinContent(ic)); copy1.setFillColor(col2);}
                                            copy2=h1c.histClone("Copy"); copy2.reset(); copy2.setBinContent(ic, h1c.getBinContent(ic)); copy2.setFillColor(col2);

        c.cd(3); h1a.setOptStat(Integer.parseInt("1000000")); c.draw(h1a); if (lr==1) c.draw(copy1,"same");
        c.cd(4); h1b.setOptStat(Integer.parseInt("1000000")); c.draw(h1b); if (lr==2) c.draw(copy1,"same");
        c.cd(5); h1c.setOptStat(Integer.parseInt("1000000")); c.draw(h1c);            c.draw(copy2,"same");
        
        h1a = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,1,0).sliceY(ics); h1a.setTitleX("UP PMT "+(ic+1)+" TDC");   h1a.setFillColor(col0);  
        h1b = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,2,0).sliceY(ics); h1b.setTitleX("DN PMT "+(ic+1)+" TDC" ); h1b.setFillColor(col0);  
        h1c = ctofPix[ilm].strips.hmap2.get("H2_t_Hist").get(is,0,0).sliceY(ics); h1c.setTitleX("TDIF PADDLE "+(ic+1));       h1c.setFillColor(col2);  
        
        if (lr==1) h1a.setFillColor(col2);
        if (lr==2) h1b.setFillColor(col2);
        c.cd(6); h1a.setOptStat(Integer.parseInt("1000100")); h1a.setTitle(""); c.draw(h1a);  
        c.cd(7); h1b.setOptStat(Integer.parseInt("1000100")); h1b.setTitle(""); c.draw(h1b); 
        c.cd(8); h1c.setOptStat(Integer.parseInt("1000100")); h1c.setTitle(""); c.draw(h1c);      
        
        ics=ic;
        
        c.repaint();
        
    }


}
