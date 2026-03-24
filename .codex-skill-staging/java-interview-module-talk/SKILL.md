---
name: java-interview-module-talk
description: Generate reusable Chinese interview scripts for Java backend project modules. Use when the user wants a spoken project-module explanation, module walkthrough, interview-ready module script, or follow-up Q&A for any Java project. When code is available, inspect the real implementation first and base the script on actual business flow, data design, async flow, and performance work. Keep the final script first-person, oral, back-and-forth interview friendly, and avoid variable names, method names, and class names in the final spoken answer.
metadata:
  short-description: Java项目模块面试话术与追问生成
---

# Java Interview Module Talk

## Overview

Use this skill when the user wants a Chinese interview script for a Java backend project module, such as:

- "帮我讲一下这个项目的订单模块"
- "生成支付模块的面试话术"
- "基于代码写一版可背诵的项目模块讲解"
- "补这个模块的高频追问和标准回答"

This skill is generic for any Java project and any business domain, including e-commerce, insurance, content, SaaS, finance, logistics, or platform systems.

## Grounding First

If local code is available, inspect the real implementation before writing anything substantial.

Prioritize these sources of truth:

1. Controller or API entrypoints
2. Service implementation flow
3. Mapper or repository queries
4. XML SQL, constants, MQ consumers, cache aspects, config
5. Table names and data relationships visible in code

Only fall back to the user's description when the repo does not contain enough information.

Never invent:

- business flows
- performance optimizations
- table structures
- async chains
- consistency guarantees
- quantified results

If a result metric is not available from the user or codebase, use `*行业基准值，可根据实际情况调整*`.

## What To Extract From Code

Before writing, extract the module's real:

- business positioning in the project
- user-facing entry or carrier
- main request or processing chain
- actual performance optimizations used in code
- key business rules and branch conditions
- table or storage design
- async flow, MQ flow, cache flow, or idempotency flow
- boundary cases and failure handling

Performance optimizations must be dynamic and code-driven. Do not force any fixed technique. Only include techniques that are actually present, for example:

- `CompletableFuture` async aggregation
- Redis caching
- `Redisson` distributed locks
- `SETNX` or Lua idempotency
- MQ async decoupling
- Nacos rate limiting
- SQL or index optimization
- pagination or batch processing
- thread pool isolation
- delayed queue
- bloom filter

If the code only shows SQL optimization or rate limiting, write only that. If there is no cache in code, do not mention Redis.

## Default Output

Unless the user explicitly asks for a different format, output both:

1. A main interview script of about 4 minutes
2. About 6 follow-up questions with:
   - `追问`
   - `答题思路`
   - `标准回答`

If the user explicitly asks for only the main script or only follow-ups, follow that request.

## Main Script Structure

The main script must be in Chinese, first person, oral, and directly answer the interviewer.

Use this exact high-level structure:

1. Opening calibration
2. `**【性能优化】**`
3. `**【核心业务规则设计】**`
4. `**【核心数据处理与优化】**`

### Opening

Open by clearly stating:

- I was mainly responsible for the full lifecycle of `[模块名称]`
- the module's business positioning
- the two core challenges:
  - high-concurrency or performance pressure
  - accurate handling of core business logic
- I mainly landed work from:
  - performance optimization
  - core business rule design
  - core data processing and optimization

### Required Closure In Each Section

Each of the three sections must follow this closure:

1. business scenario or original pain point
2. my technical solution and landing details
3. final effect or business value

## Style Constraints

The final spoken answer must:

- use only first person
- be easy to memorize
- sound like a real interview answer, not documentation
- be oral and conversational
- have strong business sense and strong implementation sense
- avoid team-style vague wording such as "我们"
- avoid variable names, method names, and class names
- prefer business expressions or table names such as:
  - 订单主表
  - 订单明细表
  - 保单信息表
  - 用户权益表
  - 专辑信息表
  - 专辑统计表

You may inspect method names or classes while reasoning, but do not expose them in the final spoken script unless the user explicitly asks for code references.

## Section Requirements

### `**【性能优化】**`

This section must be written from real code usage.

It must explain:

- what the original serial or high-cost path was
- what optimization was actually used
- why that optimization fit the module
- what user experience or system pressure improved

Only include optimizations that really exist in code. If the code uses Nacos rate limiting, write that. If the code uses async aggregation, write that. If the code uses cache, write cache. If several exist, combine them naturally.

### `**【核心业务规则设计】**`

This section must explain:

- the business goal
- the complete rule judgment chain
- multi-branch conditions
- at least one clear boundary condition
- the business value after landing

Make sure the logic covers all real scenarios visible from code or user input.

### `**【核心数据处理与优化】**`

This section must explain:

- storage dimension design
- how one set of stored data supports multiple functions
- async data flow if it exists
- deduplication, idempotency, or reliability guarantees if they exist
- the effect on accuracy and performance

If the module has no MQ or async flow in code, do not fabricate it. Instead, focus on real data reuse, query shaping, transaction boundaries, or persistence design.

## Follow-Up Questions

Default to about 6 mixed technical and business follow-ups.

Choose follow-ups based on the actual module implementation, such as:

- why a specific optimization was chosen
- how cache or rate limiting works
- the easiest business rule to get wrong
- how boundary cases are handled
- why tables are split this way
- how async reliability or idempotency is guaranteed
- how to troubleshoot inconsistency or performance jitter

For each follow-up, output:

`追问`

`答题思路`

`标准回答`

Keep `答题思路` short. Make `标准回答` directly speakable.

## Failure Mode

If the repo does not provide enough evidence for one section:

- say less, not more
- stay conservative
- rely on user-provided facts when available
- do not pretend uncertain details came from code
