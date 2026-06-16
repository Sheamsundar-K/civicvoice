// ============================================
// Page Renderers — All page rendering functions
// ============================================

import { icons } from './icons.js';
import { auth } from './auth.js';
import {
  showToast, timeAgo, formatDate, getInitials, capitalize,
  formatCategory, statusClass, openGoogleMaps, showModal, hideModal,
  animateCount,
} from './utils.js';
import { fetchWithAuth } from './api.js';


// ─────────────────────────────────────────────
// LOGIN PAGE
// ─────────────────────────────────────────────
export function renderLoginPage(app, router, defaultEmail = '') {
  app.innerHTML = `
    <div class="auth-page">
      <div class="auth-container">
        <div class="auth-logo">
          <div class="auth-logo-icon">${icons.shield}</div>
          <h1>CivicVoice</h1>
          <p>Empowering Citizens, Transforming Governance</p>
        </div>
        <div class="auth-card">
          <h2>Welcome Back</h2>
          <div id="auth-error" class="auth-error hidden"></div>
          <form id="login-form" class="auth-form">
            <div class="form-group">
              <label for="login-email">Email Address</label>
              <input type="email" id="login-email" placeholder="you@example.com" value="${defaultEmail}" required />
            </div>
            <div class="form-group">
              <label for="login-password">Password</label>
              <input type="password" id="login-password" placeholder="Enter your password" required />
            </div>
            <button type="submit" class="btn btn-primary" id="login-submit-btn" style="width:100%;">Sign In</button>
          </form>
          <div class="auth-divider">
            Don't have an account?<a href="#/register">Sign up</a>
          </div>
        </div>
      </div>
    </div>
  `;

  document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = document.getElementById('login-submit-btn');
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    submitBtn.disabled = true;
    submitBtn.textContent = 'Signing in...';
    const result = await auth.login(email, password);
    if (result.success) {
      showToast(`Welcome back, ${auth.user.name}!`, 'success');
      const role = auth.getRole();
      if (role === 'ADMIN' || role === 'AUTHORITY') {
        router.navigate('/dashboard');
      } else {
        router.navigate('/issues');
      }
    } else {
      const errorEl = document.getElementById('auth-error');
      errorEl.classList.remove('hidden');
      errorEl.textContent = result.error || 'Invalid email or password.';
      submitBtn.disabled = false;
      submitBtn.textContent = 'Sign In';
    }
  });
}


// ─────────────────────────────────────────────
// REGISTER PAGE
// ─────────────────────────────────────────────
export function renderRegisterPage(app, router) {
  app.innerHTML = `
    <div class="auth-page">
      <div class="auth-container">
        <div class="auth-logo">
          <div class="auth-logo-icon">${icons.shield}</div>
          <h1>CivicVoice</h1>
          <p>Join the civic engagement movement</p>
        </div>
        <div class="auth-card">
          <h2>Create Account</h2>
          <div id="auth-error" class="auth-error hidden"></div>
          <form id="register-form" class="auth-form">
            <div class="form-group">
              <label for="reg-name">Full Name</label>
              <input type="text" id="reg-name" placeholder="Enter your name" required />
            </div>
            <div class="form-group">
              <label for="reg-email">Email Address</label>
              <input type="email" id="reg-email" placeholder="you@example.com" required />
            </div>
            <div class="form-group">
              <label for="reg-password">Password</label>
              <input type="password" id="reg-password" placeholder="Create a password" required minlength="6" />
            </div>
            <button type="submit" class="btn btn-primary" id="register-submit-btn">Create Account</button>
          </form>
          <div class="auth-divider">
            Already have an account?<a href="#/login">Sign in</a>
          </div>
        </div>
      </div>
    </div>
  `;

  document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = document.getElementById('register-submit-btn');
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating account...';
    const result = await auth.register(name, email, password);
    if (result.success) {
      showToast('Account created successfully!', 'success');
      router.navigate('/issues');
    } else {
      const errorEl = document.getElementById('auth-error');
      errorEl.classList.remove('hidden');
      errorEl.textContent = result.error || 'Registration failed. Please try again.';
      submitBtn.disabled = false;
      submitBtn.textContent = 'Create Account';
    }
  });
}


// ─────────────────────────────────────────────
// APP LAYOUT (sidebar + topbar shell)
// ─────────────────────────────────────────────
export function renderLayout(app, router, pageTitle, contentRenderer) {
  const user = auth.user;
  const role = auth.getRole();
  const unread = 0; // TODO: fetch real unread notifications count if needed

  const citizenNav = `
    <div class="nav-section-title">Citizen</div>
    <div class="nav-item ${pageTitle === 'Issues' ? 'active' : ''}" data-nav="/issues">
      ${icons.issues}<span>Issues</span>
    </div>
    <div class="nav-item ${pageTitle === 'Polls' ? 'active' : ''}" data-nav="/polls">
      ${icons.polls}<span>Polls</span>
    </div>
    <div class="nav-item ${pageTitle === 'Notifications' ? 'active' : ''}" data-nav="/notifications">
      ${icons.bell}<span>Notifications</span>
      ${unread > 0 ? `<span class="nav-badge">${unread}</span>` : ''}
    </div>
  `;

  const authorityNav = `
    <div class="nav-section-title">Authority</div>
    <div class="nav-item ${pageTitle === 'Dashboard' ? 'active' : ''}" data-nav="/dashboard">
      ${icons.dashboard}<span>Dashboard</span>
    </div>
    <div class="nav-item ${pageTitle === 'Manage Issues' ? 'active' : ''}" data-nav="/issues/manage">
      ${icons.manage}<span>Manage Issues</span>
    </div>
    <div class="nav-item ${pageTitle === 'Manage Polls' ? 'active' : ''}" data-nav="/polls/manage">
      ${icons.polls}<span>Manage Polls</span>
    </div>
  `;

  const adminNav = `
    <div class="nav-section-title">Admin</div>
    <div class="nav-item ${pageTitle === 'Audit Trail' ? 'active' : ''}" data-nav="/audit">
      ${icons.audit}<span>Audit Trail</span>
    </div>
    <div class="nav-item ${pageTitle === 'Users' ? 'active' : ''}" data-nav="/users">
      ${icons.users}<span>Users</span>
    </div>
  `;

  app.innerHTML = `
    <div class="app-layout">
      <aside class="sidebar" id="sidebar">
        <div class="sidebar-header">
          <div class="sidebar-logo">${icons.shield}</div>
          <span class="sidebar-brand">CivicVoice</span>
        </div>
        <nav class="sidebar-nav">
          ${citizenNav}
          ${(role === 'AUTHORITY' || role === 'ADMIN') ? authorityNav : ''}
          ${role === 'ADMIN' ? adminNav : ''}
        </nav>
        <div class="sidebar-footer">
          <div class="sidebar-user" id="sidebar-user-btn">
            <div class="sidebar-avatar">${getInitials(user?.name || 'A')}</div>
            <div class="sidebar-user-info">
              <div class="sidebar-user-name">${user?.name || 'Anonymous Citizen'}</div>
              <div class="sidebar-user-role">${capitalize(user?.role || 'Guest')}</div>
            </div>
          </div>
          <div class="nav-item" id="logout-btn" style="margin-top: 4px; color: ${user ? 'var(--error)' : 'var(--accent-blue)'};">
            ${user ? icons.logout + '<span>Sign Out</span>' : '🔑<span>Sign In</span>'}
          </div>
        </div>
      </aside>
      <main class="main-area">
        <header class="topbar">
          <div class="topbar-left">
            <button class="mobile-menu-btn" id="mobile-menu-btn">${icons.menu}</button>
            <h2 class="topbar-title">${pageTitle}</h2>
          </div>
          <div class="topbar-right">
            <div class="topbar-search">
              ${icons.search}
              <input type="text" placeholder="Search..." id="topbar-search-input" />
            </div>
            <div class="notification-bell" id="notification-bell-btn">
              ${icons.bell}
              ${unread > 0 ? `<span class="notification-count">${unread}</span>` : ''}
              <span class="live-pulse" title="Live connection active"></span>
            </div>
          </div>
        </header>
        <section class="content" id="page-content">
        </section>
      </main>
    </div>
  `;

  // Render page content
  const contentEl = document.getElementById('page-content');
  contentRenderer(contentEl, router);

  // Navigation handlers
  document.querySelectorAll('[data-nav]').forEach(item => {
    item.addEventListener('click', () => {
      router.navigate(item.dataset.nav);
    });
  });

  // Logout / Login toggle
  document.getElementById('logout-btn').addEventListener('click', () => {
    if (user) {
      auth.logout();
      showToast('Signed out successfully.', 'info');
    }
    router.navigate('/login');
  });

  // Mobile menu
  const mobileBtn = document.getElementById('mobile-menu-btn');
  if (mobileBtn) {
    mobileBtn.addEventListener('click', () => {
      document.getElementById('sidebar').classList.toggle('open');
    });
  }

  // Notification bell → go to notifications page
  document.getElementById('notification-bell-btn').addEventListener('click', () => {
    router.navigate('/notifications');
  });
}


