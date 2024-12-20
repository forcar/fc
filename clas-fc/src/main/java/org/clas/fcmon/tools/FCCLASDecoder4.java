package org.clas.fcmon.tools;

import java.util.ArrayList;
import java.util.List;

import java.sql.Time;
import java.util.Date;

import org.jlab.detector.base.DetectorType;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioSource;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSync;

import org.jlab.detector.decode.CodaEventDecoder;
//import org.jlab.detector.decode.DaqScalers;
//import org.clas.fcmon.tools.CodaEventDecoder;
//import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.FADCData;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoWriterSorted;

import org.jlab.utils.benchmark.ProgressPrintout;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.utils.options.OptionParser;
import org.jlab.utils.system.ClasUtilsFile;

public class FCCLASDecoder4 {
       
    public CodaEventDecoder          codaDecoder = null; 
    public DetectorEventDecoder  detectorDecoder = null;
    public List<DetectorDataDgtz>       dataList = new ArrayList<DetectorDataDgtz>();    
    public HipoDataSync                   writer = null;
    public HipoDataEvent               hipoEvent = null;
    public String                   HipoFileName = null;
    public Boolean                isHipoFileOpen = false;  
    private boolean             isRunNumberFixed = false;
    private int                 decoderDebugMode = 0;
    private SchemaFactory          schemaFactory = new SchemaFactory();
    
    public int runno;
    public int evtno;
    public long timeStamp;
    public long triggerBits;
   
    public int phase_offset = 1;
    
    public FCCLASDecoder4(){    
        System.out.println("***************");
        System.out.println("*FCCLASDecoder4*");
        System.out.println("***************");
        codaDecoder = new CodaEventDecoder();
        detectorDecoder = new DetectorEventDecoder();
        writer = new HipoDataSync();
        hipoEvent = (HipoDataEvent) writer.createEvent();
        String dir = ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4");
        schemaFactory.initFromDirectory(dir);
    }
    
