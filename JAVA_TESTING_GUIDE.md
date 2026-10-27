# MUJ Campus Lost & Found — Java Evaluation & Testing Guide

> **Course:** Object-Oriented Programming (OOPS)  
> **Institution:** Manipal University Jaipur (MUJ)  
> **Platform Target:** Core Java SE (JDK 17 / 21 / 26)  
> **Evaluation Mode:** Terminal / Command Line (Pure Java verification without browser requirement)

---

## 1. Quick Start for Evaluators

You can test and demonstrate the entire Java backend in **under 2 minutes** using either the automated test suite or the interactive console:

| Test Mode | Quick Command | Description |
|---|---|---|
| **Automated Test Suite (16/16)** | Double-click `test-java.bat`<br>*(or run `java -cp . JavaTestSuite`)* | Runs automated assertions across all 7 evaluation rubrics. Takes 2 seconds. |
| **Interactive Live Console** | Double-click `run-cli.bat`<br>*(or run `java -cp . InteractiveConsole`)* | Interactive 8-option terminal menu to report items, search, test matches, claim items, and verify persistence. |
| **Quick Terminal Overview** | `java -cp . Main` | Runs the baseline console test demonstrating loading from `items.dat`, searching, and matching. |

---

## 2. Environment Verification

To verify that your machine has the proper Java compiler and runtime configured:

```powershell
# Check Java Runtime version (Requires Java 11+)
java -version

# Check Java Compiler version
javac -version
```

### Manual Compilation Command
If compiling manually from the project root:
```powershell
javac -cp ".;lib/javax.servlet-api-4.0.1.jar" model/*.java service/*.java storage/*.java JavaTestSuite.java InteractiveConsole.java Main.java
```

---

## 3. Rubric-by-Rubric Verification Mapping

Here is how each of the professor's 7 evaluation points is implemented, tested, and verified:

### Rubric 1: OOP Pillars
- **What is evaluated:** Classes, Encapsulation, Inheritance, Polymorphism, Abstraction.
- **Where in code:**
  - `model/Item.java`: Abstract superclass defining encapsulated fields (`id`, `name`, `category`, `description`, `location`, `date`, `contact`, `regNo`, `status`, `claimedBy`, `claimedDate`) with private access and public getters/setters.
  - `model/LostItem.java` & `model/FoundItem.java`: Subclasses inheriting from `Item` and providing polymorphic implementations of `getType()`.
  - Dynamic dispatch: Calling `item.getType()` invokes the subclass method at runtime without any `instanceof` cascades.
  - State mutation: `markClaimed(claimedBy, date)` encapsulates updating `status = "CLAIMED"`.
- **Automated Tests:** `Test 01`, `Test 02`, `Test 03` in `JavaTestSuite`.

---

### Rubric 2: ArrayList & Collections
- **What is evaluated:** How records are stored, filtered, and manipulated using Java Collections Framework.
- **Where in code:**
  - `service/ItemManager.java`: Uses `ArrayList<Item>` as the internal repository data structure.
  - Generics: Type-safe `ArrayList<Item>` and `ArrayList<MatchResult>` throughout services.
  - Collections sorting: `Collections.sort(matches, Comparator.comparingDouble(MatchResult::getMatchScore).reversed())` sorts matches in descending preference order.
- **Automated Tests:** `Test 04`, `Test 05` in `JavaTestSuite`.

---

### Rubric 3: String Handling & Search
- **What is evaluated:** Searching item names, categories, and campus locations with case-insensitivity and substring matching.
- **Where in code:**
  - `service/SearchService.java`:
    - Name Search: `item.getName().toLowerCase().contains(name.toLowerCase())`
    - Location Search: `item.getLocation().toLowerCase().contains(location.toLowerCase())`
    - Category Search: `item.getCategory().equalsIgnoreCase(category)`
    - ID Lookup: `item.getId().equalsIgnoreCase(id)`
- **Automated Tests:** `Test 06`, `Test 07`, `Test 08` in `JavaTestSuite`.

---

### Rubric 4: File Handling & Object Serialization
- **What is evaluated:** How `FileItemRepository.java` saves and loads records using `items.dat`.
- **Where in code:**
  - `storage/FileItemRepository.java`:
    - **Save:** Uses `FileOutputStream` wrapped in `ObjectOutputStream` to serialize `ArrayList<Item>`.
    - **Load:** Uses `FileInputStream` wrapped in `ObjectInputStream` to deserialize the object graph.
    - Explicit `serialVersionUID` values on `Item` (`1L`), `LostItem` (`-8395453581734716096L`), and `FoundItem` (`3849222380800978291L`) prevent binary serialization mismatches across runs.
- **Automated Tests:** `Test 09`, `Test 10` in `JavaTestSuite`.

---

### Rubric 5: Intelligent Matching Algorithm & Preference Ranking
- **What is evaluated:** Multi-factor compatibility comparison and preference ranking (top match to bottom).
- **Where in code:**
  - `service/ItemMatcher.java`:
    1. **Exclusion Gate:** Claimed items (`isClaimed() == true`) and identical report types (lost vs lost) are rejected immediately.
    2. **Multi-Factor Heuristic Calculation:**
       - Category Match: $+35\%$
       - Location Token Overlap (Jaccard coefficient): up to $+25\%$
       - Name & Description Keyword Overlap (Tokenization + stop-word removal): up to $+30\%$
       - Date Proximity ($\le 7$ days): up to $+10\%$
    3. **Threshold Filter:** Only pairs scoring $\ge 30\%$ pass.
    4. **Preference-Wise Ranking:** Results sorted in descending order of compatibility score so the best match is always displayed first (`Rank #1`).
