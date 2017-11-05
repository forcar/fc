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
import org.jlab.detector.decode.CLASDecoder;
import org.jlab.detector.decode.CodaEventDecoder;
//import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.groot.data.H1F;
import org.jlab.detector.decode.DetectorDataDgtz;

public class FCCLASDecoder {
       
    public CodaEventDecoder          codaDecoder = null; 
    public DetectorEventDecoder  detectorDecoder = null;
    public List<DetectorDataDgtz>       dataList = new ArrayList<DetectorDataDgtz>();    
    public HipoDataSync                   writer = null;
    public HipoDataEvent               hipoEvent = null;
    private int                 decoderDebugMode = 0;
    
    public int runno;
    public int evtno;
    public long timeStamp;
    public int triggerBits;
   
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
   
    public void initEvent(DataEvent event){
        
        if(event instanceof EvioDataEvent){
            try {
                dataList    = codaDecoder.getDataEntries( (EvioDataEvent) event);
                runno       = codaDecoder.getRunNumber();
                evtno       = codaDecoder.getEventNumber();
                timeStamp   = codaDecoder.getTimeStamp();
                triggerBits = codaDecoder.getTriggerBits();
                if(this.decoderDebugMode>0){
                    System.out.println("\n>>>>>>>>> RAW decoded data");
                    for(DetectorDataDgtz data : dataList){
                        System.out.println(data);
                    }
                }
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
    
    public int getRun() {
        return this.runno;    	
    }
    
    public int getEvent() {
        return this.evtno;    	
    }
    
    public long getTimestamp() {
    	    return this.timeStamp;
    }
    
    public int getTriggerbits() {
    	    return this.triggerBits;    	
    }
    
    public long getPhase() {
    	    return ((this.timeStamp%6)+this.phase_offset)%6;
    }
    
    public int getBitsec() {
        return	(int) (Math.log10(this.triggerBits>>24)/0.301+1);
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
            if(name.equals("BMT::adc")||name.equals("FMT::adc")){
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
    
    public DataEvent getDataEvent(){
        
        HipoDataEvent event = (HipoDataEvent) writer.createEvent();
        
        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc"};
        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL};
        
        String[]        tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc"};
        DetectorType[]  tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL};
        
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
        /**
         * Adding un-decoded banks to the event
         */
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
/*        
        try {
            DataBank tdcBankUD = this.getDataBankUndecodedTDC("RAW::tdc", DetectorType.UNDEFINED);
            if(tdcBankUD!=null){
                if(tdcBankUD.rows()>0){
                    event.appendBanks(tdcBankUD);
                }
            } else {
                
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
*/        
        return event;
    }
    public HipoDataBank createHeaderBank(DataEvent event, int nrun, int nevent, float torus, float solenoid){
        HipoDataBank bank = (HipoDataBank) event.createBank("RUN::config", 1);
        
        int    localRun = this.codaDecoder.getRunNumber();
        int  localEvent = this.codaDecoder.getEventNumber();
        long  timeStamp = this.codaDecoder.getTimeStamp();
        int triggerBits = this.codaDecoder.getTriggerBits();
        
        if(nrun>0){
            localRun = nrun;
            localEvent = nevent;
        }
        bank.setInt("run",        0, localRun);
        bank.setInt("event",      0, localEvent);
        bank.setInt("trigger",    0, triggerBits);        
        bank.setFloat("torus",    0, torus);
        bank.setFloat("solenoid", 0, solenoid);        
        bank.setLong("timestamp", 0, timeStamp);        
        
        
        return bank;
    }
}
