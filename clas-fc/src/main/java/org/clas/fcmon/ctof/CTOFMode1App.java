package org.clas.fcmon.ctof;

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

public class CTOFMode1App extends FCApplication {
    
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    mode1  = new EmbeddedCanvasTabbed("Event");
    EmbeddedCanvas               c = this.getCanvas(this.getName()); 
    
    int is,lr,ic,idet,nstr;
    int ics[][] = new int[3][2];
    String otab[]={" UP PMT "," DOWN PMT "};
    double tlo = ctofPix[0].tlim[0];
    double thi = ctofPix[0].tlim[1];
    double fhi = ctofPix[0].fmax;
    
    public CTOFMode1App(String name, CTOFPixels[] ctofPix) {
        super(name,ctofPix);    
     }
    
    public JPanel getPanel() {
        engineView.setLayout(new BorderLayout());
        mode1.addCanvas("Sum");
        mode1.addCanvas("TDIF");
        mode1.addCanvas("AvsT");
        mode1.addCanvas("LOGRAT");
        mode1.addCanvas("SYNC");  
        engineView.add(mode1);
        return engineView;
    }    
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.is = dd.getSector();        
        this.lr = dd.getOrder()+1;
        this.ic = dd.getComponent();   
        this.idet = ilmap;      
        
        if (lr>3) return;
        
        this.nstr = ctofPix[idet].nstr;    
        
