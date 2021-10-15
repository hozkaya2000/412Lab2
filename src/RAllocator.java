import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
    Integer[] VRToSpillLoc;
    Integer[] PRNU;

    /**
     * The next spill location
     */
    Integer nextSpillLoc;

    LinkedList<Integer[]> iRep;

    IntRepList iRepBetter;


    int numPhysRegs;
    /**
     * Creates the allocator class with the given iRep
     * @param iRep the intermediate representation
     * @param numPhysRegs the number of physical registers available
     * @param minRegsForNoSpill the maximum virtual register number that occurs
     */
    public RAllocator (LinkedList<Integer[]> iRep, int numPhysRegs, int minRegsForNoSpill) {
        this.iRep = iRep;
        this.iRepBetter = new IntRepList();
        this.iRepBetter.transferLinkedList(iRep);
        this.numPhysRegs = numPhysRegs;
        VRToPR = getNegArray(minRegsForNoSpill);
        PRToVR = getNegArray(numPhysRegs);
        VRToSpillLoc = getNegArray(minRegsForNoSpill);
        PRNU = getNegArray(numPhysRegs);
        nextSpillLoc = 32764;
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

                    int vrNum = thisOP[opBaseInd + 1];
                    if (isSpilled(vrNum)) {
                        addRestoreNodes(current, vrNum, opBaseInd);
                        System.out.println("hi");
                    }
                    if (-1 == VRToPR[vrNum] && VRToPR[vrNum] != -1) { // checks if operand has a pr in the VRtoPR map
                        int nextFreePR = getNextFreePR();
                        //TODO: what if the nextFReePR is not -1?
                        if (nextFreePR != -1) {
                            VRToPR[vrNum] = nextFreePR; //
                            PRToVR[nextFreePR] = vrNum; //
                            PRNU[nextFreePR] = thisOP[opBaseInd + 3]; // store the next use in the PRNU map
                            thisOP[opBaseInd + 2] = nextFreePR; // set the PR in the IR to the value found
                        }
                        else { // spill the pr with the furthest use
                            addSpillNodes(current);
                            System.out.println("Hello there");

                        }
                    }
                    else if (VRToPR[vrNum] != -1) {
                        thisOP[opBaseInd + 2] = VRToPR[vrNum]; // set the PR in the IR to the value it should be
                        PRNU[VRToPR[vrNum]] = thisOP[opBaseInd + 3]; // store the next use in the PRNU map
                    }


                }
            }

            // for each operand USED in nextOP
            for (int opBaseInd = 1; opBaseInd < lastOPUseInd; opBaseInd += 4) { // for each operand USED in nextOP
                if (thisOP[opBaseInd] != null && thisOP[0] != 1 && thisOP[0] < 8) { // only use registers

                    int vrNum = thisOP[opBaseInd + 1];
                    int NU = thisOP[opBaseInd + 3];
                    if (NU == Integer.MAX_VALUE && VRToPR[vrNum] != -1) { // if this is the last use of virtual reg,
                        PRNU[VRToPR[vrNum]] = -1;
                        PRToVR[VRToPR[vrNum]] = -1; // free the pr
                        VRToPR[vrNum] = -1; // free the vr
                    }

                }
            }

            // for every operand DEFINED in nextOP
            // only do this if the OP is not output, nop, or store
            if (thisOP[0] != 2 && thisOP[0] < 8) {
                int oBaseInd = 9; // operand base index for any definition is 9
                int vrNum = thisOP[oBaseInd + 1];
                int freePR = getNextFreePR();
                if (freePR == -1) {
                    addSpillNodes(current);
                }
                vrNum = thisOP[oBaseInd + 1];
                freePR = getNextFreePR();
                if (freePR != -1) {
                    VRToPR[vrNum] = freePR;
                    PRToVR[freePR] = vrNum;
                    PRNU[freePR] = thisOP[oBaseInd + 3];
                    thisOP[oBaseInd + 2] = freePR;
                }
            }

            current = current.next;
        }

    }


    private void addSpillNodes(IntRepList.OpNode current) {
        Integer[] thisOP = current.opArray;
        int prToSpill = getPRFurthestUse();
        int vrToSpill = PRToVR[prToSpill];
        PRToVR[prToSpill] = -1; // free the register

        Integer[] loadMemory = new Integer[13];
        loadMemory[0] = 1;
        loadMemory[1] = this.nextSpillLoc += 4; // set the memory spill location
        VRToSpillLoc[vrToSpill] = loadMemory[1];
        loadMemory[11] = PRToVR.length - 1;
        loadMemory[12] = thisOP[12];

        IntRepList.OpNode loadOpNode = new IntRepList.OpNode(loadMemory);

        Integer[] storeOPToInsert = new Integer[13];
        storeOPToInsert[0] = 2; // store operation
        storeOPToInsert[2] = vrToSpill; // put the vr in there
        storeOPToInsert[3] = prToSpill;
        storeOPToInsert[11] = PRToVR.length - 1; // store the current register value in memory
        storeOPToInsert[12] = Integer.MAX_VALUE;

        IntRepList.OpNode storeOpNode = new IntRepList.OpNode(storeOPToInsert);

        // manage node connection

        loadOpNode.prev = current.prev;
        current.prev.next = loadOpNode;
        storeOpNode.prev = loadOpNode;
        loadOpNode.next = storeOpNode;
        storeOpNode.next = current;
        current.prev = storeOpNode;
    }

    private void addRestoreNodes(IntRepList.OpNode current, int vrNum, int opBaseInd) {
        Integer[] thisOP = current.opArray;

        int memoryLocToPull = VRToSpillLoc[vrNum];
        VRToSpillLoc[vrNum] = -1;
        int nextFreePR = getNextFreePR();
        VRToPR[vrNum] = nextFreePR;

        Integer[] loadIOpArray = new Integer[13];
        loadIOpArray[0] = 1;
        loadIOpArray[1] = memoryLocToPull; // set the memory spill location
        loadIOpArray[11] = PRToVR.length - 1; // special register for restoration

        IntRepList.OpNode loadINode = new IntRepList.OpNode(loadIOpArray);

        Integer[] loadOpArray = new Integer[13];
        loadOpArray[0] = 0; // load operation
        loadOpArray[3] = PRToVR.length - 1; // pull from special register
        loadOpArray[10] = vrNum;
        loadOpArray[11] = nextFreePR; // the register we're trying to restore

        IntRepList.OpNode loadNode = new IntRepList.OpNode(loadOpArray);

        // manage node connection

        loadINode.prev = current.prev;
        current.prev.next = loadINode;
        loadNode.prev = loadINode;
        loadINode.next = loadNode;
        loadNode.next = current;
        current.prev = loadNode;
    }


                     /**
     * Gets the index of the next physical register which is not empty
     * @return the first non used physical register at the moment
     */
    private int getNextFreePR() {
        for (int i = 0; i < PRToVR.length - 1; i++) { // last PR reserved
            if (PRToVR[i] == -1) { // a -1 in the value of the PRToVR map = non used phys reg
                return i;
            }
        }
        return -1; // no free physical register
    }

    /**
     * Gets the PR with the furthest next use
     * @return the pr number
     */
    private int getPRFurthestUse() {
        int max = -1;
        int maxPR = -1;
        for (int i = 0; i < PRNU.length; i++) {
            if (PRNU[i] > max) {
                max = PRNU[i];
                maxPR = i;
            }
        }
        return maxPR; // no free physical register
    }

    private boolean isSpilled(int vrNum) {
        return VRToSpillLoc[vrNum] != -1;
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
