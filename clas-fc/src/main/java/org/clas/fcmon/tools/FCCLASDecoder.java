package org.clas.fcmon.tools;

import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorType;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.detector.decode.CodaEventDecoder;
//import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.FADCData;

public class FCCLASDecoder {
       
    public CodaEventDecoder          codaDecoder = null; 
    public DetectorEventDecoder  detectorDecoder = null;
    public List<DetectorDataDgtz>       dataList = new ArrayList<DetectorDataDgtz>();    
    public HipoDataSync                   writer = null;
    public HipoDataEvent               hipoEvent = null;
    public String                   HipoFileName = null;
    public Boolean                isHipoFileOpen = false;  
    private boolean             isRunNumberFixed = false;
    private int                 decoderDebugMode = 0;
    
    public int runno;
    public int evtno;
    public long timeStamp;
    public long triggerBits;
   
    public int phase_offset = 1;
    
    public FCCLASDecoder(){    
        System.out.println("***************");
        System.out.println("*FCCLASDecoder*");
        System.out.println("***************");
        codaDecoder = new CodaEventDecoder();
        detectorDecoder = new DetectorEventDecoder();
        writer = new HipoDataSync();
        hipoEvent = (HipoDataEvent) writer.createEvent();
    }
    
    public static FCCLASDecoder createDecoder(){
        FCCLASDecoder decoder = new FCCLASDecoder();
        return decoder;
    }
        
    public void setDebugMode(int mode){
        this.decoderDebugMode = mode;
    }
    
    public void setRunNumber(int run){
    	this.runno = run;
        if(this.isRunNumberFixed==false){
            this.detectorDecoder.setRunNumber(run);
        }
    }

    public void setRunNumber(int run, boolean fixed){        
        this.isRunNumberFixed = fixed;
        this.detectorDecoder.setRunNumber(run);
        System.out.println(" SETTING RUN NUMBER TO " + run + " FIXED = " + this.isRunNumberFixed);
    }
    
    public CodaEventDecoder getCodaEventDecoder() {
	return codaDecoder;
    }
    
    public void openHipoFile(String path) {               
    	HipoFileName = path+"clas_00"+runno+".hipo";
        System.out.println("FCCLASDecoder.openHipoFile(): Opening "+HipoFileName);
        writer.setCompressionType(2);
        writer.open(HipoFileName);
        isHipoFileOpen = true;
    }
    
    public void closeHipoFile() {

        System.out.println("FCCLASDecoder.closeHipoFile(): Closing "+HipoFileName);
        writer.close();
        isHipoFileOpen = false;
    } 
    
