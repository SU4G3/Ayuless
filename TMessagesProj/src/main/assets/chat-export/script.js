/* SPDX-License-Identifier: GPL-3.0-or-later */
'use strict';
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.spoiler').forEach(element => element.tabIndex = 0);
  document.addEventListener('click', async event => {
    const button = event.target.closest('button[data-copy]');
    if (!button) return;
    const text = button.dataset.copy;
    try {
      await navigator.clipboard.writeText(text);
    } catch (_) {
      // Clipboard access may be unavailable for local files. Show the exported payload instead.
      window.prompt('Exported button data (no request is sent):', text);
    }
  });
});
