package org.clas.fcmon.tools;

import java.util.List;

import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioSource;
import org.jlab.clas.reco.io.EvioHipoEvent;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.utils.options.OptionParser;

public class MyEvioHipoEvent extends EvioHipoEvent {
    
    @Override
    public void fillHipoEventGenPart(HipoDataEvent hipoEvent, EvioDataEvent evioEvent){
        
        if(evioEvent.hasBank("GenPart::true")==true){
            EvioDataBank evioBank = (EvioDataBank) evioEvent.getBank("GenPart::true");
            HipoDataBank hipoBank = (HipoDataBank) hipoEvent.createBank("MC::Particle", evioBank.rows());
            for(int i = 0; i < evioBank.rows(); i++){
                hipoBank.setInt("pid", i, evioBank.getInt("pid", i));
                hipoBank.setFloat("px", i, (float) (evioBank.getDouble("px", i)/1000.0) );
                hipoBank.setFloat("py", i, (float) (evioBank.getDouble("py", i)/1000.0) );
                hipoBank.setFloat("pz", i, (float) (evioBank.getDouble("pz", i)/1000.0) );
                hipoBank.setFloat("vx", i, (float) (evioBank.getDouble("vx", i)) );
                hipoBank.setFloat("vy", i, (float) (evioBank.getDouble("vy", i)) );
                hipoBank.setFloat("vz", i, (float) (evioBank.getDouble("vz", i)) );
            }
            if(evioBank.rows()>0) hipoEvent.appendBanks(hipoBank);
        }
        
        if(evioEvent.hasBank("PCAL::true")==true){
            EvioDataBank evioBank = (EvioDataBank) evioEvent.getBank("PCAL::true");
            HipoDataBank hipoBank = (HipoDataBank) hipoEvent.createBank("MC::True", evioBank.rows());
            for(int i = 0; i < evioBank.rows(); i++){
                hipoBank.setFloat("avgX", i, (float) (evioBank.getDouble("avgX", i)) );
                hipoBank.setFloat("avgY", i, (float) (evioBank.getDouble("avgY", i)) );
                hipoBank.setFloat("avgZ", i, (float) (evioBank.getDouble("avgZ", i)) );
                hipoBank.setFloat("avgT", i, (float) (evioBank.getDouble("avgT", i)) );
            }
            if(evioBank.rows()>0) hipoEvent.appendBanks(hipoBank);
        } 
        
    }
    public static void main(String[] args){
        
        OptionParser parser = new OptionParser();
        parser.addRequired("-o");
        parser.addOption("-r","10");
        parser.addOption("-t","-1.0");
        parser.addOption("-s","1.0");
        parser.addOption("-n", "-1");
        
        parser.parse(args);
        
        if(parser.hasOption("-o")==true){
        
            String outputFile = parser.getOption("-o").stringValue();
        
            List<String> inputFiles = parser.getInputList();
            
            /*for(int i = 1; i < args.length; i++){
                inputFiles.add(args[i]);
            }*/
        
        
            MyEvioHipoEvent convertor = new MyEvioHipoEvent();
            
            HipoDataSync  writer = new HipoDataSync();
            writer.open(outputFile);
            writer.setCompressionType(2);
            System.out.println(">>>>>  SIZE OF THE INPUT FILES = " + inputFiles.size());
            int nevent = 1;
            int maximumEvents = parser.getOption("-n").intValue();
            
            for(String input : inputFiles){
                System.out.println(">>>>>  appending file : " + input);
                try {
                    EvioSource reader = new EvioSource();
                    reader.open(input);
                    
                    while(reader.hasEvent()==true){
                        EvioDataEvent evioEvent = (EvioDataEvent) reader.getNextEvent();                    
                        HipoDataEvent hipoEvent = convertor.getHipoEvent(writer, evioEvent);
                        int nrun = parser.getOption("-r").intValue();
                        float torus    = (float) parser.getOption("-t").doubleValue();
                        float solenoid = (float) parser.getOption("-s").doubleValue();
                        HipoDataBank header = convertor.createHeaderBank(hipoEvent, nrun, nevent, torus, solenoid);
                        hipoEvent.appendBanks(header);
                        writer.writeEvent(hipoEvent);
                        nevent++;
                        if(maximumEvents>0&&nevent>=maximumEvents) {
                            reader.close();
                            writer.close();
                            System.out.println("\n\n\n Finished output file at event count = " + nevent);
                            System.exit(0);
                        }
                }
            } catch (Exception e){
                e.printStackTrace();
                System.out.println(">>>>>  processing file :  failed ");
            }
            System.out.println(">>>>>  processing file :  success ");
            System.out.println();            
        }
        writer.close();
        }
        /*
        EvioDataDictionary dictionary = new EvioDataDictionary("/Users/gavalian/Work/Software/Release-9.0/COATJAVA/coatjava/etc/bankdefs/hipo");
        dictionary.show();
        
        EvioDataDescriptor desc = (EvioDataDescriptor) dictionary.getDescriptor("ECAL::dgtz");
        desc.show();*/
    }
}

