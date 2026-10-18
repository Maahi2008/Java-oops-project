/**
 * Manipal University Jaipur (MUJ) Campus Lost & Found Matching System
 * Frontend Controller with Reg No verification & campus locations (Library, AB1, AB2, AB3, LHC).
 */

// Application State
const state = {
    currentView: 'dashboard',
    items: [],
    matches: [],
    filters: {
        query: '',
        category: '',
        location: '',
        type: ''
    },
    isBackendOnline: false,
    selectedLostIdForMatch: 'ALL'
};

// Initial sample items tailored to Manipal University Jaipur
const INITIAL_SAMPLE_ITEMS = [
    {
        id: "L101",
        name: "Black Leather Wallet",
        category: "Wallets & Bags",
        description: "Black leather wallet containing MUJ Student ID and transit card",
        location: "Library (2nd Floor Reading Hall)",
        date: "2026-10-16",
        reporterName: "Aryan Sharma",
        regNo: "229301045",
        contact: "aryan.229301045@muj.manipal.edu",
        type: "LOST"
    },
    {
        id: "F101",
        name: "Black Leather Wallet",
        category: "Wallets & Bags",
        description: "Black wallet found on 2nd floor desk near reference section",
        location: "Library",
        date: "2026-10-16",
        reporterName: "Kavita Rao",
        regNo: "229301128",
        contact: "kavita.229301128@muj.manipal.edu",
        type: "FOUND"
    },
    {
        id: "L102",
        name: "Apple AirPods Pro 2",
        category: "Electronics",
        description: "White AirPods Pro case with orange silicone cover and keychain",
        location: "AB2 (Room 310)",
        date: "2026-10-18",
        reporterName: "Rohan Gupta",
        regNo: "239302199",
        contact: "rohan.239302199@muj.manipal.edu",
        type: "LOST"
    },
    {
        id: "F102",
        name: "AirPods Pro",
        category: "Electronics",
        description: "Apple AirPods charging case with orange cover found in AB2 lecture room",
        location: "AB2",
        date: "2026-10-18",
        reporterName: "MUJ Security Desk",
        regNo: "MUJ-SEC-03",
        contact: "security.ab2@jaipur.manipal.edu",
        type: "FOUND"
    }
];

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    // Set default date in forms to today
    const today = new Date().toISOString().split('T')[0];
    const lostDateInput = document.getElementById('lost-date');
    const foundDateInput = document.getElementById('found-date');
    if (lostDateInput) lostDateInput.value = today;
    if (foundDateInput) foundDateInput.value = today;

    // Load data
    loadAllData();
});

/**
 * Loads data from Java Servlet Backend if available, otherwise initializes local sample state.
 */
async function loadAllData() {
    try {
        const response = await fetch('api/items', { method: 'GET' });
        if (response.ok) {
            const data = await response.json();
            if (data && data.length > 0) {
                state.items = data;
            } else {
                state.items = [...INITIAL_SAMPLE_ITEMS];
            }
            state.isBackendOnline = true;
            await loadBackendMatches();
        } else {
            throw new Error("Backend not responding");
        }
    } catch (err) {
        state.isBackendOnline = false;
        if (!state.items || state.items.length === 0) {
            state.items = [...INITIAL_SAMPLE_ITEMS];
        }
        recalculateLocalMatches();
    }

    updateDashboardStats();
    renderBrowseItems();
    renderMatches();
    populateMatchLostSelector();
}

/**
 * Fetch matches from Java Servlet /api/matches
 */
async function loadBackendMatches() {
    try {
        const res = await fetch('api/matches');
        if (res.ok) {
            const serverMatches = await res.json();
            state.matches = serverMatches;
        } else {
            recalculateLocalMatches();
        }
    } catch (e) {
        recalculateLocalMatches();
    }
}

/**
 * Local implementation of ItemMatcher algorithm identical to Java ItemMatcher.java
 */
function recalculateLocalMatches() {
    const results = [];
    const lostItems = state.items.filter(item => item.type === 'LOST');
    const foundItems = state.items.filter(item => item.type === 'FOUND');

    for (const lost of lostItems) {
        for (const found of foundItems) {
            const score = calculateMatchScore(lost, found);
            if (score >= 30) {
                results.push({
                    lostItem: lost,
                    foundItem: found,
                    score: Math.round(score * 10) / 10
                });
            }
        }
    }

    results.sort((a, b) => b.score - a.score);
    state.matches = results;
}

