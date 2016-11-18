/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fcmon.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jlab.detector.base.DetectorCollection;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.DataSetSerializer;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.ui.TBrowser;
import org.jlab.hipo.data.HipoEvent;
import org.jlab.hipo.io.HipoReader;

/**
 *
 * @author fanchini
 * @modified by lcsmith
 */
public class HipoFile {
    
    private final TreeMap<String,Object>  map = new TreeMap<String,Object>();
    private final TDirectory            hdirw = new TDirectory();
    private final TDirectory            hdirr = new TDirectory();
    
    private final String                 file;
    private final TreeMap<String,Object> map_extracted;
    
    public HipoFile(String filename){
        file = filename;
        map_extracted = new TreeMap<String,Object>();
        File filein = new File(filename);
        if(!this.map_extracted.isEmpty())this.map_extracted.clear();
        if(filein.exists()){
            System.out.println("Reading Hipo file: "+this.file);  
            this.readFile(filename);
            this.fillPathMaps();
        }        
    }
   
    public void addToMap(String str, Object h){
        String key=str;
        Object obj = null;
        if(!this.map.isEmpty()){
            for(Map.Entry<String,Object> subdir : this.map.entrySet()){
                if(subdir.getKey().equals(str)){                    
                    ArrayList alnew = new ArrayList();
                    if(subdir.getValue() instanceof ArrayList){
                        ArrayList al = (ArrayList) subdir.getValue();
                        for(int i=0; i<al.size(); i++){alnew.add(al.get(i));}
                    }
                    else 
                    key=str;
                    alnew.add(h);
                    obj=alnew.clone();
                    this.map.remove(key);
                }
                else {
                    key=str;
                    obj=h;
                }
            } 
        }
        else {
            key=str;
            obj=h;             
        }
        this.map.put(key,obj);
    }

    public void writeHipoFile(String filename){
        System.out.println("HipoFile: WRITING TO FILE: "+filename);
        for(Map.Entry<String,Object> subdir : this.map.entrySet()){
            String dirName = subdir.getKey();
            hdirw.cd();
            hdirw.mkdir(dirName);
            hdirw.cd(dirName);
             if(subdir.getValue() instanceof DetectorCollection){
                 DetectorCollection dc = (DetectorCollection) subdir.getValue();
                 Set<Integer> Sectors = dc.getSectors();
                 for(Integer is : Sectors) {
                	 Set<Integer> Layers = dc.getLayers(is);
                	 	for(Integer il : Layers) {
                	 		for(Object obj : dc.getComponents(is,il)){
                	 			int ii = (Integer)obj;
                	 			if(dc.get(is,il,ii) instanceof H1F) hdirw.addDataSet((H1F)dc.get(is,il,ii));
                	 			if(dc.get(is,il,ii) instanceof H2F) hdirw.addDataSet((H2F)dc.get(is,il,ii));
                	 			if(dc.get(is,il,ii) instanceof GraphErrors) hdirw.addDataSet((GraphErrors)dc.get(is,il,ii));
                	 		}
                	 	}
                 }
             }
             
            if(subdir.getValue() instanceof ArrayList){
                    ArrayList al = (ArrayList)subdir.getValue();
                    for(int k=0; k<al.size(); k++){
                        if(al.get(k) instanceof H1F) hdirw.addDataSet((H1F)al.get(k));
                        if(al.get(k) instanceof H2F) hdirw.addDataSet((H2F)al.get(k));
                        if(al.get(k) instanceof GraphErrors) hdirw.addDataSet((GraphErrors)al.get(k));
                    }
             }
            
             else {
                    if(subdir.getValue() instanceof H1F) hdirw.addDataSet((H1F)subdir.getValue());
                    if(subdir.getValue() instanceof H2F) hdirw.addDataSet((H2F)subdir.getValue());
                    if(subdir.getValue() instanceof GraphErrors) hdirw.addDataSet((GraphErrors)subdir.getValue());
             }
        }
        
        File nn = new File(filename);
        if(nn.exists()) nn.delete();
        this.hdirw.cd(); this.hdirw.tree(); this.hdirw.writeFile(filename);
        System.out.println("Hipo file written: "+filename);
    }
    
    public void readFile(String filename){

        System.out.println("HipoFile.readFile: "+this.file);  
        HipoReader reader = new HipoReader();
        reader.open(filename);
        
        hdirr.clear();
        
        for(int i = 0; i < reader.getEventCount(); i++){
            byte[] eventBuffer = reader.readEvent(i);
            //System.out.println(" EVENT # " + i + "  SIZE = " + eventBuffer.length);
            HipoEvent    event = new HipoEvent(eventBuffer);
            
            if(event.hasGroup(1200)==true){
//                System.out.println("--> reading data group descriptor");
//                System.out.println(event);
//                DataGroupDescriptor desc = DataSetSerializer.deserializeDataGroupDescriptor(event);
//                this.groupDescriptors.put(desc.getName(), desc);
            } else {            
                //System.out.println(event.toString());
                IDataSet h1 = DataSetSerializer.deserializeDataSet(event);
                String h1name = h1.getName();
                String dirname = hdirr.stringDirectoryFromPath(h1name);
                hdirr.mkdir(dirname);
                hdirr.cd(dirname);
                h1.setName(hdirr.stringObjectFromPath(h1name));
                hdirr.addDataSet(h1);
            }
        }
        
//        startBrowser(hdirr);
    }
    
    public void startBrowser(TDirectory dir) {
        TBrowser br = new TBrowser();
        br.setDirectory(dir);
    }
    
    private void fillPathMaps() {
        if(!this.map_extracted.isEmpty())this.map_extracted.clear();
        for(String path : hdirr.getDirectoryList()) {
            System.out.println("HipoFile.fillPathMaps: "+path);  
            ArrayList all = new ArrayList();
            for(String obj : hdirr.getCompositeObjectList(hdirr.getDir(path))) {
                all.add(hdirr.getObject(obj));
            }
            this.map_extracted.put(path, all);
        }
    }
  
    public ArrayList getArrayList(String subdirname){
        ArrayList AL = new ArrayList();
        int nd=0, nde=0;
        for(Map.Entry<String,Object> ss : this.map_extracted.entrySet()){
            String key = ss.getKey();
            if(key.equals(subdirname)){
                AL = (ArrayList) this.map_extracted.get(key);
            }else nde++;
            nd++;
            }
        if(nde==nd)System.out.println("SUB-DIRECTORY NOT FOUND ");
        return AL;
    }
    
}