/**
 * FearAI Engine - Multi-Provider AI Service & Web Search Engine
 */

class FearAIEngine {
  constructor() {
    this.geminiKey = localStorage.getItem('fearai_gemini_key') || '';
    this.groqKey = localStorage.getItem('fearai_groq_key') || '';
    this.openrouterKey = localStorage.getItem('fearai_openrouter_key') || '';
    this.activeProvider = localStorage.getItem('fearai_active_provider') || 'auto'; // 'gemini', 'groq', 'openrouter', 'pollinations'
  }

  setGeminiKey(key) {
    this.geminiKey = key.trim();
    localStorage.setItem('fearai_gemini_key', this.geminiKey);
  }

  setGroqKey(key) {
    this.groqKey = key.trim();
    localStorage.setItem('fearai_groq_key', this.groqKey);
  }

  setOpenRouterKey(key) {
    this.openrouterKey = key.trim();
    localStorage.setItem('fearai_openrouter_key', this.openrouterKey);
  }

  setActiveProvider(provider) {
    this.activeProvider = provider;
    localStorage.setItem('fearai_active_provider', provider);
  }

  getSystemPrompt(persona = 'coding') {
    if (typeof SYSTEM_PROMPTS !== 'undefined' && SYSTEM_PROMPTS[persona]) {
      return SYSTEM_PROMPTS[persona];
    }
    return "You are FearAI, an advanced mobile AI assistant specialized in complex coding, Minecraft modding, reasoning, and research.";
  }

  /**
   * Main function to generate completion from FearAI
   */
  async generateResponse(userPrompt, conversationHistory = [], options = {}) {
    const persona = options.persona || 'coding';
    const enableSearch = options.enableSearch || false;
    const systemPrompt = this.getSystemPrompt(persona);

    let searchContext = "";
    if (enableSearch) {
      try {
        const searchResults = await this.performWebSearch(userPrompt);
        if (searchResults && searchResults.length > 0) {
          searchContext = "\n\n[LATEST WEB RESEARCH RESULTS]:\n" +
            searchResults.map((r, i) => `${i+1}. ${r.title}\n${r.snippet}\nSource: ${r.link}`).join('\n\n');
        }
      } catch (err) {
        console.warn("Web search failed, proceeding without search results:", err);
      }
    }

    const fullPrompt = searchContext ? `${userPrompt}\n${searchContext}` : userPrompt;

    // Determine provider execution order
    let providerToUse = this.activeProvider;
    if (providerToUse === 'auto') {
      if (this.geminiKey) providerToUse = 'gemini';
      else if (this.groqKey) providerToUse = 'groq';
      else if (this.openrouterKey) providerToUse = 'openrouter';
      else providerToUse = 'pollinations';
    }

    // Try primary provider, fallback gracefully if it fails
    try {
      if (providerToUse === 'gemini' && this.geminiKey) {
        return await this.callGemini(fullPrompt, conversationHistory, systemPrompt);
      } else if (providerToUse === 'groq' && this.groqKey) {
        return await this.callGroq(fullPrompt, conversationHistory, systemPrompt);
      } else if (providerToUse === 'openrouter' && this.openrouterKey) {
        return await this.callOpenRouter(fullPrompt, conversationHistory, systemPrompt);
      } else {
        return await this.callPollinationsFree(fullPrompt, conversationHistory, systemPrompt);
      }
    } catch (primaryErr) {
      console.warn(`Primary provider (${providerToUse}) error, attempting free fallback:`, primaryErr);
      // Fallback to Pollinations zero-key provider
      try {
        return await this.callPollinationsFree(fullPrompt, conversationHistory, systemPrompt);
      } catch (fallbackErr) {
        throw new Error(`Primary Error: ${primaryErr.message || primaryErr}. (Fallback note: ${fallbackErr.message || fallbackErr})`);
      }
    }
  }

