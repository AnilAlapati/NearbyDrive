// Mock Data & Local Storage Init
const VEHICLE_TYPES = ["Car", "Motorbike", "Scooter", "Bicycle", "Other"];
const BLOCKS = ["Block A", "Block B", "Block C", "Block D"];

const DEFAULT_VEHICLES = [
    { id: 'v1', model: 'Honda City', type: 'Car', reg: 'MH12AB1234', rent: 150, hours: 'Daily 9AM-9PM', desc: 'Well maintained, no smoking', isEv: false, ownerName: 'Aarav', ownerBlock: 'Block A', ownerFlat: '101', ownerId: 'user_1', isRented: false },
    { id: 'v2', model: 'Ather 450X', type: 'Scooter', reg: 'MH14XY9876', rent: 40, hours: 'Weekends only', desc: 'Fully charged, double helmet', isEv: true, ownerName: 'Priya', ownerBlock: 'Block B', ownerFlat: '205', ownerId: 'user_2', isRented: false },
    { id: 'v3', model: 'Tata Nexon EV', type: 'Car', reg: 'MH12EV5555', rent: 200, hours: 'Anytime', desc: 'Clean interior', isEv: true, ownerName: 'Vikram', ownerBlock: 'Block C', ownerFlat: '304', ownerId: 'user_3', isRented: true },
];

const INIT_PROFILE = { name: '', block: '', flat: '', phone: '', verified: false, id: 'my_user_id' };

// State
let appState = {
    country: 'IN', // 'IN' or 'US'
    vehicles: [],
    bookings: [],
    profile: null,
    searchQuery: '',
    evOnly: false,
    activeTypeFilter: 'All'
};

// DOM Elements
const els = {
    tabs: document.querySelectorAll('.tab-section'),
    navItems: document.querySelectorAll('.nav-item'),
    
    // Header
    countrySelectorBtn: document.getElementById('countrySelectorBtn'),
    countryDropdown: document.getElementById('countryDropdown'),
    currentFlag: document.getElementById('currentFlag'),
    currentCountry: document.getElementById('currentCountry'),
    headerResidentBadge: document.getElementById('headerResidentBadge'),
    headerResidentText: document.getElementById('headerResidentText'),
    
    // Browse
    searchInput: document.getElementById('searchInput'),
    clearSearchBtn: document.getElementById('clearSearchBtn'),
    typeFiltersContainer: document.getElementById('typeFiltersContainer'),
    evFilterToggle: document.getElementById('evFilterToggle'),
    vehiclesFeed: document.getElementById('vehiclesFeed'),
    
    // Host
    addVehicleForm: document.getElementById('addVehicleForm'),
    hostModel: document.getElementById('hostModel'),
    hostReg: document.getElementById('hostReg'),
    hostRent: document.getElementById('hostRent'),
    hostHours: document.getElementById('hostHours'),
    hostDesc: document.getElementById('hostDesc'),
    hostIsEv: document.getElementById('hostIsEv'),
    hostSubmitBtn: document.getElementById('hostSubmitBtn'),
    hostFormWarning: document.getElementById('hostFormWarning'),
    hostCurrencySymbol: document.getElementById('hostCurrencySymbol'),
    myListingsFeed: document.getElementById('myListingsFeed'),
    myListingsCount: document.getElementById('myListingsCount'),
    
    // Bookings
    subtabs: document.querySelectorAll('.subtab'),
    subtabContents: document.querySelectorAll('.subtab-content'),
    borrowingFeed: document.getElementById('borrowingFeed'),
    lendingFeed: document.getElementById('lendingFeed'),
    borrowingEmpty: document.getElementById('borrowingEmpty'),
    lendingEmpty: document.getElementById('lendingEmpty'),
    borrowingCount: document.getElementById('borrowingCount'),
    lendingCount: document.getElementById('lendingCount'),
    pendingHostBadge: document.getElementById('pendingHostBadge'),
    navBookingsBadge: document.getElementById('navBookingsBadge'),
    
    // Profile
    passportCard: document.getElementById('passportCard'),
    passportName: document.getElementById('passportName'),
    passportIcon: document.getElementById('passportIcon'),
    passportStatusText: document.getElementById('passportStatusText'),
    passportBlockValue: document.getElementById('passportBlockValue'),
    passportFlatValue: document.getElementById('passportFlatValue'),
    passportPhoneValue: document.getElementById('passportPhoneValue'),
    verificationPromptCard: document.getElementById('verificationPromptCard'),
    startVerificationBtn: document.getElementById('startVerificationBtn'),
    profileName: document.getElementById('profileName'),
    profileFlat: document.getElementById('profileFlat'),
    profilePhone: document.getElementById('profilePhone'),
    saveProfileBtn: document.getElementById('saveProfileBtn'),
    resetProfileBtn: document.getElementById('resetProfileBtn'),
    formVerifiedTag: document.getElementById('formVerifiedTag'),
    profileChangeWarning: document.getElementById('profileChangeWarning'),
    
    // Verification Modal
    verifyModal: document.getElementById('verifyModal'),
    verifyLoading: document.getElementById('verifyLoading'),
    verifyProgressText: document.getElementById('verifyProgressText'),
    verifyInputArea: document.getElementById('verifyInputArea'),
    verifyPin: document.getElementById('verifyPin'),
    verifyError: document.getElementById('verifyError'),
    verifyCancelBtn: document.getElementById('verifyCancelBtn'),
    verifySubmitBtn: document.getElementById('verifySubmitBtn'),
    verifyModalDesc: document.getElementById('verifyModalDesc'),
    
    // Booking Modal
    bookModal: document.getElementById('bookModal'),
    bookCloseBtn: document.getElementById('bookCloseBtn'),
    bookVIcon: document.getElementById('bookVIcon'),
    bookVModel: document.getElementById('bookVModel'),
    bookVOwner: document.getElementById('bookVOwner'),
    bookHours: document.getElementById('bookHours'),
    incHoursBtn: document.getElementById('incHoursBtn'),
    decHoursBtn: document.getElementById('decHoursBtn'),
    bookNotes: document.getElementById('bookNotes'),
    bookRateLabel: document.getElementById('bookRateLabel'),
    bookTotalLabel: document.getElementById('bookTotalLabel'),
    bookConfirmBtn: document.getElementById('bookConfirmBtn'),
    
    // Warning Modal
    warningModal: document.getElementById('warningModal'),
    warningModalTitle: document.getElementById('warningModalTitle'),
    warningModalText: document.getElementById('warningModalText'),
    warningCancelBtn: document.getElementById('warningCancelBtn'),
    warningGoToProfileBtn: document.getElementById('warningGoToProfileBtn')
};

