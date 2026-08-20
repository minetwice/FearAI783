package com.agent.app.github

object WorkflowTemplates {

    fun getTemplate(language: String, filename: String): String {
        return when (language.lowercase()) {
            "python" -> """
name: Build and Run
on:
  workflow_dispatch:
jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - run: pip install -r requirements.txt 2>/dev/null || true
      - run: python $filename > output.txt 2>&1 || true
      - uses: actions/upload-artifact@v4
        with:
          name: result
          path: output.txt
""".trimIndent()

            "javascript", "js" -> """
name: Build and Run
on:
  workflow_dispatch:
jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm install 2>/dev/null || true
      - run: node $filename > output.txt 2>&1 || true
      - uses: actions/upload-artifact@v4
        with:
          name: result
          path: output.txt
""".trimIndent()

            "c" -> """
name: Build and Run
on:
  workflow_dispatch:
jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: gcc $filename -o program -lm 2>output.txt || echo "Compile error" >> output.txt
      - run: ./program >> output.txt 2>&1 || true
      - uses: actions/upload-artifact@v4
        with:
          name: result
          path: output.txt
""".trimIndent()

            "cpp", "c++" -> """
name: Build and Run
on:
  workflow_dispatch:
jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: g++ $filename -o program -lm 2>output.txt || echo "Compile error" >> output.txt
      - run: ./program >> output.txt 2>&1 || true
      - uses: actions/upload-artifact@v4
        with:
          name: result
          path: output.txt
""".trimIndent()

            "java" -> """
name: Build and Run
on:
  workflow_dispatch:
jobs:
  run:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: javac $filename 2>output.txt || echo "Compile error" >> output.txt
      - run: java Main >> output.txt 2>&1 || true
      - uses: actions/upload-artifact@v4
        with:
          name: result
          path: output.txt
""".trimIndent()

            else -> getTemplate("python", filename)
        }
    }
}