/**
 * Replicates the exact ItemMatcher.java algorithm
 */
function calculateMatchScore(lost, found) {
    let score = 0;

    // 1. Name Similarity (* 20)
    score += nameSimilarity(lost.name, found.name) * 20;

    // 2. Category Match (+20)
    if (lost.category && found.category && lost.category.toLowerCase() === found.category.toLowerCase()) {
        score += 20;
    }

    // 3. Location Match (+20) (Checks if either location contains the other, e.g. "AB2" and "AB2 Room 310")
    if (lost.location && found.location) {
        const l1 = lost.location.toLowerCase();
        const l2 = found.location.toLowerCase();
        if (l1 === l2 || l1.includes(l2) || l2.includes(l1)) {
            score += 20;
        }
    }

    // 4. Description Jaccard Similarity (* 25)
    score += descriptionSimilarity(lost.description || '', found.description || '') * 25;

    // 5. Date Proximity (+15, +10, or +5)
    if (lost.date && found.date) {
        const d1 = new Date(lost.date);
        const d2 = new Date(found.date);
        const diffDays = Math.abs(Math.round((d1 - d2) / (1000 * 60 * 60 * 24)));

        if (diffDays === 0) {
            score += 15;
        } else if (diffDays <= 2) {
            score += 10;
        } else if (diffDays <= 7) {
            score += 5;
        }
    }

    return score;
}

function nameSimilarity(first, second) {
    const a = (first || '').toLowerCase().trim();
    const b = (second || '').toLowerCase().trim();
    if (a === b) return 1.0;
    if (a.includes(b) || b.includes(a)) return 0.7;
    return 0.0;
}

function descriptionSimilarity(first, second) {
    const wordsA = getWords(first);
    const wordsB = getWords(second);
    if (wordsA.size === 0 || wordsB.size === 0) return 0.0;

    let intersectionSize = 0;
    for (const w of wordsA) {
        if (wordsB.has(w)) intersectionSize++;
    }

    const unionSize = new Set([...wordsA, ...wordsB]).size;
    return intersectionSize / unionSize;
}

function getWords(text) {
    const words = new Set();
    const tokens = text.toLowerCase().split(/[^a-z0-9]+/);
    for (const token of tokens) {
        if (token.trim().length > 0) {
            words.add(token.trim());
        }
    }
    return words;
}

/**
 * View Navigation System
 */
function navigateTo(viewId) {
    document.querySelectorAll('.view-section').forEach(el => {
        el.classList.remove('active');
    });

    document.querySelectorAll('.nav-link').forEach(el => {
        el.classList.remove('active');
    });

    const target = document.getElementById(`${viewId}-view`);
    if (target) {
        target.classList.add('active');
        state.currentView = viewId;
    }

    const navBtn = document.querySelector(`.nav-link[data-view="${viewId}"]`);
    if (navBtn) {
        navBtn.classList.add('active');
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });

    if (viewId === 'dashboard') {
        updateDashboardStats();
    } else if (viewId === 'browse') {
        renderBrowseItems();
    } else if (viewId === 'matches') {
        renderMatches();
    }
}

/**
 * Update Dashboard Counters & Mini-Lists
 */
