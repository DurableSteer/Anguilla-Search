package de.fernunihagen.dbis.anguillasearch.helpers;

import java.util.List;
import java.util.LinkedList;

/**
 * A parametric implementation of a simple AVLTree.
 * The nodes only contain a value that doubles as the searchkey.
 * The tree may not contain douplicate values, hence such values will not be
 * inserted.
 * Values may not be null.
 * 
 * @author Nico Beyer
 */
public class AVLTree<T extends Comparable<T>> {
    private Node root;
    private long size;

    /**
     * An interface to create listener objects for the AVLTree.inOrder() method
     * with.
     */
    public interface Action<T> {
        /**
         * The action to be taken upon reaching a node.
         * 
         * @param value The value held by each visited node.
         */
        public void act(T value);
    }

    /**
     * A node in the tree each holding a value.
     */
    private class Node {
        protected Node left;
        protected Node right;
        protected int height;
        protected T value;

        /**
         * Instantiate a new node containing a value not yet linked.
         * 
         * @param value The value to be contained by the node.
         */
        public Node(T value) {
            this.value = value;
            this.height = 1;
        }
    }

    /**
     * Instantiate a new empty tree.
     */
    public AVLTree() {
        this.root = null;
        this.size = 0;
    }

    /**
     * Get the current height of the given node.
     * 
     * @param node The node to be checked.
     * @return The height of node in the tree.
     */
    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    /**
     * Finds roots subtree with the tallest height.
     * Assumes root is not null!
     * 
     * @param root The node to be checked.
     * @return The height of roots taller subtree.
     */
    private int maxHeight(Node root) {
        return max(height(root.left), height(root.right));
    }

    /**
     * Get the balance indicator of the subtree rooted in root.
     * 
     * (indicator > 1) => left subtree is unbalanced.
     * (indicator < -1) => right subtree is unbalanced.
     * 
     * @param root The root of the subtree to be analysed, may be null.
     * @return The indicator for root.
     */
    private int getBalance(Node root) {
        if (root == null)
            return 0;
        int leftHeight = height(root.left);
        int rightHeight = height(root.right);
        return leftHeight - rightHeight;
    }

    /**
     * Rotate root and its right child one step to the left.
     * Assumes that root and root.right are not null!
     * 
     * @param root The node to be rotated around.
     * @return The new root of the subtree after the rotation.
     */
    private Node rotateLeft(Node root) {
        // Get the nodes that will switch positions.
        Node right = root.right;
        Node rightLeft = right.left;
        // Relink the nodes.
        right.left = root;
        root.right = rightLeft;
        // Update the height of new lower node first then the now higher node.
        root.height = maxHeight(root) + 1;
        right.height = maxHeight(right) + 1;

        return right;
    }

    /**
     * Rotate root and its left child one step to the right.
     * Assumes that root and root.left are not null!
     * 
     * @param root The node to be rotated around.
     * @return The new root of the subtree after the rotation.
     */
    private Node rotateRight(Node root) {
        // Get the nodes that will switch positions.
        Node left = root.left;
        Node leftRight = left.right;
        // Relink the nodes.
        left.right = root;
        root.left = leftRight;
        // Update the height of new lower node first then the now higher node.
        root.height = maxHeight(root) + 1;
        left.height = maxHeight(left) + 1;

        return left;
    }

    /**
     * Insert a new value into the subtree of root and perform rebalancing rotations
     * if neccessary.
     * The new root should be used to update this.root.
     * Null values will not be inserted.
     * 
     * @param root  The root node of the subtree to be inserted into.
     * @param value The value to be inserted.
     * @return The (new) root of the subtree after the insertion.
     */
    private Node insert(Node root, T value) {
        // We reached a leaf, insert Node.
        Node newNode = new Node(value);
        if (root == null) {
            this.size++;
            return newNode;
        }

        int result = root.value.compareTo(value);
        if (result > 0) // root.value > value
            root.left = insert(root.left, value);
        else if (result < 0)// root.value < value
            root.right = insert(root.right, value);
        else // root.value == value
            return root;

        // Recalculate the Height on each node on the way back.
        root.height = maxHeight(root) + 1;

        // Return the (new) rebalanced root.
        return rebalanceAfterInsertion(root, value);
    }

    /**
     * Handle the rebalancing after an insertion for each Node on the way back up.
     * Performs rotations if neccessary.
     * Assumes root is not null!
     * 
     * @param root  The root of the subtree.
     * @param value The value that has been inserted.
     * @return The (new) root of the rebalanced subtree.
     */
    private Node rebalanceAfterInsertion(Node root, T value) {

        int balance = getBalance(root);

        // We inserted left left and imbalance is to the left so rotate right
        if (balance > 1 && root.left.value.compareTo(value) > 0)
            return rotateRight(root);

        // We inserted Right and imbalance is to the right, so rotate left
        if (balance < -1 && root.right.value.compareTo(value) < 0)
            return rotateLeft(root);

        // We inserted to the right of the left child, and imbalance is to the left, LR
        // double rotate
        if (balance > 1 && root.left.value.compareTo(value) < 0) {
            root.left = rotateLeft(root.left);
            return rotateRight(root);
        }

        // We inserted to the left of the right child and imbalance is to the right, RL
        // double rotate
        if (balance < -1 && root.right.value.compareTo(value) > 0) {
            root.right = rotateRight(root.right);
            return rotateLeft(root);
        }

        return root;
    }

