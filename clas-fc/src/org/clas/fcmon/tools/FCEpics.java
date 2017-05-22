package org.clas.fcmon.tools;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.Timer;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.epics.ca.Channel;
import org.epics.ca.Context;
import org.epics.ca.Listener;
import org.epics.ca.Monitor;
import org.epics.ca.Status;
import org.epics.ca.data.Alarm;
import org.epics.ca.data.Control;
import org.epics.ca.data.Graphic;
import org.epics.ca.data.GraphicEnum;
import org.epics.ca.data.Timestamped;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.utils.groups.IndexedList;

import org.jlab.groot.graphics.EmbeddedCanvas;

public class FCEpics  {
    
    public String      appName = null;
    public String      detName = null;

    public MonitorApp      app = null;
    public DetectorMonitor mon = null;
    public Context     context = null;
    
    public Monitor<Double>     monitor = null;
    
    public JPanel                   engineView = new JPanel();    
    public JSplitPane               enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    public JPanel                 engine1DView = new JPanel();
    public JPanel                 engine2DView = new JPanel();
    public EmbeddedCanvasTabbed engine1DCanvas = null;
    public EmbeddedCanvasTabbed engine2DCanvas = null;
    public JPanel                   buttonPane = null;
    public JPanel                   sliderPane = null;
    
    IndexedList<String>                             map = new IndexedList<String>(4);
    TreeMap<String,IndexedList<String>>           pvMap = new TreeMap<String,IndexedList<String>>();
    TreeMap<String,IndexedList<Channel<Double>>>  caMap = new TreeMap<String,IndexedList<Channel<Double>>>();
    public TreeMap<String,String[]>              layMap = new TreeMap<String,String[]>();
    public TreeMap<String,int[]>                nlayMap = new TreeMap<String,int[]>();
   
    String   grps[] = {"HV","DISC","FADC"};
    String   ltcc[] = {"L","R"};
    String   ftof[] = {"PANEL1A_L","PANEL1A_R","PANEL1B_L","PANEL1B_R","PANEL2_L","PANEL2_R"};
    String     ec[] = {"U","V","W","UI","VI","WI","UO","VO","WO"};
    int     nltcc[] = {18,18};
    int     nftof[] = {23,23,62,62,5,5};
    int       nec[] = {68,62,62,36,36,36,36,36,36};
    
    public int is1,is2;
    public int sectorSelected, layerSelected, channelSelected;
    public Boolean online = false;
    
	public FCEpics(String name, String det){
	    System.out.println("Initializing detector "+det);
	    this.appName = name;
	    this.detName = det;
        this.layMap.put("LTCC",ltcc); this.nlayMap.put("LTCC", nltcc);
        this.layMap.put("FTOF",ftof); this.nlayMap.put("FTOF", nftof);
        this.layMap.put("EC",ec);     this.nlayMap.put("EC", nec);
	}
	
	public int createContext() {
	    if (!online) return 0;
	    this.context = new Context();
	    return 1;
	}
	
	public int destroyContext() {
        if (!online) return 0;
	    this.context.close();
	    return 1;
	}
	
    public void setApplicationClass(MonitorApp app) {
        this.app = app;
    }
    
    public void setMonitoringClass(DetectorMonitor mon) {
        this.mon = mon;
    }
    
    public String getName() {
        return this.appName;
    }
    
    public int connectCa(int grp, String action, int sector, int layer, int channel) {
        if (!online) return 0;
        try {
        //System.out.println("Connecting to grp "+grp+" sector "+sector+" layer "+layer+" channel "+channel);
        caMap.get(action).getItem(grp,sector,layer,channel).connectAsync().get(1,TimeUnit.MILLISECONDS);  //org.epics.ca
        }
        catch (InterruptedException e) {  
            return -1;
        }        
        catch (TimeoutException e) {  
            return -1;
        }        
        catch (ExecutionException e) {  
            return -1;
        }
        return 1;
        
    }
    
    public double getCaValue(int grp, String action, int sector, int layer, int channel) {
        if (!online) return 1000.0;
        try {
        CompletableFuture<Double> ffd = caMap.get(action).getItem(grp,sector,layer,channel).getAsync(); //org.epics.ca
        return ffd.get(); 
        }
        catch (InterruptedException e) {  
            return -1.0;
        }        
        catch (ExecutionException e) {  
            return -1.0;
        }   
    }
    