let currentBookingVehicle = null;
let currentBookingHours = 3;

// Initialize
function init() {
    loadState();
    setupEventListeners();
    renderAll();
}

function loadState() {
    const saved = localStorage.getItem('nearbydrive_state');
    if (saved) {
        appState = JSON.parse(saved);
    } else {
        appState.vehicles = [...DEFAULT_VEHICLES];
        appState.profile = { ...INIT_PROFILE };
        saveState();
    }
}

function saveState() {
    localStorage.setItem('nearbydrive_state', JSON.stringify(appState));
}

// Render Functions
function renderAll() {
    renderFilters();
    renderHostFormEnums();
    renderProfileFormEnums();
    updateUIFromState();
}

function updateUIFromState() {
    // Country logic
    els.currentFlag.textContent = appState.country === 'IN' ? '🇮🇳' : '🇺🇸';
    els.currentCountry.textContent = appState.country;
    els.hostCurrencySymbol.textContent = appState.country === 'IN' ? '₹' : '$';
    
    // Header Profile
    if (appState.profile.block && appState.profile.flat) {
        els.headerResidentBadge.style.display = 'flex';
        els.headerResidentText.textContent = `${appState.profile.block}-${appState.profile.flat}`;
    } else {
        els.headerResidentBadge.style.display = 'none';
    }
    
    renderVehicles();
    renderHostListings();
    renderBookings();
    renderProfile();
}

// Helpers
function getCurrency() { return appState.country === 'IN' ? '₹' : '$'; }
function getIconForType(type) {
    const map = { 'Car': 'directions_car', 'Motorbike': 'two_wheeler', 'Scooter': 'electric_scooter', 'Bicycle': 'pedal_bike', 'Other': 'local_shipping' };
    return map[type] || 'directions_car';
}
function getBannerClass(type) {
    const map = { 'Car': 'v-banner-car', 'Motorbike': 'v-banner-bike', 'Scooter': 'v-banner-scooter', 'Bicycle': 'v-banner-bicycle', 'Other': 'v-banner-other' };
    return map[type] || 'v-banner-other';
}
function getColorClass(type) {
    const map = { 'Car': 'v-color-car', 'Motorbike': 'v-color-bike', 'Scooter': 'v-color-scooter', 'Bicycle': 'v-color-bicycle', 'Other': 'v-color-other' };
    return map[type] || 'v-color-other';
}

