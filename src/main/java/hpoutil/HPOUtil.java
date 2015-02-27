package hpoutil;


/** Command line parser from apache */
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;



import hpoutil.io.*;


public class HPOUtil {

    private String pathToHpoOBOFile=null;

    public static void main(String args[]) {
	HPOUtil hpoutil = new HPOUtil(args);
	System.out.println("hello");
    }




    public HPOUtil(String args[]) {
	parseCommandLineArguments(args);
	parseHPOFile();
    }





    private void parseHPOFile() {
	HPOParser parser = new HPOParser(pathToHpoOBOFile); 

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