/**
 * Electricity Trading Platform - Client Application
 * WebSocket communication with STOMP protocol
 */

// ============================================
// State Management
// ============================================
let stompClient = null;
let jwtToken = null;
let currentUsername = null;
let currentBalance = null;
const priceData = {};
let orderCounter = 1;

// Client-side order tracking - stores pending orders by orderId
const pendingOrders = new Map();

// ============================================
// Reconnection Configuration
// ============================================
const reconnectConfig = {
    maxAttempts: 5,
    baseDelay: 1000,      // 1 second
    maxDelay: 30000,      // 30 seconds
    multiplier: 2         // Exponential backoff multiplier
};

let reconnectState = {
    attempts: 0,
    timeoutId: null,
    isManualDisconnect: false
};

// ============================================
// Toast Notification System
// ============================================
function showToast(title, message, type = 'info') {
    const container = document.getElementById('toastContainer');

    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;

    const icons = {
        success: `<svg class="toast__icon" viewBox="0 0 20 20" fill="currentColor" style="color: var(--accent-success)">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
        </svg>`,
        error: `<svg class="toast__icon" viewBox="0 0 20 20" fill="currentColor" style="color: var(--accent-danger)">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
        </svg>`,
        warning: `<svg class="toast__icon" viewBox="0 0 20 20" fill="currentColor" style="color: var(--accent-warning)">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
        </svg>`,
        info: `<svg class="toast__icon" viewBox="0 0 20 20" fill="currentColor" style="color: var(--accent-primary)">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
        </svg>`
    };

    toast.innerHTML = `
        ${icons[type] || icons.info}
        <div class="toast__content">
            <div class="toast__title">${title}</div>
            <div class="toast__message">${message}</div>
        </div>
        <button class="toast__close" onclick="this.parentElement.remove()">
            <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
        </button>
    `;

    container.appendChild(toast);

    // Auto-remove after 4 seconds
    setTimeout(() => {
        toast.classList.add('toast--hiding');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// ============================================
// System Log
// ============================================
function log(message, type = 'info') {
    const logDiv = document.getElementById('logEntries');
    const entry = document.createElement('div');

    let entryClass = 'system-log__entry';
    if (type === 'error') entryClass += ' system-log__entry--error';
    if (type === 'success') entryClass += ' system-log__entry--success';
    if (type === 'warning') entryClass += ' system-log__entry--warning';
    if (type === 'pending') entryClass += ' system-log__entry--pending';

    entry.className = entryClass;
    entry.innerHTML = `<span class="system-log__timestamp">[${new Date().toLocaleTimeString()}]</span> ${message}`;
    logDiv.insertBefore(entry, logDiv.firstChild);

    // Keep only last 50 entries
    while (logDiv.children.length > 50) {
        logDiv.removeChild(logDiv.lastChild);
    }
}

// ============================================
// Balance Management
// ============================================
function updateBalanceDisplay(balance) {
    const balanceEl = document.getElementById('userBalance');
    if (balanceEl && balance !== null && balance !== undefined) {
        const balanceNum = parseFloat(balance);
        currentBalance = balanceNum;
        balanceEl.textContent = `Balance: €${balanceNum.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }
}

async function fetchBalance() {
    if (!jwtToken) return;

    try {
        const response = await fetch('/api/balance', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            updateBalanceDisplay(data.balance);
        }
    } catch (error) {
        log('Failed to fetch balance: ' + error.message, 'error');
    }
}

// ============================================
// Authentication
// ============================================
async function login(event) {
    event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    log(`Attempting login as ${username}...`);

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            throw new Error('Invalid credentials');
        }

        const data = await response.json();
        jwtToken = data.token;
        currentUsername = data.username;

        // Persist session to localStorage
        localStorage.setItem('jwtToken', jwtToken);
        localStorage.setItem('username', currentUsername);
        if (data.balance) {
            localStorage.setItem('balance', data.balance);
        }

        log(`Login successful for ${currentUsername}`, 'success');
        showToast('Welcome!', `Logged in as ${currentUsername}`, 'success');

        // Update balance display
        updateBalanceDisplay(data.balance);

        // Show trading screen, hide login
        document.getElementById('loginScreen').classList.add('hidden');
        document.getElementById('tradingScreen').classList.remove('hidden');
        document.getElementById('currentUser').textContent = currentUsername;

        // Fetch order history from server
        fetchOrderHistory();

    } catch (error) {
        log('Login failed: ' + error.message, 'error');
        showToast('Login Failed', error.message, 'error');
    }
}

function logout() {
    // Cancel any pending reconnection attempts
    reconnectState.isManualDisconnect = true;
    cancelReconnect();

    disconnect();
    jwtToken = null;
    currentUsername = null;
    currentBalance = null;
    pendingOrders.clear();

    // Clear session from localStorage
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('username');
    localStorage.removeItem('balance');

    // Reset balance display
    document.getElementById('userBalance').textContent = 'Balance: --';

    document.getElementById('loginScreen').classList.remove('hidden');
    document.getElementById('tradingScreen').classList.add('hidden');
    log('Logged out');
    showToast('Logged Out', 'You have been logged out successfully', 'info');
}

// ============================================
// Connection State Management
// ============================================
const ConnectionState = {
    DISCONNECTED: 'disconnected',
    CONNECTING: 'connecting',
    CONNECTED: 'connected',
    RECONNECTING: 'reconnecting'
};

function setConnectionState(state, message = null) {
    const statusBadge = document.getElementById('statusBadge');
    const statusText = document.getElementById('statusText');
    const connectBtn = document.getElementById('connectBtn');
    const disconnectBtn = document.getElementById('disconnectBtn');
    const submitBtn = document.getElementById('submitBtn');

    // Remove all state classes
    statusBadge.classList.remove(
        'status-badge--connected',
        'status-badge--reconnecting'
    );

    switch (state) {
        case ConnectionState.CONNECTED:
            statusBadge.classList.add('status-badge--connected');
            statusText.textContent = message || `Connected as ${currentUsername}`;
            connectBtn.disabled = true;
            disconnectBtn.disabled = false;
            submitBtn.disabled = false;
            break;

        case ConnectionState.RECONNECTING:
            statusBadge.classList.add('status-badge--reconnecting');
            statusText.textContent = message || 'Reconnecting...';
            connectBtn.disabled = true;
            disconnectBtn.disabled = false;
            submitBtn.disabled = true;
            break;

        case ConnectionState.CONNECTING:
            statusText.textContent = message || 'Connecting...';
            connectBtn.disabled = true;
            disconnectBtn.disabled = true;
            submitBtn.disabled = true;
            break;

        case ConnectionState.DISCONNECTED:
        default:
            statusText.textContent = message || 'Disconnected';
            connectBtn.disabled = false;
            disconnectBtn.disabled = true;
            submitBtn.disabled = true;
            break;
    }
}

// Legacy function for backwards compatibility
function setConnected(connected) {
    setConnectionState(connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED);
}

// ============================================
// WebSocket Connection
// ============================================
function connect() {
    if (!jwtToken) {
        log('Please login first', 'error');
        showToast('Not Authenticated', 'Please login first', 'error');
        return;
    }

    // Reset reconnect state on manual connect
    reconnectState.isManualDisconnect = false;
    reconnectState.attempts = 0;
    cancelReconnect();

    doConnect();
}

function doConnect() {
    const isReconnect = reconnectState.attempts > 0;

    if (isReconnect) {
        log(`Reconnection attempt ${reconnectState.attempts}/${reconnectConfig.maxAttempts}...`);
        setConnectionState(ConnectionState.RECONNECTING,
            `Reconnecting (${reconnectState.attempts}/${reconnectConfig.maxAttempts})...`);
    } else {
        log('Connecting to WebSocket server...');
        setConnectionState(ConnectionState.CONNECTING);
    }

    const socket = new SockJS('/ws-electricity?token=' + jwtToken);
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable STOMP debug logging

    stompClient.connect({}, function() {
        // Success - reset reconnect state
        reconnectState.attempts = 0;

        if (isReconnect) {
            log('Reconnected to WebSocket server', 'success');
            showToast('Reconnected', 'Connection restored', 'success');
        } else {
            log('Connected to WebSocket server', 'success');
            showToast('Connected', 'Real-time data stream active', 'success');
        }

        setConnectionState(ConnectionState.CONNECTED);
        subscribeToTopics();

    }, function(error) {
        log('Connection error: ' + error, 'error');
        setConnectionState(ConnectionState.DISCONNECTED);
        handleConnectionError();
    });

    // Handle unexpected disconnection via SockJS close event
    socket.onclose = function() {
        if (!reconnectState.isManualDisconnect && jwtToken) {
            handleConnectionError();
        }
    };
}

function subscribeToTopics() {
    // Subscribe to price updates (broadcast to all)
    stompClient.subscribe('/topic/prices', function(message) {
        const price = JSON.parse(message.body);
        onPriceReceived(price);
    });
    log('Subscribed to /topic/prices');

    // Subscribe to personal order confirmations
    stompClient.subscribe('/user/queue/order-confirmation', function(message) {
        const confirmation = JSON.parse(message.body);
        onConfirmationReceived(confirmation);
    });
    log('Subscribed to /user/queue/order-confirmation');

    // Subscribe to personal error messages
    stompClient.subscribe('/user/queue/errors', function(message) {
        log('Server error: ' + message.body, 'error');
        showToast('Server Error', message.body, 'error');
    });
    log('Subscribed to /user/queue/errors');
}

function disconnect() {
    reconnectState.isManualDisconnect = true;
    cancelReconnect();

    if (stompClient !== null) {
        stompClient.disconnect();
        log('Disconnected from server');
    }
    setConnectionState(ConnectionState.DISCONNECTED);
}

// ============================================
// Reconnection Logic
// ============================================
function handleConnectionError() {
    // Don't reconnect if manually disconnected or logged out
    if (reconnectState.isManualDisconnect || !jwtToken) {
        return;
    }

    // Check if we've exceeded max attempts
    if (reconnectState.attempts >= reconnectConfig.maxAttempts) {
        log(`Max reconnection attempts (${reconnectConfig.maxAttempts}) reached. Giving up.`, 'error');
        showToast('Connection Lost', 'Unable to reconnect. Please try manually.', 'error');
        setConnectionState(ConnectionState.DISCONNECTED, 'Connection lost');
        reconnectState.attempts = 0;
        return;
    }

    reconnectState.attempts++;

    // Calculate delay with exponential backoff
    const delay = Math.min(
        reconnectConfig.baseDelay * Math.pow(reconnectConfig.multiplier, reconnectState.attempts - 1),
        reconnectConfig.maxDelay
    );

    log(`Connection lost. Reconnecting in ${delay / 1000}s...`);
    setConnectionState(ConnectionState.RECONNECTING,
        `Reconnecting in ${Math.round(delay / 1000)}s...`);

    if (reconnectState.attempts === 1) {
        showToast('Connection Lost', 'Attempting to reconnect...', 'warning');
    }

    reconnectState.timeoutId = setTimeout(() => {
        doConnect();
    }, delay);
}

function cancelReconnect() {
    if (reconnectState.timeoutId) {
        clearTimeout(reconnectState.timeoutId);
        reconnectState.timeoutId = null;
    }
}

// ============================================
// Order Management
// ============================================
function submitOrder(event) {
    event.preventDefault();

    if (!stompClient || !stompClient.connected) {
        log('Cannot submit order: not connected', 'error');
        showToast('Not Connected', 'Please connect to the server first', 'error');
        return;
    }

    const order = {
        orderId: 'ORD-' + Date.now() + '-' + (orderCounter++),
        region: document.getElementById('orderRegion').value,
        type: document.getElementById('orderType').value,
        quantity: parseInt(document.getElementById('orderQuantity').value),
        price: parseFloat(document.getElementById('orderPrice').value),
        submittedAt: new Date().toISOString()
    };

    // Store order locally for correlation when confirmation arrives
    pendingOrders.set(order.orderId, order);

    // Add to order history UI (with Pending status)
    addOrderToHistory(order, 'pending');

    // Send order to server via WebSocket
    stompClient.send('/app/order', {}, JSON.stringify(order));
    log(`Order submitted: ${order.type} ${order.quantity} MWh @ ${order.price.toFixed(2)} EUR/MWh (${order.region})`);
    showToast('Order Submitted', `${order.type} ${order.quantity} MWh @ ${order.price.toFixed(2)} EUR/MWh`, 'info');
}

// ============================================
// Order History
// ============================================

/**
 * Fetch order history from the server for the current user.
 * Called after login or session restore to populate the order history UI.
 */
async function fetchOrderHistory() {
    if (!jwtToken) {
        log('Cannot fetch order history: not authenticated', 'error');
        return;
    }

    try {
        log('Fetching order history...');
        const response = await fetch('/api/orders/history?limit=10', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const orders = await response.json();
        log(`Loaded ${orders.length} orders from history`, 'success');

        // Clear existing history UI
        const historyBody = document.getElementById('orderHistoryBody');
        historyBody.innerHTML = '';

        if (orders.length === 0) {
            historyBody.innerHTML = `
                <div class="order-history__empty">
                    No orders yet. Submit an order to get started.
                </div>
            `;
            return;
        }

        // Add orders to UI (they come sorted by most recent first)
        orders.forEach(order => {
            addOrderToHistoryFromServer(order);
        });

    } catch (error) {
        log('Failed to fetch order history: ' + error.message, 'error');
    }
}

/**
 * Add an order from server response to the history UI.
 * Different from addOrderToHistory because server response has different format.
 */
function addOrderToHistoryFromServer(order) {
    const historyBody = document.getElementById('orderHistoryBody');

    // Remove empty state message if present
    const emptyState = historyBody.querySelector('.order-history__empty');
    if (emptyState) {
        emptyState.remove();
    }

    const badgeClass = order.type === 'BUY' ? 'order-history__badge--buy' : 'order-history__badge--sell';
    const status = order.status.toLowerCase();
    const statusDotClass = `order-history__status-dot--${status}`;
    const statusText = status.charAt(0).toUpperCase() + status.slice(1);

    // Parse numeric values from strings
    const quantity = parseFloat(order.quantity);
    const price = parseFloat(order.price);

    const row = document.createElement('div');
    row.className = 'order-history__row';
    row.id = `order-${order.orderId}`;

    let statusHtml = `
        <div class="order-history__status">
            <span class="order-history__status-dot ${statusDotClass}"></span>
            <span class="order-history__status-text">${statusText}</span>
        </div>
    `;

    // Add tooltip for rejected orders
    if (status === 'rejected' && order.rejectReason) {
        statusHtml = `
            <div class="order-history__status order-history__status--has-tooltip" data-reason="${order.rejectReason}">
                <span class="order-history__status-dot ${statusDotClass}"></span>
                <span class="order-history__status-text">${statusText}</span>
            </div>
        `;
    }

    row.innerHTML = `
        <span class="order-history__badge ${badgeClass}">${order.type}</span>
        <div class="order-history__details">
            <div class="order-history__quantity">${quantity} MWh</div>
            <div class="order-history__price">@ ${price.toFixed(2)} EUR/MWh</div>
            <div class="order-history__region">${order.region}</div>
        </div>
        ${statusHtml}
    `;

    // Append (not prepend) since server already sorted by most recent
    historyBody.appendChild(row);
}

function addOrderToHistory(order, status = 'pending') {
    const historyBody = document.getElementById('orderHistoryBody');

    // Remove empty state message if present
    const emptyState = historyBody.querySelector('.order-history__empty');
    if (emptyState) {
        emptyState.remove();
    }

    const badgeClass = order.type === 'BUY' ? 'order-history__badge--buy' : 'order-history__badge--sell';
    const statusDotClass = `order-history__status-dot--${status}`;
    const statusText = status.charAt(0).toUpperCase() + status.slice(1);

    const row = document.createElement('div');
    row.className = 'order-history__row';
    row.id = `order-${order.orderId}`;
    row.innerHTML = `
        <span class="order-history__badge ${badgeClass}">${order.type}</span>
        <div class="order-history__details">
            <div class="order-history__quantity">${order.quantity} MWh</div>
            <div class="order-history__price">@ ${order.price.toFixed(2)} EUR/MWh</div>
            <div class="order-history__region">${order.region}</div>
        </div>
        <div class="order-history__status">
            <span class="order-history__status-dot ${statusDotClass}"></span>
            <span class="order-history__status-text">${statusText}</span>
        </div>
    `;

    // Prepend new order at the top
    historyBody.insertBefore(row, historyBody.firstChild);

    // Keep only last 10 orders in view
    while (historyBody.children.length > 10) {
        historyBody.removeChild(historyBody.lastChild);
    }
}

function updateOrderStatus(orderId, status, reason = null) {
    const row = document.getElementById(`order-${orderId}`);
    if (!row) return;

    const statusDot = row.querySelector('.order-history__status-dot');
    const statusText = row.querySelector('.order-history__status-text');
    const statusContainer = row.querySelector('.order-history__status');

    // Remove existing status classes
    statusDot.classList.remove(
        'order-history__status-dot--pending',
        'order-history__status-dot--submitted',
        'order-history__status-dot--filled',
        'order-history__status-dot--rejected'
    );

    // Add new status class
    statusDot.classList.add(`order-history__status-dot--${status}`);
    statusText.textContent = status.charAt(0).toUpperCase() + status.slice(1);

    // Add tooltip for rejected orders with reason
    if (status === 'rejected' && reason) {
        statusContainer.setAttribute('data-reason', reason);
        statusContainer.classList.add('order-history__status--has-tooltip');
    }
}

// ============================================
// Data Handlers
// ============================================
function onPriceReceived(price) {
    priceData[price.region] = price;
    updatePriceTable();
}

function onConfirmationReceived(confirmation) {
    const order = pendingOrders.get(confirmation.orderId);
    const status = confirmation.status; // PENDING, SUBMITTED, FILLED, REJECTED

    // Map backend status to UI status, log type, and toast type
    let historyStatus;
    let logType;
    let toastType;
    let toastTitle;

    switch (status) {
        case 'PENDING':
            historyStatus = 'pending';
            logType = 'pending';      // Orange
            toastType = 'info';
            toastTitle = 'Order PENDING';
            break;
        case 'SUBMITTED':
            historyStatus = 'submitted';
            logType = 'warning';      // Yellow
            toastType = 'info';
            toastTitle = 'Order SUBMITTED';
            break;
        case 'FILLED':
            historyStatus = 'filled';
            logType = 'success';      // Green
            toastType = 'success';
            toastTitle = 'Order FILLED';
            // Order complete - remove from pending
            pendingOrders.delete(confirmation.orderId);
            // Refresh balance after order is filled
            fetchBalance();
            break;
        case 'REJECTED':
            historyStatus = 'rejected';
            logType = 'error';        // Red
            toastType = 'error';
            toastTitle = 'Order REJECTED';
            // Order complete - remove from pending
            pendingOrders.delete(confirmation.orderId);
            // Refresh balance (may be refunded for rejected BUY orders)
            fetchBalance();
            break;
        default:
            historyStatus = 'pending';
            logType = 'info';
            toastType = 'info';
            toastTitle = `Order ${status}`;
    }

    // Include rejection reason in log if available
    const logMessage = status === 'REJECTED' && confirmation.message
        ? `Order ${confirmation.orderId}: ${status} - ${confirmation.message}`
        : `Order ${confirmation.orderId}: ${status}`;
    log(logMessage, logType);

    // Update order status in history (pass reason for rejected orders)
    const reason = status === 'REJECTED' ? confirmation.message : null;
    updateOrderStatus(confirmation.orderId, historyStatus, reason);

    // Show toast notification
    const toastMessage = order
        ? `${order.type} ${order.quantity} MWh @ ${order.price.toFixed(2)} EUR/MWh (${order.region})`
        : confirmation.message || confirmation.orderId;

    showToast(toastTitle, toastMessage, toastType);
}

function updatePriceTable() {
    const tbody = document.getElementById('priceTable');
    const regions = Object.keys(priceData).sort();

    if (regions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="market-table__empty">
                    Connect to see live prices
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = regions.map(region => {
        const p = priceData[region];
        const changeClass = p.changePercent > 0
            ? 'market-table__change--positive'
            : p.changePercent < 0
                ? 'market-table__change--negative'
                : 'market-table__change--neutral';
        const changePrefix = p.changePercent > 0 ? '+' : '';
        const time = new Date(p.timestamp).toLocaleTimeString();

        return `
            <tr>
                <td>
                    <div class="market-table__region">${p.region}</div>
                    <div class="market-table__area">${p.area}</div>
                </td>
                <td class="market-table__price">${p.price.toFixed(2)}</td>
                <td class="market-table__change ${changeClass}">${changePrefix}${p.changePercent.toFixed(2)}%</td>
                <td class="market-table__time">${time}</td>
            </tr>
        `;
    }).join('');
}

// ============================================
// Initialize on DOM Ready
// ============================================
document.addEventListener('DOMContentLoaded', function() {
    log('Application initialized');

    // Check for saved session in localStorage
    const savedToken = localStorage.getItem('jwtToken');
    const savedUsername = localStorage.getItem('username');
    const savedBalance = localStorage.getItem('balance');

    if (savedToken && savedUsername) {
        // Restore session
        jwtToken = savedToken;
        currentUsername = savedUsername;

        log(`Session restored for ${currentUsername}`, 'success');

        // Show trading screen, hide login
        document.getElementById('loginScreen').classList.add('hidden');
        document.getElementById('tradingScreen').classList.remove('hidden');
        document.getElementById('currentUser').textContent = currentUsername;

        // Restore balance from localStorage (quick display) then fetch fresh from server
        if (savedBalance) {
            updateBalanceDisplay(savedBalance);
        }

        // Fetch fresh balance from server (balance may have changed)
        fetchBalance();

        // Fetch order history from server
        fetchOrderHistory();

        // Auto-connect to WebSocket
        connect();
    }
});
