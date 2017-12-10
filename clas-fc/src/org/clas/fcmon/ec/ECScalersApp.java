package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCEpics;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;

import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.ui.RangeSlider;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import java.io.EOFException;
import java.io.IOException;

public class ECScalersApp extends FCEpics {
   
    public DetectorCollection<H1F> H1_SCA = new DetectorCollection<H1F>();
    public DetectorCollection<H2F> H2_SCA = new DetectorCollection<H2F>();
    float norm1[] = null;
    float norm2[] = null;
    float norm3[][] = new float[9][68];
    float norm4[][] = new float[9][68];
    float  rate[][] = new float[9][68];
    double ca_fc=0, ca_2c24a=0, ca_2h01=0;
    
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
    
    ECScalersApp(String name, String det) {
        super(name, det);
    }
    
    public void init() {
        this.is1=ECConstants.IS1; 
        this.is2=ECConstants.IS2; 
        setPvNames(this.detName,1);
        setPvNames(this.detName,2);
        sectorSelected=is1;
        layerSelected=1;
        channelSelected=1;
        initHistos(); 
        getScalerMap(app.scalerPath+"ecal_occupancy_cut.txt");
    }
    
    public void startEPICS() {
        createContext();
        setCaNames(this.detName,1);
        setCaNames(this.detName,2);
        setCaNames(this.detName,3);
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
        slider.setMinimum((int)  0.);
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
        System.out.println("ECScalersApp.initHistos():");
        for (int is=is1; is<is2 ; is++) {
            for (int il=1 ; il<layMap.get(detName).length+1 ; il++){
                int nb=nlayMap.get(detName)[il-1]; int mx=nb+1;
                H1_SCA.add(is, il, 0, new H1F("HV_dsc2"+is+"_"+il, nb,1,mx));                
                H1_SCA.add(is, il, 1, new H1F("HV_fadc"+is+"_"+il, nb,1,mx));                               
                H1_SCA.add(is, il, 2, new H1F("HV_dsc2_sum"+is+"_"+il, nb,1,mx));                               
                H1_SCA.add(is, il, 3, new H1F("HV_fadc_sum"+is+"_"+il, nb,1,mx));                               
                H2_SCA.add(is, il, 0, new H2F("HV_dsc2"+is+"_"+il, nb,1,mx,nmax,0,nmax));                
                H2_SCA.add(is, il, 1, new H2F("HV_fadc"+is+"_"+il, nb,1,mx,nmax,0,nmax));                               
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
                    connectCa(1,"cTdc",is,il,ic);
                    connectCa(2,"c",is,il,ic);
                }
            }
        }
        app.fifo4.add(0,0,1,new LinkedList<Double>()); connectCa(3,"BEAM",0,0,1);
        app.fifo4.add(0,0,2,new LinkedList<Double>()); connectCa(3,"BEAM",0,0,2);
        app.fifo4.add(0,0,3,new LinkedList<Double>()); connectCa(3,"BEAM",0,0,3);
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
                    app.fifo4.get(is, il, ic).add(getCaValue(1,"cTdc",is, il, ic));
                    app.fifo5.get(is, il, ic).add(getCaValue(2,"c",is, il, ic));
                }
            }
         }
        app.fifo4.get(0,0,1).add(getCaValue(3,"BEAM",0,0,1));
        app.fifo4.get(0,0,2).add(getCaValue(3,"BEAM",0,0,2));
        app.fifo4.get(0,0,3).add(getCaValue(3,"BEAM",0,0,3));
               
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
        ca_fc    = app.fifo4.get(0,0,1).getLast(); 
        ca_2c24a = app.fifo4.get(0,0,2).getLast();
        ca_2h01  = app.fifo4.get(0,0,3).getLast();
                
    }
    
    public synchronized void updateScalers(int flag) {
        update1DScalers(engine1DCanvas.getCanvas("Scalers"),flag);   
        update2DScalers(engine2DCanvas.getCanvas("Stripcharts"),flag);        
    }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        sectorSelected  = dd.getSector(); 
        layerSelected   = dd.getLayer();
        channelSelected = dd.getComponent();   
        
        updateScalers(0);
        
        isCurrentSector = sectorSelected;
        isCurrentLayer  = layerSelected;
    }
    
    public synchronized void update1DScalers(EmbeddedCanvas canvas, int flag) {
        
        H1F h = new H1F();
        H1F c = new H1F();
        
        int is = sectorSelected;
        int lr = layerSelected+3*app.detectorIndex;
        int ip = channelSelected; 
        
        if (layerSelected>3||lr==0||lr>layMap.get(detName).length) return;
        
        canvas.divide(2, 1);
//        canvas.getPad(0).getAxisY().setAutoScale(false);
//        canvas.getPad(1).getAxisY().setAutoScale(false);
        canvas.getPad(0).getAxisY().setLog(isLogy); 
        canvas.getPad(1).getAxisY().setLog(isLogy); 
        canvas.getPad(0).getAxisY().getAttributes().setAxisMinimum(isLogy?1.0:0.0);
//        canvas.getPad(0).getAxisY().getAttributes().setAxisMaximum(isLogy?1500.0:1200.0);
        canvas.getPad(1).getAxisY().getAttributes().setAxisMinimum(isLogy?1.0:0.0);
//        canvas.getPad(1).getAxisY().getAttributes().setAxisMaximum(isLogy?1500.0:1200.0);
                
        String titx = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
        
        String tit = "FC: "+String.format("%5.2f",ca_fc)+"   2C24A: "+ca_2c24a+"   2H01: "+ca_2h01;
        
        h = H1_SCA.get(is, lr, 0); h.setTitleX(titx); h.setTitle(tit); h.setTitleY("DSC2 RATE (HZ)");
        h.setFillColor(32); canvas.cd(0); canvas.draw(h);

        h = H1_SCA.get(is, lr, 1); h.setTitleX(titx); h.setTitle(tit); h.setTitleY("FADC RATE (HZ)");
        h.setFillColor(32); canvas.cd(1); canvas.draw(h); 
        
        H1F hbeam = histScalerMap(h,lr,ca_fc*1e3/75.);
        canvas.draw(hbeam,"same");
        
        c = H1_SCA.get(is, lr, 0).histClone("Copy"); c.reset() ; 
        c.setBinContent(ip, H1_SCA.get(is, lr, 0).getBinContent(ip));
        c.setFillColor(2);  canvas.cd(0); canvas.draw(c,"same");
        
        c = H1_SCA.get(is, lr, 1).histClone("Copy"); c.reset() ; 
        c.setBinContent(ip, H1_SCA.get(is, lr, 1).getBinContent(ip));
        c.setFillColor(2);  canvas.cd(1); canvas.draw(c,"same");
        
        canvas.repaint();
    }
    
    public synchronized void update2DScalers(EmbeddedCanvas canvas, int flag) {
        
        H2F h = new H2F();
        
        int is = sectorSelected;
        int lr = layerSelected+3*app.detectorIndex;
        
        if (layerSelected>3||lr==0||lr>layMap.get(detName).length) return;
        
        //Don't redraw unless timer fires or new sector selected
//        if (flag==0&&lr==isCurrentLayer) return;  
        
        canvas.divide(2, 1);
        
//        canvas.getPad(0).getAxisY().setLog(false);
//        canvas.getPad(1).getAxisY().setLog(false); 
        
        canvas.getPad(0).getAxisZ().setLog(isLogz); 
        canvas.getPad(0).getAxisZ().setRange(zMinLab,zMaxLab);
        canvas.getPad(1).getAxisZ().setLog(isLogz); 
        canvas.getPad(1).getAxisZ().setRange(zMinLab,zMaxLab);
        if (isNorm) {
            canvas.getPad(0).getAxisZ().setLog(false); 
            canvas.getPad(1).getAxisZ().setLog(false); 
            canvas.getPad(0).getAxisZ().setRange(-8.0,8.0);
            canvas.getPad(1).getAxisZ().setRange(-8.0,8.0);
        }
        String tit = "Sector "+is+" "+layMap.get(detName)[lr-1]+" PMT";
        
        h = H2_SCA.get(is, lr, 0); h.setTitleX(tit); h.setTitleY("TIME");
        canvas.cd(0); canvas.draw(h);
        h = H2_SCA.get(is, lr, 1); h.setTitleX(tit); h.setTitleY("TIME");
        canvas.cd(1); canvas.draw(h);

        canvas.repaint();
        
        isCurrentSector = is;
        isCurrentLayer  = lr;
        
    }
    
    public H1F histScalerMap(H1F h, int layer, double scale) {
    	   
       	H1F c = h.histClone("Copy"); c.reset() ;
       	for (int i=0; i<h.getAxis().getNBins(); i++) c.setBinContent(i, rate[layer-1][i]*scale);
       	return c;
    }
    
    public void getScalerMap(String filename) {   
        
        try{
            FileReader       file = new FileReader(filename);
            BufferedReader reader = new BufferedReader(file);
            int n = 0 ;
            while (n<420) {
              String line = reader.readLine();
              String[] col = line.trim().split("\\s+"); 
              int i = Integer.parseInt(col[0]); int j = Integer.parseInt(col[1]);
              rate[i-1][j-1] = Float.parseFloat(col[2]);
              n++;
            }    
            reader.close();
            file.close();
         }  
         
         catch(FileNotFoundException ex) {
            ex.printStackTrace();            
         }     
         catch(IOException ex) {
             ex.printStackTrace();
         }
        
         System.out.println("Exiting getScalerMap()");

    }
              
    
}