// ─────────────────────────────────────────────
// ISSUES PAGE (Citizen)
// ─────────────────────────────────────────────
export function renderIssuesContent(el, router) {
  let viewMode = 'grid';
  let filterStatus = '';
  let filterCategory = '';
  let issues = [];
  let loading = true;

  async function loadIssues() {
    loading = true;
    render();
    try {
      const params = new URLSearchParams();
      if (filterStatus) params.set('status', filterStatus);
      if (filterCategory) params.set('category', filterCategory);
      params.set('size', '50');
      const data = await fetchWithAuth('/issues?' + params.toString());
      issues = data.content || [];
    } catch(e) {
      showToast('Failed to load issues: ' + e.message, 'error');
      issues = [];
    }
    loading = false;
    render();
  }

  function render() {

    el.innerHTML = `
      <div class="page-header">
        <div>
          <h1>Community Issues</h1>
          <p>Report, track, and upvote civic issues in your area</p>
        </div>
        <div class="page-actions">
          <div class="view-toggle">
            <button class="view-toggle-btn ${viewMode === 'grid' ? 'active' : ''}" id="view-grid">
              ${icons.list} List
            </button>
            <button class="view-toggle-btn ${viewMode === 'map' ? 'active' : ''}" id="view-map">
              ${icons.map} Map
            </button>
          </div>
          <button class="btn btn-primary" id="report-issue-btn">
            ${icons.plus} Report Issue
          </button>
        </div>
      </div>

      <div class="issue-filters">
        <select id="filter-status">
          <option value="">All Statuses</option>
          <option value="OPEN" ${filterStatus === 'OPEN' ? 'selected' : ''}>Open</option>
          <option value="ASSIGNED" ${filterStatus === 'ASSIGNED' ? 'selected' : ''}>Assigned</option>
          <option value="IN_PROGRESS" ${filterStatus === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
          <option value="RESOLVED" ${filterStatus === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
          <option value="CLOSED" ${filterStatus === 'CLOSED' ? 'selected' : ''}>Closed</option>
        </select>
        <select id="filter-category">
          <option value="">All Categories</option>
          <option value="ROAD" ${filterCategory === 'ROAD' ? 'selected' : ''}>Road</option>
          <option value="WATER" ${filterCategory === 'WATER' ? 'selected' : ''}>Water</option>
          <option value="ELECTRICITY" ${filterCategory === 'ELECTRICITY' ? 'selected' : ''}>Electricity</option>
          <option value="SANITATION" ${filterCategory === 'SANITATION' ? 'selected' : ''}>Sanitation</option>
          <option value="STREET_LIGHT" ${filterCategory === 'STREET_LIGHT' ? 'selected' : ''}>Street Light</option>
          <option value="SEWAGE" ${filterCategory === 'SEWAGE' ? 'selected' : ''}>Sewage</option>
          <option value="PARK" ${filterCategory === 'PARK' ? 'selected' : ''}>Park</option>
          <option value="NOISE" ${filterCategory === 'NOISE' ? 'selected' : ''}>Noise</option>
          <option value="ILLEGAL_CONSTRUCTION" ${filterCategory === 'ILLEGAL_CONSTRUCTION' ? 'selected' : ''}>Illegal Construction</option>
          <option value="OTHER" ${filterCategory === 'OTHER' ? 'selected' : ''}>Other</option>
        </select>
        <span class="text-sm text-muted" style="margin-left: auto;">${issues.length} issues found</span>
      </div>

      ${viewMode === 'grid' ? (loading ? '<div class="empty-state"><div class="spinner"></div><h3>Loading issues...</h3></div>' : renderIssueCards(issues)) : renderMapView()}
    `;

    // Bind events
    document.getElementById('view-grid')?.addEventListener('click', () => { viewMode = 'grid'; render(); });
    document.getElementById('view-map')?.addEventListener('click', () => { viewMode = 'map'; render(); });
    document.getElementById('filter-status')?.addEventListener('change', (e) => { filterStatus = e.target.value; loadIssues(); });
    document.getElementById('filter-category')?.addEventListener('change', (e) => { filterCategory = e.target.value; loadIssues(); });
    document.getElementById('report-issue-btn')?.addEventListener('click', () => showReportIssueModal(loadIssues));

    // Issue card clicks
    document.querySelectorAll('.issue-card').forEach(card => {
      card.addEventListener('click', (e) => {
        if (e.target.closest('.upvote-btn') || e.target.closest('.view-on-maps-btn')) return;
        router.navigate('/issues/' + card.dataset.id);
      });
    });

    // Upvote clicks
    document.querySelectorAll('.upvote-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const id = btn.dataset.id;
        try {
          const res = await fetchWithAuth(`/issues/${id}/upvote`, { method: 'POST' });
          const issue = issues.find(i => i.id === id);
          if (issue) {
            issue.upvoteCount = res.upvoteCount;
            issue.upvotedByCurrentUser = res.upvoted;
            btn.classList.toggle('upvoted', res.upvoted);
            btn.querySelector('.upvote-count').textContent = res.upvoteCount;
          }
        } catch(err) {
          showToast('Could not upvote: ' + err.message, 'error');
        }
      });
    });

    // View on maps
    document.querySelectorAll('.view-on-maps-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        openGoogleMaps(btn.dataset.lat, btn.dataset.lng);
      });
    });
  }

  loadIssues();
}

function renderIssueCards(issues) {
  if (!issues.length) {
    return `<div class="empty-state">${icons.issues}<h3>No issues found</h3><p>Try adjusting your filters or report a new issue.</p></div>`;
  }
  return `
    <div class="issues-grid">
      ${issues.map((issue, i) => `
        <div class="issue-card" data-id="${issue.id}" style="animation-delay: ${i * 0.05}s">
          <div class="issue-card-header">
            <span class="issue-card-title">${issue.title}</span>
            <span class="badge badge-priority-${(issue.priority || 'MEDIUM').toLowerCase()}">${issue.priority || 'MEDIUM'}</span>
          </div>
          <div class="issue-card-meta">
            <span class="badge badge-status-${statusClass(issue.status)}">${issue.status.replace(/_/g, ' ')}</span>
            <span class="issue-card-category">${formatCategory(issue.category)}</span>
          </div>
          <p class="issue-card-description">${issue.description}</p>
          <div class="issue-card-footer">
            <div class="issue-card-actions">
              <button class="upvote-btn ${issue.upvotedByCurrentUser ? 'upvoted' : ''}" data-id="${issue.id}">
                ${icons.thumbsUp}
                <span class="upvote-count">${issue.upvoteCount}</span>
              </button>
              <span class="comment-count">
                ${icons.messageCircle}
                ${issue.commentCount}
              </span>
              <button class="view-on-maps-btn" data-lat="${issue.latitude}" data-lng="${issue.longitude}" title="View on Google Maps">
                ${icons.externalLink} Maps
              </button>
            </div>
            <span class="issue-card-time">${timeAgo(issue.createdAt)}</span>
          </div>
        </div>
      `).join('')}
    </div>
  `;
}

function renderMapView() {
  return `
    <div class="map-container">
      <div class="map-placeholder">
        ${icons.map}
        <h3>Map View</h3>
        <p style="font-size: var(--font-sm); max-width: 300px; text-align: center;">
          Connect your Google Maps API key to see issues plotted on an interactive map.
        </p>
      </div>
    </div>
  `;
}

