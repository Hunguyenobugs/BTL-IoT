/**
 * dashboard.js – Logic cho trang Dashboard
 * Phải load sau: api.js, device-control.js
 */

// ─── HẰNG SỐ ─────────────────────────────────────────────────────────────────
const POLL_SENSOR_MS = 2000;
const POLL_CHART_MS = 2000;
const MAX_FAIL_COUNT = 5;

// ─── BIẾN TRẠNG THÁI ─────────────────────────────────────────────────────────
let sensorFailCount = 0;
let chartInstance = null;

// ─── KHỞI TẠO ────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initDashboard();
});

async function initDashboard() {
  // 1. Tải trạng thái thực tế của 2 đèn từ API
  await loadDeviceStatus();

  // 2. Gắn sự kiện toggle
  bindToggleEvents();

  // 3. Load lần đầu ngay lập tức
  await updateSensorValues();
  await updateChart();

  // 4. Bắt đầu polling bằng setTimeout-chain (tránh overlap khi request chậm)
  scheduleSensorPoll();
  scheduleChartPoll();
}

// ─── CẢM BIẾN ────────────────────────────────────────────────────────────────

async function updateSensorValues() {
  try {
    const data = await fetchLatestSensors();
    sensorFailCount = 0;
    hideBanner();
    updateStatCard('temp', data.temperature?.value, data.temperature?.unit);
    updateStatCard('humidity', data.humidity?.value, data.humidity?.unit);
    updateStatCard('light', data.light?.value, data.light?.unit);
  } catch (err) {
    sensorFailCount++;
    if (sensorFailCount >= MAX_FAIL_COUNT) {
      showBanner('Mất kết nối tới server');
    }
    console.warn('Sensor fetch failed:', err.message);
  }
}

/**
 * setTimeout-chain: chỉ schedule lần kế sau khi lần hiện tại đã xong.
 * Tránh tình trạng nhiều request chồng chất khi mạng chậm.
 */
function scheduleSensorPoll() {
  setTimeout(async () => {
    await updateSensorValues();
    scheduleSensorPoll(); // schedule tiếp
  }, POLL_SENSOR_MS);
}

function scheduleChartPoll() {
  setTimeout(async () => {
    await updateChart();
    scheduleChartPoll();
  }, POLL_CHART_MS);
}

function updateStatCard(type, value, unit) {
  const idMap = {
    temp: 'stat-val-temp',
    humidity: 'stat-val-humidity',
    light: 'stat-val-light',
  };
  const el = document.getElementById(idMap[type]);
  if (!el) return;

  if (value === undefined || value === null) {
    el.textContent = '—';
    return;
  }
  const formatted = (Number.isInteger(value) || value % 1 === 0)
    ? Math.round(value) + (unit || '')
    : value.toFixed(1) + (unit || '');
  el.textContent = formatted;
}

// ─── BIỂU ĐỒ ─────────────────────────────────────────────────────────────────

async function updateChart() {
  try {
    const data = await fetchChartData(15);
    const canvas = document.getElementById('sensor-chart');
    if (!canvas) return;

    if (!chartInstance) {
      chartInstance = createChart(canvas, data);
    } else {
      chartInstance.data.labels = data.labels;
      chartInstance.data.datasets[0].data = data.temperature;
      chartInstance.data.datasets[1].data = data.humidity;
      chartInstance.data.datasets[2].data = data.light;
      chartInstance.update('none');
    }
  } catch (err) {
    console.warn('Chart fetch failed:', err.message);
  }
}

