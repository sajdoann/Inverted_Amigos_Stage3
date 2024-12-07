package org.ulpgc.inverted_index.apps;


import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Class representing a node in the Trie
public class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>(); // Map of child nodes
    Map<String, List<Integer>> docInfo = new HashMap<>(); // Map to hold document info (book ID to positions)

    // Serializes the TrieNode into MessagePack format
    public void toMessagePack(MessagePacker packer) throws IOException {
        packer.packMapHeader(docInfo.size()); // Pack the document info map
        for (Map.Entry<String, List<Integer>> entry : docInfo.entrySet()) {
            packer.packString(entry.getKey()); // Pack the book ID
            packer.packArrayHeader(entry.getValue().size()); // Pack the size of positions array
            for (Integer position : entry.getValue()) {
                packer.packInt(position); // Pack each position
            }
        }

        // Serialize the child nodes
        packer.packMapHeader(children.size());
        for (Map.Entry<Character, TrieNode> childEntry : children.entrySet()) {
            packer.packString(String.valueOf(childEntry.getKey())); // Pack the character key
            childEntry.getValue().toMessagePack(packer); // Recursively serialize child nodes
        }
    }
}
