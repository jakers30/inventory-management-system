# Inventory System: UI Restyle Plan

Companion to `INVENTORY_SYSTEM_SPEC.md` and `INVENTORY_GUI_SPEC.md`.

**Goal:** restyle the working `MainFrame` to look like the NetBeans design **without changing any logic**.

- The design is the look.
- The existing build is the behavior.
- If this file and `INVENTORY_GUI_SPEC.md` disagree about how the main window *looks*, this file wins. Behavior rules in the GUI spec still apply.

---

## 0. Before starting

- Copy the whole project folder (or `git commit`) so you can roll back.
- **Do not edit:**
  - `inventory.service` (`InventoryService`, `Validator`, `InventoryException`)
  - `inventory.structures` (linked list, hash table, stack)
  - `inventory.algorithm` (linear search, binary search, sort)
  - `inventory.storage` (`FileHandler`)
  - `ProductDialog`, `StockDialog`, `HistoryDialog` (behavior)
- **Only change:** `inventory/gui/MainFrame.java`, plus small styling helpers inside `inventory.gui`.
- Swing and AWT only. No new libraries.

---

## 1. Design-to-code mapping

| Design | Current build (per GUI spec) | What to do |
|---|---|---|
| 4 colored cards | Status line: total value, items, low count | Replace with cards: Total Products, Total Value, Low Stock, Out of Stock |
| Search box + Search button | Search type combo, text box, Search, Show All | Keep all of it, laid out like the design |
| Sort By: order + field + Sort | Sort field combo, Asc/Desc combo, Sort | Same controls, arranged like the design |
| Colored buttons: Read / Delete / Edit / Add | Add / Update / Delete / Stock In / Stock Out | See section 2 |
| Columns: ID, Product, Category, Price, Quantity, Status | ID, Name, Category, Quantity, Price, Reorder Level | Use the design's column order; Status is computed |
| (not in design) | Low-Stock Only, Undo, History, menu bar | Keep them |

---

## 2. Decisions

- **Edit** = the existing Update. It opens `ProductDialog` with the ID field read-only.
- **Read:** shows the details of the selected product (all fields, including Reorder Level and Status). Use a simple `JOptionPane.showMessageDialog`, so no new class is needed. If no row is selected, show "Select a product first."
- **Stock In, Stock Out, Undo:** second row of buttons in the same flat style. They call the same methods as the menu items.
- **Reorder Level:** hidden from the table, still editable in `ProductDialog`.
- **Status column:** computed for display only, never typed or saved.
  - `Out` if quantity is 0
  - `Low` if quantity <= reorder level
  - `OK` otherwise
- **Cards:** read their numbers from the existing service getters. If there is no getter for Out of Stock, count it in `MainFrame` from the array the service already returns. Do not change the service.
- **Menu bar:** unchanged (File, Inventory, Stock, View).

---

## 3. Main window layout (top to bottom, `BorderLayout` + nested `JPanel`s)

```
+--------------------------------------------------------------+
| File | Inventory | Stock | View                 (menu bar)     |
+--------------------------------------------------------------+
| [Total Products] [Total Value] [Low Stock] [Out of Stock]      |  stat cards
+--------------------------------------------------------------+
| Search: [type v] [text........] [Search] [Show All]            |  control panel
| Sort:   [order v] [field v] [Sort]        [ ] Low-Stock Only   |
+--------------------------------------------------------------+
| [Read] [Delete] [Edit] [Add]                                   |  button row 1
| [Stock In] [Stock Out] [Undo]                                  |  button row 2
+--------------------------------------------------------------+
| ID | Product | Category | Price | Quantity | Status           |  table
+--------------------------------------------------------------+
```

1. **Stat cards row** (`GridLayout`, 1 row, 4 columns): rounded panels with pastel backgrounds and a larger colored value label under a small title label.
2. **Control panel:** search and sort on two rows, plus Low-Stock Only.
3. **Button row 1:** Read (orange), Delete (red), Edit (blue), Add (green).
4. **Button row 2:** Stock In, Stock Out, Undo in the same flat style (neutral gray or dark).
5. **Table:** `JScrollPane` > `JTable`, columns as in section 1. Keep the low-stock row tinting renderer and apply it to the new column order.
6. History still opens from the View menu in `HistoryDialog`.

