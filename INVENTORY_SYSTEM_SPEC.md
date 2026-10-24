# Inventory Management System — Program Specification

**Course:** IT 201 – Data Structures and Algorithms
**Tech:** Java, Java Swing + AWT only, NetBeans IDE
**Storage:** Plain `.txt` files via `BufferedReader` / `BufferedWriter`
**Rule:** Do not over-engineer. Build only what is listed here.

---

## 1. Goal

A desktop app that monitors products, stock levels, and inventory transactions.
The data structures and algorithms must be **written by us** (no `ArrayList`, `LinkedList`, `HashMap`, `Stack` from `java.util` inside the logic). Plain arrays are allowed.

---

## 2. Data Structures (3 required — each has a real job)

| Structure | Class | Purpose in the app |
|---|---|---|
| **Linked List** | `ProductLinkedList` | Master store of all products. Used for display, add, delete, save, load. |
| **Hash Table** | `ProductHashTable` | Index by Product ID. Gives fast lookup, fast duplicate-ID check, fast update/delete targeting. |
| **Stack** | `TransactionStack` | Transaction history (newest on top, LIFO) and **Undo last action**. |

### 2.1 ProductLinkedList (singly linked list)
- Inner `Node` class: `Product data`, `Node next`.
- Methods: `addLast(Product)`, `remove(String id)`, `get(String id)`, `size()`, `isEmpty()`, `clear()`, `toArray()` (returns `Product[]` for sorting/searching/table display).

### 2.2 ProductHashTable (separate chaining)
- Fixed-size array of buckets (e.g. 31 or 101). Each bucket is a small chain of nodes.
- Key = Product ID (String). Hash = sum/polynomial of characters `% capacity` (write your own, do not use `hashCode()` alone).
- Methods: `put(Product)`, `get(String id)`, `remove(String id)`, `containsKey(String id)`, `clear()`.

### 2.3 TransactionStack (array-based or linked)
- The stack is the **append-only transaction log**: rows are added on top and are never deleted from the middle of the history.
- `pop()` is used only to *walk down* the log while looking for the newest action that can still be undone. Every entry taken off on the way is `push()`ed back in its original order before the method returns, so the stack is left exactly as it was found.
- Methods: `push(Transaction)`, `pop()`, `peek()`, `isEmpty()`, `size()`, `toArray()` (top to bottom, for the history table).
- If array-based: resize (double) when full.

> **Sync rule:** Every add / delete / update goes through `InventoryService`, which updates the linked list **and** the hash table together so they never disagree.

---

## 3. Algorithms (1 search + 1 sort minimum; we do 2 searches + 1 sort)

| Algorithm | Class | Used for |
|---|---|---|
| **Linear Search** | `LinearSearch` | Search by Name or Category (partial, case-insensitive match; can return multiple results). |
| **Binary Search** | `BinarySearch` | Search by Product ID on an array **sorted by ID** (sort first, then search). |
| **Insertion Sort** *(or Quick Sort)* | `SortAlgorithm` | Sort by ID, Name, Quantity, or Price — ascending/descending. Works on `Product[]`. |

Notes:
- Sorting works on the array from `toArray()`; it does not need to modify the linked list. Display the sorted array in the table.
- The hash table is for exact-ID lookups inside the service (add/update/delete). The **Search** feature in the GUI uses linear + binary search so the algorithms are actually shown.

---

## 4. Model Classes (`inventory.model`)

### `Product`
| Field | Type | Notes |
|---|---|---|
| `id` | String | Unique, e.g. `P001` |
| `name` | String | Required |
| `category` | String | Required |
| `quantity` | int | ≥ 0 |
| `price` | double | > 0 |
| `reorderLevel` | int | ≥ 0 — low-stock threshold |

Include constructor, getters/setters, and `toFileString()` / a static `fromFileString(String)` for the txt format.

### `Transaction`
| Field | Type | Notes |
|---|---|---|
| `type` | String | `ADD`, `UPDATE`, `DELETE`, `STOCK_IN`, `STOCK_OUT`, `UNDO` |
| `productId` | String | |
| `productName` | String | |
| `quantityChange` | int | For stock in/out; 0 otherwise. On an `UNDO` row: the net change actually applied (e.g. `-10` when reversing a `STOCK_IN` of `+10`; `0` for the other types) |
| `timestamp` | String | `yyyy-MM-dd HH:mm:ss` |
| `undone` | boolean | `true` once this action has been reversed by an Undo. The row **stays in the log** — it is flagged, never deleted. |
| `undoTargetType` | String | Only on `UNDO` rows: the type of the action that was reversed (empty on every other row) |
| previous values | — | Snapshot of the product **before** the action (name, category, qty, price, reorder level) so Undo can restore it |

An `UNDO` row is itself a transaction: it is pushed on top of the action it reversed, so the log keeps both the event and its reversal.