function showReportIssueModal(onSuccess) {
  showModal(`
    <div class="modal">
      <div class="modal-header">
        <h2>${icons.plus} Report New Issue</h2>
        <button class="modal-close" id="close-modal">${icons.close}</button>
      </div>
      <form id="report-issue-form" class="auth-form">
        <div class="form-group">
          <label for="issue-title">Title</label>
          <input type="text" id="issue-title" placeholder="Brief description of the issue (min 10 chars)" required />
        </div>
        <div class="form-group">
          <label for="issue-description">Description</label>
          <textarea id="issue-description" placeholder="Detailed description of the problem... (min 20 chars)" required></textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label for="issue-category">Category</label>
            <select id="issue-category" required>
              <option value="">Select category</option>
              <option value="ROAD">Road</option>
              <option value="WATER">Water</option>
              <option value="ELECTRICITY">Electricity</option>
              <option value="SANITATION">Sanitation</option>
              <option value="STREET_LIGHT">Street Light</option>
              <option value="SEWAGE">Sewage</option>
              <option value="PARK">Park</option>
              <option value="NOISE">Noise</option>
              <option value="ILLEGAL_CONSTRUCTION">Illegal Construction</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div class="form-group">
            <label for="issue-priority">Priority</label>
            <select id="issue-priority" required>
              <option value="">Select priority</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM" selected>Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
        </div>

        <div style="border: 1.5px solid var(--border-subtle); border-radius: var(--radius-md); padding: var(--space-md); margin-bottom: var(--space-md);">
          <div style="display:flex; align-items:center; gap:8px; margin-bottom: 10px;">
            ${icons.mapPin}
            <span style="font-weight:600; font-size: var(--font-sm);">Location</span>
          </div>
          <button type="button" class="btn btn-secondary btn-sm" id="get-location-btn" style="width:100%; margin-bottom:10px;">
            📍 Auto-detect My Location (fills address automatically)
          </button>
          <div id="location-status" class="text-sm" style="display:none; margin-bottom:10px; padding:8px; border-radius:6px; background: rgba(59,130,246,0.08);"></div>
          <div class="form-row">
            <div class="form-group">
              <label for="issue-address">Address</label>
              <input type="text" id="issue-address" placeholder="Street address" />
            </div>
            <div class="form-group">
              <label for="issue-ward">Ward / Area</label>
              <input type="text" id="issue-ward" placeholder="e.g. Ward 5" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="issue-city">City</label>
              <input type="text" id="issue-city" value="Bangalore" />
            </div>
            <div class="form-group">
              <label for="issue-pincode">PIN Code</label>
              <input type="text" id="issue-pincode" placeholder="e.g. 560001" />
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>Photos / Videos (optional, max 5 files)</label>
          <div id="upload-drop-zone" style="border: 2px dashed var(--border-subtle); border-radius: var(--radius-md); padding: 24px; text-align:center; cursor:pointer; transition: border-color 0.2s; background: var(--bg-primary);">
            <div style="color: var(--text-muted); font-size: var(--font-sm);">
              📎 Click to select or drag &amp; drop photos here<br/>
              <span style="font-size: 0.72rem; color: var(--text-muted);">JPEG, PNG, WebP, MP4 — max 10MB each</span>
            </div>
            <input type="file" id="issue-files" multiple accept="image/jpeg,image/png,image/webp,video/mp4" style="display:none;" />
          </div>
          <div id="file-preview-list" style="display:flex; flex-wrap:wrap; gap:8px; margin-top:8px;"></div>
          <div id="upload-status" class="text-sm text-muted" style="display:none; margin-top:4px;"></div>
        </div>

        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
          <input type="checkbox" id="issue-anonymous" style="width: auto;" />
          <label for="issue-anonymous" style="font-size: var(--font-sm); color: var(--text-secondary); cursor: pointer;">Report anonymously</label>
        </div>
        <button type="submit" class="btn btn-primary">Submit Issue</button>
      </form>
    </div>
  `);

  document.getElementById('close-modal').addEventListener('click', hideModal);

  // ── Location: GPS + OpenStreetMap reverse geocoding ──
  let capturedLat = null, capturedLng = null;
  const locationBtn = document.getElementById('get-location-btn');
  const statusEl = document.getElementById('location-status');

  locationBtn.addEventListener('click', () => {
    if (!navigator.geolocation) {
      statusEl.style.display = 'block';
      statusEl.textContent = '❌ Geolocation not supported by this browser.';
      return;
    }
    locationBtn.textContent = '⏳ Getting GPS location...';
    locationBtn.disabled = true;
    statusEl.style.display = 'block';
    statusEl.textContent = 'Requesting location from your device...';

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        capturedLat = pos.coords.latitude;
        capturedLng = pos.coords.longitude;
        statusEl.textContent = `📡 Got coordinates (${capturedLat.toFixed(5)}, ${capturedLng.toFixed(5)}) — fetching address...`;

        try {
          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${capturedLat}&lon=${capturedLng}&format=json&addressdetails=1`,
            { headers: { 'Accept-Language': 'en', 'User-Agent': 'CivicVoiceApp/1.0' } }
          );
          const geo = await res.json();
          const addr = geo.address || {};

          const road = addr.road || addr.pedestrian || addr.path || '';
          const suburb = addr.suburb || addr.neighbourhood || addr.quarter || addr.village || '';
          const city = addr.city || addr.town || addr.county || 'Bangalore';
          const pincode = addr.postcode || '';
          const ward = addr.suburb || addr.quarter || suburb || '';

          if (road || suburb) document.getElementById('issue-address').value = [road, suburb].filter(Boolean).join(', ');
          document.getElementById('issue-city').value = city;
          if (pincode) document.getElementById('issue-pincode').value = pincode;
          if (ward) document.getElementById('issue-ward').value = ward;

          locationBtn.textContent = '✅ Location Detected';
          statusEl.innerHTML = `✅ <strong>${geo.display_name || `${capturedLat.toFixed(4)}, ${capturedLng.toFixed(4)}`}</strong>`;
        } catch {
          locationBtn.textContent = '✅ Coordinates Captured';
          statusEl.textContent = `✅ Coordinates saved (${capturedLat.toFixed(4)}, ${capturedLng.toFixed(4)}). Please fill the address fields manually.`;
        }
      },
      (err) => {
        locationBtn.textContent = '📍 Auto-detect My Location';
        locationBtn.disabled = false;
        statusEl.style.display = 'block';
        const msg = err.code === 1 ? 'Permission denied — please allow location access in your browser.' :
                    err.code === 2 ? 'Location unavailable. Enter address manually.' :
                    'Location request timed out. Enter address manually.';
        statusEl.textContent = '❌ ' + msg;
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
    );
  });

  // ── Image Upload: drag-and-drop + preview ──
  const dropZone = document.getElementById('upload-drop-zone');
  const fileInput = document.getElementById('issue-files');
  const previewList = document.getElementById('file-preview-list');
  const uploadStatus = document.getElementById('upload-status');
  let selectedFiles = [];

  dropZone.addEventListener('click', () => fileInput.click());
  dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.style.borderColor = 'var(--accent-blue)'; });
  dropZone.addEventListener('dragleave', () => { dropZone.style.borderColor = ''; });
  dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.style.borderColor = '';
    addFiles([...e.dataTransfer.files]);
  });
  fileInput.addEventListener('change', () => { addFiles([...fileInput.files]); fileInput.value = ''; });

  function addFiles(newFiles) {
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'video/mp4'];
    for (const f of newFiles) {
      if (selectedFiles.length >= 5) { showToast('Max 5 files allowed.', 'warning'); break; }
      if (!allowed.includes(f.type)) { showToast(`${f.name}: unsupported type.`, 'warning'); continue; }
      if (f.size > 10 * 1024 * 1024) { showToast(`${f.name}: exceeds 10MB.`, 'warning'); continue; }
      selectedFiles.push(f);
    }
    renderPreviews();
  }

  function renderPreviews() {
    previewList.innerHTML = '';
    selectedFiles.forEach((f, i) => {
      const wrap = document.createElement('div');
      wrap.style.cssText = 'position:relative;display:inline-block;';
      if (f.type.startsWith('image/')) {
        const img = document.createElement('img');
        img.src = URL.createObjectURL(f);
        img.style.cssText = 'width:72px;height:72px;object-fit:cover;border-radius:6px;border:1.5px solid var(--border-subtle);display:block;';
        wrap.appendChild(img);
      } else {
        const vid = document.createElement('div');
        vid.style.cssText = 'width:72px;height:72px;border-radius:6px;border:1.5px solid var(--border-subtle);background:var(--bg-primary);display:flex;align-items:center;justify-content:center;font-size:1.8rem;';
        vid.textContent = '🎥';
        wrap.appendChild(vid);
      }
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.textContent = '✕';
      removeBtn.style.cssText = 'position:absolute;top:-6px;right:-6px;background:var(--error);color:#fff;border:none;border-radius:50%;width:18px;height:18px;cursor:pointer;font-size:10px;line-height:18px;text-align:center;padding:0;';
      removeBtn.addEventListener('click', () => { selectedFiles.splice(i, 1); renderPreviews(); });
      wrap.appendChild(removeBtn);
      previewList.appendChild(wrap);
    });
  }

  // ── Submit ──
  document.getElementById('report-issue-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = e.target.querySelector('[type="submit"]');
    submitBtn.textContent = 'Submitting...';
    submitBtn.disabled = true;

    const title = document.getElementById('issue-title').value.trim();
    const description = document.getElementById('issue-description').value.trim();
    if (title.length < 10) { showToast('Title must be at least 10 characters.', 'warning'); submitBtn.disabled = false; submitBtn.textContent = 'Submit Issue'; return; }
    if (description.length < 20) { showToast('Description must be at least 20 characters.', 'warning'); submitBtn.disabled = false; submitBtn.textContent = 'Submit Issue'; return; }

    // Upload images first
    let mediaUrls = [];
    if (selectedFiles.length > 0) {
      uploadStatus.style.display = 'block';
      uploadStatus.textContent = `⬆ Uploading ${selectedFiles.length} file(s)...`;
      try {
        const formData = new FormData();
        selectedFiles.forEach(f => formData.append('files', f));
        const authData = JSON.parse(localStorage.getItem('civicvoice_auth') || '{}');
        const headers = authData.token ? { 'Authorization': `Bearer ${authData.token}` } : {};
        const uploadRes = await fetch('http://localhost:8080/api/v1/upload/multiple', {
          method: 'POST',
          headers: headers,
          body: formData,
        });
        if (!uploadRes.ok) throw new Error(`Upload error: ${uploadRes.status}`);
        const uploadData = await uploadRes.json();
        mediaUrls = (uploadData.files || []).map(f => f.url).filter(Boolean);
        if (!mediaUrls.length && uploadData.url) mediaUrls = [uploadData.url];
        uploadStatus.textContent = `✅ ${mediaUrls.length} file(s) uploaded.`;
      } catch (uploadErr) {
        showToast('Image upload failed — submitting without images.', 'warning');
        uploadStatus.textContent = '⚠ Upload failed, continuing without images.';
      }
    }

    const payload = {
      title,
      description,
      category: document.getElementById('issue-category').value,
      priority: document.getElementById('issue-priority').value || 'MEDIUM',
      latitude: capturedLat ?? 12.9716,
      longitude: capturedLng ?? 77.5946,
      address: document.getElementById('issue-address').value,
      ward: document.getElementById('issue-ward').value,
      city: document.getElementById('issue-city').value || 'Bangalore',
      state: 'Karnataka',
      pinCode: document.getElementById('issue-pincode').value,
      anonymous: document.getElementById('issue-anonymous').checked,
      mediaUrls,
    };

    try {
      await fetchWithAuth('/issues', { method: 'POST', body: JSON.stringify(payload) });
      hideModal();
      showToast('Issue reported successfully! 🎉', 'success');
      onSuccess();
    } catch (err) {
      showToast('Failed to submit: ' + err.message, 'error');
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit Issue';
    }
  });
}


// ─────────────────────────────────────────────
// ISSUE DETAIL PAGE
// ─────────────────────────────────────────────
export function renderIssueDetailContent(el, router, issueId) {
  el.innerHTML = `<div class="empty-state"><div class="spinner"></div><h3>Loading issue details...</h3></div>`;

  async function loadIssue() {
    try {
      const issue = await fetchWithAuth('/issues/' + issueId);
      
      // Support missing comments/upvotes in response structure gracefully
      const comments = issue.comments || [];
      const upvoteCount = issue.upvoteCount || 0;
      const commentCount = issue.commentCount || comments.length;
      
      const reporterName = issue.anonymous ? 'Anonymous' : (issue.reporter?.name || 'Citizen');
      const mediaHtml = (issue.mediaUrls || issue.media || []).map(m => {
        const url = m.url || m;
        if (url.endsWith('.mp4')) {
          return `<video src="${url}" controls style="max-width: 100%; border-radius: var(--radius-md); margin-top: 12px;"></video>`;
        }
        return `<img src="${url}" style="max-width: 100%; border-radius: var(--radius-md); margin-top: 12px; display: block; border: 1.5px solid var(--border-subtle);" alt="Issue media" />`;
      }).join('');

      el.innerHTML = `
        <div class="issue-detail animate-fade-in">
          <button class="btn btn-ghost" id="back-to-issues">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="15 18 9 12 15 6"/></svg>
            Back to Issues
          </button>
          
          <div class="issue-detail-header">
            <h1>${issue.title}</h1>
            <div class="issue-detail-badges">
              <span class="badge badge-status-${statusClass(issue.status)}">${issue.status.replace(/_/g, ' ')}</span>
              <span class="badge badge-priority-${(issue.priority || 'MEDIUM').toLowerCase()}">${issue.priority || 'MEDIUM'}</span>
              <span class="issue-card-category">${formatCategory(issue.category)}</span>
              ${issue.anonymous ? '<span class="badge" style="background: rgba(107,114,128,0.15); color: var(--text-muted);">Anonymous</span>' : ''}
            </div>
          </div>

          <div class="issue-detail-body">
            ${issue.description}
            ${mediaHtml}
          </div>

          <div class="issue-detail-info">
            <div class="info-item">
              <div class="info-item-label">Location</div>
              <div class="info-item-value">${issue.address || 'Location provided via map'}</div>
            </div>
            <div class="info-item">
              <div class="info-item-label">Ward</div>
              <div class="info-item-value">${issue.ward || 'N/A'}</div>
            </div>
            <div class="info-item">
              <div class="info-item-label">City</div>
              <div class="info-item-value">${issue.city || 'Bangalore'}</div>
            </div>
            <div class="info-item">
              <div class="info-item-label">Reported</div>
              <div class="info-item-value">${formatDate(issue.createdAt || new Date().toISOString())}</div>
            </div>
            <div class="info-item">
              <div class="info-item-label">Upvotes</div>
              <div class="info-item-value" style="color: var(--accent-blue);">${upvoteCount}</div>
            </div>
            <div class="info-item">
              <div class="info-item-label">Reported By</div>
              <div class="info-item-value">${reporterName}</div>
            </div>
          </div>

          <button class="view-maps-detail-btn" id="view-maps-detail-btn" data-lat="${issue.latitude}" data-lng="${issue.longitude}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
            View on Google Maps
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
          </button>

          <div class="comments-section">
            <h3>
              <span style="display:inline-flex;width:18px;height:18px;flex-shrink:0;">${icons.messageCircle}</span>
              Comments
              <span style="background:var(--bg-input);border:1.5px solid var(--border-subtle);border-radius:var(--radius-full);padding:1px 9px;font-size:var(--font-xs);font-weight:600;color:var(--text-muted);">${commentCount}</span>
            </h3>
            ${comments.length === 0 ? `
              <div class="comments-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="36" height="36"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
                <p>No comments yet. Be the first to comment on this issue.</p>
              </div>
            ` : comments.map(c => `
              <div class="comment">
                <div class="comment-avatar">${getInitials(c.authorName || 'User')}</div>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-author">${c.authorName || 'User'}</span>
                    ${c.isOfficial ? '<span class="badge badge-role-authority">Official</span>' : ''}
                    <span class="comment-time">${timeAgo(c.createdAt || new Date().toISOString())}</span>
                  </div>
                  <p class="comment-text">${c.text}</p>
                </div>
              </div>
            `).join('')}
            <div class="comment-input-box">
              <input type="text" id="comment-input" placeholder="Write a comment..." />
              <button class="btn btn-primary btn-sm" id="post-comment-btn">${icons.send}</button>
            </div>
          </div>
        </div>
      `;

      document.getElementById('back-to-issues').addEventListener('click', () => router.navigate('/issues'));
      
      document.getElementById('view-maps-detail-btn')?.addEventListener('click', () => {
        openGoogleMaps(issue.latitude, issue.longitude);
      });

      document.getElementById('post-comment-btn').addEventListener('click', async () => {
        const input = document.getElementById('comment-input');
        const text = input.value.trim();
        if (!text) return;
        
        try {
          await fetchWithAuth(`/issues/${issueId}/comments`, {
            method: 'POST',
            body: JSON.stringify({ text })
          });
          showToast('Comment posted!', 'success');
          loadIssue(); // reload to show new comment
        } catch(err) {
          showToast('Failed to post comment: ' + err.message, 'error');
        }
      });

    } catch(err) {
      el.innerHTML = `<div class="empty-state">${icons.issues}<h3>Issue not found</h3><p>Could not load issue details: ${err.message}</p></div>`;
      
      const backBtn = document.createElement('button');
      backBtn.className = 'btn btn-primary';
      backBtn.style.marginTop = '16px';
      backBtn.textContent = 'Back to Issues';
      backBtn.onclick = () => router.navigate('/issues');
      el.querySelector('.empty-state').appendChild(backBtn);
    }
  }

  loadIssue();
}

export function renderPollsContent(el, router) {
  let activeTab = 'active';
  const selectedOptions = {}; // pollId -> optionId
  let polls = [];

  async function loadPolls() {
    el.innerHTML = `<div class="empty-state"><div class="spinner"></div><h3>Loading polls...</h3></div>`;
    try {
      // Use /polls for citizens (public), /polls/all for authority/admin
      const role = auth.getRole();
      const endpoint = (role === 'AUTHORITY' || role === 'ADMIN') ? '/polls/all?size=50' : '/polls?size=50';
      const response = await fetchWithAuth(endpoint);
      polls = response.content || [];
      render();
    } catch(err) {
      el.innerHTML = `<div class="empty-state">${icons.polls}<h3>Error loading polls</h3><p>${err.message}</p></div>`;
    }
  }

  function render() {
    const role = auth.getRole();
    const canManagePolls = role === 'AUTHORITY' || role === 'ADMIN';
    const activePolls = polls.filter(p => !p.closed);
    const closedPolls = polls.filter(p => p.closed);
    const displayPolls = activeTab === 'active' ? activePolls : closedPolls;

    el.innerHTML = `
      <div class="page-header">
        <div>
          <h1>Public Polls</h1>
          <p>Have your say on civic decisions that matter</p>
        </div>
        <div class="page-actions">
          ${canManagePolls ? `<button class="btn btn-primary" id="create-poll-btn">${icons.plus} Create Poll</button>` : ''}
        </div>
      </div>

      <div class="tabs" style="margin-bottom: var(--space-xl);">
        <div class="tab ${activeTab === 'active' ? 'active' : ''}" data-tab="active">Active (${activePolls.length})</div>
        <div class="tab ${activeTab === 'closed' ? 'active' : ''}" data-tab="closed">Closed (${closedPolls.length})</div>
      </div>

      <div class="polls-grid">
        ${displayPolls.map(poll => renderPollCard(poll, selectedOptions[poll.id])).join('')}
        ${displayPolls.length === 0 ? `<div class="empty-state">${icons.polls}<h3>No ${activeTab} polls</h3><p>Check back later for new polls.</p></div>` : ''}
      </div>
    `;

    // Create poll button (authority/admin)
    document.getElementById('create-poll-btn')?.addEventListener('click', () => showCreatePollModal(loadPolls));

    // Tab switching
    document.querySelectorAll('.tab').forEach(tab => {
      tab.addEventListener('click', () => { activeTab = tab.dataset.tab; render(); });
    });

    // Poll option clicks (unvoted only)
    document.querySelectorAll('.poll-option').forEach(opt => {
      opt.addEventListener('click', () => {
        const pollId = opt.dataset.pollid;
        const optId = opt.dataset.optid;
        selectedOptions[pollId] = optId;
        // Update UI
        document.querySelectorAll(`.poll-option[data-pollid="${pollId}"]`).forEach(o => o.classList.remove('selected'));
        opt.classList.add('selected');
      });
    });

    // Submit vote
    document.querySelectorAll('.submit-vote-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const pollId = btn.dataset.pollid;
        const optId = selectedOptions[pollId];
        if (!optId) { showToast('Please select an option first.', 'warning'); return; }
        
        try {
          const btnContent = btn.innerHTML;
          btn.innerHTML = `<div class="spinner" style="width:16px;height:16px;"></div>`;
          btn.disabled = true;
          
          await fetchWithAuth(`/polls/${pollId}/vote`, {
            method: 'POST',
            body: JSON.stringify({ optionId: optId })
          });
          
          showToast('Vote submitted! Thank you for participating. 🗳️', 'success');
          loadPolls(); // Reload from server to get accurate percentages
        } catch(err) {
          showToast('Failed to submit vote: ' + err.message, 'error');
          btn.disabled = false;
          btn.innerHTML = 'Submit Vote';
        }
      });
    });
  }

  loadPolls();
}

function renderPollCard(poll, selectedOptId) {
  const hasVoted = poll.hasVoted;
  const isClosed = poll.closed;
  const showResults = hasVoted || isClosed;
  const totalVotes = poll.totalVotes || 0;
  const options = poll.options || [];

  return `
    <div class="poll-card">
      <div class="poll-card-header">
        <div class="poll-card-status">
          ${isClosed
            ? '<span class="poll-closed-badge">Closed</span>'
            : '<span class="poll-active-badge">Live</span>'}
          <span class="text-sm text-muted">${poll.createdBy || 'Authority'}</span>
        </div>
        <h3 class="poll-card-question">${poll.question}</h3>
        <p class="poll-card-description">${poll.description || ''}</p>
      </div>

      ${showResults ? `
        <div class="poll-results">
          ${options.map(opt => `
            <div class="poll-result-item ${poll.votedOptionId === opt.id ? 'user-voted' : ''}">
              <div class="poll-result-header">
                <span class="poll-result-label">
                  ${opt.optionText} ${poll.votedOptionId === opt.id ? '<span class="your-vote-indicator">Your vote</span>' : ''}
                </span>
                <span class="poll-result-percentage">${(opt.percentage || 0).toFixed(1)}%</span>
              </div>
              <div class="poll-result-bar">
                <div class="poll-result-bar-fill" style="width: ${opt.percentage || 0}%"></div>
              </div>
              <div class="poll-result-count">${opt.voteCount || 0} votes</div>
            </div>
          `).join('')}
        </div>
      ` : `
        <div class="poll-options">
          ${options.map(opt => `
            <div class="poll-option ${selectedOptId === opt.id ? 'selected' : ''}" data-pollid="${poll.id}" data-optid="${opt.id}">
              <div class="poll-radio"></div>
              <span class="poll-option-label">${opt.optionText}</span>
            </div>
          `).join('')}
        </div>
        <button class="btn btn-primary submit-vote-btn" data-pollid="${poll.id}" style="width: 100%;">
          ${icons.vote} Submit Vote
        </button>
      `}

      <div class="poll-card-footer">
        <span class="poll-total-votes">${icons.users} ${totalVotes.toLocaleString()} votes</span>
        <span class="poll-expiry">${isClosed ? 'Ended ' + formatDate(poll.expiresAt) : 'Ends ' + formatDate(poll.expiresAt)}</span>
      </div>
    </div>
  `;
}


// ─────────────────────────────────────────────
// NOTIFICATIONS PAGE
// ─────────────────────────────────────────────
export function renderNotificationsContent(el, router) {
  let notifications = [];

  async function loadNotifications() {
    el.innerHTML = `<div class="empty-state"><div class="spinner"></div><h3>Loading notifications...</h3></div>`;
    try {
      const response = await fetchWithAuth('/notifications?size=50');
      notifications = response.content || [];
      render();
    } catch(err) {
      el.innerHTML = `<div class="empty-state">${icons.alertTriangle}<h3>Error loading notifications</h3><p>${err.message}</p></div>`;
    }
  }

  function render() {
    el.innerHTML = `
      <div class="page-header">
        <div>
          <h1>Notifications</h1>
          <p>Stay updated on your reported issues and civic activity</p>
        </div>
        <div class="page-actions">
          <div class="sse-status connected">
            <span class="sse-dot"></span>
            Live Connection
          </div>
          <button class="btn btn-ghost btn-sm" id="mark-all-read">Mark all as read</button>
        </div>
      </div>

      <div class="notifications-list">
        ${notifications.map(notif => {
          const typeStr = notif.type ? notif.type.toLowerCase() : 'info';
          const iconType = typeStr.includes('status') ? 'status' : typeStr.includes('comment') ? 'comment' : typeStr.includes('assign') ? 'assignment' : typeStr.includes('sla') ? 'sla' : 'info';
          const iconSvg = typeStr.includes('status') ? icons.activity : typeStr.includes('comment') ? icons.messageCircle : typeStr.includes('assign') ? icons.target : icons.alertTriangle;
          return `
            <div class="notification-item ${notif.read ? '' : 'unread'}" data-id="${notif.id}">
              <div class="notification-icon ${iconType}">${iconSvg}</div>
              <div class="notification-content">
                <div class="notification-title">${notif.title}</div>
                <div class="notification-description">${notif.message || notif.description}</div>
              </div>
              <span class="notification-time">${timeAgo(notif.createdAt)}</span>
            </div>
          `;
        }).join('')}
        ${notifications.length === 0 ? `<div class="empty-state">${icons.bell}<h3>No notifications</h3><p>You're all caught up!</p></div>` : ''}
      </div>
    `;

    document.getElementById('mark-all-read')?.addEventListener('click', async () => {
      try {
        await fetchWithAuth('/notifications/read-all', { method: 'PUT' });
        showToast('All notifications marked as read.', 'success');
        loadNotifications();
      } catch(err) {
        showToast('Failed to mark read: ' + err.message, 'error');
      }
    });

    document.querySelectorAll('.notification-item.unread').forEach(item => {
      item.addEventListener('click', async () => {
        const id = item.dataset.id;
        try {
          await fetchWithAuth(`/notifications/${id}/read`, { method: 'PUT' });
          item.classList.remove('unread');
        } catch(err) {
          console.error('Failed to mark single notification as read', err);
        }
      });
    });
  }

  loadNotifications();
}


// ─────────────────────────────────────────────
// DASHBOARD PAGE (Authority)
// ─────────────────────────────────────────────
export async function renderDashboardContent(el, router) {
  el.innerHTML = `
    <div class="page-header">
      <div>
        <h1>Analytics Dashboard</h1>
        <p>Real-time overview of civic issue management performance</p>
      </div>
    </div>
    <div class="stats-grid">
      <div class="stat-card blue">
        <div class="stat-card-icon">${icons.issues}</div>
        <div class="stat-card-value" id="count-total">…</div>
        <div class="stat-card-label">Total Issues</div>
      </div>
      <div class="stat-card teal">
        <div class="stat-card-icon">${icons.checkCircle}</div>
        <div class="stat-card-value" id="count-resolved">…</div>
        <div class="stat-card-label">Resolved</div>
      </div>
      <div class="stat-card amber">
        <div class="stat-card-icon">${icons.alertTriangle}</div>
        <div class="stat-card-value" id="count-open">…</div>
        <div class="stat-card-label">Open Issues</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-card-icon">${icons.activity}</div>
        <div class="stat-card-value" id="count-inprog">…</div>
        <div class="stat-card-label">In Progress</div>
      </div>
    </div>

    <div class="dashboard-panel full-width" style="margin-top: var(--space-xl);">
      <div class="panel-header">
        <div class="panel-title">Recent Issues</div>
        <div class="panel-subtitle">Latest issues submitted by citizens</div>
      </div>
      <div id="recent-issues-table" style="padding: var(--space-lg);">Loading…</div>
    </div>
  `;

  try {
    const [allData, openData, resolvedData, inProgData] = await Promise.all([
      fetchWithAuth('/issues?size=5&sortBy=createdAt'),
      fetchWithAuth('/issues?status=OPEN&size=1'),
      fetchWithAuth('/issues?status=RESOLVED&size=1'),
      fetchWithAuth('/issues?status=IN_PROGRESS&size=1'),
    ]);

    const total = allData.totalElements ?? 0;
    const open = openData.totalElements ?? 0;
    const resolved = resolvedData.totalElements ?? 0;
    const inProg = inProgData.totalElements ?? 0;

    animateCount(document.getElementById('count-total'), total);
    animateCount(document.getElementById('count-open'), open);
    animateCount(document.getElementById('count-resolved'), resolved);
    animateCount(document.getElementById('count-inprog'), inProg);

    const recentIssues = allData.content || [];
    const tableEl = document.getElementById('recent-issues-table');
    if (!tableEl) return;
    if (!recentIssues.length) {
      tableEl.innerHTML = '<div class="empty-state" style="padding: 32px;">No issues reported yet.</div>';
      return;
    }
    tableEl.innerHTML = `
      <table class="manage-issues-table">
        <thead><tr><th>Title</th><th>Category</th><th>Priority</th><th>Status</th><th>Reported</th></tr></thead>
        <tbody>
          ${recentIssues.map(issue => `
            <tr>
              <td><div class="font-medium text-sm" style="max-width:250px">${issue.title}</div></td>
              <td><span class="issue-card-category">${formatCategory(issue.category)}</span></td>
              <td><span class="badge badge-priority-${(issue.priority||'MEDIUM').toLowerCase()}">${issue.priority||'MEDIUM'}</span></td>
              <td><span class="badge badge-status-${statusClass(issue.status)}">${issue.status.replace(/_/g,' ')}</span></td>
              <td class="text-sm text-muted">${timeAgo(issue.createdAt)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } catch (err) {
    showToast('Failed to load dashboard data: ' + err.message, 'error');
  }
}



function drawTrendChart(trends) {
  const canvas = document.getElementById('trend-chart');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const rect = canvas.parentElement.getBoundingClientRect();
  canvas.width = rect.width * 2;
  canvas.height = rect.height * 2;
  canvas.style.width = rect.width + 'px';
  canvas.style.height = rect.height + 'px';
  ctx.scale(2, 2);

  const w = rect.width;
  const h = rect.height;
  const pad = { top: 20, right: 20, bottom: 40, left: 50 };
  const chartW = w - pad.left - pad.right;
  const chartH = h - pad.top - pad.bottom;

  const allVals = [...trends.submitted, ...trends.resolved];
  const maxVal = Math.max(...allVals) * 1.15;

  function xPos(i) { return pad.left + (i / (trends.labels.length - 1)) * chartW; }
  function yPos(v) { return pad.top + chartH - (v / maxVal) * chartH; }

  // Grid lines
  ctx.strokeStyle = 'rgba(255,255,255,0.06)';
  ctx.lineWidth = 1;
  for (let i = 0; i <= 4; i++) {
    const y = pad.top + (chartH / 4) * i;
    ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(w - pad.right, y); ctx.stroke();
    ctx.fillStyle = '#64748b';
    ctx.font = '11px Inter';
    ctx.textAlign = 'right';
    ctx.fillText(Math.round(maxVal - (maxVal / 4) * i), pad.left - 8, y + 4);
  }

  // X labels
  ctx.fillStyle = '#64748b';
  ctx.font = '11px Inter';
  ctx.textAlign = 'center';
  trends.labels.forEach((label, i) => {
    if (i % 2 === 0) ctx.fillText(label, xPos(i), h - pad.bottom + 20);
  });

  // Submitted area gradient
  const gradBlue = ctx.createLinearGradient(0, pad.top, 0, pad.top + chartH);
  gradBlue.addColorStop(0, 'rgba(59, 130, 246, 0.3)');
  gradBlue.addColorStop(1, 'rgba(59, 130, 246, 0.0)');
  ctx.fillStyle = gradBlue;
  ctx.beginPath();
  ctx.moveTo(xPos(0), yPos(0));
  trends.submitted.forEach((v, i) => ctx.lineTo(xPos(i), yPos(v)));
  ctx.lineTo(xPos(trends.submitted.length - 1), pad.top + chartH);
  ctx.lineTo(xPos(0), pad.top + chartH);
  ctx.closePath();
  ctx.fill();

  // Submitted line
  ctx.strokeStyle = '#3b82f6';
  ctx.lineWidth = 2.5;
  ctx.lineJoin = 'round';
  ctx.beginPath();
  trends.submitted.forEach((v, i) => { i === 0 ? ctx.moveTo(xPos(i), yPos(v)) : ctx.lineTo(xPos(i), yPos(v)); });
  ctx.stroke();

  // Resolved area gradient
  const gradGreen = ctx.createLinearGradient(0, pad.top, 0, pad.top + chartH);
  gradGreen.addColorStop(0, 'rgba(16, 185, 129, 0.2)');
  gradGreen.addColorStop(1, 'rgba(16, 185, 129, 0.0)');
  ctx.fillStyle = gradGreen;
  ctx.beginPath();
  ctx.moveTo(xPos(0), yPos(0));
  trends.resolved.forEach((v, i) => ctx.lineTo(xPos(i), yPos(v)));
  ctx.lineTo(xPos(trends.resolved.length - 1), pad.top + chartH);
  ctx.lineTo(xPos(0), pad.top + chartH);
  ctx.closePath();
  ctx.fill();

  // Resolved line
  ctx.strokeStyle = '#10b981';
  ctx.lineWidth = 2.5;
  ctx.beginPath();
  trends.resolved.forEach((v, i) => { i === 0 ? ctx.moveTo(xPos(i), yPos(v)) : ctx.lineTo(xPos(i), yPos(v)); });
  ctx.stroke();

  // Data points
  trends.submitted.forEach((v, i) => {
    ctx.fillStyle = '#3b82f6';
    ctx.beginPath(); ctx.arc(xPos(i), yPos(v), 3, 0, Math.PI * 2); ctx.fill();
  });
  trends.resolved.forEach((v, i) => {
    ctx.fillStyle = '#10b981';
    ctx.beginPath(); ctx.arc(xPos(i), yPos(v), 3, 0, Math.PI * 2); ctx.fill();
  });

  // Legend
  ctx.font = '12px Inter';
  const lx = w - pad.right - 180;
  ctx.fillStyle = '#3b82f6';
  ctx.fillRect(lx, 8, 12, 12); ctx.fillStyle = '#94a3b8'; ctx.fillText('Submitted', lx + 18, 18);
  ctx.fillStyle = '#10b981';
  ctx.fillRect(lx + 95, 8, 12, 12); ctx.fillStyle = '#94a3b8'; ctx.fillText('Resolved', lx + 113, 18);
}


// ─────────────────────────────────────────────
// MANAGE ISSUES PAGE (Authority)
// ─────────────────────────────────────────────
export async function renderManageIssuesContent(el, router) {
  let issues = [];
  let filterStatus = '';
  let filterPriority = '';

  async function loadIssues() {
    const params = new URLSearchParams({ size: '100', sortBy: 'createdAt' });
    if (filterStatus) params.set('status', filterStatus);
    const data = await fetchWithAuth('/issues?' + params.toString());
    issues = data.content || [];
  }

  function renderTable() {
    const tbody = document.getElementById('issues-tbody');
    if (!tbody) return;
    let display = issues;
    if (filterStatus) display = display.filter(i => i.status === filterStatus);
    if (filterPriority) display = display.filter(i => i.priority === filterPriority);
    if (!display.length) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:32px;color:var(--text-muted);">No issues found.</td></tr>`;
      return;
    }
    tbody.innerHTML = display.map(issue => `
      <tr class="audit-row" data-id="${issue.id}">
        <td>
          <div style="max-width: 250px;">
            <div class="font-medium text-sm">${issue.title}</div>
            <div class="text-sm text-muted" style="margin-top: 2px;">${issue.anonymous ? 'Anonymous' : (issue.reporter?.fullName || '—')}</div>
          </div>
        </td>
        <td><span class="issue-card-category">${formatCategory(issue.category)}</span></td>
        <td><span class="badge badge-priority-${(issue.priority||'MEDIUM').toLowerCase()}">${issue.priority||'MEDIUM'}</span></td>
        <td class="text-sm">${issue.ward || '—'}</td>
        <td>
          <select class="status-select" data-id="${issue.id}">
            ${['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED','REJECTED'].map(s =>
              `<option value="${s}" ${issue.status === s ? 'selected' : ''}>${s.replace(/_/g, ' ')}</option>`
            ).join('')}
          </select>
        </td>
        <td class="text-sm text-muted">${timeAgo(issue.createdAt)}</td>
        <td>
          <button class="btn btn-ghost btn-sm view-issue-btn" data-id="${issue.id}" title="View">${icons.eye}</button>
        </td>
      </tr>
    `).join('');

    document.querySelectorAll('.status-select').forEach(sel => {
      sel.addEventListener('change', async (e) => {
        const id = sel.dataset.id;
        const newStatus = e.target.value;
        try {
          await fetchWithAuth(`/issues/${id}/status`, {
            method: 'PUT',
            body: JSON.stringify({ newStatus, note: 'Updated by authority' })
          });
          // Use string comparison since dataset.id is always a string
          const issue = issues.find(i => String(i.id) === String(id));
          if (issue) issue.status = newStatus;
          showToast(`Status updated to ${newStatus.replace(/_/g,' ')}`, 'success');
        } catch(err) {
          showToast('Update failed: ' + err.message, 'error');
          const issue = issues.find(i => String(i.id) === String(id));
          sel.value = issue?.status || '';
        }
      });
    });
    document.querySelectorAll('.view-issue-btn').forEach(btn => {
      btn.addEventListener('click', () => router.navigate('/issues/' + btn.dataset.id));
    });
  }

  el.innerHTML = `
    <div class="page-header">
      <div>
        <h1>Manage Issues</h1>
        <p>Assign, update, and resolve civic issues</p>
      </div>
    </div>
    <div class="issue-filters">
      <select id="manage-filter-status">
        <option value="">All Statuses</option>
        <option value="OPEN">Open</option>
        <option value="ASSIGNED">Assigned</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="RESOLVED">Resolved</option>
        <option value="CLOSED">Closed</option>
      </select>
      <select id="manage-filter-priority">
        <option value="">All Priorities</option>
        <option value="LOW">Low</option>
        <option value="MEDIUM">Medium</option>
        <option value="HIGH">High</option>
        <option value="CRITICAL">Critical</option>
      </select>
    </div>
    <div class="table-container">
      <table class="manage-issues-table">
        <thead>
          <tr>
            <th>Issue</th><th>Category</th><th>Priority</th><th>Ward</th><th>Status</th><th>Reported</th><th>Actions</th>
          </tr>
        </thead>
        <tbody id="issues-tbody"><tr><td colspan="7" style="text-align:center;padding:32px;">Loading…</td></tr></tbody>
      </table>
    </div>
  `;

  document.getElementById('manage-filter-status').addEventListener('change', (e) => { filterStatus = e.target.value; renderTable(); });
  document.getElementById('manage-filter-priority').addEventListener('change', (e) => { filterPriority = e.target.value; renderTable(); });

  try {
    await loadIssues();
    renderTable();
  } catch(err) {
    showToast('Failed to load issues: ' + err.message, 'error');
  }
}


// ─────────────────────────────────────────────
// MANAGE POLLS PAGE (Authority)
// ─────────────────────────────────────────────
export function renderManagePollsContent(el, router) {
  let polls = [];

  async function loadPolls() {
    el.innerHTML = `<div class="empty-state"><div class="spinner"></div><h3>Loading polls...</h3></div>`;
    try {
      const response = await fetchWithAuth('/polls/all?size=100');
      polls = response.content || [];
      render();
    } catch(err) {
      el.innerHTML = `<div class="empty-state">${icons.alertTriangle}<h3>Error loading polls</h3><p>${err.message}</p></div>`;
    }
  }

  function render() {
    el.innerHTML = `
      <div class="page-header">
        <div>
          <h1>Manage Polls</h1>
          <p>Create, manage, and close public polls</p>
        </div>
        <div class="page-actions">
          <button class="btn btn-primary" id="create-poll-btn">${icons.plus} Create Poll</button>
        </div>
      </div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>Question</th>
              <th>Status</th>
              <th>Votes</th>
              <th>Created By</th>
              <th>Expires</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${polls.map(poll => `
              <tr>
                <td style="max-width: 300px;">
                  <div class="font-medium text-sm">${poll.question}</div>
                </td>
                <td>
                  ${!poll.closed
                    ? '<span class="poll-active-badge">Live</span>'
                    : '<span class="poll-closed-badge">Closed</span>'}
                </td>
                <td class="font-medium">${(poll.totalVotes || 0).toLocaleString()}</td>
                <td class="text-sm text-muted">${poll.createdBy || 'Authority'}</td>
                <td class="text-sm text-muted">${formatDate(poll.expiresAt)}</td>
                <td>
                  ${!poll.closed ? `
                    <button class="btn btn-danger btn-sm close-poll-btn" data-id="${poll.id}">Close</button>
                  ` : '<span class="text-sm text-muted">—</span>'}
                </td>
              </tr>
            `).join('')}
            ${polls.length === 0 ? `<tr><td colspan="6" style="text-align: center; padding: 32px;">No polls found.</td></tr>` : ''}
          </tbody>
        </table>
      </div>
    `;

    document.getElementById('create-poll-btn')?.addEventListener('click', () => showCreatePollModal(loadPolls));

    document.querySelectorAll('.close-poll-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          await fetchWithAuth(`/polls/${id}/close`, { method: 'PUT' });
          showToast('Poll closed.', 'info');
          loadPolls();
        } catch(err) {
          showToast('Failed to close poll: ' + err.message, 'error');
        }
      });
    });
  }

  loadPolls();
}

