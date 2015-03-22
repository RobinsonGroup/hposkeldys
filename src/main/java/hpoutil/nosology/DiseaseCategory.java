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

    private String categoryname=null;
    /** Number of the category in the 2010 nosology. */
    private int categorynumber=0;
    /** reference to teh HPO object */
    private static HPO hpo;

    private ArrayList<String> diseasegenes=null;
    private ArrayList<Integer> featurelist=null;
    private ArrayList<Integer>notFeaturelist=null;
    /** At least one of these features must be present */
    private ArrayList<Integer> optionalFeaturelist=null;
    /** At least {@link #N} descendents of this feature must be present
	to call the disease*/
    private ArrayList<Integer> featureNlist;
    /** Minimum number of times {@link #featureNlist} must be present
     Note: Order of arraylist matches.*/
    private ArrayList<Integer> N;
    private Integer neonatalFeature=null;
    /** Key: integer representation of the OMIM id; Value: name of the disease */
    private HashMap<Integer,String> goldstandard=null;
    /** Count of diseases we evaluated but found not to be members of this category */
    int notMemberCount=0;

    ArrayList<DiseaseAnnotation> goodCandidate=new ArrayList<DiseaseAnnotation>();
    /** Keep a list of the false negative candidates here. */
    ArrayList<DiseaseAnnotation> notFound=new ArrayList<DiseaseAnnotation>();

    public void addGoldStandard(HashMap<Integer,String> gs) { this.goldstandard=gs; }

    public void setOptionalList(ArrayList<Integer> list) {
	optionalFeaturelist=list;
    }
    /** Sets HPO terms that must be present at least N times. Note, 
	featureNlist.get(i) must be present N.get(i) times.
    */
    public void setFeatureN(ArrayList<Integer> featureNlist,ArrayList<Integer> N) {
	this.featureNlist=featureNlist;
	this.N=N;
    }

    public void setNumber(Integer n) { this.categorynumber=n; }

    public DiseaseCategory( String name, ArrayList<String> diseasegenes,ArrayList<Integer> flist,ArrayList<Integer>notflist) {
	this.categoryname=name;
	this.diseasegenes=diseasegenes;
	this.featurelist=flist;
	this.notFeaturelist=notflist;
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

    public void setNeonatalFeature(Integer hpo) {
	this.neonatalFeature=hpo;
    }


    private String join(String conn, ArrayList<String> lst) {
	StringBuilder sb = new StringBuilder();
	sb.append(lst.get(0));
	for (int i=1; i<lst.size(); ++i) {
	    sb.append(conn + lst.get(i));
	}
	return sb.toString();
    }

    private String joinHPO(ArrayList<Integer> lst) {
	if (lst==null || lst.size()==0)
	    return "No constraint";
	else if (lst.size()==1) {
	    Integer id = lst.get(0);
	    String name = DiseaseCategory.hpo.getTermName(id);
	    return String.format("HP:%07d: %s",id,name); 
	} else {
	    StringBuilder sb = new StringBuilder();
	    boolean notfirst=false;
	    for (Integer id : lst) {
		String name = DiseaseCategory.hpo.getTermName(id);
		String s =String.format("HP:%07d: %s",id,name); 
		if (notfirst) sb.append("; ");
		sb.append(s);
		notfirst=true;
	    }
	    return sb.toString();
	}
    }



    public void printDefinition(Writer out) throws IOException {
	int n = this.diseasegenes.size();
	out.write("Disease Gene: ");
	switch(n) {
	case 0: out.write("no constraint.\n"); break;
	case 1: out.write(this.diseasegenes.get(0) + ".\n"); break;
	default: out.write(join(";",this.diseasegenes) + "\n");break;
	}
	out.write("NOT features: " + joinHPO(this.notFeaturelist) + "\n");
	out.write("Required features: " + joinHPO(this.featurelist)+ "\n");
	out.write("Optional features(>=1 must be present):" + joinHPO(this.optionalFeaturelist)+"\n");
	if (this.featureNlist!=null) {
	    int len = this.featureNlist.size();
	    for (int i=0;i<len;++i) {
		Integer id = this.featureNlist.get(i);
		n = this.N.get(i);
		String name = DiseaseCategory.hpo.getTermName(id);
		String s =String.format("HP:%07d: %s",id,name); 
		out.write(String.format("Require at least %d descendents of %s\n",n,s));
	    } 
	}
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
	printDefinition(out);
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



    public String getName() { return this.categoryname; }


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



    /**
     * @param disease A disease to be tested
     * @param feat An HPO feature (Integer coding of its id, e.g., HP:0001234 would be 1234)
     * @param N minimum number of annotations the disease must have to feat or descendents thereof.
     * @return true if the disease has at least N annotations to HPO term feat or any of its ancestors, otherwise false.
     */
    private boolean hasAtLeastNDescendentFeatures(DiseaseAnnotation disease, Integer feat, Integer N) {
	//String name = DiseaseCategory.hpo.getTermName(feat);
	//String s =String.format("HP:%07d: %s",feat,name); 
	//System.out.println("Looking for at least " + n + " annotations for " + s + " in " + disease.getDiseaseName());
	ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	int n_found=0;
	for (Integer pos:positiveannotations) {
	    try{
		if (DiseaseCategory.hpo.isAncestorOf(feat,pos)){
		    n_found++;
		} 
	    } catch (IllegalArgumentException e) {
		log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		continue;
	    }
	}
	if (n_found < N)
	    return false;
	else
	    return true;
    }


    /**
     * This function is used to test whether a disease has a feature with neonatal onset.
     * Note that we include "Congenital onset"
     * @param disease The disease to be tested
     * @param feature an HPO term (represented as Integer) that should be present with neonatal onset
     */
    private boolean hasNeonatalFeature(DiseaseAnnotation disease, Integer feature) {

	ArrayList<Integer> neonatalAnnotations = disease.getNeonatalAnnotations();
	ArrayList<Integer> congenitalAnnotations = disease.getCongenitalAnnotations();
	if (neonatalAnnotations==null)
	    neonatalAnnotations=congenitalAnnotations;
	else
	    neonatalAnnotations.addAll(congenitalAnnotations);
	for (Integer neo : neonatalAnnotations) {
	    try{
		if (DiseaseCategory.hpo.isAncestorOf(feature,neo)){
		    return true;
		} 
	    } catch (IllegalArgumentException e) {
		log.error(String.format("Could not find HP:%07d for disease %s",neo,disease.getDiseaseName()));
		continue;
	    }
	}
	return false; /** did not find a neonatal annotation. */
    }



    /**
     * Some of the categories are defined based on disease genes. In this case, a disease must be
     * associated with at least one of the disease genes to be considered a member of the
     * category (some other conditions may apply).
     * @param disease Disease to be tested for having at least one of the disease genes for this category
     */
    private boolean hasDiseaseGene(DiseaseAnnotation disease) {
	for (String symbol:this.diseasegenes) {
	    if (disease.hasDiseaseGene(symbol)) {
		return true;
	    }
	}
	/* If we get here, no match was found */
	return false;
    }


    /**
     * This function checks if a disease has an annotation for an HPO 
     * term that is used as a NOT annotation for the category. That is, if
     * category XYZ has NOT sneezing, and disease abc is annotated to 
     * sneezing, this function will return true. In that case, the
     * disease should be rejected.
     * @param disease The disease to be evaluated for category membership
     * @param not An HPO term that Category members CANNOT have
     */
    public boolean hasNOTannotation(DiseaseAnnotation disease, Integer not) {
	ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	for (Integer pos:positiveannotations) {
	    try{
		if (DiseaseCategory.hpo.isAncestorOf(not,pos)){
		    notMemberCount++;
		    return true;
		}
	    } catch (IllegalArgumentException e) {
		log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		continue;
	    }
	}
	/* If we get here, there was no contradiction. */
	return false;
    }



    public boolean evaluateCandidateDisease(DiseaseAnnotation disease) {
	boolean verbose=false;
	//if (disease.MIMid().equals(200600))
	//  verbose=true;

	if (this.diseasegenes != null && this.diseasegenes.size()>0) {
	    boolean OK = hasDiseaseGene(disease);
	    if (!OK) {
		notMemberCount++; 
		return false;
	    }
	}
	/* If we get here, then either the Category is not defined by means of
	 * a disease gene (then diseasegenes.size()==0), or the disease in question
	 * is associated with one of the disease genes. */

	// If a disease has a NOT feature, then it cannot belong to this category
	for (Integer not: notFeaturelist) {
	    boolean reject = hasNOTannotation(disease, not);
	    if (reject) {
		notMemberCount++; 
		return false;
	    }
	 }
	// Check for neonatalfeatures
	if (this.neonatalFeature!=null) {
	    boolean OK=hasNeonatalFeature(disease,this.neonatalFeature);
	    if (! OK ) 
		return false;
	}


	// Now check for constraints that a disease have at least N features descending from some term.

	if (featureNlist!=null && featureNlist.size()>0) {
	    int i;
	    int len = featureNlist.size();
	    for (i=0;i<len;i++) {
		Integer feat = featureNlist.get(i);
		Integer n = N.get(i);
		boolean OK =  hasAtLeastNDescendentFeatures(disease,feat,n);
		if (! OK) return false; /* the disease did not have enough annotations that match feat */
	    }
	}
	/* if we get here, then the disease has satisfied and featureN constraints there were. */
	if (this.featurelist==null || this.featurelist.size()==0) {
	    /* If we get here, then the DiseaseCategory definition does
	       not have any HPO terms that a candidate disease has to have, 
	       and the other conditions about disease genes and NOT terms
	       were ok */
	    return true;
	}

	int n_found=0;
	for (Integer yes: this.featurelist) {
	    //if (verbose) {
	    //	System.out.println(String.format("-required- HP:%07d - %s",yes,DiseaseCategory.hpo.getTermName(yes)));
	    // }
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	    for (Integer pos:positiveannotations) {
		//	if (verbose) {
		//  System.out.println(String.format("-annot-HP:%07d - %s",pos,DiseaseCategory.hpo.getTermName(pos)));
		//}
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(yes,pos)){
			 n_found++;
			 if (verbose)
			     System.out.println("FOUND:" + pos);
			break;
		     } 
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	}
	if (n_found<this.featurelist.size())
	    return false;
	if (this.optionalFeaturelist==null || this.optionalFeaturelist.size()==0)
	    return true;
	// If we get here, then just one of the optional features must be present
	for (Integer optf : this.optionalFeaturelist) {
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	    for (Integer pos:positiveannotations) {
		 try{
		     if (DiseaseCategory.hpo.isAncestorOf(optf,pos)){
			 return true;
		     } 
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	}
	return false; // we did not find an optional annotation.

    }
    


}
