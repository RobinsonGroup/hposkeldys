package hpoutil.io;



import java.io.*;

public class HPOParser {


    public HPOParser(String path) {
	
	parseFile(path);

    }



    private void parseFile(String path) {
	try {
	    FileInputStream fstream = new FileInputStream(path);
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    String line;
	    while ((line = br.readLine()) != null)   {
		System.out.println (line);
	    }
	    in.close();
	} catch (IOException e) {
	    e.printStackTrace();

	}



    }



}