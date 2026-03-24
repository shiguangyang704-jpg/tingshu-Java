---
name: project-talk-packaging
description: Generate reusable Chinese talk tracks for Java backend project modules. Use when the user wants a module explained as a speakable answer with the main flow first, then business logic, implementation difficulties, and optimizations, grounded in real code and without invention.
metadata:
  short-description: Chinese three-part project module talk packaging
---

# Project Talk Packaging

## Overview

Use this skill when the user wants to package a Java backend project module into a Chinese, speakable explanation, especially requests like:

- "帮我讲一下这个模块"
- "按业务逻辑、难点、优化拆开讲"
- "基于代码包装成项目话术"
- "把这个模块整理成能直接说的面试表达"

Default output is one main answer only. Do not add follow-up Q&A unless the user explicitly asks for it.

## Grounding First

If local code is available, inspect the real implementation before writing anything substantial.

Prioritize these sources of truth:

1. Controller or API entrypoints
2. Service implementation flow
3. Mapper or repository queries
4. XML SQL, constants, MQ consumers, cache annotations, remote clients
5. Table names and data relationships visible in code

Only fall back to the user's description when the repo does not prove enough detail.

Never invent:

- business flows
- rule branches
- optimizations
- table structures
- MQ or async links
- cache behavior
- performance claims
- reliability guarantees

If evidence is partial, say so explicitly with wording such as:

- `按当前代码看`
- `从这版实现看`
- `当前代码里主要是`

## What To Extract From Code

Before writing, extract the module's real:

- business positioning in the project
- user-facing entry or trigger
- complete main flow in time order
- rule judgment chain and branch conditions
- key boundary conditions
- storage or table design
- data flow, rights flow, or state transition flow
- async, MQ, cache, or idempotency behavior if it exists
- real implementation difficulties
- real optimizations that are actually visible in code

Optimization points must be code-driven. Only include techniques that really exist, for example:

- batch querying
- pagination
- SQL aggregation
- Redis caching
- Lua or duplicate-submit protection
- MQ async decoupling
- idempotency
- strategy pattern
- bloom filter
- remote-call short-circuiting

If the code does not show a technique, do not mention it.

## Default Output

Unless the user explicitly asks for a different format, output exactly these top-level sections in this order:

1. `业务逻辑`
2. `遇到的难点`
3. `怎么优化的`

### Required Structure Inside `业务逻辑`

`业务逻辑` must begin with a `主流程` subsection.

The `主流程` subsection must:

- explain the flow in time order
- answer "用户进入后，系统先做什么，再做什么，最后怎么落库或回写"
- avoid scattered bullets before the full chain is clear

After `主流程`, continue with a `详细展开` subsection that explains:

- key rule branches
- boundary conditions
- table or data usage
- state, rights, or data flow where relevant

### Required Structure Inside `遇到的难点`

This section should explain real difficulties visible from the code or strongly implied by the implementation, such as:

- complex rule matrices
- cross-service coordination
- consistency after payment or state changes
- performance pressure on list queries
- boundary conditions that are easy to get wrong

Do not write generic filler like "并发高" unless the code actually shows the relevant pressure or mitigation.

### Required Structure Inside `怎么优化的`

Each optimization point should follow this closure:

1. original problem
2. concrete solution in the code
3. effect or business value

Prefer wording equivalent to:

- `原来...`
- `后来我把它改成...`
- `这样做的好处是...`

Only include optimizations that exist in the code.

## Style Constraints

The final answer should:

- be in Chinese by default
- be oral, natural, and easy to say aloud
- be suitable for interview-style module explanation
- explain business before technology
- prefer first-person expression when the user wants "话术" or "面试表达"
- avoid class names, method names, and code links unless the user explicitly asks for them
- prefer business terms, table names, and domain concepts over source-code identifiers
- keep structure stable and easy to scan

Do not:

- start with code references
- dump method-by-method walkthroughs
- mix the three top-level sections together
- over-abstract into architecture jargon with no code evidence

## Response Pattern

When the user asks for a module explanation, follow this working order:

1. inspect code
2. rebuild the main flow
3. sort rules and branches
4. identify real difficulties
5. identify real optimizations
6. write the final answer in the fixed three-part structure

## Failure Mode

If the repo does not provide enough evidence for one part:

- say less, not more
- stay conservative
- explain only what can be proven
- separate "当前实现" from "业务设计意图" when they differ
- do not fill gaps with standard interview clichés
