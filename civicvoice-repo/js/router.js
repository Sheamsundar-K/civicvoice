// ============================================
// Router — Simple hash-based SPA router
// ============================================

export class Router {
  constructor() {
    this.routes = [];
    this.currentRoute = null;
    window.addEventListener('hashchange', () => this.resolve());
  }

  /** Register a route */
  on(path, handler, { roles = null } = {}) {
    this.routes.push({ path, handler, roles });
    return this;
  }

  /** Navigate to a path */
  navigate(path) {
    window.location.hash = '#' + path;
  }

  /** Resolve current hash to a route */
  resolve() {
    const hash = window.location.hash.slice(1) || '/login';
    
    // Sort routes: exact matches first, parameterized last
    const sorted = [...this.routes].sort((a, b) => {
      const aHasParam = a.path.includes(':');
      const bHasParam = b.path.includes(':');
      if (aHasParam && !bHasParam) return 1;
      if (!aHasParam && bHasParam) return -1;
      return b.path.length - a.path.length; // longer exact paths first
    });

    for (const route of sorted) {
      const match = this.matchRoute(route.path, hash);
      if (match) {
        this.currentRoute = route;
        route.handler(match.params);
        return;
      }
    }

    // 404 fallback
    this.navigate('/login');
  }

  /** Match route pattern to hash, supporting :param syntax */
  matchRoute(pattern, hash) {
    const patternParts = pattern.split('/');
    const hashParts = hash.split('/');

    if (patternParts.length !== hashParts.length) return null;

    const params = {};
    for (let i = 0; i < patternParts.length; i++) {
      if (patternParts[i].startsWith(':')) {
        params[patternParts[i].slice(1)] = hashParts[i];
      } else if (patternParts[i] !== hashParts[i]) {
        return null;
      }
    }
    return { params };
  }
}
