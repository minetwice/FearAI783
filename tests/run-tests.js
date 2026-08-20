/**
 * Automated Verification & Testing Suite for FearAI App Engine
 */

const assert = require('assert');
const SYSTEM_PROMPTS = require('../public/js/system-prompts.js');
const FearAIEngine = require('../public/js/fearai-engine.js');

// Mock fetch for API testing
global.fetch = async function(url, options = {}) {
  // Mock Qwen Open-Source Engine Endpoint
  if (url.includes('text.pollinations.ai')) {
    return {
      ok: true,
      text: async () => "```java\npublic class FearAIMain extends JavaPlugin {\n    @Override\n    public void onEnable() {\n        getLogger().info(\"FearAI Qwen Coder Minecraft Plugin Enabled!\");\n    }\n}\n```"
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
  console.log("🚀 Starting FearAI Qwen Engine Verification Tests...\n");

  // Test 1: Verify System Prompts
  console.log("Test 1: System Prompts Check...");
  assert.ok(SYSTEM_PROMPTS.minecraft.includes("Minecraft Developer"), "Minecraft prompt missing persona title");
  assert.ok(SYSTEM_PROMPTS.coding.includes("Master Software Engineer"), "Coding prompt missing persona title");
  assert.ok(SYSTEM_PROMPTS.reasoning.includes("Deep Reasoning"), "Reasoning prompt missing persona title");
  console.log("✅ Test 1 Passed: System prompts structured correctly.\n");

  // Test 2: Qwen Engine Code Generation Test
  console.log("Test 2: Qwen Open-Source Engine Generation Test...");
  const engine = new FearAIEngine();
  const response = await engine.generateResponse("Create a Minecraft paper plugin", [], { persona: 'minecraft' });
  assert.ok(response.includes("public class FearAIMain extends JavaPlugin"), "Qwen Coder response invalid");
  console.log("✅ Test 2 Passed: Qwen Coder code generation verified.\n");

  // Test 3: Web Search Research Integration
  console.log("Test 3: Web Research Module Test...");
  const searchResults = await engine.performWebSearch("PaperMC");
  assert.ok(searchResults.length > 0, "Web search results empty");
  assert.strictEqual(searchResults[0].title, "PaperMC");
  console.log("✅ Test 3 Passed: Web search integration verified.\n");

  console.log("🎉 ALL FEARAI TESTS PASSED SUCCESSFULLY!");
}

runTests().catch(err => {
  console.error("❌ Test Suite Error:", err);
  process.exit(1);
});
