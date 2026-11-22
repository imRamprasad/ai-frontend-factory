# AI Frontend Factory Architecture

## 1. Scope and principles

The AI Frontend Factory is a Spring Boot platform that turns a project brief into a
reviewable, reproducible frontend delivery workspace. It coordinates analysis, technology
selection, agent execution, quality validation, and artifact delivery. It is not a
replacement for the tools that build, test, scan, or package applications.

Principles:

1. Plan before execution; no coding agent receives an unapproved plan.
2. Keep AI provider access behind adapters so Gemini, OpenAI-compatible endpoints, and local
   models can be exchanged.
3. Reuse existing CLIs, APIs, and MCP servers; adapters capture and normalize their evidence.
4. Record prompts, inputs, outputs, versions, container image references, and gate decisions.
5. Run untrusted generated code in short-lived Docker environments with explicit limits.
6. Require human approval for technology selection, execution, privileged access, and delivery.
7. Keep the public boundary REST/OpenAPI and use MCP only for tools that actually expose it.

## 1.1 Agreed MVP defaults

These decisions are fixed for the initial web MVP:

- **Backend:** Java 25 with a Spring Boot modular monolith.
- **Database:** MySQL 8.x as the primary relational database, running locally during
  development.
- **Persistence:** Spring Data JPA and Hibernate behind repository interfaces, with Flyway for
  schema migrations.
- **Web frontend:** React and TypeScript.
- **Android:** native Kotlin and Jetpack Compose are the planned target; Android
  implementation is deferred until the web MVP is working.
- **Authentication:** Spring Security with JWT; development initially supports one local
  user, while production requires a proper signing-key and user-management policy.
- **Artifact storage:** local filesystem in development behind an `ArtifactStore` boundary,
  with a later S3-compatible implementation possible.
- **AI:** one provider adapter initially behind provider-neutral interfaces.
- **Coding agent:** an OpenCode CLI adapter only after the current documented interface,
  version, permissions, and execution requirements are verified.
- **Workflow:** MySQL-backed coordination and worker polling; no separate workflow
  engine or broker in the MVP.
- **Execution:** restricted Docker workers with no Docker socket mounted into workers. Docker
  is not considered a complete security boundary.
- **Licensing:** preserve third-party notices and license metadata; the user selects the
  license for generated projects.

The MVP does not include Android generation, local-model routing, multiple AI providers,
parallel specialist execution, automatic publishing, or a broad technology catalog.

## 2. Logical architecture

```text
React web console
        |
        v
Spring Boot API / identity / project lifecycle
        |
        +--> Workflow engine and run coordinator
        |       +--> Requirements analyst
        |       +--> Technology intelligence
        |       +--> Agent planner and registry
        |       +--> Execution scheduler
        |       +--> Quality-gate coordinator
        |       +--> Delivery packager
        |
        +--> AI provider gateway ----> Gemini / compatible / local endpoint
        +--> Tool and MCP adapters --> coding / test / scan / performance tools
        +--> Docker execution manager -> isolated worker containers
        +--> MySQL 8.x -> control-plane metadata and audit
        +--> Artifact storage -> source, logs, reports, archives
```

The first deployment should be a modular monolith with separate worker processes. These are
module boundaries, not an early microservice mandate. Split a module only when measured
scaling, isolation, or ownership needs justify the operational cost.

## 3. Backend modules

### Identity and access

Users, organizations, projects, roles, sessions, authorization, project policies, and
short-lived worker credentials. Provider secrets are references to an external secret store or
environment-injected values and are never persisted as plaintext project data.

### Project intake

Accepts project name, description, target platforms, constraints, license preferences, and
reference materials. Creates an immutable intake revision so every plan is traceable to the
exact brief.

### Requirements analysis

Calls an AI provider through the gateway and produces structured users, journeys, screens,
data needs, integrations, non-functional requirements, assumptions, open questions, and
acceptance criteria. It distinguishes user facts from model suggestions and stops for
clarification rather than silently guessing.

### Technology intelligence

Maintains a versioned catalog of frameworks, runtimes, tools, licenses, platform support,
maintenance evidence, and adapter capabilities. Produces a decision record with evidence,
alternatives, and trade-offs, not just an opaque score. A tool is not “supported” until its
adapter and license policy are verified.

### Agent catalog and planner

Stores reusable templates and their permissions. Converts approved requirements and the
technology decision into an execution graph. Dynamic agents are project-scoped instances of
known capabilities, not arbitrary privileged software.

Initial role templates:

- requirements analyst
- UX and information-architecture designer
- web architect
- Android architect
- visual designer
- web implementation specialist
- Android implementation specialist
- integration specialist
- test specialist
- accessibility specialist
- security specialist
- performance specialist
- documentation and delivery specialist

