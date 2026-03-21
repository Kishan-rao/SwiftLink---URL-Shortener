/**
 * auth.js — SwiftLink Authentication Page Logic
 *
 * Depends on: api.js (loaded before this script)
 */

(function () {
    // Redirect if already authenticated
    if (API.isAuthenticated()) {
        window.location.href = '/dashboard';
        return;
    }

    // ----------------------------------------------------------
    // Tab switching
    // ----------------------------------------------------------
    window.switchTab = function (tab) {
        const loginSection    = document.getElementById('loginSection');
        const registerSection = document.getElementById('registerSection');
        const loginTab        = document.getElementById('loginTab');
        const registerTab     = document.getElementById('registerTab');
        const errorContainer  = document.getElementById('errorContainer');

        errorContainer.classList.add('hidden');

        if (tab === 'login') {
            loginSection.classList.remove('hidden');
            registerSection.classList.add('hidden');
            loginTab.classList.add('active');
            loginTab.classList.remove('text-gray-500');
            registerTab.classList.remove('active');
            registerTab.classList.add('text-gray-500');
        } else {
            loginSection.classList.add('hidden');
            registerSection.classList.remove('hidden');
            loginTab.classList.remove('active');
            loginTab.classList.add('text-gray-500');
            registerTab.classList.add('active');
            registerTab.classList.remove('text-gray-500');
        }
    };

    // ----------------------------------------------------------
    // Login form
    // ----------------------------------------------------------
    const loginForm = document.getElementById('loginForm');
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const email          = document.getElementById('loginEmail').value;
        const password       = document.getElementById('loginPassword').value;
        const btn            = document.getElementById('loginBtn');
        const errorContainer = document.getElementById('errorContainer');
        const errorMsg       = document.getElementById('errorMsg');

        btn.disabled = true;
        btn.textContent = 'Signing in...';
        errorContainer.classList.add('hidden');

        try {
            const data = await API.auth.login(email, password);
            API.setToken(data.token);
            localStorage.setItem('userEmail', email);
            window.location.href = '/dashboard';
        } catch (err) {
            errorMsg.textContent = err.message;
            errorContainer.classList.remove('hidden');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Sign In';
        }
    });

    // ----------------------------------------------------------
    // Register form
    // ----------------------------------------------------------
    const registerForm = document.getElementById('registerForm');
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const name           = document.getElementById('regName').value;
        const email          = document.getElementById('regEmail').value;
        const password       = document.getElementById('regPassword').value;
        const btn            = document.getElementById('regBtn');
        const errorContainer = document.getElementById('errorContainer');
        const errorMsg       = document.getElementById('errorMsg');

        btn.disabled = true;
        btn.textContent = 'Creating account...';
        errorContainer.classList.add('hidden');

        try {
            await API.auth.register(name, email, password);
            alert('Account created! You can now sign in.');
            switchTab('login');
            document.getElementById('loginEmail').value = email;
        } catch (err) {
            errorMsg.textContent = err.message;
            errorContainer.classList.remove('hidden');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Register';
        }
    });
})();
