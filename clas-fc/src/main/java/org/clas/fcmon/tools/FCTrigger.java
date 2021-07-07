package org.clas.fcmon.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jlab.utils.groups.IndexedList;

/**
 * 
 * @author lcsmith
 * Adapted from Rafayel Paremuzjan clas12 trigger code
 * https://github.com/JeffersonLab/clas12-trigger/blob/master/src/ECTrig.cxx
 */

public class FCTrigger {
	  
	public class TEC_Peak {
		public int inst;
		public int view;
		public int coord;
		public int energy;
		public int time;
	}
	
	public class TEC_Cluster{
		public int inst;
		public int Ustrip;   // U strip number
		public int Vstrip;   // V strip number
		public int Wstrip;   // W strip number
		public int energy;
		public int time;
	}

	public class TCLAS12Trigger{
		public int inst;
		public int lane;
		public int time;
	} 

	public class THTCC_mask{
		public int[] chan;          // Vector of channels that are fired 
		public int time;            // time shows the channel in the readout window
	} 

	public class TFTOF_mask{
		public int[] chan;         // Vector of channels that are fired 
		public int time;           // time shows the channel in the readout window
	} 

	public class Trig_word{
		public int tr_time;       // trigger time with the 4ns resolution
		public int tr_word;       // The trigger word
	}
	
	public class FitData {
		
		Iterator<Integer> it;	
		public int data;
		
		public FitData(Iterator<Integer> it) {
			this.it = it;
			data = it.next();
		}
		
		public int range(int bit1, int bit2) {
		    int b12 = bit1-bit2+1; int mask=(int)(Math.pow(2,b12)-1);
		    return (data>>bit2)&mask;
		}
		
		public void next() {
			data=it.next();			
		}
	}
	
	FitData fit_data = null;
	
	public FCTrigger() {
		makeMaps();		
	}
	
	public boolean getTriggerWords(Iterator<Integer> it, int a_adcECvtp_tag) {
		
		fECVTP_tag = a_adcECvtp_tag;		
		if(!EC_vtp_sector.hasItem(fECVTP_tag)) return false;
		fSector = EC_vtp_sector.getItem(fECVTP_tag);
		if(!EC_vtp_detector.hasItem(fECVTP_tag)) return false;
		fDet    = EC_vtp_detector.getItem(fECVTP_tag);
		
		while (it.hasNext()) {
			fit_data = new FitData(it);
			
			boolean is_type_def = fit_data.range(31,31)==1;
			
			if (is_type_def) {
				
				int data_type =  fit_data.range(30,27);
				
	            switch (data_type) {
	            case type_blk_head:    ReadBlockHeader();
                    break;
                case type_blk_trail:   ReadBlockTrailer();
                    break;
                case type_ev_head:     ReadEventHeader();
                    break;
                case type_trig_time:   ReadTriggerTime();
                    break;
                case type_ECtrig_peak: ReadECTriggerPeak();
                    break;
                case type_ECTrig_clust:ReadECTriggerCluster();
                    break;
                case type_trigger:     ReadTrigger();
                    break;
                case type_HTCC_clust:  ReadHTCCTrigMask();
                    break;
                case type_FTOF_clust:  ReadFTOFTrigMask();
                    break;
                }		            
			}			
		}
		
        for (int i = 0; i < n_inst; i++) {
            fnClusters[i] = fv_ECAllClusters.hasItem(i) ? fv_ECAllClusters.getItem(i).size():0;
            fnAllClusters+=fnClusters[i];
            for (int j = 0; j < n_view; j++) {
                fnPeaks[i][j] = fv_ECAllPeaks.hasItem(i,j) ? fv_ECAllPeaks.getItem(i,j).size():0;
                fnAllPeaks+=fnPeaks[i][j];
            }
        }
        
        fnHTCC_Masks = fv_HTCCMasks.size();
        fnFTOF_Masks = fv_FTOFMasks.size();	
        
        return true;
	}	
	
	public int GetDetector()         {return fDet;}   // 0 = Global Trigger, 1 = EC, 2 = PCal
	public int GetSector()           {return fSector;}
	