function updateDashboardStats() {
    const lostCount = state.items.filter(i => i.type === 'LOST').length;
    const foundCount = state.items.filter(i => i.type === 'FOUND').length;
    const matchCount = state.matches.length;
    const totalCount = state.items.length;

    const lostEl = document.getElementById('stat-lost-count');
    const foundEl = document.getElementById('stat-found-count');
    const matchEl = document.getElementById('stat-matches-count');
    const totalEl = document.getElementById('status-total-count');
    const navMatchEl = document.getElementById('nav-match-count');

    if (lostEl) lostEl.textContent = lostCount;
    if (foundEl) foundEl.textContent = foundCount;
    if (matchEl) matchEl.textContent = matchCount;
    if (totalEl) totalEl.textContent = totalCount;
    if (navMatchEl) navMatchEl.textContent = matchCount;

    // Render top matches in dashboard
    const topMatchesContainer = document.getElementById('top-matches-container');
    if (topMatchesContainer) {
        if (state.matches.length === 0) {
            topMatchesContainer.innerHTML = `
                <div class="empty-state" style="padding: 2rem 1rem;">
                    <p>No matches detected yet. Report items to start automated pairing.</p>
                </div>
            `;
        } else {
            const topSlice = state.matches.slice(0, 3);
            topMatchesContainer.innerHTML = topSlice.map(m => `
                <div class="match-mini-card" onclick="navigateTo('matches')">
                    <div class="match-mini-info">
                        <span class="match-mini-title">${escapeHtml(m.lostItem.name)} &harr; ${escapeHtml(m.foundItem.name)}</span>
                        <span class="match-mini-details">${escapeHtml(m.foundItem.location)} &bull; ${escapeHtml(m.foundItem.reporterName)} (${escapeHtml(m.foundItem.regNo || 'Staff')})</span>
                    </div>
                    <div class="match-score-pill">${m.score.toFixed(1)}%</div>
                </div>
            `).join('');
        }
    }

    // Render recent items feed in dashboard
    const recentItemsContainer = document.getElementById('recent-items-container');
    if (recentItemsContainer) {
        const recent = [...state.items].reverse().slice(0, 4);
        recentItemsContainer.innerHTML = recent.map(item => `
            <div class="recent-mini-card" onclick="openItemModal('${item.id}')">
                <span class="badge ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}">${item.type}</span>
                <div class="recent-mini-details">
                    <div class="recent-mini-name">${escapeHtml(item.name)}</div>
                    <div class="recent-mini-meta">${escapeHtml(item.location)} &bull; Reg: ${escapeHtml(item.regNo || 'N/A')}</div>
                </div>
                <span class="item-id-pill">${item.id}</span>
            </div>
        `).join('');
    }
}

function filterBrowseByType(type) {
    setTypeFilter(type);
    navigateTo('browse');
}

function setTypeFilter(type) {
    state.filters.type = type;
    document.querySelectorAll('.pill-tab').forEach(tab => {
        if (tab.getAttribute('data-type') === type) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    });
    handleSearchChange();
}

/**
 * Search & Filter Handling
 */
function handleSearchChange() {
    const input = document.getElementById('search-input');
    const categorySelect = document.getElementById('category-filter');
    const locationSelect = document.getElementById('location-filter');
    const clearBtn = document.getElementById('search-clear-btn');

    state.filters.query = input ? input.value.trim().toLowerCase() : '';
    state.filters.category = categorySelect ? categorySelect.value : '';
    state.filters.location = locationSelect ? locationSelect.value : '';

    if (clearBtn) {
        clearBtn.style.display = state.filters.query ? 'block' : 'none';
    }

    renderBrowseItems();
}

function clearSearchInput() {
    const input = document.getElementById('search-input');
    if (input) input.value = '';
    handleSearchChange();
}

function resetSearchFilters() {
    const input = document.getElementById('search-input');
    const categorySelect = document.getElementById('category-filter');
    const locationSelect = document.getElementById('location-filter');

    if (input) input.value = '';
    if (categorySelect) categorySelect.value = '';
    if (locationSelect) locationSelect.value = '';

    setTypeFilter('');
    handleSearchChange();
    showToast('Filters reset to default MUJ campus directory.');
}

function handleLocationSelectChange(type) {
    // Optional hook for location select change
}

/**
 * Render Browse Items Grid
 */
