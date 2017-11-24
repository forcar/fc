package org.clas.fcmon.cc;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.H1F;
import org.jlab.groot.graphics.EmbeddedCanvas;


public class CCSpeApp extends FCApplication {

    public CCSpeApp(String name, CCPixels ccPix) {
        super(name,ccPix);    
     }

    public void updateCanvas(DetectorDescriptor dd) {
        
        EmbeddedCanvas c = this.getCanvas(this.getName()); 
        this.getDetIndices(dd);   
        int lr = layer;
        
        int il=1,col0=0,col1=4,col2=2;
        int nstr = ccPix.nstr[0];
        
        H1F h;
        String alab;
        String otab[]={" Left PMT "," Right PMT "};
        String lab4[]={" ADC"," TDC"};      
        
        c.divide(3,6);
        c.setAxisFontSize(12);
        c.setStatBoxFontSize(10);
        
        il = 1; 
        
        for(int iip=0;iip<nstr;iip++) {
            alab = "Sector "+is+otab[lr-1]+(iip+1)+lab4[0];
            c.cd(iip); h = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,lr,0).sliceY(iip); 
            h.setOptStat(Integer.parseInt("1110"));
            h.setTitleX(alab); h.setTitle(""); h.setFillColor(col1); c.draw(h);
        }
/*
        il = 2;
        
        for(int iip=0;iip<nstr;iip++) {
            alab = otab[il-1]+(iip+1)+lab4[0];
            canvas.cd(nstr+iip); h = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is+1,il,0).sliceY(iip); 
            h.setXTitle(alab); h.setTitle(""); h.setFillColor(col1); canvas.draw(h,"S");
        }       
        canvas.cd((lr-1)*nstr+ic); h = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is+1,lr,0).sliceY(ic); 
        h.setTitle(""); h.setFillColor(col2); canvas.draw(h,"same");  
        */
        
        c.cd(ic); h = ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,lr,0).sliceY(ic); 
        h.setTitle(""); h.setFillColor(col2); c.draw(h,"same");    

        c.repaint();
    }
}