	public boolean HasBlockHeader()  {return has_BlockHeader;}
	public boolean HasBlockTrailer() {return has_BlockTrailer;}
	public boolean HasEventHeader()  {return has_EventHeader;}
	public boolean HasTrigTime()     {return has_TrigTime;}
	public boolean HasTrigPeak()     {return has_TrigPeak;}
	public boolean HasTrigClust()    {return has_TrigClust;}
	public boolean HasTrigger()      {return has_Trigger;}
	
	public int GetNwords()           {return has_BlockTrailer ? fnwords: UNDEF; }
	public int GetSlotID()           {return (has_BlockHeader || has_BlockTrailer) ? fslotid : UNDEF; }
	public int GetEvNumber()         {return has_EventHeader ? fev_number : UNDEF; }
	public int GetBlockNUmber()      {return has_BlockHeader ? fblock_number: UNDEF; }
	public int GetBlockLevel()       {return has_BlockHeader ? fblock_level: UNDEF; }
	public int GetNAllPeaks()        {return fnAllPeaks;}
	public int GetNAllClust()        {return fnAllClusters;}
	public int GetNHTCCMasks()       {return fnHTCC_Masks;}   // Return number of THTCC_Hits objects
	public int GetNFTOFMasks()       {return fnFTOF_Masks;}   // Return number of THTCC_Hits objects
	
	public int GetNTrig()            {return fnTrigWords;};   // Return number of triggers in the VTP Bank
	public int GetTrigLane()         {return ftrig_lane;};    // trigger number, i.e. which trigger is fired
	public long GetLocalTrigTime()   {return ftrg_time;};     // Trig time wrt the start of the event window beginning
	public int GetTrigInst()         {return ftrig_inst;};    // 0=EC_in, 1=EC_out	
	
	static int n_inst = 2;
	static int n_view = 3;
	static int n_HTCC_chan = 48;                              // Number of HTCC channels;
	static int n_FTOF_chan = 62;                              // Number of Max FTOF channels per sector/panel;
	 
	TEC_Peak     cur_peak = null;
	TEC_Cluster cur_clust = null;
	THTCC_mask   cur_mask = null;
	Trig_word     cur_trg = null;
	
	int fECVTP_tag;
	int fDet;       // The detector, 0 = global trigger, 1 = EC, 2 = PCal
	int fSector;    // sector
	int fslotid;    
	int fnwords;
	int fev_number;
	int fblock_number;
	int fblock_level;	
	
	// TEC_Peak 1: 0=PCAL, 1=ECAL 2: 0=U, 1=V, 2=W
	public IndexedList<List<TEC_Peak>>      fv_ECAllPeaks    = new IndexedList<List<TEC_Peak>>(2); 
	public IndexedList<List<TEC_Cluster>>   fv_ECAllClusters = new IndexedList<List<TEC_Cluster>>(1); 
	
	List<THTCC_mask> fv_HTCCMasks = new ArrayList<THTCC_mask>();
	List<TFTOF_mask> fv_FTOFMasks = new ArrayList<TFTOF_mask>();
	List<Trig_word>  fv_TrigWords = new ArrayList<Trig_word>();
   	      
   	int fnAllPeaks;
   	int[][] fnPeaks = new int[n_inst][n_view];
   	int fnAllClusters;
   	int[] fnClusters = new int[n_inst];
   	
   	int fnTrigWords;  
   	
   	int fnHTCC_Masks;
   	int fnFTOF_Masks;

   	int ftrig_inst;
   	int ftrig_lane;
   	long ftrg_time;
   	
    void ReadBlockHeader() {
    	    has_BlockHeader = true;
    	    fslotid       = fit_data.range(26,22);
    	    fblock_number = fit_data.range(17, 8);
    	    fblock_level  = fit_data.range(7, 0);    	
    }
    
    void ReadBlockTrailer() {
    	    has_BlockTrailer = true;
    	    fslotid = fit_data.range(26, 22);
    	    fnwords = fit_data.range(21, 0);
    }
    
    void ReadEventHeader() {
    	    has_EventHeader = true;
    	    fev_number = fit_data.range(26, 0);
    }
    
