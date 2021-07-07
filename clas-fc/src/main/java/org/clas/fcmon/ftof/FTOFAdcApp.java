package org.clas.fcmon.ftof;

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


public class FTOFAdcApp extends FCApplication {

    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    F1D f1,f2;
    int ics;
    
    public FTOFAdcApp(String name, FTOFPixels[] ftofPix) {
        super(name,ftofPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd); 
        int lr = layer;
        lr = dd.getOrder()+1; 
        
        int col0=0,col1=4,col2=2;
        
        H1F h1a,h1b,h1c,copy1=null, copy2=null;
          
        c.divide(3,4);       
        c.setAxisFontSize(14);
        
//        canvas.setAxisTitleFontSize(14);
//        canvas.setTitleFontSize(14);
//        canvas.setStatBoxFontSize(12);
        
        int ilm = ilmap;
        double nstr = ftofPix[ilm].nstr;
        
        H2F h2a = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,1,0); h2a.setTitleY("Sector "+is+" Left PMT")  ; h2a.setTitleX("Left PMT ADC");
        H2F h2b = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,2,0); h2b.setTitleY("Sector "+is+" Right PMT") ; h2b.setTitleX("Right PMT ADC");
        H2F h23 = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,3,0); h23.setTitleY("Sector "+is+" Left PMT")  ; h23.setTitleX("Left PMT ADC");
        H2F h24 = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,4,0); h24.setTitleY("Sector "+is+" Right PMT") ; h24.setTitleX("Right PMT ADC");
        H2F h2c = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0); h2c.setTitleY("Sector "+is+" PADDLE")    ; h2c.setTitleX("GMEAN");
        canvasConfig(c,0,0.,ftofPix[0].amax[3],  1.,nstr+1.,true).draw(h23);
        canvasConfig(c,1,0.,ftofPix[0].amax[3],  1.,nstr+1.,true).draw(h24);
        canvasConfig(c,2,0.,ftofPix[0].amax[ilm],1.,nstr+1.,true).draw(h2c);
        canvasConfig(c,3,0.,ftofPix[0].amax[ilm],1.,nstr+1.,true).draw(h2a);
        canvasConfig(c,4,0.,ftofPix[0].amax[ilm],1.,nstr+1.,true).draw(h2b);
        canvasConfig(c,5,0.,ftofPix[0].amax[ilm],1.,nstr+1.,true).draw(h2c);
       
        f1 = new F1D("p0","[a]",0.,ftofPix[0].amax[3]); f1.setParameter(0,ic+1); f1.setLineColor(2);
        f2 = new F1D("p0","[a]",0.,ftofPix[0].amax[3]); f2.setParameter(0,ic+2); f2.setLineColor(2);       
        c.cd(lr-1); c.draw(f1,"same"); c.draw(f2,"same");
        
        f1 = new F1D("p0","[a]",0.,ftofPix[0].amax[ilm]); f1.setParameter(0,ic+1); f1.setLineColor(2);
        f2 = new F1D("p0","[a]",0.,ftofPix[0].amax[ilm]); f2.setParameter(0,ic+2); f2.setLineColor(2);       
        c.cd(lr-1+3); c.draw(f1,"same"); c.draw(f2,"same");
        
        f1 = new F1D("p0","[a]",0.,ftofPix[0].amax[ilm]); f1.setParameter(0,ic+1); f1.setLineColor(2);
        f2 = new F1D("p0","[a]",0.,ftofPix[0].amax[ilm]); f2.setParameter(0,ic+2); f2.setLineColor(2);
        c.cd(5);  c.draw(f1,"same"); c.draw(f2,"same");   
      
        h1a = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,1,0).projectionY(); h1a.setTitleX("Sector "+is+" Left PMT");   h1a.setFillColor(col0);  
        h1b = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,2,0).projectionY(); h1b.setTitleX("Sector "+is+" Right PMT" ); h1b.setFillColor(col0);  
        h1c = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).projectionY(); h1c.setTitleX("Sector "+is+" PADDLE" );    h1c.setFillColor(col1);  

        if (lr==1) {h1a.setFillColor(col1); copy1=h1a.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1a.getBinContent(ic)); copy1.setFillColor(col2);}
        if (lr==2) {h1b.setFillColor(col1); copy1=h1b.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1b.getBinContent(ic)); copy1.setFillColor(col2);}
                                            copy2=h1c.histClone("Copy"); copy2.reset(); copy2.setBinContent(ic, h1c.getBinContent(ic)); copy2.setFillColor(col2);
     
        c.cd(6); h1a.setOptStat(Integer.parseInt("1000000")); c.draw(h1a);  if (lr==1) c.draw(copy1,"same");
        c.cd(7); h1b.setOptStat(Integer.parseInt("1000000")); c.draw(h1b);  if (lr==2) c.draw(copy1,"same");
        c.cd(8); h1c.setOptStat(Integer.parseInt("1000000")); c.draw(h1c);             c.draw(copy2,"same");
        
        h1a = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,1,0).sliceY(ic); h1a.setTitleX("Left PMT "+(ic+1)+" ADC");   h1a.setFillColor(col0);  
        h1b = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,2,0).sliceY(ic); h1b.setTitleX("Right PMT "+(ic+1)+" ADC" ); h1b.setFillColor(col0);  
        h1c = ftofPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(ic); h1c.setTitleX("GMEAN PADDLE "+(ic+1));      h1c.setFillColor(col2);  
        
        if (lr==1) h1a.setFillColor(col2);
        if (lr==2) h1b.setFillColor(col2);
        c.cd(9);  h1a.setOptStat(Integer.parseInt("1000100")); h1a.setTitle(""); c.draw(h1a);  
        c.cd(10); h1b.setOptStat(Integer.parseInt("1000100")); h1b.setTitle(""); c.draw(h1b); 
        c.cd(11); h1c.setOptStat(Integer.parseInt("1000100")); h1c.setTitle(""); c.draw(h1c);      
       
        c.repaint();
        
        ics=ic;
        
    }



}