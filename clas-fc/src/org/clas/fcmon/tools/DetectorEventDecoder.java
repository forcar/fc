package org.clas.fcmon.tools;

import java.util.Arrays;

import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.detector.decode.DetectorDataDgtz.ADCData;
import org.jlab.utils.groups.IndexedTable;

//lcs
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.BasicFADCFitter;
import org.jlab.detector.decode.ExtendedFADCFitter;
import org.jlab.detector.decode.MVTFitter;

/**
 *
 * @author gavalian
 */
public class DetectorEventDecoder {
    
    ConstantsManager  translationManager = new ConstantsManager();
    ConstantsManager  fitterManager      = new ConstantsManager();
    
    List<String>  tablesTrans            = null;
    List<String>  keysTrans              = null;
    
    List<String>  tablesFitter            = null;
    List<String>  keysFitter              = null;
    
    private  int  runNumber               = 10;
    
    private  BasicFADCFitter      basicFitter     = new BasicFADCFitter();
    private  ExtendedFADCFitter   extendedFitter  = new ExtendedFADCFitter();
    private  MVTFitter            mvtFitter       = new MVTFitter();
    
    private  Boolean          useExtendedFitter   = false;
    
    //lcs
    private  int                            tet  = 0;
    private  int                            nsa  = 0;
    private  int                            nsb  = 0;    
    
    public DetectorEventDecoder(boolean development){
        if(development==true){
            this.initDecoderDev();
        } else {
            this.initDecoder();
        }
    }
    
    public void setRunNumber(int run){
        this.runNumber = run;
    }
    
    //lcs
    public void setTET(int tet) {
    	  this.tet = tet;
    }
      
    public void setNSA(int nsa) {
    	  this.nsa = nsa;
    }
      
    public void setNSB(int nsb) {
    	  this.nsb = nsb;
    }
    
    public void setFitterParms(int tet, int nsa, int nsb) {
    	    this.tet = tet;
    	    this.nsa = nsa;
    	    this.nsb = nsb;
    }
      
    public DetectorEventDecoder(){
        this.initDecoder();
        /*
        keysTrans = Arrays.asList(new String[]{
            "FTCAL","FTHODO","LTCC","EC","FTOF","HTCC","DC"
        });
        
        tablesTrans = Arrays.asList(new String[]{
            "/daq/tt/ftcal","/daq/tt/fthodo","/daq/tt/ltcc",
            "/daq/tt/ec","/daq/tt/ftof","/daq/tt/htcc","/daq/tt/dc"
        });
        
        translationManager.init(keysTrans,tablesTrans);
        
        keysFitter   = Arrays.asList(new String[]{"FTCAL","FTOF","LTCC","EC","HTCC"});
        tablesFitter = Arrays.asList(new String[]{
            "/daq/fadc/ftcal","/daq/fadc/ftof","/daq/fadc/ltcc","/daq/fadc/ec",
            "/daq/fadc/htcc"
        });
        fitterManager.init(keysFitter, tablesFitter);
        */
    }
    
    public final void initDecoderDev(){
        keysTrans = Arrays.asList(new String[]{ "HTCC","BST"} );
        tablesTrans = Arrays.asList(new String[]{ "/daq/tt/clasdev/htcc","/daq/tt/clasdev/svt" });
        
        keysFitter   = Arrays.asList(new String[]{"HTCC"});
        tablesFitter = Arrays.asList(new String[]{"/daq/fadc/clasdev/htcc"});
        translationManager.init(keysTrans,tablesTrans);
        fitterManager.init(keysFitter, tablesFitter);
    }
    