function showCreatePollModal(onSuccess) {
  let optionCount = 2;
  function renderOptions() {
    return Array.from({ length: optionCount }, (_, i) => `
      <div class="poll-option-input-row">
        <input type="text" placeholder="Option ${i + 1}" class="poll-opt-input" required />
        ${i >= 2 ? `<button type="button" class="remove-option-btn" data-idx="${i}">${icons.close}</button>` : ''}
      </div>
    `).join('');
  }

  showModal(`
    <div class="modal">
      <div class="modal-header">
        <h2>${icons.polls} Create New Poll</h2>
        <button class="modal-close" id="close-modal">${icons.close}</button>
      </div>
      <form id="create-poll-form" class="auth-form">
        <div class="form-group">
          <label>Question</label>
          <input type="text" id="poll-question" placeholder="What would you like to ask?" required />
        </div>
        <div class="form-group">
          <label>Description</label>
          <textarea id="poll-desc" placeholder="Provide context for the poll..."></textarea>
        </div>
        <div class="form-group">
          <label>Options</label>
          <div class="poll-option-input-list" id="poll-options-list">${renderOptions()}</div>
          <button type="button" class="btn btn-ghost btn-sm" id="add-option-btn" style="margin-top: 8px;">${icons.plus} Add Option</button>
        </div>
        <div class="form-group">
          <label>Expiry Date</label>
          <input type="date" id="poll-expiry" required />
        </div>
        <button type="submit" class="btn btn-primary">Create Poll</button>
      </form>
    </div>
  `);

  document.getElementById('close-modal').addEventListener('click', hideModal);

  document.getElementById('add-option-btn').addEventListener('click', () => {
    optionCount++;
    document.getElementById('poll-options-list').innerHTML = renderOptions();
  });

  document.getElementById('create-poll-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const inputs = document.querySelectorAll('.poll-opt-input');
    const options = Array.from(inputs).map(inp => inp.value.trim()).filter(v => v);
    
    if (options.length < 2) {
      showToast('A poll must have at least 2 options.', 'warning');
      return;
    }

    const payload = {
      question: document.getElementById('poll-question').value.trim(),
      description: document.getElementById('poll-desc').value.trim(),
      options: options,
      expiresAt: new Date(document.getElementById('poll-expiry').value).toISOString(),
    };

    try {
      const submitBtn = e.target.querySelector('button[type="submit"]');
      submitBtn.disabled = true;
      submitBtn.innerHTML = `<div class="spinner" style="width:16px;height:16px;"></div>`;
      
      await fetchWithAuth('/polls', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      
      showToast('Poll created successfully!', 'success');
      hideModal();
      onSuccess();
    } catch(err) {
      showToast('Failed to create poll: ' + err.message, 'error');
      const submitBtn = e.target.querySelector('button[type="submit"]');
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Create Poll';
    }
  });
}


