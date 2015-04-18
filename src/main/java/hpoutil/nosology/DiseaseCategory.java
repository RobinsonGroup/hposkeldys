package hpoutil.nosology;

import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.HashMap;
import java.io.Writer;
import java.io.IOException;

import hpoutil.ontology.*;
import hpoutil.omim.DiseaseAnnotation;

/**
 * This class represents one of the 40 skeletal nosology categories
 * @author Peter Robinson
 * @version 0.02 (1 March 2015)
 */
public class DiseaseCategory {

    private static Logger log = Logger.getLogger(DiseaseCategory.class.getName());
    /** One of the 40 names of the skeletal nosology */
    private String categoryname=null;
    /** Number of the category in the 2010 nosology. */
    private int categorynumber=0;
    /** reference to the HPO object */
    private static HPO hpo;
    /** Key: integer representation of the OMIM id; Value: name of the disease */
    private HashMap<Integer,String> goldstandard=null;
    /** Count of diseases we evaluated but found not to be members of this category */
    int notMemberCount=0;
    /** A list of 1 or more definitions for a disease to belong to this category */
    ArrayList<Classifier> classifierList=null;


    ArrayList<DiseaseAnnotation> goodCandidate=new ArrayList<DiseaseAnnotation>();
    /** Keep a list of the false negative candidates here. */
    ArrayList<DiseaseAnnotation> notFound=new ArrayList<DiseaseAnnotation>();

    public void addGoldStandard(HashMap<Integer,String> gs) { this.goldstandard=gs; }

    /**
     * The constructor takes the name of the disease and if applicable the disease genes.
     * @param name The name of the category (one of the 40)
     * @param diseasegenes (Can be null) names of the diseases genes of the category
     * @param gs Gold standard diseases
     */
    public DiseaseCategory(String name,Integer number,HashMap<Integer,String> gs) {
	this.categoryname=name;
	this.categorynumber=number;
	this.goldstandard=gs;
	this.classifierList=new ArrayList<Classifier>();
    }
    


    public void addClassifier(Classifier def) {
	this.classifierList.add(def);
    }


    static public void setHPO(HPO hpo) {
	DiseaseCategory.hpo=hpo;
    }

    public ArrayList<Integer> getMemberList() {
	ArrayList<Integer> l = new ArrayList<Integer>();
	for (DiseaseAnnotation da:this.goodCandidate) {
	    Integer id = da.MIMid();
	    l.add(id);
	}
	return l;
    }

  


  

    


    // private String getDiseaseCategory
    public static  ArrayList<DiseaseCategory> categorylist=null;
    public static void setCategoryList( ArrayList<DiseaseCategory> list) {
	DiseaseCategory.categorylist=list;
    }

    private String getCategory(Integer disease) {
	for (DiseaseCategory dc : DiseaseCategory.categorylist) {
	    if (dc.goldstandard.containsKey(disease)) {
		return  String.format("%d->%s", dc.categorynumber,dc.categoryname);
   	    }
	}
	return "no other category";
    }

    public void printOutput(Writer out) throws IOException {
	ArrayList<String> newprediction = new ArrayList<String>();
	int n_gc_identified=0;
	HashMap<Integer,Boolean> foundGS=new HashMap<Integer,Boolean>();
	for (Integer id : this.goldstandard.keySet()) {
	    foundGS.put(id,false);
	}
	if (categorynumber != 0)
	    out.write(this.categorynumber + ".");
	out.write(this.categoryname + "\n");

	for (Classifier c : this.classifierList) {
	    c.printDefinition(out);
	}
	out.write("Predictions:\n");
	for (DiseaseAnnotation da : goodCandidate) {
	    Integer id = da.getDiseaseId();
	    if (goldstandard.containsKey(id)) {
		n_gc_identified++;
		out.write(n_gc_identified + ") " + da.toString() + "[+]\n");
		foundGS.put(id,true);
	    } else {
		String cat=getCategory(id);
		String dis=String.format("%s %s",da.toString(),cat);
		newprediction.add(dis);
	    }
	}
	int tot = goldstandard.size();
	out.write(String.format("I got %d/%d (%.1f%%) of the gold standard diseases.\n",n_gc_identified,tot,((double)100*n_gc_identified/tot)));
	if (n_gc_identified < tot) {
	    int i=0;
	    for (Integer id : foundGS.keySet()) {
		if (foundGS.get(id))
		    continue;
		else {
		    String mimname = goldstandard.get(id);
		    if (mimname==null) mimname="?";
		    i++;
		    String ret = String.format("MIM:%06d %s",id,mimname);
		    out.write(i + ". Did not found disease " + ret + "\n");

		}
	    }
	    i=0;
	    for (DiseaseAnnotation notda:this.notFound) {
		i++;
		out.write(i + ". Did not find:" + notda + "\n");
	    }
	}
	if (newprediction.size()==0) {
	    out.write("No new predictions\n");
	} else {
	    out.write(String.format("New predictions (n=%d):\n",newprediction.size()));
	    int i=0;
	    for (String s: newprediction) {
		out.write("\t" + (++i) + ": " +s+"\n");
	    }
	}


    }


    /** @return Name of the category.*/
    public String getName() { return this.categoryname; }

    /**
     * Try to identify all diseases that can be assigned to this category.
     * As a side effect, add all found diseases to {@link #goodCandidate}, and all
     * all diseases that are not found but are in the gold standard to 
     * {@link #notFound}.
     * @param diseasemap HashMap with all of the OMIM diseases and their annotations.
     */
    public void findMembers(HashMap<Integer,DiseaseAnnotation> diseasemap) {
	for (Integer id:diseasemap.keySet()) {
	    DiseaseAnnotation d = diseasemap.get(id);
	    boolean ok = evaluateCandidateDisease(d);
	    if (ok) {
		goodCandidate.add(d);
	    } else {
		if (this.goldstandard.containsKey(id)) {
		    notFound.add(d);
		}
	    }
	}
    }



   





    public boolean evaluateCandidateDisease(DiseaseAnnotation disease) {
	boolean verbose=false;
	//if (disease.MIMid().equals(200600))
	//  verbose=true;

	for (Classifier c : this.classifierList) {
	    if (c.satisfiesDefinition(disease) )
		return true;
	}
	return false; // Definition not satisfied at all.
    }
   
}


/* eof */
