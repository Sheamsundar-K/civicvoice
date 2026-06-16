// ============================================
// Auth State — Uses Real Backend API
// ============================================

import { fetchWithAuth } from './api.js';

const AUTH_KEY = 'civicvoice_auth';

export const auth = {
  user: null,
  token: null,

  init() {
    const stored = localStorage.getItem(AUTH_KEY);
    if (stored) {
      try {
        const data = JSON.parse(stored);
        this.user = data.user;
        this.token = data.token;
      } catch { this.logout(); }
    }
  },

  async login(email, password) {
    try {
      const data = await fetchWithAuth('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      // Backend returns accessToken, user.fullName, user.role
      this.token = data.accessToken;
      this.user = {
        ...data.user,
        name: data.user.fullName,  // normalize for UI
      };
      localStorage.setItem(AUTH_KEY, JSON.stringify({ user: this.user, token: this.token }));
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  },

  async register(name, email, password) {
    try {
      const data = await fetchWithAuth('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ fullName: name, email, password })
      });
      // Backend returns accessToken, user.fullName, user.role
      this.token = data.accessToken;
      this.user = {
        ...data.user,
        name: data.user.fullName,  // normalize for UI
      };
      localStorage.setItem(AUTH_KEY, JSON.stringify({ user: this.user, token: this.token }));
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  },

  logout() {
    this.user = null;
    this.token = null;
    localStorage.removeItem(AUTH_KEY);
  },

  isAuthenticated() {
    return !!this.token;
  },

  getRole() {
    return this.user?.role || null;
  },

  hasRole(...roles) {
    return roles.includes(this.getRole());
  },

  getAuthHeader() {
    return this.token ? `Bearer ${this.token}` : '';
  },
};