function createChart(canvas, data) {
  return new Chart(canvas, {
    type: 'line',
    data: {
      labels: data.labels,
      datasets: [
        {
          label: 'Nhiệt độ (°C)',
          data: data.temperature,
          borderColor: '#ff6b6b',
          backgroundColor: 'rgba(255,107,107,0.08)',
          borderWidth: 2.5,
          pointRadius: 3,
          pointHoverRadius: 7,
          pointBackgroundColor: '#ff6b6b',
          tension: 0.3,
          yAxisID: 'y',
        },
        {
          label: 'Độ ẩm (%)',
          data: data.humidity,
          borderColor: '#4cc9f0',
          backgroundColor: 'rgba(76,201,240,0.08)',
          borderWidth: 2.5,
          pointRadius: 3,
          pointHoverRadius: 7,
          pointBackgroundColor: '#4cc9f0',
          tension: 0.3,
          yAxisID: 'y',
        },
        {
          label: 'Ánh sáng (lux)',
          data: data.light,
          borderColor: '#f4a261',
          backgroundColor: 'rgba(244,162,97,0.08)',
          borderWidth: 2.5,
          pointRadius: 3,
          pointHoverRadius: 7,
          pointBackgroundColor: '#f4a261',
          tension: 0.3,
          yAxisID: 'y2',
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,       // tắt animation khi data thay đổi liên tục
      interaction: {
        mode: 'nearest',
        axis: 'xy',
        intersect: true
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          mode: 'nearest',
          intersect: true,
          position: 'nearest',
          caretPadding: 6,
          caretSize: 6,
          callbacks: {
            label: ctx => `${ctx.dataset.label}: ${ctx.parsed.y}`,
          },
        },
      },
      scales: {
        x: {
          ticks: { maxRotation: 45, font: { size: 11 } },
          grid: { color: 'rgba(0,0,0,0.06)' },
        },
        y: {
          position: 'left',
          min: 0,
          max: 100,
          title: { display: true, text: '°C  /  %' },
          grid: { color: 'rgba(0,0,0,0.06)' },
          ticks: { stepSize: 10 },
        },
        y2: {
          position: 'right',
          min: 0,
          max: 1000,
          title: { display: true, text: 'lux' },
          grid: { drawOnChartArea: false },
          ticks: { stepSize: 100 },
        },
      },
    },
  });
}

// ─── TOGGLE LED ───────────────────────────────────────────────────────────────

async function loadDeviceStatus() {
  try {
    const devices = await fetchDeviceStatus();
    devices.forEach(d => {
      const toggleEl = document.getElementById(`led${d.deviceId}-toggle`);
      if (toggleEl) {
        toggleEl.checked = (d.status === 'ON');
        updateLampIcon(d.deviceId, d.status === 'ON');
      }
    });
  } catch (err) {
    console.warn('Failed to load device status:', err.message);
  }
}

/**
 * Cập nhật icon đèn: thay đổi src của thẻ img
 * @param {number} deviceId
 * @param {boolean} isOn
 */
function updateLampIcon(deviceId, isOn) {
  const icon = document.getElementById(`lamp-icon-${deviceId}`);
  if (!icon) return;
  if (isOn) {
    icon.src = 'assets/light-on.png';
  } else {
    icon.src = 'assets/light-off.png';
  }
}

function bindToggleEvents() {
  const led1 = document.getElementById('led1-toggle');
  if (led1) {
    led1.addEventListener('change', async (e) => {
      const prevChecked = !e.target.checked;
      const action = e.target.checked ? 'ON' : 'OFF';
      // Optimistic: sáng/tắt ngay khi bấm
      updateLampIcon(1, e.target.checked);
      // onComplete được gọi SAU KHI polling nhận kết quả thực (ON/OFF/FAILED/timeout)
      await controlDevice(1, action, e.target, prevChecked, (isOn) => updateLampIcon(1, isOn));
    });
  }

  const led2 = document.getElementById('led2-toggle');
  if (led2) {
    led2.addEventListener('change', async (e) => {
      const prevChecked = !e.target.checked;
      const action = e.target.checked ? 'ON' : 'OFF';
      updateLampIcon(2, e.target.checked);
      await controlDevice(2, action, e.target, prevChecked, (isOn) => updateLampIcon(2, isOn));
    });
  }
}

// ─── BANNER ──────────────────────────────────────────────────────────────────



function showBanner(message) {
  const banner = document.getElementById('connection-banner');
  if (!banner) return;
  banner.textContent = message;
  banner.style.display = 'flex';
}

function hideBanner() {
  const banner = document.getElementById('connection-banner');
  if (banner) banner.style.display = 'none';
}
