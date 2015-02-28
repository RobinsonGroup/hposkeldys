package hpoutil;


/** Command line parser from apache */
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;


import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import hpoutil.io.*;
import hpoutil.ontology.*;
import hpoutil.omim.*;


public class HPOUtil {

    private String pathToHpoOBOFile=null;
    private String pathToMorbidMap=null;
    private String pathToHPOAnnot=null;
    /** Key: a MIM ID, value: list of OMIM diseases */
    private HashMap<Integer,List<OMIMDisease> > omimmap=null;

    private HashMap<Integer,DiseaseAnnotation> diseasemap=null;

    /** A representation of the HPO Ontology */
    private HPO hpo=null;

    public static void main(String args[]) {
	HPOUtil hpoutil = new HPOUtil(args);
	hpoutil.parseHPOFile();
	hpoutil.parseMorbidMap();
	hpoutil.parseHPOAnnotationFiles();
    }




    public HPOUtil(String args[]) {
	parseCommandLineArguments(args);
	
    }


    public void parseMorbidMap() {
	MorbidMap map = new MorbidMap(this.pathToMorbidMap);
	this.omimmap=map.getOMIMDiseaseMap();
    }


    public void parseHPOFile() {
	HPOParser parser = new HPOParser(pathToHpoOBOFile); 
	this.hpo = new HPO(parser.getTermList());
    }

    public void parseHPOAnnotationFiles() {
	HPOAnnotationFileParser parser = new HPOAnnotationFileParser(this.pathToHPOAnnot);
	this.diseasemap = parser.getDiseaseMap();
	//private HashMap<Integer,List<OMIMDisease> > omimmap=null;
	for (Integer mimID:this.omimmap.keySet()) {
	    List<OMIMDisease> lst = this.omimmap.get(mimID);
	    for (OMIMDisease disease:lst) {
		if (disease.is_modifier())
		    continue; // Not using this information for the nosology
		if (disease.is_somatic()) {
		    continue; // Not using somatic mutations for the skel nos
		}
		DiseaseAnnotation da = this.diseasemap.get(mimID);
		ArrayList<String> genelist = disease.getGenes(); 
		da.addGeneList(genelist);
	    }
	}
	//for (String
    }



    
     /**
     * Parse the command line using apache's CLI. A copy of the library is included
     * in the Jannovar tutorial archive in the lib directory.
     * @see "http://commons.apache.org/proper/commons-cli/index.html"
     */
    private void parseCommandLineArguments(String[] args)
    {
	try {
	    Options options = new Options();
	    options.addOption(new Option("H","help",false,"Shows this help"));
	    options.addOption(new Option(null,"hpo",true,"Path to HPO OBO file."));
	    options.addOption(new Option("M","morbidmap",true,"Path to morbidmap file"));
	    options.addOption(new Option("A","annot",true,"Path to HPO annotation file directory"));
	    
	    Parser parser = new GnuParser();
	    CommandLine cmd = parser.parse(options,args);
	    if ( cmd.hasOption("H")){
		usage();
	    }
	    if (cmd.hasOption("hpo")) {
		this.pathToHpoOBOFile = cmd.getOptionValue("hpo");
	    } else {
		usage();
	    }
	    if (cmd.hasOption("M")) {
		this.pathToMorbidMap=cmd.getOptionValue("M");
	    } else {
		usage();
	    }
	    if (cmd.hasOption("A")) {
		this.pathToHPOAnnot=cmd.getOptionValue("A");
	    } else {
		usage();
	    }
	} catch (ParseException pe) {
	    System.err.println("Error parsing command line options");
	    System.err.println(pe.getMessage());
	    System.exit(1);
	}
    }


     /**
     * Prints a usage message to the console.
     */
    public static void usage() {
	System.err.println("[INFO] Usage: java -jar HPOUtil ");
	System.err.println("[INFO] where");
	System.err.println("[INFO]");
	
	System.exit(1);
    }


}