Window: `setMinimumSize` around 900 x 600, `setLocationRelativeTo(null)`.

---

## 4. Colors

| Item | Color |
|---|---|
| Read button (orange) | `#E8772E` |
| Delete button (crimson) | `#E41E4D` |
| Edit button (blue) | `#2D5FE0` |
| Add button (green) | `#10B981` |
| Card backgrounds | pastel green, blue, red, amber |
| Card value text | a darker shade of each card's color |

These are estimates from the screenshot. Check them with the color picker in the NetBeans designer and replace them with the exact values. Keep all colors as constants at the top of `MainFrame` (or in one `Theme` helper) so they are easy to change.

---

## 5. How to do the rounded look (Swing/AWT only)

- **Cards:** small helper class (e.g. `RoundedPanel extends JPanel`). Override `paintComponent`, use `Graphics2D`, turn on antialiasing, fill a `RoundRectangle2D` (radius around 16), `setOpaque(false)`.
- **Buttons:** helper `RoundedButton extends JButton` or a method that styles a button: `setContentAreaFilled(false)`, `setBorderPainted(false)`, `setFocusPainted(false)`, white bold font, custom `paintComponent` with a rounded fill. Darken slightly on hover with a `MouseListener`.
- Put helpers in `inventory.gui`. Keep them under about 40 lines each. This is styling only, not new logic.

---

## 6. Prompt to give Cline

```
Restyle inventory/gui/MainFrame.java to match my design. Do not change any
logic: InventoryService, data structures, search/sort algorithms, undo, the
dialogs' behavior, and file handling stay exactly as they are. Only change
layout and appearance. Read INVENTORY_GUI_SPEC.md and INVENTORY_UI_RESTYLE.md first.

1. Add a row of 4 rounded stat cards at the top (Total Products, Total Value,
   Low Stock, Out of Stock) with pastel backgrounds (green, blue, red, amber)
   and a colored value label. Replace the old status line; refresh the cards
   everywhere the old status line was refreshed.
2. Below the cards, one control panel: search (type combo, text box, Search,
   Show All) and sort (order combo, field combo, Sort), plus Low-Stock Only.
3. Flat, colored, rounded buttons: Add (green #10B981), Edit (blue #2D5FE0,
   same action as Update), Delete (red #E41E4D), Read (orange #E8772E, shows
   the selected product's details in a JOptionPane). Put Stock In, Stock Out,
   and Undo in the same style on a second row.
4. Table columns in this order: ID, Product, Category, Price, Quantity, Status.
   Status is computed for display only: Out (qty 0), Low (qty <= reorder
   level), otherwise OK. Keep the low-stock row tinting.
5. Keep the menu bar as it is. Java Swing/AWT only; no new libraries. Put
   styling helpers (rounded panel/button) in package inventory.gui.
6. Compile, then confirm each button still works.
```

---

## 7. Checklist after Cline finishes

- [ ] Project compiles in NetBeans
- [ ] Cards update after add, edit, delete, stock in/out, undo, and load
- [ ] Add, Edit, Delete, Read all work (and warn when no row is selected)
- [ ] Stock In, Stock Out, Undo still work (including the error for too much stock out)
- [ ] Search, Show All, Sort, Low-Stock Only still work
- [ ] Status column shows Out / Low / OK correctly
- [ ] Low-stock rows are still tinted
- [ ] Save/Load and close/reopen still keep the data
- [ ] Only `MainFrame` and styling helpers changed (check the diff)

---

## Alternative: keep the NetBeans GUI Builder

If the design must stay in the visual designer:

1. Rename every component in the designer (right-click > Change Variable Name), e.g. `btnAdd`, `btnEdit`, `btnDelete`, `btnRead`, `btnSearch`, `btnSort`, `txtSearch`, `cmbSearchType`, `cmbSortOrder`, `cmbSortField`, `tblProducts`.
2. Tell Cline not to edit `initComponents()` or the `.form` file, and to add all logic after `initComponents()`.
3. Move the current `MainFrame` logic into the new frame.

This keeps the exact design but takes more manual work. The designer's generated code also makes the file long and harder to explain in the defense, so use it only if the design must be pixel-exact.
