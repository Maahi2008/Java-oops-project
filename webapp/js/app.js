/**
 * Manipal University Jaipur (MUJ) Campus Lost & Found Matching System
 * Frontend Controller with Reg No verification, Campus Locations (Library, AB1, AB2, AB3, LHC),
 * Item Claim Lifecycle, Image Upload / Preview, Custody Admin Panel with CSV Export,
 * and Interactive Visual Analytics.
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
        type: '',
        status: 'ACTIVE'
    },
    adminFilters: {
        status: '',
        query: ''
    },
    uploadedImages: {
        lost: null,
        found: null
    },
    currentItemId: null,
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
        type: "LOST",
        status: "ACTIVE",
        imageUrl: ""
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
        type: "FOUND",
        status: "ACTIVE",
        imageUrl: ""
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
        type: "LOST",
        status: "ACTIVE",
        imageUrl: ""
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
        type: "FOUND",
        status: "ACTIVE",
        imageUrl: ""
    },
    {
        id: "F103",
        name: "Scientific Calculator Casio fx-991EX",
        category: "Electronics",
        description: "Black scientific calculator left on desk in LHC Audi 2 after exam",
        location: "LHC (Audi 2)",
        date: "2026-10-15",
        reporterName: "Vikas Meena",
        regNo: "229304012",
        contact: "vikas.229304012@muj.manipal.edu",
        type: "FOUND",
        status: "CLAIMED",
        claimedBy: "229304088 (Tanmay Roy)",
        claimDate: "2026-10-19",
        imageUrl: ""
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
                // Ensure all items have a default status of ACTIVE if null
                state.items = data.map(i => ({
                    ...i,
                    status: i.status || 'ACTIVE',
                    imageUrl: i.imageUrl || ''
                }));
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
 * Skips items that have been CLAIMED / RESOLVED.
 */
function recalculateLocalMatches() {
    const results = [];
    const lostItems = state.items.filter(item => item.type === 'LOST' && item.status !== 'CLAIMED');
    const foundItems = state.items.filter(item => item.type === 'FOUND' && item.status !== 'CLAIMED');

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
    } else if (viewId === 'analytics') {
        loadAnalyticsData();
    } else if (viewId === 'admin') {
        renderAdminTable();
    }
}

/**
 * Update Dashboard Counters & Mini-Lists
 */
