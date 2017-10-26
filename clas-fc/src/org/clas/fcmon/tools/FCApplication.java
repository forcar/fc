package org.clas.fcmon.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.cc.CCPixels;
import org.clas.fcmon.cnd.CNDPixels;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.ec.ECPixels;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import org.clas.fcmon.ftof.FTOFPixels;
import org.clas.fcmon.ctof.CTOFPixels;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.tasks.CalibrationEngineView;

import org.jlab.groot.group.DataGroup;
import org.jlab.service.ec.ECEngine;
import org.jlab.utils.groups.IndexedList;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

public class FCApplication implements ActionListener  {
	
    ColorPalette palette = new ColorPalette();
    
    private IndexedList<DataGroup>               analysisData = new IndexedList<DataGroup>(1);    
    private String                                    appName = null;
    private List<EmbeddedCanvas>                     canvases = new ArrayList<EmbeddedCanvas>();
    private JPanel                                  radioPane = null;
    public JPanel                                  buttonPane = null;  
    private CalibrationEngineView                   calibPane = null;
    private List<String>                               fields = new ArrayList<String>();
//    private List<FCParameter>                    parameters = new ArrayList<FCParameter>();
    
    public ECPixels[]                                   ecPix = new ECPixels[2];
    public CCPixels                                     ccPix = null;
    public FTOFPixels[]                               ftofPix = null;
    public CTOFPixels[]                               ctofPix = null;
    public CNDPixels[]                                 cndPix = null;
    
	public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new  DetectorCollection<TreeMap<Integer,Object>>();
	public TreeMap<String, DetectorCollection<H1F>>     hmap1 = new TreeMap<String, DetectorCollection<H1F>>();
	public TreeMap<String, DetectorCollection<H2F>>     hmap2 = new TreeMap<String, DetectorCollection<H2F>>();
	 
	public MonitorApp      app = null;
	public DetectorMonitor mon = null;
	
    public TreeMap<String,JPanel>  rbPanes = new TreeMap<String,JPanel>();
    public TreeMap<String,String>  bStore = new TreeMap<String,String>();
	
	public int is,layer,ic;
	public int panel,opt,io,of,lay,l1,l2,is1,is2,iis1,iis2;

    private String             buttonSelect;
    private int                buttonIndex;
    private String             canvasSelect;
    private int                canvasIndex;
    
    double    PCMon_zmin = 0;
    double    PCMon_zmax = 0;    
    public int      omap = 0;
    public int     ilmap = 0;
    
    public int sectorSelected, layerSelected, channelSelected;
    
    public FCApplication(ECPixels[] ecPix) {
        this.ecPix = ecPix;     
    }
    
    public FCApplication(CCPixels ccPix) {
        this.ccPix = ccPix;     
    }
    
    public FCApplication(FTOFPixels[] ftofPix) {
        this.ftofPix = ftofPix;     
    }
    
    public FCApplication(String name) {
        this.appName = name;
        this.addCanvas(name);
    }
     
    public FCApplication(String name, ECPixels[] ecPix) {
        this.appName = name;
        this.ecPix = ecPix;   
        this.addCanvas(name);
    }
    
    public FCApplication(String name, CCPixels ccPix) {
        this.appName = name;
        this.ccPix = ccPix;   
        this.addCanvas(name);
    }
    
    public FCApplication(String name, FTOFPixels[] ftofPix) {
        this.appName = name;
        this.ftofPix = ftofPix;   
        this.addCanvas(name);
    }
    
    public FCApplication(String name, CTOFPixels[] ctofPix) {
        this.appName = name;
        this.ctofPix = ctofPix;   
        this.addCanvas(name);
    }
    
    public FCApplication(String name, CNDPixels[] cndPix) {
        this.appName = name;
        this.cndPix = cndPix;   
        this.addCanvas(name);
    } 
    
    public void setApplicationClass(MonitorApp app) {
        this.app = app;
        app.getDetectorView().addFCApplicationListeners(this);
    }
    
    public void setMonitoringClass(DetectorMonitor mon) {
        this.mon = mon;
    }
    
    public String getName() {
        return this.appName;
    }
    
    public void setName(String name) {
        this.appName = name;
    }	
    
    public IndexedList<DataGroup>  getDataGroup(){
        return analysisData;
    }    
    
	public void getDetIndices(DetectorDescriptor dd) {
        is    = dd.getSector();
        layer = dd.getLayer();
        ic    = dd.getComponent(); 
        io    = dd.getOrder();
        
        panel = omap;
        lay   = 0;
        opt   = 0;
        
        if (panel==1) opt = 1;
        if (layer<7)  lay = layer;
        if (layer==14) lay = 7;
        if (panel==9) lay = panel;
        if (panel>10) lay = panel;
	}
	
	public void addH1DMaps(String name, DetectorCollection<H1F> map) {
		this.hmap1.put(name,map);
	}
	
	public void addH2DMaps(String name, DetectorCollection<H2F> map) {
		this.hmap2.put(name,map);
	}
	
	public void addLMaps(String name, DetectorCollection<TreeMap<Integer,Object>> map) {
		this.Lmap_a=map;
	}
	
	public void analyze() {
	}
	
    public synchronized void analyze(int idet, int is1, int is2, int il1, int il2, int ip1, int ip2) {
    }
    
    public synchronized void analyze(int idet, int is1, int is2, int il1, int il2) {
    }
	
	public void updateCanvas(DetectorDescriptor dd, EmbeddedCanvas canvas) {		
	}
	
    public final void addCanvas(String name) {
        EmbeddedCanvas c = new EmbeddedCanvas();
        this.canvases.add(c);
        this.canvases.get(this.canvases.size()-1).setName(name);
    }
    
