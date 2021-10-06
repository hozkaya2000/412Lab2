import java.io.*;
import java.util.HashMap;
import java.util.Stack;

/**
 * A class meant to scan the next word in an ILOC file.
 */
public class ILOCScanner {

    /**
     * The buffered input stream
     */
    BufferedInputStream bufInStream;

    /**
     * Classifier table
     */
    HashMap<Character, Integer> classifierTable;

    /**
     * Transition table
     */
    Integer[][] transitionTable;

    /**
     * The token types represented as strings. The index represents the number assigned to that
     * token (see tokenTypeInts)
     */
    String[] tokenTypeStrings;

    /**
     * The token types represented as integers
     */
    Integer[] tokenTypeInts;

    /**
     * The language map represented as integers. This excludes constants and register integer representations,
     * which are converted in the method langToInt.
     */
    HashMap<String, Integer> langMap;

    /**
     * To keep track of the -s flag to print the token, lexeme tuples
     */
    boolean printTokens;

    /**
     * The constructor for the scanner
     * @param filePath the absolute file to scan
     */
    public ILOCScanner(String filePath, boolean printTokens) {
        this.classifierTable = this.createClassifierTable(); // establish the classifier Table
        this.transitionTable = this.createTransitionTable(); // establish the transition table

        this.tokenTypeInts = this.createTokenTypeTable();
        this.tokenTypeStrings = new String[] {"MEMOP", "LOADI", "ARITHOP", "OUTPUT", "NOP", "CONSTANT",
                "REG", "COMMA", "INTO", "EOF", "COMMENT", "NEWLINE", "ERROR"}; // 13 token types

        this.langMap = createLangMap();
        this.printTokens = printTokens;


        try {
            bufInStream = new BufferedInputStream(new FileInputStream(filePath), 1000); // 1 kilobyte buffer
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Gets the next word character by character
     * @return The next word
     * @throws IOException when there is an error reading from the inputstream
     */
    public Integer[] NextToken() throws IOException {
        char nextChar; // the next character to be read
        int state = 0; // the current state
        int charTransition = 20; // the column number for the state, character to next state transition. space default
        StringBuilder lexeme = new StringBuilder();
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(-2); // -2 represents 'bad'
        this.bufInStream.mark(Integer.MAX_VALUE); // mark at the beginning ('bad') mark

        while(state != -1) {
            nextChar = (char) this.NextChar();

            lexeme.append(nextChar);
            if (checkAcceptingState(state)) {
                stateStack.clear();
            }
            stateStack.push(state); // track states in the stack

            try
            {
                charTransition = this.classifierTable.get(nextChar);
            }
            catch (Exception ignored) {}

            state = this.transitionTable[state][charTransition]; // transition into the next state
            if (checkAcceptingState(state))
                this.bufInStream.mark(Integer.MAX_VALUE);
        }

        this.RollBack();
        while(state != -2 && !checkAcceptingState(state) ) {
            state = stateStack.pop();
            if (lexeme.length() != 0)
                lexeme.deleteCharAt(lexeme.length() - 1);
        }

        // if an accepting state, return the token type and the lexeme
        if (checkAcceptingState(state)) {
            if (this.printTokens)
                System.out.println("<" + tokenTypeStrings[tokenTypeInts[state]] + ", " + lexeme.toString().trim() + ">");
            return new Integer[]
                    {tokenTypeInts[state],
                            lexemeToInt(tokenTypeInts[state], lexeme.toString().trim())};
        }

        if (this.printTokens)
            System.out.println("<" + tokenTypeStrings[12] + ", " + lexeme.toString().trim() + ">");

        /*
         * In the error case, read to the end of the ine
         */

        do {
            this.bufInStream.mark(Integer.MAX_VALUE); //
            nextChar = (char) this.NextChar();
            lexeme.append(nextChar);
        } while (nextChar != '\n' && nextChar != '\uFFFF');

        if (nextChar == '\n') {
            this.bufInStream.reset(); // in the
        }


        return new Integer[]{12, -1}; // The ERROR token is returned
    }

    /**
     * Creates the opcode map, sending the string version of an opcode to its integer representation
     * @return the opcode map
     */
    private HashMap<String, Integer> createLangMap() {
        HashMap<String, Integer> lMap = new HashMap<>();
        lMap.put("load", 0);
        lMap.put("loadI", 1);
        lMap.put("store", 2);
        lMap.put("add", 3);
        lMap.put("sub", 4);
        lMap.put("mult", 5);
        lMap.put("lshift", 6);
        lMap.put("rshift", 7);
        lMap.put("output", 8);
        lMap.put("nop", 9);
        lMap.put(",", 10);
        lMap.put("=>", 11);
        return lMap;
    }

    /**
     * Gets the integer representation of the valid lexeme word
     * @param lexeme the lexeme
     * @return the integer representation
     */
    private int lexemeToInt(int tokenType, String lexeme) {
        if (tokenType == 5) // if constant store as in
            return Integer.parseInt(lexeme);
        else if (tokenType == 6) // if register store as int as well
            return Integer.parseInt(lexeme.substring(1));
        else // get the mapped integer value from 0 to 11. 12 represents an error opcode
            return langMap.getOrDefault(lexeme, 12);
    }

    /**
     * Gets the next character from the input stream
     * @return the next character being read
     * @throws IOException in case the input stream fails
     */
    private int NextChar() throws IOException {
        return this.bufInStream.read();
    }

    /**
     * Goes back to the marked position in the BufferedInputStream
     * @throws IOException if there is an IO error while trying to go back in the stream
     */
    private void RollBack() throws IOException {
        this.bufInStream.reset();
    }

    /**
     * Check if the given state is an accepting state
     * @param state The state to check if accepting
     * @return Whether it is an accepting state
     */
    private boolean checkAcceptingState(int state) {
        return state >= 0 && tokenTypeInts[state] != 12; // 11 represents the non accepting state
    }

    /**
     * Creates the token type table based on ILOC specifications.
     * the operations types they represent.
     * @return the token type table from state to token representation
     */
    private Integer[] createTokenTypeTable () {

        // 0 "MEMOP",
        // 1 "LOADI",
        // 2 "ARITHOP",
        // 3 "OUTPUT",
        // 4 "NOP",
        // 5 "CONSTANT",
        // 6 "REG",
        // 7 "COMMA",
        // 8 "INTO",
        // 9 "EOF",
        // 10 "COMMENT"
        // 11 INVALID, "ERROR"

        Integer[] tTInts = new Integer[44];

        for(int i = 0; i < 42; i++) {
            tTInts[i] = 12; // every state is not accepting at first
        }

        tTInts[5] = 0;
        tTInts[7] = 2;
        tTInts[11] = 0;
        tTInts[12] = 1;
        tTInts[18] = 2;
        tTInts[24] = 2;
        tTInts[27] = 4;
        tTInts[33] = 3;
        tTInts[35] = 8;
        tTInts[36] = 7;
        tTInts[37] = 5;
        tTInts[38] = 6;
        tTInts[41] = 10; // Comment
        tTInts[42] = 9;  // EOF
        tTInts[43] = 11; // Newline


        return tTInts;
    }

    /**
     * Creates the classifier table as specified within the function.
     * Each character corresponds to an integer that they're reading will
     * associated with when trying to access the correct column in the
     * transition table.
     * @return the created Classifier Table
     */
    private HashMap<Character, Integer> createClassifierTable () {
        HashMap<Character, Integer> cTable = new HashMap<>();

        cTable.put('s', 0);
        cTable.put('t', 1);
        cTable.put('o', 2);
        cTable.put('r', 3);
        cTable.put('e', 4);
        cTable.put('u', 5);
        cTable.put('b', 6);
        cTable.put('l', 7);
        cTable.put('a', 8);
        cTable.put('d', 9);
        cTable.put('h', 10);
        cTable.put('i', 11);
        cTable.put('f', 12);
        cTable.put('m', 13);
        cTable.put('n', 14);
        cTable.put('p', 15);
        cTable.put('I', 16);
        cTable.put('=', 17);
        cTable.put('>', 18);
        cTable.put(',', 19);
        cTable.put(' ', 20); cTable.put('\t', 20); //the tab and space characters are equivalent
        cTable.put('/', 21);
        cTable.put('\n', 22);

        // numerical integers
        cTable.put('0', 23);
        cTable.put('1', 23);
        cTable.put('2', 23);
        cTable.put('3', 23);
        cTable.put('4', 23);
        cTable.put('5', 23);
        cTable.put('6', 23);
        cTable.put('7', 23);
        cTable.put('8', 23);
        cTable.put('9', 23);

        //EOF
        cTable.put('\uFFFF', 24);

        return cTable;
    }

    /**
     * Creates the transition table for an ILOC Scanner
     * @return the two dimensional array where the row, column transition represents what
     *         next state(value) the current state(row num) transitions(column num) to
     */
    private Integer[][] createTransitionTable() {
        return new Integer[][]
                {
                        //0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19  20  21  22  23  24 25
                        //s   t   o   r   e   u   b   l   a   d   h   i   f   m   n   p   I   =   >   ,  \t   /  \n nums EOF other
                        { 1, -1, 28, 13, -1, -1, -1,  8, 22, -1, -1, -1, -1, 19, 25, -1, -1, 34, -1, 36,  0, 39, 43, 37, 42, -1}, // s0
                        {-1,  2, -1, -1, -1,  6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s1
                        {-1, -1,  3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s2
                        {-1, -1, -1,  4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s3
                        {-1, -1, -1, -1,  5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s4
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s5
                        {-1, -1, -1, -1, -1, -1,  7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s6
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s7
                        {14, -1,  9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s8
                        {-1, -1, -1, -1, -1, -1, -1, -1, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s9
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s10
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 12, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s11
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s12
                        {14, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -1, -1}, // s13
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s14
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s15
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s16
                        {-1, 18, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s17
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s18
                        {-1, -1, -1, -1, -1, 20, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s19
                        {-1, -1, -1, -1, -1, -1, -1, 21, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s20
                        {-1, 18, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s21
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, 23, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s22
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, 24, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s23
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s24
                        {-1, -1, 26, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s25
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 27, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s26
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s27
                        {-1, -1, -1, -1, -1, 29, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s28
                        {-1, 30, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s29
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s30
                        {-1, -1, -1, -1, -1, 32, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s31
                        {-1, 33, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s32
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s33
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 35, -1, -1, -1, -1, -1, -1, -1}, // s34
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s35
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s36
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 37, -1, -1}, // s37
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -1, -1}, // s38
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 40, -1, -1, -1, -1}, // s39
                        {40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 41 ,40, -1, 40}, // s40
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s41
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // s42
                        {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}  // s43
                        //s   t   o   r   e   u   b   l   a   d   h   i   f   m   n   p   I   =   >   ,  \t   /  \n nums other
                };
    }
}
