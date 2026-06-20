// ============================================
// CivicVoice App — Main Entry Point
// ============================================

import { Router } from './router.js';
import { auth } from './auth.js';
import {
  renderLoginPage,
  renderRegisterPage,
  renderLayout,
  renderIssuesContent,
  renderIssueDetailContent,
  renderPollsContent,
  renderNotificationsContent,
  renderDashboardContent,
  renderManageIssuesContent,
  renderManagePollsContent,
  renderAuditContent,
  renderUsersContent,
} from './pages.js';

// Initialize auth
auth.init();

// Initialize theme
const savedTheme = localStorage.getItem('theme');
if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
  document.documentElement.setAttribute('data-theme', 'dark');
}

const app = document.getElementById('app');
const router = new Router();

// ─── Helper: require auth + optional role check ───
function requireAuth(pageTitle, contentRenderer, allowedRoles = null) {
  if (!auth.isAuthenticated()) {
    router.navigate('/login');
    return;
  }
  if (allowedRoles && !auth.hasRole(...allowedRoles)) {
    // Redirect non-authorized users to their home
    router.navigate(auth.hasRole('ADMIN', 'AUTHORITY') ? '/dashboard' : '/issues');
    return;
  }
  renderLayout(app, router, pageTitle, contentRenderer);
}

// ─── Public Routes ───
router.on('/login', () => {
  if (auth.isAuthenticated()) {
    router.navigate(auth.hasRole('ADMIN', 'AUTHORITY') ? '/dashboard' : '/issues');
    return;
  }
  renderLoginPage(app, router);
});

router.on('/login/citizen', () => {
  if (auth.isAuthenticated() && auth.hasRole('CITIZEN')) { router.navigate('/issues'); return; }
  if (auth.isAuthenticated()) auth.logout();
  renderLoginPage(app, router, 'arjun@example.com');
});

router.on('/login/authority', () => {
  if (auth.isAuthenticated() && auth.hasRole('AUTHORITY', 'ADMIN')) { router.navigate('/dashboard'); return; }
  if (auth.isAuthenticated()) auth.logout();
  renderLoginPage(app, router, 'priya@authority.gov');
});

router.on('/login/admin', () => {
  if (auth.isAuthenticated() && auth.hasRole('ADMIN')) { router.navigate('/dashboard'); return; }
  if (auth.isAuthenticated()) auth.logout();
  renderLoginPage(app, router, 'vikram@admin.gov');
});

router.on('/register', () => {
  if (auth.isAuthenticated()) {
    router.navigate(auth.hasRole('ADMIN', 'AUTHORITY') ? '/dashboard' : '/issues');
    return;
  }
  renderRegisterPage(app, router);
});

// ─── Citizen Routes ───
router.on('/issues', () => {
  renderLayout(app, router, 'Issues', renderIssuesContent);
});

router.on('/issues/:id', (params) => {
  renderLayout(app, router, 'Issue Detail', (el, r) => renderIssueDetailContent(el, r, params.id));
});

router.on('/polls', () => {
  requireAuth('Polls', renderPollsContent);
});

router.on('/notifications', () => {
  requireAuth('Notifications', renderNotificationsContent);
});

// ─── Authority Routes ───
router.on('/dashboard', () => {
  requireAuth('Dashboard', renderDashboardContent, ['AUTHORITY', 'ADMIN']);
});

router.on('/issues/manage', () => {
  requireAuth('Manage Issues', renderManageIssuesContent, ['AUTHORITY', 'ADMIN']);
});

router.on('/polls/manage', () => {
  requireAuth('Manage Polls', renderManagePollsContent, ['AUTHORITY', 'ADMIN']);
});

// ─── Admin Routes ───
router.on('/audit', () => {
  requireAuth('Audit Trail', renderAuditContent, ['ADMIN']);
});

router.on('/users', () => {
  requireAuth('Users', renderUsersContent, ['ADMIN']);
});

// ─── Start routing ───
router.resolve();