function renderFilters() {
    const types = ['All', ...VEHICLE_TYPES];
    els.typeFiltersContainer.innerHTML = types.map(t => 
        `<div class="chip ${appState.activeTypeFilter === t ? 'active' : ''}" data-type="${t}">${t}</div>`
    ).join('');
    
    els.typeFiltersContainer.querySelectorAll('.chip').forEach(c => {
        c.addEventListener('click', (e) => {
            appState.activeTypeFilter = e.target.dataset.type;
            renderFilters();
            renderVehicles();
        });
    });
    
    els.evFilterToggle.className = `ev-filter ${appState.evOnly ? 'active' : ''}`;
}

function renderHostFormEnums() {
    const hostTypeRadios = document.getElementById('hostTypeRadios');
    hostTypeRadios.innerHTML = VEHICLE_TYPES.map((t, i) => 
        `<div class="radio-chip ${i === 0 ? 'active' : ''}" data-type="${t}">${t}</div>`
    ).join('');
    
    hostTypeRadios.querySelectorAll('.radio-chip').forEach(c => {
        c.addEventListener('click', (e) => {
            hostTypeRadios.querySelectorAll('.radio-chip').forEach(rc => rc.classList.remove('active'));
            e.target.classList.add('active');
            checkHostFormValid();
        });
    });
}

function renderProfileFormEnums() {
    const profileBlockRadios = document.getElementById('profileBlockRadios');
    profileBlockRadios.innerHTML = BLOCKS.map(b => 
        `<div class="radio-chip ${appState.profile.block === b ? 'active' : ''}" data-block="${b}">${b}</div>`
    ).join('');
    
    profileBlockRadios.querySelectorAll('.radio-chip').forEach(c => {
        c.addEventListener('click', (e) => {
            profileBlockRadios.querySelectorAll('.radio-chip').forEach(rc => rc.classList.remove('active'));
            e.target.classList.add('active');
            checkProfileChanges();
        });
    });
}

function getActiveHostType() {
    return document.querySelector('#hostTypeRadios .active')?.dataset.type || VEHICLE_TYPES[0];
}
function getActiveProfileBlock() {
    return document.querySelector('#profileBlockRadios .active')?.dataset.block || '';
}

function renderVehicles() {
    let filtered = appState.vehicles.filter(v => v.ownerId !== appState.profile.id); // Don't show own vehicles in browse
    
    if (appState.activeTypeFilter !== 'All') {
        filtered = filtered.filter(v => v.type === appState.activeTypeFilter);
    }
    if (appState.evOnly) {
        filtered = filtered.filter(v => v.isEv);
    }
    if (appState.searchQuery) {
        const q = appState.searchQuery.toLowerCase();
        filtered = filtered.filter(v => 
            v.model.toLowerCase().includes(q) || 
            v.ownerBlock.toLowerCase().includes(q) || 
            v.desc.toLowerCase().includes(q)
        );
    }
    
    if (filtered.length === 0) {
        els.vehiclesFeed.innerHTML = `
            <div class="empty-state">
                <span class="material-icons-outlined empty-icon">search_off</span>
                <h3>No vehicles found</h3>
                <p>Try adjusting your filters or search query.</p>
            </div>
        `;
        return;
    }
    
    els.vehiclesFeed.innerHTML = filtered.map(v => `
        <div class="vehicle-card">
            <div class="vehicle-banner ${getBannerClass(v.type)}">
                <div class="v-tag v-tag-type">${v.type}</div>
                ${v.isEv ? `<div class="v-tag v-tag-ev top-right"><span class="material-icons">bolt</span> EV</div>` : ''}
                <div class="v-banner-icon-bg">
                    <span class="material-icons v-banner-icon ${getColorClass(v.type)}">${getIconForType(v.type)}</span>
                </div>
            </div>
            <div class="v-body">
                <div class="v-header-row">
                    <div>
                        <div class="v-model">${v.model}</div>
                        <div class="v-owner-info">
                            <span class="material-icons">verified_user</span>
                            ${v.ownerName} • ${v.ownerBlock}-${v.ownerFlat}
                        </div>
                    </div>
                    <div class="v-price-col">
                        <div class="v-price">${getCurrency()}${v.rent}</div>
                        <div class="v-price-unit">per hour</div>
                    </div>
                </div>
                <div class="divider"></div>
                <div class="v-desc">${v.desc}</div>
                <div class="v-footer-row">
                    <div>
                        <div class="v-info-item"><span class="material-icons">schedule</span> ${v.hours}</div>
                    </div>
                    ${v.isRented 
                        ? `<div class="v-status-badge v-status-rented">Currently Rented</div>`
                        : `<button class="btn primary btn-book" onclick="openBookModal('${v.id}')">Request Ride</button>`
                    }
                </div>
            </div>
        </div>
    `).join('');
}