function renderBrowseItems() {
    const grid = document.getElementById('items-grid');
    const emptyPlaceholder = document.getElementById('no-items-placeholder');
    const countText = document.getElementById('results-count-text');

    if (!grid) return;

    let filtered = state.items.filter(item => {
        if (state.filters.query) {
            const q = state.filters.query;
            const matchName = item.name.toLowerCase().includes(q);
            const matchDesc = item.description && item.description.toLowerCase().includes(q);
            const matchReg = item.regNo && item.regNo.toLowerCase().includes(q);
            if (!matchName && !matchDesc && !matchReg) return false;
        }
        if (state.filters.category && item.category !== state.filters.category) {
            return false;
        }
        if (state.filters.location && !item.location.toLowerCase().includes(state.filters.location.toLowerCase())) {
            return false;
        }
        if (state.filters.type && item.type !== state.filters.type) {
            return false;
        }
        return true;
    });

    if (countText) {
        countText.textContent = `Showing ${filtered.length} of ${state.items.length} items`;
    }

    if (filtered.length === 0) {
        grid.innerHTML = '';
        if (emptyPlaceholder) emptyPlaceholder.style.display = 'flex';
        return;
    }

    if (emptyPlaceholder) emptyPlaceholder.style.display = 'none';

    grid.innerHTML = filtered.map(item => `
        <div class="item-card" onclick="openItemModal('${item.id}')">
            <div>
                <div class="item-card-header">
                    <span class="badge ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}">${item.type}</span>
                    <span class="item-id-pill">${item.id}</span>
                </div>
                <h3 class="item-title">${escapeHtml(item.name)}</h3>
                <span class="item-category-tag">${escapeHtml(item.category)}</span>
                <p class="item-desc">${escapeHtml(item.description)}</p>
            </div>

            <div>
                <div class="item-meta-list">
                    <div class="item-meta-row">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                        <strong>${escapeHtml(item.location)}</strong>
                    </div>
                    <div class="item-meta-row">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                        <span>${item.date}</span>
                    </div>
                    ${item.regNo ? `
                    <div class="item-meta-row" style="color: var(--orange-primary); font-weight: 700;">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                        <span>Reg No: ${escapeHtml(item.regNo)}</span>
                    </div>` : ''}
                </div>

                <div class="item-footer-actions">
                    <div>
                        <span class="reporter-label">${item.type === 'LOST' ? 'Student:' : 'Finder:'}</span>
                        <span class="reporter-name"> ${escapeHtml(item.reporterName)}</span>
                    </div>
                    <button class="btn btn-outline" style="padding: 0.35rem 0.75rem; font-size: 0.78rem;">Details</button>
                </div>
            </div>
        </div>
    `).join('');
}

/**
 * Handle Report Lost Item Form Submission
 */
async function handleReportLost(e) {
    e.preventDefault();

    const name = document.getElementById('lost-name').value.trim();
    const category = document.getElementById('lost-category').value;
    const locSelect = document.getElementById('lost-location-select').value;
    const specificSpot = document.getElementById('lost-specific-spot').value.trim();
    const location = specificSpot ? `${locSelect} (${specificSpot})` : locSelect;

    const date = document.getElementById('lost-date').value;
    const reporterName = document.getElementById('lost-reporter').value.trim();
    const regNo = document.getElementById('lost-regno').value.trim();
    const contact = document.getElementById('lost-contact').value.trim();
    const description = document.getElementById('lost-description').value.trim();

    // Generate Lost ID
    const lostCount = state.items.filter(i => i.type === 'LOST').length + 1;
    const id = `L${100 + lostCount}`;

    const newItem = {
        id,
        name,
        category,
        location,
        date,
        reporterName,
        regNo,
        contact,
        description,
        type: 'LOST'
    };

    if (state.isBackendOnline) {
        try {
            const res = await fetch('api/items/lost', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newItem)
            });
            if (res.ok) {
                const saved = await res.json();
                state.items.push(saved);
                await loadBackendMatches();
            } else {
                state.items.push(newItem);
                recalculateLocalMatches();
            }
        } catch (err) {
            state.items.push(newItem);
            recalculateLocalMatches();
        }
    } else {
        state.items.push(newItem);
        recalculateLocalMatches();
    }

    updateDashboardStats();
    populateMatchLostSelector();
    showToast(`Lost notice for "${name}" registered for Reg No: ${regNo}! (ID: ${id})`);
    resetForm('lost-form');
    navigateTo('dashboard');
}

/**
 * Handle Report Found Item Form Submission
 */
async function handleReportFound(e) {
    e.preventDefault();

    const name = document.getElementById('found-name').value.trim();
    const category = document.getElementById('found-category').value;
    const locSelect = document.getElementById('found-location-select').value;
    const specificSpot = document.getElementById('found-specific-spot').value.trim();
    const location = specificSpot ? `${locSelect} (${specificSpot})` : locSelect;

    const date = document.getElementById('found-date').value;
    const reporterName = document.getElementById('found-reporter').value.trim();
    const regNo = document.getElementById('found-regno').value.trim();
    const contact = document.getElementById('found-contact').value.trim();
    const description = document.getElementById('found-description').value.trim();

    // Generate Found ID
    const foundCount = state.items.filter(i => i.type === 'FOUND').length + 1;
    const id = `F${100 + foundCount}`;

    const newItem = {
        id,
        name,
        category,
        location,
        date,
        reporterName,
        regNo,
        contact,
        description,
        type: 'FOUND'
    };

    if (state.isBackendOnline) {
        try {
            const res = await fetch('api/items/found', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newItem)
            });
            if (res.ok) {
                const saved = await res.json();
                state.items.push(saved);
                await loadBackendMatches();
            } else {
                state.items.push(newItem);
                recalculateLocalMatches();
            }
        } catch (err) {
            state.items.push(newItem);
            recalculateLocalMatches();
        }
    } else {
        state.items.push(newItem);
        recalculateLocalMatches();
    }

    updateDashboardStats();
    populateMatchLostSelector();
    showToast(`Found item "${name}" at ${locSelect} logged successfully! (ID: ${id})`);
    resetForm('found-form');
    navigateTo('dashboard');
}

