/**
 * auth.js – Quản lý trạng thái Đăng nhập, Đăng xuất, Đổi mật khẩu
 * Tự động bảo vệ các trang Dashboard, Data sensor, History, Profile.
 * Gắn widget người dùng và các thao tác vào dưới cùng bên trái ở sidebar.
 */

(function () {
  const token = localStorage.getItem('token');
  const path = window.location.pathname;
  const isLoginPage = path.endsWith('login.html');

  // ─── ROUTE GUARD ──────────────────────────────────────────────────────────
  // Bắt buộc phải đăng nhập mới có thể truy cập các trang nội bộ
  if (!token && !isLoginPage) {
    window.location.replace('login.html');
    return;
  }

  // Nếu đã đăng nhập mà truy cập login.html thì tự động vào dashboard.html
  if (token && isLoginPage) {
    window.location.replace('dashboard.html');
    return;
  }

  // ─── Khởi tạo khi DOM sẵn sàng ──────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', initAuth);
  window.addEventListener('auth:state-change', renderSidebarAuth);

  function getCurrentUser() {
    try {
      const u = localStorage.getItem('user');
      return u ? JSON.parse(u) : null;
    } catch {
      return null;
    }
  }

  function initAuth() {
    setupSidebarContainer();
    renderSidebarAuth();
    setupChangePasswordModal();
  }

  // ─── Gắn container auth vào dưới cùng sidebar ──────────────────────────────
  function setupSidebarContainer() {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;

    let authContainer = document.getElementById('sidebar-auth');
    if (!authContainer) {
      authContainer = document.createElement('div');
      authContainer.id = 'sidebar-auth';
      authContainer.className = 'sidebar-auth';
      sidebar.appendChild(authContainer);
    }
  }

  // ─── Hiển thị trạng thái ở Sidebar (dưới cùng bên trái) ────────────────────
  function renderSidebarAuth() {
    const container = document.getElementById('sidebar-auth');
    if (!container) return;

    const user = getCurrentUser();
    const displayName = (user && user.name) ? user.name : (user && user.username ? user.username : 'Người dùng');
    // Hiển thị mã sinh viên, nếu không có thì hiện username
    const subText = user && user.studentCode ? user.studentCode : (user && user.username ? user.username : '');

    // Lấy chữ cái đầu làm Avatar
    const initial = displayName.charAt(0).toUpperCase();

    container.innerHTML = `
      <div class="sidebar-user-container">
        <!-- Menu popup ẩn, hiện khi có class is-open -->
        <div class="sidebar-user-menu" id="sidebar-user-menu">
          <button type="button" class="btn-sidebar-menu-item" id="btn-sidebar-change-pwd">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <span>Đổi mật khẩu</span>
          </button>
          <button type="button" class="btn-sidebar-menu-item btn-logout" id="btn-sidebar-logout">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            <span>Đăng xuất</span>
          </button>
        </div>

        <!-- Nút bấm chính (Avatar + Tên + Dấu 3 chấm) -->
        <div class="sidebar-user-trigger" id="sidebar-user-trigger">
          <div class="sidebar-user-avatar-badge">${escapeHtml(initial)}</div>
          <div class="sidebar-user-meta">
            <span class="sidebar-user-name" title="${escapeHtml(displayName)}">${escapeHtml(displayName)}</span>
            <span class="sidebar-user-sub" title="${escapeHtml(subText)}">${escapeHtml(subText)}</span>
          </div>
          <div class="sidebar-user-more">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>
          </div>
        </div>
      </div>
    `;

    const trigger = document.getElementById('sidebar-user-trigger');
    const menu = document.getElementById('sidebar-user-menu');

    // Bật/tắt menu khi click vào khu vực user
    trigger?.addEventListener('click', (e) => {
      e.stopPropagation(); // Ngăn sự kiện click lan ra ngoài document
      menu?.classList.toggle('is-open');
    });

    // Click ra ngoài thì đóng menu
    document.addEventListener('click', (e) => {
      if (menu && !menu.contains(e.target) && !trigger?.contains(e.target)) {
        menu.classList.remove('is-open');
      }
    });

    // Sự kiện Đổi mật khẩu
    document.getElementById('btn-sidebar-change-pwd')?.addEventListener('click', () => {
      menu?.classList.remove('is-open');
      openModal('change-password');
    });

    // Sự kiện Đăng xuất (đăng xuất luôn không hỏi)
    document.getElementById('btn-sidebar-logout')?.addEventListener('click', () => {
      handleLogout();
    });
  }

  // ─── Đăng xuất ─────────────────────────────────────────────────────────────
  function handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.replace('login.html');
  }

  // ─── Modal Đổi mật khẩu ───────────────────────────────────────────────────
  function setupChangePasswordModal() {
    if (document.getElementById('auth-modal-overlay')) return;

    const overlay = document.createElement('div');
    overlay.id = 'auth-modal-overlay';
    overlay.className = 'auth-modal-overlay';
    overlay.innerHTML = `
      <div class="auth-modal-card">
        <button type="button" class="auth-modal-close" id="auth-modal-close" aria-label="Đóng">&times;</button>
        <div id="auth-modal-content">
          <h2 class="auth-modal-title">Đổi mật khẩu</h2>
          <p class="auth-modal-desc">Cập nhật mật khẩu mới cho tài khoản của bạn</p>
          <div id="auth-error" class="auth-form-error"></div>
          <div id="auth-success" class="auth-form-success"></div>
          <form id="form-change-password">
            <div class="auth-form-group">
              <label for="pwd-old">Mật khẩu hiện tại *</label>
              <input type="password" id="pwd-old" placeholder="Nhập mật khẩu hiện tại" required autocomplete="current-password" />
            </div>
            <div class="auth-form-group">
              <label for="pwd-new">Mật khẩu mới *</label>
              <input type="password" id="pwd-new" placeholder="Tối thiểu 6 ký tự" required autocomplete="new-password" />
            </div>
            <div class="auth-form-group">
              <label for="pwd-confirm">Xác nhận mật khẩu mới *</label>
              <input type="password" id="pwd-confirm" placeholder="Nhập lại mật khẩu mới" required autocomplete="new-password" />
            </div>
            <button type="submit" class="btn-auth-submit" id="btn-submit-change-pwd">Lưu thay đổi</button>
          </form>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    overlay.addEventListener('click', (e) => {
      if (e.target === overlay || e.target.id === 'auth-modal-close') {
        closeModal();
      }
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeModal();
    });

    bindChangePasswordForm();
  }

  function openModal() {
    const overlay = document.getElementById('auth-modal-overlay');
    if (!overlay) return;
    clearError();
    const successEl = document.getElementById('auth-success');
    if (successEl) successEl.style.display = 'none';
    const form = document.getElementById('form-change-password');
    if (form) form.reset();

    overlay.classList.add('is-visible');
  }

  function closeModal() {
    const overlay = document.getElementById('auth-modal-overlay');
    if (overlay) overlay.classList.remove('is-visible');
  }

  function showError(msg) {
    const el = document.getElementById('auth-error');
    if (el) {
      el.textContent = msg;
      el.style.display = 'block';
    }
  }

  function clearError() {
    const el = document.getElementById('auth-error');
    if (el) el.style.display = 'none';
  }

  function showSuccess(msg) {
    const el = document.getElementById('auth-success');
    if (el) {
      el.textContent = msg;
      el.style.display = 'block';
    }
  }

  function bindChangePasswordForm() {
    const form = document.getElementById('form-change-password');
    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearError();

      const oldPassword = document.getElementById('pwd-old').value;
      const newPassword = document.getElementById('pwd-new').value;
      const confirmPassword = document.getElementById('pwd-confirm').value;
      const btn = document.getElementById('btn-submit-change-pwd');

      if (newPassword.length < 6) {
        showError('Mật khẩu mới phải có ít nhất 6 ký tự');
        return;
      }

      if (newPassword !== confirmPassword) {
        showError('Mật khẩu xác nhận không khớp');
        return;
      }

      if (oldPassword === newPassword) {
        showError('Mật khẩu mới không được trùng với mật khẩu cũ');
        return;
      }

      btn.disabled = true;
      btn.textContent = 'Đang xử lý...';

      try {
        await apiChangePassword(oldPassword, newPassword);
        showSuccess('Đổi mật khẩu thành công!');
        form.reset();
        setTimeout(() => {
          closeModal();
        }, 1500);
      } catch (err) {
        showError(err.message || 'Đổi mật khẩu thất bại');
      } finally {
        btn.disabled = false;
        btn.textContent = 'Lưu thay đổi';
      }
    });
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
})();
