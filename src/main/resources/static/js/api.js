// api.js
const API_BASE_URL = '/api';

let authToken = localStorage.getItem('token');

function setAuthToken(token) {
    authToken = token;
    if (token) {
        localStorage.setItem('token', token);
    } else {
        localStorage.removeItem('token');
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userName');
    authToken = null;
    window.location.href = '/index.html';
}

async function apiRequest(endpoint, method = 'GET', data = null) {
    const headers = {
        'Content-Type': 'application/json',
    };
    
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }
    
    const config = {
        method,
        headers,
    };
    
    if (data) {
        config.body = JSON.stringify(data);
    }
    
    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    
    if (response.status === 401) {
        logout();
        throw new Error('Session expired');
    }
    
    const responseData = await response.json();
    
    if (!response.ok) {
        throw new Error(responseData.error || 'Request failed');
    }
    
    return responseData;
}