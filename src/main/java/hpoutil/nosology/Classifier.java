package hpoutil.nosology;


import hpoutil.ontology.*;
import hpoutil.omim.DiseaseAnnotation;
import java.io.*;

public interface Classifier {

    public boolean satisfiesDefinition(DiseaseAnnotation disease);

    public void addGermlineMutation(String g);

    public void addNotFeature(Integer notf);

    public void addFeature(Integer f);

    public void addNfeature(Integer hpo,Integer n);


    public void setNeonatalFeature(Integer hpo);

    public void addOptionalFeature(Integer hpo);

    public  void printDefinition(Writer out) throws IOException;


}
