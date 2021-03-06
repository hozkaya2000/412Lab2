import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class renames a given intermediate representation
 */
public class IRRenamer {

    /**
     * The intermediate representation to rename
     */
    private LinkedList<Integer[]> iRep;

    /**
     * The maximum register number int he IRep
     */
    private int maxRegNumber;

    /**
     * The maximum virtual register number
     */
    private int maxVRegNum;

    //                                      0        1        2       3      4      5        6
    String[] opCodeStrings = new String[]{"load", "loadI", "store", "add", "sub", "mult", "lshift",
            "rshift", "output", "nop", ",", "=>", "NOT IN LEXEME"};
    //         7         8        9    10    11        12

    /**
     * Creates the IRRenamer
     * @param iRep the given intermediate representation
     */
    public IRRenamer(LinkedList<Integer[]> iRep, int maxRegNumber) {
        this.iRep = iRep;
        this.maxRegNumber = maxRegNumber;
    }

    /**
     * The renaming algorithm. Adds vr slots to the operation arrays as well as Next Use info
     */
    public void Rename() {

        int VRName = 0;

        Integer[] SRToVR = new Integer[this.maxRegNumber + 1];
        Integer[] LU = new Integer[this.maxRegNumber + 1];
        for (int i = 0; i < this.maxRegNumber + 1; i ++) {
            SRToVR[i] = -1;
            LU[i] = Integer.MAX_VALUE;
        }

        Iterator<Integer[]> IRReverseIterator = this.iRep.descendingIterator();
        int index = this.iRep.size();
        while (IRReverseIterator.hasNext()) {
            Integer[] nextOP = IRReverseIterator.next();


            /*
             * Opcode Array representation:
             *
             * OPCODE       Argument 1          Argument 2          Argument 3
             *             SR VR PR NU         SR VR PR NU         SR VR PR NU
             *   0         1  2  3  4          5  6  7  8          9  10 11 12
             *
             */

            // for every operand DEFINED in nextOP
            // only do this if the OP is not output, nop, or store
            if (nextOP[0] != 2 && nextOP[0] < 8) {
                int oBaseInd = 9; // operand base index for any definition is 9
                int SR = nextOP[oBaseInd]; // SR num for readability
                if (SRToVR[SR] == -1) { // the definition could only be nextOP[9]
                    SRToVR[SR] = VRName ++;
                }
                nextOP[oBaseInd + 1] = SRToVR[SR]; // set Virtual register in representation
                nextOP[oBaseInd + 3] = LU[SR]; // set Next use in representation to Last use
                SRToVR[SR] = -1; // set to invalid
                LU[SR] = Integer.MAX_VALUE;
            }

            int lastOPUseInd = nextOP[0] == 2 ? 10 : 6; // if it is a store, the last use register is
            for (int opBaseInd = 1; opBaseInd < lastOPUseInd; opBaseInd += 4) { // for each operand USED in nextOP
                if (nextOP[opBaseInd] != null && nextOP[0] != 1 && nextOP[0] < 8) { // only use registers

                    int opSR = nextOP[opBaseInd]; // the source register of the current operand

                    if (SRToVR[opSR] == -1) {
                        SRToVR[opSR] = VRName ++;
                    }
                    nextOP[opBaseInd + 1] = SRToVR[opSR]; // set the virtual register of the operand
                    nextOP[opBaseInd + 3] = LU[opSR]; // set the next use of the register in the operand
                    LU[opSR] = index; // set the last use of this source register to the index of the op
                }
            }
            // for every operand used in nextOP


            index --;
        }
        maxVRegNum = VRName;
    }


    /**
     * Prints out the representation of an op line
     * @param op the operation array
     */
    private void ShowRep(Integer[] op) {
        System.out.print(" " + this.opCodeStrings[op[0]]);
        for (int i = 1; i < 13; i++) {
            if ((i - 1) % 4 == 0)
                System.out.print(" | ");
            if (op[i] != null)
                System.out.print(" " + op[i]);
            else
                System.out.print(" - ");
        }
        System.out.println();
    }

    /**
     * Shows the entire representation of
     */
    public void ShowAllRep() {
        for (Integer[] operation: this.iRep) {
            this.ShowRep(operation);
        }
    }

    /**
     * This prints the renamed ILOC block to stdout
     */
    public void PrintRenamedBlock() {
        for (Integer[] operation: this.iRep) {
            this.PrintRenamedOperation(operation);
        }
    }

    /**
     * Prints the operation after it has been renamed
     * @param op the operation array
     */
    private void PrintRenamedOperation(Integer[] op) {
        if (op[0] >= 3 && op[0] <= 7) // arithop
            System.out.println("" + opCodeStrings[op[0]] + " r" + op[2] + "," + " r" + op[6] + " =>" + " r" + op[10]);
        else if (op[0] == 2 || op[0] == 0) { // load or store
            System.out.println(opCodeStrings[op[0]] + " r" + op[2] + " =>" + " r" + op[10]);
        } else if (op[0] == 1) { // loadI -- prints a constant
            System.out.println(opCodeStrings[op[0]] + " " + op[1] + " =>" + " r" + op[10]);
        } else if (op[0] == 8) { // output
            System.out.println(opCodeStrings[op[0]] + " " + op[1]);
        } else if (op[0] == 9) { // nop
            System.out.println(opCodeStrings[op[0]]);
        }
    }

    public int getMaxVRegNum() {
        return this.maxVRegNum;
    }

}
