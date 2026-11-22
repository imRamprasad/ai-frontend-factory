# AI Frontend Factory Roadmap

## Delivery approach

Work in vertical slices. Each phase must leave a usable increment and must not silently
broaden the supported technology matrix. “Supported” means an adapter, policy, tests,
documentation, and license review exist for that capability.

## Phase 0 — Architecture and policy approval

**Goal:** agree on boundaries before implementation.

- Review `docs/architecture.md`.
- Choose the modular Spring Boot control plane plus isolated workers.
- Define identity, tenant, retention, provider, network, and artifact policies.
- Define the initial approved license policy.
- Select the first AI provider and coding-agent integration only after verifying its
  documented API or protocol.
- Define the first web template and Android strategy decision rule.
- Freeze the MVP matrix to one web stack, one provider, one coding-agent interface, one worker
  image, and a minimal quality-tool set.
- Record the agreed defaults: Spring Boot modular monolith, MySQL 8.x, Spring Data JPA,
  Hibernate, Flyway, repository interfaces, React/TypeScript,
  native Kotlin/Compose deferred to post-web MVP, Spring Security/JWT single-user development
  mode, local `ArtifactStore`, MySQL coordinator, restricted Docker without a socket, and
  user-selected generated-project licensing.

**Exit evidence:** approved architecture, policy decisions, capability matrix, and deferred
integration list, plus the unresolved MVP decisions listed in `docs/architecture.md`.

## Phase 1 — Governed project intake

**Goal:** accept and preserve a project brief.

- Implement project, organization, membership, and authorization boundaries.
- Add project creation and immutable intake revisions.
- Add validation, reference metadata, audit events, and project status.
- Add the React/TypeScript project console.
- Add Spring Security/JWT single-user development mode and authorization checks, keeping the
  signing-key boundary ready for production identity.
- Add the local filesystem `ArtifactStore` interface, project/run scoping, checksums, and
  quotas.
- Configure local MySQL 8.x for development, add Spring Data JPA/Hibernate repositories, and
  apply schema changes exclusively through Flyway migrations. Keep domain and orchestration
  code independent of database-specific APIs.
- Define OpenAPI contracts and error conventions.

**Exit evidence:** a user can create and retrieve a project revision with an audit trail,
authenticate in development, and store a scoped artifact without exposing storage details
through the API.

## Phase 2 — Requirements analysis vertical slice

**Goal:** turn one brief into reviewable structured requirements.

- Implement the AI provider gateway with one provider adapter.
- Define schemas for requirements, assumptions, questions, journeys, and acceptance criteria.
- Add redaction, budgets, timeouts, retries, and explicit provider errors.
- Persist model provenance and the exact input revision.
- Add clarification and approval states.

**Exit evidence:** malformed or incomplete model output is rejected rather than silently
accepted, and the result is traceable to its input.

## Phase 3 — Technology intelligence and plan approval

**Goal:** make stack selection evidence-based.

- Create the versioned technology catalog.
- Add compatibility, platform, maintenance, license, and adapter metadata.
- Implement candidate evaluation and decision records with alternatives and rationale.
- Add plan review and approval.
- Keep the initial catalog deliberately small: one web path, one Android path, one database,
  one container approach, and verified quality tools only.
- Record the user's generated-project license choice and preserve third-party notices in the
  technology decision.

**Exit evidence:** a reviewer can see why a stack was selected and approve or reject it.

## Phase 4 — Agent graph and Docker execution

**Goal:** execute one approved task safely.

- Implement the initial bounded agent template and task graph; defer dynamic parallel
  specialist creation until the web MVP is proven.
- Implement the database-backed coordinator and worker contract.
- Add Docker worker provisioning, resource limits, mounts, cancellation, timeout, cleanup,
  and execution provenance.
- Enforce rootless/non-root execution, dropped capabilities, default network denial, no Docker
  socket, pinned image digests, and a documented decision for stronger sandboxing.
- Add idempotent submission and retry rules.
- Verify and pin one OpenCode CLI version and its documented invocation/session requirements.
  Do not assume ACP or MCP semantics for this MVP.

**Exit evidence:** one approved web task runs in isolation and can be resumed or cancelled
with complete audit evidence.

## Phase 5 — Quality gates and delivery

**Goal:** produce a trustworthy first delivery.

- Add adapters for build/test, accessibility, and one dependency/security check.
- Defer performance automation until the MVP has a stable browser environment and baseline.
- Normalize tool results into quality-gate evidence.
- Implement pass/fail/blocked/waived decisions and approvals.
- Package source, documentation, reports, manifests, checksums, and deployment instructions.
- Link every output to its producing task and tool version.

