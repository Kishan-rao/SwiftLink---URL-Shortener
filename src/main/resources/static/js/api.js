/**
 * api.js — SwiftLink Centralized API Client
 *
 * Responsibilities:
 *  - Token management (get, set, clear)
 *  - Auth status checks
 *  - HTTP request wrapper with auth headers & 401 auto-redirect
 *  - Namespaced API endpoint methods
 */

const API = (() => {
    const BASE = '';

    // ----------------------------------------------------------
    // Token Management
    // ----------------------------------------------------------

    function getToken() {
        return localStorage.getItem('token');
    }

    function setToken(token) {
        localStorage.setItem('token', token);
    }

    function clearToken() {
        localStorage.removeItem('token');
        localStorage.removeItem('userEmail');
    }

    function isAuthenticated() {
        return !!getToken();
    }

    function getAuthHeaders() {
        const token = getToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    }

    // ----------------------------------------------------------
    // Core HTTP Wrapper
    // ----------------------------------------------------------

    /**
     * Makes an HTTP request to the given endpoint.
     * Automatically attaches auth headers and handles 401 responses.
     *
     * @param {string} endpoint - API path (e.g. '/api/urls')
     * @param {RequestInit} options - Fetch options (method, body, etc.)
     * @returns {Promise<any>} Parsed JSON response body
     * @throws {Error} With a descriptive message on failure
     */
    async function request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...getAuthHeaders(),
            ...(options.headers || {}),
        };

        const response = await fetch(BASE + endpoint, { ...options, headers });

        if (response.status === 401) {
            clearToken();
            window.location.href = '/auth';
            return;
        }

        if (!response.ok) {
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const errorData = await response.json();
                throw new Error(errorData.message || errorData.error || `Error ${response.status}`);
            } else {
                const text = await response.text();
                throw new Error(text || `Error ${response.status}`);
            }
        }

        // Handle empty responses (e.g. DELETE 204)
        const responseContentType = response.headers.get('content-type');
        if (response.status === 204 || !responseContentType) {
            return null;
        }

        return response.json();
    }

    // ----------------------------------------------------------
    // Namespaced Endpoints
    // ----------------------------------------------------------

    const auth = {
        /**
         * @param {string} email
         * @param {string} password
         * @returns {Promise<{token: string}>}
         */
        login(email, password) {
            return request('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password }),
            });
        },

        /**
         * @param {string} name
         * @param {string} email
         * @param {string} password
         * @returns {Promise<any>}
         */
        register(name, email, password) {
            return request('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ name, email, password }),
            });
        },
    };

    const urls = {
        /**
         * Create a new shortened URL.
         * @param {string} url - The long URL to shorten
         * @param {string} [alias] - Optional custom alias
         * @param {number} [ttlHours] - Optional time-to-live in hours
         */
        create(url, alias, ttlHours) {
            const body = { url };
            if (alias) body.alias = alias;
            if (ttlHours) body.ttlHours = ttlHours;
            return request('/api/urls', {
                method: 'POST',
                body: JSON.stringify(body),
            });
        },

        /**
         * Get metadata / analytics for a given short code.
         * @param {string} code
         */
        getMetadata(code) {
            return request(`/api/urls/${code}`);
        },

        /**
         * Get all links belonging to the authenticated user.
         */
        getMyLinks() {
            return request('/api/urls/my');
        },

        /**
         * Delete a link by short code.
         * @param {string} code
         */
        delete(code) {
            return request(`/api/urls/${code}`, { method: 'DELETE' });
        },
    };

    // ----------------------------------------------------------
    // Public API
    // ----------------------------------------------------------

    return {
        getToken,
        setToken,
        clearToken,
        isAuthenticated,
        getAuthHeaders,
        request,
        auth,
        urls,
    };
})();
