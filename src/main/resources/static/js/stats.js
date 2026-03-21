/**
 * stats.js — SwiftLink Link Analytics Page Logic
 *
 * Depends on: api.js, common.js (loaded before this script)
 * Note: stats page is public — no auth guard required.
 */

(async function () {
    // ----------------------------------------------------------
    // Resolve short code from the URL path: /stats/:code
    // ----------------------------------------------------------
    const pathSegments = window.location.pathname.split('/');
    const code = pathSegments.findLast(segment => segment.length > 0);

    // ----------------------------------------------------------
    // DOM refs
    // ----------------------------------------------------------
    const originalLink     = document.getElementById('originalLink');
    const shortLinkDisplay = document.getElementById('shortLinkDisplay');
    const clickCount       = document.getElementById('clickCount');
    const createdDate      = document.getElementById('createdDate');
    const timeAgoEl        = document.getElementById('timeAgo');
    const qrImage          = document.getElementById('qrImage');
    const loading          = document.getElementById('loading');
    const dashboard        = document.getElementById('dashboard');
    const errorDiv         = document.getElementById('error');
    const refreshBtn       = document.getElementById('refreshBtn');

    // ----------------------------------------------------------
    // Copy button setup
    // ----------------------------------------------------------
    function setupCopyButton() {
        const copyBtn  = document.getElementById('copyBtn');
        const feedback = document.getElementById('copyFeedback');

        if (copyBtn && feedback) {
            // Clone to remove any pre-existing listeners
            const newBtn = copyBtn.cloneNode(true);
            copyBtn.parentNode.replaceChild(newBtn, copyBtn);

            newBtn.addEventListener('click', () => {
                copyToClipboard(shortLinkDisplay.textContent, feedback);
            });
        }
    }

    // ----------------------------------------------------------
    // Load / refresh data
    // ----------------------------------------------------------
    async function loadData(isRefresh = false) {
        try {
            if (isRefresh) {
                const icon = refreshBtn.querySelector('svg');
                if (icon) icon.classList.add('animate-spin');
                refreshBtn.disabled = true;
            }

            const data = await API.urls.getMetadata(code);

            originalLink.href      = data.originalUrl;
            originalLink.textContent = data.originalUrl;
            shortLinkDisplay.textContent = buildPublicShortUrl(data.shortUrl, code);
            clickCount.textContent = data.clicks;

            const date = new Date(data.createdAt);
            createdDate.textContent = date.toLocaleString(undefined, {
                year: 'numeric', month: 'long', day: 'numeric',
                hour: '2-digit', minute: '2-digit',
            });
            timeAgoEl.textContent = timeAgo(date);

            if (!isRefresh) {
                qrImage.src = `/api/urls/${code}/qr`;
                setupCopyButton();
                loading.classList.add('hidden');
                dashboard.classList.remove('hidden');
            }
        } catch (err) {
            if (!isRefresh) {
                loading.classList.add('hidden');
                errorDiv.classList.remove('hidden');
            }
            console.error(err);
        } finally {
            if (isRefresh) {
                setTimeout(() => {
                    const icon = refreshBtn.querySelector('svg');
                    if (icon) icon.classList.remove('animate-spin');
                    refreshBtn.disabled = false;
                }, 500);
            }
        }
    }

    // ----------------------------------------------------------
    // Refresh button
    // ----------------------------------------------------------
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => loadData(true));
    }

    // Init
    await loadData(false);
})();
