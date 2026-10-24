# Inventory Management System

Desktop inventory manager built for the **IT 201 - Data Structures and Algorithms**
course project.

* **Language / GUI:** Java 21, Java Swing + AWT only (no external libraries)
* **Storage:** plain text files (`data/products.txt`, `data/transactions.txt`)
* **Data structures:** hand-written singly linked list, hash table with separate
  chaining, and a transaction stack - no `ArrayList`, `LinkedList`, `HashMap` or
  `Stack` from `java.util` in the logic
* **Algorithms:** linear search, binary search, insertion sort

---

## 1. How to run

### NetBeans (Java with Ant) - recommended

1. `File > Open Project...` and select this folder. NetBeans recognises it as a
   Java Application project because of `nbproject/`.
2. Right-click the project -> `Properties` -> `Sources` and set the
   source/binary format to **JDK 21** (`javac.source` / `javac.target` are already
   set to `21` in `nbproject/project.properties`).
3. Press **F6** (`Run Project`). The main class is `inventory.Main`.

`Build > Clean and Build` produces `dist/InventoryManagementSystem.jar`.
Keep the `data/` folder next to the JAR when you submit or copy the project,
because the storage layer uses the relative path `data/`.

### Command line (Windows PowerShell)

```powershell
cd "C:\path\to\InventoryManagementSystem"
javac -d out -encoding UTF-8 -sourcepath src src\inventory\Main.java
java -cp out inventory.Main
```

`javac` follows the imports, so compiling `Main.java` with `-sourcepath src`
builds every class of the project (28 `.class` files in `out/`).

### Command line (macOS / Linux)

```bash
cd /path/to/InventoryManagementSystem
javac -d out -encoding UTF-8 -sourcepath src src/inventory/Main.java
java -cp out inventory.Main
```

### Runnable JAR

```powershell
jar cfm InventoryManagementSystem.jar manifest.mf -C out .
java -jar InventoryManagementSystem.jar
```

### Apache Ant

NetBeans ships with Ant; a standalone Ant installation also works. `build.xml`
and `nbproject/build-impl.xml` define the targets `compile`, `run`, `jar`,
`clean` and `default`:

```bash
ant run     # compile and start the application
ant jar     # compile and package dist/InventoryManagementSystem.jar
ant clean   # delete build/ and dist/
```

The program must be started from the project folder (the folder that contains
`data/`), otherwise the storage layer creates a new empty `data/` folder next to
the current working directory.

### Prebuilt packages (submission copy)

The folder handed in for grading also carries two ready-to-run builds in
`release/`. They are not part of this git repository (the bundled runtime makes
them about 25 MB), and `ant clean` only deletes `build/` and `dist/`, so they
stay in place:

* `release/A_jar/` - the JAR plus `run.bat` (Java version check, then
  `java -jar`) and a clean `data/` folder. Needs Java 21 or newer.
* `release/B_app/` - a `jpackage` app image with its own Java 21 runtime, so it
  starts on a machine that has no Java installed at all.
* `release/InventoryManagementSystem.zip` - both folders plus the end-user
  `README.txt`, packed for hand-in.
* `release/README.txt` - the same text that ships inside the zip.

Extract the zip before running anything: a file launched from the Explorer zip
view is copied to a temporary folder on its own, so `data/` is missing and the
table comes up empty.

---

## 2. What the program does

| Feature | Notes |
|---|---|
| **Display all products** | `JTable` with ID, Name, Category, Quantity, Price, Reorder Level |
| **Add product** | Modal `ProductDialog`; the ID is validated and checked for duplicates against the hash table |
| **Update product** | Modal `ProductDialog` filled in; the ID field is read-only, quantity is only changed by Stock In / Stock Out |
| **Delete product** | Confirmation dialog first, then the product leaves the list and the index; the full product is kept in the transaction log so Undo can restore it |
| **Stock In / Stock Out** | Modal `StockDialog`; Stock Out is rejected when the amount is larger than the quantity on hand, so a quantity can never go negative |
| **Undo Last Action** | Reverses the newest action that has not been undone yet (ADD / UPDATE / DELETE / STOCK_IN / STOCK_OUT) and logs the undo as its own transaction |
| **Transaction history** | Modal `HistoryDialog` shows the stack newest first, with reversed rows marked as undone |
| **Search** | By name or category (partial, case-insensitive, linear search) or by exact ID (binary search on an ID-sorted copy) |
| **Sort** | By ID, Name, Quantity or Price, ascending or descending (insertion sort on a copy) |
| **Low-stock highlight** | Rows whose `quantity <= reorderLevel` are painted red and counted in the status bar |
| **Total inventory value** | Status bar reads `Total Inventory Value: P0.00 \| Items: 0 \| Low: 0` (value = `sum of quantity * price`) |
| **Save / Load** | Menu `File > Save`, `File > Load`; the data is also loaded at startup and saved when the window is closed |

