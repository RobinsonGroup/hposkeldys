package hpoutil.nosology;




import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.HashMap;
import java.io.Writer;
import java.io.IOException;

import hpoutil.ontology.*;
import hpoutil.omim.DiseaseAnnotation;

/**
 * This class represents a definition of a disease category.
 * A disease in OMIM needs to satisfy at least one of the definitions for the category to be included.
 * @author Peter Robinson
 * @version 0.02 (18 April 2015)
 */
public class Definition implements Classifier{

    private static Logger log = Logger.getLogger(Definition.class.getName());


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

     /** reference to the HPO object */
    private static HPO hpo;
   
  

    public Definition() {
	this.diseasegenes=new ArrayList<String> ();
	this.notFeaturelist= new ArrayList<Integer>();
	this.featurelist= new ArrayList<Integer>();
	this.featureNlist=new ArrayList<Integer>();
	this.optionalFeaturelist=new ArrayList<Integer>();
	this.N=new ArrayList<Integer>();
    }

    
    static public void setHPO(HPO hpo) {
	Definition.hpo=hpo;
    }

    @Override public void addGermlineMutation(String sym){
	this.diseasegenes.add(sym);
    }

    @Override public void addNotFeature(Integer notf){
	this.notFeaturelist.add(notf);
    }

    @Override public void addFeature(Integer f) {
	this.featurelist.add(f);

    }

 
    @Override public void addNfeature(Integer hpo,Integer n) {
	  featureNlist.add(hpo);
	  N.add(n);
    }

    @Override  public void setNeonatalFeature(Integer hpo){
	this.neonatalFeature=hpo;
    }


    @Override  public void addOptionalFeature(Integer hpo){
	this.optionalFeaturelist.add(hpo);
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



    /** Returns true if this Definition does not involve disease genes (trivial).
     * If the definition does have disease genes (i.e., {@link #diseasegenes} has entries),
     * then it returns true if the disease has at least one of them, otherwise false.
     */
    private boolean satisfiesDiseaseGeneDefinition(DiseaseAnnotation disease) {
	if (this.diseasegenes != null && this.diseasegenes.size()>0) {
	    return hasDiseaseGene(disease);
	} else { /* trivial, no diseases genes in the definition */
	    return true;
	}
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
    private boolean hasNOTannotation(DiseaseAnnotation disease, Integer not) {
	ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	for (Integer pos:positiveannotations) {
	    try{
		if (Definition.hpo.isAncestorOf(not,pos)){
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





    /**
     * If the definition has a non-null list of NOT features, than
     * any disease that has any of the features is ruled out.
     * (i.e., If a disease has a NOT feature, then it cannot belong to this category).
     */
    private boolean satisfiesNotFeatureDefinition(DiseaseAnnotation disease) {
	for (Integer not: notFeaturelist) {
	    if ( hasNOTannotation(disease, not) )
		return false;
	}
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
		if (Definition.hpo.isAncestorOf(feature,neo)){
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
     * Check if the definition has an HPO feature that is required to
     * be present with neonatal onset.
     */
    private boolean satisfiesNeonatalDefinition(DiseaseAnnotation disease) {
	// Check for neonatalfeatures
	if (this.neonatalFeature!=null) {
	    return hasNeonatalFeature(disease,this.neonatalFeature);
	} else { // trivial, the definition has no neonatal feature.
	    return true; // 
	}
    }

    /**
     * Check for constraints that a disease have at least N features descending from some term.
     /* Returns false if the disease did not have enough annotations that match feat 
     */
    private boolean satisfiesHasN_FeaturesDefinition(DiseaseAnnotation disease) { 
	if (featureNlist!=null && featureNlist.size()>0) {
	    int i;
	    int len = featureNlist.size();
	    for (i=0;i<len;i++) {
		Integer feat = featureNlist.get(i);
		Integer n = N.get(i);
		return  hasAtLeastNDescendentFeatures(disease,feat,n);
	    }
	} 
	/* trivial, no requirement to have at least N terms descendent from a definition term */
	return true;
    }


    private boolean hasFeatures(DiseaseAnnotation disease) { 
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
		    if (Definition.hpo.isAncestorOf(yes,pos)){
			n_found++;
		    } 
		 } catch (IllegalArgumentException e) {
		     log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		     continue;
		 }
	     }
	}
	if (n_found<this.featurelist.size())
	    return false;
	else
	    return true;
    }

    private boolean hasOptionalFeatures(DiseaseAnnotation disease) {
	for (Integer optf : this.optionalFeaturelist) {
	    ArrayList<Integer> positiveannotations = disease.getPositiveAnnotations();
	    for (Integer pos:positiveannotations) {
		try{
		    if (Definition.hpo.isAncestorOf(optf,pos)){
			return true;
		    } 
		} catch (IllegalArgumentException e) {
		    log.error(String.format("Could not find HP:%07d for disease %s",pos,disease.getDiseaseName()));
		    continue;
		}
	    }
	}
	return false; /* If we get here, we could not find anything */
    }

    public boolean satisfiesDefinition(DiseaseAnnotation disease) { 
	if (! satisfiesDiseaseGeneDefinition(disease))
	    return false;
	if (! satisfiesNotFeatureDefinition(disease))
	    return false;
	if (! satisfiesNeonatalDefinition(disease))
	    return false;
	if ( ! satisfiesHasN_FeaturesDefinition(disease))
	    return false;
	if (this.featurelist!=null && this.featurelist.size()>0) {
	    if (! hasFeatures(disease) )
		return false;
	}
	if (this.optionalFeaturelist!=null && this.optionalFeaturelist.size()>0) {
	    if ( ! hasOptionalFeatures(disease))
		return false;
	}
	return true; /* If we get here, then all requirements were satisfied. */
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
		if (Definition.hpo.isAncestorOf(feat,pos)){
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
		String name = Definition.hpo.getTermName(id);
		String s =String.format("HP:%07d: %s",id,name); 
		out.write(String.format("Require at least %d descendents of %s\n",n,s));
	    } 
	}
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
	    String name = Definition.hpo.getTermName(id);
	    return String.format("HP:%07d: %s",id,name); 
	} else {
	    StringBuilder sb = new StringBuilder();
	    boolean notfirst=false;
	    for (Integer id : lst) {
		String name = Definition.hpo.getTermName(id);
		String s =String.format("HP:%07d: %s",id,name); 
		if (notfirst) sb.append("; ");
		sb.append(s);
		notfirst=true;
	    }
	    return sb.toString();
	}
    }




}
