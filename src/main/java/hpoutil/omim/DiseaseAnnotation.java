package hpoutil.omim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents an individual disease and its
 * annotations to HPO terms and other metadata.
 * For now, we will extract the disease ID (e.g., OMIM:123456),
 * the disease name (e.g., Smith Syndrome), as well as the
 * individual HPO annotations and any negations.
 * @author peter.robinson@charite.de, Sebastian Bauer
 * @version 0.3 March 21, 2015
 */
public class DiseaseAnnotation {
    /** ID heading column number for annotation file (used only for the parse) */
    private static final String COL_ID = "ID";
    /** Name heading column number for annotation file (used only for the parse) */
    private static final String COL_NAME = "NAME";
    /** NOT heading column number for annotation file (used only for the parse) */
    private static final String COL_NEGATION = "NEG";
    /** HPO (annotation) heading column number for annotation file (used only for the parse) */
    private static final String COL_HPO = "HPO";
    /** Age of ontset name ("Age of Onset Name") heading column number for annotation file (used only for the parse) */
    private static final String COL_AO = "AO";
  


    /** Number of columns for annotation file (used only for the parse) */
    private static final String NO_COLUMNS = "NO_COLUMNS";

    private static final String[] COLUMNS = new String[] {
    	COL_ID, COL_NAME, COL_HPO, COL_NEGATION, COL_AO
    };
    
    /** the name of this disease (e.g., Smith syndrome). */
    private String diseaseName = null;
    
    /** the accession number of the disease (e.g.,OMIM:123456) */
    private Integer diseaseId = null;
    /** A list of all of the (positive and negative) annotations of this disease */
    private ArrayList<AnnotationItem> annotationItems;

    /** List of germline disease genes for this disease */
    private ArrayList<String> diseaseGenes=null;

    /** List of somatic (de novo) disease genes for this disease */
    private ArrayList<String> somaticDiseaseGenes=null;


    public DiseaseAnnotation(String filename) {
	annotationItems = new ArrayList<AnnotationItem>();
	this.parseDiseaseFile(filename);
    }

    /**
     * Return a list of items joined by a comma */
    private String join(ArrayList<String> al) {
	if (al==null || al.size()==0)
	    return "-";
	if (al.size()==1)
	    return al.get(0);
	StringBuilder sb = new StringBuilder();
	sb.append(al.get(0));
	int len = al.size();
	for (int i=1;i<len;i++) {
	    sb.append("," + al.get(i));
	}
	return sb.toString();
    }


    public String toString() {
	String genes = join(diseaseGenes);
	return String.format("MIM:%06d %s [%s]", this.diseaseId,this.diseaseName,genes);
    }


    /**
     * Returns true if there is a matching disease gene for the
     * argument sym.
     */
    public boolean hasDiseaseGene(String sym) {
	if (this.diseaseGenes==null)
	    return false;
	for (String gene:this.diseaseGenes)
	    if (gene.equals(sym))
		return true;
	return false;

    }


    public void addGeneList(ArrayList<String> lst) {
	if (this.diseaseGenes==null) 
	    this.diseaseGenes=new ArrayList<String>();
	this.diseaseGenes.addAll(lst);
    }

    public void addDiseaseGene(String symbol) {
	if (this.diseaseGenes==null) 
	    this.diseaseGenes=new ArrayList<String>();
	this.diseaseGenes.add(symbol);
    }

    public void addSomaticDiseaseGene(String symbol){
	if (this.somaticDiseaseGenes==null) 
	    this.somaticDiseaseGenes=new ArrayList<String>();
	this.somaticDiseaseGenes.add(symbol);
    }


    /** This method parses the header of an HPO annotation file
     * in order to determine the index numbers of the important fields.
     */
    private Map<String, Integer> parseHeader(String headerLine) {
    	Map<String, Integer> headerMap = new LinkedHashMap<String, Integer>();
	String[] segments = headerLine.split("\\t");
	for (int i = 0; i < segments.length; i++) {
	    if (segments[i].equals("Disease ID")) {
		headerMap.put(COL_ID, i);
	    }
	    if (segments[i].equals("Disease Name")) {
		headerMap.put(COL_NAME, i);
	    }
	    if (segments[i].equals("Phenotype ID")) {
		headerMap.put(COL_HPO, i);
	    }
	    if (segments[i].equals("Negation ID")) {
		headerMap.put(COL_NEGATION, i);
	    }
	    if (segments[i].equals("Age of Onset Name")) {
		headerMap.put(COL_AO, i);
	    }
	}
	for (String column : COLUMNS) {
	    if (!headerMap.containsKey(column)) {
		throw new RuntimeException("Column: " + column + " is not present in the header.");
	    }
	}
	int noColumns = Math.max(Math.max(Math.max(headerMap.get(COL_ID), headerMap.get(COL_NAME)), headerMap.get(COL_NEGATION)),headerMap.get(COL_HPO));
	
	headerMap.put(NO_COLUMNS, noColumns);
    	return headerMap;
    }


    public Integer MIMid() {
	return this.diseaseId;
    }


    public ArrayList<Integer> getPositiveAnnotations() {
	ArrayList<Integer> lst = new ArrayList<Integer>();
	for (AnnotationItem item : this.annotationItems) {
	    if (item.is_negated())
		continue;
	    lst.add(item.getHPOid());
	}
	return lst;
    }

     public ArrayList<Integer> getNegativeAnnotations() {
	ArrayList<Integer> lst = new ArrayList<Integer>();
	for (AnnotationItem item : this.annotationItems) {
	    if (item.is_negated())
		lst.add(item.getHPOid());
	}
	return lst;
    }