function resetForm(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.reset();
        const today = new Date().toISOString().split('T')[0];
        const dateInput = form.querySelector('input[type="date"]');
        if (dateInput) dateInput.value = today;
    }
}

function populateMatchLostSelector() {
    const select = document.getElementById('match-lost-selector');
    if (!select) return;

    const lostItems = state.items.filter(i => i.type === 'LOST');
    const options = [
        `<option value="ALL">All Lost Items with Matches (${state.matches.length})</option>`
    ];

    for (const lost of lostItems) {
        const count = state.matches.filter(m => m.lostItem.id === lost.id).length;
        options.push(`<option value="${lost.id}">${lost.id} - ${escapeHtml(lost.name)} [Reg: ${escapeHtml(lost.regNo || 'N/A')}] (${count} matches)</option>`);
    }

    select.innerHTML = options.join('');
    select.value = state.selectedLostIdForMatch;
}

function filterMatchesByLostItem() {
    const select = document.getElementById('match-lost-selector');
    if (select) {
        state.selectedLostIdForMatch = select.value;
        renderMatches();
    }
}

function loadMatches() {
    if (state.isBackendOnline) {
        loadBackendMatches().then(() => {
            renderMatches();
            showToast('MUJ match engine calculations refreshed.');
        });
    } else {
        recalculateLocalMatches();
        renderMatches();
        showToast('Local match evaluation refreshed.');
    }
}

/**
 * Render Match Results Cards
 */
