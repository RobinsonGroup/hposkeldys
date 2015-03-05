package hpoutil.nosology;



import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;


public class CategoryParser {

    private final static String nosopath="src/main/resources/nosology";

    private ArrayList<DiseaseCategory> categorylist=null;

   
    public ArrayList<DiseaseCategory> getDiseaseCategoryList() { return this.categorylist; }

    public CategoryParser() {
	ArrayList<String> paths=getOrderedFilePaths();

	this.categorylist = new ArrayList<DiseaseCategory> ();
	for (String p : paths) {   
	    parseCategory(p);
	}
    }



    private ArrayList<String> getOrderedFilePaths() {
	String dir="src/main/resources/nosology";
	String f[] = {
	    "FGFR3.nos", /* #1 */
	    "Type2Collagen.nos", /* #2 */
	    "Type11Collagen_Group.nos", /* #3 */
	    "SulfationDisorders_Group.nos", /* #4 */
	    "Perlecan_Group.nos", /* #5 */
	    "Aggrecan_Group.nos", /* #6 */
	    "Filamin_Group.nos", /* #7 */
	    "TRPV4_Group.nos",  /* #8 */
	    "ShortRibsDysplasia_Group.nos", /* #9 */
	    "MEDandPseudoachondroplasia_Group.nos", /* #10 */
	    // Starting here we need to change the order to make things more specific! */
	    "SpondylometaphysealDysplasiaGroup.nos", /* #12 */
	    "SevereSpondylodysplasticGroup.nos", /* 14 */
	    "MetaphysealDysplasia_Group.nos", /* #11 */
	    "SpondyloEpiMetaphysealDysplasiaGroup.nos", /* 13 */
	    "AcromesomelicDysplasiaGroup.nos", /* #16 */
	    "AcromelicDysplasiaGroup.nos", /* #15 */
	    "BentBonesGroup.nos", /* #18 */
	    "SlenderBonesGroup.nos", /* #19 */
	    "DysplasiasWithMultipleJointDislocationsGroup.nos", /* #20 */
	    "ChondrodysplasiaPunctataGroup.nos", /* #21 */
	    "IncreasedBoneDensityWithoutModificationOfBoneShapeGroup.nos", /* #22 */
	    "IncreasedBoneDensityWithMetaphysealDiaphysealInvolvementGroup.nos", /* #23 */
	    "OsteogenesisImperfectaGroup.nos", /* #24 */

	    "DysostosisMultiplexGroup.nos", /* 27 */
	    "CraniosynostosisGroup.nos",
	    "OsteolysisGroup.nos",
	    "PolydactylySyndactylyTriphalangismGroup.nos", /* # 39 */

	};
	ArrayList<String> ret = new ArrayList<String>();
	for (String fname:f) {
	    String path = String.format("%s/%s",dir,fname);
	    ret.add(path);
	}
	return ret;
    }




    /*
      name:FGFR3Group
      gold:187600=Thanatophoric dysplasia type 1 (TD1)
      gold:187601=Thanatophoric dysplasia type 2 (TD2),Severe achondroplasia with developmental delay and acanthosis nigricans (SADDAN)
      gold:100800=Achondroplasia
      gold:146000=Hypochondroplasia
      gold: 610474=Camptodactyly, tall stature, and hearing loss syndrome (CATSHL)
      //[187600, 187601, 100800, 146000, 610474]
      hasGermlineMutationIn:FGFR3
      notHasFeature:HP_0001363
      // NOT HPO_craniosynostosis (HP:0001363)
      // NOT Triphalangeal thumb (HP:0001199)
      //!#hasFeature HP_0001363
      //!#hasFeature HP_0001199
      */

    private void parseCategory(String filepath) {

	HashMap<Integer,String> goldstandard=new  HashMap<Integer,String>();
	ArrayList<String> diseasegenes=new ArrayList<String>();
	ArrayList<Integer> featurelist=new ArrayList<Integer>();
	ArrayList<Integer> notFeaturelist=new ArrayList<Integer>();
	ArrayList<Integer> optionallist=new ArrayList<Integer>();
	ArrayList<Integer> featureNlist=new ArrayList<Integer>();
	ArrayList<Integer> N=new ArrayList<Integer>();
	String name=null;
	Integer number=null;
	try {
	    BufferedReader br = new BufferedReader(new FileReader(filepath));
	    String line=null;
	    while ((line = br.readLine())!=null) {
		if (line.startsWith("//"))
		    continue;
		else if (line.isEmpty())
		    continue;
		else if (line.startsWith("name:"))
		    name=line.substring(5).trim();
		else if (line.startsWith("number:"))
		    number=Integer.parseInt(line.substring(7).trim());
		else if (line.startsWith("gold:")) {
		    String gold = line.substring(5).trim();
		    String a[] = gold.split("=");
		    Integer mim = Integer.parseInt(a[0].trim());
		    goldstandard.put(mim,a[1].trim());
		} else if (line.startsWith("hasGermlineMutationIn:")) {
		    int i = line.indexOf(":");
		    String sym = line.substring(i+1).trim();
		    diseasegenes.add(sym);
		} else if (line.startsWith("notHasFeature:")) {
		    String hp=line.substring(14).trim();
		    Integer hpo=getHPcode(hp);
		    notFeaturelist.add(hpo);
		} else if (line.startsWith("hasFeature:")) {
		    String hp=line.substring(11).trim();
		    Integer hpo=getHPcode(hp);
		    featurelist.add(hpo);
		} else if (line.startsWith("hasFeatureN(")) {
		    line=line.substring(12).trim();
		    int x = line.indexOf(")");
		    Integer n = Integer.parseInt(line.substring(0,x));
		    line=line.substring(x+2); /* skip the '):' */
		    Integer hpo=getHPcode(line);
		    featureNlist.add(hpo);
		    N.add(n);
		} else if (line.startsWith("hasOptionalFeature:")) {
		    String hp = line.substring(19).trim();
		    Integer hpo=getHPcode(hp);
		    optionallist.add(hpo);
		}
	    }
	    br.close();
	} catch(IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	DiseaseCategory dc = new DiseaseCategory(name,diseasegenes,featurelist,notFeaturelist);
	dc.addGoldStandard(goldstandard);
	if (optionallist.size()>0)
	    dc.setOptionalList(optionallist);
	if (featureNlist.size()>0)
	    dc.setFeatureN(featureNlist,N);
	if (number != null)
	    dc.setNumber(number);
	this.categorylist.add(dc);
	    
    }


	private Integer getHPcode(String h) {
	    if (h.startsWith("HP_") || h.startsWith("HP:"))
		h = h.substring(3).trim();
	    if (h.length()!=7)
		throw new IllegalArgumentException("Bad length for HP term in CategoryParser:" + h);
	    return Integer.parseInt(h);
	}



    private ArrayList<String> getFilePaths() {
	ArrayList<String> paths = new ArrayList<String>();
	File folder = new File(nosopath);
	if (folder.exists()) {
	    File[] files = folder.listFiles();
	    for (File file : files) {
		if (file.isFile()) {
		    if (file.getName().endsWith("nos")) {
			paths.add(file.getAbsolutePath());
		    }
		} 
	    }
	    //log.info("Compiled a total of " + paths.size()+ " annotation files.");
	} else {
	    System.out.println("Could not initialize annotation folder: " + nosopath);
	    System.out.println("Please check path (e.g., in makefile variable NOSOLOGY) and try again");
	    System.exit(1);
	}
	return paths;


    }



}
