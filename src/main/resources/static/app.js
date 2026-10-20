const form = document.querySelector('#form');
const text = document.querySelector('#text');
const submit = document.querySelector('#submit');
const result = document.querySelector('#result');
const statusLine = document.querySelector('#status');
const badge = document.querySelector('#badge');
const copy = document.querySelector('#copy');
const taskId = document.querySelector('#task-id');
const labels = {NEW: 'В очереди', PROCESSING: 'Проверяем', DONE: 'Готово', FAILED: 'Ошибка'};

text.addEventListener('input', updateCount);
function updateCount() {
  document.querySelector('#count').textContent = `${text.value.length.toLocaleString('ru-RU')} / 100 000`;
}
document.querySelector('#example').addEventListener('click', () => {
  text.value = document.querySelector('#language').value === 'RU'
    ? 'Превет! Это небальшой текст для проверки.'
    : 'Helo! This is a smple text.';
  updateCount();
  text.focus();
});
copy.addEventListener('click', async () => {
  try {
    await navigator.clipboard.writeText(result.textContent);
    statusLine.textContent = 'Текст скопирован.';
  } catch {
    statusLine.textContent = 'Не удалось скопировать. Выдели текст и скопируй вручную.';
  }
});

async function request(url, options) {
  const response = await fetch(url, {...options, signal: AbortSignal.timeout(15000)});
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.errorMessage || `Ошибка запроса (${response.status})`);
  return data;
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  submit.disabled = true;
  copy.disabled = true;
  result.textContent = 'Отправляем текст на проверку…';
  result.classList.add('placeholder');
  statusLine.classList.remove('error');
  taskId.textContent = '';
  badge.textContent = 'Отправляем';
  try {
    const created = await request('/api/tasks', {
      method: 'POST', headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({text: text.value, language: document.querySelector('#language').value})
    });
    taskId.textContent = `Задача ${created.id.slice(0, 8)}`;
    taskId.title = created.id;
    const deadline = Date.now() + 10 * 60 * 1000;
    while (Date.now() < deadline) {
      const task = await request(`/api/tasks/${created.id}`);
      badge.textContent = labels[task.status] || task.status;
      if (task.status === 'FAILED') throw new Error(task.errorMessage);
      if (task.status === 'DONE') {
        result.textContent = task.correctedText;
        result.classList.remove('placeholder');
        copy.disabled = false;
        statusLine.textContent = 'Проверка завершена. Результат можно скопировать.';
        return;
      }
      statusLine.textContent = task.status === 'NEW' ? 'Текст в очереди на проверку…' : 'Проверяем орфографию…';
      await new Promise(resolve => setTimeout(resolve, 1500));
    }
    throw new Error(`Ожидание затянулось. Задача сохранена: ${created.id}`);
  } catch (error) {
    badge.textContent = 'Не завершено';
    result.textContent = 'Не удалось получить результат.';
    statusLine.textContent = error.name === 'TimeoutError' ? 'Сервер не ответил вовремя.' : error.message;
    statusLine.classList.add('error');
  } finally {
    submit.disabled = false;
  }
});
