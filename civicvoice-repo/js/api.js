// ============================================
// API Client
// ============================================

export const API_BASE_URL = 'http://localhost:8080/api/v1';

export async function fetchWithAuth(endpoint, options = {}) {
    const authData = JSON.parse(localStorage.getItem('civicvoice_auth') || '{}');
    const token = authData.token;
    
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        let errorData = {};
        try {
            errorData = await response.json();
        } catch(e) {
            errorData = { message: 'An error occurred during the request.' };
        }
        throw new Error(errorData.message || response.statusText);
    }

    const text = await response.text();
    return text ? JSON.parse(text) : null;
}
