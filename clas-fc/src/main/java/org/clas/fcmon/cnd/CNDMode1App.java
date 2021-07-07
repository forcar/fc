package org.clas.fcmon.cnd;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
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

public class CNDMode1App extends FCApplication {
    
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    mode1  = new EmbeddedCanvasTabbed("Event");
    EmbeddedCanvas               c = this.getCanvas(this.getName()); 
    
    int is,il,lr,ic,idet,nstr;
    String otab[]={" L PMT "," R PMT "};
    double tlo = cndPix[0].tlim[0];
    double thi = cndPix[0].tlim[1];
    
    public CNDMode1App(String name, CNDPixels[] cndPix) {
        super(name,cndPix);    
     }
    
    public JPanel getPanel() {
        engineView.setLayout(new BorderLayout());
        mode1.addCanvas("Sum");
        mode1.addCanvas("AvsT");
        mode1.addCanvas("LOGRAT");
        engineView.add(mode1);
        return engineView;
    }    
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.is = dd.getSector();
        this.il = dd.getLayer();
        this.lr = dd.getOrder()+1;
        this.ic = dd.getComponent();   
        
        this.idet = ilmap;      
        
        if (lr>3) return;
        
        this.nstr = cndPix[idet].nstr;    
        
        switch (mode1.selectedCanvas) {
        case   "Event": updateEvent(); break;
        case     "Sum": updateSum();   break;
        case    "AvsT": updateAvsT();  break;
        case  "LOGRAT": updateLOGRAT();
        }
        
     } 
    
    public void updateEvent() {

        int min=0, max=nstr;
        c = mode1.getCanvas("Event");       
          
        c.divide(2,3);
        c.setAxisFontSize(14);
        
//        app.mode7Emulation.init("/daq/fadc/ftof",app.currentCrate, app.currentSlot, app.currentChan);
       
        int tet = app.mode7Emulation.tet;
        
        if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
        if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
        
        F1D f1 = new F1D("p0","[a]",0.,100.); f1.setParameter(0,tet);
        f1.setLineColor(2);
        F1D f2 = new F1D("p0","[a]",0.,100.); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
        f2.setLineColor(4);f2.setLineStyle(2);

        H1F h ; 
       
        c.clear();
        
        int n = 0;
        
        for (int il=1; il<4; il++) {
        for (int lr=1; lr<3; lr++) {
            c.cd(n); 
            c.getPad(n).setOptStat(Integer.parseInt("0"));
            c.getPad(n).getAxisX().setRange(0.,100.);
            c.getPad(n).getAxisY().setRange(-100.,2000*app.displayControl.pixMax);
            h = cndPix[idet].strips.hmap2.get("H2_a_Sevd").get(is,lr,0).sliceY(il-1);    
            h.setTitle(" ");
            h.setTitleX("Sector "+is+" Layer "+il+otab[lr-1]+" (4 ns/ch)"); h.setTitleY("Counts");
            h.setFillColor(4); c.draw(h);
            h = cndPix[idet].strips.hmap2.get("H2_a_Sevd").get(is,lr,1).sliceY(il-1); 
            h.setFillColor(2); c.draw(h,"same");
            c.draw(f1,"same"); c.draw(f2,"same");
            n++;
        }
        }
            
        c.repaint();
    }   
    
    public void updateSum() {
        
        DetectorCollection<H2F> dc2a = cndPix[idet].strips.hmap2.get("H2_a_Hist");        
        DetectorCollection<H2F> dc2t = cndPix[idet].strips.hmap2.get("H2_t_Sevd"); 
        
        H1F h1; H2F h2;
        
        F1D f1 = new F1D("p0","[a]",0.,100.); 
        F1D f2 = new F1D("p0","[a]",0.,100.); 
        f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(0);
        f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(0);
               
        c = mode1.getCanvas("Sum");  c.clear(); c.divide(2,2);       
            
        for (int ilr=1; ilr<3 ; ilr++) {
            h2 = dc2a.get(is,ilr,5); h2.setTitleY("Sector "+is+otab[ilr-1]) ; h2.setTitleX("SAMPLES (4 ns/ch)");
            canvasConfig(c,ilr-1,0.,100.,1.,nstr+1.,true).draw(h2);
            if (lr==ilr) {c.draw(f1,"same"); c.draw(f2,"same");}
            h1 = dc2a.get(is,ilr,5).sliceY(ic); h1.setOptStat(Integer.parseInt("10"));
            h1.setTitle(" ");
            h1.setTitleX("Sector "+is+" Layer "+(ic+1)+otab[ilr-1]+" (4 ns/ch)"); h1.setFillColor(0);
            c.cd(ilr+1); h1.setTitle(" "); c.draw(h1);
            if (lr==ilr) {            	
                h1=dc2a.get(is,ilr,5).sliceY(ic) ; h1.setFillColor(2);            	
                h1.setTitleX("Sector "+is+" Layer "+(ic+1)+otab[ilr-1]+" (4 ns/ch)"); h1.setTitle(" "); c.draw(h1);
            }            
//            if (app.isSingleEvent()) {h1=dc2t.get(is,il,0).sliceY(il-1) ; h1.setFillColor(4); c.draw(h1,"same");}
        }
        
        c.repaint();
        
     } 
    
    public void updateAvsT() {
        
        DetectorCollection<H2F> dc2t = cndPix[idet].strips.hmap2.get("H2_t_Hist");       
         
        H2F h2;
        
        c = mode1.getCanvas("AvsT");  c.clear(); c.divide(2,3);       
       
        int n = 0;
        
        for (int il=1; il<4; il++) {
        for (int lr=1; lr<3; lr++) {
            h2=dc2t.get(is,lr,il); 
            h2.setTitleY("Sector "+is+" Layer "+il+otab[lr-1]+" TDC") ; 
            h2.setTitleX("Sector "+is+" Layer "+il+otab[lr-1]+" FADC");
            canvasConfig(c,n,0.,cndPix[0].amax[0],tlo,thi,true).draw(h2);   
            n++;
        }
        }
        
        c.repaint();
        
    }
    
    public void updateLOGRAT() {
        
        DetectorCollection<H2F> dc2a = cndPix[idet].strips.hmap2.get("H2_t_Hist");       
           
        H2F h2;
        int min=1,max=9;
                
        c = mode1.getCanvas("LOGRAT");  c.clear(); c.divide(2,4); 
        max=9 ; if (is>8) {min=9; max=17;} if (is>16) {min=17; max=25;} 
        
        for(int iis=min;iis<max;iis++) {
            h2=dc2a.get(iis,ic+1,4); h2.setTitleX(" SEC "+iis+" LAY "+(ic+1)+" TUP-TDOWN (ns)") ;
            canvasConfig(c,iis-min,-15.,15.,-1.1,1.1,true).draw(h2);            
        }
        
        c.repaint();
        
    }    
}