---

## 5. Required System Functions

### A. Basic functions
1. **Add** a new product.
2. **Display** all products (JTable).
3. **Search** a product — by ID (binary), by Name/Category (linear).
4. **Update** product info (name, category, price, reorder level; ID cannot be changed).
5. **Delete** a product (with confirm dialog).
6. **Sort** — by ID / Name / Quantity / Price, ascending or descending.
7. **Additional functions** (see below).
8. **Save and Load** records.

### B. Additional functions (tied to the data structures)
- **Stock In** — add quantity to a product (logged to the stack).
- **Stock Out** — remove quantity; block if it goes below 0.
- **Undo Last Action** — reverse the newest action that has not already been undone, and **log the undo as its own transaction** instead of erasing the original row:
  - `ADD` → remove the product
  - `DELETE` → re-add the saved product
  - `UPDATE` / `STOCK_IN` / `STOCK_OUT` → restore the previous values
  - After reversing, the original row is marked `undone = true` and stays in the history; a new `UNDO` row is then pushed on top of the stack.
  - The row to reverse is found with `pop()`: entries that are already `undone` (or that are `UNDO` rows) are skipped and `push()`ed back in their original order, so the log is preserved.
  - If every row is already undone, or the stack is empty → "Nothing to undo." (stack left unchanged). Undo-of-undo (redo) is **not** supported — `UNDO` rows are never themselves undoable.
- **View Transaction History** — table showing the stack, newest first, with both the `UNDO` row and the original it reversed visible (the original shown as undone).
- **Low-Stock Highlight** — rows where `quantity <= reorderLevel` shown in a different color (or a "Low Stock" label in a status column).
- **Total Inventory Value** — label showing sum of `quantity × price`.

That is enough. Do not add login, users, reports, charts, or database.

---

## 6. GUI (Swing + AWT only)

**Goal: a clean main window that only shows the table and a few controls. Forms and extra views open in `JDialog` windows.**

### 6.1 Windows

| Window | Class | Contains |
|---|---|---|
| **Main window** | `MainFrame` (`JFrame`) | Search/sort bar, product table, action buttons, status bar, menu bar |
| **Add / Update dialog** | `ProductDialog` (`JDialog`, modal) | Form: ID, Name, Category, Quantity, Price, Reorder Level + Save / Cancel |
| **Stock In / Out dialog** | `StockDialog` (`JDialog`, modal) | Product ID + name (read-only), current quantity (read-only), amount field + Confirm / Cancel |
| **Transaction History dialog** | `HistoryDialog` (`JDialog`, non-modal or modal) | `JTable` of the stack (newest first) + Undo Last / Close |

### 6.2 Main window layout (`BorderLayout`)

```
+------------------------------------------------------------------+
| File | Inventory | Stock | View                    <- JMenuBar     |
+------------------------------------------------------------------+
| NORTH  : JPanel (FlowLayout)                                       |
|   [Search type v] [ search text ] [Search] [Show All]              |
|   [Sort by v] [Asc/Desc v] [Sort]                                  |
+------------------------------------------------------------------+
| CENTER : JScrollPane > JTable (product list)                       |
|          low-stock rows highlighted                                |
+------------------------------------------------------------------+
| SOUTH  : JPanel (BorderLayout)                                     |
|   WEST : [Add] [Update] [Delete] [Stock In] [Stock Out]           |
|   EAST : Total Inventory Value: P0.00  |  Items: 0  |  Low: 0     |
+------------------------------------------------------------------+
```

- The main window has **no input form**. Typing only happens in the search box.
- Only 5 action buttons are shown on the main window. Everything else lives in the menu bar.

### 6.3 Menu bar (`JMenuBar`, `JMenu`, `JMenuItem` from Swing)

| Menu | Items |
|---|---|
| **File** | Save, Load, Exit |
| **Inventory** | Add Product, Update Product, Delete Product |
| **Stock** | Stock In, Stock Out, Undo Last Action |
| **View** | Transaction History, Show All, Low-Stock Only |

Menu items call the same methods as the buttons (no duplicated logic).

### 6.4 Components used

| Component | Use |
|---|---|
| `JFrame` | Main window |
| `JDialog` | Add/Update form, Stock In/Out, Transaction History |
| `JPanel` | North search/sort panel, south button + status panels, and the form panels inside dialogs |
| `JLabel` | Field names, read-only info, status bar text |
| `JTextField` | Form fields, search box, stock amount |
| `JComboBox` | Search type (ID / Name / Category), Sort field, Sort order |
| `JButton` | Add, Update, Delete, Stock In, Stock Out, Search, Show All, Sort, Save, Cancel |
| `JTable` + `DefaultTableModel` (+ `JScrollPane`) | Product list and history list |
| `JMenuBar` / `JMenu` / `JMenuItem` | Secondary actions |
| `JOptionPane` | Error, warning, info, and confirm messages |
| `java.awt` | `BorderLayout`, `FlowLayout`, `GridLayout` / `GridBagLayout`, `Color`, `Font`, `Dimension`, `ActionListener` |

