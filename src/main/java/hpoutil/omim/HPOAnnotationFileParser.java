package hpoutil.omim;



import org.apache.log4j.Logger;
//import org.apache.log4j.PropertyConfigurator;
import java.util.*;
import java.io.File;
import hpoutil.omim.*;

public class HPOAnnotationFileParser {

    private static Logger log = Logger.getLogger(HPOAnnotationFileParser.class.getName());

    HashMap<Integer,DiseaseAnnotation> diseasemap=null;



    public HPOAnnotationFileParser(String directorypath) {
	List<String> filePaths = getHPOAnnotationFiles(directorypath);
	this.diseasemap=new HashMap<Integer,DiseaseAnnotation>();
	int c=0;
	for (String p:filePaths) {
	    DiseaseAnnotation da = new DiseaseAnnotation(p);
	    if (!da.isValid()) {
    		System.out.println("Error extracting Disease Annotations for " + p);
    		System.out.println(da);
    		System.exit(1);
	    }
	    diseasemap.put(da.MIMid(),da);
	    c++;
	    if (c%500==0) {
		log.info("Parsed " + c + " HPO annotation files");
	    }
	}
    }

    public HashMap<Integer,DiseaseAnnotation> getDiseaseMap() { return this.diseasemap; }




    /**
     * Get a list of all HPO Annotation files from the annotation_diectory and
     * return the ArrayList (which then contains the full path for each file).
     * The files are the "small files" each of which contains the annotations
     * for a single disease, e.g., <b>OMIM-123456.tab</b>.
     */
    private List<String> getHPOAnnotationFiles(String annotationsFolder) {
	log.info("Finding all annotation files from directory: "  + annotationsFolder);
	List<String> paths = new ArrayList<String>();
	File folder = new File(annotationsFolder);
	if (folder.exists()) {
	    File[] files = folder.listFiles();
	    for (File file : files) {
		if (file.isFile()) {
		    if (file.getName().startsWith("OMIM") && ! file.getName().endsWith("~")) {
			paths.add(file.getAbsolutePath());
		    }
		}
	    }
	    log.info("Compiled a total of " + paths.size()+ " annotation files.");
	} else {
	    System.out.println("Could not initialize annotation folder: " + annotationsFolder);
	    System.out.println("Please check path (e.g., in makefile variable ANNOTDIR) and try again");
	    System.exit(1);
	}
	return paths;
    }



}