function renderHostListings() {
    const myListings = appState.vehicles.filter(v => v.ownerId === appState.profile.id);
    els.myListingsCount.textContent = myListings.length;
    
    if (myListings.length === 0) {
        els.myListingsFeed.innerHTML = `
            <div class="empty-state">
                <p>You haven't listed any vehicles yet.</p>
            </div>
        `;
        return;
    }
    
    els.myListingsFeed.innerHTML = myListings.map(v => `
        <div class="hosted-item">
            <div class="h-item-left">
                <div class="h-item-icon-bg">
                    <span class="material-icons">${getIconForType(v.type)}</span>
                </div>
                <div>
                    <div class="h-item-title">${v.model} ${v.isEv ? '⚡' : ''}</div>
                    <div class="h-item-sub">${v.reg} • ${getCurrency()}${v.rent}/hr</div>
                </div>
            </div>
            <div class="h-item-right">
                <div class="h-status ${v.isRented ? 'rented' : 'available'}">${v.isRented ? 'Rented Out' : 'Available'}</div>
                <span class="material-icons icon-btn" onclick="deleteListing('${v.id}')">delete_outline</span>
            </div>
        </div>
    `).join('');
}

function renderBookings() {
    const myBorrowings = appState.bookings.filter(b => b.borrowerId === appState.profile.id);
    const myLendings = appState.bookings.filter(b => b.lenderId === appState.profile.id);
    
    els.borrowingCount.textContent = myBorrowings.length;
    els.lendingCount.textContent = myLendings.length;
    
    const pendingLendings = myLendings.filter(b => b.status === 'Requested').length;
    if (pendingLendings > 0) {
        els.pendingHostBadge.style.display = 'flex';
        els.pendingHostBadge.textContent = pendingLendings;
        els.navBookingsBadge.style.display = 'flex';
        els.navBookingsBadge.textContent = pendingLendings;
    } else {
        els.pendingHostBadge.style.display = 'none';
        els.navBookingsBadge.style.display = 'none';
    }
    
    els.borrowingEmpty.style.display = myBorrowings.length === 0 ? 'flex' : 'none';
    els.lendingEmpty.style.display = myLendings.length === 0 ? 'flex' : 'none';
    
    const statusClassMap = { 'Requested': 'requested', 'Approved': 'approved', 'Completed': 'completed', 'Cancelled': 'cancelled' };
    
    els.borrowingFeed.innerHTML = myBorrowings.map(b => `
        <div class="card booking-card">
            <div class="b-header">
                <div>
                    <div class="b-model">${b.vehicleModel}</div>
                    <div class="b-user">from ${b.lenderName}</div>
                </div>
                <div class="b-status ${statusClassMap[b.status]}">${b.status}</div>
            </div>
            <div class="divider"></div>
            <div class="b-details-row">
                <div><div class="b-label">DURATION</div><div class="b-val">${b.hours} hours</div></div>
                <div><div class="b-label">TOTAL</div><div class="b-total">${getCurrency()}${b.totalPrice}</div></div>
            </div>
            ${b.status === 'Requested' ? `
            <div class="b-actions">
                <button class="btn text-danger full-width btn-outline" onclick="updateBookingStatus('${b.id}', 'Cancelled')">Cancel Request</button>
            </div>` : ''}
            ${b.status === 'Approved' ? `
            <div class="b-actions">
                <button class="btn primary full-width" onclick="updateBookingStatus('${b.id}', 'Completed')">Mark Completed</button>
            </div>` : ''}
        </div>
    `).join('');
    
    els.lendingFeed.innerHTML = myLendings.map(b => `
        <div class="card booking-card">
            <div class="b-header">
                <div>
                    <div class="b-model">${b.vehicleModel}</div>
                    <div class="b-user">Requested by ${b.borrowerName} (${b.borrowerBlock}-${b.borrowerFlat})</div>
                    <div class="b-phone">📞 ${b.borrowerPhone}</div>
                </div>
                <div class="b-status ${statusClassMap[b.status]}">${b.status}</div>
            </div>
            ${b.notes ? `<div class="b-notes">"${b.notes}"</div>` : ''}
            <div class="divider"></div>
            <div class="b-details-row">
                <div><div class="b-label">DURATION</div><div class="b-val">${b.hours} hours</div></div>
                <div><div class="b-label">EARNINGS</div><div class="b-total">${getCurrency()}${b.totalPrice}</div></div>
            </div>
            ${b.status === 'Requested' ? `
            <div class="b-actions">
                <button class="btn text-danger full-width btn-outline" onclick="updateBookingStatus('${b.id}', 'Cancelled')">Decline</button>
                <button class="btn primary full-width" onclick="updateBookingStatus('${b.id}', 'Approved')">Approve</button>
            </div>` : ''}
        </div>
    `).join('');
}

