import java.util.LinkedList;

public class ILOCAllocMain {

    /**
     * The main method of the program
     * @param args the arguments in the command line
     */
    public static void main(String[] args) {




        String filePath;
        ILOCParser parser;
        IRRenamer renamer;
        RAllocator allocator;
        int filePathInd = 1;
        if (inArgs("-h", args)) {
            showCommandLineInfo();
            if (args.length > 1) {
                System.err.println("Please only use one command argument at a time");
            }
        }
        else if(inArgs("-x", args)){
            filePath = args[1];
            parser = new ILOCParser(filePath, false, false);
            renamer = new IRRenamer(parser.ParseGetIRep(), parser.getMaxSRNum());
            renamer.Rename();
            System.out.println(renamer.getMaxVRegNum());
            renamer.PrintRenamedBlock();
        }
        else {
            filePath = args[1];
            parser = new ILOCParser(filePath, false, false);
            LinkedList<Integer[]> iRep = parser.ParseGetIRep(); // parse and get the intermediate representation
            renamer = new IRRenamer(iRep, parser.getMaxSRNum());
            renamer.Rename(); // this will add VRs to iRep
            //renamer.ShowAllRep();
            //renamer.PrintRenamedBlock();
            allocator = new RAllocator(iRep, Integer.parseInt(args[0]), renamer.getMaxVRegNum());
            allocator.Allocate();
            //allocator.ShowAllRep();
            allocator.PrintRenamedBlock();
        }

    }

    /**
     * @param string the string to search for
     * @param args the String array to search for the string in
     * @return whether string is in args
     */
    private static boolean inArgs(String string, String[] args) {
        for (String arg: args) {
            if (arg.equals(string))
                return true;
        }
        return false;
    }

    /**
     * Shows the comman dl.,ine info
     */
    private static void showCommandLineInfo() {
        System.out.print(" How to use the ILOC Parser:\n " +
                "======================================================================\n" +
                "412alloc -h\n" +
                "-----------\n" +
                "When a -h flag is detected, 412alloc will produce a list of valid command\n" +
                "line arguments as well as their description. 412alloc is not required to\n" +
                "process command line arguments that appear after the -h flag\n" +
                "======================================================================\n" +
                "412alloc -x <file name>\n" +
                "-----------\n" +
                "When a -x flag is detected, 412alloc reads the file specified by <file name>" +
                "and renames the registers in the input block "
        );
    }



    /**
     * For the simplest form of testing that I tried before testing thoroughly.
     */
    private static void selfTest() {
        ILOCParser ip = new ILOCParser("/storage-home/h/ho8/comp412/lab1/412Lab1/src/ILOCTest.txt", false, true);


        ip.Parse();
    }
}
