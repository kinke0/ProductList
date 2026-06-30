#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { readFile } from "fs/promises";
import { resolve } from "path";

const ARK_API_KEY = process.env.ARK_API_KEY;
const ARK_BASE_URL = process.env.ARK_BASE_URL || "https://ark.cn-beijing.volces.com/api/v3";
const DEFAULT_MODEL = process.env.ARK_VISION_MODEL || "doubao-vision-pro-32k";

async function arkRequest(endpoint, body) {
  if (!ARK_API_KEY) throw new Error("ARK_API_KEY environment variable is required");

  const res = await fetch(`${ARK_BASE_URL}${endpoint}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${ARK_API_KEY}` },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(`Ark API error (${res.status}): ${err}`);
  }
  return res.json();
}

async function analyzeImage({ imagePath, prompt, model, max_tokens, temperature }) {
  const absPath = resolve(imagePath);
  const buffer = await readFile(absPath);
  const base64 = buffer.toString("base64");
  const ext = absPath.split(".").pop().toLowerCase();
  const mimeMap = { png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp", bmp: "image/bmp" };
  const mime = mimeMap[ext] || "image/png";

  const response = await arkRequest("/chat/completions", {
    model: model || DEFAULT_MODEL,
    messages: [
      {
        role: "user",
        content: [
          { type: "text", text: prompt || "请详细描述这张图片的内容和布局" },
          { type: "image_url", image_url: { url: `data:${mime};base64,${base64}` } },
        ],
      },
    ],
    max_tokens: max_tokens || 4096,
    temperature: temperature || 0.7,
  });

  return {
    text: response.choices?.[0]?.message?.content || "",
    model: response.model,
    usage: response.usage,
  };
}

async function visionChat({ messages, model, max_tokens, temperature }) {
  for (const msg of messages) {
    if (msg.image_path) {
      const absPath = resolve(msg.image_path);
      const buffer = await readFile(absPath);
      const base64 = buffer.toString("base64");
      const ext = absPath.split(".").pop().toLowerCase();
      const mimeMap = { png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp" };
      msg.content = [
        { type: "text", text: msg.content || "" },
        { type: "image_url", image_url: { url: `data:${mimeMap[ext] || "image/png"};base64,${base64}` } },
      ];
      delete msg.image_path;
    }
  }

  const response = await arkRequest("/chat/completions", {
    model: model || DEFAULT_MODEL,
    messages,
    max_tokens: max_tokens || 4096,
    temperature: temperature || 0.7,
  });

  return {
    response: response.choices?.[0]?.message?.content || "",
    model: response.model,
    usage: response.usage,
  };
}

const server = new Server(
  { name: "doubao-vision-mcp", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "doubao_vision_analyze",
      description: "使用豆包视觉模型分析图片。传入图片文件路径和提示词，返回图片分析结果。适合分析UI截图、文档图片等。",
      inputSchema: {
        type: "object",
        properties: {
          imagePath: { type: "string", description: "图片文件的绝对路径或相对路径" },
          prompt: { type: "string", description: "分析提示词，描述你希望从图片中获取什么信息" },
          model: { type: "string", description: "模型ID，默认 doubao-vision-pro-32k" },
          max_tokens: { type: "number", description: "最大生成token数，默认4096" },
          temperature: { type: "number", description: "采样温度(0-1)，默认0.7" },
        },
        required: ["imagePath"],
      },
    },
    {
      name: "doubao_vision_chat",
      description: "使用豆包视觉模型进行多轮对话，支持在对话中发送图片。适合需要结合上下文的图片分析任务。",
      inputSchema: {
        type: "object",
        properties: {
          messages: {
            type: "array",
            description: "对话消息数组，每条消息包含 role(user/assistant/system)、content，可选 image_path(用户消息中带图片路径)",
            items: {
              type: "object",
              properties: {
                role: { type: "string", enum: ["system", "user", "assistant"] },
                content: { type: "string" },
                image_path: { type: "string", description: "可选，用户消息中附带图片路径" },
              },
              required: ["role", "content"],
            },
          },
          model: { type: "string", description: "模型ID" },
          max_tokens: { type: "number", description: "最大生成token数" },
          temperature: { type: "number", description: "采样温度(0-1)" },
        },
        required: ["messages"],
      },
    },
  ],
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  try {
    if (name === "doubao_vision_analyze") {
      const result = await analyzeImage(args);
      return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
    }
    if (name === "doubao_vision_chat") {
      const result = await visionChat(args);
      return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
    }
    throw new Error(`Unknown tool: ${name}`);
  } catch (error) {
    return { content: [{ type: "text", text: `Error: ${error.message}` }], isError: true };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Doubao Vision MCP Server running on stdio");
}

main().catch(console.error);
