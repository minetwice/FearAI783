/**
 * FearAI Engine - Injected with Qwen Open-Source Model (Zero API Key required)
 */

class FearAIEngine {
  constructor() {
    this.modelName = 'Qwen 2.5 Coder (Open-Source)';
  }

  getSystemPrompt(persona = 'coding') {
    if (typeof SYSTEM_PROMPTS !== 'undefined' && SYSTEM_PROMPTS[persona]) {
      return SYSTEM_PROMPTS[persona];
    }
    return "You are FearAI, an advanced mobile AI assistant powered by the Qwen Open-Source model specialized in complex coding, Minecraft modding, reasoning, and research.";
  }

  /**
   * Main function to generate completion using Qwen Open-Source Model
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

    return await this.callQwenEngine(fullPrompt, conversationHistory, systemPrompt);
  }

  /**
   * Directly Call Injected Qwen Open-Source Engine
   */
  async callQwenEngine(userPrompt, history, systemPrompt) {
    const url = 'https://text.pollinations.ai/';

    const messages = [{ role: 'system', content: systemPrompt }];

    // Add past conversation history (exclude current prompt if already at end)
    const pastHistory = history.length > 0 && history[history.length - 1].content === userPrompt
      ? history.slice(0, history.length - 1)
      : history;

    for (const msg of pastHistory) {
      messages.push({ role: msg.role === 'user' ? 'user' : 'assistant', content: msg.content });
    }

    // Append current user prompt
    messages.push({ role: 'user', content: userPrompt });

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messages,
          model: 'qwen-coder'
        })
      });

      if (response.ok) {
        const text = await response.text();
        if (text && text.trim()) return text;
      }
    } catch (e) {
      console.warn("Qwen POST request failed, attempting GET request fallback:", e);
    }

    // Secondary GET fallback for Qwen
    const formattedPrompt = `${systemPrompt}\n\nUser Question: ${userPrompt}`;
    const encodedPrompt = encodeURIComponent(formattedPrompt);
    const getUrl = `https://text.pollinations.ai/${encodedPrompt}?model=qwen-coder`;

    const responseGet = await fetch(getUrl);
    if (!responseGet.ok) {
      throw new Error(`Qwen Engine Error HTTP ${responseGet.status}`);
    }

    const textGet = await responseGet.text();
    return textGet || "No response received from Qwen Engine.";
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
