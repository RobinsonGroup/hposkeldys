package hpoutil.nosology;

import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.HashMap;
import java.io.Writer;

import hpoutil.ontology.*;
import hpoutil.omim.DiseaseAnnotation;

public class DiseaseCategory {

    private static Logger log = Logger.getLogger(DiseaseCategory.class.getName());

    private String categoryname=null;
    /** reference to teh HPO object */
    private static HPO hpo;

    private ArrayList<String> diseasegenes=null;
    private ArrayList<Integer> featurelist=null;
    private ArrayList<Integer>notFeaturelist=null;

    HashMap<Integer,String> goldstandard=null;
    /** Count of diseases we evaluated but found not to be members of this category */
    int notMemberCount=0;

    ArrayList<DiseaseAnnotation> goodCandidate=new ArrayList<DiseaseAnnotation>();

    public void addGoldStandard(HashMap<Integer,String> gs) { this.goldstandard=gs; }

    public DiseaseCategory( String name, ArrayList<String> diseasegenes,ArrayList<Integer> flist,ArrayList<Integer>notflist) {
	this.categoryname=name;
	this.diseasegenes=diseasegenes;
	this.featurelist=flist;
	this.notFeaturelist=notflist;
    }

    static public void setHPO(HPO hpo) {
	DiseaseCategory.hpo=hpo;
    }



    public void printOutput(Writer out) {
	ArrayList<String> newprediction = new ArrayList<String>();
	int n_gc_identified=0;
	for (DiseaseAnnotation da : goodCandidate) {
	    Integer id = da.getDiseaseId();
	    if (goldstandard.containsKey(id)) {
		n_gc_identified++;
		out.write(n_gc_identified + ") " + da.toString() + "[+]\n");
	    } else {
		newprediction.add(da.toString());
	    }
	}
	int tot = goldstandard.size();
	out.write(String.format("I got %d/%d (%.1f%%) of the gold standard diseases.\n",n_gc_identified,tot,((double)100*n_gc_identified/tot)));
	if (newprediction.size()==0) {
	    out.write("No new predictions\n");
	} else {
	    out.write("New predictions:\n");
	    for (String s: newprediction) {
		out.write("\t"+s+"\n");
	    }
	}


    }



    public String getName() { return this.categoryname; }


    public void findMembers(HashMap<Integer,DiseaseAnnotation> diseasemap) {
	for (Integer id:diseasemap.keySet()) {
	    DiseaseAnnotation d = diseasemap.get(id);
	    evaluateCandidateDisease(d);
	}
    }


    public void evaluateCandidateDisease(DiseaseAnnotation disease) {
	if (this.diseasegenes != null && this.diseasegenes.size()>0) {
	    boolean match=false; // at least one gene must match
	    for (String symbol:this.diseasegenes) {
		if (disease.hasDiseaseGene(symbol)) {
		    match=true;
		    break;
		}
	    }
	    if (!match) {
		notMemberCount++;
		return;
	    }
	}
	// If a disease has a NOT feature, then it cannot belong to this category
	boolean reject = false;
	for (Integer not: notFeaturelist) {
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	     for (Integer pos:positiveannotations) {
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(not,pos)){
			 reject=true;
			 break;
		     }
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	 }
	if (reject) {
	     notMemberCount++;
	     return;
	 }
	// If we get here, the candidate is good!
	goodCandidate.add(disease);
	System.out.println("Found candidate " + disease);
    }
    


}
