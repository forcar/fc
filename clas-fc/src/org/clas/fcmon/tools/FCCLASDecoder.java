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
import org.jlab.groot.data.H1F;
import org.jlab.detector.decode.DetectorDataDgtz;

import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioSource;
import org.jlab.io.evio.EvioTreeBranch;
import org.jlab.detector.decode.DetectorDataDgtz.VTPData;
import org.jlab.coda.jevio.ByteDataTransformer;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioNode;

public class FCCLASDecoder {
       
    public CodaEventDecoder          codaDecoder = null; 
    public DetectorEventDecoder  detectorDecoder = null;
    public List<DetectorDataDgtz>       dataList = new ArrayList<DetectorDataDgtz>();    
    public HipoDataSync                   writer = null;
    public HipoDataEvent               hipoEvent = null;
    public String                   HipoFileName = null;
    public Boolean                isHipoFileOpen = false;  
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
                runno       = codaDecoder.getRunNumber();
                evtno       = codaDecoder.getEventNumber();
                timeStamp   = codaDecoder.getTimeStamp();
                triggerBits = codaDecoder.getTriggerBits();
//                List<DetectorDataDgtz> junk = getDataEntries_TI((EvioDataEvent) event);
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
    
    public void clearTriggerbits() {
      	this.triggerBits = 0;
    }
    
    public void setTriggerbits(long word) {
    	   this.triggerBits  = word;
    }
    
    public List<DetectorDataDgtz> getDataEntries_VTP(EvioDataEvent event){
        
        List<DetectorDataDgtz> vtpEntries = new ArrayList<DetectorDataDgtz>();        
        List<EvioTreeBranch> branches = codaDecoder.getEventBranches(event);
        
        for(EvioTreeBranch branch : branches){
            int  crate = branch.getTag();
//            EvioTreeBranch cbranch = this.getEventBranch(branches, branch.getTag());
            for(EvioNode node : branch.getNodes()){
                if(node.getTag()==57634){
                    int[] intData =  ByteDataTransformer.toIntArray(node.getStructureBuffer(true));
                    for(int loop = 0; loop < intData.length; loop++){
                        int  dataEntry = intData[loop];
                        DetectorDataDgtz   entry = new DetectorDataDgtz(crate,0,0);
                        entry.addVTP(new VTPData(dataEntry));
//                        System.out.println(crate + " " + dataEntry + " " + entry.toString());
                        vtpEntries.add(entry);
//                        System.out.println(entry.toString());
                    }
                }
            }
        }
//        System.out.println(vtpEntries.size());
        return vtpEntries;
    }  
    
    public List<DetectorDataDgtz>  getDataEntries_TI(EvioDataEvent event){

    	    clearTriggerbits();
    	    
        List<DetectorDataDgtz> tiEntries = new ArrayList<>();
        List<EvioTreeBranch> branches = codaDecoder.getEventBranches(event);

        for(EvioTreeBranch branch : branches){
            int  crate = branch.getTag();
            EvioTreeBranch cbranch = codaDecoder.getEventBranch(branches, branch.getTag());
            for(EvioNode node : cbranch.getNodes()){
                if(node.getTag()==57610){
                    long[] longData = ByteDataTransformer.toLongArray(node.getStructureBuffer(false));
                    int[]  intData  = ByteDataTransformer.toIntArray(node.getStructureBuffer(false));
                    DetectorDataDgtz entry = new DetectorDataDgtz(crate,0,0);
                    long tStamp = longData[2]&0x00000000ffffffff;
                    entry.setTimeStamp(tStamp);
                    if(node.getDataLength()==4) tiEntries.add(entry);
                    else if(node.getDataLength()==5) { // data before run 1700
                      this.setTriggerbits(intData[5]);
                    }
                    else if(node.getDataLength()==6) { // data after run 1700
//                      System.out.println("6 words "+intData[6]+" "+intData[7]);
                      this.setTriggerbits(intData[6]<<16|intData[7]);
                    }
                    else if(node.getDataLength()==7) { // data after run 1787
//                      System.out.println("7 words "+intData[6]+" "+intData[7]);
                      this.setTriggerbits(intData[6]|intData[7]<<32);
                    }
                }
            }
        }
        return tiEntries;
    }   
    
    public void setPhaseOffset(int offset) {
    	this.phase_offset = offset;
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
   
    public int getFCTrigger() {    	    
    	    return (int)(getTriggerbits())&0x00000000ffffffff;
    }
    
    public int getCDTrigger() {
    	    return (int)(getTriggerbits()>>32)&0x00000000ffffffff;
    }
    
    public long getTriggerbits() {
    	    return this.triggerBits;    	
    }
    
    public long getPhase() {
    	    return ((this.timeStamp%6)+this.phase_offset)%6;
    }
    
    public int getBitsec() {    
    	int trig = getFCTrigger();
        if (trig>0) return (int) (Math.log10(trig>>8)/0.301+1);
        return 0;
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
        
        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc","CTOF::adc","CND::adc","LTCC::adc"};
        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC};
        
        String[]        tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc","CTOF::tdc","CND::tdc","LTCC::tdc"};
        DetectorType[]  tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC};
        
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
        
        int    localRun  = this.codaDecoder.getRunNumber();
        int  localEvent  = this.codaDecoder.getEventNumber();
        long  timeStamp  = this.codaDecoder.getTimeStamp();
        long triggerBits = this.codaDecoder.getTriggerBits();
        
        if(nrun>0){
            localRun = nrun;
            localEvent = nevent;
        }
        bank.setInt("run",        0, localRun);
        bank.setInt("event",      0, localEvent);
        bank.setLong("trigger",   0, triggerBits);        
        bank.setFloat("torus",    0, torus);
        bank.setFloat("solenoid", 0, solenoid);        
        bank.setLong("timestamp", 0, timeStamp);        
        
        
        return bank;
    }
}