### Workflow and execution coordinator

Persists graph state, schedules ready tasks, handles retries and cancellation, enforces
concurrency limits, and sends work to isolated workers. Task submission uses idempotency keys;
every attempt is recorded. The first implementation can use a MySQL-backed queue and worker
polling; a durable workflow engine can be introduced later behind the same contract.

### AI provider gateway

Normalizes chat, structured-output, token-budget, timeout, usage, redaction, retry, and health
behavior. Supports Gemini, OpenAI-compatible endpoints, and local model servers only when their
documented protocol is verified. It stores provider/model metadata, never provider secrets.

### Tool and MCP adapter gateway

Every adapter must:

1. validate its capability and input contract;
2. create a workspace-scoped invocation;
3. run the documented CLI, API, or MCP operation;
4. capture status, structured results, logs, and versions;
5. translate results into factory evidence; and
6. remove temporary credentials and workspace state.

Candidate adapter categories are coding agents, browser/UI tests, accessibility, security,
performance, and Android build/test. Each adapter declares its upstream project, version,
license, invocation method, permissions, network needs, and artifact format. If no verified
adapter exists, the capability is unavailable rather than replaced by an invented integration.

An OpenCode adapter must select one verified interface (CLI, ACP, or MCP) and implement that
interface's documented session, permission, cancellation, streaming, and artifact semantics.
ACP and MCP are not interchangeable. The adapter conformance test must run against the pinned
OpenCode version and prove task submission, approval handling, failure propagation, and
artifact capture before the capability is enabled.

### Docker execution manager

Resolves approved worker images, mounts only declared workspaces/caches, applies CPU, memory,
disk, process, timeout, and network policies, captures image digests and logs, and destroys
temporary containers. The baseline must use rootless or non-root workers, a read-only image
filesystem where possible, dropped Linux capabilities, seccomp/AppArmor or equivalent host
policy, no Docker socket, no host-path mounts, and network denial by default. Package access
must use a narrow allowlist and an auditable proxy/cache.

Docker alone is not a sufficient boundary for hostile code. The threat model must decide when
to use a stronger sandbox such as a VM or a sandboxed container runtime, and which host
operators are trusted. Image scanning and signing reduce supply-chain risk but do not replace
runtime isolation.

### Quality-gate coordinator

Consumes adapter evidence and produces pass, fail, waived, or blocked decisions:

- functionality: build, unit, integration, and declared user-flow checks;
- security: dependency/container scanning, secret detection, and static analysis;
- accessibility: automated checks plus an explicit manual-review boundary;
- performance: build-size and browser performance evidence with environment metadata;
- platform: Android build, lint, and emulator/device evidence;
- delivery: source completeness, documentation, reproducibility, and deployment instructions.

Every gate records scope, tool version, thresholds, exemptions, and waiver approval.

### Artifact and delivery

Packages source, documentation, reports, manifests, deployment instructions, and checksums.
Large content belongs in durable artifact storage; MySQL stores metadata, provenance,
checksums, and access policy. Deliveries may be downloadable archives or Git-compatible
exports without requiring a hosted Git service.

## 4. Target frontend platforms

React and TypeScript are the default web target. The technology decision selects routing,
styling, state, and build tools from the reviewed catalog rather than hard-coding an
unreviewed matrix.

Native Kotlin and Jetpack Compose are the planned Android target. The Android implementation
is deferred until the web MVP is working. The Android contract will include the required
Android SDK, Gradle/JDK, emulator or device strategy, signing policy, and package outputs.
React Native may be evaluated later, but it is not an implicit fallback. Web evidence never
substitutes for Android build and runtime evidence.

## 5. Agent communication

Agents do not call one another directly. The coordinator owns scheduling and state.

### Typed task contract

```text
Task {
  id, runId, agentInstanceId, capability, attempt,
  inputArtifactRefs, constraints, budget, deadline,
  requestedTools, approvalRequirements
}

TaskResult {
  taskId, status, outputArtifactRefs, evidenceRefs,
  diagnostics, usage, toolVersions, nextActions
}
```

Large content is immutable and referenced by artifact IDs. Small state changes are events. A
worker never mutates another agent's output in place.

### Workflow events

Use versioned, append-only events such as `requirements.completed`,
`technology.decision.proposed`, `plan.approved`, `task.ready`, `task.completed`,
`gate.failed`, and `delivery.ready`. Each has actor, timestamp, correlation ID, and schema
version. A MySQL outbox is sufficient for the first milestone; an external broker is a later
scaling option.

### MCP

Use MCP only when an upstream tool exposes an MCP server and the adapter can enforce
permissions and capture evidence. MCP tools are capability-scoped per task. The factory
domain model stays independent of MCP so a verified CLI or HTTP adapter can provide the same
capability.