    void ReadTriggerTime() {
    	    has_TrigTime = true;
    	    int timel = fit_data.range(23, 0);
    	    fit_data.next();
    	    int timeh = fit_data.range(23, 0);
    	    ftrg_time = 0;
    	    ftrg_time = timeh;
    	    ftrg_time = ftrg_time << 24;
    	    ftrg_time = ftrg_time | timel;
    }
    
    void ReadECTriggerPeak() {
    	
        has_TrigPeak = true;
        
        cur_peak = new TEC_Peak();        
        cur_peak.inst = fit_data.range(26, 26);
        cur_peak.view = fit_data.range(25, 24);
        cur_peak.time = fit_data.range(23, 16);
        fit_data.next(); 
        cur_peak.coord = fit_data.range(25, 16);
        cur_peak.energy = fit_data.range(15, 0);
//        System.out.println("Sect:"+(GetSector()+1)+" Det:"+GetDetector()+" Inst:"+cur_peak.inst+" View: "+cur_peak.view+" Coord:"+cur_peak.coord);
        if(!fv_ECAllPeaks.hasItem(cur_peak.inst,cur_peak.view)) {
            fv_ECAllPeaks.add(new ArrayList<TEC_Peak>(),cur_peak.inst,cur_peak.view);
        }
            fv_ECAllPeaks.getItem(cur_peak.inst,cur_peak.view).add(cur_peak);
    }           
   
    void ReadECTriggerCluster() {
    	
        has_TrigClust = true;
        
        cur_clust = new TEC_Cluster();
        cur_clust.inst = fit_data.range(26, 26);

        if (fit_data.range(25, 24)>0) {
            System.out.println("Wrong Data Format in bits (25, 24) should be 0");
        }

        cur_clust.time   = fit_data.range(23, 16);
        cur_clust.energy = fit_data.range(15, 0);
        
        fit_data.next();

        // This should be data continuation word,
        // if not then data format is not correct
        if (fit_data.range(31, 30)>0) {
            System.out.println("Wrong Data Format. In the 2nd word, bits(31,30) should be 0. Exiting");
        }

        cur_clust.Wstrip = fit_data.range(29, 20);
        cur_clust.Vstrip = fit_data.range(19, 10);
        cur_clust.Ustrip = fit_data.range(9, 0);

        if(!fv_ECAllClusters.hasItem(cur_clust.inst)) {
    	        fv_ECAllClusters.add(new ArrayList<TEC_Cluster>(), cur_clust.inst);
        }
            fv_ECAllClusters.getItem(cur_clust.inst).add(cur_clust); 
    }    
    
    void ReadHTCCTrigMask() {
    	
        has_HTCCMask = true;
        cur_mask = new THTCC_mask();
        cur_mask.time = fit_data.range(26, 16);

 /*
        // std::cout << "1st Word of HTCC " << (bitset<32>(*fit_data)) << endl;

        // Go to the next word
        fit_data.next();

        // HTCC mask is a 48 bit word, each bit tells whether that channel is fired
        // Highst 17 bits of the mask are in one word (16, 0),
        // and the rest 31 are in another word
        
        ap_int<n_HTCC_chan> HTCC_mask;

        //std::cout << "2nd Word " << (bitset<32>(*fit_data)) << endl;

        HTCC_mask(47, 31) = fit_data->range(16, 0);

        // Go to the next word
        fit_data = std::next(fit_data, 1);

        //std::cout << "3rd Word " << (bitset<32>(*fit_data)) << endl;

        HTCC_mask(30, 0) = fit_data->range(30, 0);

        // std::cout << "HTCC Mask is " << (bitset<48>(HTCC_mask)) << endl;

        // Now lets check which channels are fired
        for (int i = 0; i < n_HTCC_chan; i++) {
            if (HTCC_mask(i, i)) {
                cur_mask.chan.push_back(n_HTCC_chan - i - 1);
            }
        }

        fv_HTCCMasks.push_back(cur_mask);
        */
    }   
    
    void ReadFTOFTrigMask() {};                   // This will read FTOF Trigger mask
   
