(() => {
    "use strict";

    const STORAGE_KEY = "online-executor.source";
    const STDIN_KEY = "online-executor.stdin";
    const STATUS_TEXT = {
        SUCCESS: "运行成功",
        COMPILE_ERROR: "编译错误",
        RUNTIME_ERROR: "运行错误",
        TIMEOUT: "超时",
        OUTPUT_LIMIT_EXCEEDED: "输出超限",
        FAILED: "请求失败",
    };
    const MODE_TEXT = {
        IN_PROCESS: "同进程模式（不隔离）",
        PROCESS: "子进程模式",
        DOCKER: "Docker 沙箱模式",
    };

    const $ = (id) => document.getElementById(id);
    const runButton = $("run");
    const runLabel = $("run-label");
    const output = $("output");
    const statusBadge = $("status");
    const stats = $("stats");
    const stdin = $("stdin");
    const examples = window.EXAMPLES || [];

    // ---- Editor ----

    const darkScheme = window.matchMedia("(prefers-color-scheme: dark)");
    const editor = CodeMirror.fromTextArea($("source"), {
        mode: "text/x-java",
        theme: darkScheme.matches ? "material-darker" : "idea",
        lineNumbers: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: false,
        matchBrackets: true,
        autoCloseBrackets: true,
        extraKeys: {
            "Ctrl-Enter": run,
            "Cmd-Enter": run,
            Tab: (cm) => cm.somethingSelected() ? cm.indentSelection("add") : cm.replaceSelection("    "),
        },
    });
    darkScheme.addEventListener("change", (e) => editor.setOption("theme", e.matches ? "material-darker" : "idea"));

    editor.setValue(localStorage.getItem(STORAGE_KEY) || examples[0].source);
    stdin.value = localStorage.getItem(STDIN_KEY) || "";
    editor.on("change", () => {
        localStorage.setItem(STORAGE_KEY, editor.getValue());
        clearErrorMarks();
        updateFileName();
    });
    stdin.addEventListener("input", () => localStorage.setItem(STDIN_KEY, stdin.value));
    updateFileName();

    function updateFileName() {
        const match = /public\s+(?:(?:final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+(\w+)/
            .exec(editor.getValue());
        $("file-name").textContent = (match ? match[1] : "Main") + ".java";
    }

    // ---- Examples ----

    const select = $("examples");
    examples.forEach((example, index) => select.add(new Option(example.name, String(index))));
    select.addEventListener("change", () => {
        const example = examples[Number(select.value)];
        if (example) {
            editor.setValue(example.source);
            stdin.value = example.stdin || "";
            localStorage.setItem(STDIN_KEY, stdin.value);
            editor.focus();
        }
        select.value = "";
    });

    // ---- Runtime info ----

    fetch("/api/runtime")
        .then((response) => response.json())
        .then((info) => {
            const runtime = $("runtime");
            runtime.textContent = `${MODE_TEXT[info.mode] || info.mode} · Java ${info.javaVersion} · 超时 ${info.timeoutMillis / 1000}s`;
            runtime.classList.toggle("warn", info.mode === "IN_PROCESS");
        })
        .catch(() => { $("runtime").textContent = "运行信息不可用"; });

    // ---- Run ----

    let running = false;
    runButton.addEventListener("click", run);
    $("clear").addEventListener("click", () => {
        statusBadge.hidden = true;
        stats.textContent = "";
        output.replaceChildren(note("运行后，这里显示程序输出。", "placeholder"));
        clearErrorMarks();
    });
    document.addEventListener("keydown", (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
            e.preventDefault();
            run();
        }
    });

    async function run() {
        if (running) {
            return;
        }
        setRunning(true);
        clearErrorMarks();
        output.replaceChildren(note("编译并运行中…"));
        try {
            const response = await fetch("/api/executions", {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({ source: editor.getValue(), stdin: stdin.value }),
            });
            const body = await response.json().catch(() => ({}));
            if (!response.ok) {
                showFailure(body.detail || `HTTP ${response.status}`);
            } else {
                showResult(body);
            }
        } catch (error) {
            showFailure(`无法连接服务器：${error.message}`);
        } finally {
            setRunning(false);
        }
    }

    function setRunning(value) {
        running = value;
        runButton.disabled = value;
        runLabel.textContent = value ? "运行中…" : "运行";
    }

    function showResult(result) {
        setStatus(result.status);
        const parts = [];
        if (result.status !== "COMPILE_ERROR") {
            parts.push(`${result.durationMillis} ms`);
        }
        if (result.exitCode !== null && result.exitCode !== undefined) {
            parts.push(`exit ${result.exitCode}`);
        }
        stats.textContent = parts.join(" · ");

        const nodes = [];
        if (result.status === "COMPILE_ERROR") {
            nodes.push(compileErrorList(result.compileErrors));
        }
        if (result.stdout) {
            nodes.push(pre(result.stdout));
        }
        if (result.stderr) {
            nodes.push(pre(result.stderr, "stderr"));
        }
        if (result.status === "TIMEOUT") {
            nodes.push(note("程序运行超时，已被终止。"));
        }
        if (result.status === "OUTPUT_LIMIT_EXCEEDED") {
            nodes.push(note("输出超过上限，程序已被终止。"));
        }
        if (nodes.length === 0) {
            nodes.push(note("程序没有输出。"));
        }
        output.replaceChildren(...nodes);
    }

    function showFailure(message) {
        setStatus("FAILED");
        stats.textContent = "";
        output.replaceChildren(pre(message, "stderr"));
    }

    function setStatus(status) {
        statusBadge.hidden = false;
        statusBadge.className = `status ${status}`;
        statusBadge.textContent = STATUS_TEXT[status] || status;
    }

    // ---- Compile errors ----

    let markedLines = [];

    function compileErrorList(errors) {
        const list = document.createElement("ul");
        list.className = "compile-errors";
        for (const error of errors) {
            const item = document.createElement("li");
            if (error.line > 0) {
                const position = document.createElement("span");
                position.className = "position";
                position.textContent = `第 ${error.line} 行，第 ${error.column} 列：`;
                item.append(position);
                const line = editor.addLineClass(error.line - 1, "background", "error-line");
                markedLines.push(line);
                item.addEventListener("click", () => {
                    editor.setCursor({ line: error.line - 1, ch: Math.max(error.column - 1, 0) });
                    editor.focus();
                });
            }
            item.append(error.message);
            list.append(item);
        }
        return list;
    }

    function clearErrorMarks() {
        markedLines.forEach((line) => editor.removeLineClass(line, "background", "error-line"));
        markedLines = [];
    }

    // ---- DOM helpers ----

    function pre(text, className) {
        const element = document.createElement("pre");
        if (className) {
            element.className = className;
        }
        element.textContent = text;
        return element;
    }

    function note(text, className = "note") {
        const element = document.createElement("p");
        element.className = className;
        element.textContent = text;
        return element;
    }
})();