function renderMatches() {
    const container = document.getElementById('matches-container');
    const emptyPlaceholder = document.getElementById('no-matches-placeholder');
    const summaryLabel = document.getElementById('match-summary-label');

    if (!container) return;

    let displayMatches = state.matches;
    if (state.selectedLostIdForMatch && state.selectedLostIdForMatch !== 'ALL') {
        displayMatches = displayMatches.filter(m => m.lostItem.id === state.selectedLostIdForMatch);
    }

    if (summaryLabel) {
        summaryLabel.textContent = `Showing ${displayMatches.length} matching pair(s)`;
    }

    if (displayMatches.length === 0) {
        container.innerHTML = '';
        if (emptyPlaceholder) emptyPlaceholder.style.display = 'flex';
        return;
    }

    if (emptyPlaceholder) emptyPlaceholder.style.display = 'none';

    container.innerHTML = displayMatches.map(m => {
        const confidenceLabel = m.score >= 80 ? 'High Confidence' : (m.score >= 50 ? 'Moderate Match' : 'Potential Lead');
        return `
            <div class="match-card">
                <div class="match-card-header">
                    <div class="match-header-left">
                        <div class="match-score-badge">
                            ${m.score.toFixed(1)}%
                            <small>Score</small>
                        </div>
                        <div>
                            <div class="match-headline">${escapeHtml(m.lostItem.name)} &harr; ${escapeHtml(m.foundItem.name)}</div>
                            <span class="match-confidence-tag">${confidenceLabel}</span>
                        </div>
                    </div>
                    <div class="item-id-pill">Lost: ${m.lostItem.id} | Found: ${m.foundItem.id}</div>
                </div>

                <div class="match-card-body">
                    <!-- Lost Item Column -->
                    <div class="item-compare-box">
                        <div class="compare-box-header">
                            <span class="badge badge-lost">Reported Lost</span>
                            <span class="item-id-pill">${m.lostItem.id}</span>
                        </div>
                        <h4 class="compare-box-title">${escapeHtml(m.lostItem.name)}</h4>
                        <div class="compare-meta-grid">
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">MUJ Location</span>
                                <span class="compare-meta-value">${escapeHtml(m.lostItem.location)}</span>
                            </div>
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">Date Lost</span>
                                <span class="compare-meta-value">${m.lostItem.date}</span>
                            </div>
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">Student Name</span>
                                <span class="compare-meta-value">${escapeHtml(m.lostItem.reporterName)}</span>
                            </div>
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">Reg No.</span>
                                <span class="compare-meta-value" style="color: var(--orange-primary);">${escapeHtml(m.lostItem.regNo || 'N/A')}</span>
                            </div>
                        </div>
                        <div class="compare-desc">
                            <strong>Description:</strong> ${escapeHtml(m.lostItem.description)}
                        </div>
                    </div>

                    <!-- Found Item Column -->
                    <div class="item-compare-box">
                        <div class="compare-box-header">
                            <span class="badge badge-found">Reported Found</span>
                            <span class="item-id-pill">${m.foundItem.id}</span>
                        </div>
                        <h4 class="compare-box-title">${escapeHtml(m.foundItem.name)}</h4>
                        <div class="compare-meta-grid">
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">MUJ Location</span>
                                <span class="compare-meta-value">${escapeHtml(m.foundItem.location)}</span>
                            </div>
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">Date Found</span>
                                <span class="compare-meta-value">${m.foundItem.date}</span>
                            </div>
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">Finder Name</span>
                                <span class="compare-meta-value">${escapeHtml(m.foundItem.reporterName)}</span>
                            </div>
                            <div class="compare-meta-item">
                                <span class="compare-meta-label">Finder ID / Reg</span>
                                <span class="compare-meta-value" style="color: var(--navy-primary);">${escapeHtml(m.foundItem.regNo || 'Staff')}</span>
                            </div>
                        </div>
                        <div class="compare-desc">
                            <strong>Description:</strong> ${escapeHtml(m.foundItem.description)}
                        </div>
                    </div>
                </div>

                <!-- Found Reporter's Contact Banner -->
                <div class="match-contact-banner">
                    <div class="contact-founder-info">
                        <div class="contact-founder-avatar">
                            ${(m.foundItem.reporterName || 'F').charAt(0).toUpperCase()}
                        </div>
                        <div>
                            <div class="founder-title">Found Reporter Contact / Custody Details</div>
                            <div class="founder-name">${escapeHtml(m.foundItem.reporterName)} (Reg/ID: ${escapeHtml(m.foundItem.regNo || 'Staff')})</div>
                            <div class="founder-contact-detail">${escapeHtml(m.foundItem.contact)}</div>
                        </div>
                    </div>
                    <div>
                        <button class="btn btn-orange" onclick="copyContact('${escapeHtml(m.foundItem.contact)}')">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                            <span>Copy Contact</span>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function copyContact(contact) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(contact).then(() => {
            showToast(`Contact info copied: ${contact}`);
        });
    } else {
        prompt('Founder Contact Info:', contact);
    }
}

/**
 * Modal Inspection
 */
function openItemModal(itemId) {
    const item = state.items.find(i => i.id === itemId);
    if (!item) return;

    const modal = document.getElementById('item-modal');
    const badge = document.getElementById('modal-type-badge');
    const name = document.getElementById('modal-item-name');
    const id = document.getElementById('modal-item-id');
    const cat = document.getElementById('modal-item-category');
    const loc = document.getElementById('modal-item-location');
    const date = document.getElementById('modal-item-date');
    const regno = document.getElementById('modal-item-regno');
    const desc = document.getElementById('modal-item-description');
    const rep = document.getElementById('modal-item-reporter');
    const contact = document.getElementById('modal-item-contact');

    badge.className = `badge ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}`;
    badge.textContent = item.type;
    name.textContent = item.name;
    id.textContent = item.id;
    cat.textContent = item.category;
    loc.textContent = item.location;
    date.textContent = item.date;
    if (regno) regno.textContent = item.regNo || 'Not specified';
    desc.textContent = item.description;
    rep.textContent = item.reporterName;
    contact.textContent = item.contact;

    modal.classList.add('active');
}

function closeItemModal(e) {
    const modal = document.getElementById('item-modal');
    if (modal) modal.classList.remove('active');
}

/**
 * Toast Notification Utility
 */
function showToast(message) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:18px;height:18px;color:#ea580c;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>${escapeHtml(message)}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 200ms ease';
        setTimeout(() => toast.remove(), 250);
    }, 3500);
}

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
}
