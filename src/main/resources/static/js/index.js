/**
 * index.js — SwiftLink Home Page Logic
 *
 * Depends on: api.js, common.js (loaded before this script)
 */

(function () {
    // ----------------------------------------------------------
    // DOM refs
    // ----------------------------------------------------------
    const form           = document.getElementById('shortenForm');
    const urlInput       = document.getElementById('urlInput');
    const submitBtn      = document.getElementById('submitBtn');
    const resultCard     = document.getElementById('resultCard');
    const shortLink      = document.getElementById('shortLink');
    const errorContainer = document.getElementById('errorContainer');
    const errorMsg       = document.getElementById('errorMsg');
    const statsLink      = document.getElementById('statsLink');
    const copyFeedback   = document.getElementById('copyFeedback');

    // ----------------------------------------------------------
    // Nav auth state
    // ----------------------------------------------------------
    renderNavAuthState();

    // ----------------------------------------------------------
    // Shorten form
    // ----------------------------------------------------------
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        errorContainer.classList.add('hidden');
        resultCard.classList.add('hidden');
        copyFeedback.classList.add('hidden');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Shortening...';

        const originalUrl = urlInput.value;

        try {
            const data = await API.urls.create(originalUrl);
            const publicShortUrl = buildPublicShortUrl(data.shortUrl);
            const code = extractCodeFromShortUrl(data.shortUrl);

            shortLink.href = publicShortUrl;
            shortLink.textContent = publicShortUrl;
            statsLink.href = `/stats/${code}`;

            resultCard.classList.remove('hidden');
        } catch (err) {
            errorMsg.textContent = err.message;
            errorContainer.classList.remove('hidden');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Shorten URL';
        }
    });

    // ----------------------------------------------------------
    // Copy to clipboard
    // ----------------------------------------------------------
    window.copyToClipboardIndex = function () {
        copyToClipboard(shortLink.textContent, copyFeedback);
    };
})();