    public final void initDecoder(){
        keysTrans = Arrays.asList(new String[]{
		"FTCAL","FTHODO","FTTRK","LTCC","ECAL","FTOF","HTCC","DC","CTOF","CND","BST","RF","BMT","FMT","RICH"
        });
        
        tablesTrans = Arrays.asList(new String[]{
            "/daq/tt/ftcal","/daq/tt/fthodo","/daq/tt/fttrk","/daq/tt/ltcc",
            "/daq/tt/ec","/daq/tt/ftof","/daq/tt/htcc","/daq/tt/dc","/daq/tt/ctof","/daq/tt/cnd","/daq/tt/svt",
            "/daq/tt/rf","/daq/tt/bmt","/daq/tt/fmt","/daq/tt/clasdev/richcosmic"
        });
        
        translationManager.init(keysTrans,tablesTrans);
        
        keysFitter   = Arrays.asList(new String[]{"FTCAL","FTOF","FTTRK","LTCC","ECAL","HTCC","CTOF","CND","BMT","FMT"});
        tablesFitter = Arrays.asList(new String[]{
            "/daq/fadc/ftcal","/daq/fadc/ftof","/daq/config/fttrk","/daq/fadc/ltcc","/daq/fadc/ec",
            "/daq/fadc/htcc","/daq/fadc/ctof","/daq/fadc/cnd","/daq/config/bmt","/daq/config/fmt"
        });
        fitterManager.init(keysFitter, tablesFitter);
    }
    /**
     * Set the flag to use extended fitter instead of basic fitter
     * which simply integrates over given bins inside of the given
     * windows for the pulse. The pulse parameters are provided by 
     * fitterManager (loaded from database).
     * @param flag 
     */
    public void setUseExtendedFitter(boolean flag){
        this.useExtendedFitter = flag;
    }
    /**
     * applies translation table to the digitized data to translate
     * crate,slot channel to sector layer component.
     * @param detectorData 
     */
    
    
    public void translate(List<DetectorDataDgtz>  detectorData){
        
        for(DetectorDataDgtz data : detectorData){
            
            int crate    = data.getDescriptor().getCrate();
            int slot     = data.getDescriptor().getSlot();
            int channel  = data.getDescriptor().getChannel();

            boolean hasBeenAssigned = false;
            
            for(String table : keysTrans){
                IndexedTable  tt = translationManager.getConstants(runNumber, table);
                DetectorType  type = DetectorType.getType(table);

                if(tt.hasEntry(crate,slot,channel)==true){
                    int sector    = tt.getIntValue("sector", crate,slot,channel);
                    int layer     = tt.getIntValue("layer", crate,slot,channel);
                    int component = tt.getIntValue("component", crate,slot,channel);
                    int order     = tt.getIntValue("order", crate,slot,channel);
                    
                    data.getDescriptor().setSectorLayerComponent(sector, layer, component);
                    data.getDescriptor().setOrder(order);
                    data.getDescriptor().setType(type);
                    for(int i = 0; i < data.getADCSize(); i++) {
                        data.getADCData(i).setOrder(order);
                    }
                    for(int i = 0; i < data.getTDCSize(); i++) {
                        data.getTDCData(i).setOrder(order);
                    }
                }
            }
        }
    }
    
/*    
    public void translate(List<DetectorDataDgtz>  detectorData){
        
        for(DetectorDataDgtz data : detectorData){
        	
         	String table = data.getDescriptor().getType().getName();
         	IndexedTable  tt = translationManager.getConstants(runNumber, table);
         	
            int crate    = data.getDescriptor().getCrate();
            int slot     = data.getDescriptor().getSlot();
            int channel  = data.getDescriptor().getChannel();

            DetectorType  type = DetectorType.getType(table);

            if(tt.hasEntry(crate,slot,channel)==true){
                int sector    = tt.getIntValue("sector", crate,slot,channel);
                int layer     = tt.getIntValue("layer", crate,slot,channel);
                int component = tt.getIntValue("component", crate,slot,channel);
                int order     = tt.getIntValue("order", crate,slot,channel);

                data.getDescriptor().setSectorLayerComponent(sector, layer, component);
                data.getDescriptor().setOrder(order);
                data.getDescriptor().setType(type);
                
                for(int i = 0; i < data.getADCSize(); i++) data.getADCData(i).setOrder(order);                
                for(int i = 0; i < data.getTDCSize(); i++) data.getTDCData(i).setOrder(order);                
            }            
        }

    } 
    */   
        public void fitPulses(List<DetectorDataDgtz>  detectorData){
        for(DetectorDataDgtz data : detectorData){            
            int crate    = data.getDescriptor().getCrate();
            int slot     = data.getDescriptor().getSlot();
            int channel  = data.getDescriptor().getChannel();
            for(String table : keysFitter){
                //custom MM fitter
            	if( ( (table.equals("BMT"))&&(data.getDescriptor().getType().getName().equals("BMT")) ) 
             || ( (table.equals("FMT"))&&(data.getDescriptor().getType().getName().equals("FMT")) ) 
             || ( (table.equals("FTTRK"))&&(data.getDescriptor().getType().getName().equals("FTTRK")) ) ){
                    IndexedTable daq = fitterManager.getConstants(runNumber, table);
                    short adcOffset = (short) daq.getDoubleValue("adc_offset", 0, 0, 0);
                    double fineTimeStampResolution = (byte) daq.getDoubleValue("dream_clock", 0, 0, 0);
                    double samplingTime = (byte) daq.getDoubleValue("sampling_time", 0, 0, 0);
                    if (data.getADCSize() > 0) {
                        ADCData adc = data.getADCData(0);
                        mvtFitter.fit(adcOffset, fineTimeStampResolution, samplingTime, adc.getPulseArray(), adc.getTimeStamp());
                        adc.setHeight((short) (mvtFitter.adcMax));
                        adc.setTime((int) (mvtFitter.timeMax));
                        adc.setIntegral((int) (mvtFitter.integral));
                        adc.setTimeStamp(mvtFitter.timestamp);
                    }
                } else {
                    IndexedTable  daq = fitterManager.getConstants(runNumber, table);
                    DetectorType  type = DetectorType.getType(table);
                    if(daq.hasEntry(crate,slot,channel)==true){                    
                        //basicFitter.setPulse(0, 4).setPedestal(35, 70);
                        //for(int i = 0; i < data.getADCSize(); i++){
                        //    basicFitter.fit(data.getADCData(i));
                        //}
            	            int tet = (this.tet>0) ? this.tet:daq.getIntValue("tet", crate,slot,channel); //lcs
            	            int nsa = (this.nsa>0) ? this.nsa:daq.getIntValue("nsa", crate,slot,channel); //lcs
            	            int nsb = (this.nsb>0) ? this.nsb:daq.getIntValue("nsb", crate,slot,channel); //lcs
                        if(data.getADCSize()>0){
                            for(int i = 0; i < data.getADCSize(); i++){
                                ADCData adc = data.getADCData(i);
                                if(adc.getPulseSize()>0){
                                    //System.out.println("-----");
                                    //System.out.println(" FITTING PULSE " + 
                                    //        crate + " / " + slot + " / " + channel);
                                    try {
                                        extendedFitter.fit(nsa, nsb, tet, 0, adc.getPulseArray());
                                    } catch (Exception e) {
                                        System.out.println(">>>> error : fitting pulse "
                                                            +  crate + " / " + slot + " / " + channel);
                                    }
                                    //System.out.println(" FIT RESULT = " + extendedFitter.adc + " / "
                                    //        + this.extendedFitter.t0 + " / " + this.extendedFitter.ped);
                                    int adc_corrected = extendedFitter.adc + extendedFitter.ped*(nsa+nsb);
                                    adc.setHeight((short) this.extendedFitter.pulsePeakValue);
                                    adc.setIntegral(adc_corrected);
                                    adc.setTimeWord(this.extendedFitter.t0);
                                    adc.setPedestal((short) this.extendedFitter.ped);                                
                                }
                            }
                        }
                        //System.out.println(" apply nsa nsb " + nsa + " " + nsb);
                        if(data.getADCSize()>0){
                            for(int i = 0; i < data.getADCSize(); i++){
                                    data.getADCData(i).setADC(nsa, nsb);
                            }
                        }
                    }
                }
            }
        }
    }
}