The AI provider gateway must expose capability negotiation rather than promise uniform
features. A provider declares support for structured output, tool calls, streaming, vision,
context limits, embeddings, and local/offline operation. The planner selects only capabilities
that the chosen provider can satisfy; there is no silent model or provider fallback.

## 5.1 First end-to-end MVP workflow

The first supported path is one user journey over asynchronous run execution:

1. **Create project:** submit a name, description, target web platform, constraints, and
   generated-project license choice. Create `Project` and immutable `ProjectRevision` records.
2. **Enter requirements:** review or edit the revision and submit it for analysis. Reject empty
   or over-sized input and record a revision checksum.
3. **Analyze requirements:** create an analysis task for the single configured provider
   adapter. Produce a validated `RequirementSet`, clarification questions, assumptions,
   acceptance criteria, model metadata, and usage. Invalid structured output is a failed task,
   not a success-shaped fallback.
4. **Select technology:** evaluate the small MVP catalog and create a `TechnologyDecision`
   containing the React/TypeScript baseline, evidence, alternatives, license checks, and
   unresolved questions.
5. **Approve plan:** require user approval before creating the first `WorkflowRun`; no coding
   agent is invoked before approval.
6. **Generate React frontend:** submit one bounded web-generation task to the verified OpenCode
   CLI adapter in a restricted Docker worker. The adapter returns source, logs, versions, and
   diagnostics as artifacts.
7. **Validate and deliver:** run MVP build/test, accessibility, and one dependency/security
   check through verified adapters. Passing gates create a manifest and package source,
   documentation, notices, and deployment instructions in the local `ArtifactStore`.
   Failures stop delivery and remain reviewable.

Run states are `DRAFT`, `ANALYZING`, `DECISION_PENDING`, `APPROVAL_PENDING`, `EXECUTING`,
`VALIDATING`, `DELIVERED`, `FAILED`, `CANCELLED`, and `BLOCKED`. State transitions are
persisted with optimistic versioning and audit events. The UI observes progress through the
run-events endpoint; it does not call provider or worker internals directly.

## 6. Database entities

MySQL 8.x stores control-plane metadata and structured records. Spring Data JPA and Hibernate
provide persistence through repository interfaces, while Flyway owns versioned schema
migrations. Database access must not leak into orchestration or domain services, allowing
another database implementation only if a future requirement genuinely justifies it. Source
trees, large logs, screenshots, videos, and archives belong in artifact storage.

- **User**, **Organization**, **Membership**, **Role**: access control.
- **Project**: owner, name, status, policy, timestamps.
- **ProjectRevision**: immutable brief, constraints, platforms, and references.
- **RequirementSet**: analyzed requirements, assumptions, questions, acceptance criteria, and
  model provenance.
- **TechnologyCatalogEntry**: versioned tool/framework/provider metadata, compatibility,
  maintenance evidence, and license identifiers.
- **TechnologyDecision**: selected stack, alternatives, evidence, policy checks, and approval.
- **AgentTemplate**: reusable role, prompt contract, capability requirements, and version.
- **AgentInstance**: project-scoped template configuration, permissions, and status.
- **WorkflowRun**: complete run, input revision, policy snapshot, and lifecycle status.
- **WorkflowTask**: graph node, dependencies, agent, input/output references, retries, status,
  and idempotency key.
- **ToolInvocation**: adapter, command/protocol, version, permissions, environment, result,
  and logs.
- **QualityGateDefinition** and **QualityGateResult**: policy, evidence, decision, waiver,
  and approver.
- **Artifact**: media type, checksum, storage locator, retention, and provenance.
- **Delivery**: packaged artifact set, manifest, checksum, and release state.
- **AuditEvent**: actor, action, resource, correlation ID, and timestamp.

Relationships:

- Project has many revisions, runs, artifacts, and deliveries.
- Run has one requirement set and technology decision, and many tasks, invocations, gates,
  and artifacts.
- Tasks have dependency edges and at most one active attempt.
- Artifacts link to producing tasks and invocations.
- Gate results link to the exact evidence evaluated.

Use optimistic locking for mutable workflow records and uniqueness constraints for revision
numbers, task idempotency keys, artifact checksums within a delivery, and event sequence
numbers within a run.

`ArtifactStore` owns bytes and lifecycle operations; MySQL stores artifact metadata and
checksums. The development implementation is local filesystem storage with project/run
scoping and quotas. S3-compatible storage is a later implementation, not an MVP dependency.

## 7. API boundaries

The public API is versioned under `/api/v1` and described with OpenAPI.

**Project and intake**

- `POST /projects`
- `GET /projects/{projectId}`
- `POST /projects/{projectId}/revisions`
- `GET /projects/{projectId}/revisions/{revisionId}`