### 6.5 Behavior

- **Add:** click Add -> `ProductDialog` opens empty -> Save validates -> on success closes and the table refreshes. On error, show `JOptionPane` and keep the dialog open.
- **Update:** select a row -> click Update -> `ProductDialog` opens filled in with the ID field **disabled** (read-only). If no row is selected, show "Select a product first."
- **Delete:** select a row -> confirm with `JOptionPane.showConfirmDialog`.
- **Stock In / Out:** select a row -> `StockDialog` opens showing the product and its current quantity.
- **History:** opens `HistoryDialog`; the Undo button inside it reverses the last action, appends an `UNDO` row to the log, and refreshes both the dialog and the main table.
- **Search:** results replace the table rows; **Show All** restores the full list.
- **Low-stock rows:** use a custom `DefaultTableCellRenderer` (AWT `Color`) for rows where `quantity <= reorderLevel`.
- **Startup:** auto-load products and history. **Close:** auto-save (`WindowListener`). Manual Save/Load is in the File menu.
- Dialogs use `setLocationRelativeTo(parent)` and `setModal(true)` (history may be non-modal) so they open centered over the main window.
- Dialogs only collect and validate input through `InventoryService`. They do not touch the data structures directly.

---

## 7. Package Structure (NetBeans Source Packages)

```
inventory
 +- Main.java                      // starts the GUI with SwingUtilities.invokeLater
 |
inventory.model
 +- Product.java
 +- Transaction.java
 |
inventory.structures
 +- ProductLinkedList.java
 +- ProductHashTable.java
 +- TransactionStack.java
 |
inventory.algorithm
 +- LinearSearch.java
 +- BinarySearch.java
 +- SortAlgorithm.java
 |
inventory.service
 +- InventoryService.java          // all business logic; keeps list + hash + stack in sync
 +- Validator.java                 // all input validation
 +- InventoryException.java        // one custom checked exception for rule violations
 |
inventory.storage
 +- FileHandler.java               // BufferedReader / BufferedWriter
 |
inventory.gui
 +- MainFrame.java                 // JFrame: table, search/sort bar, buttons, menu
 +- ProductDialog.java             // JDialog: add / update form
 +- StockDialog.java               // JDialog: stock in / stock out
 +- HistoryDialog.java             // JDialog: transaction history + undo
```

Rules:
- **GUI** only talks to `InventoryService`. No logic in button listeners beyond reading fields and showing messages.
- **Service** talks to structures, algorithms, validator, and storage.
- **Storage** only reads/writes files. No business rules, no GUI calls.
- Pass the same `InventoryService` object into `MainFrame` and each dialog through the constructor (do not create new services in dialogs).

---

## 8. File Storage

Folder: `data/` (created automatically if missing).

| File | Content |
|---|---|
| `data/products.txt` | One product per line |
| `data/transactions.txt` | One transaction per line (oldest first; reload by pushing in order so newest ends on top). Includes the `UNDO` rows and the `undone` flag. |

**Product format** (pipe-delimited, 6 fields):

```
P001|Ballpen Blue|Office Supplies|120|12.50|20
P002|Notebook A5|Office Supplies|45|35.00|10
```

**Transaction format** (pipe-delimited, 12 fields):

```
TYPE|productId|productName|quantityChange|timestamp|prevName|prevCategory|prevQuantity|prevPrice|prevReorderLevel|undone|undoTargetType
```

Example (one action, then the undo that reversed it — note the original is kept and flagged):

```
ADD|P001|Ballpen Blue|0|2026-10-24 14:00:05||||||0|
STOCK_IN|P001|Ballpen Blue|10|2026-10-24 14:01:22|Ballpen Blue|Office Supplies|120|12.50|20|1|
UNDO|P001|Ballpen Blue|-10|2026-10-24 14:01:40||||||0|STOCK_IN
```

- Missing values are written as **empty fields** — split with a limit so empty fields are kept, never dropped.
- The `undone` flag must be saved. Without it, reopening the app would let the same action be undone a second time.
- `products.txt` never stores undone/invalid states; it only holds the current products.

Implementation requirements:
- Write with `BufferedWriter` wrapped around `FileWriter`, using **try-with-resources**.
- Read with `BufferedReader` wrapped around `FileReader`, using **try-with-resources**, `readLine()` until `null`.
- Names/categories must not contain `|` (validator rejects it) so the format never breaks.
- On load: skip blank lines; if a line is corrupted (wrong field count, bad number), **skip it and continue**, then tell the user how many lines were skipped.
- If the file does not exist on first run: create an empty one and start with an empty inventory (no crash).
- Include a **sample `products.txt`** with ~10 products in the submission.