    /**
     * @return List of annotations that have neonatal onset
     */
    public ArrayList<Integer>  getNeonatalAnnotations() {
	ArrayList<Integer> lst = new ArrayList<Integer>();
	for (AnnotationItem item : this.annotationItems) {
	    if (item.is_neonatal())
		lst.add(item.getHPOid());
	}
	return lst;
    }

     /**
     * @return List of annotations that have neonatal onset
     */
    public ArrayList<Integer>  getCongenitalAnnotations() {
	ArrayList<Integer> lst = new ArrayList<Integer>();
	for (AnnotationItem item : this.annotationItems) {
	    if (item.is_congenital())
		lst.add(item.getHPOid());
	}
	return lst;
    }

    
    /**
     * Create a disease annotation from the given file.
     * @param filename One of the "small files" from the HPO project, i.e., an annotation file for one disease
     */
    private void parseDiseaseFile(String filename) {
	try {
	    BufferedReader br = new BufferedReader(new FileReader(filename));
	    String header = br.readLine();
	    Map<String, Integer> headerMap = parseHeader(header);
	    
	    int lineNo = 1;
	    while (br.ready()) {
		String line = br.readLine();
		boolean negative = false; // is this line a NOT line (ie disease does not have this feature).
	    	
		String[] segments = line.split("\\t");
		if (segments.length == 0 || line.equals("")) {
		    /* There are some empty lines in the input file, apparently a minor Phenote bug,
		       but we can just skip them. */
		    continue; 
		}    		    
		if (segments.length <= headerMap.get(NO_COLUMNS)) {
		    System.err.println("[WARN] Ignoring line " + lineNo + " of " + filename + " due to missing columns");
		    System.err.println("[WARN] Number of fields was " + segments.length + ", but I was expecting at least " + headerMap.get(NO_COLUMNS) + " fields");
		    System.err.println("[WARN] The line was: " + line);
		    continue;
		}
	  	
		if (lineNo == 1) {
		    diseaseName = segments[headerMap.get(COL_NAME)];
		    String id = segments[headerMap.get(COL_ID)];
		    this.diseaseId = parseMimId(id);
		    lineNo++;
		}
		
		String negation = segments[headerMap.get(COL_NEGATION)];
		if (negation.equalsIgnoreCase("NOT")) {
		    negative = true;
		}
		String hpo = segments[headerMap.get(COL_HPO)];
		AnnotationItem item=null;
	    	try {
		    item = new AnnotationItem(hpo, negative);
		} catch(IllegalArgumentException e) {
		    System.err.println("[DiseaseAnnotation.java ERROR]:" + e.getMessage());
		    System.err.println("Affected line is "+ line);
		    //System.exit(1);
		    continue;
		}


		String age = segments[headerMap.get(COL_AO)];
		if (!age.isEmpty() ) {
		    if (age.equalsIgnoreCase("Congenital onset")) {
			item.set_congenital_age();
		    }
		    if (age.equalsIgnoreCase("Neonatal onset")) {
			item.set_neonatal_age();
		    }
		    if (age.equalsIgnoreCase("Infantile onset") || age.equalsIgnoreCase("Onset in infancy")) {
			item.set_infantile_age();
		    }
		    if (age.equalsIgnoreCase("Childhood onset") || age.equalsIgnoreCase("Juvenile onset")) {
			item.set_childhood_age();
		    }
		    if (age.equalsIgnoreCase("Onset in adolescence") || age.equalsIgnoreCase("Adult onset") ||
			age.equalsIgnoreCase("Young adult onset") || age.equalsIgnoreCase("Onset in early adulthood") || 
			age.equalsIgnoreCase("Late onset")) {
			item.set_adult_age();
		    }
		}
		
		annotationItems.add(item);
	    }
	    
	    br.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Ths ID can start with MIM:123456 or OMIM:123456
     * Return the corresponding Integer value */
    private Integer parseMimId(String id) {
	if (! id.startsWith("OMIM:") && ! id.startsWith("MIM:")) {
	    throw new IllegalArgumentException("Invalid MIM is in DiseaseAnnotation.java: " + id);
	}
	int x = id.indexOf(":");
	id = id.substring(x+1).trim();
	if (id.length() != 6) {
	    throw new IllegalArgumentException("Invalid MIM is in DiseaseAnnotation.java: " + id + "(length=" + id.length() + ")");
	}
	return Integer.parseInt(id);
    }


      
    public Iterator<AnnotationItem>  get_item_iterator(){
    	return this.annotationItems.iterator();
    }
    
    public String getDiseaseName() {
	return diseaseName;
    }
    
    /**
     * @return an Integer representation of a MIM id, e.g., for OMIM:123456 return 123456
     */
    public Integer getDiseaseId() {
	return this.diseaseId;
    }
    
    /**
     * @return true if there is at least one annotation for this disease, otherwise false (probably something went wrong in parsing)
     */
    public boolean isValid() {
    	return (annotationItems.size() > 0);
    }
    
    public static void main(String[] args) {
    	DiseaseAnnotation da = new DiseaseAnnotation("/home/tudor/EXTRA_SPACE/NEW_Charite/data/annotated/OMIM-300066.tab");
    	System.out.println(da.getDiseaseId());
	Iterator<AnnotationItem> item_it = da.get_item_iterator();
	while (item_it.hasNext()) {
	    AnnotationItem item = item_it.next();
	    
	    System.out.println(item.hpo_id + " - " + item.is_childhood());
	}
    }
}