---

## 3. Using the program

### Windows and dialogs

| Window | Class | Contents |
|---|---|---|
| Main window | `MainFrame` (`JFrame`) | search + sort bar, product table, five action buttons (Add, Update, Delete, Stock In, Stock Out), status bar, menu bar |
| Add / Update | `ProductDialog` (`JDialog`, modal) | ID, Name, Category, Quantity, Price, Reorder Level, Save / Cancel (ID and Quantity become read-only in Update mode) |
| Stock In / Stock Out | `StockDialog` (`JDialog`, modal) | read-only product ID, name and quantity, amount field, Stock In / Stock Out and Cancel buttons |
| Transaction history | `HistoryDialog` (`JDialog`, modal) | the stack newest first, Undo Last Action / Close, row counter |

### Menu bar (each item has a Ctrl accelerator)

| Menu | Items |
|---|---|
| **File** | Save (Ctrl+S), Load (Ctrl+R), Exit (Ctrl+Q) |
| **Inventory** | Add Product (Ctrl+N), Update Product (Ctrl+U), Delete Product (Ctrl+D) |
| **Stock** | Stock In (Ctrl+I), Stock Out (Ctrl+O), Undo Last Action (Ctrl+Z) |
| **View** | Transaction History (Ctrl+H), Show All (Ctrl+A), Low-Stock Only (Ctrl+L) |

### Typical workflow

1. Start the program - the products from `data/products.txt` appear in the table.
2. Add a product with **Inventory > Add Product** (or the **Add** button); a
   duplicate ID or bad number is reported in a message box and the dialog stays
   open.
3. Change stock with **Stock In** / **Stock Out**; taking out more than the
   quantity on hand is refused ("Not enough stock: ...").
4. Made a mistake? **Stock > Undo Last Action** (or the button inside the history
   dialog) reverses the newest action, and the history dialog shows both the
   reversal and the original row marked as undone.
5. Search or sort; the table shows the result and **Show All** returns to the
   complete list.
6. Close the window (or **File > Save**) to write both text files.

---

## 4. Project structure

```
InventoryManagementSystem/
  build.xml                      Ant build (compile / run / jar / clean)
  manifest.mf                    Main-Class manifest entry
  nbproject/                     NetBeans project metadata
  data/
    products.txt                 sample inventory (10 products)
    transactions.txt             transaction log (empty on a fresh copy)
  src/inventory/
    Main.java                    entry point, creates the window on the EDT
    model/
      Product.java               product + toFileString / fromFileString / copy
      Transaction.java           log row + undo flags + toFileString / fromFileString
    structures/
      ProductLinkedList.java     DATA STRUCTURE 1 - master list
      ProductHashTable.java      DATA STRUCTURE 2 - index by ID
      TransactionStack.java      DATA STRUCTURE 3 - undo history
    algorithm/
      LinearSearch.java          ALGORITHM 1 - name / category search
      BinarySearch.java          ALGORITHM 2 - exact ID search
      SortAlgorithm.java         ALGORITHM 3 - insertion sort
    service/
      InventoryService.java      business rules, keeps list + index + stack in sync
      Validator.java             all input validation and parsing
      InventoryException.java    message shown to the user
    storage/
      FileHandler.java           BufferedReader / BufferedWriter persistence
    gui/
      MainFrame.java             main window
      ProductDialog.java         add / update form
      StockDialog.java           stock in / out form
      HistoryDialog.java         transaction history + undo
  release/                       prebuilt A_jar / B_app / zip for hand-in
                                 (local only, not in git)
```

The GUI never touches the linked list, the hash table or the files. It collects
text, calls `InventoryService` and shows the result, so every rule exists once.

---

## 5. Data structures

### 5.1 `ProductLinkedList` - master list of products

Singly linked list with an inner `Node` (`Product data`, `Node next`) and a `tail`
reference.

| Method | Purpose |
|---|---|
| `addLast(Product)` | append in O(1) - keeps insertion order for the table and the file |
| `get(String id)` / `remove(String id)` | walk the chain, O(n) |
| `size()`, `isEmpty()`, `clear()` | bookkeeping |
| `toArray()` | `Product[]` copy used by sorting, searching, saving and the table |

The list owns the products: it is the source of truth for display and for
`products.txt`.

### 5.2 `ProductHashTable` - index by product ID

* 101 buckets (a prime, so IDs spread out well), each bucket is a chain of
  `Entry` nodes - that chain is the **separate chaining** collision handling.
