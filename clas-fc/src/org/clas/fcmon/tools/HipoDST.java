package org.clas.fcmon.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.clas.fcmon.ftof.DataProvider;
import org.clas.fcmon.ftof.TOFPaddle;
import org.jlab.hipo.data.HipoEvent;
import org.jlab.hipo.data.HipoGroup;
import org.jlab.hipo.io.HipoReader;
import org.jlab.hipo.io.HipoWriter;
import org.jlab.hipo.schema.Schema;
import org.jlab.hipo.schema.SchemaFactory;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.service.ec.ECEngine;
import org.jlab.utils.benchmark.ProgressPrintout;
import org.jlab.utils.options.OptionParser;

// L.C. Smith, G. Gavalian
// Code below adapted from clas12rec/clas-reco/src/main/java/org/jlab/clas/reco/io/HipoFileUtils.java 
// Used to filter data based on result of method saveEvent.  In this example I use ECEngine to recreate
// ECAL::clusters and cut on the number of cluster to reject cosmics


public class HipoDST {
    
    static HipoWriter writer = new HipoWriter();
    static ECEngine ecEngine = new ECEngine();
    static int[] idet = {0,0,0,1,1,1,2,2,2};
	
    public static void init(int filter) {
        ecEngine.init();
        ecEngine.setVariation("default");  
        switch (filter) {
        case 0: ecEngine.setStripThresholds(15,20,20);
                ecEngine.setPeakThresholds(15,20,20);
                ecEngine.setClusterCuts(6,10,10); break;
        case 1: ecEngine.setStripThresholds(10,9,8);
                ecEngine.setPeakThresholds(18,20,15);
                ecEngine.setClusterCuts(7,15,20);                       
                writer.defineSchema(new Schema("{20,MIP::event}[1,sector,BYTE][2,mip,BYTE]"));           
        }       
    }

    public static void writeHipo(String outputName, int compression, int filter, String keep, int debug, List<String> files){
        
        writer.open(outputName);
        int nFiles = files.size();
        writer.setCompressionType(compression);
        
        String[] keepSchema = keep.split(":");
        SchemaFactory writerFactory = new SchemaFactory();
        ProgressPrintout  progress = new ProgressPrintout();
                
        for(int i = 0; i < nFiles; i++){
            HipoReader reader = new HipoReader();
            reader.open(files.get(i));
            if(i==0){
                SchemaFactory factory = reader.getSchemaFactory();
                System.out.println(" OPENING FIRST FILE : " + files.get(i));
                System.out.println(" Scanning Schema FACTORY");
                List<Schema> list = factory.getSchemaList();
        
                for(Schema schema : list){
                    for(String key : keepSchema){
                        if(schema.getName().contains(key)==true||keep.compareTo("ALL")==0){
                            writerFactory.addSchema(schema);
                            writerFactory.addFilter(schema.getName());
                            writer.defineSchema(schema);
                            System.out.println("\t >>>>> adding schema to writer : " + schema.getName());
                        }
                    }
                }
                if(filter==1) {                    
                    Schema mip = new Schema("{20,MIP::event}[1,sector,BYTE][2,mip,BYTE]");
                    writerFactory.addSchema(mip);
                    writerFactory.addFilter(mip.getName());
                    writer.defineSchema(mip);           
                    System.out.println("\t >>>>> adding schema to writer : " + mip.getName());
                }
                writerFactory.show();
            }
            int nEvents = reader.getEventCount();
            for(int nev = 0; nev < nEvents; nev++){
                HipoEvent    event = reader.readHipoEvent(nev);
                HipoEvent outEvent = writerFactory.getFilteredEvent(event);                
                byte[] array = reader.readEvent(nev);
                DataEvent de = (DataEvent) new HipoDataEvent(array,reader.getSchemaFactory());
                ecEngine.processDataEvent(de);     
                if (saveEvent(de,outEvent,filter)) {
                    if (debug==1) de.getBank("ECAL::clusters").show();
                    writer.writeEvent(outEvent);
                }
                progress.updateStatus();
            }
        }
        writer.close();
    }
    
    public static Boolean saveEvent(DataEvent de, HipoEvent event, int filter) {
        
        switch (filter) {
        case 0: return savePionEvent(de);
        case 1: return savePizeroEvent(de, event);
        }
        
        return false;
        
    }
    
    public static Boolean savePizeroEvent(DataEvent de, HipoEvent event) {
        
        int[][]        clust = new int[6][3];
        Byte[]      good_mip = new Byte[6];
        Boolean[] good_clust = new Boolean[6];
        
        List<TOFPaddle> paddleList = DataProvider.getPaddleList(de);
        
        HipoGroup group = writer.getSchemaFactory().getSchema("MIP::event").createGroup(6);
        
        clust = getClusters(de);
        
        for (int s=0; s<6; s++) { 
            good_mip[s]=isGoodTOFMIP(paddleList,s+1);     // goodMIP in FTOF 1A or 1B                            
            good_clust[s]=good_mip[s]==0&&clust[s][0]>1;  // 2+ clusters in PCAL and no FTOF MIP                            
            group.getNode("sector").setByte(s, (byte)(s+1));
            group.getNode("mip").setByte(s, good_mip[s]);
        }
        
        event.writeGroup(group);
        
        return  good_clust[0]||good_clust[1]||good_clust[2]||good_clust[3]||good_clust[4]||good_clust[5];
    }
    