    public static FCCLASDecoder4 createDecoder(){
        FCCLASDecoder4 decoder = new FCCLASDecoder4();
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
        System.out.println("FCCLASDecoder4.openHipoFile(): Opening "+HipoFileName);
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
    
    public Bank getDataBankADC(String name, DetectorType type){
        
        List<DetectorDataDgtz> adcDGTZ = this.getEntriesADC(type);
        
        if(schemaFactory.hasSchema(name)==false) return null;    
        
        Bank adcBANK = new Bank(schemaFactory.getSchema(name), adcDGTZ.size());
        
        for(int i = 0; i < adcDGTZ.size(); i++){
            adcBANK.putByte("sector", i, (byte) adcDGTZ.get(i).getDescriptor().getSector());
            adcBANK.putByte("layer", i, (byte) adcDGTZ.get(i).getDescriptor().getLayer());
            adcBANK.putShort("component", i, (short) adcDGTZ.get(i).getDescriptor().getComponent());
            adcBANK.putByte("order", i, (byte) adcDGTZ.get(i).getDescriptor().getOrder());
            adcBANK.putInt("ADC", i, adcDGTZ.get(i).getADCData(0).getADC());
            adcBANK.putFloat("time", i, (float) adcDGTZ.get(i).getADCData(0).getTime());
            adcBANK.putShort("ped", i, (short) adcDGTZ.get(i).getADCData(0).getPedestal());            
            if(name == "BST::adc") adcBANK.putLong("timestamp", i, adcDGTZ.get(i).getADCData(0).getTimeStamp()); // 1234 = dummy placeholder value
            if(name.equals("BMT::adc")||name.equals("FMT::adc")|| name.equals("FTTRK::adc")){
            	    adcBANK.putInt("ADC", i, adcDGTZ.get(i).getADCData(0).getHeight());
                	adcBANK.putInt("integral", i, adcDGTZ.get(i).getADCData(0).getIntegral());
            	    adcBANK.putLong("timestamp", i, adcDGTZ.get(i).getADCData(0).getTimeStamp());
            }	
         }
        return adcBANK;
    }    
    
    public Bank getDataBankTDC(String name, DetectorType type){
        
        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);        
        if(schemaFactory.hasSchema(name)==false) return null;
        Bank tdcBANK = new Bank(schemaFactory.getSchema(name), tdcDGTZ.size());
        
        if(tdcBANK==null) return null;
        
        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.putByte("sector", i, (byte) tdcDGTZ.get(i).getDescriptor().getSector());
            tdcBANK.putByte("layer", i, (byte) tdcDGTZ.get(i).getDescriptor().getLayer());
            tdcBANK.putShort("component", i, (byte) tdcDGTZ.get(i).getDescriptor().getComponent());
            tdcBANK.putByte("order", i, (byte) tdcDGTZ.get(i).getDescriptor().getOrder());
            tdcBANK.putInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
        }
        return tdcBANK;
    }
    
    public Bank getDataBankUndecodedADC(String name, DetectorType type){
    	
        List<DetectorDataDgtz> adcDGTZ = this.getEntriesADC(type);
        Bank adcBANK = new Bank(schemaFactory.getSchema(name), adcDGTZ.size());
        
        for(int i = 0; i < adcDGTZ.size(); i++){
            adcBANK.putByte("crate", i, (byte) adcDGTZ.get(i).getDescriptor().getCrate());
            adcBANK.putByte("slot", i, (byte) adcDGTZ.get(i).getDescriptor().getSlot());
            adcBANK.putShort("channel", i, (short) adcDGTZ.get(i).getDescriptor().getChannel());
            adcBANK.putInt("ADC", i, adcDGTZ.get(i).getADCData(0).getADC());
            adcBANK.putFloat("time", i, (float) adcDGTZ.get(i).getADCData(0).getTime());
            adcBANK.putShort("ped", i, (short) adcDGTZ.get(i).getADCData(0).getPedestal());            
        }
        return adcBANK;
    }
    
    public Bank getDataBankUndecodedTDC(String name, DetectorType type){
        
        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);
        
        Bank tdcBANK = new Bank(schemaFactory.getSchema(name), tdcDGTZ.size());
        if(tdcBANK==null) return null;
        
        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.putByte("crate", i, (byte) tdcDGTZ.get(i).getDescriptor().getCrate());
            tdcBANK.putByte("slot", i, (byte) tdcDGTZ.get(i).getDescriptor().getSlot());
            tdcBANK.putShort("channel", i, (byte) tdcDGTZ.get(i).getDescriptor().getChannel());
            tdcBANK.putInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
        }
        return tdcBANK;
    }
    
    public Bank getDataBankUndecodedVTP(String name, DetectorType type){

        List<DetectorDataDgtz> vtpDGTZ = this.getEntriesVTP(type);

        Bank vtpBANK = new Bank(schemaFactory.getSchema(name), vtpDGTZ.size());
        if(vtpBANK==null) return null;

        for(int i = 0; i < vtpDGTZ.size(); i++){
            vtpBANK.putByte("crate", i, (byte) vtpDGTZ.get(i).getDescriptor().getCrate());
//            vtpBANK.setByte("slot", i, (byte) vtpDGTZ.get(i).getDescriptor().getSlot());
//            vtpBANK.setShort("channel", i, (short) vtpDGTZ.get(i).getDescriptor().getChannel());
            vtpBANK.putInt("word", i, vtpDGTZ.get(i).getVTPData(0).getWord());
        }
        return vtpBANK;
    } 
    
    public Bank getDataBankUndecodedSCALER(String name, DetectorType type){

        List<DetectorDataDgtz> scalerDGTZ = this.getEntriesSCALER(type);

        Bank scalerBANK = new Bank(schemaFactory.getSchema(name), scalerDGTZ.size());
        if(scalerBANK==null) return null;

        for(int i = 0; i < scalerDGTZ.size(); i++){
            scalerBANK.putByte("crate", i, (byte) scalerDGTZ.get(i).getDescriptor().getCrate());
            scalerBANK.putByte("slot", i, (byte) scalerDGTZ.get(i).getDescriptor().getSlot());
            scalerBANK.putShort("channel", i, (short) scalerDGTZ.get(i).getDescriptor().getChannel());
            scalerBANK.putByte("helicity", i, (byte) scalerDGTZ.get(i).getSCALERData(0).getHelicity());
            scalerBANK.putByte("quartet", i, (byte) scalerDGTZ.get(i).getSCALERData(0).getQuartet());
            scalerBANK.putLong("value", i, scalerDGTZ.get(i).getSCALERData(0).getValue());
        }
//        if(scalerBANK.rows()>0)scalerBANK.show();
        return scalerBANK;
    }
    
    public Event getDataEvent(DataEvent rawEvent){
        this.initEvent(rawEvent);
        return getDataEvent();
    }    
    
    public Event getDataEvent(){
        
        Event event = new Event();
        
        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc","CTOF::adc","CND::adc","LTCC::adc","HTCC::adc","BAND::adc"};
        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC,DetectorType.BAND};
        
        String[]        tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc","CTOF::tdc","CND::tdc","LTCC::tdc","HTCC::tdc","BAND::tdc"};
        DetectorType[]  tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC,DetectorType.BAND};
 
