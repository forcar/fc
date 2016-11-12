package org.clas.fcmon.ftof;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
//groot
//groot
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class FTOFMode1App extends FCApplication {
    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 

    public FTOFMode1App(String name, FTOFPixels[] ftofPix) {
        super(name,ftofPix);    
     }
    
    public void updateCanvas(DetectorDescriptor dd) {
              
        this.getDetIndices(dd);   
        int  lr = layer;
        int ilm = ilmap;
        
        int nstr = ftofPix[ilm].nstr;
        int min=0, max=nstr;
        
        switch (ilmap) {
        case 0: c.divide(4,6); break;
        case 1: c.divide(4,6); max=24 ; if (ic>23) {min=24; max=48;} if (ic>47) {min=48; max=nstr;} break;
        case 2: c.divide(2,3);
        }    

        c.setAxisFontSize(14);
//        canvas.setTitleFontSize(14);
//        canvas.setAxisTitleFontSize(14);
        
        H1F h ; 
        String otab[]={" Left PMT "," Right PMT "};

//        app.mode7Emulation.init("/daq/fadc/ftof",app.currentCrate, app.currentSlot, app.currentChan);
       
        int tet = app.mode7Emulation.tet;
        
        if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
        if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
        
        F1D f1 = new F1D("p0","[a]",0.,100.); f1.setParameter(0,tet);
        f1.setLineColor(2);
        F1D f2 = new F1D("p0","[a]",0.,100.); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
        f2.setLineColor(4);f2.setLineStyle(2);
       
        c.clear();
        
        for(int ip=min;ip<max;ip++){
            c.cd(ip-min); 
            c.getPad(ip-min).setOptStat(Integer.parseInt("0"));
            c.getPad(ip-min).getAxisX().setRange(0.,100.);
            c.getPad(ip-min).getAxisY().setRange(-100.,4000*app.displayControl.pixMax);
            h = ftofPix[ilm].strips.hmap2.get("H2_a_Sevd").get(is,lr,0).sliceY(ip); h.setTitleX("Samples (4 ns)"); h.setTitleY("Counts");
            h.setTitle("Sector "+is+otab[lr-1]+(ip+1)); h.setFillColor(4); c.draw(h);
            h = ftofPix[ilm].strips.hmap2.get("H2_a_Sevd").get(is,lr,1).sliceY(ip); h.setFillColor(2); c.draw(h,"same");
            c.draw(f1,"same"); c.draw(f2,"same");
            }  
            
        c.repaint();
    }   
    
}