* The hash function is written by hand and does **not** use `hashCode()`:
  `h = (h * 31 + character) % 101` for every character of the key.
* `put`, `get`, `remove`, `containsKey`, `clear`, `size`, `capacity`.

It answers "does this ID exist?" and "give me the product with this ID" in
(expected) O(1), which is what Add (duplicate check), Update, Delete, Stock In
and Stock Out need.

### 5.3 `TransactionStack` - undo history

Array-based LIFO stack that starts with 8 slots and **doubles** when full, so
`push` is amortized O(1).

| Method | Purpose |
|---|---|
| `push(Transaction)` | new row on top |
| `pop()` | remove the top row (used while walking down during an undo) |
| `peek()` | look at the top row without removing it |
| `toArray()` | copy newest first - the order the history table shows |
| `isEmpty()`, `size()` | bookkeeping |

Rows are never deleted from the middle of the log: the stack *is* the
append-only transaction history.

---

## 6. Algorithms

### 6.1 Linear search - `LinearSearch`

`searchByName(data, keyword)` and `searchByCategory(data, keyword)` scan the
array from the first to the last element and keep every product whose name or
category contains the keyword (case-insensitive, partial match). The matches are
copied into a new `Product[]`, so several results are possible and the original
array is untouched. O(n) per search.

### 6.2 Binary search - `BinarySearch`

`indexOfById(sortedById, id)` / `findById(sortedById, id)` work on an array that is
already sorted ascending by ID. The search keeps a `low`/`high` range, compares
`id` with the middle ID (`compareToIgnoreCase`) and halves the range each step:
found in O(log n), not found after about `log2(n)` comparisons.

`InventoryService.searchById` performs the required "sort first, then search":
it copies the list with `toArray()`, sorts the copy by ID with the same insertion
sort and only then calls the binary search.

### 6.3 Insertion sort - `SortAlgorithm`

One implementation with a field parameter (`BY_ID`, `BY_NAME`, `BY_QUANTITY`,
`BY_PRICE`) and an `ascending` flag; numbers are compared with
`Integer.compare` / `Double.compare`, text with `compareToIgnoreCase`.

```
for i = 1 .. n-1
    current = data[i]
    j = i - 1
    while j >= 0 and data[j] > current
        data[j+1] = data[j]
        j--
    data[j+1] = current
```

Worst case O(n^2) (reverse-sorted input), best case O(n) (already sorted), in
place, stable. It runs on the array returned by `toArray()`, so the linked list
keeps its insertion order and the sorted result is only what the table displays.

---

## 7. Undo design

Undo never deletes or rewrites history. It walks down the stack and reverses the
newest action that is still undoable.

1. Rows are `pop()`ed from the top until one is found that `isUndoable()`.
   Skipped rows (already undone, or `UNDO` rows) are kept in a temporary array.
2. The skipped rows are `push()`ed back in their original order, so the stack
   looks exactly as it did before the walk.
3. `reverse(target)` applies the opposite action:

   | Original | Reversal |
   |---|---|
   | `ADD` | remove the product (refused if it no longer exists) |
   | `DELETE` | re-create the product from the snapshot in the row (refused if the ID is used again) |
   | `UPDATE` | restore name, category, price, reorder level from the snapshot |
   | `STOCK_IN` / `STOCK_OUT` | restore the previous quantity from the snapshot |

4. The reversed row is marked `undone = true` and stays in the log, and a new
   `UNDO` row - carrying the reversed type in `undoTargetType` and the net
   quantity change - is pushed on top of it.
5. When nothing is left to undo the stack is restored and the user sees
   "Nothing to undo." Undo-of-undo (redo) is deliberately not supported: `UNDO`
   rows are never themselves undoable.

Because the `undone` flag and the `UNDO` rows are saved to
`data/transactions.txt`, the same action can not be undone twice after a
restart. Example of the log after a `STOCK_IN` and the undo that reversed it:

```
STOCK_IN|P001|Notebook A5|10|2026-10-24 14:01:22|Notebook A5|Stationery|120|2.5|30|1|
UNDO|P001|Notebook A5|-10|2026-10-24 14:01:40|||0|0.0|0|0|STOCK_IN
```

The first row shows `undone = 1` and the second row records what was reversed.

---

## 8. File formats

Both files are pipe-delimited text files in `data/`. They are written with
`BufferedWriter` inside try-with-resources (one line per record) and read with
`BufferedReader` in a `readLine()` loop, also inside try-with-resources.

### 8.1 `data/products.txt` - 6 fields, one product per line

```
id|name|category|quantity|price|reorderLevel
```