//        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc","CTOF::adc","CND::adc","LTCC::adc","HTCC::adc"};
//        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC};
        
//        String[]        tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc","CTOF::tdc","CND::tdc","LTCC::tdc","HTCC::tdc"};
//        DetectorType[]  tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.CTOF,DetectorType.CND,DetectorType.LTCC,DetectorType.HTCC};
        
        for(int i = 0; i < adcBankTypes.length; i++){
            Bank adcBank = getDataBankADC(adcBankNames[i],adcBankTypes[i]);
            if(adcBank!=null){
                if(adcBank.getRows()>0){
                    event.write(adcBank);
                }
            }
        }

        for(int i = 0; i < tdcBankTypes.length; i++){
            Bank tdcBank = getDataBankTDC(tdcBankNames[i],tdcBankTypes[i]);
            if(tdcBank!=null){
                if(tdcBank.getRows()>0){
                    event.write(tdcBank);
                }
            }
        }       

        for(int i = 0; i < adcBankTypes.length; i++){
            Bank adcBank = getDataBankADC(adcBankNames[i],adcBankTypes[i]);
            if(adcBank!=null){
                if(adcBank.getRows()>0){
                    event.write(adcBank);
                }
            }
        }

        for(int i = 0; i < tdcBankTypes.length; i++){
            Bank tdcBank = getDataBankTDC(tdcBankNames[i],tdcBankTypes[i]);
            if(tdcBank!=null){
                if(tdcBank.getRows()>0){
                    event.write(tdcBank);
                }
            }
        }
        
        return event;
    }

    public long getTriggerPhase() {    	
        long timestamp    = this.codaDecoder.getTimeStamp();
        int  phase_offset = 1;
        return ((timestamp%6)+phase_offset)%6; // TI derived phase correction due to TDC and FADC clock differences 
    }  
    
    public Bank createHeaderBank( int nrun, int nevent, float torus, float solenoid){

        if(schemaFactory.hasSchema("RUN::config")==false) return null;

        Bank bank = new Bank(schemaFactory.getSchema("RUN::config"), 1);

        int    localRun = this.codaDecoder.getRunNumber();
        int  localEvent = this.codaDecoder.getEventNumber();
        int   localTime = this.codaDecoder.getUnixTime();
        long  timeStamp = this.codaDecoder.getTimeStamp();
        long triggerBits = this.codaDecoder.getTriggerBits();

        if(nrun>0){
            localRun = nrun;
            localEvent = nevent;
        }

        /*
        // example of getting torus/solenoid from RCDB:
        if (Math.abs(solenoid)>10) {
            solenoid = this.detectorDecoder.getRcdbSolenoidScale();
        }
        if (Math.abs(torus)>10) {
            torus = this.detectorDecoder.getRcdbTorusScale();
        }
        */

        bank.putInt("run",        0, localRun);
        bank.putInt("event",      0, localEvent);
        bank.putInt("unixtime",   0, localTime);
        bank.putLong("trigger",   0, triggerBits);
        bank.putFloat("torus",    0, torus);
        bank.putFloat("solenoid", 0, solenoid);
        bank.putLong("timestamp", 0, timeStamp);


        return bank;
    }
    
    public Bank createTriggerBank(){

        if(schemaFactory.hasSchema("RUN::trigger")==false) return null;

        Bank bank = new Bank(schemaFactory.getSchema("RUN::trigger"), this.codaDecoder.getTriggerWords().size());

        for(int i=0; i<this.codaDecoder.getTriggerWords().size(); i++) {
            bank.putInt("id",      i, i+1);
            bank.putInt("trigger", i, this.codaDecoder.getTriggerWords().get(i));
        }
        return bank;
    }
    
    public Bank createEpicsBank(){
        if(schemaFactory.hasSchema("RAW::epics")==false) return null;
        if (this.codaDecoder.getEpicsData().isEmpty()==true) return null;
        String json = this.codaDecoder.getEpicsData().toString();
        Bank bank = new Bank(schemaFactory.getSchema("RAW::epics"), json.length());
        for (int ii=0; ii<json.length(); ii++) {
            bank.putByte("json",ii,(byte)json.charAt(ii));
        }
        return bank;
    }
    
    /**
     * create the RUN::scaler bank
     *
     * Requires:
     *   RAW::scaler
     *   event unix time from RUN::config
     *   fcup calibrations from CCDB
     *   run start time from RCDB
     * Otherwise returns null
     *
     * FIXME:  refactor this out more cleanly
     */
    
    public Bank createReconScalerBank(Event event){

        // abort if run number corresponds to simulation:
        if (this.detectorDecoder.getRunNumber() < 1000) return null;

        // abort if we don't know about the required banks:
        if(schemaFactory.hasSchema("RUN::config")==false) return null;
        if(schemaFactory.hasSchema("RAW::scaler")==false) return null;
        if(schemaFactory.hasSchema("RUN::scaler")==false) return null;

        // retrieve necessary input banks, else abort:
        Bank configBank = new Bank(schemaFactory.getSchema("RUN::config"),1);
        Bank rawScalerBank = new Bank(schemaFactory.getSchema("RAW::scaler"),1);
        event.read(configBank);
        event.read(rawScalerBank);
        if (configBank.getRows()<1 || rawScalerBank.getRows()<1) return null;

        // retrieve fcup calibrations from CCDB:
        IndexedTable fcupTable = this.detectorDecoder.scalerManager.
                getConstants(this.detectorDecoder.getRunNumber(),"/runcontrol/fcup");

        // get unix event time (in seconds), and convert to Java's date (via milliseconds):
        Date uet=new Date(configBank.getInt("unixtime",0)*1000L);

        // retrieve RCDB run start time:
        Time rst;
        try {
            rst = (Time)this.detectorDecoder.scalerManager.
                    getRcdbConstant(this.detectorDecoder.getRunNumber(),"run_start_time").getValue();
        }
        catch (Exception e) {
            // abort if no RCDB access (e.g. offsite)
            return null;
        }

        // seconds since 00:00:00, on their given day:
        final double s1 = rst.getSeconds()+60*rst.getMinutes()+60*60*rst.getHours();
        final double s2 = uet.getSeconds()+60*uet.getMinutes()+60*60*uet.getHours();

        // Run duration in seconds.  Nasty but works, until RCDB (uses java.sql.Time)
        // is changed to support full date and not just HH:MM:SS.  Meanwhile just
        // requires that runs last less than 24 hours.
        final double seconds = s2<s1 ? s2+60*60*24-s1 : s2-s1;

        // interpret/calibrate RAW::scaler into RUN::scaler:
//        DaqScalers r = DaqScalers.create(rawScalerBank,fcupTable,seconds);
//        if (r==null) return null;

        Bank scalerBank = new Bank(schemaFactory.getSchema("RUN::scaler"),1);
//        scalerBank.putFloat("fcup",0,r.getBeamCharge());
//        scalerBank.putFloat("fcupgated",0,r.getBeamChargeGated());
//        scalerBank.putFloat("livetime",0,r.getLivetime());

        return scalerBank;
    }
    

    
}
