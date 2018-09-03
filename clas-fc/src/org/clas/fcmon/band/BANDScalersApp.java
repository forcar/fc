package org.clas.fcmon.band;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.ColorPalette;
import org.clas.fcmon.tools.FCEpics;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;

import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.ui.RangeSlider;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class BANDScalersApp extends FCEpics {

        DetectorCollection<H1F> H1_SCA = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_SCA = new DetectorCollection<H2F>();
        float norm1[] = null;
        float norm2[] = null;
        float norm3[][] = new float[9][68];
        float norm4[][] = new float[9][68];  
        
        updateGUIAction action = new updateGUIAction();
        
        Timer timer = null;
        int delay=2000, nfifo=0, nmax=120;
        Boolean isAccum = false;
        Boolean  isLogz = false;
        Boolean  isLogy = false;
        Boolean  isNorm = false;
        int nTimer = 0;
        double zMin,zMax,zMinLab,zMaxLab;
        int isCurrentSector;
        int isCurrentLayer;
        
        BANDScalersApp(String name, String det) {
            super(name, det);
        }  
        
        public void init() {
            this.is1=1; 
            this.is2=2;  
            setPvNames(this.detName,2);
            sectorSelected=is1;
            layerSelected=1;
            channelSelected=1;
            orderSelected=1;
            initHistos();
        }
        
        public void startEPICS() {
            createContext();
            setCaNames(this.detName,2);
            initFifos();
            this.timer = new Timer(delay,action);  
            this.timer.setDelay(delay);
            this.timer.start();              
        }
        
        public void stopEPICS() {
            if(this.timer.isRunning()) this.timer.stop();
            destroyContext();
        }   
        
        public JPanel getPanel() {        
            engineView.setLayout(new BorderLayout());
            engineView.add(getEnginePane(),BorderLayout.CENTER);
            return engineView;       
        } 
        
        public JSplitPane getEnginePane() {  
            enginePane.setTopComponent(getEngine1DView());
            enginePane.setBottomComponent(getEngine2DView());       
            enginePane.setResizeWeight(0.2);
            return enginePane;
        }
        
        public JPanel getEngine1DView() {
            engine1DView.setLayout(new BorderLayout());
            engine1DCanvas = new EmbeddedCanvasTabbed("Scalers");
            engine1DView.add(engine1DCanvas,BorderLayout.CENTER);
            get1DButtonPane();
            return engine1DView;
        }
        
        public JPanel getEngine2DView() {
            engine2DView.setLayout(new BorderLayout());
            engine2DCanvas = new EmbeddedCanvasTabbed("Stripcharts");
            engine2DView.add(engine2DCanvas,BorderLayout.CENTER);
            getSliderPane();
            get2DButtonPane();
            return engine2DView;        
        }
        
        public void get1DButtonPane() {
            JCheckBox cb1 = new JCheckBox("Accumulate");
            cb1.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    isAccum = (e.getStateChange() == ItemEvent.SELECTED) ? true:false;
                    nTimer = 1;
                    updateScalers(1);
                }
            });    
            cb1.setSelected(false);
            engine1DCanvas.actionPanel.add(cb1);
            
            JCheckBox cb2 = new JCheckBox("Lin Y/Log Y");
            cb2.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    isLogy = (e.getStateChange() == ItemEvent.SELECTED) ? true:false;
                    updateScalers(1);
                }
            });         
            cb2.setSelected(false);
            engine1DCanvas.actionPanel.add(cb2);        
        }
        
        public void get2DButtonPane() {
            JCheckBox cb3 = new JCheckBox("Lin Z/Log Z");
            cb3.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    isLogz = (e.getStateChange() == ItemEvent.SELECTED) ? true:false;
                    updateScalers(1);
                }
            });    
            cb3.setSelected(false);
            engine2DCanvas.actionPanel.add(cb3);
            
            JCheckBox cb4 = new JCheckBox("Normalize");
            cb4.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    isNorm = (e.getStateChange() == ItemEvent.SELECTED) ? true:false;
                    updateScalers(1);
                }
            });    
            cb4.setSelected(false);
            engine2DCanvas.actionPanel.add(cb4);
        }
        
        public void getSliderPane() {
            JLabel xLabel = new JLabel("Z-Range:");
            RangeSlider slider = new RangeSlider();
            slider.setMinimum((int) 0.);
            slider.setMaximum((int) 70.);
            slider.setValue((int) 0.);
            slider.setUpperValue((int) 20.);            
            zMin = slider.getValue();
            zMax = slider.getUpperValue();
            zMinLab = Math.pow(10, zMin/10); zMaxLab = Math.pow(10, zMax/10);
            JLabel rangeSliderValue1 = new JLabel("" + String.format("%4.0f", zMinLab));
            JLabel rangeSliderValue2 = new JLabel("" + String.format("%4.0f", zMaxLab));
            engine2DCanvas.actionPanel.add(xLabel);
            engine2DCanvas.actionPanel.add(rangeSliderValue1);
            engine2DCanvas.actionPanel.add(slider);
            engine2DCanvas.actionPanel.add(rangeSliderValue2);           
            slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    zMin = slider.getValue();
                    zMax = slider.getUpperValue();
                    zMinLab = Math.pow(10, zMin/10); zMaxLab = Math.pow(10, zMax/10);
                    rangeSliderValue1.setText(String.valueOf("" + String.format("%4.0f", zMinLab)));
                    rangeSliderValue2.setText(String.valueOf("" + String.format("%4.0f", zMaxLab)));
                    updateScalers(1);
                }
            });   
        }
        
        private class updateGUIAction implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                fillFifos();
                fillHistos();           
                nTimer++;
                updateScalers(1);
            }
        } 
        
        public void initHistos() {       
            System.out.println("BANDScalersApp.initHistos():");
            for (int is=is1; is<is2 ; is++) {
                for (int il=1 ; il<layMap.get(detName).length+1 ; il++){
                    int nb=nlayMap.get(detName)[il-1]; int mx=nb+1;
                    H1_SCA.add(is, il, 0, new H1F("HV_dsc2"+is+"_"+il, nb,1,mx));                
                    H1_SCA.add(is, il, 1, new H1F("HV_fadc"+is+"_"+il, nb,1,mx));                               
                    H2_SCA.add(is, il, 0, new H2F("HV_dsc2"+is+"_"+il, nb,1,mx,nmax,0,nmax));                
                    H2_SCA.add(is, il, 1, new H2F("HV_fadc"+is+"_"+il, nb,1,mx,nmax,0,nmax));                               
                }
            }
        }
            
        public void initFifos() {
            System.out.println("BANDScalersApp.initFifos():");
            for (int is=is1; is<is2 ; is++) {
                for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                    for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {
                        app.fifo4.add(is, il, ic,new LinkedList<Double>());
                        app.fifo5.add(is, il, ic,new LinkedList<Double>());
//                        connectCa(2,"c",is,il,ic);
                        connectCa(2,"c",is,il,ic);
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
                        app.fifo4.get(is, il, ic).add(getCaValue(2,"c",is, il, ic));
                        app.fifo5.get(is, il, ic).add(getCaValue(2,"c",is, il, ic));
                    }
                }
             }
           // System.out.println("time= "+(System.currentTimeMillis()-startTime));
            
        }

        public void fillHistos() {
            
            for (int is=is1; is<is2 ; is++) {
                for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                    if(!isAccum) {H1_SCA.get(is, il, 0).reset(); H1_SCA.get(is, il, 1).reset();}
                    H2_SCA.get(is, il, 0).reset(); H2_SCA.get(is, il, 1).reset();
                    for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {                    
                        H1_SCA.get(is, il, 0).fill(ic,app.fifo4.get(is, il, ic).getLast());
                        H1_SCA.get(is, il, 1).fill(ic,app.fifo5.get(is, il, ic).getLast());
                        Double ts1[] = new Double[app.fifo4.get(is, il, ic).size()];
                        Double ts2[] = new Double[app.fifo5.get(is, il, ic).size()];
                        app.fifo4.get(is, il, ic).toArray(ts1);
                        app.fifo5.get(is, il, ic).toArray(ts2);
                        for (int it=0; it<ts1.length; it++) {
                            if(isNorm) {
                                ts1[it]=(ts1[it]-norm3[il-1][ic-1])/Math.sqrt(ts1[it]);
                                ts2[it]=(ts2[it]-norm4[il-1][ic-1])/Math.sqrt(ts2[it]);
                            }                
                            H2_SCA.get(is, il, 0).fill(ic,it,ts1[it]);
                            H2_SCA.get(is, il, 1).fill(ic,it,ts2[it]);
                        }
                    }
                    if(isAccum&&!isNorm&&nTimer==10) {
                        norm1 = H1_SCA.get(is, il, 0).getData();
                        norm2 = H1_SCA.get(is, il, 1).getData();
                        int itim = nTimer+1;
                        for (int i=0; i<norm1.length; i++) {norm3[il-1][i]=norm1[i]/itim;norm4[il-1][i]=norm2[i]/itim;}
                    }    
                }
            }
            
        }
        
        public synchronized void updateScalers(int flag) {
            update1DScalers(engine1DCanvas.getCanvas("Scalers"),flag);   
            update2DScalers(engine2DCanvas.getCanvas("Stripcharts"),flag);        
        }
        
        public void updateCanvas(DetectorDescriptor dd) {
            
            sectorSelected  = dd.getSector();  
            layerSelected   = dd.getOrder()+1;
            channelSelected = dd.getComponent(); 
            
            updateScalers(0);
            
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
            canvas.getPad(0).getAxisY().setLog(isLogy); 
            canvas.getPad(1).getAxisY().setLog(isLogy); 
            
            String tit = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
            
            h = H1_SCA.get(is, lr, 0); h.setTitleX(tit); h.setTitleY("DSC2 HITS");
            h.setFillColor(32); canvas.cd(0); canvas.draw(h);

            h = H1_SCA.get(is, lr, 1); h.setTitleX(tit); h.setTitleY("FADC HITS");
            h.setFillColor(32); canvas.cd(1); canvas.draw(h);
            
            c = H1_SCA.get(is, lr, 0).histClone("Copy"); c.reset() ; 
            c.setBinContent(ip, H1_SCA.get(is, lr, 0).getBinContent(ip));
            c.setFillColor(2);  canvas.cd(0); canvas.draw(c,"same");
            
            c = H1_SCA.get(is, lr, 1).histClone("Copy"); c.reset() ; 
            c.setBinContent(ip, H1_SCA.get(is, lr, 1).getBinContent(ip));
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
            canvas.getPad(0).getAxisY().setLog(false); 
            canvas.getPad(1).getAxisY().setLog(false); 
            canvas.getPad(0).getAxisZ().setLog(isLogz); 
            canvas.getPad(0).getAxisZ().setRange(zMinLab,zMaxLab);
            canvas.getPad(1).getAxisZ().setLog(isLogz); 
            canvas.getPad(1).getAxisZ().setRange(zMinLab,zMaxLab);
            if (isNorm) {
                canvas.getPad(0).getAxisZ().setLog(false); 
                canvas.getPad(1).getAxisZ().setLog(false); 
                canvas.getPad(0).getAxisZ().setRange(-3.0,3.0);
                canvas.getPad(1).getAxisZ().setRange(-3.0,3.0);
            }
            
            String tit = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
            
            h = H2_SCA.get(is, lr, 0); h.setTitleX(tit); h.setTitleY("TIME");
            canvas.cd(0); canvas.draw(h);
            
            h = H2_SCA.get(is, lr, 1); h.setTitleX(tit); h.setTitleY("TIME");
            canvas.cd(1); canvas.draw(h);
            
            isCurrentSector = is;
            isCurrentLayer  = lr;
            
            canvas.repaint();
            
        }
        
        public void updateDetectorView(DetectorShape2D shape) {
        	
            ColorPalette palette3 = new ColorPalette(3);
            ColorPalette palette4 = new ColorPalette(4);
                       
            ColorPalette pal = palette4;
            
            DetectorDescriptor dd = shape.getDescriptor(); 
            
            int is = dd.getSector();  
            int il = dd.getOrder()+1;
            int ip = dd.getComponent(); 
                        
            float z = (float) H1_SCA.get(is, il, 1).getBinContent(ip) ;
            
            if (app.omap==3) {
            	double colorfraction=(z-zMinLab)/(zMaxLab-zMinLab);
                app.getDetectorView().getView().zmax = zMaxLab;
                app.getDetectorView().getView().zmin = zMinLab;
                Color col = pal.getRange(colorfraction);
                shape.setColor(col.getRed(),col.getGreen(),col.getBlue());              
            }
        }
        
}