```
P001|Notebook A5|Stationery|120|2.50|30
P002|Ballpoint Pen Blue|Stationery|8|0.80|25
P004|Wireless Mouse|Electronics|23|18.90|10
```

The file always holds the current products only - undone or deleted products are
not written here. When the file does not exist on the first run it is created
empty and the program starts with an empty inventory.

### 8.2 `data/transactions.txt` - 12 fields, one transaction per line

```
type|productId|productName|quantityChange|timestamp|prevName|prevCategory|prevQuantity|prevPrice|prevReorderLevel|undone|undoTargetType
```

* `timestamp` uses `yyyy-MM-dd HH:mm:ss`.
* `undoTargetType` is filled only on `UNDO` rows.
* `undone` is `1` when the action has been reversed and `0` otherwise.
* Unused fields are written as **empty** fields, and each line is split with a
  limit so empty fields are kept. That is why names and categories may not
  contain `|`.
* The file stores the rows **oldest first**; on load they are pushed oldest to
  newest so the newest row ends up on top of the stack again.

Load rules:

* Blank lines are skipped.
* A corrupted line (wrong number of fields, unparsable number) is skipped, the
  rest keeps loading, and the user is told how many lines were ignored.
* A product ID that appears twice is loaded once (the first line wins) and
  counted as skipped.
* A missing file is created empty instead of crashing.

---

## 9. Validation rules (`Validator`)

| Field | Rule |
|---|---|
| Product ID | required, no spaces, no `|`, at most 10 characters, unique (checked with the hash table) |
| Name | required, not blank, no `|`, at most 50 characters |
| Category | required, not blank, no `|`, at most 50 characters |
| Quantity | required, whole number, `>= 0` |
| Price | required, number, `> 0` |
| Reorder Level | required, whole number, `>= 0` |
| Stock In amount | whole number `> 0` |
| Stock Out amount | whole number `> 0` and `<= quantity on hand` |
| Search keyword | not empty |
| Update / Delete / Stock | a product must be selected (or a valid ID entered) and must exist |

Every value is `trim()`ed before it is validated, and every failure throws an
`InventoryException` whose message is shown to the user as it is.

---

## 10. Complexity summary

| Operation | Cost |
|---|---|
| Add product | O(1) list append + expected O(1) hash put |
| Find by ID (hash table) | expected O(1) |
| Update / Stock In / Stock Out | expected O(1) |
| Delete | expected O(1) hash remove + O(n) list unlink |
| Linear search (name / category) | O(n) |
| Binary search (ID) | O(log n) after a sort of O(n^2) |
| Insertion sort | O(n^2) worst, O(n) best, O(1) extra space |
| Undo | O(k) where k rows are popped before the target is found |
| Save / load | O(n) |
| History view | O(n) copy of the stack |

---

## 11. Error handling

* `NumberFormatException` is caught inside `Validator`, which turns it into a
  friendly message ("Price must be a number, for example 12.50.").
* `InventoryException` messages (duplicate ID, unknown ID, not enough stock,
  nothing to undo, unreadable file) are shown with
  `JOptionPane.showMessageDialog(..., ERROR_MESSAGE)`.
* An `IOException` in `FileHandler` becomes
  "Could not save the data file: ..." or "Could not load the data file: ...".
* Delete asks for confirmation first, and `File > Load` asks before it
  replaces the current data.
* Empty states are handled: nothing to undo, no search result, empty table and
  sorting an empty inventory all produce a message or a no-op, never a crash.
* No exception is swallowed silently.

---

## 12. Notes and limitations

* Single-user desktop application: no login, no database, no reports or charts.
* The product ID identifies a product: Update can not change it. If the ID of a
  deleted product is used again by a new product, the delete can no longer be
  undone and Undo reports that.
* Undo goes back one undoable action per click; press it again to go further
  back. Redo is not supported.
* `products.txt` is rewritten on every save, so the order of the lines follows
  the list order (insertion order), not the sorted view on screen.
* The application writes to `data/` relative to the working directory, so the
  folder must be writable (avoid running it from `C:\Program Files`).
* Prices are stored with `Double.toString`, so `2.50` in the sample file becomes
  `2.5` after the first save. The value is unchanged.

---

## 13. What was tested

* Every rule above was exercised with a throwaway test harness (79 checks):
  validation messages, duplicate IDs, stock in/out limits, update, delete,
  totals, low stock, the full undo chain for ADD / UPDATE / STOCK_IN / STOCK_OUT
  / DELETE, both searches, sorting on every field in both directions, saving and
  reloading, corrupted lines being skipped, and undo state surviving a restart.
* The GUI was launched against the sample data: the window opens with the
  products loaded, the table shows the expected rows, and the main window,
  Product dialog, Stock dialog and History dialog all construct and close
  without errors.


