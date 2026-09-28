# Changelog — TJSC AI Platform

Histórico de versões da Plataforma de IA — Diretoria de Tecnologia da Informação / TJSC.


## 1.0.0-beta.0 (2026-09-28)


### Novas Funcionalidades

* add .env.example with placeholder credentials ([20a8680](https://github.com/brenothales/tjsc-ai/commit/20a8680edfd906c7034e1d89e9eb29152d8d9d25))
* add Docker Compose with environment variable injection ([e248054](https://github.com/brenothales/tjsc-ai/commit/e2480540900534ae417bbb76ce0c5eb7134b7d44))
* **frontend:** add Angular 21 project with Tailwind CSS and pnpm ([78e3dd5](https://github.com/brenothales/tjsc-ai/commit/78e3dd5cfc5263bf0dfa751ad51ecba4cf53f436))
* **frontend:** add app shell, shared components, pipes, directives and i18n ([01beb47](https://github.com/brenothales/tjsc-ai/commit/01beb4750d84c334b9c53c2c47b6e75304113f55))
* **frontend:** add chat page, message list, message item, input and sidebar components ([3adc66a](https://github.com/brenothales/tjsc-ai/commit/3adc66a08602285ed45bc6ac599cd4f425a69d96))
* **frontend:** add chat services, state store and interfaces ([250915f](https://github.com/brenothales/tjsc-ai/commit/250915fd5222fcaf2c6cc51c1af684926f902067))
* **frontend:** add processo panel, minuta modal and settings modal components ([a6921d1](https://github.com/brenothales/tjsc-ai/commit/a6921d1a1137a22799eface58d8476be71ba53b2))
* **process-agent:** add 5-layer guardrail chain for input, tool and output protection ([7447692](https://github.com/brenothales/tjsc-ai/commit/744769239a05cc2e1771b7f73581a001031423ba))
* **process-agent:** add chat API with SSE streaming and conversation management ([5405bf0](https://github.com/brenothales/tjsc-ai/commit/5405bf0cd3c48eb2d36a946af4b892c68342c865))
* **process-agent:** add minuta loop with multi-version sentence draft generation ([6ebf4ba](https://github.com/brenothales/tjsc-ai/commit/6ebf4ba6636a783c2112c5e4385ca67661c1b5db))
* **process-agent:** add Spring AI agent with MongoDB memory and Redis cache ([89d4fb0](https://github.com/brenothales/tjsc-ai/commit/89d4fb08a2b8efd0aef60c43a331960b448b8d16))
* **process-data-service:** add REST controllers, OpenAPI config and exception handling ([a167cef](https://github.com/brenothales/tjsc-ai/commit/a167cef358325a2f10a83eac00b16a0c342ac8e0))
* **process-data-service:** add virtual threads support via Spring property ([369f00c](https://github.com/brenothales/tjsc-ai/commit/369f00c20630c1a28b55bf8505e2dfe4241ba0be))
* **process-mcp-server:** add 14 @McpTool definitions for judicial process queries ([10d3568](https://github.com/brenothales/tjsc-ai/commit/10d3568b4ce65383c28545e99187bc10ff6686d1))
* **process-mcp-server:** add HTTP client for process-data-service with error handling ([125c68a](https://github.com/brenothales/tjsc-ai/commit/125c68abf5f42b1ec840e963328fdd6533e10edc))
* **process-mcp-server:** add Streamable HTTP MCP server with Spring AI ([8efcd0a](https://github.com/brenothales/tjsc-ai/commit/8efcd0ae95a1ae3fb87cec364ea14f43dfefc265))


### Correções

* anchor out/ rule to repo root to avoid matching Java adapter packages ([07ce095](https://github.com/brenothales/tjsc-ai/commit/07ce0953ec26f253c09f09343b4a3e5f7b118d36))
* **frontend:** add security headers to Nginx — X-Frame-Options, X-Content-Type-Options, Referrer-Policy ([0526fbc](https://github.com/brenothales/tjsc-ai/commit/0526fbccc303c088e8157d61f010c3012455e10a))
* **process-mcp-server:** add connection timeout to process-data-service HTTP client ([086a59a](https://github.com/brenothales/tjsc-ai/commit/086a59a41cf561b48f227c9e46e84aecfb769fa6))


### Documentação

* add DECISOES with architectural decision records ([fa86a99](https://github.com/brenothales/tjsc-ai/commit/fa86a9903e7952f95796c69301d0bbd1e9513490))
* add MELHORIAS with improvement roadmap ([c934490](https://github.com/brenothales/tjsc-ai/commit/c9344903d46deb6c3b26c38f4955040e9605cd97))
* add OKF knowledge base with architecture, services and decision records ([de6ea9c](https://github.com/brenothales/tjsc-ai/commit/de6ea9c10d59617c3292d20b84073f72a1d34664))
* add README with architecture diagram and setup guide ([7f60049](https://github.com/brenothales/tjsc-ai/commit/7f60049d9221fb90f699ce2021e8fcea100e4e5b))
* add stack versions, local setup guide, service sequence and Swagger/Boot UI links ([930a76a](https://github.com/brenothales/tjsc-ai/commit/930a76a69de2e5a703727c7d10c38c1699a83f21))
* **process-data-service:** add service-level README, data model and decision records ([53a30e4](https://github.com/brenothales/tjsc-ai/commit/53a30e4e420d729d6f7b95728eaae0f2552d1118))
