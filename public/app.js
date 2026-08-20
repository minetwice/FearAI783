/**
 * FearAI Mobile Application Script
 */

document.addEventListener('DOMContentLoaded', () => {
  const engine = new FearAIEngine();

  // DOM Elements
  const chatContainer = document.getElementById('chatContainer');
  const userInput = document.getElementById('userInput');
  const sendBtn = document.getElementById('sendBtn');
  const micBtn = document.getElementById('micBtn');
  const clearChatBtn = document.getElementById('clearChatBtn');
  const voiceToggleBtn = document.getElementById('voiceToggleBtn');
  const searchToggle = document.getElementById('searchToggle');
  const activeProviderLabel = document.getElementById('activeProviderLabel');

  // Settings Modal Elements
  const settingsModal = document.getElementById('settingsModal');
  const openSettingsBtn = document.getElementById('openSettingsBtn');
  const closeSettingsBtn = document.getElementById('closeSettingsBtn');
  const saveSettingsBtn = document.getElementById('saveSettingsBtn');
  const providerSelect = document.getElementById('providerSelect');
  const geminiKeyInput = document.getElementById('geminiKeyInput');
  const groqKeyInput = document.getElementById('groqKeyInput');

  // App State
  let currentPersona = 'minecraft';
  let conversationHistory = [];
  let isVoiceEnabled = false;

  // Initialize UI Values
  geminiKeyInput.value = engine.geminiKey;
  groqKeyInput.value = engine.groqKey;
  providerSelect.value = engine.activeProvider;
  updateProviderLabel();

  // Register PWA Service Worker if supported
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(err => console.log('SW register error:', err));
  }

  // Persona Chip Switching
  const personaChips = document.querySelectorAll('.persona-chip');
  personaChips.forEach(chip => {
    chip.addEventListener('click', () => {
      personaChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentPersona = chip.dataset.persona;
    });
  });

  // Settings Modal Controls
  openSettingsBtn.addEventListener('click', () => settingsModal.classList.add('open'));
  closeSettingsBtn.addEventListener('click', () => settingsModal.classList.remove('open'));

  saveSettingsBtn.addEventListener('click', () => {
    engine.setGeminiKey(geminiKeyInput.value);
    engine.setGroqKey(groqKeyInput.value);
    engine.setActiveProvider(providerSelect.value);
    updateProviderLabel();
    settingsModal.classList.remove('open');
    appendMessage('ai', '⚙️ Settings updated successfully! FearAI is ready.');
  });

  function updateProviderLabel() {
    let text = 'Auto (Free)';
    if (engine.activeProvider === 'gemini') text = 'Google Gemini';
    else if (engine.activeProvider === 'groq') text = 'Groq Engine';
    else if (engine.activeProvider === 'openrouter') text = 'OpenRouter';
    activeProviderLabel.textContent = `Provider: ${text}`;
  }

  // Clear Chat
  clearChatBtn.addEventListener('click', () => {
    conversationHistory = [];
    chatContainer.innerHTML = `
      <div class="message-wrapper ai">
        <div class="message-bubble">
          🧹 Chat cleared. What shall we build or solve next?
        </div>
      </div>`;
  });

  // Voice Toggle
  voiceToggleBtn.addEventListener('click', () => {
    isVoiceEnabled = !isVoiceEnabled;
    voiceToggleBtn.style.color = isVoiceEnabled ? '#06b6d4' : '#f3f4f6';
    if (isVoiceEnabled) {
      speakText("Voice response activated.");
    }
  });

  // Voice Input (Speech Recognition)
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.lang = 'en-US';

    micBtn.addEventListener('click', () => {
      micBtn.style.color = '#ef4444';
      recognition.start();
    });

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      userInput.value = transcript;
      micBtn.style.color = '#f3f4f6';
      handleSendMessage();
    };

    recognition.onerror = () => {
      micBtn.style.color = '#f3f4f6';
    };

    recognition.onend = () => {
      micBtn.style.color = '#f3f4f6';
    };
  } else {
    micBtn.addEventListener('click', () => {
      alert("Speech Recognition is not supported on this device/browser.");
    });
  }

  // Send Message Logic
  sendBtn.addEventListener('click', handleSendMessage);
  userInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handleSendMessage();
  });

  async function handleSendMessage() {
    const message = userInput.value.trim();
    if (!message) return;

    userInput.value = '';
    appendMessage('user', message);
    conversationHistory.push({ role: 'user', content: message });

    // Show Loading Bubble
    const loadingId = appendLoadingBubble();

    try {
      const enableSearch = searchToggle.checked;
      const response = await engine.generateResponse(message, conversationHistory, {
        persona: currentPersona,
        enableSearch
      });

      removeLoadingBubble(loadingId);
      appendMessage('ai', response);
      conversationHistory.push({ role: 'assistant', content: response });

      if (isVoiceEnabled) {
        speakText(stripCodeBlocks(response));
      }
    } catch (err) {
      removeLoadingBubble(loadingId);
      appendMessage('ai', `⚠️ **Error**: ${err.message || 'Failed to generate response. Please check your settings or network.'}`);
    }
  }

  // UI Message Append with Code Block Handler
  function appendMessage(sender, content) {
    const wrapper = document.createElement('div');
    wrapper.className = `message-wrapper ${sender}`;

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';

    bubble.innerHTML = formatMarkdownAndCode(content);

    wrapper.appendChild(bubble);
    chatContainer.appendChild(wrapper);
    chatContainer.scrollTop = chatContainer.scrollHeight;
  }

  function appendLoadingBubble() {
    const id = 'loading-' + Date.now();
    const wrapper = document.createElement('div');
    wrapper.className = 'message-wrapper ai';
    wrapper.id = id;

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.innerHTML = `<i class="fas fa-spinner fa-spin"></i> FearAI is thinking & writing code...`;

    wrapper.appendChild(bubble);
    chatContainer.appendChild(wrapper);
    chatContainer.scrollTop = chatContainer.scrollHeight;
    return id;
  }

  function removeLoadingBubble(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
  }

  // Simple Markdown & Code Formatting with Copy/Download Buttons
  function formatMarkdownAndCode(text) {
    let formatted = text;

    // Code Blocks parsing: ```lang ... ```
    const codeBlockRegex = /```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g;
    formatted = formatted.replace(codeBlockRegex, (match, lang, code) => {
      const language = lang || 'code';
      const codeId = 'code-' + Math.random().toString(36).substr(2, 9);
      const escapedCode = escapeHtml(code.trim());

      return `
        <pre class="code-block">
          <div class="code-header">
            <span><i class="fas fa-file-code"></i> ${language.toUpperCase()}</span>
            <div class="code-header-btns">
              <button class="code-action-btn" onclick="copyCode('${codeId}')"><i class="fas fa-copy"></i> Copy</button>
              <button class="code-action-btn" onclick="downloadCode('${codeId}', '${language}')"><i class="fas fa-download"></i> Save File</button>
            </div>
          </div>
          <code id="${codeId}">${escapedCode}</code>
        </pre>
      `;
    });

    // Formatting bold, linebreaks, links
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
  }

  function escapeHtml(str) {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function stripCodeBlocks(str) {
    return str.replace(/```[\s\S]*?```/g, 'Code block generated.').replace(/\*/g, '');
  }

  function speakText(text) {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text.substring(0, 300));
      utterance.rate = 1.0;
      window.speechSynthesis.speak(utterance);
    }
  }

  // Global Helpers for Copy & Download
  window.copyCode = function(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return;
    const text = el.innerText;
    navigator.clipboard.writeText(text).then(() => {
      alert("Code copied to clipboard!");
    }).catch(() => {
      alert("Failed to copy code.");
    });
  };

  window.downloadCode = function(elementId, lang) {
    const el = document.getElementById(elementId);
    if (!el) return;
    const text = el.innerText;

    let ext = 'txt';
    if (lang.includes('java')) ext = 'java';
    else if (lang.includes('py')) ext = 'py';
    else if (lang.includes('json')) ext = 'json';
    else if (lang.includes('js')) ext = 'js';
    else if (lang.includes('html')) ext = 'html';
    else if (lang.includes('css')) ext = 'css';

    const blob = new Blob([text], { type: 'text/plain' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `FearAI_Code_${Date.now()}.${ext}`;
    a.click();
  };
});
