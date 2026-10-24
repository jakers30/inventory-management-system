# Inventory Management System — GUI Specification (Dialogs and Panels)

Companion to `INVENTORY_SYSTEM_SPEC.md`. This file only covers the GUI layout so the main window stays clean.
The GUI classes live in package `inventory.gui` and talk only to `InventoryService`.

---

## GUI (Swing + AWT only)

**Goal: a clean main window that only shows the table and a few controls. Forms and extra views open in `JDialog` windows.**

### 1. Windows

| Window | Class | Contains |
|---|---|---|
| **Main window** | `MainFrame` (`JFrame`) | Search/sort bar, product table, action buttons, status bar, menu bar |
| **Add / Update dialog** | `ProductDialog` (`JDialog`, modal) | Form: ID, Name, Category, Quantity, Price, Reorder Level + Save / Cancel |
| **Stock In / Out dialog** | `StockDialog` (`JDialog`, modal) | Product ID + name (read-only), current quantity (read-only), amount field + Confirm / Cancel |
| **Transaction History dialog** | `HistoryDialog` (`JDialog`, non-modal or modal) | `JTable` of the stack (newest first) + Undo Last / Close |

### 2. Main window layout (`BorderLayout`)

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

### 3. Menu bar (`JMenuBar`, `JMenu`, `JMenuItem` from Swing)

| Menu | Items |
|---|---|
| **File** | Save, Load, Exit |
| **Inventory** | Add Product, Update Product, Delete Product |
| **Stock** | Stock In, Stock Out, Undo Last Action |
| **View** | Transaction History, Show All, Low-Stock Only |

Menu items call the same methods as the buttons (no duplicated logic).

### 4. Components used

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

### 5. Behavior

- **Add:** click Add -> `ProductDialog` opens empty -> Save validates -> on success closes and the table refreshes. On error, show `JOptionPane` and keep the dialog open.
- **Update:** select a row -> click Update -> `ProductDialog` opens filled in with the ID field **disabled** (read-only). If no row is selected, show "Select a product first."
- **Delete:** select a row -> confirm with `JOptionPane.showConfirmDialog`.
- **Stock In / Out:** select a row -> `StockDialog` opens showing the product and its current quantity.
- **History:** opens `HistoryDialog`; the Undo button inside it pops the stack and refreshes both the dialog and the main table.
- **Search:** results replace the table rows; **Show All** restores the full list.
- **Low-stock rows:** use a custom `DefaultTableCellRenderer` (AWT `Color`) for rows where `quantity <= reorderLevel`.
- **Startup:** auto-load products and history. **Close:** auto-save (`WindowListener`). Manual Save/Load is in the File menu.
- Dialogs use `setLocationRelativeTo(parent)` and `setModal(true)` (history may be non-modal) so they open centered over the main window.
- Dialogs only collect and validate input through `InventoryService`. They do not touch the data structures directly.
