package org.clas.fcmon.cnd;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;

public class CNDMipApp extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
//    F1D f1 = new F1D("landau",300.,3000.);

    public CNDMipApp(String name, CNDPixels[] cndPix) {
        super(name,cndPix);    
     }

    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd);   
        int  lr = io+1;
        int ilm = ilmap;
        
        int nstr = cndPix[ilm].nstr;
        int min=0, max=nstr;
        
        switch (ilmap) {
        case 0: c.divide(4,6); break;
        }     
        
        c.setAxisFontSize(12);
//      canvas.setAxisTitleFontSize(12);
//      canvas.setTitleFontSize(14);
//      canvas.setStatBoxFontSize(10);
        
        H1F h;
        String alab;
        String otab[]={" L PMT "," R PMT "};
        String lab4[]={" ADC"," TDC","GMEAN "};      

       
        for(int iis=1 ; iis<25 ; iis++) {
            alab = "SECTOR "+iis+" LAYER "+(ic+1)+otab[lr-1]+lab4[0];
            c.cd(iis-1);                           
            h = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(iis,lr,0).sliceY(ic); 
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setFillColor(32); c.draw(h);
            h = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(iis,0,0).sliceY(ic);
            h.setFillColor(24); c.draw(h,"same");  
//            if (h.getEntries()>100) {h.fit(f1,"REQ");}
        }

        c.cd(is-1); 
        h = cndPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(ic); 
        h.setOptStat(Integer.parseInt("1000100")); 
        alab = "SECTOR "+is+" LAYER "+(ic+1)+" GMEAN"; 
        h.setTitleX(alab); h.setTitle(""); h.setFillColor(2); c.draw(h); 
        
        c.repaint();

    }
}
