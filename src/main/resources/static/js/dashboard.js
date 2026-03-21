/**
 * dashboard.js — SwiftLink Dashboard Page Logic
 *
 * Depends on: api.js, common.js (loaded before this script)
 */

(function () {
    // Guard: must be authenticated
    if (!API.isAuthenticated()) {
        window.location.href = '/auth';
        return;
    }

    // Show user email in nav
    const emailDisplay = document.getElementById('userEmailDisplay');
    if (emailDisplay) {
        emailDisplay.textContent = localStorage.getItem('userEmail') || '';
    }

    // ----------------------------------------------------------
    // Fetch & display links
    // ----------------------------------------------------------
    async function fetchLinks() {
        try {
            const links = await API.urls.getMyLinks();
            displayLinks(links);
        } catch (err) {
            console.error('Error fetching links:', err);
            document.getElementById('loadingState').innerHTML =
                '<p class="text-red-400">Failed to load links. Please try again.</p>';
        }
    }

    function displayLinks(links) {
        const grid    = document.getElementById('linksGrid');
        const loading = document.getElementById('loadingState');
        const empty   = document.getElementById('emptyState');

        loading.classList.add('hidden');
        grid.innerHTML = '';

        if (!links || links.length === 0) {
            empty.classList.remove('hidden');
            return;
        }

        empty.classList.add('hidden');

        links.forEach(link => {
            const code = link.shortUrl.split('/').pop();
            const card = document.createElement('div');
            card.className = 'bg-gray-800 p-6 rounded-2xl border border-gray-700 shadow-lg hover:border-gray-500 transition flex flex-col justify-between';

            card.innerHTML = `
                <div>
                    <div class="flex justify-between items-start mb-4">
                        <span class="text-xs font-bold px-2 py-1 bg-blue-900/50 text-blue-400 rounded uppercase tracking-wider">
                            ${link.alias ? 'Custom' : 'Standard'}
                        </span>
                        <div class="flex space-x-2">
                            <button onclick="deleteLink('${code}')" class="text-gray-500 hover:text-red-400 transition">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                            </button>
                        </div>
                    </div>
                    <h3 class="text-emerald-400 font-mono text-lg mb-1 truncate">
                        <a href="${link.shortUrl}" target="_blank" class="hover:underline">${link.shortUrl}</a>
                    </h3>
                    <p class="text-gray-500 text-sm truncate mb-4">${link.originalUrl}</p>
                </div>
                <div class="pt-4 border-t border-gray-700 flex justify-between items-center">
                    <div class="flex items-center text-gray-300">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                        <span class="font-bold">${link.clicks}</span>
                        <span class="ml-1 text-xs text-gray-500">clicks</span>
                    </div>
                    <a href="/stats/${code}" class="text-xs text-blue-400 hover:underline">View Detailed Stats</a>
                </div>
            `;
            grid.appendChild(card);
        });
    }

    // ----------------------------------------------------------
    // Delete a link (exposed globally for inline onclick)
    // ----------------------------------------------------------
    window.deleteLink = async function (code) {
        if (!confirm('Are you sure you want to delete this link?')) return;
        try {
            await API.urls.delete(code);
            fetchLinks();
        } catch (err) {
            console.error('Error deleting link:', err);
            alert('Failed to delete link');
        }
    };

    // ----------------------------------------------------------
    // Logout (exposed globally for inline onclick)
    // ----------------------------------------------------------
    window.logout = function () {
        API.clearToken();
        window.location.href = '/auth';
    };

    // Init
    fetchLinks();
})();