    public EmbeddedCanvas getCanvas(){
        return this.canvases.get(0);
    }
    
    public EmbeddedCanvas getCanvas(int index) {
        return this.canvases.get(index);
    }
    
    public EmbeddedCanvas getCanvas(String name) {
        int index=0;
        for(int i=0; i<this.canvases.size(); i++) {
            if(this.canvases.get(i).getName().equals(name)) {
                index=i;
                break;
            }
        }
        return this.canvases.get(index);
    }  
/*    
    public void setCalibPane(CalibrationEngine engine) {
        calibPane = new CalibrationEngineView(engine);
    }
    
    public CalibrationEngineView getCalibPane(){
        return calibPane;
    }
*/
    
    public void mapButtonAction(String group, String name, int key) {
        this.bStore = app.getDetectorView().bStore;
        if (!bStore.containsKey(group)) {
            bStore.put(group,name);
        }else{
            bStore.replace(group,name);
        }
        omap = key;
        if (key>10&&key<14) app.viewIndex=key-10;
//        System.out.println("mapButtonAction omap= "+omap);
        app.getDetectorView().getView().updateGUI();     
    }
    
    public void viewButtonAction(String group, String name, int key) {
        this.bStore = app.getDetectorView().bStore;
        this.rbPanes = app.getDetectorView().rbPanes;
        if(group.equals("LAY")) {
            app.currentView = name;
            if (key<3) app.viewIndex = key+1;
            name = name+Integer.toString(ilmap);
            app.getDetectorView().getView().setLayerState(name, true);    
            if (key<3) {rbPanes.get("PMT").setVisible(true);rbPanes.get("PIX").setVisible(false); omap=app.getDetectorView().getMapKey("PMT",bStore.get("PMT"));}       
//          if (key>2) {rbPanes.get("PIX").setVisible(true);rbPanes.get("PMT").setVisible(false); app.getDetectorView().selectMapButton("PIX",bStore.get("PIX"));}
            if (key>2) {rbPanes.get("PIX").setVisible(true);rbPanes.get("PMT").setVisible(false); omap=app.getDetectorView().getMapKey("PIX",bStore.get("PIX"));}
            
        }
        if(group.equals("DET")) {
            ilmap = key;  
            app.detectorIndex = key;
            name = app.currentView+Integer.toString(ilmap);  
            app.getDetectorView().getView().setLayerState(name, true);
        }
        app.getDetectorView().getView().updateGUI();        
    }  
    
    public void setRadioButtons() {
        this.radioPane.setLayout(new FlowLayout());
        ButtonGroup bG = new ButtonGroup();
        for (String field : this.fields) {
            String item = field;
            JRadioButton b = new JRadioButton(item);
            if(bG.getButtonCount()==0) b.setSelected(true);
            b.addActionListener(this);
            this.radioPane.add(b);
            bG.add(b);
        }
    }  
    
    public void actionPerformed(ActionEvent e) {
      buttonSelect=e.getActionCommand();
      for(int i=0; i<this.fields.size(); i++) {
          if(buttonSelect.equals(this.fields.get(i))) {
              buttonIndex=i;
              break;
          }
      }
    }
    
    public void setRadioPane(JPanel radioPane) {
        this.radioPane = radioPane;
    }

    public JPanel getRadioPane() {
        radioPane = new JPanel();
        return radioPane;
    }
    
    public String getButtonSelect() {
        return buttonSelect;
    }
    
    public int getButtonIndex() {
        return buttonIndex;
    }
    
    public void setButtonPane(JPanel buttonPane) {
        this.buttonPane = buttonPane;
    }
    
    public JPanel getButtonPane(){
        buttonPane = new JPanel();
        return buttonPane;
    }
    
    public String getCanvasSelect() {
        if(canvasSelect == null) {
            canvasIndex  = 0;
            canvasSelect = this.canvases.get(0).getName();
        }
        return canvasSelect;
    }
    
    public void setCanvasSelect(String name) {
        canvasIndex  = 0;
        canvasSelect = this.canvases.get(0).getName();
        for(int i=0; i<canvases.size(); i++) {
            if(canvases.get(i).getName().equals(name)) {
                canvasIndex = i;
                canvasSelect = name;
                break;
            }
        }
    }
    
    public void setCanvasIndex(int index) {
        if(index>=0 && index < this.canvases.size()) {
            canvasIndex  = index;
            canvasSelect = this.canvases.get(index).getName();
        }
        else {
            canvasIndex  = 0;
            canvasSelect = this.canvases.get(0).getName();
        }
    }
    
    public EmbeddedCanvas canvasConfig(EmbeddedCanvas canvas, int num, double xmin, double xmax, double ymin, double ymax, boolean linlog) {
        canvas.cd(num);        
        if (xmin!=xmax) canvas.getPad(num).getAxisX().setRange(xmin,xmax);
        if (ymin!=ymax) canvas.getPad(num).getAxisY().setRange(ymin,ymax);
        if (ymin==ymax) canvas.getPad(num).getAxisY().setAutoScale(true);
        canvas.getPad(num).getAxisFrame().getAxisZ().setLog(linlog);
        double zmin =  100*app.displayControl.pixMin;      
        double zmax = 4000*app.displayControl.pixMax;      
        if (zmax<4000)  canvas.getPad(num).getAxisZ().setRange(zmin,zmax);
        if (zmax==4000) canvas.getPad(num).getAxisZ().setAutoScale(true);            
        return canvas;
    } 
    
    public Boolean isGoodTrigger(int is) {return (app.isTB)? is==app.bitsec:true;}
           
    public Boolean isGoodSector(int is) {return is>=is1&&is<is2&&isGoodTrigger(is);}      
     
}
