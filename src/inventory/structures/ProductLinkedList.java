package inventory.structures;

import inventory.model.Product;

/**
 * DATA STRUCTURE 1 - Singly linked list.
 *
 * <p>The master store of every product. The GUI displays the list, and adding or
 * deleting a product changes this list. It is written by hand (no
 * {@code java.util.LinkedList}) and every node only knows the next node.</p>
 *
 * <p>Operations used by the system:</p>
 * <ul>
 *   <li>{@link #addLast(Product)} - O(1) because the tail node is kept</li>
 *   <li>{@link #get(String)} / {@link #remove(String)} - O(n) scan by ID</li>
 *   <li>{@link #toArray()} - O(n) copy used for sorting, searching and the JTable</li>
 * </ul>
 */
public class ProductLinkedList {

    /** One link of the chain. */
    private class Node {
        private Product data;
        private Node next;

        Node(Product data) {
            this.data = data;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    /**
     * Adds a product at the end of the list.
     */
    public void addLast(Product product) {
        if (product == null) {
            return;
        }
        Node node = new Node(product);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /**
     * Removes the product with the given ID.
     *
     * @return true when a node was removed, false when the ID was not found
     */
    public boolean remove(String id) {
        if (id == null) {
            return false;
        }
        Node previous = null;
        Node current = head;
        while (current != null && !id.equals(current.data.getId())) {
            previous = current;
            current = current.next;
        }
        if (current == null) {
            return false;
        }
        if (previous == null) {
            head = current.next;
        } else {
            previous.next = current.next;
        }
        if (current == tail) {
            tail = previous;
        }
        size--;
        return true;
    }

    /** Finds a product by scanning the chain. Returns null when not found. */
    public Product get(String id) {
        if (id == null) {
            return null;
        }
        Node current = head;
        while (current != null) {
            if (id.equals(current.data.getId())) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Copies the chain into a plain array so the algorithms (search and sort)
     * and the table can work on it without touching the list itself.
     */
    public Product[] toArray() {
        Product[] result = new Product[size];
        int position = 0;
        Node current = head;
        while (current != null) {
            result[position++] = current.data;
            current = current.next;
        }
        return result;
    }
}