function renderProfile() {
    const p = appState.profile;
    
    // Passport
    els.passportName.textContent = p.name || 'Guest Resident';
    els.passportBlockValue.textContent = p.block || 'Unassigned';
    els.passportFlatValue.textContent = p.flat || 'Pending Setup';
    els.passportPhoneValue.textContent = p.phone || 'None added';
    
    if (p.verified) {
        els.passportCard.classList.add('verified');
        els.passportIcon.textContent = 'verified';
        els.passportStatusText.textContent = 'VERIFIED RESIDENT';
        els.verificationPromptCard.style.display = 'none';
        els.formVerifiedTag.style.display = 'flex';
    } else {
        els.passportCard.classList.remove('verified');
        els.passportIcon.textContent = 'badge';
        els.passportStatusText.textContent = 'UNVERIFIED STATUS';
        
        if (p.name && p.block && p.flat) {
            els.verificationPromptCard.style.display = 'block';
        } else {
            els.verificationPromptCard.style.display = 'none';
        }
        els.formVerifiedTag.style.display = 'none';
    }
    
    // Form Values
    els.profileName.value = p.name;
    els.profileFlat.value = p.flat;
    els.profilePhone.value = p.phone;
    renderProfileFormEnums();
}

function checkProfileChanges() {
    const p = appState.profile;
    const currentBlock = getActiveProfileBlock();
    
    const isChanged = 
        els.profileName.value !== p.name ||
        currentBlock !== p.block ||
        els.profileFlat.value !== p.flat ||
        els.profilePhone.value !== p.phone;
        
    if (isChanged && p.verified) {
        els.profileChangeWarning.style.display = 'block';
    } else {
        els.profileChangeWarning.style.display = 'none';
    }
}

function checkHostFormValid() {
    const isValid = els.hostModel.value && els.hostReg.value && els.hostRent.value && els.hostHours.value && els.hostDesc.value;
    els.hostSubmitBtn.disabled = !isValid;
    
    if (!appState.profile.verified) {
        els.hostFormWarning.textContent = '⚠️ Society Verification Required to Host Vehicles';
        els.hostFormWarning.className = 'form-warning amber';
        els.hostSubmitBtn.disabled = true;
    } else {
        els.hostFormWarning.textContent = '';
        els.hostFormWarning.className = 'form-warning';
    }
}

// Actions
window.openBookModal = function(vehicleId) {
    if (!appState.profile.verified) {
        showWarningModal('Society Verification Required', 'You must verify your society residency in the Profile tab before requesting rides.');
        return;
    }
    
    currentBookingVehicle = appState.vehicles.find(v => v.id === vehicleId);
    if (!currentBookingVehicle) return;
    
    currentBookingHours = 3;
    els.bookVIcon.textContent = getIconForType(currentBookingVehicle.type);
    els.bookVModel.textContent = currentBookingVehicle.model;
    els.bookVOwner.textContent = `Owner: ${currentBookingVehicle.ownerName} (${currentBookingVehicle.ownerBlock}-${currentBookingVehicle.ownerFlat})`;
    els.bookHours.textContent = currentBookingHours;
    els.bookNotes.value = '';
    
    updateBookingTotals();
    els.bookModal.style.display = 'flex';
}

