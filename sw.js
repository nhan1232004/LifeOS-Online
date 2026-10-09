const CACHE = 'lifeos-v14-fix-pomodoro-savings';
const APP_SHELL = [
  './',
  './index.html',
  './manifest.json',
  './icon.svg',
  './css/style.css',
  './css/bento.css',
  './css/modules.css',
  './js/data.js',
  './js/main.js',
  './js/workspace.js',
  './js/projViews.js',
  './js/modules/cmd.js',
  './js/modules/export.js',
  './js/modules/goals.js',
  './js/modules/habits.js',
  './js/modules/journal.js',
  './js/modules/mocktests.js',
  './js/modules/notes.js',
  './js/modules/pomodoro.js',
  './js/modules/todos.js',
  './js/modules/vocab.js',
  './js/modules/mobile.js',
  './js/modules/ai/chat.js',
  './js/modules/ai/context.js',
  './js/modules/ai/tools.js'
];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(APP_SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => Promise.all(cacheNames.filter(name => name !== CACHE).map(name => caches.delete(name))))
  );
  self.clients.claim();
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  if (event.request.url.startsWith('chrome-extension')) return;
  if (event.request.url.includes('firestore.googleapis.com') || event.request.url.includes('googleapis.com')) return;
  event.respondWith(fetch(event.request).then(response => {
    if (response.ok && new URL(event.request.url).origin === self.location.origin) {
      caches.open(CACHE).then(cache => cache.put(event.request, response.clone()));
    }
    return response;
  }).catch(() => caches.match(event.request).then(hit => hit || caches.match('./index.html'))));
});