        switch (mode1.selectedCanvas) {
        case  "Event": updateEvent(); break;
        case    "Sum": updateSum();   break;
        case   "TDIF": updateTDIF();   break;
        case   "AvsT": updateAvsT();  break;
        case "LOGRAT": updateLOGRAT();break;
        case   "SYNC": updateSync();
        }
        
     } 
    
    public void updateEvent() {

        int min=0, max=nstr;
        c = mode1.getCanvas("Event");   c.clear();    
        
        switch (idet) {
        case 0: c.divide(4,6); max=24 ; if (ic>23) {min=24; max=48;} if (ic>47) {min=48; max=nstr;} break;
        }    

        c.setAxisFontSize(14);
        
//        app.mode7Emulation.init("/daq/fadc/ftof",app.currentCrate, app.currentSlot, app.currentChan);
       
        int tet = app.mode7Emulation.tet;
        
        if (app.mode7Emulation.User_tet>0)  tet=app.mode7Emulation.User_tet;
        if (app.mode7Emulation.User_tet==0) tet=app.mode7Emulation.CCDB_tet;
        
        F1D f1 = new F1D("p0","[a]",0.,fhi); f1.setParameter(0,tet);
        f1.setLineColor(2);
        F1D f2 = new F1D("p0","[a]",0.,fhi); f2.setParameter(0,app.mode7Emulation.CCDB_tet);
        f2.setLineColor(4);f2.setLineStyle(2);

        H1F h ; 
              
        for(int ip=min;ip<max;ip++){
            c.cd(ip-min); 
            c.getPad(ip-min).setOptStat(Integer.parseInt("0"));
            c.getPad(ip-min).getAxisX().setRange(0.,fhi);
            c.getPad(ip-min).getAxisY().setRange(-20.,2000*app.displayControl.pixMax);
            h = ctofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is,lr,0).sliceY(ip);            
            h.setTitleX("Sector "+is+otab[lr-1]+(ip+1)+" (4 ns/ch)"); h.setTitleY("Counts"); h.setTitle(" ");
            h.setFillColor(4); c.draw(h);
            h = ctofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is,lr,1).sliceY(ip); 
            h.setFillColor(2); c.draw(h,"same");
            c.draw(f1,"same"); c.draw(f2,"same");
            }  
            
        c.repaint();
    }   
    
    public void updateSum() {
        
        DetectorCollection<H2F> dc2a = ctofPix[idet].strips.hmap2.get("H2_a_Hist");        
        DetectorCollection<H2F> dc2t = ctofPix[idet].strips.hmap2.get("H2_t_Sevd"); 
        
        H1F h1; H2F h2;
        
        F1D f1 = new F1D("p0","[a]",0.,fhi); 
        F1D f2 = new F1D("p0","[a]",0.,fhi); 
        f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(0);
        f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(0);
               
        c = mode1.getCanvas("Sum");  c.clear(); c.divide(2,2);       
            
        for (int il=1; il<3 ; il++) {
            h2 = dc2a.get(is,il,5); h2.setTitleY("Sector "+is+otab[il-1]) ; h2.setTitleX("SAMPLES (4 ns/ch)");
            canvasConfig(c,il-1,0.,fhi,1.,nstr+1.,true).draw(h2);
            if (lr==il) {c.draw(f1,"same"); c.draw(f2,"same");}
            h1 = dc2a.get(is,il,5).sliceY(ic); h1.setOptStat(Integer.parseInt("10"));
            h1.setTitleX("Sector "+is+otab[il-1]+(ic+1)+" (4 ns/ch)"); h1.setFillColor(0);
            c.cd(il+1); h1.setTitle(" "); c.draw(h1);
            if (lr==il) {
                h1=dc2a.get(is,il,5).sliceY(ic) ; h1.setFillColor(2);
                h1.setTitleX("Sector "+is+otab[il-1]+(ic+1)+" (4 ns/ch)"); c.draw(h1);
            }            
            if (app.isSingleEvent()) {h1=dc2t.get(is,il,0).sliceY(ic) ; h1.setFillColor(4); c.draw(h1,"same");}
        }
        
        c.repaint();
        
        ics[idet][lr-1]=ic;
        
     } 
    
     public void updateTDIF() {
        
        DetectorCollection<H2F> dc2a = ctofPix[idet].strips.hmap2.get("H2_a_Hist");        
        
        H1F h1; H2F h2;
        
        F1D f1 = new F1D("p0","[a]",-30.,30.); 
        F1D f2 = new F1D("p0","[a]",-30.,30.); 
        f1.setParameter(0,ic+1); f1.setLineWidth(1); f1.setLineColor(0);
        f2.setParameter(0,ic+2); f2.setLineWidth(1); f2.setLineColor(0);
               
        c = mode1.getCanvas("TDIF");  c.clear(); c.divide(2,2);       
            
        for (int il=1; il<3 ; il++) {
            h2 = dc2a.get(is,il,6); h2.setTitleY("Sector "+is+otab[il-1]) ; h2.setTitleX("TDC-FADC (NSEC)");
            canvasConfig(c,il-1,-30.,30.,1.,nstr+1.,true).draw(h2);
            if (lr==il) {c.draw(f1,"same"); c.draw(f2,"same");}
            h1 = dc2a.get(is,il,6).sliceY(ic); h1.setOptStat(Integer.parseInt("10"));
            h1.setTitleX("Sector "+is+otab[il-1]+(ic+1)+" (NSEC)"); h1.setFillColor(0);
            c.cd(il+1); h1.setTitle(" "); c.draw(h1);
            if (lr==il) {
                h1=dc2a.get(is,il,6).sliceY(ic) ; h1.setFillColor(2);
                h1.setTitleX("Sector "+is+otab[il-1]+(ic+1)+" (NSEC)"); c.draw(h1);
            }            
        }
        
        c.repaint();
        
        ics[idet][lr-1]=ic;
        
     }  
    
    public void updateAvsT() {
        
        DetectorCollection<H2F> dc2a = ctofPix[idet].strips.hmap2.get("H2_a_Hist");       
        DetectorCollection<H2F> dc2t = ctofPix[idet].strips.hmap2.get("H2_t_Hist");       
        
        H2F h2;
        
        c = mode1.getCanvas("AvsT");  c.clear(); c.divide(2,1);       
       
        for (int il=1; il<3; il++) {
            h2=dc2a.get(is,il,1); h2.setTitleY("Sector "+is+otab[il-1]+" TDC") ; h2.setTitleX("Sector "+is+otab[il-1]+" FADC");
            canvasConfig(c,il-1,0.,ctofPix[0].amax[1],tlo,thi,true).draw(h2);            
        }
        
//        h2=dc2t.get(is, 0, 1); h2.setTitleX("Paddle Pair Time Difference (ns)") ; h2.setTitleY("Paddle Separation");        
//        canvasConfig(c,2,-20.,20.,1.,49.,true).draw(h2);
        
        c.repaint();
        
    }  
    
    public void updateLOGRAT() {
        
        DetectorCollection<H2F> dc2a = ctofPix[idet].strips.hmap2.get("H2_t_Hist");       
           
        H2F h2;
        int min=0,max=24;
                
        c = mode1.getCanvas("LOGRAT");  c.clear(); c.divide(6,4); 
        max=24 ; if (ic>23) {min=24; max=48;}; 
        
        for(int iip=min;iip<max;iip++) {
            h2=dc2a.get(is,iip+1,2); h2.setTitleX(" PMT "+(iip+1)+" TUP-TDOWN (ns)") ;
//            canvasConfig(c,iip-min,-32.,-15.,-0.6,0.6,true).draw(h2);            
            canvasConfig(c,iip-min,-10., 10.,-0.6,0.6,true).draw(h2);            
        }
        
        c.repaint();
        
    }
    
    public void updateSync() {
        
        DetectorCollection<H2F> dc2a = ctofPix[idet].strips.hmap2.get("H2_t_Hist"); 
        
        H2F h2;
        
        c = mode1.getCanvas("SYNC");  c.clear(); c.divide(1,2); 
        
 
        h2 = dc2a.get(is,3,3) ;  h2.setTitleY("PHASE") ; h2.setTitleX("Sector "+is+" TDC-FADC (ns)");   
        canvasConfig(c,is-1,-30,30,0.,6.,true).draw(h2);
              
        h2 = dc2a.get(is,3,4) ;  h2.setTitleY("PHASE") ; h2.setTitleX("Sector "+is+" CTOF Corrected TDC (ns)");   
        canvasConfig(c,is,tlo,thi,0.,6.,true).draw(h2);
               
        c.repaint();   
    }    
}
