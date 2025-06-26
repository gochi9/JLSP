package gmail.vladimir.JLSP.Helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used to add names and prefixes for function support
 */
public class NameSearcher {

    private Map<String, Integer> prefixCounts = new HashMap<>();

    /**
     * Method that breaks a string into multiple prefixes for searching purposes
     */
    public void addName(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            sb.append(c);
            String prefix = sb.toString();
            prefixCounts.merge(prefix, 1, Integer::sum);
        }
    }

    /**
     * Removes the name and prefixes for the selected function. Should certain prefixes also be shared by other functions, they will not be deleted.
     */
    public void removeName(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            sb.append(c);
            String prefix = sb.toString();
            prefixCounts.computeIfPresent(prefix, (k, v) -> v == 1 ? null : v - 1);
        }
    }

    /**
     * Check if the current string is a valid function prefix
     */
    public boolean exists(String prefix) {
        return prefixCounts.containsKey(prefix);
    }

    /**
     * Clears every function name
     */
    public void clear(){
        prefixCounts.clear();
    }

}