**Exit evidence:** a generated web frontend is delivered with reproducible gate evidence.

## Phase 5A — End-to-end web MVP gate

**Goal:** prove the complete approved workflow before adding Android or more agents.

- Create a project and requirements revision.
- Analyze requirements with the single configured provider.
- Select and approve the React/TypeScript technology decision.
- Generate one basic React frontend through the verified OpenCode CLI adapter in Docker.
- Run the MVP quality gates and package source, documentation, notices, and deployment
  instructions in the local `ArtifactStore`.

**Exit evidence:** the workflow in `docs/architecture.md` completes from project creation to
downloadable delivery, and failures remain resumable and auditable.

## Phase 6 — Dynamic specialist orchestration

**Goal:** expand from one task to a requirement-driven specialist graph.

- Add UX, architecture, implementation, integration, testing, accessibility, security,
  performance, and documentation templates.
- Add capability negotiation and dependency-aware scheduling.
- Add bounded parallel execution with explicit workspace ownership.
- Add artifact handoff contracts and conflict detection.
- Add human gates for plan approval, privileged access, and final delivery.

**Exit evidence:** different requirements produce different justified graphs while preserving
permissions, budgets, provenance, and reproducibility.

## Phase 7 — Android delivery

**Goal:** support a reviewed Android path.

- Implement native Kotlin and Jetpack Compose as the planned Android target. This phase is
  intentionally deferred until the web MVP gate passes.
- Add Android SDK/build-tool images and license acceptance policy.
- Pin the required JDK, Gradle, Android SDK/build-tools, emulator/device strategy, signing
  policy, and package outputs.
- Add build, lint, unit, emulator/device, and package artifact adapters.
- Extend gates and delivery manifests for Android.
- Validate Android independently; web evidence does not prove Android correctness.

**Exit evidence:** a supported Android project builds in the declared image and produces
platform-specific test and delivery evidence.

## Phase 8 — Provider and scale expansion

**Goal:** offer free-first and local execution without weakening controls.

- Add a second OpenAI-compatible provider and a documented local-model adapter.
- Add capability discovery, routing policy, fallback rules, and per-provider budgets.
- Add durable workflow recovery and worker autoscaling only when measured workloads require it.
- Evaluate an external broker and service split from observed bottlenecks.
- Add safe dependency/model caching without leaking project data.

**Exit evidence:** provider selection is policy-driven, failures are explicit, and local
execution has equivalent artifact and gate contracts.

## Phase 9 — Hardening and ecosystem growth

**Goal:** make the platform maintainable for contributors.

- Publish adapter and agent-template contribution contracts.
- Add reproducible environments, CI checks, dependency/container scanning, secret detection,
  and license reporting.
- Add threat modeling, abuse-case testing, rate limits, quota enforcement, and retention.
- Document supported versions and deprecation policy.
- Add adapter integration tests and a worker conformance suite.

**Exit evidence:** contributors can add adapters or templates without changing core
orchestration semantics, and releases include provenance, license, security, and regression
evidence.

## Milestone gates

| Gate | Required before moving forward |
| --- | --- |
| G0 Architecture | Boundaries, policies, and capability matrix approved |
| G1 Intake | Project and immutable revision lifecycle works |
| G2 Analysis | Structured requirements persist with provenance |
| G3 Planning | Technology decision is explainable and approved |
| G4 Execution | One task runs in a constrained Docker worker |
| G5 Delivery | Source, docs, reports, and checksums are packaged |
| G6 Expansion | Specialist graph preserves contracts and permissions |
| G7 Android | Android build/test evidence is independently repeatable |
| G8 Scale | Provider/local routing and recovery are measured and governed |

## Explicitly deferred

- Microservice splitting before measured need.
- A custom workflow engine before the coordinator proves insufficient.
- Reimplementing browser, accessibility, security, or performance tools.
- Automatic publishing to Git hosting, app stores, or cloud environments.
- Unrestricted autonomous agents, unrestricted network access, and silent model fallback.
- A large technology catalog without verified adapters, license metadata, and maintenance
  evidence.
- Android implementation until the web MVP gate passes.
- Local filesystem replacement with S3-compatible storage until artifact volume requires it.
- Multiple AI providers and local-model routing until the single-provider workflow is stable.
- Dynamic parallel specialist creation until task isolation and artifact handoff are proven.
