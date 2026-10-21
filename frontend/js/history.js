/**
 * history.js – Logic cho trang History
 * Phải load sau: api.js
 */

// ─── TRẠNG THÁI ──────────────────────────────────────────────────────────────
let currentPage = 1;
let PAGE_SIZE = 10;
let currentFilters = {
  deviceName: '',
  time: '',
  action: '',
  status: '',
  sort: 'desc',
};

let inputDebounce = null;
let isUserTyping = false;

// ─── KHỞI TẠO ────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  bindFilterEvents();
  loadPage(1);
  scheduleAutoRefresh();
});

// ─── AUTO-REFRESH ─────────────────────────────────────────────────────────────
function scheduleAutoRefresh() {
  setTimeout(async () => {
    if (!isUserTyping) await loadPage(currentPage);
    scheduleAutoRefresh();
  }, 2000);
}

// ─── GẮN SỰ KIỆN ─────────────────────────────────────────────────────────────
function bindFilterEvents() {
  const timeInput = document.getElementById('hist-filter-input');
  const deviceSelect = document.getElementById('hist-filter-device');
  const actionSelect = document.getElementById('hist-filter-action');
  const statusSelect = document.getElementById('hist-filter-status');
  const sortToggle = document.getElementById('sort-toggle');
  const pageSizeSelect = document.getElementById('page-size-select');

  // --- Ô tìm kiếm thời gian (debounce)
  if (timeInput) {
    timeInput.addEventListener('focus', () => { isUserTyping = true; });
    timeInput.addEventListener('blur', () => { isUserTyping = false; });
    timeInput.addEventListener('input', () => {
      clearTimeout(inputDebounce);
      inputDebounce = setTimeout(() => {
        currentFilters.time = timeInput.value.trim();
        loadPage(1);
      }, 400);
    });
  }

  // --- Dropdown: tên thiết bị
  if (deviceSelect) {
    deviceSelect.addEventListener('change', () => {
      currentFilters.deviceName = deviceSelect.value;
      loadPage(1);
    });
  }

  // --- Dropdown: hành động
  if (actionSelect) {
    actionSelect.addEventListener('change', () => {
      currentFilters.action = actionSelect.value;
      loadPage(1);
    });
  }

  // --- Dropdown: trạng thái
  if (statusSelect) {
    statusSelect.addEventListener('change', () => {
      currentFilters.status = statusSelect.value;
      loadPage(1);
    });
  }

  // --- Sắp xếp
  if (sortToggle) {
    sortToggle.addEventListener('change', () => {
      currentFilters.sort = sortToggle.checked ? 'asc' : 'desc';
      loadPage(1);
    });
  }

  // --- Page size
  if (pageSizeSelect) {
    pageSizeSelect.addEventListener('change', () => {
      PAGE_SIZE = parseInt(pageSizeSelect.value, 10);
      loadPage(1);
    });
  }
}

// ─── TẢI TRANG ───────────────────────────────────────────────────────────────
async function loadPage(page) {
  currentPage = page;
  const params = {
    page,
    size: PAGE_SIZE,
    sort: currentFilters.sort,
  };
  if (currentFilters.deviceName) params.deviceName = currentFilters.deviceName;
  if (currentFilters.action) params.action = currentFilters.action;
  if (currentFilters.status) params.status = currentFilters.status;
  if (currentFilters.time) params.time = convertTimeInput(currentFilters.time);

  try {
    const data = await fetchDeviceHistory(params);
    renderTable(data.items);
    renderEntriesInfo(data.currentPage, data.totalPages, data.items.length, data.totalItems);
    renderPagination(data.currentPage, data.totalPages);
  } catch (err) {
    console.error('Failed to load history:', err.message);
    renderTable([]);
  }
}

// ─── RENDER BẢNG ─────────────────────────────────────────────────────────────
function renderTable(items) {
  const tbody = document.querySelector('.data-table tbody');
  if (!tbody) return;

  if (!items || items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#999">Không có dữ liệu</td></tr>';
    return;
  }

  tbody.innerHTML = items.map(item => `
    <tr>
      <td>${item.id}</td>
      <td>${item.deviceName}</td>
      <td>${item.action}</td>
      <td>${item.status}</td>
      <td>${item.username ?? '—'}</td>
      <td>${formatDateTime(item.time)}<button class="btn-copy" onclick="copyTime('${formatDateTime(item.time)}', this)" title="Copy">⧉</button></td>
    </tr>
  `).join('');
}

// ─── ENTRIES INFO ─────────────────────────────────────────────────────────────
function renderEntriesInfo(currentPageNum, totalPages, pageItemCount, totalItems) {
  const el = document.getElementById('entries-info');
  if (!el) return;
  if (!totalItems) { el.textContent = ''; return; }
  const from = (currentPageNum - 1) * PAGE_SIZE + 1;
  const to = from + pageItemCount - 1;
  el.textContent = `Hiển thị ${from} đến ${to} trong ${totalItems} mục`;
}

// ─── RENDER PHÂN TRANG ────────────────────────────────────────────────────────
function renderPagination(current, total) {
  const nav = document.querySelector('.pagination');
  if (!nav) return;

  const btns = [];
  btns.push(`
    <button class="page-btn" type="button" ${current <= 1 ? 'disabled' : ''}
      onclick="loadPage(${current - 1})">
      <img src="assets/chevron-left.svg" alt="" width="16" height="16" /> Trước
    </button>
  `);

  getPageNumbers(current, total).forEach(p => {
    if (p === '...') {
      btns.push(`<button class="page-btn" type="button" disabled>
        <img src="assets/ellipsis.svg" alt="" width="16" height="16" /></button>`);
    } else {
      btns.push(`<button class="page-btn ${p === current ? 'is-active' : ''}" type="button"
        onclick="loadPage(${p})">${p}</button>`);
    }
  });

  btns.push(`
    <button class="page-btn" type="button" ${current >= total ? 'disabled' : ''}
      onclick="loadPage(${current + 1})">
      Sau <img src="assets/chevron-right.svg" alt="" width="16" height="16" />
    </button>
  `);

  nav.innerHTML = btns.join('');
}

function getPageNumbers(current, total) {
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = [];
  if (current <= 3) pages.push(1, 2, 3, '...', total);
  else if (current >= total - 2) pages.push(1, '...', total - 2, total - 1, total);
  else pages.push(1, '...', current - 1, current, current + 1, '...', total);
  return pages;
}

function copyTime(text, btn) {
  navigator.clipboard.writeText(text).then(() => {
    btn.textContent = '✓';
    setTimeout(() => btn.textContent = '⧉', 1000);
  });
}