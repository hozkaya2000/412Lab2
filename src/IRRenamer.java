import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class renames a given intermediate representation
 */
public class IRRenamer {

    /**
     * The intermediate representation to rename
     */
    LinkedList<Integer[]> iRep;

    /**
     * The maximum register number int he IRep
     */
    int maxRegNumber;


    //                                      0        1        2       3      4      5        6
    String[] opCodeStrings = new String[]{"load", "loadI", "store", "add", "sub", "mult", "lshift",
            "rshift", "output", "nop", ",", "=>", "NOT IN LEXEME"};
    //         7         8        9    10    11        12

    /**
     * Creates the IR Renamer
     * @param iRep the given intermediate representation
     */
    public IRRenamer(LinkedList<Integer[]> iRep, int maxRegNumber) {
        this.iRep = iRep;
        this.maxRegNumber = maxRegNumber;
    }

    /**
     * The renaming algorithm
     */
    public void Rename() {

        int VRName = 0;

        Integer[] SRToVR = new Integer[iRep.size()];
        Integer[] LU = new Integer[iRep.size()];
        for (int i = 0; i < iRep.size(); i ++) {
            SRToVR[i] = -1;
            LU[i] = Integer.MAX_VALUE;
        }

        Iterator<Integer[]> IRReverseIterator = iRep.descendingIterator();

        Integer[] nextOP;
        while(IRReverseIterator.hasNext()) {
            nextOP = IRReverseIterator.next();
            ShowRep(nextOP);
        }


    }


    /**
     * Prints out the representation
     */
    private void ShowRep(Integer[] rep) {
        System.out.println(" " + this.opCodeStrings[rep[0]] + " " + rep[1] + " " + rep[5] + " " + rep[9]);
    }


}