    public void initEvent(DataEvent event){
       
        if(event instanceof EvioDataEvent){
            try {
                dataList    = codaDecoder.getDataEntries( (EvioDataEvent) event);
                //-----------------------------------------------------------------------------
                // This part reads the BITPACKED FADC data from tag=57638 Format (cmcms)
                // Then unpacks into Detector Digigitized data, and appends to existing buffer
                // Modified on 9/5/2018
                //-----------------------------------------------------------------------------
                
                List<FADCData>  fadcPacked = codaDecoder.getADCEntries((EvioDataEvent) event);
                if(fadcPacked!=null){
                    List<DetectorDataDgtz> fadcUnpacked = FADCData.convert(fadcPacked);
                    dataList.addAll(fadcUnpacked);
                }
                //  END of Bitpacked section                
                //-----------------------------------------------------------------------------
                if(this.decoderDebugMode>0){
                    System.out.println("\n>>>>>>>>> RAW decoded data");
                    for(DetectorDataDgtz data : dataList){
                        System.out.println(data);
                    }
                }
                int runNumberCoda = codaDecoder.getRunNumber();
                this.setRunNumber(runNumberCoda);
                
                detectorDecoder.translate(dataList);
                detectorDecoder.fitPulses(dataList);
                
                if(this.decoderDebugMode>0){
                    System.out.println("\n>>>>>>>>> TRANSLATED data");
                    for(DetectorDataDgtz data : dataList){
                        System.out.println(data);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }   
    
    public List<DetectorDataDgtz>  getEntriesADC(DetectorType type){
        return this.getEntriesADC(type, dataList);        
    }

    public List<DetectorDataDgtz>  getEntriesADC(DetectorType type, 
            List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  adc = new ArrayList<DetectorDataDgtz>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getADCSize()>0&&entry.getTDCSize()==0){
                    adc.add(entry);
                }
            }
        }        
        return adc;
    }
    
    public List<DetectorDataDgtz>  getEntriesTDC(DetectorType type){
        return getEntriesTDC(type,dataList);    
    }

    public List<DetectorDataDgtz>  getEntriesTDC(DetectorType type, 
            List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  tdc = new ArrayList<DetectorDataDgtz>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getTDCSize()>0&&entry.getADCSize()==0){
                    tdc.add(entry);
                }
            }
        }
        return tdc;
    }    
    
    public List<DetectorDataDgtz>  getEntriesVTP(DetectorType type){
        return getEntriesVTP(type,dataList);    
    }
    /**
     * returns VTP entries from decoded data for given detector type
     * @param type detector type
     * @param entries digitized data list
     * @return list of VTP's for detector type
     */
    public List<DetectorDataDgtz>  getEntriesVTP(DetectorType type, List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  vtp = new ArrayList<DetectorDataDgtz>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getVTPSize()>0){
                    vtp.add(entry);
                }
            }
        }
        return vtp;
    }
    
    public List<DetectorDataDgtz>  getEntriesSCALER(DetectorType type){
        return getEntriesSCALER(type,dataList);    
    }
    /**
     * returns VTP entries from decoded data for given detector type
     * @param type detector type
     * @param entries digitized data list
     * @return list of VTP's for detector type
     */
    public List<DetectorDataDgtz>  getEntriesSCALER(DetectorType type, 
        List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  scaler = new ArrayList<DetectorDataDgtz>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getSCALERSize()>0){
                    scaler.add(entry);
                }
            }
        }
