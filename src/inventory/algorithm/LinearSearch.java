package inventory.algorithm;

import inventory.model.Product;
import java.util.Locale;

/**
 * ALGORITHM 1 - Linear Search, written by hand.
 *
 * <p>Used by the Search box for <b>Name</b> and <b>Category</b>. The match is
 * partial and case-insensitive ("note" finds "Notebook A5") and more than one
 * product can be returned, so the method first counts the hits and then fills an
 * array of exactly that size. Every element is visited once, so the cost is O(n).</p>
 */
public class LinearSearch {

    /** Products whose name contains the keyword (ignoring case). */
    public static Product[] searchByName(Product[] data, String keyword) {
        return search(data, keyword, true);
    }

    /** Products whose category contains the keyword (ignoring case). */
    public static Product[] searchByCategory(Product[] data, String keyword) {
        return search(data, keyword, false);
    }

    /** Shared linear scan used by both searches above. */
    private static Product[] search(Product[] data, String keyword, boolean byName) {
        if (data == null || keyword == null || keyword.trim().isEmpty()) {
            return new Product[0];
        }
        String needle = keyword.trim().toLowerCase(Locale.ENGLISH);

        // Pass 1: count the matches so the result array has the right size.
        int matches = 0;
        for (int i = 0; i < data.length; i++) {
            if (matchesIn(data[i], needle, byName)) {
                matches++;
            }
        }

        // Pass 2: copy the matches into the result array.
        Product[] result = new Product[matches];
        int position = 0;
        for (int i = 0; i < data.length; i++) {
            if (matchesIn(data[i], needle, byName)) {
                result[position++] = data[i];
            }
        }
        return result;
    }

    private static boolean matchesIn(Product product, String needle, boolean byName) {
        String field = byName ? product.getName() : product.getCategory();
        if (field == null) {
            return false;
        }
        return field.toLowerCase(Locale.ENGLISH).contains(needle);
    }
}
