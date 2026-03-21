/**
 * common.js — SwiftLink Shared UI Utilities
 *
 * Utilities shared across multiple pages:
 *  - Clipboard helpers
 *  - URL code extraction
 *  - Time formatting (timeAgo)
 *  - Nav auth state rendering
 */

// ----------------------------------------------------------
// Clipboard
// ----------------------------------------------------------

/**
 * Copies the given text to the clipboard and briefly shows a
 * feedback element by removing its 'hidden' class.
 *
 * @param {string} text - Text to copy
 * @param {HTMLElement} feedbackEl - Element to show on success
 * @param {number} [durationMs=2000] - How long to show feedback
 */
function copyToClipboard(text, feedbackEl, durationMs = 2000) {
    navigator.clipboard.writeText(text).then(() => {
        if (feedbackEl) {
            feedbackEl.classList.remove('hidden');
            setTimeout(() => feedbackEl.classList.add('hidden'), durationMs);
        }
    }).catch(err => console.error('Failed to copy:', err));
}

// ----------------------------------------------------------
// URL helpers
// ----------------------------------------------------------

/**
 * Extracts the short code from a short URL string.
 * e.g. "https://example.com/s/abc123" → "abc123"
 *
 * @param {string} shortUrl
 * @returns {string}
 * @throws {Error} if a valid code cannot be determined
 */
function extractCodeFromShortUrl(shortUrl) {
    if (!shortUrl) {
        throw new Error('Short URL was not returned by the server');
    }
    const trimmed = shortUrl.trim();
    const segments = trimmed.split('/').filter(s => s.length > 0);
    const code = segments.at(-1);
    if (!code || code === 's') {
        throw new Error(`Unable to determine short code from URL: ${shortUrl}`);
    }
    return code;
}

/**
 * Builds a fully-qualified, origin-correct short URL.
 * Uses the current window origin so it works in all environments.
 *
 * @param {string} shortUrl - The server-returned short URL value
 * @param {string} [code] - Optional fallback code
 * @returns {string}
 */
function buildPublicShortUrl(shortUrl, code) {
    const trimmed = shortUrl?.trim();
    if (!trimmed) {
        return `${window.location.origin}/s/${code}`;
    }
    try {
        const parsed = new URL(trimmed);
        if (parsed.host) return trimmed;
    } catch (_) {
        // Fall back to current origin
    }
    const resolvedCode = code || extractCodeFromShortUrl(trimmed);
    return `${window.location.origin}/s/${resolvedCode}`;
}

// ----------------------------------------------------------
// Time formatting
// ----------------------------------------------------------

/**
 * Returns a human-readable "time ago" string.
 *
 * @param {Date|number} date - Date object or timestamp
 * @returns {string}
 */
function timeAgo(date) {
    const seconds = Math.floor((Date.now() - new Date(date).getTime()) / 1000);
    const intervals = [
        { label: 'year', seconds: 31536000 },
        { label: 'month', seconds: 2592000 },
        { label: 'day', seconds: 86400 },
        { label: 'hour', seconds: 3600 },
        { label: 'minute', seconds: 60 },
    ];
    for (const { label, seconds: s } of intervals) {
        const count = Math.floor(seconds / s);
        if (count >= 1) return `${count} ${label}${count !== 1 ? 's' : ''} ago`;
    }
    return 'Just now';
}

// ----------------------------------------------------------
// Nav auth state
// ----------------------------------------------------------

/**
 * Updates the navbar to show the correct auth state.
 * Expects elements with ids: navAuth and navUser.
 */
function renderNavAuthState() {
    const navAuth = document.getElementById('navAuth');
    const navUser = document.getElementById('navUser');
    if (!navAuth || !navUser) return;

    if (API.isAuthenticated()) {
        navAuth.classList.add('hidden');
        navUser.classList.remove('hidden');
    } else {
        navAuth.classList.remove('hidden');
        navUser.classList.add('hidden');
    }
}

/**
 * Logs the current user out and reloads the page.
 */
function logout() {
    API.clearToken();
    location.reload();
}
