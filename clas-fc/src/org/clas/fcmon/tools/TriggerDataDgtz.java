package org.clas.fcmon.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.clas.fcmon.tools.FCTrigger.TEC_Cluster;
import org.clas.fcmon.tools.FCTrigger.TEC_Peak;
import org.jlab.coda.jevio.ByteDataTransformer;
import org.jlab.coda.jevio.EvioNode;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioTreeBranch;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedList.IndexGenerator;

public class TriggerDataDgtz {
	
	FCTrigger trig = null;
	CodaEventDecoder codadecoder = new CodaEventDecoder();
	
    public IndexedList<List<TEC_Peak>>         peaks = new IndexedList<List<TEC_Peak>>(3);
    public IndexedList<List<TEC_Cluster>>   clusters = new IndexedList<List<TEC_Cluster>>(2);
    public int npeaks;
    public int nclusters;
    
	void TriggerDataDgtz() {
	}
	
	void init() {
		this.peaks.clear();
		this.clusters.clear();
		this.npeaks=0;
		this.nclusters=0;
	}
	
	public IndexedList<List<TEC_Peak>> getPeaks() {
		return this.peaks;
	}	
	
	public IndexedList<List<TEC_Cluster>> getClusters() {
		return this.clusters;
	}
	      
    public void getTriggerBank(DataEvent event) {
   	 
        if(!event.hasBank("RAW::vtp")==true) return;
        
        init();
        
        IndexedList<List<Integer>> crates = new IndexedList<List<Integer>>(1);
        DataBank  bank = event.getBank("RAW::vtp");
        int rows = bank.rows();
        for(int i = 0; i < rows; i++){            
            int  ic = bank.getByte("crate",i);
            int  iw = bank.getInt("word",i);
            if(!crates.hasItem(ic)) {
                crates.add(new ArrayList<Integer>(),ic);}
                crates.getItem(ic).add(iw);
        }
        
        IndexGenerator ig = new IndexGenerator();
        
        for (Map.Entry<Long,List<Integer>>  entry : crates.getMap().entrySet()){
            long hash = entry.getKey();
            int crate = ig.getIndex(hash, 0);              
            List<Integer> list = crates.getItem(crate); 
            Iterator<Integer> it = list.iterator();
            trig = new FCTrigger(); trig.resetAll(); trig.getTriggerWords(it,crate);
            fillVTPStructure();
        }
        
    }
        
    public void getTriggerBank(EvioDataEvent event){
    	
    	    init();
            
        List<EvioTreeBranch> branches = codadecoder.getEventBranches(event);
            
        for(EvioTreeBranch branch : branches){
            int  crate = branch.getTag();
            for(EvioNode node : branch.getNodes()){
                if(node.getTag()==57634){
                    int[] intData = ByteDataTransformer.toIntArray(node.getStructureBuffer(true));
                    List<Integer> list  = Arrays.stream( intData ).boxed().collect( Collectors.toList() );    
                    Iterator<Integer> it = list.iterator();
                    trig = new FCTrigger(); trig.resetAll(); trig.getTriggerWords(it,crate);
                    fillVTPStructure();
                }
            }
        }            
    }  
    
    public void makeTriggerBank() {
   	 
       HipoDataSync writer = new HipoDataSync();
       HipoDataEvent hipoEvent = (HipoDataEvent) writer.createEvent();
       DataBank tdcBANK = hipoEvent.createBank("TRIGGER::peaks", npeaks);
       	 
     }

    public void fillVTPStructure() {
    	
        int detector  = trig.GetDetector();
        int sector    = trig.GetSector()+1; 
        
        for (int i_view=0; i_view<3; i_view++) {
         	int n_ECpeaks = trig.GetNPeaks(0, i_view);
         	for (int i_peak=0; i_peak < n_ECpeaks; i_peak++) {
         		if(!peaks.hasItem(sector, detector, i_view)) {
         			peaks.add(new ArrayList<TEC_Peak>(), sector, detector, i_view);}
         		    peaks.getItem(sector, detector, i_view).add(trig.GetECPeak(0, i_view, i_peak));
         		    npeaks++;
         	}
        }
        
        int n_ECclust = trig.GetNClust(0);
        for (int i_clust=0; i_clust < n_ECclust; i_clust++) {
         	if(!clusters.hasItem(sector,detector)) {
         		clusters.add(new ArrayList<TEC_Cluster>(), sector, detector);}
         	    clusters.getItem(sector, detector).add(trig.GetECCluster(0, i_clust));
         	    nclusters++;
        }     	
    } 
//        System.out.println("Sector: "+sector+" Detector: "+detector);
//        System.out.println("NPEAKS: "+npeaks+" NCLUSTERS: "+nclusters);
         

}