// ─────────────────────────────────────────────
// AUDIT PAGE (Admin)
// ─────────────────────────────────────────────
export function renderAuditContent(el, router) {
  const expandedRows = new Set();
  let logs = [];

  async function loadLogs() {
    el.innerHTML = `<div class="empty-state"><div class="spinner"></div><h3>Loading audit logs...</h3></div>`;
    try {
      const response = await fetchWithAuth('/audit?size=100&sort=timestamp,desc');
      logs = response.content || [];
      render();
    } catch(err) {
      el.innerHTML = `<div class="empty-state">${icons.alertTriangle}<h3>Error loading logs</h3><p>${err.message}</p></div>`;
    }
  }

  function render() {
    el.innerHTML = `
      <div class="page-header">
        <div>
          <h1>Audit Trail</h1>
          <p>Immutable log of all system mutations for transparency</p>
        </div>
      </div>

      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th></th>
              <th>Timestamp</th>
              <th>Action</th>
              <th>Entity Type</th>
              <th>Entity ID</th>
              <th>Actor</th>
            </tr>
          </thead>
          <tbody>
            ${logs.map(log => {
              const actionClass = log.action.includes('CREATED') ? 'create'
                : log.action.includes('DELETE') ? 'delete'
                : log.action.includes('STATUS') ? 'status' : 'update';
              const isExpanded = expandedRows.has(log.id);
              return `
                <tr class="audit-row" data-id="${log.id}">
                  <td style="width: 30px; cursor: pointer;" class="audit-toggle" data-id="${log.id}">
                    <span style="transition: transform 0.2s; display: inline-block; ${isExpanded ? 'transform: rotate(90deg);' : ''}">${icons.chevronRight}</span>
                  </td>
                  <td class="text-sm">${formatDate(log.timestamp)}</td>
                  <td><span class="audit-action ${actionClass}">${log.action.replace(/_/g, ' ')}</span></td>
                  <td class="text-sm font-medium">${log.entityType}</td>
                  <td><span class="audit-entity-id" title="${log.entityId}">${log.entityId}</span></td>
                  <td class="text-sm text-muted">${log.actor}</td>
                </tr>
                ${isExpanded ? `
                  <tr>
                    <td colspan="6" style="padding: 0;">
                      <div class="audit-expand">
                        <div class="audit-diff">
                          <div class="audit-diff-panel old">
                            <h4>Old Value</h4>
                            <div class="audit-diff-content">${log.oldValue ? JSON.stringify(log.oldValue, null, 2) : '—  (created)'}</div>
                          </div>
                          <div class="audit-diff-panel new">
                            <h4>New Value</h4>
                            <div class="audit-diff-content">${log.newValue ? JSON.stringify(log.newValue, null, 2) : '—  (deleted)'}</div>
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>
                ` : ''}
              `;
            }).join('')}
            ${logs.length === 0 ? `<tr><td colspan="6" style="text-align: center; padding: 32px;">No audit logs found.</td></tr>` : ''}
          </tbody>
        </table>
      </div>
    `;

    document.querySelectorAll('.audit-toggle').forEach(toggle => {
      toggle.addEventListener('click', () => {
        const id = toggle.dataset.id;
        if (expandedRows.has(id)) expandedRows.delete(id);
        else expandedRows.add(id);
        render();
      });
    });
  }

  loadLogs();
}


