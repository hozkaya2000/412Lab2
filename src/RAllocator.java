import java.util.*;

/**
 * Sets the physical register
 */
public class RAllocator {


    //                                      0        1        2       3      4      5        6
    String[] opCodeStrings = new String[]{"load", "loadI", "store", "add", "sub", "mult", "lshift",
            "rshift", "output", "nop", ",", "=>", "NOT IN LEXEME"};
    //         7         8        9    10    11        12

    Integer[] VRToPR;
    Integer[] PRToVR;
    Stack<Integer> PRStack;
    Integer[] VRToSpillLoc;
    Integer[] PRNU;

    /**
     * The next spill location
     */
    Integer nextSpillLoc;

    LinkedList<Integer[]> iRep;

    IntRepList iRepBetter;

    Stack<Integer> prsToFree;


    int numPhysRegs;
    /**
     * Creates the allocator class with the given iRep
     * @param iRep the intermediate representation
     * @param numPhysRegs the number of physical registers available
     * @param maxVRNum the maximum virtual register number that occurs
     */
    public RAllocator (LinkedList<Integer[]> iRep, int numPhysRegs, int maxVRNum) {
        this.iRep = iRep;
        this.iRepBetter = new IntRepList();
        this.iRepBetter.transferLinkedList(iRep);
        this.numPhysRegs = numPhysRegs;
        VRToPR = getNegArray(maxVRNum);
        PRToVR = getNegArray(numPhysRegs); // note that the last register is reserved for spilling
        VRToSpillLoc = getNegArray(maxVRNum);
        PRNU = getNegArray(numPhysRegs);
        nextSpillLoc = 32764; // a number to keep track of the memory location to which to spill
        this.PRStack = new Stack<>(); //
        this.fillPRStack();
        prsToFree = new Stack<>();
    }

    public void Allocate() {

        /*
         * Opcode Array representation:
         *
         * OPCODE       Argument 1          Argument 2          Argument 3
         *             SR VR PR NU         SR VR PR NU         SR VR PR NU
         *   0         1  2  3  4          5  6  7  8          9  10 11 12
         *
         */

        /*                                      0        1        2       3      4      5        6
        String[] opCodeStrings = new String[]{"load", "loadI", "store", "add", "sub", "mult", "lshift",
                "rshift", "output", "nop", ",", "=>", "NOT IN LEXEME"};
                   7         8        9    10    11        12
         */


        IntRepList.OpNode current = this.iRepBetter.head;
        while (null != current) {

            Integer[] thisOP = current.opArray;

            // for each operand USED in nextOP
            int lastOPUseInd = thisOP[0] == 2 ? 10 : 6; // only the store has uses past the argument 2 indexes in the IR
            for (int opBaseInd = 1; opBaseInd < lastOPUseInd; opBaseInd += 4) { // for each operand USED in nextOP
                if (thisOP[opBaseInd] != null && thisOP[0] != 1 && thisOP[0] < 8) { // only use registers

                    int VRInd = opBaseInd + 1;
                    int PRInd = opBaseInd + 2;
                    int NUInd = opBaseInd + 3;

                    int PRNum;
                    int VRNum = thisOP[VRInd];
                    if (VRToSpillLoc[VRNum] != -1) //checks if spilled
                        PRNum = Restore(VRNum, current); // restores it
                    else if (VRToPR[VRNum] != -1){ // if the vr already has a pr assigned
                        PRNum = VRToPR[VRNum];
                    }
                    else {
                        PRNum = this.getPR(current);
                        VRToPR[VRNum] = PRNum;
                        PRToVR[PRNum] = VRNum;
                    }
                    thisOP[PRInd] = PRNum;
                    PRNU[PRNum] = thisOP[NUInd];
                    if (null == thisOP[NUInd] || thisOP[NUInd] <= 0) {
                        this.prsToFree.push(VRNum);
                    }

                }
            }

            this.FreePRs();

            // for every operand DEFINED in nextOP
            // only do this if the OP is not output, nop, or store
            if (thisOP[0] != 2 && thisOP[0] < 8) {
                int opBaseInd = 9; // operand base index for any definition is 9
                int VRInd = opBaseInd + 1;
                int PRInd = opBaseInd + 2;
                int NUInd = opBaseInd + 3;
                thisOP[PRInd] = this.getPR(current);
                PRToVR[thisOP[PRInd]] = thisOP[VRInd];
                VRToPR[thisOP[VRInd]] = thisOP[PRInd];
                PRNU[thisOP[PRInd]] = thisOP[NUInd];
                if (thisOP[NUInd] == null || thisOP[NUInd] <= 0) {
                    this.prsToFree.push(thisOP[VRInd]);
                    this.FreePRs();
                }
            }
            this.ShowRep(thisOP);
            current = current.next;
        }

    }

    /**
     * Frees the stack of vrs bound to prs and vice versa accumulated at
     * uses processing
     */
    private void FreePRs() {
        if (prsToFree.empty())
            return;

        int VRNum = prsToFree.pop();
        PRToVR[VRToPR[VRNum]] = -1;
        PRStack.push(VRToPR[VRNum]);
        VRToPR[VRNum] = -1;
    }