  /**
   * Google Gemini API Call with Auto-Fallback Models (gemini-2.0-flash, gemini-1.5-flash-latest, gemini-1.5-flash)
   */
  async callGemini(userPrompt, history, systemPrompt) {
    const modelsToTry = ['gemini-2.0-flash', 'gemini-1.5-flash-latest', 'gemini-1.5-flash', 'gemini-1.5-pro'];
    let lastError = null;

    const contents = [];
    contents.push({
      role: 'user',
      parts: [{ text: `System Instruction: ${systemPrompt}` }]
    });
    contents.push({
      role: 'model',
      parts: [{ text: "Understood. I am FearAI, ready to assist." }]
    });

    for (const msg of history) {
      contents.push({
        role: msg.role === 'user' ? 'user' : 'model',
        parts: [{ text: msg.content }]
      });
    }

    contents.push({
      role: 'user',
      parts: [{ text: userPrompt }]
    });

    for (const model of modelsToTry) {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${this.geminiKey}`;
        const response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ contents })
        });

        if (response.ok) {
          const data = await response.json();
          const replyText = data.candidates?.[0]?.content?.parts?.[0]?.text;
          if (replyText) return replyText;
        } else {
          const errJson = await response.json().catch(() => ({}));
          lastError = new Error(errJson.error?.message || `Gemini ${model} HTTP ${response.status}`);
        }
      } catch (err) {
        lastError = err;
      }
    }

    throw lastError || new Error("Failed to connect to Gemini API with provided key.");
  }

  /**
   * Groq API Call (Free Llama 3.3 70B & DeepSeek R1)
   */
  async callGroq(userPrompt, history, systemPrompt) {
    const url = 'https://api.groq.com/openai/v1/chat/completions';

    const messages = [{ role: 'system', content: systemPrompt }];
    for (const msg of history) {
      messages.push({ role: msg.role, content: msg.content });
    }
    messages.push({ role: 'user', content: userPrompt });

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.groqKey}`
      },
      body: JSON.stringify({
        model: 'llama-3.3-70b-versatile',
        messages,
        temperature: 0.7,
        max_tokens: 4096
      })
    });

    if (!response.ok) {
      const errJson = await response.json().catch(() => ({}));
      throw new Error(errJson.error?.message || `Groq API HTTP Error ${response.status}`);
    }

    const data = await response.json();
    return data.choices?.[0]?.message?.content || "No response from Groq.";
  }

  /**
   * OpenRouter API Call (Free Models)
   */
  async callOpenRouter(userPrompt, history, systemPrompt) {
    const url = 'https://openrouter.ai/api/v1/chat/completions';

    const messages = [{ role: 'system', content: systemPrompt }];
    for (const msg of history) {
      messages.push({ role: msg.role, content: msg.content });
    }
    messages.push({ role: 'user', content: userPrompt });

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.openrouterKey}`,
        'HTTP-Referer': 'https://fearai.app',
        'X-Title': 'FearAI Mobile'
      },
      body: JSON.stringify({
        model: 'qwen/qwen-2.5-coder-32b:free',
        messages
      })
    });

    if (!response.ok) {
      const errJson = await response.json().catch(() => ({}));
      throw new Error(errJson.error?.message || `OpenRouter HTTP Error ${response.status}`);
    }

    const data = await response.json();
    return data.choices?.[0]?.message?.content || "No response from OpenRouter.";
  }

  /**
   * Pollinations & Free Engine Zero-Key Provider
   */
  async callPollinationsFree(userPrompt, history, systemPrompt) {
    // Try Pollinations JSON API first
    try {
      const url = 'https://text.pollinations.ai/';
      const messages = [{ role: 'system', content: systemPrompt }];
      for (const msg of history) {
        messages.push({ role: msg.role, content: msg.content });
      }
      messages.push({ role: 'user', content: userPrompt });

      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages,
          model: 'openai'
        })
      });

      if (response.ok) {
        const text = await response.text();
        if (text && text.trim()) return text;
      }
    } catch (e) {
      console.warn("Pollinations POST endpoint failed, trying GET fallback:", e);
    }

    // Secondary GET fallback
    const encodedPrompt = encodeURIComponent(`${systemPrompt}\n\nUser Question: ${userPrompt}`);
    const getUrl = `https://text.pollinations.ai/${encodedPrompt}?model=openai`;
    const responseGet = await fetch(getUrl);
    if (!responseGet.ok) {
      throw new Error(`Free Engine HTTP Error ${responseGet.status}`);
    }
    const textGet = await responseGet.text();
    return textGet || "No response received from Free Engine.";
  }

  /**
   * Web Search Engine using DuckDuckGo Free Endpoint
   */
  async performWebSearch(query) {
    try {
      const encodedQuery = encodeURIComponent(query);
      const url = `https://api.duckduckgo.com/?q=${encodedQuery}&format=json&no_redirect=1&no_html=1`;
      const response = await fetch(url);
      if (!response.ok) return [];

      const data = await response.json();
      const results = [];

      if (data.AbstractText) {
        results.push({
          title: data.Heading || query,
          snippet: data.AbstractText,
          link: data.AbstractURL || 'DuckDuckGo Search'
        });
      }

      if (data.RelatedTopics && Array.isArray(data.RelatedTopics)) {
        for (const topic of data.RelatedTopics.slice(0, 3)) {
          if (topic.Text && topic.FirstURL) {
            results.push({
              title: topic.Text.split(' - ')[0] || 'Search Result',
              snippet: topic.Text,
              link: topic.FirstURL
            });
          }
        }
      }

      return results;
    } catch (e) {
      console.warn("Search fetch error:", e);
      return [];
    }
  }
}

if (typeof module !== 'undefined') {
  module.exports = FearAIEngine;
}