**Analysis and decisions**

- `POST /projects/{projectId}/analyses`
- `GET /analyses/{analysisId}`
- `POST /projects/{projectId}/technology-decisions`
- `POST /technology-decisions/{decisionId}/approve`

**Runs**

- `POST /projects/{projectId}/runs`
- `GET /runs/{runId}`
- `POST /runs/{runId}/pause`
- `POST /runs/{runId}/resume`
- `POST /runs/{runId}/cancel`
- `GET /runs/{runId}/tasks`
- `GET /runs/{runId}/events` (SSE is suitable for the first console)

**Agents and capabilities**

- `GET /agent-templates`
- `GET /projects/{projectId}/agents`
- `GET /capabilities`

Agent creation is a coordinator operation, not an unrestricted public endpoint.

**Quality and delivery**

- `GET /runs/{runId}/gates`
- `POST /gates/{gateResultId}/waive`
- `GET /projects/{projectId}/artifacts`
- `POST /deliveries`
- `GET /deliveries/{deliveryId}`
- `GET /artifacts/{artifactId}/download`

Workers use a separate authenticated internal contract with short-lived task tokens.

The MVP uses one local development JWT user and a configured signing key. JWT claims must
carry the user and project authorization context needed by the API; no endpoint may trust a
client-supplied project ID without authorization.

## 8. Security, licensing, and trade-offs

Never place provider keys in prompts, generated source, logs, or database rows. Redact tool
output, reject secret artifacts, default generated-code tasks to no network access, and
require approval for execution, external access, delivery, and publishing. Treat repositories
and model output as untrusted input.

License metadata belongs in the technology catalog. Permissive licenses such as Apache-2.0,
MIT, and BSD still require notices and attribution. Copyleft, source-available, hosted-only,
or commercial terms require a policy decision because redistribution and SaaS obligations can
differ. Candidate tools such as Docker/OCI tooling, OpenCode, Playwright, axe-core,
Lighthouse, OWASP ZAP, Trivy, Testcontainers, and Android tooling must be checked for their
current license and integration surface at implementation time; this list is not automatic
approval or a claim that every tool supports every workflow.

The main trade-offs are:

- modular monolith first: fewer operational dependencies while workers remain isolated;
- MySQL outbox first: simple local deployment, with a broker added only after measured need;
- external artifact storage: smaller database backups, at the cost of lifecycle management;
- provider adapters: free/local flexibility, at the cost of capability negotiation;
- Native Kotlin/Compose first for Android: strongest platform fidelity and explicit Android
  behavior, at the cost of a separate language/toolchain from the React web frontend.
- Android emulators and browser workers: predictable evidence, but significant CPU, storage,
  startup-time, and CI cost; device testing may require dedicated infrastructure.
- local models: lower per-request cost and stronger data locality, but potentially high GPU
  and operations cost plus weaker structured-output/tool-call support.
- JWT development mode: low setup cost, but it must not be promoted to production without
  key rotation, revocation/expiry policy, and a real user-management decision.

## 9. Recommended first milestone

For a small team, the MVP must freeze the supported matrix to one web stack, one AI provider,
one coding-agent interface, one Docker worker image, and a minimal set of quality tools.
Android, local models, multiple providers, and dynamic parallel execution remain design
targets until the vertical slice is reliable.

Build one governed vertical slice:

1. Create a project and immutable intake revision.
2. Run requirements analysis through one configured AI provider.
3. Persist structured requirements and clarification questions.
4. Produce a technology decision from a small reviewed catalog.
5. Require human approval.
6. Execute one web planning/implementation task in Docker through one verified coding-agent
   adapter.
7. Run functionality, accessibility, and one dependency/security gate through adapters.
8. Package source, evidence, and documentation as a delivery.

This validates the highest-risk seams: provider credentials, structured model output,
orchestration, isolation, adapter contracts, provenance, and approval. Performance evidence,
Android, dynamic multi-agent expansion, local-model routing, and horizontal scaling should
follow only after this path is observable and repeatable.

## 10. MVP decisions still required

The defaults above are agreed; these implementation decisions remain open:

1. Which AI provider and model are the first adapter target, including structured-output
   support, context limit, budget, and data-retention policy.
2. Which OpenCode CLI version and exact invocation/session contract will be pinned and tested.
3. The worker host policy: rootless Docker availability, package-cache/proxy design, resource
   quotas, and when a VM or stronger sandbox is mandatory.
4. The initial technology-catalog entries and evidence required to mark each supported.
5. MVP quality thresholds and the authority allowed to waive a failed gate.
6. Local artifact quotas, retention, archive format, and whether downloads require signed URLs.
7. JWT signing-key storage, expiry/refresh behavior, and the production identity migration path.
