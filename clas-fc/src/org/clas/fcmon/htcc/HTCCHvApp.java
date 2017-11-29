package org.clas.fcmon.htcc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.ColorPalette;
import org.clas.fcmon.tools.FCEpics;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;

import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class HTCCHvApp extends FCEpics implements ActionListener {
    
    JTextField   newhv = new JTextField(4);
    JLabel statuslabel = new JLabel();   
    
    DetectorCollection<H1F> H1_HV = new DetectorCollection<H1F>();
    DetectorCollection<H2F> H2_HV = new DetectorCollection<H2F>();
    
    updateGUIAction action = new updateGUIAction();
    
    Timer timer = null;
    boolean epicsEnabled = false;
    int delay=2000;
    int nfifo=0, nmax=120;
    int isCurrentSector;
    int isCurrentLayer;
    double newHV=0;
    
    HTCCHvApp(String name, String det) {
        super(name, det);
    }
    
    public void init() {
        this.is1=HTCCConstants.IS1;
        this.is2=HTCCConstants.IS2;
        setPvNames(this.detName,0);
        sectorSelected=is1;
        layerSelected=1;
        channelSelected=1;
        initHistos();
    }
    
    public void startEPICS() {
    	System.out.println("HTCCHvApp: Connect to EPICS Channel Access");
    	clearMaps(); nfifo=0; 
        createContext();
        setCaNames(this.detName,0);
        initFifos();
        this.timer = new Timer(delay,action);  
        this.timer.setDelay(delay);
        this.timer.start();           	
   }
    
    public void stopEPICS() {
    	System.out.println("HTCCHvApp: Disconnect from EPICS Channel Access");
    	epicsEnabled = false;
        if(this.timer.isRunning()) this.timer.stop();
        destroyContext();
    }   
    
    public JPanel getPanel() {        
        engineView.setLayout(new BorderLayout());
        engineView.add(getEnginePane(),BorderLayout.CENTER);
        engineView.add(getButtonPane(),BorderLayout.PAGE_END);
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
        engine1DCanvas = new EmbeddedCanvasTabbed("HV");
        engine1DView.add(engine1DCanvas,BorderLayout.CENTER);
        return engine1DView;
    }
    
    public JPanel getEngine2DView() {
        engine2DView.setLayout(new BorderLayout());
        engine2DCanvas = new EmbeddedCanvasTabbed("Stripcharts");
        engine2DView.add(engine2DCanvas,BorderLayout.CENTER);
        return engine2DView;        
    }   
    
    public JPanel getButtonPane() {
        buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        
        JButton loadBtn = new JButton("Load HV");
        loadBtn.addActionListener(this);
        buttonPane.add(loadBtn); 

        buttonPane.add(new JLabel("New HV:"));
        newhv.setActionCommand("NEWHV"); newhv.addActionListener(this); newhv.setText("0");  
        buttonPane.add(newhv); 
        
        statuslabel = new JLabel(" ");         
        buttonPane.add(statuslabel);
        
        return buttonPane;
    }
    
    private class updateGUIAction implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            fillFifos();
            fillHistos(); 
            update1DScalers(engine1DCanvas.getCanvas("HV"),1);   
            update2DScalers(engine2DCanvas.getCanvas("Stripcharts"),1); 
       }
    } 
    
    public void initHistos() {       
        System.out.println("HTCCHvApp.initHistos():");
        for (int is=is1; is<is2 ; is++) {
            for (int il=1 ; il<layMap.get(detName).length+1 ; il++){
                int nb=nlayMap.get(detName)[il-1]; int mx=nb+1;
                H1_HV.add(is, il, 0, new H1F("HV_vset"+is+"_"+il, nb,1,mx));                
                H1_HV.add(is, il, 1, new H1F("HV_vmon"+is+"_"+il, nb,1,mx));                
                H1_HV.add(is, il, 2, new H1F("HV_imon"+is+"_"+il, nb,1,mx));                
                H2_HV.add(is, il, 0, new H2F("HV_vset"+is+"_"+il, nb,1,mx,nmax,0,nmax));                
                H2_HV.add(is, il, 1, new H2F("HV_vmon"+is+"_"+il, nb,1,mx,nmax,0,nmax));                
                H2_HV.add(is, il, 2, new H2F("HV_imon"+is+"_"+il, nb,1,mx,nmax,0,nmax));                
            }
        }
    }
        
    public void initFifos() {
        System.out.println("HTCCHvApp.initFifos():");
        app.fifo1.clear(); app.fifo2.clear(); app.fifo3.clear(); app.fifo6.clear();
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {
                    app.fifo1.add(is, il, ic, new LinkedList<Double>());
                    app.fifo2.add(is, il, ic, new LinkedList<Double>());
                    app.fifo3.add(is, il, ic, new LinkedList<Double>());
                    app.fifo6.add(is, il, ic, new LinkedList<Double>());
                    connectCa(0,"vset",is,il,ic);
                    connectCa(0,"vmon",is,il,ic);
                    connectCa(0,"imon",is,il,ic);
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
                        app.fifo1.get(is, il, ic).removeFirst();
                        app.fifo2.get(is, il, ic).removeFirst();
                        app.fifo3.get(is, il, ic).removeFirst();
                    }
                    app.fifo1.get(is, il, ic).add(getCaValue(0,"vset",is, il, ic));
                    app.fifo2.get(is, il, ic).add(getCaValue(0,"vmon",is, il, ic));
                    app.fifo3.get(is, il, ic).add(getCaValue(0,"imon",is, il, ic));
                }
            }
         }
       // System.out.println("time= "+(System.currentTimeMillis()-startTime));
        epicsEnabled = true;
    }

    public void fillHistos() {
        
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(detName).length+1 ; il++) {
                H1_HV.get(is, il, 0).reset(); H2_HV.get(is, il, 0).reset();
                H1_HV.get(is, il, 1).reset(); H2_HV.get(is, il, 1).reset();
                H1_HV.get(is, il, 2).reset(); H2_HV.get(is, il, 2).reset();
                for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {                    
                    H1_HV.get(is, il, 0).fill(ic,app.fifo1.get(is, il, ic).getLast());
                    H1_HV.get(is, il, 1).fill(ic,app.fifo2.get(is, il, ic).getLast());
                    H1_HV.get(is, il, 2).fill(ic,app.fifo3.get(is, il, ic).getLast());
                    Double ts1[] = new Double[app.fifo1.get(is, il, ic).size()];
                    app.fifo1.get(is, il, ic).toArray(ts1);
                    Double ts2[] = new Double[app.fifo2.get(is, il, ic).size()];
                    app.fifo2.get(is, il, ic).toArray(ts2);
                    Double ts3[] = new Double[app.fifo3.get(is, il, ic).size()];
                    app.fifo3.get(is, il, ic).toArray(ts3);
                    for (int it=0; it<ts1.length; it++) {
                        H2_HV.get(is, il, 0).fill(ic,it,ts1[it]);
                        H2_HV.get(is, il, 1).fill(ic,it,ts2[it]);
                        H2_HV.get(is, il, 2).fill(ic,it,ts3[it]);
                    }
                }
            }
        }
        
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        System.out.println("HTCCHvApp.loadHV()");
        for (int is=is1; is<is2 ; is++) {
            for (int il=il1; il<il2 ; il++) {
                for (int ic=1; ic<nlayMap.get(detName)[il-1]+1; ic++) {
                    System.out.println("is="+is+" il="+il+" ic="+ic+" HV="+app.fifo6.get(is, il, ic).getLast());
                    putCaValue(0,"vset",is,il,ic,app.fifo6.get(is, il, ic).getLast());  
                }
            }
        }
        
    }   
    
    public void updateStatus(int is, int il, int ic) {
//      int vset = (int)getCaValue(0,"vset",is, il, ic);
//      int vmon = (int)getCaValue(0,"vmon",is, il, ic);
//      int imon = (int)getCaValue(0,"imon",is, il, ic);
      double vset = app.fifo1.get(is,il,ic).getLast();
      double vmon = app.fifo2.get(is,il,ic).getLast(); 
      double imon = app.fifo3.get(is,il,ic).getLast(); 
      this.statuslabel.setText(" Sector:"+is+"  SuperLayer:" +il+"  PMT:"+ic+"  Vset:"+(int)vset+"  Vmon:"+(int)vmon+"  Imon:"+(int)imon);        
    }  
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        sectorSelected  = dd.getSector();
        layerSelected   = dd.getOrder()+1;
        channelSelected = dd.getComponent(); 
        
        update1DScalers(engine1DCanvas.getCanvas("HV"),0);   
        update2DScalers(engine2DCanvas.getCanvas("Stripcharts"),0);
        
        if (epicsEnabled) updateStatus(sectorSelected,layerSelected+2*app.detectorIndex,channelSelected+1);

        isCurrentSector = sectorSelected;
        isCurrentLayer  = layerSelected;
    }
    
    public void update1DScalers(EmbeddedCanvas canvas, int flag) {
        
        H1F h = new H1F();
        H1F c = new H1F();
        
        int is = sectorSelected;
        int lr = layerSelected;
        int ip = channelSelected; 
        
        if (lr==0||lr>layMap.get(detName).length) return;
        
        int off = 2*app.detectorIndex;
             
        canvas.divide(4, 1);
        
        h = H1_HV.get(is, 1+off, 0); h.setTitleX("Sector "+is+" L PMT"); h.setTitleY("VOLTS");
        h.setFillColor(33); canvas.cd(0); canvas.draw(h);
        h = H1_HV.get(is, 2+off, 0); h.setTitleX("Sector "+is+" R PMT"); h.setTitleY("VOLTS");
        h.setFillColor(33); canvas.cd(1);    canvas.draw(h);
        
        h = H1_HV.get(is, 1+off, 1); h.setTitleX("Sector "+is+" L PMT"); h.setTitleY("VOLTS");
        h.setFillColor(32); canvas.cd(0); canvas.draw(h,"same");
        h = H1_HV.get(is, 2+off, 1); h.setTitleX("Sector "+is+" R PMT"); h.setTitleY("VOLTS");
        h.setFillColor(32); canvas.cd(1);    canvas.draw(h,"same");

        h = H1_HV.get(is, 1+off, 2); h.setTitleX("Sector "+is+" L PMT"); h.setTitleY("MICROAMPS");
        h.setFillColor(32); canvas.cd(2); canvas.draw(h);
        h = H1_HV.get(is, 2+off, 2); h.setTitleX("Sector "+is+" R PMT"); h.setTitleY("MICROAMPS");
        h.setFillColor(32); canvas.cd(3); canvas.draw(h);
        
        c = H1_HV.get(is, lr+off, 0).histClone("Copy"); c.reset() ; 
        c.setBinContent(ip, H1_HV.get(is, lr+off, 0).getBinContent(ip));
        c.setFillColor(2);  canvas.cd(lr-1); canvas.draw(c,"same");
        
        c = H1_HV.get(is, lr+off, 2).histClone("Copy"); c.reset() ; 
        c.setBinContent(ip, H1_HV.get(is, lr+off, 2).getBinContent(ip));
        c.setFillColor(2);  canvas.cd(lr-1+2); canvas.draw(c,"same");
        
        canvas.repaint();
    }
    
    public void update2DScalers(EmbeddedCanvas canvas, int flag) {
        
        H2F h = new H2F();
        
        int is = sectorSelected;
        int lr = layerSelected;
                
        if (lr==0||lr>layMap.get(detName).length) return;
        
        //Don't redraw unless timer fires or new sector selected
        if (flag==0&&(is==isCurrentSector)) return;  
        
        int off = 2*app.detectorIndex;
        
        canvas.divide(4, 1);

        h = H2_HV.get(is, 1+off, 0); h.setTitleX("Sector "+is+" L PMT"); h.setTitleY("TIME");
        canvas.cd(0); canvas.draw(h);
        h = H2_HV.get(is, 2+off, 0); h.setTitleX("Sector "+is+" R PMT"); h.setTitleY("TIME");
        canvas.cd(1);    canvas.draw(h);

        h = H2_HV.get(is, 1+off, 2); h.setTitleX("Sector "+is+" L PMT"); h.setTitleY("TIME");
        canvas.cd(2); canvas.draw(h);
        h = H2_HV.get(is, 2+off, 2); h.setTitleX("Sector "+is+" R PMT"); h.setTitleY("TIME");
        canvas.cd(3); canvas.draw(h);
        
        canvas.repaint();
        
        isCurrentSector = is;
        
    }
    
    public void updateDetectorView(DetectorShape2D shape) {
    	
        ColorPalette palette3 = new ColorPalette(3);
        ColorPalette palette4 = new ColorPalette(4);
                   
        ColorPalette pal = palette4;
        
        DetectorDescriptor dd = shape.getDescriptor(); 
        
        int is = dd.getSector();  
        int il = dd.getOrder()+1;
        int ip = dd.getComponent(); 
                    
        float z = (float) H1_HV.get(is, il, 2).getBinContent(ip) ;
        float zmin = 300 ; float zmax = 400;
        if (app.omap==3) {
        	double colorfraction=(z-zmin)/(zmax-zmin);
            app.getDetectorView().getView().zmax = zmin;
            app.getDetectorView().getView().zmin = zmax;
            Color col = pal.getRange(colorfraction);
            shape.setColor(col.getRed(),col.getGreen(),col.getBlue());              
        }
    }   
    
    @Override
    public void actionPerformed(ActionEvent e) {
        int is = sectorSelected;
        int lr = layerSelected+2*app.detectorIndex;
        int ip = channelSelected+1; 
        if(e.getActionCommand().compareTo("Load HV")==0) putCaValue(0,"vset",is,lr,ip,newHV);
        if(e.getActionCommand().compareTo("NEWHV")==0)   newHV = Double.parseDouble(newhv.getText());
        
    }    
    
}
