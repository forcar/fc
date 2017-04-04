package org.clas.fcmon.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class HipoDST {
    
    static ECEngine ecEngine = new ECEngine();
    static int[] idet = {0,0,0,1,1,1,2,2,2};
	
    public static void init() {
        ecEngine.init();
        ecEngine.setVariation("default");   
        ecEngine.setStripThresholds(15,20,20);
        ecEngine.setPeakThresholds(15,20,20);
        ecEngine.setClusterCuts(6,10,10);

    }
    
	public static void readDST() {
		
//		HipoReader reader = new HipoReader();
		HipoDataSource reader = new HipoDataSource();
		reader.open("/Users/colesmith/hipo_test_ntuple.hipo");
		Integer current = reader.getCurrentIndex();
        Integer nevents = reader.getSize(); 
        System.out.println("Current event:"+current+" Nevents: "+nevents);
        for (int i=0; i<nevents; i++) {
            DataEvent event = reader.getNextEvent();
            if(event.hasBank("DST::event")==true) {              
            	DataBank bank = event.getBank("DST::event");
                System.out.println("nrows="+bank.rows());
            	bank.show();
            }
		}	
		reader.close();
	}
	
	public static void writeDST(){
		    
	   HipoWriter writer = new HipoWriter();
	        
//     writer.defineSchema("Event", 20, "id/I:px/F:py/F:pz/F");
       writer.defineSchema(new Schema("{20,DST::event}[1,id,BYTE][2,px,FLOAT][3,py,FLOAT][4,pz,FLOAT]"));	        
	   writer.setCompressionType(2);
	   writer.open("/Users/colesmith/hipo_test_ntuple.hipo");
       int nevents = 10;
	   int id = 127;
	   for(int i = 0; i < 1; i++){
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
	
    public static void writeHipo(String outputName, int compression, String keep, int debug, List<String> files){
        
        HipoWriter writer = new HipoWriter();
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

                System.out.println(" OPENNING FIRST FILE : " + files.get(i));
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
                writerFactory.show();
                //writeFactory.getSchemaEvent()
            }
            int nEvents = reader.getEventCount();
            for(int nev = 0; nev < nEvents; nev++){
                HipoEvent    event = reader.readHipoEvent(nev);
                HipoEvent outEvent = writerFactory.getFilteredEvent(event);                
                byte[] array = reader.readEvent(nev);
                DataEvent de = (DataEvent) new HipoDataEvent(array,reader.getSchemaFactory());
                ecEngine.processDataEvent(de);     
                if (saveEvent(de)) {
                    if (debug==1) de.getBank("ECAL::clusters").show();
                    writer.writeEvent(outEvent);
                }
//                writer.writeEvent(outEvent);
                progress.updateStatus();
            }
        }
        writer.close();
    }
    
    public static Boolean saveEvent(DataEvent de) {
        int[][]        clust = new int[6][3];
        Boolean[] good_clust = new Boolean[6];
        
        if (de.hasBank("ECAL::clusters")) {
            DataBank bank = de.getBank("ECAL::clusters");
            for (int ii=0; ii<6; ii++) {
                good_clust[ii]=false;
                for (int jj=0; jj<3; jj++) {
                    clust[ii][jj]=0;
                }
            }
            for(int loop = 0; loop < bank.rows(); loop++){
                int sec = bank.getByte("sector", loop);
                int det = idet[bank.getByte("layer", loop)];   
                clust[sec-1][det]++;
            }
            for (int s=0; s<6; s++) {
                good_clust[s]=clust[s][0]>0&&clust[s][0]<3&&
                              clust[s][1]>0&&clust[s][1]<3&&
                              clust[s][2]>0&&clust[s][2]<3;
            }
        }
        
        return good_clust[0]||good_clust[1]||good_clust[2]||good_clust[3]||good_clust[4]||good_clust[5];
                
    }
    
    public static void main(String[] args){   

        List<String> inputFileList = new ArrayList<String>();
        String keepBanks="ECAL" ; int compression=2; int debug=0; 
        String  inputFile="/Users/colesmith/kpp/decoded/clas12_000761_a00214.hipo";
        String outputFile="/Users/colesmith/kpp/test.hipo";

        
        OptionParser parser = new OptionParser();
        parser.addRequired("-o");
        parser.addOption("-keep", "ALL", "Selection of banks to keep in the output");
        parser.addOption("-c", "2","Compression algorithm (0-none, 1-gzip, 2-lz4)");
        parser.addOption("-d", "0", "Display cluster banks");
        
        parser.parse(args);
        
        outputFile    = parser.getOption("-o").stringValue();
        inputFileList = parser.getInputList();
        compression   = parser.getOption("-c").intValue();
        keepBanks     = parser.getOption("-keep").stringValue();
        debug         = parser.getOption("-d").intValue();
        
        inputFileList.add(0,inputFile);
        HipoDST.init();
        HipoDST.writeHipo(outputFile, compression, keepBanks, debug, inputFileList);
        
// DST test        
//        HipoDST.writeDST();
//        HipoDST.readDST();
    }
    
}