    /**
     * Inserts the given node before the given current node
     * @param node the node to insert
     * @param current the node we're trying to insert behind
     */
    private void InsertAt(IntRepList.OpNode node, IntRepList.OpNode current) {
        current.prev.next = node;
        node.prev = current.prev;
        node.next = current;
    }

    /**
     * Fills the pr stack with available PRs
     */
    private void fillPRStack() {
        for (int i = 0; i < PRToVR.length - 1; i++) {
            PRStack.push(i);
        }
    }

    /**
     * Gets the next available pr
     * @return the pr value
     */
    private int getPR(IntRepList.OpNode current) {
        if (PRStack.empty())
            return this.Spill(current);
        return PRStack.pop();
    }

    private int Spill(IntRepList.OpNode current) {

        Integer[] loadIOPArray = new Integer[13];
        loadIOPArray[0] = 1; // 'loadI' opcode
        loadIOPArray[1] = nextSpillLoc += 4; // set the memory spill location
        loadIOPArray[11] = this.numPhysRegs - 1; // the reserved PR for spill purposes
        IntRepList.OpNode loadINode = new IntRepList.OpNode(loadIOPArray);
        InsertAt(loadINode, current);

        int PR = getPRFurthestUse();
        int VR = PRToVR[PR];

        Integer[] storeOPArray = new Integer[13];
        storeOPArray[0] = 2; // 'store' opcode
        storeOPArray[3] = PR; // set the memory spill location
        storeOPArray[11] = this.numPhysRegs - 1; // the reserved PR for spill purposes
        IntRepList.OpNode storeNode = new IntRepList.OpNode(storeOPArray);
        InsertAt(storeNode, current);

        VRToSpillLoc[VR] = this.nextSpillLoc += 4;
        VRToPR[VR] = -1;
        PRToVR[PR] = -1;
        PRNU[PR] = -1;
        return PR;
    }

    /**
     * Spills the next operation into memory because there aren't enough physical registers
     * @param vrNum the virtual register number to restore
     * @param current the current node that the iteration is on
     * @return the pr that was assigned during the restore
     */
    private int Restore(int vrNum, IntRepList.OpNode current) {
        /*
         * Opcode Array representation:
         *
         * OPCODE       Argument 1          Argument 2          Argument 3
         *             SR VR PR NU         SR VR PR NU         SR VR PR NU
         *   0         1  2  3  4          5  6  7  8          9  10 11 12
         *
         */

        int pr = getPR(current);

        Integer[] loadIOPArray = new Integer[13];
        loadIOPArray[0] = 1; // 'loadI' opcode
        loadIOPArray[1] = VRToSpillLoc[vrNum]; // set the memory spill location
        loadIOPArray[11] = this.numPhysRegs - 1; // the reserved PR for spill purposes
        IntRepList.OpNode loadINode = new IntRepList.OpNode(loadIOPArray);

        this.InsertAt(loadINode, current);

        Integer[] loadOPArray = new Integer[13];
        loadOPArray[0] = 0; // 'load' opcode
        loadOPArray[3] = this.numPhysRegs - 1; // reserved pr
        loadOPArray[11] = pr;
        IntRepList.OpNode loadNode = new IntRepList.OpNode(loadOPArray);

        this.InsertAt(loadNode, current);

        VRToPR[vrNum] = pr;
        PRToVR[pr] = vrNum;
        VRToSpillLoc[vrNum] = -1; // has no spill location anymore

        return pr;
    }
    /**
     * Gets the PR with the furthest next use
     * @return the pr number
     */
    private int getPRFurthestUse() {
        int max = -1;
        int maxPR = -1;
        for (int i = 0; i < numPhysRegs - 1; i++) {
            if (PRNU[i] > max) {
                max = PRNU[i];
                maxPR = i;
            }
        }
        return maxPR; // no free physical register
    }

    /**
     * Gets an integer array of -1s with the given size
     * @param size the size of the array
     */
    private Integer[] getNegArray(int size) {
        Integer[] toReturn = new Integer[size];
        for (int i = 0; i < size; i++) {
            toReturn[i] = -1;
        }
        return toReturn;
    }

    public void PrintRenamedBlock() {
        IntRepList.OpNode current = this.iRepBetter.head;
        while (null != current) {
            this.PrintRenamedOperation(current.opArray);
            current = current.next;
        }
    }

    /**
     * Prints the operation after it has been renamed
     * @param op the operation array
     */
    private void PrintRenamedOperation(Integer[] op) {
        if (op[0] >= 3 && op[0] <= 7) // arithop
            System.out.println("" + opCodeStrings[op[0]] + " r" + op[3] + "," + " r" + op[6] + " =>" + " r" + op[11]);
        else if (op[0] == 2 || op[0] == 0) { // load or store
            System.out.println(opCodeStrings[op[0]] + " r" + op[3] + " =>" + " r" + op[11]);
        } else if (op[0] == 1) { // loadI -- prints a constant
            System.out.println(opCodeStrings[op[0]] + " " + op[1] + " =>" + " r" + op[11]);
        } else if (op[0] == 8) { // output
            System.out.println(opCodeStrings[op[0]] + " " + op[1]);
        } else if (op[0] == 9) { // nop
            System.out.println(opCodeStrings[op[0]]);
        }
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
        IntRepList.OpNode current = this.iRepBetter.head;
        while (null != current) {
            this.ShowRep(current.opArray);
            current = current.next;
        }
    }

}
