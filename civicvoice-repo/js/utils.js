// ============================================
// Utility helpers
// ============================================

/** Show a toast notification */
export function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const iconMap = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ',
  };
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${iconMap[type] || 'ℹ'}</span>
    <span>${message}</span>
  `;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

/** Format a date string to a human-readable relative time */
export function timeAgo(dateStr) {
  const now = new Date();
  const date = new Date(dateStr);
  const diff = Math.floor((now - date) / 1000);
  if (diff < 60) return 'Just now';
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} hours ago`;
  if (diff < 604800) return `${Math.floor(diff / 86400)} days ago`;
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/** Format date to readable string */
export function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

/** Get initials from name */
export function getInitials(name) {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
}

/** Capitalize first letter */
export function capitalize(str) {
  return str ? str.charAt(0).toUpperCase() + str.slice(1).toLowerCase() : '';
}

/** Format category enum to readable */
export function formatCategory(cat) {
  return cat ? cat.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()) : '';
}

/** Format status enum to CSS class-safe */
export function statusClass(status) {
  return status ? status.toLowerCase().replace(/ /g, '_') : '';
}

/** Open Google Maps for coordinates */
export function openGoogleMaps(lat, lng) {
  const url = `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
  window.open(url, '_blank');
}

/** Simple modal helper */
export function showModal(contentHTML) {
  const overlay = document.getElementById('modal-overlay');
  overlay.innerHTML = contentHTML;
  overlay.classList.remove('hidden');
  // Close on overlay click
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) hideModal();
  });
}

export function hideModal() {
  const overlay = document.getElementById('modal-overlay');
  overlay.classList.add('hidden');
  overlay.innerHTML = '';
}

/** Debounce */
export function debounce(fn, delay = 300) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

/** Animate a number counting up */
export function animateCount(element, target, duration = 800) {
  const start = 0;
  const startTime = performance.now();
  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
    const current = Math.round(start + (target - start) * eased);
    element.textContent = current.toLocaleString();
    if (progress < 1) requestAnimationFrame(update);
  }
  requestAnimationFrame(update);
}