    public void ReadTrigger() {
        has_Trigger = true;

        cur_trg = new Trig_word();
        
        cur_trg.tr_time = fit_data.range(26, 16);
        
        int word1 = fit_data.range(15, 0);
        fit_data.next();
        int word2 = fit_data.range(15, 0);        
        cur_trg.tr_word = word2;
        cur_trg.tr_word = cur_trg.tr_word<<16;
        cur_trg.tr_word = cur_trg.tr_word | word1;
        fv_TrigWords.add(cur_trg);
        
        fnTrigWords = fnTrigWords + 1;
    }    
    
    public Trig_word getTrigWord(int aind) {    	
    	    if (aind < fnTrigWords) return fv_TrigWords.get(aind);
    	    return null;
    }
     
    public int GetNPeaks(int ainst, int aview) {
        if (ainst >= 0 && ainst < n_inst && aview >= 0 && aview < n_view) {
            return fnPeaks[ainst][aview];
        } else {
        	    System.out.println("GetNPeaks: Request for out of range element");
        	    return -1;
        }
    }
    
    public TEC_Peak GetECPeak(int ainst, int aview, int aind) {    	
        if (fv_ECAllPeaks.hasItem(ainst,aview)) return fv_ECAllPeaks.getItem(ainst,aview).get(aind);
        return null;
    }  

    public int GetNClust(int ainst) {
        if (ainst >= 0 && ainst < n_inst) {
            return fnClusters[ainst];
        } else {
        	    System.out.println("GetNClust: Request for out of range element");
        	    return -1;
        }
    }
    
    public TEC_Cluster GetECCluster(int ainst, int aind) {    	
        if (fv_ECAllClusters.hasItem(ainst)) return fv_ECAllClusters.getItem(ainst).get(aind);
        return null;
    }
    
    void PrintECCluster(int ainst, int aind) {
        if (ainst >= 0 && ainst < n_inst && aind < fnClusters[ainst]) {
    	    	    System.out.println("fv_ECAllClusters[ainst].size() = "+fv_ECAllClusters.getItem(ainst).size());
    	    	    System.out.println("Cluster instance in the argument = "+ainst);
    	    	    System.out.println("Cluster instance from the cluster structure = "+fv_ECAllClusters.getItem(ainst).get(aind).inst);
    	    	    System.out.println("Cluster index "+aind);
    	    	    System.out.println("Cluster U = "+fv_ECAllClusters.getItem(ainst).get(aind).Ustrip);
    	    	    System.out.println("Cluster V = "+fv_ECAllClusters.getItem(ainst).get(aind).Vstrip);
    	    	    System.out.println("Cluster W = "+fv_ECAllClusters.getItem(ainst).get(aind).Wstrip);
    	    	    System.out.println("Cluster E = "+fv_ECAllClusters.getItem(ainst).get(aind).energy);
    	    	    System.out.println("Cluster T = "+fv_ECAllClusters.getItem(ainst).get(aind).time);
    	    } else {
    	    	    System.out.println("PrintECCluster: Request for out of range element");
    	    }
    }
    
    boolean has_BlockHeader;
    boolean has_BlockTrailer;
    boolean has_EventHeader;
    boolean has_TrigTime;
    boolean has_TrigPeak;
    boolean has_TrigClust;
    boolean has_Trigger;
    boolean has_HTCCMask;
    boolean has_FTOFMask;   	
    
    static final int type_blk_head = 0;
    static final int type_blk_trail = 1;
    static final int type_ev_head = 2;
    static final int type_trig_time = 3;
    static final int type_ECtrig_peak = 4;
    static final int type_ECTrig_clust = 5;
    static final int type_HTCC_clust = 6;
    static final int type_FT_clust = 7;
    static final int type_FTOF_clust = 8;
    
    static final int type_trigger = 13;   
    
    static final int UNDEF = -9999;
    
//  static const int adcECvtp_tagmax = 115; // VTP bank tags are in the range 100 - 112
//  static const int adcECvtp_tagmin = 100; // VTP bank tags are in the range 100 - 112
    
