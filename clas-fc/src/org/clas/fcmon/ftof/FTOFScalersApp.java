package org.clas.fcmon.ftof;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.Timer;

import org.clas.fcmon.tools.FCEpics;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
//import org.root.basic.EmbeddedCanvas;
//import org.root.histogram.H1D;
//import org.root.histogram.H2D;
//groot
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class FTOFScalersApp extends FCEpics {

        DetectorCollection<H1F> H1_HV = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_HV = new DetectorCollection<H2F>();
        DetectorCollection<LinkedList<Double>> fifo1 = new DetectorCollection<LinkedList<Double>>();
        DetectorCollection<LinkedList<Double>> fifo2 = new DetectorCollection<LinkedList<Double>>();
        DetectorCollection<LinkedList<Double>> fifo3 = new DetectorCollection<LinkedList<Double>>();
        
        updateGUIAction action = new updateGUIAction();
        
        Timer timer = null;
        int delay=2000;
        int nfifo=0, nmax=120;
        int isCurrentSector;
        int isCurrentLayer;
        
        FTOFScalersApp(String name, String det) {
            super(name, det);
        }
        
        public void init(int is1, int is2) {
            this.is1=is1; 
            this.is2=is2;  
            setPvNames(this.detName,1);
            setPvNames(this.detName,2);
            setCaNames(this.detName,1);
            setCaNames(this.detName,2);
            sectorSelected=is1;
            layerSelected=1;
            channelSelected=1;
            initHistos();
            initFifos();
            fillFifos();
            fillHistos();
            System.out.println("FTOFScalersApp.init():Starting Timer");
            this.timer = new Timer(delay,action);  
            this.timer.setDelay(delay);
            this.timer.start();
        }
        
        private class updateGUIAction implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                fillFifos();
                fillHistos();
                update1DScalers(scaler1DView,1);   
                update2DScalers(scaler2DView,1);        }
        } 
        
        public void initHistos() {       
            System.out.println("FTOFScalersApp.initHistos():");
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
            System.out.println("FTOFScalersApp.initFifos():");
            for (int is=is1; is<is2 ; is++) {
                for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                    for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {
                        fifo1.add(is, il, ic,new LinkedList<Double>());
                        fifo2.add(is, il, ic,new LinkedList<Double>());
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
                            fifo1.get(is, il, ic).removeFirst();
                            fifo2.get(is, il, ic).removeFirst();
                        }
                        fifo1.get(is, il, ic).add(getCaValue(1,"c3",is, il, ic));
                        fifo2.get(is, il, ic).add(getCaValue(2,"c1",is, il, ic));
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
                        H1_HV.get(is, il, 0).fill(ic,fifo1.get(is, il, ic).getLast());
                        H1_HV.get(is, il, 1).fill(ic,fifo2.get(is, il, ic).getLast());
                        Double ts1[] = new Double[fifo1.get(is, il, ic).size()];
                        fifo1.get(is, il, ic).toArray(ts1);
                        Double ts2[] = new Double[fifo2.get(is, il, ic).size()];
                        fifo2.get(is, il, ic).toArray(ts2);
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
            int lr = layerSelected+2*app.detectorIndex;
            int ip = channelSelected; 
            
            if (lr==0||lr>layMap.get(detName).length) return;
                        
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
            int lr = layerSelected+2*app.detectorIndex; 
            
            if (lr==0||lr>layMap.get(detName).length) return;
            
            //Don't redraw unless timer fires or new sector selected
            if (flag==0&&lr==isCurrentLayer) return;  
            
            canvas.divide(2, 1);
            
            String tit = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
            
            h = H2_HV.get(is, lr, 0); h.setTitleX(tit); h.setTitleY("TIME");
            canvas.cd(0); canvas.draw(h);
            
            h = H2_HV.get(is, lr, 1); h.setTitleX(tit); h.setTitleY("TIME");
            canvas.cd(1); canvas.draw(h);
            
            isCurrentSector = is;
            isCurrentLayer  = lr;
            
            canvas.repaint();
            
        }

}
