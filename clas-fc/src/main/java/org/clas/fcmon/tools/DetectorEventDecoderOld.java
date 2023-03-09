package org.clas.fcmon.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.ConstantsManager;
//import org.jlab.detector.decode.BasicFADCFitter;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorDataDgtz.ADCData;
import org.jlab.detector.decode.ExtendedFADCFitter;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedTable;

public class DetectorEventDecoderOld {
    
    ConstantsManager  translationManager = new ConstantsManager();
    ConstantsManager  fitterManager      = new ConstantsManager();
    
    List<String>  tablesTrans            = null;
    List<String>  keysTrans              = null;
    
    List<String>  tablesFitter           = null;
    List<String>  keysFitter             = null;
    
    private  int  runNumber               = 10;
    
//    private  BasicFADCFitter      basicFitter     = new BasicFADCFitter();
    private  ExtendedFADCFitter   extendedFitter  = new ExtendedFADCFitter();
    
    private  Boolean          useExtendedFitter   = false;
    
    private  int                            tet  = 0;
    private  int                            nsa  = 0;
    private  int                            nsb  = 0;
        
    public DetectorEventDecoderOld(boolean development){
        if(development==true){
            this.initDecoderDev();
        } else {
            this.initDecoder();
        }
    }
    
    public void setRunNumber(int run){
        this.runNumber = run;
    }
    
    public void setTET(int tet) {
  	  this.tet = (tet==0) ? 0:tet;
    }
    
    public void setNSA(int nsa) {
  	  this.nsa = (nsa==0) ? 0:nsa;
    }
    
    public void setNSB(int nsb) {
  	  this.nsb = (nsb==0) ? 0:nsb;
    }
    
    public DetectorEventDecoderOld(){
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
		"FTCAL","FTHODO","LTCC","ECAL","FTOF","HTCC","DC","CTOF","BST","RF","BMT","FMT","RICH"
        });
        
        tablesTrans = Arrays.asList(new String[]{
            "/daq/tt/ftcal","/daq/tt/fthodo","/daq/tt/ltcc",
            "/daq/tt/ec","/daq/tt/ftof","/daq/tt/htcc","/daq/tt/dc","/daq/tt/ctof","/daq/tt/svt",
            "/daq/tt/rf","/daq/tt/bmt","/daq/tt/fmt","/daq/tt/clasdev/richtest"
        });
        
        translationManager.init(keysTrans,tablesTrans);
        
        keysFitter   = Arrays.asList(new String[]{"FTCAL","FTHODO","FTOF","LTCC","ECAL","HTCC","CTOF","BMT","FMT"});
        tablesFitter = Arrays.asList(new String[]{
            "/daq/fadc/ftcal","/daq/fadc/fthodo","/daq/fadc/ftof","/daq/fadc/ltcc","/daq/fadc/ec",
            "/daq/fadc/htcc","/daq/fadc/ctof","/daq/config/bmt","/daq/config/fmt"
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
            //if(crate==69){
	    //System.out.println(" MVT " + crate + " " + slot + 
	    //  "  " + channel);
	// }
            boolean hasBeenAssigned = false;
            
            for(String table : keysTrans){
                IndexedTable  tt = translationManager.getConstants(runNumber, table);
                DetectorType  type = DetectorType.getType(table);
	
                if(tt.hasEntry(crate,slot,channel)==true){
                    int sector    = tt.getIntValue("sector", crate,slot,channel);
                    int layer     = tt.getIntValue("layer", crate,slot,channel);
                    int component = tt.getIntValue("component", crate,slot,channel);
                    int order     = tt.getIntValue("order", crate,slot,channel);
                    
                    /*if(crate>60&&crate<64){
                        System.out.println(" SVT " + sector + " " + layer + 
                                "  " + component);
                    }*/
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
        //Collections.sort(detectorData);
    }
    
    public void fitPulses(List<DetectorDataDgtz>  detectorData){
        for(DetectorDataDgtz data : detectorData){            
            int crate    = data.getDescriptor().getCrate();
            int slot     = data.getDescriptor().getSlot();
            int channel  = data.getDescriptor().getChannel();
            //System.out.println(" looking for " + crate + "  " 
            //       + slot + " " + channel);
            for(String table : keysFitter){
                IndexedTable  daq = fitterManager.getConstants(runNumber, table);
                DetectorType  type = DetectorType.getType(table);
                if(daq.hasEntry(crate,slot,channel)==true){                    
                    //basicFitter.setPulse(0, 4).setPedestal(35, 70);
                    //for(int i = 0; i < data.getADCSize(); i++){
                    //    basicFitter.fit(data.getADCData(i));
                    //}
            	        int tet = (this.tet>0) ? this.nsa:daq.getIntValue("tet", crate,slot,channel);
            	        int nsa = (this.nsa>0) ? this.nsa:daq.getIntValue("nsa", crate,slot,channel);
            	        int nsb = (this.nsb>0) ? this.nsb:daq.getIntValue("nsb", crate,slot,channel);
                    
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
