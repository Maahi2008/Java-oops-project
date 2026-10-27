# OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3
## Core Java Project Presentation — Complete Slide Content & 7-Minute Speaking Script

---

### File Location:
- **PowerPoint Presentation File:** [`OOPS_Project_Presentation.pptx`](file:///c:/Users/arora/OneDrive/Desktop/OOPS%20project/OOPS_Project_Presentation.pptx)
- **Project Folder:** `c:\Users\arora\OneDrive\Desktop\OOPS project`

---

## Slide 0: Title Slide
**Slide Header:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  
**Project Title:** Campus Lost & Found Matching System (MUJ Edition)  
**Presenter:** Maahi Arora  
**Registration Number:** 2502052548  
**Course & Section:** Object Oriented Programming (Core Java) • Section R3  
**Institution:** Department of Computer Science & Engineering, Manipal University Jaipur (MUJ)  
**Date:** October 2026  

> **Speaker Script (15 seconds):**  
> *"Good morning respected evaluators. My name is Maahi Arora, registration number 2502052548 from Section R3. Today I am presenting our Core Java project: the Campus Lost & Found Matching System, an automated, persistent object-oriented application customized for the Manipal University Jaipur campus."*

---

## Slide 01 • PROBLEM: Problem Statement & Use Case
**Timing Budget:** 40 seconds  
**Slide Header:** 01 • PROBLEM  
**Slide Title:** Problem Statement & Realistic Campus Use Case  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Campus Scale:** Manipal University Jaipur spans 122+ acres with multi-storey academic blocks (AB1, AB2, AB3), Lecture Hall Complex (LHC), Central Library, Hostels (B1–B12, G1–G4), and Food Court.
- **The Problem:** Misplaced high-value items (Student IDs, Casio scientific calculators, wallets, laptops) are reported across noisy, unstructured WhatsApp/Telegram groups or physical noticeboards with zero automated matching.
- **Realistic Use Case:** A student forgets a Casio fx-991CW calculator in LHC Audi 2 after an exam. A security guard finds a calculator nearby and logs it. The system automatically detects compatibility, ranks them as **#1 Priority Match**, and enables verified return custody without publicizing private phone numbers.
- **Objective Statement:** *"To build an automated, persistent Core Java application that pairs misplaced campus items using multi-factor heuristic matching and verifies custody using Object-Oriented principles."*

> **Speaker Script (40 seconds):**  
> *"Across a 122-acre campus like MUJ, hundreds of students misplace essential belongings every semester—from ID cards in the Central Library to scientific calculators in LHC lecture halls.  
> Currently, students rely on unstructured WhatsApp groups, leading to spam, missed matches, and fraudulent claims.  
> Our application provides a centralized, automated solution: students and security staff log lost and found items with their university registration numbers. The system immediately calculates multi-factor compatibility and ranks the best match at the very top, ensuring transparent custody and rapid recovery."*

---

## Slide 02 • OOP DESIGN: Object-Oriented Design
**Timing Budget:** 60 seconds  
**Slide Header:** 02 • OOP DESIGN  
**Slide Title:** Object-Oriented Design & 4 Pillars Implementation  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Main Classes & Objects:**
  - `Item`: Conceptual abstract superclass for all trackable campus items.
  - `LostItem` and `FoundItem`: Specialized entity subclasses.
  - `MatchResult`: Encapsulates a candidate pair and its compatibility score.
  - `ItemManager`: Central business controller managing collections and state.
  - `ItemMatcher`: Heuristic comparator executing multi-factor matching algorithms.
  - `SearchService`: Provides multi-attribute query operations.
  - `FileItemRepository`: Manages binary file serialization.
- **Implementation of the 4 OOP Pillars:**
  1. **Abstraction:** `abstract class Item` defines common attributes and abstract method `public abstract String getType();`. Internal serialization and heuristic algorithms are abstracted behind clean public methods.
  2. **Inheritance:** `LostItem extends Item` and `FoundItem extends Item`. 100% reuse of 13 fields via `super(...)` constructors.
  3. **Encapsulation:** All attributes declared `private` or `protected`. State transition method `markClaimed(claimant, date)` enforces atomic status updates (`ACTIVE` $\rightarrow$ `CLAIMED`).
  4. **Polymorphism:** Runtime dynamic method dispatch on `getType()` across mixed `ArrayList<Item>` collections; constructor overloading for binary backward compatibility; method overloading in `SearchService`.

> **Speaker Script (60 seconds):**  
> *"Our architecture strictly embodies all four pillars of Object-Oriented Programming:  
> First, **Abstraction**: We created an abstract class `Item` defining the common blueprint for all items, enforcing specialized behavior through the abstract method `getType()`.  
> Second, **Inheritance**: `LostItem` and `FoundItem` inherit from `Item`, reusing all 13 attributes while specializing their identity.  
> Third, **Encapsulation**: All fields are strictly private with getters and setters. Status transitions are guarded inside `markClaimed()` so records cannot be corrupted with inconsistent audit data.  
> Fourth, **Polymorphism**: In `ItemManager`, a single collection of type `ArrayList<Item>` stores both Lost and Found objects. When iterating, Java uses dynamic method dispatch to call the correct `getType()` at runtime without needing clumsy `instanceof` checks."*

---

## Slide 03 • CORE JAVA: Core Java Concepts Used
**Timing Budget:** 60 seconds  
**Slide Header:** 03 • CORE JAVA  
**Slide Title:** Core Java Concepts Genuinely Implemented  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Collections & Generics:**
  - `ArrayList<Item>` manages active campus records in memory.
  - `ArrayList<MatchResult>` holds compatibility candidate pairs.
  - `HashSet<String>` stores tokenized word sets for Jaccard similarity.
  - `HashMap<String, Integer>` aggregates hotspot analytics (Library, AB1, AB2, LHC).
- **File Handling & Serialization:**
  - Pure Java Object Streams: `ObjectOutputStream` and `ObjectInputStream`.
  - Serialized to binary disk file `items.dat` without external SQL dependencies.
  - Explicit `serialVersionUID` values defined across all serializable models to ensure stream consistency.
- **String Handling & Regular Expressions:**
  - Case normalization with `.toLowerCase().trim()`.
  - Substring checks via `.contains()` and case-insensitive matching with `.equalsIgnoreCase()`.
  - Description tokenization using `.split("[^a-zA-Z0-9]+")`.
- **Exception Handling & Thread Safety:**
  - `try-catch-finally` handling `FileNotFoundException`, `IOException`, and `EOFException`.
  - Thread synchronization on file saves in `FileItemRepository` preventing stream corruption.
  - 16-thread pooled executor (`Executors.newFixedThreadPool(16)`) in `ServerLauncher.java`.

> **Speaker Script (60 seconds):**  
> *"Every Java concept in this project was selected for a specific technical purpose:  
> For collections, we used generic `ArrayList` containers for dynamic inventory management, `HashSet` for word token sets, and `HashMap` for campus location aggregation.  
> For persistence, we implemented pure Java Object Serialization using `ObjectOutputStream` and `ObjectInputStream` writing to `items.dat`. We explicitly configured `serialVersionUID` across all models to ensure backward-compatible deserialization.  
> For string handling, we utilized regex tokenization with `.split()` and case-insensitive normalization for campus location matching.  
> Finally, file writes are synchronized to ensure thread safety during concurrent operations."*

---

## Slide 04 • PROGRAM DESIGN: Program Flow & Implementation
**Timing Budget:** 60 seconds  
**Slide Header:** 04 • PROGRAM DESIGN  
**Slide Title:** Program Flow, Heuristic Algorithm & Key Code  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Program Flow:**
  $$\text{User Submission} \longrightarrow \text{ItemManager.addItem()} \longrightarrow \text{FileItemRepository.saveItems()} \longrightarrow \text{items.dat}$$
  $$\longrightarrow \text{ItemMatcher.findMatches()} \longrightarrow \text{Ranked Preference Queue (\#1 Top Match to Lowest)}$$
- **Multi-Factor Heuristic Compatibility Formula ($0 - 100\%$):**
  - **Name Similarity (20 pts):** Exact match = 20 pts; Substring match = 14 pts.
  - **Category Match (20 pts):** Exact category equality = 20 pts.
  - **Location Match (20 pts):** Building containment (e.g., `"AB2"` matches `"AB2 Room 310"`).
  - **Description Jaccard Word Similarity (25 pts):** $|A \cap B| / |A \cup B| \times 25$.
  - **Date Proximity (15 pts):** Same day = 15; $\le 2$ days = 10; $\le 7$ days = 5.
- **Key Design Decision (Preference-Wise Ranking):**
  - Rather than displaying confusing percentages (like 78.1%), candidate pairs are strictly sorted descending by compatibility score.
  - Results are presented **preference-wise**: the most matched result appears at the very top as **#1 Priority Top Match**, followed by secondary candidates.

```java
public ArrayList<MatchResult> findMatches(Item lost, ArrayList<Item> all) {
    ArrayList<MatchResult> matches = new ArrayList<>();
    for (Item item : all) {
        if (!"FOUND".equalsIgnoreCase(item.getType())) continue;
        if ("CLAIMED".equalsIgnoreCase(item.getStatus())) continue;

        double score = calculateScore(lost, item);
        if (score >= 30.0) { // 30% cutoff threshold
            matches.add(new MatchResult(lost, item, score));
        }
    }
    matches.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
    return matches;
}
```

> **Speaker Script (60 seconds):**  
> *"Here is how our matching engine processes items:  
> When a report is logged, `ItemMatcher` evaluates it against active counterpart items using a multi-factor heuristic formula combining Name, Category, Campus Landmark, Description Jaccard similarity, and Date proximity.  
> As shown in the code snippet, any pair scoring below 30% is discarded as noise, and claimed items are bypassed.  
> Crucially, we made a key design decision: instead of displaying confusing percentage numbers like 78.1%, our system sorts the results descending and presents them **preference-wise**. The single highest compatibility result is ranked as '#1 Priority Top Match' at the very top, making decisions immediate and unambiguous for students."*

---

## Slide 05 • DEMO: Application Demonstration
**Timing Budget:** 100 seconds  
**Slide Header:** 05 • DEMO  
**Slide Title:** Application Demonstration & Core Use Cases  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Feature 1: Lost & Found Reporting with MUJ Validation:**
  - Input: Student logs lost MacBook Air M2 in AB3 Lab 102 with Student Reg No `229301888` and photo upload.
  - Processing: Instantiated as `LostItem`, saved to `items.dat` via binary serialization.
  - Output: Appears in directory with unique ID pill `L104`.
- **Feature 2: Preference-Wise Match Engine:**
  - Input: Evaluates against existing Found reports.
  - Processing: Pairs with Found item `F104` logged by faculty in AB3.
  - Output: Displayed as **#1 Priority Match** with side-by-side comparison and 1-click founder contact copy.
- **Feature 3: Campus Custody & Resolution:**
  - Security Desk verifies student registration number and marks item as `CLAIMED`.
  - Item is automatically archived from active matching and exported to official `MUJ_Custody_Report.csv`.

> **Speaker Script (100 seconds — Live Demo Walkthrough):**  
> *"Now let us demonstrate the application running live on localhost:8080.  
> First, on the Dashboard, you see real-time counters and recent campus notices.  
> Second, under 'Report Lost Item', I enter 'MacBook Air M2', select Category 'Electronics', choose campus location 'AB3', specific spot 'Lab 102', and enter student registration number '229301888'. Upon clicking submit, the item is instantly serialized to disk in `items.dat`.  
> Third, opening the 'Match Engine', notice how the Casio calculator and MacBook are automatically paired and ranked: the most compatible pair is at the very top with a '#1 Priority Top Match' badge.  
> Fourth, under 'Security Desk', campus security officers can inspect all custody items, verify student identity, and mark the item as claimed. Once claimed, the item is archived from active matching, and officers can download an official audit report as a CSV file in one click."*

---

## Slide 06 • TESTING: Testing, Results & Limitations
**Timing Budget:** 40 seconds  
**Slide Header:** 06 • TESTING  
**Slide Title:** Testing, Empirical Results & Boundaries  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Empirical Test Cases:**
  1. **TC1 (Exact Match):** Lost *Black Leather Wallet* in *Library* vs. Found *Black Leather Wallet* $\rightarrow$ **PASS:** Matched on Name, Category, Location, and Jaccard similarity; ranked at the top as #1 Priority.
  2. **TC2 (Negative Filter):** Lost *MacBook Air* in *AB3* vs. Found *Keys* in *AB2* $\rightarrow$ **PASS:** Scored below 30% cutoff; completely excluded from match suggestions.
  3. **TC3 (Persistence & Lifecycle):** Item marked as `CLAIMED` $\rightarrow$ JVM/server restarted $\rightarrow$ loaded from `items.dat` $\rightarrow$ **PASS:** Record persisted across restart with status `CLAIMED` and was properly excluded from active matches.
- **Edge Cases Handled:**
  - Missing `items.dat` handled cleanly by initializing an empty repository.
  - Explicit `serialVersionUID` prevents `InvalidClassException` during deserialization.
- **Current Limitations:**
  - Local binary serialization operates on a single server node without multi-datacenter SQL replication.
  - Designed specifically for the Manipal University Jaipur campus taxonomy.

> **Speaker Script (40 seconds):**  
> *"We validated our implementation across comprehensive test scenarios:  
> In Test Case 1, identical lost and found wallet reports in the Library matched with highest preference.  
> In Test Case 2, unrelated items like laptops and keys scored below the 30% cutoff and were discarded without generating false leads.  
> In Test Case 3, we verified end-to-end persistence: marking an item as claimed, restarting the JVM, and reloading from `items.dat` verified that state and audit timestamps remain 100% intact.  
> As for limitations, the current system uses local file serialization rather than distributed SQL clustering."*

---

## Slide 07 • WRAP-UP: Conclusion & Future Enhancements
**Timing Budget:** 20 seconds  
**Slide Header:** 07 • WRAP-UP  
**Slide Title:** Conclusion & Future Enhancements  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Key Points on Slide:
- **Key Outcomes:**
  - Delivered a robust, production-ready campus lost-and-found portal customized for MUJ.
  - Successfully demonstrated all 4 OOP pillars (Abstraction, Inheritance, Encapsulation, Polymorphism).
  - Built pure Java binary persistence (`items.dat`) with zero external database dependencies.
  - Designed an intuitive preference-wise match hierarchy eliminating confusing percentages.
- **Realistic Future Enhancements:**
  1. RFID / NFC student card tap integration at MUJ academic security desks.
  2. Computer vision embeddings for automated visual image matching.
- **Core Takeaway:**
  *"Well-structured Object-Oriented Programming transforms a disorganized campus nuisance into an automated, accountable, and transparent software solution."*

> **Speaker Script (20 seconds):**  
> *"In conclusion, this project demonstrated how Core Java and OOP principles—abstraction, inheritance, encapsulation, and polymorphism—can turn an everyday campus issue into a fast, persistent, and verifiable application.  
> For future work, we plan to integrate RFID student card scanners at campus gates.  
> Thank you, and we are now ready for your questions."*

---

## Slide 08 • PRESENTATION TIMING: Suggested 7-Minute Flow
**Slide Header:** PRESENTATION TIMING  
**Slide Title:** Suggested 7-Minute Presentation Flow  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

| Slide / Section | Focus & Activity | Allocated Time | Cumulative Time |
| :---: | :--- | :---: | :---: |
| **01** | Problem Statement & MUJ Campus Use Case | 40 seconds | 0:40 |
| **02** | OOP Design & 4 Pillars Implementation | 60 seconds | 1:40 |
| **03** | Core Java Concepts (Collections, Serialization, String Handling) | 60 seconds | 2:40 |
| **04** | Program Flow & Heuristic Matching Formula | 60 seconds | 3:40 |
| **05** | Live Application Demo (Report $\rightarrow$ Preference Match $\rightarrow$ Claim) | 100 seconds | 5:20 |
| **06** | Testing, Empirical Results & Limitations | 40 seconds | 6:00 |
| **07** | Conclusion & Future Enhancements | 20 seconds | 6:20 |
| **Q&A** | Evaluator Questions & Defense | 40 seconds | 7:00 |

---

## Slide 09 • FINAL CHECKLIST: Pre-Evaluation Verification
**Slide Header:** FINAL CHECKLIST  
**Slide Title:** Pre-Evaluation Verification Checklist  
**Footer:** OBJECT ORIENTED PROGRAMMING • CORE JAVA • SECTION R3  

### Technical Readiness:
- [x] Application compiles cleanly: `javac -cp ".;lib/javax.servlet-api-4.0.1.jar" model/*.java service/*.java storage/*.java web/*.java Main.java ServerLauncher.java`
- [x] Localhost web server is operational: `.\run-server.bat` on `http://localhost:8080/`
- [x] Standalone CLI test driver ready as backup: `java -cp . Main`
- [x] Pre-loaded test records present in `items.dat` (AirPods, Wallet, Calculator, MacBook)
- [x] Code snippets formatted and readable in high contrast

### OOP & Viva Defense Readiness:
- [x] Can justify why `Item` is an abstract class instead of an interface (holds concrete member fields).
- [x] Can explain runtime polymorphism via dynamic method dispatch on `item.getType()`.
- [x] Can explain Jaccard word similarity formula ($|A \cap B| / |A \cup B|$) for description matching.
- [x] Can explain the role of `serialVersionUID` in preventing `InvalidClassException`.
- [x] Can explain why preference ranking (#1 Priority Top Match) is superior to raw numeric percentages.
- [x] Can explain thread safety and synchronization in `FileItemRepository`.
