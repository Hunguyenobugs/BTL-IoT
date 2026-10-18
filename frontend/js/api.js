/**
 * api.js – Wrapper fetch tới Backend API
 * Tất cả file JS khác import từ file này qua <script> tag theo thứ tự.
 */

// ─── CONFIG ──────────────────────────────────────────────────────────────────
// TODO: Đổi BASE_URL nếu backend chạy ở port/host khác
const BASE_URL = 'http://localhost:8080/api';

/**
 * Hàm fetch chung. Tự động parse JSON, đính kèm JWT token nếu có, ném Error nếu success=false.
 * @param {string} endpoint  Đường dẫn relative (vd: '/sensors/latest')
 * @param {object} [options] fetch options (method, body, headers...)
 * @returns {Promise<any>}   Trả về phần `data` trong response
 */
async function apiFetch(endpoint, options = {}) {
  const url = BASE_URL + endpoint;
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const defaultOptions = {
    ...options,
    headers,
  };
  const res = await fetch(url, defaultOptions);
  
  let json;
  try {
    json = await res.json();
  } catch (err) {
    throw new Error('Không thể phân tích phản hồi từ server');
  }

  if (res.status === 401) {
    // Token hết hạn hoặc không hợp lệ
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.dispatchEvent(new CustomEvent('auth:state-change'));
    if (!window.location.pathname.endsWith('login.html')) {
      window.location.href = 'login.html';
    }
    throw new Error(json.message || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
  }

  if (!json.success) {
    throw new Error(json.message || 'API error');
  }
  return json.data;
}

// ─── AUTH APIs ───────────────────────────────────────────────────────────────

/** POST /api/auth/login */
function apiLogin(username, password) {
  return apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

/** POST /api/auth/register */
function apiRegister(data) {
  return apiFetch('/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/** POST /api/auth/change-password */
function apiChangePassword(oldPassword, newPassword) {
  return apiFetch('/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ oldPassword, newPassword }),
  });
}

/** GET /api/auth/me */
function apiGetMe() {
  return apiFetch('/auth/me');
}

// ─── SENSOR APIs ─────────────────────────────────────────────────────────────

/** GET /api/sensors/latest */
function fetchLatestSensors() {
  return apiFetch('/sensors/latest');
}

/** GET /api/sensors/chart?limit=N */
function fetchChartData(limit = 15) {
  return apiFetch(`/sensors/chart?limit=${limit}`);
}

/** GET /api/sensors với query params */
function fetchSensorList(params = {}) {
  const qs = buildQueryString(params);
  return apiFetch(`/sensors${qs}`);
}

// ─── DEVICE APIs ─────────────────────────────────────────────────────────────

/** GET /api/devices/status */
function fetchDeviceStatus() {
  return apiFetch('/devices/status');
}

/** POST /api/devices/{deviceId}/control */
function postDeviceControl(deviceId, action) {
  return apiFetch(`/devices/${deviceId}/control`, {
    method: 'POST',
    body: JSON.stringify({ action }),
  });
}

/** GET /api/devices/history/{historyId}/status */
function fetchHistoryStatus(historyId) {
  return apiFetch(`/devices/history/${historyId}/status`);
}

/** GET /api/devices/history với query params */
function fetchDeviceHistory(params = {}) {
  const qs = buildQueryString(params);
  return apiFetch(`/devices/history${qs}`);
}

// ─── HELPER ──────────────────────────────────────────────────────────────────

/** Chuyển object params thành query string (bỏ qua giá trị null/undefined/rỗng) */
function buildQueryString(params) {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== null && v !== undefined && v !== ''
  );
  if (entries.length === 0) return '';
  return '?' + entries.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&');
}

/**
 * Format datetime chuỗi ISO → "yyyy/MM/dd HH:mm:ss"
 * Ví dụ: "2026-08-18T12:54:01" → "2026/08/18 12:54:01"
 */
function formatDateTime(iso) {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  } catch {
    return iso;
  }
}

/**
 * Chuyển input người dùng sang LIKE pattern gửi lên backend.
 * DB format: "yyyy-mm-dd hh:mm:ss"
 *
 * Hỗ trợ các dạng:
 *   "01"                   → "%-01 %" (ngày 01 bất kỳ tháng/năm)
 *   "01/09"                → "%-09-01 %" (ngày 01 tháng 09 bất kỳ năm)
 *   "01/09/2026"           → "2026-09-01%" (dd/mm/yyyy)
 *   "2026/09/01"           → "2026-09-01%" (yyyy/mm/dd)
 *   "01/09/2026 17"        → "2026-09-01 17%"
 *   "01/09/2026 17:30"     → "2026-09-01 17:30%"
 *   "01/09/2026 17:30:15"  → "2026-09-01 17:30:15%"
 */
function convertTimeInput(raw) {
  if (!raw) return null;
  raw = raw.trim();
  if (!raw) return null;

  // Tách phần ngày và giờ
  const spaceIdx = raw.indexOf(' ');
  const datePart = spaceIdx >= 0 ? raw.substring(0, spaceIdx) : raw;
  const timePart = spaceIdx >= 0 ? raw.substring(spaceIdx + 1) : '';

  const dateParts = datePart.split(/[\/\-]/);

  // Chỉ 1 phần: dd hoặc yyyy
  if (dateParts.length === 1) {
    if (dateParts[0].length === 4) {
      return `${dateParts[0]}-%`;
    }
    const dd = dateParts[0].padStart(2, '0');
    return `%-${dd} %`;
  }

  // 2 phần: yyyy/mm hoặc dd/mm
  if (dateParts.length === 2) {
    if (dateParts[0].length === 4) {
      const yyyy = dateParts[0];
      const mm = dateParts[1].padStart(2, '0');
      return `${yyyy}-${mm}%`;
    }
    const dd = dateParts[0].padStart(2, '0');
    const mm = dateParts[1].padStart(2, '0');
    return `%-${mm}-${dd} %`;
  }

  // 3 phần: dd/mm/yyyy HOẶC yyyy/mm/dd
  if (dateParts.length === 3) {
    let yyyy, mm, dd;
    if (dateParts[0].length === 4) {
      // yyyy/mm/dd
      yyyy = dateParts[0];
      mm = dateParts[1].padStart(2, '0');
      dd = dateParts[2].padStart(2, '0');
    } else {
      // dd/mm/yyyy (logic cũ)
      dd = dateParts[0].padStart(2, '0');
      mm = dateParts[1].padStart(2, '0');
      yyyy = dateParts[2];
    }
    const isoDate = `${yyyy}-${mm}-${dd}`;
    if (!timePart) return `${isoDate}%`;
    return `${isoDate} ${timePart}%`;
  }

  return raw;
}