function updateBookingTotals() {
    if (!currentBookingVehicle) return;
    const cur = getCurrency();
    els.bookRateLabel.textContent = `${cur}${currentBookingVehicle.rent} / hr`;
    els.bookTotalLabel.textContent = `${cur}${currentBookingVehicle.rent * currentBookingHours}`;
}

window.deleteListing = function(id) {
    if(confirm('Remove this vehicle listing?')) {
        appState.vehicles = appState.vehicles.filter(v => v.id !== id);
        saveState();
        renderVehicles();
        renderHostListings();
    }
}

window.updateBookingStatus = function(id, status) {
    const booking = appState.bookings.find(b => b.id === id);
    if(booking) {
        booking.status = status;
        
        // Update vehicle status
        if (status === 'Approved') {
            const v = appState.vehicles.find(v => v.id === booking.vehicleId);
            if(v) v.isRented = true;
        } else if (status === 'Completed' || status === 'Cancelled') {
            const v = appState.vehicles.find(v => v.id === booking.vehicleId);
            if(v) v.isRented = false;
        }
        
        saveState();
        renderAll();
    }
}

function showWarningModal(title, text) {
    els.warningModalTitle.textContent = title;
    els.warningModalText.textContent = text;
    els.warningModal.style.display = 'flex';
}