    /**
     * * Insert a value into the tree.
     * 
     * @param value The value to be inserted.
     * @throws NullPointerException If null is passed as an argument!
     */
    public void insert(T value) throws NullPointerException {
        if (value == null)
            throw new NullPointerException("AVLTree: Tried to insert null!");
        this.root = insert(this.root, value);
    }

    /**
     * Delete value from the subtree of root.
     * 
     * @param root  The root of the subtree to be deleted from.
     * @param value The value to be deleted.
     * @return The new root of the subtree after deletion.
     */
    private Node delete(Node root, T value) {
        // We reached a leaf, value is not in the tree.
        if (root == null) {
            // Against the assumption no node was deleted.
            this.size++;
            return null;
        }

        T rootValue = root.value;
        Node left = root.left;
        Node right = root.right;

        int result = rootValue.compareTo(value);
        // rootValue > value
        if (result > 0)
            root.left = delete(left, value);
        // rootValue < value
        if (result < 0)
            root.right = delete(right, value);
        if (result == 0) {
            // Node is found and has no children.
            if ((left == null) && (right == null))
                return null;
            // Node has only one child.
            if (left == null)
                return right;
            if (right == null)
                return left;

            // Node has 2 children.
            // Set root value to inorder successor.
            T inorderSuccessorValue = getLeftmostFrom(root.right).value;
            root.value = inorderSuccessorValue;

            // Delete Inorder successor from tree.
            root.right = delete(right, inorderSuccessorValue);
        }

        // On the way back up, update the height of each node along the path.
        root.height = maxHeight(root) + 1;

        // return the rebalanced (new) root.
        return rebalanceAfterDeletion(root);
    }

    /**
     * Handle the rebalancing after a deletion for each Node on the way back up.
     * Performs rotations if neccessary.
     * Assumes root is not null!
     * 
     * @param root The root of the subtree.
     * @return The (new) root of the rebalanced subtree.
     */
    private Node rebalanceAfterDeletion(Node root) {
        Node left = root.left;
        Node right = root.right;
        // Check if the deletion disturbed the balance in each node along the path.
        int balance = getBalance(root);

        // The imbalance is left of the left child, rotate right.
        boolean leftLeft = getBalance(left) >= 0;
        if (balance > 1 && leftLeft)
            return rotateRight(root);

        // The imbalance is right of the left child, double rotate left right.
        boolean leftRight = getBalance(left) < 0;
        if (balance > 1 && leftRight) {
            root.left = rotateLeft(left);
            return rotateRight(root);
        }

        // The imbalance is right of the right child, rotate left.
        boolean rightRight = getBalance(right) <= 0;
        if (balance < -1 && rightRight)
            return rotateLeft(root);

        // The imbalance is left of the right subtree, double rotate right left.
        boolean rightLeft = getBalance(right) > 0;
        if (balance < -1 && rightLeft) {
            root.right = rotateRight(right);
            return rotateLeft(root);
        }
        return root;
    }

    /**
     * Finds the leftmost node from the given root. If the right child of a node is
     * given this is the inorder successor to that node.
     * Assumes root is not null!
     * 
     * @param root The node to go left from.
     * @return The found leftmost node.
     */
    private Node getLeftmostFrom(Node root) {
        Node left = root.left;
        if (left == null)
            return root;
        return getLeftmostFrom(left);
    }

    /**
     * * Delete a value from the tree.
     * 
     * @param value The value to be deleted.
     * @throws NullPointerException If a null value is passed as an argument.
     */
    public void delete(T value) throws NullPointerException {
        if (value == null)
            throw new NullPointerException("AVLTree: Tried to remove null!");
        this.root = delete(this.root, value);

        // Assumes the value is found and successfully deleted.
        this.size--;
    }

    /**
     * Checks if a value is contained in the tree.
     * 
     * @param value The value to be checked for.
     * @throws NullPointerException If null is passed as an argument!
     * @return True if the value is contained in the tree.
     *         False if not.
     */
    public boolean contains(T value) throws NullPointerException {
        if (value == null)
            throw new NullPointerException("AVLTree: Tried to check if null is contained!");
        return retrieve(value) != null;
    }

