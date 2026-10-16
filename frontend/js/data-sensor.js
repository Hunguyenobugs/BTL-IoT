/**
 * data-sensor.js – Logic cho trang Data Sensor
 * Phải load sau: api.js
 */

// ─── TRẠNG THÁI ──────────────────────────────────────────────────────────────
let currentPage = 1;
let PAGE_SIZE = 10;   // có thể thay đổi qua dropdown
let currentFilters = {
  type: '',
  time: '',
  value: '',
  sort: 'desc',
};

// Chế độ tìm kiếm: 'all' | 'sensor' | 'value' | 'time'
let searchType = 'all';

// Debounce timer cho input
let inputDebounce = null;

// Auto-refresh: chỉ refresh khi user không đang gõ
let isUserTyping = false;

// Lưu chuỗi tất cả IDs để so sánh, chỉ render lại khi có thay đổi
let lastItemsKey = null;

// ─── KHỞI TẠO ────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const searchTypeEl = document.getElementById('filter-search-type');
  const searchInput = document.getElementById('filter-search');
  if (searchTypeEl) {
    searchType = searchTypeEl.value;
    if (['light', 'temperature', 'humidity'].includes(searchType)) {
      currentFilters.type = searchType;
    }
    if (searchInput) {
      searchInput.placeholder = getSearchPlaceholder(searchType);
    }
  }
  bindFilterEvents();
  loadPage(1);
  scheduleAutoRefresh();
});

// ─── AUTO-REFRESH (setTimeout-chain) ─────────────────────────────────────────
function scheduleAutoRefresh() {
  setTimeout(async () => {
    if (!isUserTyping) {
      await loadPage(currentPage);
    }
    scheduleAutoRefresh();
  }, 2000);
}

// ─── GẮN SỰ KIỆN FILTER ──────────────────────────────────────────────────────
function bindFilterEvents() {
  const searchInput = document.getElementById('filter-search');
  const searchTypeEl = document.getElementById('filter-search-type');
  const sortToggle = document.getElementById('sort-toggle');
  const pageSizeSelect = document.getElementById('page-size-select');

  // --- Dropdown: chọn loại tìm kiếm
  if (searchTypeEl) {
    searchTypeEl.addEventListener('change', () => {
      searchType = searchTypeEl.value;
      // Reset tất cả filter khi đổi loại
      currentFilters.type = '';
      currentFilters.time = '';
      currentFilters.value = '';
      if (['light', 'temperature', 'humidity'].includes(searchType)) {
        currentFilters.type = searchType;
      }
      if (searchInput) {
        searchInput.value = '';
        searchInput.placeholder = getSearchPlaceholder(searchType);
      }
      loadPage(1);
    });
  }

  // --- Ô tìm kiếm chính
  if (searchInput) {
    searchInput.addEventListener('focus', () => { isUserTyping = true; });
    searchInput.addEventListener('blur', () => { isUserTyping = false; });
    searchInput.addEventListener('input', () => {
      clearTimeout(inputDebounce);
      inputDebounce = setTimeout(() => {
        applySearchFilter(searchInput.value.trim());
        loadPage(1);
      }, 400);
    });
  }

  // --- Sort
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
      lastItemsKey = null; // force re-render
      loadPage(1);
    });
  }
}

/**
 * Áp dụng giá trị tìm kiếm vào currentFilters dựa trên searchType.
 */
function applySearchFilter(val) {
  // Reset trước
  currentFilters.time = '';
  currentFilters.value = '';

  if (['light', 'temperature', 'humidity'].includes(searchType)) {
    currentFilters.type = searchType;
    if (val) {
      currentFilters.value = val;
    }
    return;
  }

  if (searchType === 'time') {
    currentFilters.type = '';
    currentFilters.time = val;
    return;
  }

  // searchType === 'all'
  currentFilters.type = '';
  if (!val) return;

  // Heuristic: chứa "/" hoặc ":" → có thể là thời gian
  if (/[\/:]/g.test(val)) {
    currentFilters.time = val;
  } else {
    currentFilters.value = val;
  }
}

function getSearchPlaceholder(type) {
  switch (type) {
    case 'light': return 'Nhập giá trị ánh sáng…';
    case 'temperature': return 'Nhập giá trị nhiệt độ…';
    case 'humidity': return 'Nhập giá trị độ ẩm…';
    case 'time': return 'Nhập thời gian (yyyy/MM/dd hh:mm:ss)…';
    default: return 'Nhập giá trị tìm kiếm…';
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
  if (currentFilters.type && currentFilters.type !== 'all') params.type = currentFilters.type;
  if (currentFilters.value) params.value = currentFilters.value;
  if (currentFilters.time) params.time = convertTimeInput(currentFilters.time);

  try {
    const data = await fetchSensorList(params);

    // So sánh toàn bộ danh sách ID để phát hiện thay đổi ở bất kỳ hàng nào
    const newKey = data.items.map(i => i.id).join(',');
    if (newKey !== lastItemsKey) {
      lastItemsKey = newKey;
      renderTable(data.items);
    }

    renderEntriesInfo(data.currentPage, data.totalPages, data.items.length, data.totalItems);
    renderPagination(data.currentPage, data.totalPages);
  } catch (err) {
    console.error('Failed to load sensor list:', err.message);
    renderTable([]);
  }
}

// ─── RENDER BẢNG ─────────────────────────────────────────────────────────────
// Dùng DocumentFragment để cập nhật DOM một lần duy nhất → giảm reflow/repaint
function renderTable(items) {
  const tbody = document.querySelector('.data-table tbody');
  if (!tbody) return;

  if (!items || items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#999">Không có dữ liệu</td></tr>';
    return;
  }

  const fragment = document.createDocumentFragment();
  items.forEach(item => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${item.id}</td>
      <td>${item.sensorType}</td>
      <td>${item.value}</td>
      <td>${formatDateTime(item.time)}</td>
    `;
    fragment.appendChild(tr);
  });

  // Thay toàn bộ nội dung tbody chỉ bằng 1 thao tác DOM duy nhất
  tbody.replaceChildren(fragment);
}

// ─── ENTRIES INFO ────────────────────────────────────────────────
function renderEntriesInfo(currentPage, totalPages, pageItemCount, totalItems) {
  const el = document.getElementById('entries-info');
  if (!el) return;
  if (!totalItems) { el.textContent = ''; return; }
  const from = (currentPage - 1) * PAGE_SIZE + 1;
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
      <img src="assets/chevron-left.svg" alt="" width="16" height="16" />
      Trước
    </button>
  `);

  const pages = getPageNumbers(current, total);
  pages.forEach(p => {
    if (p === '...') {
      btns.push(`<button class="page-btn" type="button" disabled>
        <img src="assets/ellipsis.svg" alt="" width="16" height="16" />
      </button>`);
    } else {
      btns.push(`<button class="page-btn ${p === current ? 'is-active' : ''}" type="button"
        onclick="loadPage(${p})">${p}</button>`);
    }
  });

  btns.push(`
    <button class="page-btn" type="button" ${current >= total ? 'disabled' : ''}
      onclick="loadPage(${current + 1})">
      Sau
      <img src="assets/chevron-right.svg" alt="" width="16" height="16" />
    </button>
  `);

  nav.innerHTML = btns.join('');
}

function getPageNumbers(current, total) {
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = [];
  if (current <= 3) {
    pages.push(1, 2, 3, '...', total);
  } else if (current >= total - 2) {
    pages.push(1, '...', total - 2, total - 1, total);
  } else {
    pages.push(1, '...', current - 1, current, current + 1, '...', total);
  }
  return pages;
}
