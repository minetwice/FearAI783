/**
 * Automated Verification & Testing Suite for FearAI App Engine
 */

const assert = require('assert');
const SYSTEM_PROMPTS = require('../public/js/system-prompts.js');
const FearAIEngine = require('../public/js/fearai-engine.js');

// Mock localStorage for Node.js test environment
global.localStorage = {
  _store: {},
  getItem: function(key) { return this._store[key] || null; },
  setItem: function(key, val) { this._store[key] = val.toString(); },
  removeItem: function(key) { delete this._store[key]; }
};

// Mock fetch for API testing
global.fetch = async function(url, options = {}) {
  // Mock Gemini API
  if (url.includes('generativelanguage.googleapis.com')) {
    return {
      ok: true,
      json: async () => ({
        candidates: [{
          content: {
            parts: [{ text: "```java\npublic class FearAIMain extends JavaPlugin {\n    @Override\n    public void onEnable() {\n        getLogger().info(\"FearAI Minecraft Plugin Enabled!\");\n    }\n}\n```" }]
          }
        }]
      })
    };
  }

  // Mock Pollinations Zero-Key API
  if (url.includes('text.pollinations.ai')) {
    return {
      ok: true,
      text: async () => "FearAI Free Engine online! Ready for coding."
    };
  }

  // Mock DuckDuckGo Search API
  if (url.includes('duckduckgo.com')) {
    return {
      ok: true,
      json: async () => ({
        AbstractText: "Minecraft PaperMC is a high-performance Spigot fork.",
        AbstractURL: "https://papermc.io",
        Heading: "PaperMC"
      })
    };
  }

  return { ok: true, json: async () => ({}), text: async () => "" };
};

async function runTests() {
  console.log("🚀 Starting FearAI Engine Verification Tests...\n");

  // Test 1: Verify System Prompts
  console.log("Test 1: System Prompts Check...");
  assert.ok(SYSTEM_PROMPTS.minecraft.includes("Minecraft Developer"), "Minecraft prompt missing persona title");
  assert.ok(SYSTEM_PROMPTS.coding.includes("Master Software Engineer"), "Coding prompt missing persona title");
  assert.ok(SYSTEM_PROMPTS.reasoning.includes("Deep Reasoning"), "Reasoning prompt missing persona title");
  console.log("✅ Test 1 Passed: System prompts structured correctly.\n");

  // Test 2: Engine Initialization & Provider Config
  console.log("Test 2: FearAI Engine Configuration...");
  const engine = new FearAIEngine();
  engine.setGeminiKey("AIzaSyTESTKEY123456789");
  assert.strictEqual(engine.geminiKey, "AIzaSyTESTKEY123456789");
  assert.strictEqual(localStorage.getItem('fearai_gemini_key'), "AIzaSyTESTKEY123456789");
  console.log("✅ Test 2 Passed: Gemini API Key set and persisted successfully.\n");

  // Test 3: Gemini API Integration Test
  console.log("Test 3: Gemini API Minecraft Plugin Code Generation Test...");
  engine.setActiveProvider('gemini');
  const response = await engine.generateResponse("Create a Minecraft paper plugin", [], { persona: 'minecraft' });
  assert.ok(response.includes("public class FearAIMain extends JavaPlugin"), "Gemini Minecraft code response invalid");
  console.log("✅ Test 3 Passed: Gemini API code generation verified.\n");

  // Test 4: Web Search Research Integration
  console.log("Test 4: Web Research Module Test...");
  const searchResults = await engine.performWebSearch("PaperMC");
  assert.ok(searchResults.length > 0, "Web search results empty");
  assert.strictEqual(searchResults[0].title, "PaperMC");
  console.log("✅ Test 4 Passed: Web search integration verified.\n");

  // Test 5: Fallback Zero-Key Engine Test
  console.log("Test 5: Zero-Key Free Fallback Engine Test...");
  engine.setActiveProvider('pollinations');
  const fallbackResponse = await engine.generateResponse("Hello FearAI", [], { persona: 'friendly' });
  assert.ok(fallbackResponse.includes("FearAI Free Engine online"), "Zero key fallback failed");
  console.log("✅ Test 5 Passed: Zero-key engine fallback verified.\n");

  console.log("🎉 ALL FEARAI TESTS PASSED SUCCESSFULLY!");
}

runTests().catch(err => {
  console.error("❌ Test Suite Error:", err);
  process.exit(1);
});