// ─────────────────────────────────────────────
// USERS PAGE (Admin)
// ─────────────────────────────────────────────
export function renderUsersContent(el, router) {
  let users = [];

  async function loadUsers() {
    el.innerHTML = `<div class="empty-state"><div class="spinner"></div><h3>Loading users...</h3></div>`;
    try {
      const response = await fetchWithAuth('/users?size=100');
      users = response.content || [];
      render();
    } catch(err) {
      el.innerHTML = `<div class="empty-state">${icons.alertTriangle}<h3>Error loading users</h3><p>${err.message}</p></div>`;
    }
  }

  function render() {
    el.innerHTML = `
      <div class="page-header">
        <div>
          <h1>User Management</h1>
          <p>Manage user accounts and role assignments</p>
        </div>
      </div>

      <div class="users-grid">
        ${users.map(user => `
          <div class="user-card">
            <div class="user-card-avatar">${getInitials(user.name)}</div>
            <div class="user-card-info">
              <div class="user-card-name">${user.name}</div>
              <div class="user-card-email">${user.email}</div>
              <div style="display: flex; align-items: center; gap: 6px; margin-top: 4px;">
                <span class="badge badge-role-${user.role.toLowerCase()}">${user.role}</span>
                <span class="status-dot ${user.isActive ? 'active' : 'inactive'}"></span>
                <span class="text-sm text-muted">${user.isActive ? 'Active' : 'Inactive'}</span>
              </div>
            </div>
            <div class="user-card-actions">
              <!-- Actions disabled in live mode as role updates require complex logic -->
            </div>
          </div>
        `).join('')}
        ${users.length === 0 ? `<div class="empty-state" style="grid-column: 1/-1">No users found.</div>` : ''}
      </div>
    `;
  }

  loadUsers();
}