    public int putCaValue(int grp, String action, int sector, int layer, int channel, double value) {
        if(!online) return 0;
        caMap.get(action).getItem(grp,sector,layer,channel).putNoWait(value); //org.epics.ca  
        return 1;
    } 
    
    public void startMonitor(int grp, String action, int sector, int layer, int channel) {
        this.monitor = caMap.get(action).getItem(grp,sector,layer,channel).addValueMonitor(value->System.out.println(value));
    }
    
    public void stopMonitor(){
        this.monitor.close();
    }
    
    public void setCaNames(String det, int grp) {
        switch (grp) {
        case 0:
        setCaActionNames(det,grp,"vmon");
        setCaActionNames(det,grp,"imon");
        setCaActionNames(det,grp,"vset");     
        break;
        case 1:
        setCaActionNames(det,grp,"c3"); 
        break;  
        case 2: 
        setCaActionNames(det,grp,"c1"); 
        }
    }
    
    public int setCaActionNames(String det, int grp, String action) {
        
        if (!online) return 0;
        
        IndexedList<Channel<Double>> map = new IndexedList<Channel<Double>>(4);
        
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(det).length+1; il++) {
                for (int ic=1; ic<nlayMap.get(det)[il-1]+1; ic++) {
                    String pv = getPvName(grp,action,is,il,ic);
                    map.add(context.createChannel(pv, Double.class),grp,is,il,ic); //org.epics.ca
                }
            }
        } 
        caMap.put(action,map);
        return 1;
    }
    
    public void setPvNames(String det, int grp) {
        switch (grp) {
            case 0:
            setPvActionNames(det,grp,"vmon");
            setPvActionNames(det,grp,"imon");
            setPvActionNames(det,grp,"vset");
            setPvActionNames(det,grp,"pwonoff"); 
            break;
            case 1:  
            setPvActionNames(det,grp,"c3"); break;
            case 2: 
            setPvActionNames(det,grp,"c1"); break;
        }
    }
    
    public String getPvName(int grp, String action, int sector, int layer, int channel) {
        switch (grp) {
        case 0:
        switch (action) {
        case    "vmon": return (String) pvMap.get(action).getItem(grp,sector,layer,channel);
        case    "imon": return (String) pvMap.get(action).getItem(grp,sector,layer,channel); 
        case    "vset": return (String) pvMap.get(action).getItem(grp,sector,layer,channel); 
        case "pwonoff": return (String) pvMap.get(action).getItem(grp,sector,layer,channel); 
        }
        break;
        case 1:  
                        return (String) pvMap.get(action).getItem(grp,sector,layer,channel);
        case 2: 
                        return (String) pvMap.get(action).getItem(grp,sector,layer,channel);
        }
        return "Invalid action";
    }
    
    public void setPvActionNames(String det, int grp, String action) {
      
        IndexedList<String> map = new IndexedList<String>(4);
        for (int is=is1; is<is2 ; is++) {
            for (int il=1; il<layMap.get(det).length+1 ; il++) {
                for (int ic=1; ic<nlayMap.get(det)[il-1]+1; ic++) {
                    map.add(getPvString(det,grp,is,il,ic,action),grp,is,il,ic);
                }
            }
        }
        pvMap.put(action,map);
    }
    
    public String layToStr(String det, int layer) {
        return layMap.get(det)[layer-1];
    }	
    
	public String chanToStr(int channel) {
	    return (channel<10 ? "0"+Integer.toString(channel):Integer.toString(channel));
	}
	
	public String detAlias(String det, int layer) {
	    switch (det) {
	    case "LTCC": return det;
	    case "FTOF": return det;
	    case   "EC": return (layer<4) ? "PCAL":"ECAL";
	    }
	    return "";
	}
	
	public String getPvString(String det, int grp, int sector, int layer, int channel, String action) {
	    String pv = "B_DET_"+detAlias(det,layer)+"_"+grps[grp]+"_SEC"+sector+"_"+layToStr(det,layer)+"_E"+chanToStr(channel);
	    return pv+":"+action;
	} 
    
}
