package org.ulpgc.inverted_index.apps;


import java.util.*;

// Class representing the Trie data structure
public class Trie {
    private TrieNode root; // Root node of the trie

    // Constructor initializes the root node
    public Trie() {
        this.root = new TrieNode();
    }

    // Inserts a word along with its book ID and position into the trie
    public void insert(String word, String bookId, int position) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            // Traverse the trie and create new nodes if necessary
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        // Add the position to the document info for the corresponding book ID
        node.docInfo.computeIfAbsent(bookId, k -> new ArrayList<>()).add(position);
    }

    // Returns the root node of the trie
    public TrieNode getRoot() {
        return root;
    }

    // Searches for a word and returns the document info if found
    public Map<String, List<Integer>> search(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.get(ch); // Traverse the trie
            if (node == null) {
                return null; // Return null if the word is not found
            }
        }
        return node.docInfo; // Return document info for the found word
    }
}
