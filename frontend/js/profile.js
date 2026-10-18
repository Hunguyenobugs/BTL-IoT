/**
 * profile.js – Tải thông tin sinh viên từ API và điền vào trang Profile
 * Phải load sau: api.js
 */

document.addEventListener('DOMContentLoaded', async () => {
  try {
    const user = await apiGetMe();
    renderProfile(user);
  } catch (err) {
    console.error('Failed to load profile:', err.message);
  }
});

function renderProfile(user) {
  // Tên
  const nameEl = document.getElementById('profile-name');
  if (nameEl) nameEl.textContent = user.name || '—';

  // Mã sinh viên
  const codeEl = document.getElementById('profile-student-code');
  if (codeEl) codeEl.textContent = user.studentCode || '—';

  // Lớp
  const classEl = document.getElementById('profile-class');
  if (classEl) classEl.textContent = user.classField || '—';

  // Links
  setLink('profile-link-github',  user.linkGithub,  'Xem tại đây');
  setLink('profile-link-figma',   user.linkFigma,   'Xem tại đây');
  setLink('profile-link-postman', user.linkPostman, 'Xem tại đây');
  setLink('profile-link-report',  user.linkReport,  'Xem tại đây');

  // Avatar alt text
  const avatarImg = document.querySelector('.avatar img');
  if (avatarImg && user.name) avatarImg.alt = user.name;
}

function setLink(id, href, text) {
  const el = document.getElementById(id);
  if (!el) return;
  if (href && href !== '#' && href !== '') {
    el.href = href;
    el.textContent = text;
    el.style.pointerEvents = 'auto';
    el.style.opacity = '1';
  } else {
    el.href = '#';
    el.textContent = 'Chưa cập nhật';
    el.style.opacity = '0.4';
    el.style.pointerEvents = 'none';
  }
}
