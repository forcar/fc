package org.clas.fcmon.tools;

import java.util.TreeMap;

import org.clas.fcmon.detector.view.DetectorListener;
import org.jlab.io.base.DataEvent;
import org.jlab.io.task.IDataEventListener;

public abstract class DetectorMonitor implements IDataEventListener, DetectorListener{
	
    private String moduleName      = "DetectorMonitor";
    private String moduleVersion   = "0.5";
    private String moduleAuthor    = "lcsmith";
    
    public DetectorMonitor(String name, String version, String author){
        this.moduleName     = name;
        this.moduleVersion  = version;
        this.moduleAuthor   = author;
    }
    
    public abstract void init();
//    public abstract void initDetector();
    public abstract void analyze();
    public abstract void pause();
    public abstract void go();
    public abstract void close();
    public abstract void reset();
    public abstract void writeHipoFile();
    public abstract void readHipoFile();
    public abstract void loadHV(int is1, int is2, int il1, int il2);
    public abstract void initEngine();
    public abstract void initEpics(Boolean isEpics);
    public abstract TreeMap<String,Object> getGlob();
    public abstract void putGlob(String name, Object obj);
    public String getName(){ return moduleName;}

    public void dataEventAction(DataEvent de) {
        // TODO Auto-generated method stub       
    }
	
}
