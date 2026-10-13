/**
 * device-control.js – Xử lý toàn bộ luồng điều khiển LED:
 *   POST /control → set UI waiting → polling GET status → 3 kết cục
 *
 * Phải load sau api.js
 */

// Timeout phía client (ms) – đặt lớn hơn backend một chút để tránh race condition
const CLIENT_TIMEOUT_MS = 12000;
// Interval polling (ms)
const POLL_INTERVAL_MS = 1500;

/**
 * Map deviceId → interval handle (tránh leak nhiều interval nếu user bấm nhanh)
 * @type {Map<number, number>}
 */
const activePollers = new Map();
const clientTimeouts = new Map();

/**
 * Điều khiển LED: gửi lệnh và quản lý toàn bộ lifecycle WAITING → kết quả
 *
 * @param {number} deviceId     ID thiết bị (1 hoặc 2)
 * @param {string} action       'ON' hoặc 'OFF'
 * @param {HTMLInputElement} toggleEl  Element checkbox của toggle
 * @param {boolean} prevChecked Trạng thái checked trước khi bấm (để rollback nếu FAILED)
 * @param {function(boolean): void} [onComplete]  Callback khi có kết quả cuối cùng (isOn)
 *   – Được gọi sau polling ON/OFF/FAILED, sau timeout, hoặc sau lỗi POST
 */
async function controlDevice(deviceId, action, toggleEl, prevChecked, onComplete) {
  // ── 1. Ngăn double-click: disable toggle, set UI waiting ──────────────────
  toggleEl.disabled = true;
  setDeviceWaiting(deviceId, true);

  try {
    // ── 2. Gọi POST /api/devices/{id}/control ──────────────────────────────
    const data = await postDeviceControl(deviceId, action);
    const historyId = data.historyId;

    // ── 3. Bắt đầu polling ─────────────────────────────────────────────────
    clearExistingPoller(deviceId);

    const intervalId = setInterval(async () => {
      try {
        const statusData = await fetchHistoryStatus(historyId);
        const status = statusData.status;

        if (status !== 'WAITING') {
          // Có kết quả → dừng polling
          clearExistingPoller(deviceId);

          let finalIsOn = prevChecked;
          if (status === 'ON' || status === 'OFF') {
            // Thành công: cập nhật toggle theo trạng thái thực tế
            finalIsOn = (status === 'ON');
            toggleEl.checked = finalIsOn;
            showDeviceNotification(deviceId, status, false);
          } else if (status === 'FAILED') {
            // Thất bại: rollback toggle
            finalIsOn = prevChecked;
            toggleEl.checked = finalIsOn;
            showDeviceNotification(deviceId, null, true);
          }

          toggleEl.disabled = false;
          setDeviceWaiting(deviceId, false);
          // Thông báo kết quả cuối cho caller (ví dụ: cập nhật icon đèn)
          if (onComplete) onComplete(finalIsOn);
        }
      } catch (err) {
        // Lỗi mạng khi polling: tiếp tục poll, client timeout sẽ xử lý
        console.warn(`Polling error for historyId ${historyId}:`, err.message);
      }
    }, POLL_INTERVAL_MS);

    activePollers.set(deviceId, intervalId);

    // ── 4. Client-side timeout (phòng lỗi mạng vĩnh viễn) ─────────────────
    const timeoutId = setTimeout(() => {
      clearExistingPoller(deviceId);
      // Rollback toggle
      toggleEl.checked = prevChecked;
      toggleEl.disabled = false;
      setDeviceWaiting(deviceId, false);
      showDeviceNotification(deviceId, null, true);
      if (onComplete) onComplete(prevChecked);
    }, CLIENT_TIMEOUT_MS);

    clientTimeouts.set(deviceId, timeoutId);

  } catch (err) {
    // POST /control thất bại (lỗi mạng, server error, v.v.)
    console.error(`Control failed for device ${deviceId}:`, err.message);
    toggleEl.checked = prevChecked;
    toggleEl.disabled = false;
    setDeviceWaiting(deviceId, false);
    showDeviceNotification(deviceId, null, true);
    if (onComplete) onComplete(prevChecked);
  }
}

/** Xóa interval + timeout cũ nếu còn đang chạy */
function clearExistingPoller(deviceId) {
  if (activePollers.has(deviceId)) {
    clearInterval(activePollers.get(deviceId));
    activePollers.delete(deviceId);
  }
  if (clientTimeouts.has(deviceId)) {
    clearTimeout(clientTimeouts.get(deviceId));
    clientTimeouts.delete(deviceId);
  }
}

/**
 * Cập nhật UI "đang chờ" cho 1 thiết bị.
 * Tìm `.device-waiting-${deviceId}` (spinner) và toggle class.
 */
function setDeviceWaiting(deviceId, isWaiting) {
  const spinner = document.getElementById(`device-spinner-${deviceId}`);
  if (spinner) {
    spinner.style.display = isWaiting ? 'inline-block' : 'none';
  }
}

/**
 * Hiển thị thông báo nhỏ bên cạnh device row.
 * @param {number} deviceId
 * @param {string|null} status  'ON'/'OFF' nếu thành công, null nếu lỗi
 * @param {boolean} isError
 */
function showDeviceNotification(deviceId, status, isError) {
  const el = document.getElementById(`device-msg-${deviceId}`);
  if (!el) return;

  if (isError) {
    el.textContent = 'Không phản hồi, thử lại!';
    el.className = 'device-msg device-msg-error';
  } else {
    el.textContent = `Đã ${status === 'ON' ? 'bật' : 'tắt'}`;
    el.className = 'device-msg device-msg-ok';
  }
  el.style.display = 'inline';
  setTimeout(() => { el.style.display = 'none'; }, 3000);
}
