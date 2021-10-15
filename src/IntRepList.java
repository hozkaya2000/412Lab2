import java.util.LinkedList;

/**
 * The class to store the intermediate representation
 */
public class IntRepList {

    public static class OpNode {

        Integer[] opArray;

        OpNode next = null;
        OpNode prev = null;

        public OpNode (Integer[] opArray) {
            this.opArray = opArray;
        }

        public void setNext(OpNode next) {
            this.next = next;
        }

        public void setPrev(OpNode prev) {
            this.prev = prev;
        }

    }

    public OpNode head;
    public OpNode tail;
    public int size;

    public IntRepList () {
        this.size = 0;
        this.head = null;
        this.tail = null;
    }

    public void transferLinkedList(LinkedList<Integer[]> toTransfer) {
        head = new OpNode(toTransfer.getFirst());
        tail = head;
        int i = 0;
        for (Integer[] opArray: toTransfer) {
            if (i == 0) {
                i++;
                continue;
            }
            OpNode next = new OpNode(opArray);
            this.tail.next = next;
            next.prev = tail;
            this.tail = next;
        }
    }

}
