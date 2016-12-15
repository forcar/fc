package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.Timer;

import org.clas.fcmon.tools.FCEpics;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;

import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class ECScalersApp extends FCEpics {
   
    DetectorCollection<H1F> H1_HV = new DetectorCollection<H1F>();
    DetectorCollection<H2F> H2_HV = new DetectorCollection<H2F>();
    
    updateGUIAction action = new updateGUIAction();
    
    Timer timer = null;
    int delay=2000;
    int nfifo=0, nmax=120;
    int isCurrentSector;
    int isCurrentLayer;
    
    ECScalersApp(String name, String det) {
        super(name, det);
    }
    
    public void init(Boolean online) {
        this.online = online;
        this.is1=ECConstants.IS1; 
        this.is2=ECConstants.IS2; 
        setPvNames(this.detName,1);
        setPvNames(this.detName,2);
        sectorSelected=is1;
        layerSelected=1;
        channelSelected=1;
        initHistos();  
        createContext();
        setCaNames(this.detName,1);
        setCaNames(this.detName,2);
        initFifos();
        this.timer = new Timer(delay,action);  
        this.timer.setDelay(delay);
        this.timer.start();
    }
    
    public JPanel getPanel() {        
        engineView.setLayout(new BorderLayout());
        engineView.add(getCanvasPane(),BorderLayout.CENTER);
        engineView.add(getButtonPane(),BorderLayout.PAGE_END);
        return engineView;       
    }   
    
    public JSplitPane getCanvasPane() {
        JSplitPane HVScalerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);       
        HVScalerPane.setTopComponent(scaler1DView);
        HVScalerPane.setBottomComponent(scaler2DView);       
        HVScalerPane.setResizeWeight(0.2);
        return HVScalerPane;
    }
    
    public JPanel getButtonPane() {
        buttonPane = new JPanel();
        return buttonPane;
    }
    
    private class updateGUIAction implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            fillFifos();
            fillHistos();
            update1DScalers(scaler1DView,1);   
            update2DScalers(scaler2DView,1);        }
    } 
    
    public void initHistos() {       
        System.out.println("ECScalersApp.initHistos():");
        for (int is=is1; is<is2 ; is++) {
            for (int il=1 ; il<layMap.get(detName).length+1 ; il++){
                int nb=nlayMap.get(detName)[il-1]; int mx=nb+1;
                H1_HV.add(is, il, 0, new H1F("HV_dsc2"+is+"_"+il, nb,1,mx));                
                H1_HV.add(is, il, 1, new H1F("HV_fadc"+is+"_"+il, nb,1,mx));                               
                H2_HV.add(is, il, 0, new H2F("HV_dsc2"+is+"_"+il, nb,1,mx,nmax,0,nmax));                
                H2_HV.add(is, il, 1, new H2F("HV_fadc"+is+"_"+il, nb,1,mx,nmax,0,nmax));                               
            }
        }
    }
        
    public void initFifos() {
        System.out.println("ECScalersApp.initFifos():");
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {
                    app.fifo4.add(is, il, ic,new LinkedList<Double>());
                    app.fifo5.add(is, il, ic,new LinkedList<Double>());
                    connectCa(1,"c3",is,il,ic);
                    connectCa(2,"c1",is,il,ic);
                }
            }
        }
    }
    
    public void fillFifos() {
        
        //long startTime = System.currentTimeMillis();
        nfifo++;
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {
                    if(nfifo>nmax) {
                        app.fifo4.get(is, il, ic).removeFirst();
                        app.fifo5.get(is, il, ic).removeFirst();
                    }
                    app.fifo4.get(is, il, ic).add(getCaValue(1,"c3",is, il, ic));
                    app.fifo5.get(is, il, ic).add(getCaValue(2,"c1",is, il, ic));
                }
            }
         }
       // System.out.println("time= "+(System.currentTimeMillis()-startTime));
        
    }

    public void fillHistos() {
        
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                H1_HV.get(is, il, 0).reset(); H2_HV.get(is, il, 0).reset();
                H1_HV.get(is, il, 1).reset(); H2_HV.get(is, il, 1).reset();
                for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {                    
                    H1_HV.get(is, il, 0).fill(ic,app.fifo4.get(is, il, ic).getLast());
                    H1_HV.get(is, il, 1).fill(ic,app.fifo5.get(is, il, ic).getLast());
                    Double ts1[] = new Double[app.fifo4.get(is, il, ic).size()];
                    app.fifo4.get(is, il, ic).toArray(ts1);
                    Double ts2[] = new Double[app.fifo5.get(is, il, ic).size()];
                    app.fifo5.get(is, il, ic).toArray(ts2);
                    for (int it=0; it<ts1.length; it++) {
                        H2_HV.get(is, il, 0).fill(ic,it,ts1[it]);
                        H2_HV.get(is, il, 1).fill(ic,it,ts2[it]);
                    }
                }
            }
        }
        
    }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        sectorSelected  = dd.getSector(); 
        layerSelected   = dd.getLayer();
        channelSelected = dd.getComponent(); 
        
        update1DScalers(scaler1DView,0);   
        update2DScalers(scaler2DView,0);
        
        isCurrentSector = sectorSelected;
        isCurrentLayer  = layerSelected;
    }
    
    public void update1DScalers(EmbeddedCanvas canvas, int flag) {
        
        H1F h = new H1F();
        H1F c = new H1F();
        
        int is = sectorSelected;
        int lr = layerSelected+3*app.detectorIndex;
        int ip = channelSelected; 
        
        if (layerSelected>3||lr==0||lr>layMap.get(detName).length) return;
        
        canvas.divide(2, 1);
        
        String tit = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
        
        h = H1_HV.get(is, lr, 0); h.setTitleX(tit); h.setTitleY("DSC2 HITS");
        h.setFillColor(32); canvas.cd(0); canvas.draw(h);

        h = H1_HV.get(is, lr, 1); h.setTitleX(tit); h.setTitleY("FADC HITS");
        h.setFillColor(32); canvas.cd(1); canvas.draw(h);

        
        c = H1_HV.get(is, lr, 0).histClone("Copy"); c.reset() ; 
        c.setBinContent(ip, H1_HV.get(is, lr, 0).getBinContent(ip));
        c.setFillColor(2);  canvas.cd(0); canvas.draw(c,"same");
        
        c = H1_HV.get(is, lr, 1).histClone("Copy"); c.reset() ; 
        c.setBinContent(ip, H1_HV.get(is, lr, 1).getBinContent(ip));
        c.setFillColor(2);  canvas.cd(1); canvas.draw(c,"same");
               
        canvas.repaint();
    }
    
    public void update2DScalers(EmbeddedCanvas canvas, int flag) {
        
        H2F h = new H2F();
        
        int is = sectorSelected;
        int lr = layerSelected+3*app.detectorIndex;
        
        if (layerSelected>3||lr==0||lr>layMap.get(detName).length) return;
        
        //Don't redraw unless timer fires or new sector selected
        if (flag==0&&lr==isCurrentLayer) return;  
        
        canvas.divide(2, 1);
        
        String tit = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
        
        h = H2_HV.get(is, lr, 0); h.setTitleX(tit); h.setTitleY("TIME");
        canvas.cd(0); canvas.draw(h);
        h = H2_HV.get(is, lr, 1); h.setTitleX(tit); h.setTitleY("TIME");
        canvas.cd(1); canvas.draw(h);

        canvas.repaint();
        
        isCurrentSector = is;
        isCurrentLayer  = lr;
        
    }
}