    public static Boolean savePionEvent(DataEvent de) {
        int[][]        clust = new int[6][3];
        Boolean[] good_clust = new Boolean[6];
    
         clust = getClusters(de);
         
        //Cluster multiplicity filter to reject vertical cosmics
         for (int s=0; s<6; s++) { 
             good_clust[s]=clust[s][0]>0&&clust[s][0]<3&&
                           clust[s][1]>0&&clust[s][1]<3&&
                           clust[s][2]>0&&clust[s][2]<3;             
              
         }
         return good_clust[0]||good_clust[1]||good_clust[2]||good_clust[3]||good_clust[4]||good_clust[5];                
     }
    
    public static int[][] getClusters(DataEvent de) {
        
        int[][]        clust = new int[6][3];
    
        if (de.hasBank("ECAL::clusters")) {
            DataBank bank = de.getBank("ECAL::clusters");
            for (int ii=0; ii<6; ii++) {
                for (int jj=0; jj<3; jj++) {
                    clust[ii][jj]=0;
                }
            }
            for(int loop = 0; loop < bank.rows(); loop++){
                int sec = bank.getByte("sector", loop);
                int det = idet[bank.getByte("layer", loop)];   
                clust[sec-1][det]++;
            }
        }
        
        return clust;
    }
    
    public static Byte isGoodTOFMIP(List<TOFPaddle> paddlelist, int sector) {
        
        byte goodMIP = 0;
        double[] thresh = {500,1000,1000};
        
        for (TOFPaddle paddle : paddlelist){           
            int toflay = paddle.getDescriptor().getLayer();            
            if (paddle.getDescriptor().getSector()==sector&&paddle.geometricMean()>thresh[toflay-1]) goodMIP = 1;
        }
        
        return goodMIP;
    }
    
    // Code below tests writing and reading customized banks in HIPO without need of JSON file
    // Would be used to create mini-DSTs from filtering decoded or cooked HIPO files
    // Based on hipo-io/src/main/java/org/jlab/hipo/tests/WriterTests.java 
    
    public static void readDST() {
        
        HipoDataSource reader = new HipoDataSource();
        reader.open("/Users/colesmith/hipo_test_ntuple.hipo");
        Integer current = reader.getCurrentIndex();
        Integer nevents = reader.getSize(); 
        System.out.println("Current event:"+current+" Nevents: "+nevents);
        for (int i=0; i<nevents; i++) {
            DataEvent event = reader.getNextEvent();
            if(event.hasBank("DST::event")) { 
                DataBank bank = event.getBank("DST::event");
                bank.show();
                for (int j=0; j<bank.rows(); j++) {
                    System.out.println("id="+bank.getByte("id", j));
                    System.out.println("px="+bank.getFloat("px", j));
                    System.out.println("py="+bank.getFloat("py", j));
                    System.out.println("pz="+bank.getFloat("pz", j));
                }
            }
        }   
        reader.close();
    }
    
    public static void writeDST(){
            
       HipoWriter writer = new HipoWriter();
            
       writer.defineSchema(new Schema("{20,DST::event}[1,id,BYTE][2,px,FLOAT][3,py,FLOAT][4,pz,FLOAT]"));           
       writer.setCompressionType(2);
       writer.open("/Users/colesmith/hipo_test_ntuple.hipo");

       int nevents = 1;
       int id = 127;
       for(int i = 0; i < nevents; i++){
           HipoGroup group = writer.getSchemaFactory().getSchema("DST::event").createGroup(4);
           for (int j=0; j < 4; j++) {
               group.getNode("id").setByte(j, (byte)id);
               group.getNode("px").setFloat(j, (float) Math.random());
               group.getNode("py").setFloat(j, (float) Math.random());
               group.getNode("pz").setFloat(j, (float) Math.random());
           }
           
           HipoDataBank bank = new HipoDataBank(group);
           bank.show();
           HipoEvent event = writer.createEvent();
           event.writeGroup(group);
           writer.writeEvent(event);
       }
       writer.close();
    }
        
    public static void main(String[] args){   

        List<String> inputFileList = new ArrayList<String>();
        String keepBanks="ECAL" ; int compression=2; int debug=0; int filter=1;
        String  inputFile="/Users/colesmith/kpp/hipo/clas_000809-0-21.hipo";
        String outputFile="/Users/colesmith/kpp/hipo/test.hipo";

        if(args.length==0) {   
            //HipoDST.writeDST();
           // HipoDST.readDST();
            inputFileList.add(inputFile);
            HipoDST.init(filter);
            HipoDST.writeHipo(outputFile, compression, filter, keepBanks, debug, inputFileList);
            return;
        }
                               
        OptionParser parser = new OptionParser();
        parser.addRequired("-o");
        parser.addOption("-keep", "ALL", "Selection of banks to keep in the output");
        parser.addOption("-c", "2","Compression algorithm (0-none, 1-gzip, 2-lz4)");
        parser.addOption("-d", "0", "Display cluster banks");
        parser.addOption("-f", "0", "Filter pion (0) or pizero (1) events");
        
        parser.parse(args);
        
        outputFile    = parser.getOption("-o").stringValue();
        inputFileList = parser.getInputList();
        compression   = parser.getOption("-c").intValue();
        keepBanks     = parser.getOption("-keep").stringValue();
        debug         = parser.getOption("-d").intValue();
        filter        = parser.getOption("-f").intValue();
        
//        inputFileList.add(0,inputFile);
        
        HipoDST.init(filter);
        HipoDST.writeHipo(outputFile, compression, filter, keepBanks, debug, inputFileList);
        
    }
    
}
