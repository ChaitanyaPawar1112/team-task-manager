function showTab(tab) {
    const loginForm = document.getElementById('login-form');
    const signupForm = document.getElementById('signup-form');
    const tabs = document.querySelectorAll('.tab-btn');
    
    if (tab === 'login') {
        loginForm.classList.add('active');
        signupForm.classList.remove('active');
        tabs[0].classList.add('active');
        tabs[1].classList.remove('active');
    } else {
        loginForm.classList.remove('active');
        signupForm.classList.add('active');
        tabs[0].classList.remove('active');
        tabs[1].classList.add('active');
    }
}

async function login() {
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const errorDiv = document.getElementById('login-error');
    
    // Clear previous error
    errorDiv.textContent = '';
    
    if (!email || !password) {
        errorDiv.textContent = 'Please enter email and password';
        return;
    }
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            // Use setAuthToken from api.js
            setAuthToken(data.token);
            localStorage.setItem('userRole', data.role);
            localStorage.setItem('userName', data.name);
            
            // Redirect to dashboard
            window.location.href = '/dashboard.html';
        } else {
            errorDiv.textContent = data.error || 'Login failed';
        }
    } catch (error) {
        console.error('Login error:', error);
        errorDiv.textContent = 'Network error. Please try again.';
    }
}

async function signup() {
    const name = document.getElementById('signup-name').value;
    const email = document.getElementById('signup-email').value;
    const password = document.getElementById('signup-password').value;
    const errorDiv = document.getElementById('signup-error');
    
    // Clear previous error
    errorDiv.textContent = '';
    
    if (!name || !email || !password) {
        errorDiv.textContent = 'Please fill all fields';
        return;
    }
    
    if (password.length < 6) {
        errorDiv.textContent = 'Password must be at least 6 characters';
        return;
    }
    
    try {
        const response = await fetch('/api/auth/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            alert('Account created successfully! Please login.');
            // Clear form
            document.getElementById('signup-name').value = '';
            document.getElementById('signup-email').value = '';
            document.getElementById('signup-password').value = '';
            // Switch to login tab
            showTab('login');
        } else {
            errorDiv.textContent = data.error || 'Signup failed';
        }
    } catch (error) {
        console.error('Signup error:', error);
        errorDiv.textContent = 'Network error. Please try again.';
    }
}