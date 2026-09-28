---
type: Architecture Pattern
title: Diagrama do Minuta Loop — Fluxo Sequencial
description: Diagrama Mermaid do pipeline multi-step de geração de minutas de sentença — coleta, relatório, fundamentação e dispositivo por VersaoTipo com retry e SSE.
tags: [minuta, multi-step, diagrama, mermaid, sse, retry]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/MinutaLoopService.java"
    title: "MinutaLoopService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/ColetaDadosService.java"
    title: "ColetaDadosService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/RedacaoService.java"
    title: "RedacaoService.java"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Diagrama do Minuta Loop — Fluxo Sequencial

Pipeline executado em `Schedulers.boundedElastic()` — thread separada do request HTTP.
O sink SSE é criado **antes** de iniciar o loop.

## Fluxo completo

```mermaid
graph TD
    REQ(["POST /api/v1/minuta/gerar\n{numero, versoes[]}"])
    SINK["SSE sink criado\nboundedElastic().schedule()"]

    subgraph E1["① COLETA — ColetaDadosService"]
        C1["📡 SSE: BUSCAR_DADOS iniciado"]
        C2["GPT-4o-mini + coleta-system.st\n+ coleta-user.st"]
        C3["buscar_contexto_completo"]
        C4["buscar_peticao_inicial"]
        C5["buscar_documentos_com_texto\n(Contestação)"]
        C6["buscar_movimentacoes"]
        C7["buscar_documentos_com_texto\n(todos)"]
        C8["📡 SSE: BUSCAR_DADOS concluído\n→ dados: IDENTIFICAÇÃO · PARTES\n· FATOS · DEFESA · HISTÓRICO"]
    end

    subgraph E2["② RELATÓRIO — RedacaoService + retry"]
        R1["📡 SSE: REDIGIR_RELATORIO iniciado"]
        R2["GPT-4o-mini\nredacao-system.st + relatorio-user.st\nnarra fatos sem interpretar"]
        R3["📡 SSE: REDIGIR_RELATORIO concluído"]
        R4[("MongoDB\nMinutaDocument reservado\n(versaoMinuta = MAX+1)")]
    end

    subgraph E3["③ LOOP — para cada VersaoTipo (máx 5)"]
        direction TB
        L0{"VersaoTipo:\nprocedente\nimprocedente\nparcialmente procedente"}

        subgraph P["por VersaoTipo"]
            F1["📡 SSE: REDIGIR_FUNDAMENTACAO (tipo)"]
            F2["GPT-4o-mini\nfundamentacao-user.st\n{tipoLabel + tipoInstrucao}"]
            F3["📡 SSE: REDIGIR_DISPOSITIVO (tipo)"]
            F4["GPT-4o-mini\ndispositivo-user.st\ncabeçalho TJSC · artigos CPC\n⚠️ aviso de minuta"]
            F5[("MongoDB\nMinutaVersao salva")]
            F6["📡 SSE: event versao\n{label, relatorio, fundamentacao, dispositivo}"]
        end
    end

    DONE["📡 SSE: event done {versaoMinuta}\nsink.tryEmitComplete()"]
    END(["3 minutas entregues no chat"])

    REQ --> SINK
    SINK --> C1
    C1 --> C2
    C2 --> C3 --> C4 --> C5 --> C6 --> C7
    C7 --> C8
    C8 --> R1
    R1 --> R2 --> R3 --> R4
    R4 --> L0
    L0 --> F1
    F1 --> F2 --> F3 --> F4 --> F5 --> F6
    F6 -->|"próximo VersaoTipo"| L0
    L0 -->|"todos processados"| DONE
    DONE --> END
```

## Retry automático — `comRetry`

Aplicado em `relatorio`, `fundamentacao` e `dispositivo`. Não aplicado na coleta.

```mermaid
graph LR
    IN([chamada RedacaoService]) --> T1

    T1{"tentativa 1"}
    T1 -->|"✅ ok"| OK(["resultado retornado"])
    T1 -->|"❌ erro"| W1["aguarda 1s"]

    W1 --> T2{"tentativa 2"}
    T2 -->|"✅ ok"| OK
    T2 -->|"❌ erro"| ERR(["RuntimeException\nFalha após 2 tentativas"])
```

## Eventos SSE emitidos

| Evento | Quando | Payload chave |
|--------|--------|---------------|
| `step` | a cada sub-etapa iniciada/concluída | `acao`, `mensagem` |
| `versao` | ao concluir cada VersaoTipo | `label`, `relatorio`, `fundamentacao`, `dispositivo`, `steps[]` |
| `done` | pipeline completo | `versaoMinuta` (int — versão salva no MongoDB) |
| `erro` | exceção não tratada | mensagem de erro |
