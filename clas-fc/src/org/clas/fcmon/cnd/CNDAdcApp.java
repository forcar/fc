package org.clas.fcmon.cnd;

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


public class CNDAdcApp extends FCApplication {

    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    int ics;
    
    public CNDAdcApp(String name, CNDPixels[] cndPix) {
        super(name,cndPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        double amax=  cndPix[0].amax[0];
  
        this.getDetIndices(dd); 
        
        int lr = dd.getOrder()+1;
    
        int col0=0,col1=4,col2=2;
        
        H1F h1a,h1b,h1c,copy1=null, copy2=null;
          
        c.divide(3,3);       
        c.setAxisFontSize(14);
        
//        canvas.setAxisTitleFontSize(14);
//        canvas.setTitleFontSize(14);
//        canvas.setStatBoxFontSize(12);
        
        int ilm = ilmap;
        double nstr = cndPix[ilm].nstr;
        
        H2F h2a = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,1,0); h2a.setTitleY("SECTOR "+is+" LAYER") ; h2a.setTitleX("L PMT ADC");
        H2F h2b = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,2,0); h2b.setTitleY("SECTOR "+is+" LAYER") ; h2b.setTitleX("R PMT ADC");
        H2F h2c = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0); h2c.setTitleY("SECTOR "+is+" LAYER") ; h2c.setTitleX("GMEAN");
        canvasConfig(c,0,0., amax,1.,nstr+1.,true).draw(h2a);
        canvasConfig(c,1,0., amax,1.,nstr+1.,true).draw(h2b);
        canvasConfig(c,2,0., amax,1.,nstr+1.,true).draw(h2c);
       
        F1D f1 = new F1D("p0","[a]",0.,amax); f1.setParameter(0,ic+1);
        F1D f2 = new F1D("p0","[a]",0.,amax); f2.setParameter(0,ic+2);
        c.cd(lr-1);        
        f1.setLineColor(2); c.draw(f1,"same"); 
        f2.setLineColor(2); c.draw(f2,"same");
        f1 = new F1D("p0","[a]",0.,amax); f1.setParameter(0,ic+1);
        f2 = new F1D("p0","[a]",0.,amax); f2.setParameter(0,ic+2);
        c.cd(2);        
        f1.setLineColor(2); c.draw(f1,"same"); 
        f2.setLineColor(2); c.draw(f2,"same");
      
        h1a = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,1,0).projectionY(); h1a.setTitleX("SECTOR "+is+" L PMT LAYER");  h1a.setFillColor(col0);  
        h1b = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,2,0).projectionY(); h1b.setTitleX("SECTOR "+is+" R PMT LAYER" ); h1b.setFillColor(col0);  
        h1c = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).projectionY(); h1c.setTitleX("SECTOR "+is+" GMEAN LAYER" ); h1c.setFillColor(col1);  

        if (lr==1) {h1a.setFillColor(col1); copy1=h1a.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1a.getBinContent(ic)); copy1.setFillColor(col2);}
        if (lr==2) {h1b.setFillColor(col1); copy1=h1b.histClone("Copy"); copy1.reset(); copy1.setBinContent(ic, h1b.getBinContent(ic)); copy1.setFillColor(col2);}
                                            copy2=h1c.histClone("Copy"); copy2.reset(); copy2.setBinContent(ic, h1c.getBinContent(ic)); copy2.setFillColor(col2);
     
        c.cd(3); h1a.setOptStat(Integer.parseInt("1000000")); c.draw(h1a);  if (lr==1) c.draw(copy1,"same");
        c.cd(4); h1b.setOptStat(Integer.parseInt("1000000")); c.draw(h1b);  if (lr==2) c.draw(copy1,"same");
        c.cd(5); h1c.setOptStat(Integer.parseInt("1000000")); c.draw(h1c);             c.draw(copy2,"same");
        
        h1a = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,1,0).sliceY(ic); h1a.setTitleX("SECTOR "+is+" LAYER "+(ic+1)+" L PMT ADC");  h1a.setFillColor(col0);  
        h1b = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,2,0).sliceY(ic); h1b.setTitleX("SECTOR "+is+" LAYER "+(ic+1)+" R PMT ADC" ); h1b.setFillColor(col0);  
        h1c = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(ic); h1c.setTitleX("SECTOR "+is+" LAYER "+(ic+1)+" GMEAN");      h1c.setFillColor(col2);  
        
        if (lr==1) h1a.setFillColor(col2);
        if (lr==2) h1b.setFillColor(col2);
        c.cd(6); h1a.setOptStat(Integer.parseInt("1000100")); h1a.setTitle(""); c.draw(h1a);  
        c.cd(7); h1b.setOptStat(Integer.parseInt("1000100")); h1b.setTitle(""); c.draw(h1b); 
        c.cd(8); h1c.setOptStat(Integer.parseInt("1000100")); h1c.setTitle(""); c.draw(h1c);      
       
        c.repaint();
        
        ics=ic;
        
    }



}