    static int MCadcECvtp_tagmax = 60115; 
    static int MCadcECvtp_tagmin = 60092; 
    static int DataadcECvtp_tagmax = 115; 
    static int DataadcECvtp_tagmin = 92;   
    
    int EC_vtp_sectors[][]= {
    	    {100, 10}, // Global trigger
    	    // ================= GEMC ==============
    	    {60101, 0},    {60102, 1},    {60103, 2},    {60104, 3},    {60105, 4},    {60106, 5}, // ECal
    	    {60107, 0},    {60108, 1},    {60109, 2},    {60110, 3},    {60111, 4},    {60112, 5}, // PCal
    	    {60094, 0},    {60095, 1},    {60096, 2},    {60097, 3},    {60098, 4},    {60099, 5}, // PCal
    	    {60093, 7}, // HTCC
    	    // ================= Data ==============
    	    {101, 0},    {102, 1},    {103, 2},    {104, 3},    {105, 4},    {106, 5}, // ECal
    	    {107, 0},    {108, 1},    {109, 2},    {110, 3},    {111, 4},    {112, 5}, // PCal
    	    {94, 0},    {95, 1},    {96, 2},    {97, 3},    {98, 4},    {99, 5}, // PCal
    	    {93, 7} // HTCC

    	};

    int EC_vtp_detectors[][] = {
    	    {100, 0}, // Global trigger
    	    // ================= GEMC ==============    
    	    {60101, 1},    {60102, 1},    {60103, 1},    {60104, 1},    {60105, 1},    {60106, 1}, // ECal
    	    {60107, 2},    {60108, 2},    {60109, 2},    {60110, 2},    {60111, 2},    {60112, 2}, // PCal
    	    {60094, 3},    {60095, 3},    {60096, 3},    {60097, 3},    {60098, 3},    {60099, 3}, // FTOF
    	    {60093, 4}, // HTCC
    	    // ================= Data ==============
    	    {101, 1},    {102, 1},    {103, 1},    {104, 1},    {105, 1},    {106, 1}, // ECal
    	    {107, 2},    {108, 2},    {109, 2},    {110, 2},    {111, 2},    {112, 2}, // PCal
    	    {94, 3},    {95, 3},    {96, 3},    {97, 3},    {98, 3},    {99, 3}, // FTOF
    	    {93, 4} // HTCC
    	};
    
    IndexedList<Integer> EC_vtp_sector = new IndexedList<Integer>(1);
    IndexedList<Integer> EC_vtp_detector = new IndexedList<Integer>(1);
    
    public void makeMaps() {
    	   for (int i=0; i<39; i++) {    		  
    		   EC_vtp_sector.add(EC_vtp_sectors[i][1], EC_vtp_sectors[i][0]);
    		   EC_vtp_detector.add(EC_vtp_detectors[i][1], EC_vtp_detectors[i][0]);
    	   }
    }
    
    public void testMaps() {
    	
    }
    
    public void resetAll() {
    	
        fSector = UNDEF;
        fslotid = UNDEF;
        fnwords = UNDEF;
        fev_number = UNDEF;
        fblock_number = UNDEF;
        fblock_level = UNDEF;

        ftrig_inst = UNDEF;
        ftrig_lane = UNDEF;
        ftrg_time = UNDEF;
        
        fDet = UNDEF;
       
        for (int i = 0; i < n_inst; i++) {
            fnClusters[i] = 0;
            for (int j = 0; j < n_view; j++) {
                fnPeaks[i][j] = 0;
            }
        }

//        fv_HTCCMasks.clear();
//        fv_FTOFMasks.clear();

        ftrg_time = 0;

        fv_ECAllPeaks.clear();
        fv_ECAllClusters.clear();
 
        fv_TrigWords.clear();
        
        fnAllPeaks = 0;
        fnAllClusters = 0;
        fnHTCC_Masks = 0;
        fnTrigWords = 0;

        has_BlockHeader = false;
        has_BlockTrailer = false;
        has_EventHeader = false;
        has_TrigTime = false;
        has_TrigPeak = false;
        has_TrigClust = false;
        has_Trigger = false;
        has_HTCCMask = false;
    }   
}