function updateDashboardStats() {
    const lostCount = state.items.filter(i => i.type === 'LOST' && i.status !== 'CLAIMED').length;
    const foundCount = state.items.filter(i => i.type === 'FOUND' && i.status !== 'CLAIMED').length;
    const claimedCount = state.items.filter(i => i.status === 'CLAIMED').length;
    const matchCount = state.matches.length;
    const totalCount = state.items.length;

    const lostEl = document.getElementById('stat-lost-count');
    const foundEl = document.getElementById('stat-found-count');
    const matchEl = document.getElementById('stat-matches-count');
    const claimedEl = document.getElementById('stat-claimed-count');
    const totalEl = document.getElementById('status-total-count');
    const navMatchEl = document.getElementById('nav-match-count');

    if (lostEl) lostEl.textContent = lostCount;
    if (foundEl) foundEl.textContent = foundCount;
    if (matchEl) matchEl.textContent = matchCount;
    if (claimedEl) claimedEl.textContent = claimedCount;
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

function filterBrowseByStatus(status) {
    const statusSelect = document.getElementById('status-filter');
    if (statusSelect) {
        statusSelect.value = status;
    }
    state.filters.status = status;
    setTypeFilter('');
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
    const statusSelect = document.getElementById('status-filter');
    const clearBtn = document.getElementById('search-clear-btn');

    state.filters.query = input ? input.value.trim().toLowerCase() : '';
    state.filters.category = categorySelect ? categorySelect.value : '';
    state.filters.location = locationSelect ? locationSelect.value : '';
    state.filters.status = statusSelect ? statusSelect.value : '';

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
    const statusSelect = document.getElementById('status-filter');

    if (input) input.value = '';
    if (categorySelect) categorySelect.value = '';
    if (locationSelect) locationSelect.value = '';
    if (statusSelect) statusSelect.value = 'ACTIVE';

    state.filters.status = 'ACTIVE';
    setTypeFilter('');
    handleSearchChange();
    showToast('Filters reset to active MUJ campus directory.');
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
        if (state.filters.status) {
            const isClaimed = item.status === 'CLAIMED';
            if (state.filters.status === 'ACTIVE' && isClaimed) return false;
            if (state.filters.status === 'CLAIMED' && !isClaimed) return false;
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

    grid.innerHTML = filtered.map(item => {
        const isClaimed = item.status === 'CLAIMED';
        return `
            <div class="item-card ${isClaimed ? 'is-claimed' : ''}" onclick="openItemModal('${item.id}')">
                <div>
                    ${item.imageUrl ? `
                    <div class="item-card-image-box">
                        <img src="${item.imageUrl}" alt="${escapeHtml(item.name)}" loading="lazy">
                    </div>` : ''}

                    <div class="item-card-header">
                        <div style="display:flex;align-items:center;gap:0.4rem;">
                            <span class="badge ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}">${item.type}</span>
                            <span class="badge-status ${isClaimed ? 'claimed' : 'active'}">
                                ${isClaimed ? 'Resolved' : 'Active'}
                            </span>
                        </div>
                        <span class="item-id-pill">${item.id}</span>
                    </div>

                    <h3 class="item-title">${escapeHtml(item.name)}</h3>
                    <span class="item-category-tag">${escapeHtml(item.category)}</span>
                    <p class="item-desc">${escapeHtml(item.description)}</p>

                    ${isClaimed ? `
                    <div class="item-claimed-banner">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                        <span>Returned to: ${escapeHtml(item.claimedBy || 'Student')}</span>
                    </div>` : ''}
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
                        <div class="item-meta-row" style="color: var(--blue-primary); font-weight: 700;">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                            <span>Reg No: ${escapeHtml(item.regNo)}</span>
                        </div>` : ''}
                    </div>

                    <div class="item-footer-actions">
                        <div>
                            <span class="reporter-label">${item.type === 'LOST' ? 'Student:' : 'Finder:'}</span>
                            <span class="reporter-name"> ${escapeHtml(item.reporterName)}</span>
                        </div>
                        <div style="display:flex;align-items:center;gap:0.4rem;">
                            ${!isClaimed ? `
                            <button class="btn-claim-action" onclick="openClaimModal('${item.id}', event)">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px;"><polyline points="20 6 9 17 4 12"/></svg>
                                <span>Claim</span>
                            </button>` : ''}
                            <button class="btn btn-outline" style="padding: 0.35rem 0.65rem; font-size: 0.78rem;">Details</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Image Upload & Compression Handler
 * Converts selected file to a small base64 data URI (max 500x500) to keep memory lightweight.
 */
function handleImageSelected(event, type) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
        showToast('Please select a valid image file (PNG, JPG, or WEBP).');
        return;
    }

    const reader = new FileReader();
    reader.onload = function(e) {
        const img = new Image();
        img.onload = function() {
            // Resize image to max 500x500 to maintain high quality with small Base64 payload
            const maxDim = 500;
            let width = img.width;
            let height = img.height;

            if (width > maxDim || height > maxDim) {
                if (width > height) {
                    height = Math.round((height * maxDim) / width);
                    width = maxDim;
                } else {
                    width = Math.round((width * maxDim) / height);
                    height = maxDim;
                }
            }

            const canvas = document.createElement('canvas');
            canvas.width = width;
            canvas.height = height;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0, width, height);

            const compressedBase64 = canvas.toDataURL('image/jpeg', 0.82);
            state.uploadedImages[type] = compressedBase64;

            // Show Preview UI
            const previewContainer = document.getElementById(`${type}-image-preview`);
            const thumbImg = document.getElementById(`${type}-image-thumb`);
            const nameEl = document.getElementById(`${type}-image-name`);
            const sizeEl = document.getElementById(`${type}-image-size`);

            if (previewContainer && thumbImg && nameEl && sizeEl) {
                thumbImg.src = compressedBase64;
                nameEl.textContent = file.name;
                const sizeKb = Math.round(compressedBase64.length * 0.75 / 1024);
                sizeEl.textContent = `Optimized size: ~${sizeKb} KB`;
                previewContainer.style.display = 'flex';
            }
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

function clearSelectedImage(type) {
    state.uploadedImages[type] = null;
    const input = document.getElementById(`${type}-image-input`);
    const previewContainer = document.getElementById(`${type}-image-preview`);
    if (input) input.value = '';
    if (previewContainer) previewContainer.style.display = 'none';
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
    const imageUrl = state.uploadedImages.lost || '';

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
        type: 'LOST',
        status: 'ACTIVE',
        imageUrl
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
                state.items.push({
                    ...saved,
                    status: saved.status || 'ACTIVE',
                    imageUrl: saved.imageUrl || imageUrl
                });
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

    clearSelectedImage('lost');
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
    const imageUrl = state.uploadedImages.found || '';

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
        type: 'FOUND',
        status: 'ACTIVE',
        imageUrl
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
                state.items.push({
                    ...saved,
                    status: saved.status || 'ACTIVE',
                    imageUrl: saved.imageUrl || imageUrl
                });
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

    clearSelectedImage('found');
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

    const lostItems = state.items.filter(i => i.type === 'LOST' && i.status !== 'CLAIMED');
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
                                <span class="compare-meta-value" style="color: var(--blue-primary);">${escapeHtml(m.lostItem.regNo || 'N/A')}</span>
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
                    <div style="display:flex;align-items:center;gap:0.5rem;">
                        <button class="btn btn-blue" onclick="copyContact('${escapeHtml(m.foundItem.contact)}')">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                            <span>Copy Contact</span>
                        </button>
                        <button class="btn btn-outline" onclick="openClaimModal('${m.foundItem.id}', event)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;"><polyline points="20 6 9 17 4 12"/></svg>
                            <span>Mark Claimed</span>
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

    state.currentItemId = itemId;

    const modal = document.getElementById('item-modal');
    const badge = document.getElementById('modal-type-badge');
    const statusBadge = document.getElementById('modal-status-badge');
    const name = document.getElementById('modal-item-name');
    const id = document.getElementById('modal-item-id');
    const cat = document.getElementById('modal-item-category');
    const loc = document.getElementById('modal-item-location');
    const date = document.getElementById('modal-item-date');
    const regno = document.getElementById('modal-item-regno');
    const desc = document.getElementById('modal-item-description');
    const rep = document.getElementById('modal-item-reporter');
    const contact = document.getElementById('modal-item-contact');

    const imgContainer = document.getElementById('modal-image-container');
    const modalImg = document.getElementById('modal-item-image');
    const claimBanner = document.getElementById('modal-claim-banner');
    const claimText = document.getElementById('modal-claim-text');
    const claimActionBtn = document.getElementById('modal-claim-action-btn');

    const isClaimed = item.status === 'CLAIMED';

    badge.className = `badge ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}`;
    badge.textContent = item.type;

    if (statusBadge) {
        statusBadge.className = `badge-status ${isClaimed ? 'claimed' : 'active'}`;
        statusBadge.textContent = isClaimed ? 'Resolved / Claimed' : 'Active Notice';
    }

    name.textContent = item.name;
    id.textContent = item.id;
    cat.textContent = item.category;
    loc.textContent = item.location;
    date.textContent = item.date;
    if (regno) regno.textContent = item.regNo || 'Not specified';
    desc.textContent = item.description;
    rep.textContent = item.reporterName;
    contact.textContent = item.contact;

    // Handle Item Image
    if (imgContainer && modalImg) {
        if (item.imageUrl) {
            modalImg.src = item.imageUrl;
            imgContainer.style.display = 'flex';
        } else {
            imgContainer.style.display = 'none';
        }
    }

    // Handle Claim Status Banner & Action Button
    if (claimBanner && claimText) {
        if (isClaimed) {
            claimText.textContent = `Claimed & returned to ${item.claimedBy || 'Student'} on ${item.claimDate || 'campus file'}.`;
            claimBanner.style.display = 'flex';
        } else {
            claimBanner.style.display = 'none';
        }
    }

    if (claimActionBtn) {
        claimActionBtn.style.display = isClaimed ? 'none' : 'inline-flex';
    }

    modal.classList.add('active');
}

function closeItemModal(e) {
    const modal = document.getElementById('item-modal');
    if (modal) modal.classList.remove('active');
}

/**
 * Claim Modal Workflow
 */
function openClaimModal(itemId, event) {
    if (event) event.stopPropagation();

    const item = state.items.find(i => i.id === itemId);
    if (!item) return;

    state.currentItemId = itemId;

    const modal = document.getElementById('claim-modal');
    const titleEl = document.getElementById('claim-item-title');
    const metaEl = document.getElementById('claim-item-meta');
    const idInput = document.getElementById('claim-item-id');
    const regnoInput = document.getElementById('claimant-regno');
    const nameInput = document.getElementById('claimant-name');
    const checkbox = document.getElementById('claim-verified-checkbox');

    if (idInput) idInput.value = item.id;
    if (titleEl) titleEl.textContent = `${item.id}: ${item.name} (${item.type})`;
    if (metaEl) metaEl.textContent = `MUJ Location: ${item.location} | Category: ${item.category}`;

    if (regnoInput) {
        // Pre-fill with lost student Reg No if available
        regnoInput.value = item.regNo || '';
    }
    if (nameInput) nameInput.value = '';
    if (checkbox) checkbox.checked = false;

    if (modal) modal.classList.add('active');
}

function openClaimModalFromCurrent() {
    closeItemModal();
    if (state.currentItemId) {
        openClaimModal(state.currentItemId);
    }
}

function closeClaimModal() {
    const modal = document.getElementById('claim-modal');
    if (modal) modal.classList.remove('active');
}

async function submitItemClaim(event) {
    event.preventDefault();

    const id = document.getElementById('claim-item-id').value;
    const claimantRegNo = document.getElementById('claimant-regno').value.trim();
    const claimantName = document.getElementById('claimant-name').value.trim();

    if (!id || !claimantRegNo) return;

    const claimantStr = claimantRegNo + (claimantName ? ` (${claimantName})` : '');
    const today = new Date().toISOString().split('T')[0];

    if (state.isBackendOnline) {
        try {
            await fetch('api/items/claim', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, claimedBy: claimantStr, regNo: claimantRegNo })
            });
            await loadBackendMatches();
        } catch (e) {
            console.warn('Error submitting claim to backend:', e);
        }
    }

    // Update local state item
    const item = state.items.find(i => i.id === id);
    if (item) {
        item.status = 'CLAIMED';
        item.claimedBy = claimantStr;
        item.claimDate = today;
    }

    recalculateLocalMatches();
    updateDashboardStats();
    renderBrowseItems();
    renderMatches();
    populateMatchLostSelector();
    renderAdminTable();
    loadAnalyticsData();

    closeClaimModal();
    showToast(`Item ${id} successfully marked as resolved & returned to ${claimantStr}!`);
}

/**
 * Security Admin / Custody Desk Management
 */
function setAdminStatusFilter(status) {
    state.adminFilters.status = status;
    document.querySelectorAll('[data-admin-status]').forEach(btn => {
        if (btn.getAttribute('data-admin-status') === status) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
    renderAdminTable();
}

function handleAdminSearch() {
    const input = document.getElementById('admin-search-box');
    state.adminFilters.query = input ? input.value.trim().toLowerCase() : '';
    renderAdminTable();
}

function renderAdminTable() {
    const tbody = document.getElementById('admin-table-body');
    const emptyEl = document.getElementById('admin-empty-placeholder');
    if (!tbody) return;

    let filtered = state.items.filter(item => {
        if (state.adminFilters.status) {
            const isClaimed = item.status === 'CLAIMED';
            if (state.adminFilters.status === 'ACTIVE' && isClaimed) return false;
            if (state.adminFilters.status === 'CLAIMED' && !isClaimed) return false;
        }
        if (state.adminFilters.query) {
            const q = state.adminFilters.query;
            const matchId = item.id.toLowerCase().includes(q);
            const matchName = item.name.toLowerCase().includes(q);
            const matchReg = item.regNo && item.regNo.toLowerCase().includes(q);
            const matchLoc = item.location && item.location.toLowerCase().includes(q);
            if (!matchId && !matchName && !matchReg && !matchLoc) return false;
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '';
        if (emptyEl) emptyEl.style.display = 'block';
        return;
    }

    if (emptyEl) emptyEl.style.display = 'none';

    tbody.innerHTML = filtered.map(item => {
        const isClaimed = item.status === 'CLAIMED';
        return `
            <tr>
                <td><strong class="item-id-pill">${item.id}</strong></td>
                <td><span class="badge ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}">${item.type}</span></td>
                <td>
                    <div style="font-weight:600;color:var(--navy-dark);">${escapeHtml(item.name)}</div>
                    <div style="font-size:0.75rem;color:var(--navy-muted);">${escapeHtml(item.reporterName)}</div>
                </td>
                <td>${escapeHtml(item.category)}</td>
                <td>${escapeHtml(item.location)}</td>
                <td><code style="color:var(--blue-primary);font-weight:700;">${escapeHtml(item.regNo || 'N/A')}</code></td>
                <td>${item.date}</td>
                <td>
                    <span class="badge-status ${isClaimed ? 'claimed' : 'active'}">
                        ${isClaimed ? 'Claimed' : 'In Custody'}
                    </span>
                </td>
                <td>
                    <div class="admin-actions-cell">
                        <button class="btn-table-sm btn-table-view" onclick="openItemModal('${item.id}')">View</button>
                        ${!isClaimed ? `
                        <button class="btn-table-sm" style="background:#ecfdf5;color:#047857;border-color:#a7f3d0;" onclick="openClaimModal('${item.id}')">Claim</button>` : ''}
                        <button class="btn-table-sm btn-table-delete" onclick="deleteItem('${item.id}')">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function deleteItem(itemId) {
    if (!confirm(`Are you sure you want to permanently delete record ${itemId} from MUJ Campus database?`)) {
        return;
    }

    if (state.isBackendOnline) {
        try {
            await fetch('api/items/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id: itemId })
            });
            await loadBackendMatches();
        } catch (e) {
            console.warn('Error deleting item from backend:', e);
        }
    }

    state.items = state.items.filter(i => i.id !== itemId);
    recalculateLocalMatches();
    updateDashboardStats();
    renderBrowseItems();
    renderMatches();
    populateMatchLostSelector();
    renderAdminTable();
    loadAnalyticsData();
    showToast(`Item ${itemId} removed from MUJ database.`);
}

/**
 * Export Custody Report to CSV
 */
function exportCustodyCSV() {
    if (!state.items || state.items.length === 0) {
        showToast('No records available to export.');
        return;
    }

    const headers = [
        'Item ID',
        'Type',
        'Item Name',
        'Category',
        'Campus Location',
        'Registration No',
        'Date Logged',
        'Reporter Name',
        'Contact Info',
        'Status',
        'Claimed By',
        'Claim Date'
    ];

    const rows = state.items.map(item => [
        item.id,
        item.type,
        `"${(item.name || '').replace(/"/g, '""')}"`,
        `"${(item.category || '').replace(/"/g, '""')}"`,
        `"${(item.location || '').replace(/"/g, '""')}"`,
        `"${(item.regNo || '').replace(/"/g, '""')}"`,
        item.date,
        `"${(item.reporterName || '').replace(/"/g, '""')}"`,
        `"${(item.contact || '').replace(/"/g, '""')}"`,
        item.status || 'ACTIVE',
        `"${(item.claimedBy || '').replace(/"/g, '""')}"`,
        item.claimDate || ''
    ]);

    const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    const today = new Date().toISOString().split('T')[0];
    link.setAttribute('href', url);
    link.setAttribute('download', `MUJ_Lost_Found_Custody_Report_${today}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    showToast(`Exported ${state.items.length} records to CSV report.`);
}

/**
 * Visual Analytics & Charts
 */
async function loadAnalyticsData() {
    let analytics = null;

    if (state.isBackendOnline) {
        try {
            const res = await fetch('api/analytics');
            if (res.ok) {
                analytics = await res.json();
            }
        } catch (e) {
            console.warn('Analytics API error, computing locally:', e);
        }
    }

    // Fallback local computation if offline or server response unavailable
    if (!analytics) {
        const total = state.items.length;
        const claimed = state.items.filter(i => i.status === 'CLAIMED').length;
        const lost = state.items.filter(i => i.type === 'LOST' && i.status !== 'CLAIMED').length;
        const found = state.items.filter(i => i.type === 'FOUND' && i.status !== 'CLAIMED').length;
        const recoveryRate = total > 0 ? ((claimed / total) * 100).toFixed(1) : '0.0';

        const locationStats = {};
        const categoryStats = {};

        for (const item of state.items) {
            let loc = 'Other';
            const rawLoc = (item.location || '').toLowerCase();
            if (rawLoc.includes('library')) loc = 'Library';
            else if (rawLoc.includes('ab1')) loc = 'AB1';
            else if (rawLoc.includes('ab2')) loc = 'AB2';
            else if (rawLoc.includes('ab3')) loc = 'AB3';
            else if (rawLoc.includes('lhc')) loc = 'LHC';
            else if (rawLoc.includes('food') || rawLoc.includes('mess')) loc = 'Food Court';
            else if (rawLoc.includes('hostel')) loc = 'Hostels';
            else if (rawLoc.includes('sports')) loc = 'Sports';

            locationStats[loc] = (locationStats[loc] || 0) + 1;

            const cat = item.category || 'Other';
            categoryStats[cat] = (categoryStats[cat] || 0) + 1;
        }

        analytics = {
            totalItems: total,
            lostCount: lost,
            foundCount: found,
            claimedCount: claimed,
            recoveryRate,
            locationStats,
            categoryStats
        };
    }

    // Update KPI Cards
    const kpiTotal = document.getElementById('kpi-total-items');
    const kpiActive = document.getElementById('kpi-active-items');
    const kpiClaimed = document.getElementById('kpi-claimed-items');
    const kpiRate = document.getElementById('kpi-recovery-rate');

    if (kpiTotal) kpiTotal.textContent = analytics.totalItems;
    if (kpiActive) kpiActive.textContent = (analytics.lostCount + analytics.foundCount);
    if (kpiClaimed) kpiClaimed.textContent = analytics.claimedCount;
    if (kpiRate) kpiRate.textContent = `${analytics.recoveryRate}%`;

    // Render Location Hotspot Bar Chart via SVG
    renderLocationBarChart(analytics.locationStats || {});

    // Render Category Breakdown
    renderCategoryBreakdown(analytics.categoryStats || {}, analytics.totalItems || 1);
}

function renderLocationBarChart(locStats) {
    const container = document.getElementById('location-chart-container');
    if (!container) return;

    const locations = ['Library', 'AB1', 'AB2', 'AB3', 'LHC', 'Food Court', 'Hostels', 'Sports', 'Other'];
    const counts = locations.map(l => locStats[l] || 0);
    const maxVal = Math.max(...counts, 4);

    const chartWidth = 500;
    const chartHeight = 260;
    const paddingLeft = 40;
    const paddingBottom = 40;
    const paddingTop = 25;
    const paddingRight = 20;

    const barWidth = 32;
    const availableWidth = chartWidth - paddingLeft - paddingRight;
    const step = availableWidth / locations.length;
    const usableHeight = chartHeight - paddingTop - paddingBottom;

    let barsSvg = '';

    locations.forEach((loc, index) => {
        const count = locStats[loc] || 0;
        const barHeight = Math.round((count / maxVal) * usableHeight);
        const x = paddingLeft + (index * step) + (step - barWidth) / 2;
        const y = chartHeight - paddingBottom - barHeight;

        barsSvg += `
            <g class="bar-group">
                <rect class="bar-rect" x="${x}" y="${y}" width="${barWidth}" height="${barHeight}" rx="4" ry="4">
                    <title>${loc}: ${count} item(s)</title>
                </rect>
                <text class="chart-bar-value" x="${x + barWidth / 2}" y="${y - 6}">${count > 0 ? count : ''}</text>
                <text class="chart-axis-label" x="${x + barWidth / 2}" y="${chartHeight - 14}" text-anchor="middle">${loc}</text>
            </g>
        `;
    });

    const svgHtml = `
        <svg viewBox="0 0 ${chartWidth} ${chartHeight}" class="svg-bar-chart" style="overflow:visible;">
            <!-- Grid lines -->
            <line x1="${paddingLeft}" y1="${paddingTop}" x2="${chartWidth - paddingRight}" y2="${paddingTop}" stroke="#e2e8f0" stroke-dasharray="3,3"/>
            <line x1="${paddingLeft}" y1="${paddingTop + usableHeight / 2}" x2="${chartWidth - paddingRight}" y2="${paddingTop + usableHeight / 2}" stroke="#e2e8f0" stroke-dasharray="3,3"/>
            <line x1="${paddingLeft}" y1="${chartHeight - paddingBottom}" x2="${chartWidth - paddingRight}" y2="${chartHeight - paddingBottom}" stroke="#cbd5e1" stroke-width="1.5"/>

            ${barsSvg}
        </svg>
    `;

    container.innerHTML = svgHtml;
}

function renderCategoryBreakdown(catStats, totalItems) {
    const container = document.getElementById('category-chart-container');
    if (!container) return;

    const entries = Object.entries(catStats).sort((a, b) => b[1] - a[1]);
    if (entries.length === 0) {
        container.innerHTML = '<p style="color:var(--navy-muted);font-size:0.85rem;">No category data yet.</p>';
        return;
    }

    container.innerHTML = entries.map(([cat, count]) => {
        const pct = Math.round((count / totalItems) * 100);
        return `
            <div class="category-row-item">
                <div class="category-row-meta">
                    <span>${escapeHtml(cat)}</span>
                    <span style="color:var(--blue-primary);font-weight:700;">${count} items (${pct}%)</span>
                </div>
                <div class="category-bar-bg">
                    <div class="category-bar-fill" style="width: ${pct}%;"></div>
                </div>
            </div>
        `;
    }).join('');
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
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:18px;height:18px;color:#2563eb;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
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