//        System.out.println("\t>>>>> produced list  TYPE = "  + type + "  size = " + entries.size() + "  vtp store = " + vtp.size());
        return scaler;
    }    
    
    public DataBank getDataBankADC(String name, DetectorType type){
        
        List<DetectorDataDgtz> adcDGTZ = this.getEntriesADC(type);
        
        DataBank adcBANK = hipoEvent.createBank(name, adcDGTZ.size());
        
        for(int i = 0; i < adcDGTZ.size(); i++){
            adcBANK.setByte("sector", i, (byte) adcDGTZ.get(i).getDescriptor().getSector());
            adcBANK.setByte("layer", i, (byte) adcDGTZ.get(i).getDescriptor().getLayer());
            adcBANK.setShort("component", i, (short) adcDGTZ.get(i).getDescriptor().getComponent());
            adcBANK.setByte("order", i, (byte) adcDGTZ.get(i).getDescriptor().getOrder());
            adcBANK.setInt("ADC", i, adcDGTZ.get(i).getADCData(0).getADC());
            adcBANK.setFloat("time", i, (float) adcDGTZ.get(i).getADCData(0).getTime());
            adcBANK.setShort("ped", i, (short) adcDGTZ.get(i).getADCData(0).getPedestal());            
            if(name == "BST::adc") adcBANK.setLong("timestamp", i, adcDGTZ.get(i).getADCData(0).getTimeStamp()); // 1234 = dummy placeholder value
            if(name.equals("BMT::adc")||name.equals("FMT::adc")|| name.equals("FTTRK::adc")){
            	    adcBANK.setInt("ADC", i, adcDGTZ.get(i).getADCData(0).getHeight());
                	adcBANK.setInt("integral", i, adcDGTZ.get(i).getADCData(0).getIntegral());
            	    adcBANK.setLong("timestamp", i, adcDGTZ.get(i).getADCData(0).getTimeStamp());
            }	
         }
        return adcBANK;
    }    
    
    public DataBank getDataBankTDC(String name, DetectorType type){
        
        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);        
        DataBank tdcBANK = hipoEvent.createBank(name, tdcDGTZ.size());
        if(tdcBANK==null) return null;
        
        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.setByte("sector", i, (byte) tdcDGTZ.get(i).getDescriptor().getSector());
            tdcBANK.setByte("layer", i, (byte) tdcDGTZ.get(i).getDescriptor().getLayer());
            tdcBANK.setShort("component", i, (byte) tdcDGTZ.get(i).getDescriptor().getComponent());
            tdcBANK.setByte("order", i, (byte) tdcDGTZ.get(i).getDescriptor().getOrder());
            tdcBANK.setInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
        }
        return tdcBANK;
    }
    
    public DataBank getDataBankUndecodedADC(String name, DetectorType type){
    	
        List<DetectorDataDgtz> adcDGTZ = this.getEntriesADC(type);
        DataBank adcBANK = hipoEvent.createBank(name, adcDGTZ.size());
        
        for(int i = 0; i < adcDGTZ.size(); i++){
            adcBANK.setByte("crate", i, (byte) adcDGTZ.get(i).getDescriptor().getCrate());
            adcBANK.setByte("slot", i, (byte) adcDGTZ.get(i).getDescriptor().getSlot());
            adcBANK.setShort("channel", i, (short) adcDGTZ.get(i).getDescriptor().getChannel());
            adcBANK.setInt("ADC", i, adcDGTZ.get(i).getADCData(0).getADC());
            adcBANK.setFloat("time", i, (float) adcDGTZ.get(i).getADCData(0).getTime());
            adcBANK.setShort("ped", i, (short) adcDGTZ.get(i).getADCData(0).getPedestal());            
        }
        return adcBANK;
    }
    
    public DataBank getDataBankUndecodedTDC(String name, DetectorType type){
        
        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);
        
        DataBank tdcBANK = hipoEvent.createBank(name, tdcDGTZ.size());
        if(tdcBANK==null) return null;
        
        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.setByte("crate", i, (byte) tdcDGTZ.get(i).getDescriptor().getCrate());
            tdcBANK.setByte("slot", i, (byte) tdcDGTZ.get(i).getDescriptor().getSlot());
            tdcBANK.setShort("channel", i, (byte) tdcDGTZ.get(i).getDescriptor().getChannel());
            tdcBANK.setInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
        }
        return tdcBANK;
    }
    
    public DataBank getDataBankUndecodedVTP(String name, DetectorType type){
        
        List<DetectorDataDgtz> vtpDGTZ = this.getEntriesVTP(type);
        
        DataBank vtpBANK = hipoEvent.createBank(name, vtpDGTZ.size());
        if(vtpBANK==null) return null;
        
        for(int i = 0; i < vtpDGTZ.size(); i++){
            vtpBANK.setByte("crate", i,     (byte) vtpDGTZ.get(i).getDescriptor().getCrate());
            vtpBANK.setByte("slot", i,      (byte) vtpDGTZ.get(i).getDescriptor().getSlot());
            vtpBANK.setShort("channel", i, (short) vtpDGTZ.get(i).getDescriptor().getChannel());
            vtpBANK.setInt("word", i,              vtpDGTZ.get(i).getVTPData(0).getWord());
        }
        return vtpBANK;
    }  
    
    public DataBank getDataBankUndecodedSCALER(String name, DetectorType type){
        
        List<DetectorDataDgtz> scalerDGTZ = this.getEntriesSCALER(type);
        
        DataBank scalerBANK = hipoEvent.createBank(name, scalerDGTZ.size());
        if(scalerBANK==null) return null;
        
        for(int i = 0; i < scalerDGTZ.size(); i++){
            scalerBANK.setByte("crate", i, (byte) scalerDGTZ.get(i).getDescriptor().getCrate());
            scalerBANK.setByte("slot", i, (byte) scalerDGTZ.get(i).getDescriptor().getSlot());
            scalerBANK.setShort("channel", i, (short) scalerDGTZ.get(i).getDescriptor().getChannel());
            scalerBANK.setByte("helicity", i, (byte) scalerDGTZ.get(i).getSCALERData(0).getHelicity());
            scalerBANK.setByte("quartet", i, (byte) scalerDGTZ.get(i).getSCALERData(0).getQuartet());
            scalerBANK.setLong("value", i, scalerDGTZ.get(i).getSCALERData(0).getValue());
        }
        return scalerBANK;
    }  
    
    public DataEvent getDataEvent(DataEvent rawEvent){
        this.initEvent(rawEvent);
        return getDataEvent();
    }    
    
    public DataEvent getDataEvent(){
        
        HipoDataEvent event = (HipoDataEvent) writer.createEvent();
        
//        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc","CTOF::adc","CND::adc","LTCC::adc","HTCC::adc","BAND::adc"};
//        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC,DetectorType.BAND};
        
//        String[]        tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc","CTOF::tdc","CND::tdc","LTCC::tdc","HTCC::tdc","BAND::tdc"};
//        DetectorType[]  tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC,DetectorType.BAND};
 
        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc","CTOF::adc","CND::adc","LTCC::adc","HTCC::adc"};
        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC};
        
        String[]        tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc","CTOF::tdc","CND::tdc","LTCC::tdc","HTCC::tdc"};
        DetectorType[]  tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC};
        
        for(int i = 0; i < adcBankTypes.length; i++){
            DataBank adcBank = getDataBankADC(adcBankNames[i],adcBankTypes[i]);
            if(adcBank!=null){
                if(adcBank.rows()>0){
                    event.appendBanks(adcBank);
                }
            }
        }
        
        for(int i = 0; i < tdcBankTypes.length; i++){
            DataBank tdcBank = getDataBankTDC(tdcBankNames[i],tdcBankTypes[i]);
            if(tdcBank!=null){
                if(tdcBank.rows()>0){
                    event.appendBanks(tdcBank);
                }
            }
        }        

        try {
            DataBank adcBankUD = this.getDataBankUndecodedADC("RAW::adc", DetectorType.UNDEFINED);
            if(adcBankUD!=null){
                if(adcBankUD.rows()>0){
                    event.appendBanks(adcBankUD);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        try {
            DataBank vtpBankUD = this.getDataBankUndecodedVTP("RAW::vtp", DetectorType.UNDEFINED);
            if(vtpBankUD!=null){
                if(vtpBankUD.rows()>0){
                    event.appendBanks(vtpBankUD);
                }
            } else {
                
            }
        } catch(Exception e) {
            e.printStackTrace();
        }  
        
        try {
            DataBank scalerBankUD = this.getDataBankUndecodedSCALER("RAW::scaler", DetectorType.UNDEFINED);
            if(scalerBankUD!=null){
                if(scalerBankUD.rows()>0){
                    event.appendBanks(scalerBankUD);
                }
            } else {
                
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        return event;
    }

    public long getTriggerPhase() {    	
        long timestamp    = this.codaDecoder.getTimeStamp();
        int  phase_offset = 1;
        return ((timestamp%6)+phase_offset)%6; // TI derived phase correction due to TDC and FADC clock differences 
    }  
    
    public HipoDataBank createHeaderBank(DataEvent event, int nrun, int nevent, float torus, float solenoid){
        HipoDataBank bank = (HipoDataBank) event.createBank("RUN::config", 1);
        
        int    localRun = this.codaDecoder.getRunNumber();
        int  localEvent = this.codaDecoder.getEventNumber();
        int   localTime = this.codaDecoder.getUnixTime();
        long  timeStamp = this.codaDecoder.getTimeStamp();
        long triggerBits = this.codaDecoder.getTriggerBits();
        
        if(nrun>0){
            localRun = nrun;
            localEvent = nevent;
        }
        
        bank.setInt("run",        0, localRun);
        bank.setInt("event",      0, localEvent);
        bank.setInt("unixtime",   0, localTime);
        bank.setLong("trigger",   0, triggerBits);        
        bank.setFloat("torus",    0, torus);
        bank.setFloat("solenoid", 0, solenoid);        
        bank.setLong("timestamp", 0, timeStamp);        
        
        
        return bank;
    }
    
    public HipoDataBank createTriggerBank(DataEvent event){
        HipoDataBank bank = (HipoDataBank) event.createBank("RUN::trigger", this.codaDecoder.getTriggerWords().size());
        
        for(int i=0; i<this.codaDecoder.getTriggerWords().size(); i++) {
            bank.setInt("id",      i, i+1);
            bank.setInt("trigger", i, this.codaDecoder.getTriggerWords().get(i));
        }
        return bank;
    }

}
