/**
 * login.js – Xử lý logic cho trang đăng nhập / đăng ký riêng
 */

// Nếu đã đăng nhập rồi, tự động chuyển về dashboard
if (localStorage.getItem('token')) {
  window.location.replace('dashboard.html');
}

document.addEventListener('DOMContentLoaded', () => {
  const tabLogin = document.getElementById('tab-login');
  const tabRegister = document.getElementById('tab-register');
  const paneLogin = document.getElementById('pane-login');
  const paneRegister = document.getElementById('pane-register');
  const switchToRegister = document.getElementById('switch-to-register');
  const switchToLogin = document.getElementById('switch-to-login');

  const formLogin = document.getElementById('form-page-login');
  const formRegister = document.getElementById('form-page-register');
  const loginError = document.getElementById('login-error');
  const regError = document.getElementById('reg-error');

  function showTab(tab) {
    if (tab === 'login') {
      tabLogin.classList.add('is-active');
      tabRegister.classList.remove('is-active');
      paneLogin.style.display = 'block';
      paneRegister.style.display = 'none';
      clearErrors();
    } else {
      tabRegister.classList.add('is-active');
      tabLogin.classList.remove('is-active');
      paneRegister.style.display = 'block';
      paneLogin.style.display = 'none';
      clearErrors();
    }
  }

  tabLogin.addEventListener('click', () => showTab('login'));
  tabRegister.addEventListener('click', () => showTab('register'));
  switchToRegister?.addEventListener('click', () => showTab('register'));
  switchToLogin?.addEventListener('click', () => showTab('login'));

  function clearErrors() {
    loginError.style.display = 'none';
    regError.style.display = 'none';
  }

  // ─── Xử lý Đăng nhập ───────────────────────────────────────────────────────
  formLogin.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('btn-submit-login');

    if (!username || !password) {
      loginError.textContent = 'Vui lòng nhập tên đăng nhập và mật khẩu';
      loginError.style.display = 'block';
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Đang đăng nhập...';

    try {
      const data = await apiLogin(username, password);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({
        id: data.id,
        username: data.username,
        name: data.name,
        studentCode: data.studentCode,
        classField: data.classField
      }));

      // Đăng nhập thành công -> chuyển về dashboard
      window.location.replace('dashboard.html');
    } catch (err) {
      loginError.textContent = err.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại.';
      loginError.style.display = 'block';
    } finally {
      btn.disabled = false;
      btn.textContent = 'Đăng nhập';
    }
  });

  // ─── Xử lý Đăng ký ─────────────────────────────────────────────────────────
  formRegister.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const username = document.getElementById('reg-username').value.trim();
    const password = document.getElementById('reg-password').value;
    const confirmPassword = document.getElementById('reg-confirm-password').value;
    const name = document.getElementById('reg-name').value.trim();
    const studentCode = document.getElementById('reg-student-code').value.trim();
    const classField = document.getElementById('reg-class').value.trim();
    const avatarUrl = document.getElementById('reg-avatar').value.trim();
    const linkGithub = document.getElementById('reg-github').value.trim();
    const linkFigma = document.getElementById('reg-figma').value.trim();
    const linkPostman = document.getElementById('reg-postman').value.trim();
    const linkReport = document.getElementById('reg-report').value.trim();

    const btn = document.getElementById('btn-submit-register');

    if (password.length < 6) {
      regError.textContent = 'Mật khẩu phải có ít nhất 6 ký tự';
      regError.style.display = 'block';
      return;
    }
    if (password !== confirmPassword) {
      regError.textContent = 'Mật khẩu xác nhận không khớp';
      regError.style.display = 'block';
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Đang tạo tài khoản...';

    try {
      const data = await apiRegister({
        username,
        password,
        name,
        studentCode,
        classField: classField || null,
        // ===== gửi 5 trường mới =====
        avatarUrl: avatarUrl || null,
        linkGithub: linkGithub || null,
        linkFigma: linkFigma || null,
        linkPostman: linkPostman || null,
        linkReport: linkReport || null,
      });

      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({
        id: data.id,
        username: data.username,
        name: data.name,
        studentCode: data.studentCode,
        classField: data.classField,
        avatarUrl: data.avatarUrl,   // ← thêm để sidebar dùng
      }));

      window.location.replace('dashboard.html');
    } catch (err) {
      regError.textContent = err.message || 'Đăng ký thất bại';
      regError.style.display = 'block';
    } finally {
      btn.disabled = false;
      btn.textContent = 'Tạo tài khoản';
    }
  });
});