    /**
     * Find a Value in the tree and return it.
     * Will return null if the value is not found.
     * 
     * @param root  The root of the current subtree to be searched.
     * @param value The value to be searched for.
     * @return The value of the node found or null.
     */
    private T retrieve(Node root, T value) {
        T rootValue = root.value;

        int result = rootValue.compareTo(value);
        // rootValue > value continue in the left subtree.
        if (result > 0) {
            Node left = root.left;
            if (left == null)
                return null;
            return retrieve(left, value);
        }

        // rootValue < value continue in the right subtree.
        if (result < 0) {
            Node right = root.right;
            if (right == null)
                return null;
            return retrieve(right, value);
        }

        // Root contains value. Node found.
        return root.value;

    }

    /**
     * * Find a Value in the tree and return it.
     * Will return null if the value is not found.
     * 
     * @param value The value to be searched for.
     * @throws NullPointerException If null is passed as the value to retrieve!
     * @return The value of the node found or null.
     */
    public T retrieve(T value) throws NullPointerException {
        if (value == null)
            throw new NullPointerException("AVLTree: Tried to retrieve null!");
        if (this.root == null)
            return null;
        return retrieve(this.root, value);
    }

    /**
     * Finds the bigger one of two integers.
     * 
     * @param a First integer.
     * @param b Second integer.
     * @return Returns a if a > b otherwise returns b.
     */
    private int max(int a, int b) {
        return a > b ? a : b;
    }

    /**
     * Traverse the tree in order a listener object may be added to perform an
     * action on each visited nodes value.
     * 
     * @param root   The root of the current subtree.
     * @param action The listener object.
     */
    private void inOrder(Node root, Action<T> action) {
        if (root == null)
            return;
        // Perform listener action on the nodes value.
        action.act(root.value);

        Node left = root.left;
        Node right = root.right;

        if (left != null)
            inOrder(left, action);
        if (right != null)
            inOrder(right, action);
    }

    /**
     * Traverse the tree in order a listener object of the may be added to perform
     * an action on each visited nodes value.
     * 
     * @param action The listener object implementing the AVLTree.Action interface.
     * @throws NullPointerException If null is passed as the action object.
     */
    public void inOrder(Action<T> action) throws NullPointerException {
        if (action == null)
            throw new NullPointerException("AVLTree: Tried to inOrder with null as an action!");
        inOrder(this.root, action);
    }

    /**
     * Get all of the values in the tree in order.
     * 
     * @return A List of all values in the tree in order.
     */
    public List<T> getValuesInorder() {
        List<T> inOrder = new LinkedList<>();
        class PrintValue implements Action<T> {
            @Override
            public void act(T value) {
                inOrder.add(value);
            }
        }
        inOrder(root, new PrintValue());
        return inOrder;
    }

    /**
     * Get the current height of the tree.
     * 
     * @return The height of the tree.
     */
    public int getHeight() {
        return height(this.root);
    }

    /**
     * Creates a representation of the tree as a string for debug purposes. May
     * use ⮽ for an inserted value if the value is too big to display nicely.
     * Use for small trees (height<6), otherwise the representation may not be
     * helpful.
     * 
     * @param useDummyValues If true will use ⮽ to represent existing nodes instead
     *                       of their
     *                       value. If false the value of the node is used.
     * @return A String representing the tree.
     */
    public String toString(boolean useDummyValues) {
        // Tree is empty, nothing to do.
        if (this.root == null)
            return "empty tree";

        LinkedList<Node> currentLevelNodes = new LinkedList<>();
        LinkedList<Node> nextLevelNodes = new LinkedList<>();

        // Start with the root.
        currentLevelNodes.addLast(this.root);

        // Get zero based height of the tree.
        int treeHeight = getHeight() - 1;

        // Get the space taken up by the leafs.
        int nodesMaxSpaces = (int) Math.pow(2, treeHeight) * 2;

        StringBuilder output = new StringBuilder(nodesMaxSpaces * treeHeight);

        for (int level = 0; level <= treeHeight; level++) {
            // Print a tree level.

            int nodesThisLevel = (int) Math.pow(2, level);
            String padding = " ".repeat((nodesMaxSpaces / (nodesThisLevel + 1)));
            output.append(padding);
            // Print the nodes values on this level left to right and save their children to
            // the nextLevelNodes queue.
            while (!currentLevelNodes.isEmpty()) {
                Node cur = currentLevelNodes.pop();
                if (cur == null) {
                    output.append("☐" + padding);
                    nextLevelNodes.addLast(null);
                    nextLevelNodes.addLast(null);
                } else {
                    output.append((useDummyValues ? "⮽" : cur.value) + padding);
                    nextLevelNodes.addLast(cur.left);
                    nextLevelNodes.addLast(cur.right);
                }
            }
            output.append("\n");
            // Go to the next level down.
            currentLevelNodes = nextLevelNodes;
            nextLevelNodes = new LinkedList<>();
        }
        return output.toString();
    }

    /**
     * Get the number of values in the tree.
     * 
     * @return The number of values in the tree.
     */
    public long size() {
        return size;
    }

    /**
     * Check if the tree is empty or not.
     * 
     * @return True if the tree is empty false otherwise.
     */
    public boolean isEmpty() {
        return this.root == null;
    }
}