- **Automated Tests:** `Test 11`, `Test 12` in `JavaTestSuite`.

---

### Rubric 6: Lifecycle State Transitions (Active -> Claimed)
- **What is evaluated:** Transitioning an item to claimed status and verifying it is archived and excluded from the matching queue.
- **Where in code:**
  - `item.markClaimed(claimantInfo, date)`
  - `ItemMatcher.java`: `if (candidate.isClaimed()) continue;`
- **Automated Tests:** `Test 13`, `Test 14` in `JavaTestSuite`.

---

### Rubric 7: Edge Cases & Exception Safety
- **What is evaluated:** Non-existent file recovery, empty queries, invalid inputs.
- **Where in code:**
  - `FileItemRepository.java`: Safely creates empty list if `items.dat` does not exist (`!file.exists() || file.length() == 0`).
  - `SearchService.java`: Gracefully handles blank/null strings without `NullPointerException`.
- **Automated Tests:** `Test 15`, `Test 16` in `JavaTestSuite`.

---

## 4. Live Interactive Demonstration Walkthrough (For Viva Examiner)

Follow this 5-minute step-by-step walkthrough in front of your examiner:

### Step 1: Launch Interactive Console
```powershell
java -cp . InteractiveConsole
```

### Step 2: Show Current Records (Polymorphism)
- Select option `[3] View All Campus Records`.
- Point out how both `LOST` and `FOUND` items are stored together polymorphically in `ArrayList<Item>`, displaying their specific registration numbers and locations (Library, AB2, LHC, AB3).

### Step 3: Add a New Lost Item
- Select option `[1] Report a Lost Item`.
- Input:
  - Name: `Boat Airdopes 141`
  - Category: `Electronics`
  - Location: `Food Court`
  - Reg No: `229301045`
  - Contact: `student@muj.manipal.edu`
  - Description: `Black charging case with scratch on lid`
- Notice: Console confirms item is created with an ID like `L106` and immediately persisted to `items.dat`.

### Step 4: Add a Matching Found Item
- Select option `[2] Report a Found Item`.
- Input:
  - Name: `Boat Wireless Earbuds`
  - Category: `Electronics`
  - Location: `Food Court`
  - Reg No: `MUJ-SEC-05`
  - Contact: `security.fc@muj.manipal.edu`
  - Description: `Black Boat earbuds in case found near table 4`
- Notice: Console confirms item is created with an ID like `F105`.

### Step 5: Run Preference-Wise Match Engine
- Select option `[5] Run Intelligent Match Engine`.
- The engine will compare all pairs and display:
  ```text
  =======================================================
   Matches for Lost Item [L106] Boat Airdopes 141
  =======================================================
   >> PREFERENCE RANK #1 (Highest to Lowest Compatibility):
      Found Item: [F105] Boat Wireless Earbuds
      Category  : Electronics
      Location  : Food Court
      Finder ID : MUJ-SEC-05
  ```
- Explain to the examiner: *"We rank candidate items preference-wise by heuristic compatibility so the student immediately sees the most probable match first."*

### Step 6: Test Persistence Across Restarts
1. Select option `[8] Exit`.
2. Re-launch the console:
   ```powershell
   java -cp . InteractiveConsole
   ```
3. Select option `[3] View All Campus Records`.
4. Point out to the examiner: *"Notice that L106 Boat Airdopes and F105 Boat Wireless Earbuds are still here! They were deserialized via `ObjectInputStream` from `items.dat`."*

### Step 7: Claim the Item and Verify Exclusion
1. Select option `[6] Verify & Mark Item as Claimed`.
2. Enter ID: `L106` (or `F105`).
3. Enter Claimant Reg No: `229301045 (Verified via ID Card)`.
4. Console confirms status changed to `CLAIMED`.
5. Select option `[5] Run Intelligent Match Engine`.
6. Show that the claimed item is **no longer in the active matching queue**.

---

## 5. Summary of Java Files in Project

| Directory / File | Role in Project | Key OOP Concept |
|---|---|---|
| `model/Item.java` | Base item entity | Abstract Class, Encapsulation, Serializable |
| `model/LostItem.java` | Lost item notice | Inheritance, Subclass polymorphism |
| `model/FoundItem.java` | Found item notice | Inheritance, Subclass polymorphism |
| `model/MatchResult.java` | Match pair container | Encapsulation, Heuristic score tracking |
| `service/ItemManager.java` | Core business logic | Collections (`ArrayList<Item>`), Aggregation |
| `service/ItemMatcher.java` | Intelligent matching algorithm | Tokenization, Heuristic calculation, Sorting |
| `service/SearchService.java` | Query execution service | String processing, Case-insensitivity |
| `storage/FileItemRepository.java` | Persistent storage engine | Object Serialization (`ObjectOutputStream`/`InputStream`) |
| `JavaTestSuite.java` | Automated test suite (16 tests) | Automated verification across all 7 rubrics |
| `InteractiveConsole.java` | Terminal test driver | Interactive CLI menu for viva demonstrations |
| `Main.java` | Baseline console app | Headless verification |
| `ServerLauncher.java` | Embedded HTTP microserver | Full-stack connector serving REST API & static web |