// Event Listeners
function setupEventListeners() {
    // Navigation
    els.navItems.forEach(item => {
        item.addEventListener('click', () => {
            els.navItems.forEach(n => n.classList.remove('active'));
            els.tabs.forEach(t => t.style.display = 'none');
            
            item.classList.add('active');
            document.getElementById(item.dataset.target).style.display = 'block';
        });
    });
    
    els.subtabs.forEach(tab => {
        tab.addEventListener('click', () => {
            els.subtabs.forEach(t => t.classList.remove('active'));
            els.subtabContents.forEach(c => c.style.display = 'none');
            
            tab.classList.add('active');
            document.getElementById(tab.dataset.target).style.display = 'block';
        });
    });
    
    // Header
    els.countrySelectorBtn.addEventListener('click', () => {
        els.countryDropdown.style.display = els.countryDropdown.style.display === 'none' ? 'block' : 'none';
    });
    
    document.addEventListener('click', (e) => {
        if(!els.countrySelectorBtn.contains(e.target) && !els.countryDropdown.contains(e.target)) {
            els.countryDropdown.style.display = 'none';
        }
    });
    
    document.querySelectorAll('.country-option').forEach(opt => {
        opt.addEventListener('click', (e) => {
            appState.country = e.target.dataset.code;
            els.countryDropdown.style.display = 'none';
            saveState();
            renderAll();
        });
    });
    
    // Browse Search & Filters
    els.searchInput.addEventListener('input', (e) => {
        appState.searchQuery = e.target.value;
        els.clearSearchBtn.style.display = appState.searchQuery ? 'block' : 'none';
        renderVehicles();
    });
    
    els.clearSearchBtn.addEventListener('click', () => {
        appState.searchQuery = '';
        els.searchInput.value = '';
        els.clearSearchBtn.style.display = 'none';
        renderVehicles();
    });
    
    els.evFilterToggle.addEventListener('click', () => {
        appState.evOnly = !appState.evOnly;
        els.evFilterToggle.classList.toggle('active');
        renderVehicles();
    });
    
    // Profile Forms
    ['profileName', 'profileFlat', 'profilePhone'].forEach(id => {
        document.getElementById(id).addEventListener('input', checkProfileChanges);
    });
    
    els.saveProfileBtn.addEventListener('click', () => {
        appState.profile.name = els.profileName.value;
        appState.profile.block = getActiveProfileBlock();
        appState.profile.flat = els.profileFlat.value;
        appState.profile.phone = els.profilePhone.value;
        
        if (els.profileChangeWarning.style.display === 'block') {
            appState.profile.verified = false; // Reset verification on change
        }
        
        saveState();
        renderAll();
    });
    
    els.resetProfileBtn.addEventListener('click', () => {
        if(confirm('Are you sure you want to clear your profile?')) {
            appState.profile = { ...INIT_PROFILE };
            saveState();
            renderAll();
        }
    });
    
    els.startVerificationBtn.addEventListener('click', () => {
        els.verifyModal.style.display = 'flex';
        els.verifyInputArea.style.display = 'none';
        els.verifyLoading.style.display = 'block';
        els.verifyProgressText.textContent = `Pinging Intercom for ${appState.profile.block}-${appState.profile.flat}...`;
        
        setTimeout(() => {
            els.verifyLoading.style.display = 'none';
            els.verifyInputArea.style.display = 'block';
            els.verifyPin.value = '';
            els.verifyError.style.display = 'none';
            els.verifyModalDesc.textContent = 'Please check your society intercom/app for the 4-digit code.';
        }, 1500);
    });
    
    els.verifyCancelBtn.addEventListener('click', () => els.verifyModal.style.display = 'none');
    
    els.verifySubmitBtn.addEventListener('click', () => {
        if (els.verifyPin.value.length === 4) {
            appState.profile.verified = true;
            saveState();
            renderAll();
            els.verifyModal.style.display = 'none';
        } else {
            els.verifyError.style.display = 'block';
        }
    });
    
    // Host Form
    ['hostModel', 'hostReg', 'hostRent', 'hostHours', 'hostDesc'].forEach(id => {
        document.getElementById(id).addEventListener('input', checkHostFormValid);
    });
    
    els.addVehicleForm.addEventListener('submit', (e) => {
        e.preventDefault();
        if(els.hostSubmitBtn.disabled) return;
        
        const newV = {
            id: 'v_' + Date.now(),
            model: els.hostModel.value,
            type: getActiveHostType(),
            reg: els.hostReg.value,
            rent: parseInt(els.hostRent.value),
            hours: els.hostHours.value,
            desc: els.hostDesc.value,
            isEv: els.hostIsEv.checked,
            ownerName: appState.profile.name,
            ownerBlock: appState.profile.block,
            ownerFlat: appState.profile.flat,
            ownerId: appState.profile.id,
            isRented: false
        };
        
        appState.vehicles.push(newV);
        saveState();
        
        // Reset form
        els.addVehicleForm.reset();
        els.hostIsEv.checked = false;
        checkHostFormValid();
        
        // Update lists
        renderVehicles();
        renderHostListings();
        
        alert('Vehicle successfully listed for rent!');
    });
    
    // Booking Modal
    els.bookCloseBtn.addEventListener('click', () => els.bookModal.style.display = 'none');
    
    els.incHoursBtn.addEventListener('click', () => {
        if(currentBookingHours < 24) {
            currentBookingHours++;
            els.bookHours.textContent = currentBookingHours;
            updateBookingTotals();
        }
    });
    
    els.decHoursBtn.addEventListener('click', () => {
        if(currentBookingHours > 1) {
            currentBookingHours--;
            els.bookHours.textContent = currentBookingHours;
            updateBookingTotals();
        }
    });
    
    els.bookConfirmBtn.addEventListener('click', () => {
        if(!currentBookingVehicle) return;
        
        const booking = {
            id: 'b_' + Date.now(),
            vehicleId: currentBookingVehicle.id,
            vehicleModel: currentBookingVehicle.model,
            lenderId: currentBookingVehicle.ownerId,
            lenderName: currentBookingVehicle.ownerName,
            borrowerId: appState.profile.id,
            borrowerName: appState.profile.name,
            borrowerBlock: appState.profile.block,
            borrowerFlat: appState.profile.flat,
            borrowerPhone: appState.profile.phone,
            hours: currentBookingHours,
            totalPrice: currentBookingVehicle.rent * currentBookingHours,
            notes: els.bookNotes.value,
            status: 'Requested',
            timestamp: Date.now()
        };
        
        appState.bookings.push(booking);
        saveState();
        renderAll();
        
        els.bookModal.style.display = 'none';
        
        // Switch to bookings tab
        els.navItems.forEach(n => n.classList.remove('active'));
        els.tabs.forEach(t => t.style.display = 'none');
        document.querySelector('[data-target="tab-bookings"]').classList.add('active');
        document.getElementById('tab-bookings').style.display = 'block';
    });
    
    // Warning Modal
    els.warningCancelBtn.addEventListener('click', () => els.warningModal.style.display = 'none');
    els.warningGoToProfileBtn.addEventListener('click', () => {
        els.warningModal.style.display = 'none';
        els.navItems.forEach(n => n.classList.remove('active'));
        els.tabs.forEach(t => t.style.display = 'none');
        document.querySelector('[data-target="tab-profile"]').classList.add('active');
        document.getElementById('tab-profile').style.display = 'block';
    });
}

// Start
init();