---

## 9. Validation Rules (`Validator`)

| Field | Rule |
|---|---|
| ID | Required, no spaces, no `|`, max ~10 chars, **unique** (check with hash table) |
| Name | Required, not blank, no `|`, max ~50 chars |
| Category | Required, not blank, no `|` |
| Quantity | Required, whole number, `>= 0` |
| Price | Required, number, `> 0` |
| Reorder Level | Required, whole number, `>= 0` |
| Stock In | Whole number `> 0` |
| Stock Out | Whole number `> 0` and `<= current quantity` |
| Search | Search box not empty |
| Update / Delete / Stock | A product must be selected or a valid ID entered, and it must exist |

Always `trim()` input before validating.

---

## 10. Error Handling

- Catch `NumberFormatException` when parsing quantity/price/reorder level → show a friendly message, not a stack trace.
- Validator/service throw `InventoryException` with a clear message (e.g. "Product ID already exists", "Not enough stock") → GUI catches and shows it in `JOptionPane.showMessageDialog(..., JOptionPane.ERROR_MESSAGE)`.
- Catch `IOException` in `FileHandler`; show "Could not save/load data file" with the reason.
- Empty-state handling: Undo with an empty stack → "Nothing to undo." Search with no result → "No matching product found." Sort/display on empty inventory should not crash.
- Confirm dialog (`JOptionPane.showConfirmDialog`) before Delete and before Load (since Load replaces current data).
- Never swallow an exception silently (no empty `catch` blocks).

---

## 11. NetBeans Notes

- Create a **Java Application** project (Java with Ant). Untick nothing special; Main class = `inventory.Main`.
- No external libraries. JDK only.
- Use relative path `data/` — when run in NetBeans, it resolves to the project folder.
- Build → **Clean and Build** produces `dist/<project>.jar`. Keep the `data/` folder beside the JAR when submitting.
- Keep Swing code inside `SwingUtilities.invokeLater(...)`.

---

## 12. Do NOT Do

- No database, no JavaFX, no external GUI/UI libraries (FlatLaf etc.).
- No `java.util` collections (`ArrayList`, `HashMap`, `LinkedList`, `Stack`, `Queue`) in the structures/service logic.
- No `Collections.sort` / `Arrays.sort` — use our own sort.
- No extra features beyond Section 5 (no users, login, reports, charts, barcodes, etc.).
- No interfaces/abstract classes/generics hierarchies unless truly needed — keep classes simple and readable for the defense.

---

## 13. Rubric Checklist (what the instructor grades)

| Criteria | % | How this spec covers it |
|---|---|---|
| Data Structures & Algorithms | 30 | Linked list, hash table, stack + linear search, binary search, sort — all custom |
| Functionality & Requirements | 25 | Full add / display / search / update / delete / sort / save / load + stock in/out, undo |
| Code Quality & Documentation | 15 | Clean packages, comments on each data structure and algorithm, meaningful names |
| Creativity & Innovation | 10 | Undo, low-stock highlight, inventory value |
| Presentation | 10 | Each member explains one part (see split below) |
| Collaboration | 10 | Shared work across 10 members |

### Suggested split (10 members)
1. `Product`, `Transaction` models
2. `ProductLinkedList`
3. `ProductHashTable`
4. `TransactionStack`
5. `LinearSearch` + `BinarySearch`
6. `SortAlgorithm`
7. `Validator` + `InventoryException`
8. `InventoryService`
9. `FileHandler` + sample data
10. `MainFrame` (GUI) + documentation/testing coordination

> The dialogs can be shared out: `ProductDialog` (member 10 or 7), `StockDialog` (member 8), `HistoryDialog` (member 4).

---

## 14. Demo Checklist (for the presentation)

- [ ] Add products, including a duplicate ID → error shown
- [ ] Invalid inputs (blank, letters in quantity, negative price) → error shown
- [ ] Search by ID (binary), by name (linear)
- [ ] Sort by each field, ascending and descending
- [ ] Update and delete (with confirmation)
- [ ] Stock In, Stock Out (over-withdraw → error)
- [ ] Low-stock highlight appears
- [ ] Transaction history shows newest first
- [ ] Undo works for each transaction type, and the history keeps the original row (marked undone) **plus** a new `UNDO` row
- [ ] Close the app, reopen → data is still there
- [ ] Show the source code of each structure and algorithm

## 15. Submission Checklist

- [ ] Complete NetBeans project folder
- [ ] Java source code
- [ ] Runnable JAR + `data/` folder with sample files
- [ ] Documentation PDF (title, intro, objectives, target users, features, DS & algorithms used and how each is implemented, flowchart, GUI screenshots, test results, conclusion)
