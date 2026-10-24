# Security Review: KR-Compiler

## Scope

Risk-prioritized whole-repository Standard security review across Android, Windows, Linux, native libraries, project import/build/runner flows, agent boundaries, signing, dependency provisioning, and CI.

- Scan mode: repository
- Target kind: git_revision
- Target ID: target_sha256_d48c48a8e1c912f81cd4e7f4a141fa27f4e96df8df8e57bbfb253811ad1d05fa
- Revision: 05b9bc107d8948ab23f38bb52104b4b511fc6c17
- Inventory strategy: repository
- Included paths: .
- Excluded paths: none
- Runtime or test status: Static validation completed; no unsafe proof-of-concept execution was required for source-established paths.
- Artifacts reviewed: .github/workflows/android-build.yml, .gitignore, android/CC_OpenCl_Enterprise-main/include/CipherCore_NoiseCtrl.h, android/CC_OpenCl_Enterprise-main/include/SymBio_Interface.h, android/CC_OpenCl_Enterprise-main/src/CipherCore_NoiseCtrl.c, android/README.md, android/app/build.gradle, android/app/src/main/AndroidManifest.xml, android/app/src/main/java/kr/compile/ArtifactsManager.java, android/app/src/main/java/kr/compile/BuildManager.java, android/app/src/main/java/kr/compile/CompilerEngine.java, android/app/src/main/java/kr/compile/DocumentTree.java, android/app/src/main/java/kr/compile/GitImporter.java, android/app/src/main/java/kr/compile/KrAgentCore.java, android/app/src/main/java/kr/compile/KrAgentDialog.java, android/app/src/main/java/kr/compile/KrFileProvider.java, android/app/src/main/java/kr/compile/KrPipeline.java, android/app/src/main/java/kr/compile/KrProjectConfig.java, android/app/src/main/java/kr/compile/KrSourceMap.java, android/app/src/main/java/kr/compile/ProjectManager.java, android/app/src/main/java/kr/compile/RunnerPackager.java, android/app/src/main/java/kr/compile/ToolchainInstaller.java, android/app/src/main/java/kr/compile/VcpkgManager.java, android/build.gradle, android/gradle.properties, android/gradle/wrapper/gradle-wrapper.properties, android/runner/build.gradle, android/runner/src/main/AndroidManifest.xml, android/runner/src/main/cpp/CMakeLists.txt, android/runner/src/main/cpp/runner.cpp, android/runner/src/main/java/kr/compile/runner/GuiRunnerActivity.java, android/runner/src/main/java/kr/compile/runner/LauncherActivity.java, android/runner/src/main/java/kr/compile/runner/RunnerActivity.java, android/runner/src/main/java/kr/compile/runner/RunnerFiles.java, android/scripts/ExportSigningMaterial.java, android/scripts/build-apk.ps1, android/scripts/download-toolchain.ps1, android/scripts/prepare-signing.ps1, android/settings.gradle, linux/Start-KR-IDE.sh, linux/build.sh, linux/scripts/extract-zip.py, linux/scripts/provision-toolchain.sh, linux/scripts/verify-toolchain.sh, linux/src/kr/compile/ArtifactsManager.java, linux/src/kr/compile/BuildManager.java, linux/src/kr/compile/CompilerEngine.java, linux/src/kr/compile/DocumentManager.java, linux/src/kr/compile/KrAgentCore.java, linux/src/kr/compile/KrProjectConfig.java, linux/src/kr/compile/KrSourceMap.java, linux/src/kr/compile/ProjectManager.java, linux/src/kr/compile/ToolchainManager.java, linux/src/kr/compile/VcpkgManager.java, linux/src/kr/compile/linux/AgentDialog.java, linux/src/kr/compile/linux/BuildConsolePanel.java, windows/Manifest.txt, windows/README.md, windows/Start-KR-IDE-Console.bat, windows/Start-KR-IDE.bat, windows/bootstrap-toolchain.ps1, windows/build.bat, windows/download-toolchain.ps1, windows/provision-runtime.ps1, windows/src/kr/compile/ArtifactsManager.java, windows/src/kr/compile/BuildManager.java, windows/src/kr/compile/CompilerEngine.java, windows/src/kr/compile/KrAgentCore.java, windows/src/kr/compile/KrProjectConfig.java, windows/src/kr/compile/KrSourceMap.java, windows/src/kr/compile/ProjectManager.java, windows/src/kr/compile/ToolchainManager.java, windows/src/kr/compile/windows/AgentDialog.java, windows/src/kr/compile/windows/BuildConsolePanel.java

Limitations and exclusions:
- 74 of 362 tracked paths were fully reviewed; coverage is partial.
- The three very large mirrored CC_OpenCL.c files received focused review rather than exhaustive line-by-line coverage.
- Checked-in binaries and document archives were not reverse engineered.
- The repository does not contain provenance for any externally distributed official APK.

### Scan Summary

| Field | Value |
| --- | --- |
| Reportable findings | 18 |
| Severity mix | critical: 1, high: 6, medium: 9, low: 2 |
| Confidence mix | high: 18 |
| Coverage | partial |
| Validation mode | Independent delegated discovery followed by primary source-backed validation and attack-path calibration. |

Canonical artifacts: `scan-manifest.json`, `findings.json`, and `coverage.json`. This report is a deterministic projection of those files.

## Threat Model

KR-Compiler is a local cross-platform IDE/compiler for KR, C++, Rust, Go, CMake, Android, Windows, and Linux. User-controlled projects and archives flow through parsers, generators, native libraries, provisioning scripts, compiler toolchains, KR_BUILD output, Android SAF/cache mirrors, an optional model agent with mutation/build approvals, and generated signed runner APKs.

### Assets

- User projects, SAF-backed trees, source-map data, credentials, and generated build artifacts
- Host and Android application execution authority, private application state, and persisted SAF grants
- Compiler/toolchain, native-library, dependency, CI, and release artifact integrity
- Agent API token, conversation/project context, canonical workspace confinement, and exact mutation approvals
- Android IDE and runner signing identities and update authority
- Correctness and memory safety of exported adaptive-compute APIs

### Trust Boundaries

- Local or imported project content into build tools, native parsers, generators, and KR_BUILD
- Android SAF provider into the app-private mirror and agent workspace
- Project/task context to a configured model endpoint, then model actions through read/write/build controls
- Maintainer keystore into exported application assets and RunnerPackager
- GitHub repositories and downloaded archives/packages into project and executable toolchain directories
- IDE-generated assets and native libraries into the runner application sandbox
- Pull requests into CI and release/provisioning workflows

### Attacker Capabilities

- Author a malicious repository/project, archive filenames, build metadata, symlinks, source maps, checkpoints, QASM, and public native API inputs
- Control or compromise a configured model endpoint and choose agent action payloads
- Control a GitHub repository selected for import or a referenced dependency/package/release source
- Inspect any distributed IDE or generated runner APK and redistribute a forged update
- Submit an untrusted pull request with read-only CI token context
- No assumed inbound service access, multitenant server, or silent Android installation capability

### Security Objectives

- Confine every project-derived path to the canonical active root and make destructive traversal link-safe
- Bind agent approvals to the exact canonical target and complete resulting bytes
- Isolate SAF projects and prevent cross-project residual data disclosure
- Authenticate executable toolchains/dependencies and honor offline/network choices
- Keep bearer credentials and project context on authenticated confidential transports
- Keep signing private keys out of distributed artifacts and separate IDE/runner identities
- Validate all native dimensions, counts, serialized invariants, and parser lengths before memory operations
- Preserve build and release artifact integrity across supported platforms

### Assumptions

- The authoritative workbench inventory contains 362 tracked paths and no repository SECURITY.md was present.
- The frozen source shows Android online dependencies default enabled even though repository guidance describes explicit opt-in.
- Windows/Linux may fall back to PATH-selected tools; the Linux portable launcher prepends its toolchain but inherits the remaining PATH.
- The Windows standalone toolchain downloader and Android package-provisioning source omit independent package authenticity checks.
- The standard Android build exports signing material from the same selected keystore used by Gradle; no external official APK provenance is present in the repository.
- Large native source copies and checked-in binaries/documents could only receive focused static review; prebuilt binaries were not reverse engineered.
- Generated runner code is sandboxed by Android, but same-package signed updates inherit package state and persisted grants after user installation.
- CMake, Cargo, Make, and project code execution during an explicitly started build is expected product behavior; findings focus on broken surrounding controls.

## Findings

| Finding | Severity | Confidence | Detailed write-up |
| --- | --- | --- | --- |
| [Release signing private key is exported into the distributed Android IDE](#finding-1) | critical | high | inline below |
| [Simulated VRAM APIs free and write arbitrary caller-provided addresses](#finding-2) | high | high | inline below |
| [Mycel checkpoint loader accepts attacker-controlled array bounds and indices](#finding-3) | high | high | inline below |
| [QASM gate-token parsing overflows a fixed stack buffer](#finding-4) | high | high | inline below |
| [Negative feedback count writes before the Mycel heap allocation](#finding-5) | high | high | inline below |
| [GitHub ZIP fallback permits traversal after backslash normalization](#finding-6) | high | high | inline below |
| [Agent source-map and symlink fallbacks escape project containment](#finding-7) | high | high | inline below |
| [Mycel dimensions overflow allocation sizes before initialization writes](#finding-8) | medium | high | inline below |
| [Oversized qubit counts wrap state dimensions before CPU gate indexing](#finding-9) | medium | high | inline below |
| [Windows and Linux online-package opt-outs are ignored by build engines](#finding-10) | medium | high | inline below |
| [Agent credentials and project context can be sent over cleartext HTTP](#finding-11) | medium | high | inline below |
| [Project-controlled build-directory symlinks can delete files outside the project](#finding-12) | medium | high | inline below |
| [Runner packaging follows project symlinks and bundles out-of-project files](#finding-13) | medium | high | inline below |
| [Agent mutation approvals omit most bytes that are subsequently committed](#finding-14) | medium | high | inline below |
| [Toolchain provisioning consumes executable packages without authenticated integrity](#finding-15) | medium | high | inline below |
| [Android SAF agent workspace retains files from previously opened projects](#finding-16) | medium | high | inline below |
| [CTC target length can overflow the dynamic-programming width](#finding-17) | low | high | inline below |
| [Android enables online dependency retrieval before explicit opt-in](#finding-18) | low | high | inline below |

### Confidence Scale

| Label | Meaning |
| --- | --- |
| high | Direct evidence supports the finding with no material unresolved blocker. |
| medium | Evidence supports a plausible issue, but material runtime or reachability proof remains. |
| low | Evidence is incomplete and the item is retained only for explicit follow-up. |

<a id="finding-1"></a>

### [1] Release signing private key is exported into the distributed Android IDE

| Field | Value |
| --- | --- |
| Severity | critical |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | cryptographic key exposure |
| CWE | CWE-321, CWE-312 |
| Affected lines | android/scripts/prepare-signing.ps1:13, android/scripts/prepare-signing.ps1:41-53, android/scripts/ExportSigningMaterial.java:34-35, android/scripts/build-apk.ps1:26-31, android/app/build.gradle:3-38, android/app/src/main/java/kr/compile/RunnerPackager.java:58-67 |

#### Summary

The standard Android release workflow exports the same keystore key used to sign the IDE as an unencrypted PKCS#8 asset, and RunnerPackager consumes that asset to sign every fixed-identity runner APK. Any recipient of an APK built through this workflow can recover an application update-signing key.

#### Root Cause

One keystore is used both as the IDE release identity and as runtime runner-signing material, and its raw private key is deliberately copied into the IDE asset source set.

#### Validation

Validated the same-key flow from build-apk.ps1 through prepare-signing.ps1 and ExportSigningMaterial into Gradle assets, then to RunnerPackager's PKCS8EncodedKeySpec and APK signer. No build exclusion removes the generated asset.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- build-apk.ps1 invokes prepare-signing.ps1 before :app:assembleRelease.
- prepare-signing.ps1 selects KR_SIGNING_STORE and exports kr-runner-key.pk8 into app/src/main/assets.
- ExportSigningMaterial writes key.getEncoded(), and RunnerPackager reconstructs a usable PrivateKey from the packaged bytes.
- app/build.gradle signs release builds from the same KR_SIGNING_\* values; runner/build.gradle fixes applicationId to kr.compile.runner.

Counterevidence and remaining uncertainty:
- No private key or built APK is tracked at the frozen revision, and independently built forks may use different development keys.
- The repository does not prove which signer an externally published APK used.
- Installation is not silent: Android still requires the victim to permit and confirm installation.

#### Dataflow

The canonical finding records the affected path at android/scripts/prepare-signing.ps1:13, android/scripts/prepare-signing.ps1:41-53, android/scripts/ExportSigningMaterial.java:34-35, android/scripts/build-apk.ps1:26-31, android/app/build.gradle:3-38, android/app/src/main/java/kr/compile/RunnerPackager.java:58-67, but no expanded source-to-sink narrative was recorded.

- **Source:** Ordinary inspection of app/src/main/assets content packaged in the IDE APK

- **Sink:** Android package update signature verification

- **Outcome:** Forged trusted updates execute under the existing package identity with its private state and persisted grants

Transformations:
- Extract kr-runner-key.pk8 and its certificate
- Build a higher-version APK with package kr.compile or kr.compile.runner
- Sign it with the recovered key

#### Reachability

The build workflow deterministically embeds the selected private key; no runtime secret or privileged filesystem access is needed to extract an APK asset.

- **Attacker:** Any recipient of an Android IDE APK produced by the standard workflow

- **Entry point:** Ordinary inspection of app/src/main/assets content packaged in the IDE APK

- **Sink:** Android package update signature verification

- **Outcome:** Forged trusted updates execute under the existing package identity with its private state and persisted grants

Preconditions:
- A victim installs an APK built by the standard workflow
- The victim approves the attacker's update installation

Existing controls:
- No private key or built APK is tracked at the frozen revision, and independently built forks may use different development keys.
- The repository does not prove which signer an externally published APK used.
- Installation is not silent: Android still requires the victim to permit and confirm installation.

#### Severity

**Critical** — A distributed artifact produced by the standard workflow discloses its own signing authority. A recipient can forge same-signer, higher-version IDE or runner updates and, after Android's user-mediated installation step, inherit the target package's private data and persisted SAF grants.

Reduce to high if release packaging is changed to prove the IDE signer is never exported while a separate embedded runner key remains; remove the finding only when no reusable private key is packaged.

Impact assessment:
- **Level:** critical
- **Rationale:** Compromise of an Android application signing identity permits durable arbitrary code execution in the updated application identity and access to package-private state.

Likelihood assessment:
- **Level:** high
- **Rationale:** Asset extraction and APK signing are routine, and the vulnerable standard build path is explicit; only user-mediated installation remains.

#### Remediation

Never package an application-signing private key. Separate the IDE release identity from runner signing, use non-exportable per-installation Android Keystore keys and preferably per-project runner package identities, and rotate any IDE or runner signing identities already shipped by this workflow.

Tests:
- Build a release and assert no private-key asset or PKCS#8 material exists in the APK.
- Assert the IDE release certificate differs from all runner certificates.
- Verify runner signing succeeds with a non-exportable per-installation key and that generated runners use isolated identities.

Preventive controls:
- Build-time secret scanning of APK contents
- Release-key separation policy
- Signer identity regression tests

<a id="finding-2"></a>

### [2] Simulated VRAM APIs free and write arbitrary caller-provided addresses

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | arbitrary native memory write/free |
| CWE | CWE-123, CWE-763 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:17787-17799, windows/components/cc-opencl/src/CC_OpenCL.c:17787-17799, linux/components/cc-opencl/src/CC_OpenCL.c:17787-17799 |

#### Summary

The public simulated_vram_free and simulated_vram_write APIs cast integer addresses supplied by the caller directly to host pointers, then call free or memcpy without allocation ownership or range validation.

#### Root Cause

Host pointer values are exposed as integers and later trusted without an allocation registry, exact-base validation, lifetime tracking, or checked offset/size bounds.

#### Validation

free ignores its size argument and writes check only nonzero address/size/source before memcpy. Cooperative use is safe but not enforced by the public boundary.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The documented API exposes allocate/free/write operations.
- free casts the caller integer and invokes free.
- write casts the caller integer and copies caller-controlled size bytes.

Counterevidence and remaining uncertainty:
- Null and zero-length operations are ignored.
- A cooperative caller can pass only exact live addresses returned by allocate.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:17787-17799, windows/components/cc-opencl/src/CC_OpenCL.c:17787-17799, linux/components/cc-opencl/src/CC_OpenCL.c:17787-17799, but no expanded source-to-sink narrative was recorded.

- **Source:** simulated_vram_free(address, size) or simulated_vram_write(address, source, size)

- **Sink:** free(pointer) or memcpy(pointer, source, size)

- **Outcome:** Arbitrary invalid free or memory overwrite in the native process

Transformations:
- integer converted to host pointer

#### Reachability

No registry or bounds check separates the exported input from the host-memory sink.

- **Attacker:** A public-library caller able to invoke simulated VRAM exports

- **Entry point:** simulated_vram_free(address, size) or simulated_vram_write(address, source, size)

- **Sink:** free(pointer) or memcpy(pointer, source, size)

- **Outcome:** Arbitrary invalid free or memory overwrite in the native process

Preconditions:
- The attacker can call the exported library API

Existing controls:
- Null and zero-length operations are ignored.
- A cooperative caller can pass only exact live addresses returned by allocate.

#### Severity

**High** — A public API caller can convert chosen integers into native free/write targets, crossing the intended opaque-allocation boundary and directly corrupting process memory.

Reduce to medium if the API is provably available only to fully trusted in-process code; remove when callers receive opaque handles validated against an allocation table.

Impact assessment:
- **Level:** high
- **Rationale:** Direct memory corruption can crash the process or enable code execution.

Likelihood assessment:
- **Level:** high
- **Rationale:** The primitives accept simple chosen values and need no race or malformed large object.

#### Remediation

Return opaque allocation IDs, keep a synchronized allocation table with base, size, and lifetime, and reject unknown, interior, freed, or overflowing ranges.

Tests:
- Attempt unknown, interior, freed, and overflowing handles under ASan.
- Property-test allocation-table lifetime and range validation.

Preventive controls:
- Opaque native handles
- Checked range arithmetic
- Allocation lifetime registry

<a id="finding-3"></a>

### [3] Mycel checkpoint loader accepts attacker-controlled array bounds and indices

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | unsafe deserialization into native array state |
| CWE | CWE-20, CWE-129 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:10568-10631, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:9788-9824, windows/components/cc-opencl/src/CC_OpenCL.c:10568-10631, linux/components/cc-opencl/src/CC_OpenCL.c:10568-10631 |

#### Summary

load_mycel_state validates magic and version but commits checkpoint-controlled T_act, free_head, dimensions, and free-list entries without enforcing their relationships to T_cap. Later update and reproduction operations use those fields as loop bounds and indices.

#### Root Cause

Serialized structural invariants are not checked before attacker values become live loop bounds and heap indices; payload fread results are also ignored.

#### Validation

For T_cap=1 and T_act=2, a subsequent feedback/update loop reaches element 1 beyond one-element arrays. Invalid free_head/free_list values provide independent out-of-bounds paths.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- Only checkpoint magic and version are checked.
- T_act and free_head are assigned without comparison to T_cap.
- Free-list entries are read without range validation.
- Subsequent public operations index arrays with restored fields.

Counterevidence and remaining uncertainty:
- Invalid magic/version is rejected.
- mycel_initialize rejects non-positive dimensions.
- The safe setter for active count clamps, but the loader bypasses it.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:10568-10631, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:9788-9824, windows/components/cc-opencl/src/CC_OpenCL.c:10568-10631, linux/components/cc-opencl/src/CC_OpenCL.c:10568-10631, but no expanded source-to-sink narrative was recorded.

- **Source:** load_mycel_state(path)

- **Sink:** Later update/reproduction loops and free-list indexing

- **Outcome:** Native heap out-of-bounds reads/writes, crash, and potential code execution

Transformations:
- header parsed
- arrays allocated from T_cap
- untrusted counters and lists installed into live state

#### Reachability

The checkpoint path is a documented public API; no authenticity or complete-state validation intervenes.

- **Attacker:** A caller or file supplier controlling a Mycel checkpoint passed to the exported loader

- **Entry point:** load_mycel_state(path)

- **Sink:** Later update/reproduction loops and free-list indexing

- **Outcome:** Native heap out-of-bounds reads/writes, crash, and potential code execution

Preconditions:
- The application loads the crafted checkpoint and invokes a normal state-processing API afterward

Existing controls:
- Invalid magic/version is rejected.
- mycel_initialize rejects non-positive dimensions.
- The safe setter for active count clamps, but the loader bypasses it.

#### Severity

**High** — A small crafted checkpoint reaches a public loader and causes repeatable out-of-bounds native reads and writes during normal subsequent API operations.

Reduce to medium if checkpoint files are strongly authenticated and isolated from user content; remove after complete temporary parsing and invariant validation.

Impact assessment:
- **Level:** high
- **Rationale:** Serialized input corrupts native heap memory.

Likelihood assessment:
- **Level:** high
- **Rationale:** Small internally inconsistent fields are sufficient; no huge allocation is required.

#### Remediation

Parse into temporary storage, validate all dimensions, counters, indices, lengths, and every fread result, then atomically commit only a complete valid state.

Tests:
- Fuzz checkpoint headers and truncated payloads under ASan/UBSan.
- Assert T_act/free_head \<= T_cap and every list index is in range before commit.

Preventive controls:
- Versioned serialization schema validation
- Atomic temporary-state loading
- Native parser fuzzing

<a id="finding-4"></a>

### [4] QASM gate-token parsing overflows a fixed stack buffer

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | native stack memory corruption |
| CWE | CWE-121 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:12904-12913, windows/components/cc-opencl/src/CC_OpenCL.c:12904-12913, linux/components/cc-opencl/src/CC_OpenCL.c:12904-12913 |

#### Summary

quantum_import_from_qasm reads a line of up to 511 bytes into a bounded line buffer but copies the leading gate token into gate_token\[64\] without bounding the index, then writes a terminator out of bounds.

#### Root Cause

The parser bounds the outer line but not the destination-specific token index before copying attacker-controlled bytes into a 64-byte stack array.

#### Validation

A non-whitespace token of 64 or more characters reaches the copy loop and its trailing NUL write; max_gates and later bounded parameter copies do not protect gate_token.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The exported function opens a caller-selected QASM path and uses fgets with a 512-byte line buffer.
- gate_token is 64 bytes while idx increments until whitespace or '(' without a sizeof check.
- The same source sequence exists in all three shipped platform copies.

Counterevidence and remaining uncertainty:
- The outer line buffer is bounded.
- Later parameter and argument buffers use bounded copies.
- The output gate count is separately bounded.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:12904-12913, windows/components/cc-opencl/src/CC_OpenCL.c:12904-12913, linux/components/cc-opencl/src/CC_OpenCL.c:12904-12913, but no expanded source-to-sink narrative was recorded.

- **Source:** quantum_import_from_qasm(filepath, ...)

- **Sink:** gate_token\[idx++\] and the trailing gate_token\[idx\] NUL write

- **Outcome:** Native stack corruption, process crash, and potential code execution

Transformations:
- fgets accepts the crafted line
- the tokenizer scans the leading mnemonic

#### Reachability

The parser is exported and documented; no authentication or semantic validation precedes the unsafe copy.

- **Attacker:** A caller or file supplier controlling a QASM document consumed by the public CC_OpenCL API

- **Entry point:** quantum_import_from_qasm(filepath, ...)

- **Sink:** gate_token\[idx++\] and the trailing gate_token\[idx\] NUL write

- **Outcome:** Native stack corruption, process crash, and potential code execution

Preconditions:
- The consuming application invokes the public import API on the attacker's file

Existing controls:
- The outer line buffer is bounded.
- Later parameter and argument buffers use bounded copies.
- The output gate count is separately bounded.

#### Severity

**High** — A short crafted QASM file reaches a public exported parser and produces controlled native stack corruption on Android, Windows, and Linux.

Reduce to medium if every consumer can prove QASM is trusted and the parser is isolated in a disposable sandbox; remove after length-aware tokenization rejects tokens at the boundary.

Impact assessment:
- **Level:** high
- **Rationale:** Memory corruption occurs inside the consuming native process.

Likelihood assessment:
- **Level:** high
- **Rationale:** A 64-byte mnemonic is trivial to construct and needs no resource exhaustion or race.

#### Remediation

Use a length-aware tokenizer, reject overlong complete tokens before copying, and apply the fix to all platform copies.

Tests:
- Run ASan/UBSan tests for 63-, 64-, 65-, and 511-byte gate tokens.
- Fuzz the complete QASM import function.

Preventive controls:
- Centralized bounded parsing helpers
- Native parser fuzzing in CI

<a id="finding-5"></a>

### [5] Negative feedback count writes before the Mycel heap allocation

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | native heap out-of-bounds write |
| CWE | CWE-129 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:9848-9863, windows/components/cc-opencl/src/CC_OpenCL.c:9848-9863, linux/components/cc-opencl/src/CC_OpenCL.c:9848-9863 |

#### Summary

step_subqg_feedback clamps count only when it exceeds C. A negative count becomes the initial index of a clearing loop and writes before kappa_mood.

#### Root Cause

The function enforces only count \<= C, then uses the still-negative signed count as an array index in a write loop.

#### Validation

count=-1 skips the positive memcpy and immediately writes kappa_mood\[-1\]; more-negative inputs underwrite more elements.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- kappa_mood is allocated for exactly C floats.
- Only the upper bound is clamped.
- The clearing loop starts at attacker-controlled count.

Counterevidence and remaining uncertainty:
- The function requires initialized Mycel state.
- Positive oversized counts are safely clamped.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:9848-9863, windows/components/cc-opencl/src/CC_OpenCL.c:9848-9863, linux/components/cc-opencl/src/CC_OpenCL.c:9848-9863, but no expanded source-to-sink narrative was recorded.

- **Source:** step_subqg_feedback(..., count)

- **Sink:** st-\>kappa_mood\[i\] = 0.0f for negative i

- **Outcome:** Heap corruption, crash, and potential native code execution

Transformations:
- upper-only clamp
- negative value retained as loop initializer

#### Reachability

The exported API accepts the signed count directly and contains no lower-bound guard.

- **Attacker:** A public CC_OpenCL library caller

- **Entry point:** step_subqg_feedback(..., count)

- **Sink:** st-\>kappa_mood\[i\] = 0.0f for negative i

- **Outcome:** Heap corruption, crash, and potential native code execution

Preconditions:
- Mycel state has been initialized

Existing controls:
- The function requires initialized Mycel state.
- Positive oversized counts are safely clamped.

#### Severity

**High** — A public-library caller can trigger direct heap underwrite with a single negative integer after ordinary initialization.

Reduce to medium only if the API is unreachable from untrusted or extension-controlled callers; remove after validating the lower bound before indexing.

Impact assessment:
- **Level:** high
- **Rationale:** Attacker-controlled out-of-bounds heap writes occur in the native process.

Likelihood assessment:
- **Level:** high
- **Rationale:** A single negative integer deterministically reaches the sink.

#### Remediation

Reject count \< 0 before any loop or conversion, then add lower/upper bound assertions at the internal array boundary.

Tests:
- ASan-test count values INT_MIN, -2, -1, 0, C, and C+1 on all source copies.

Preventive controls:
- Signed-input validation
- Sanitizer-backed native API tests

<a id="finding-6"></a>

### [6] GitHub ZIP fallback permits traversal after backslash normalization

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | archive extraction path traversal |
| CWE | CWE-22 |
| Affected lines | android/app/src/main/java/kr/compile/GitImporter.java:175-200 |

#### Summary

GitImporter converts repository-controlled backslashes to forward slashes and writes the resulting relative entry under the destination without rejecting parent components or checking canonical containment.

#### Root Cause

The extractor introduces path separators by normalizing backslashes but never rejects '..', absolute forms, or a canonical destination outside destDir.

#### Validation

The fallback is reachable when the bundled git binary is absent or clone fails. An entry such as repository-root/..\\..\\usr\\bin\\clang++ becomes a traversing path after root-prefix stripping.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The URL restriction authenticates the GitHub host, not repository-controlled filenames.
- entry.getName().replace('\\\\','/') changes attacker bytes into separators.
- new File(destDir, relName) is written without canonical containment.
- Toolchain readiness relies on stamps and expected files that can be planted outside the project.

Counterevidence and remaining uncertainty:
- A successful native git clone avoids this extractor.
- HTTPS prevents opportunistic transit modification but not malicious repository contents.

#### Dataflow

The canonical finding records the affected path at android/app/src/main/java/kr/compile/GitImporter.java:175-200, but no expanded source-to-sink narrative was recorded.

- **Source:** A crafted branch ZIP entry name containing backslashes and parent components

- **Sink:** FileOutputStream on the unchecked output File

- **Outcome:** Overwrite or creation of app-writable files outside the imported project and possible later toolchain execution

Transformations:
- Backslashes become slashes
- the archive root prefix is stripped
- relName is joined to destDir

#### Reachability

Fresh installations may reach the fallback before a toolchain git exists; the vulnerable path performs no final containment check.

- **Attacker:** Owner of a GitHub repository selected for import

- **Entry point:** A crafted branch ZIP entry name containing backslashes and parent components

- **Sink:** FileOutputStream on the unchecked output File

- **Outcome:** Overwrite or creation of app-writable files outside the imported project and possible later toolchain execution

Preconditions:
- The Android user imports the attacker's repository
- native git is unavailable or clone fails

Existing controls:
- A successful native git clone avoids this extractor.
- HTTPS prevents opportunistic transit modification but not malicious repository contents.

#### Severity

**High** — A repository owner can use a legal backslash-containing Git filename to escape the new project during the common no-git fallback and plant or overwrite app-private files, including toolchain components later executed by the IDE.

Reduce to medium if the ZIP fallback is disabled or extracted into an OS sandbox with no sensitive writable neighbors; remove after post-normalization component and canonical-root enforcement.

Impact assessment:
- **Level:** high
- **Rationale:** Arbitrary app-private file overwrite can corrupt projects, poison configuration, or place executable toolchain bytes.

Likelihood assessment:
- **Level:** high
- **Rationale:** The filename is repository-controlled and the fallback is an intentional supported path.

#### Remediation

After separator normalization, reject absolute, drive-qualified, empty, dot, and dot-dot components and require every normalized/canonical output to remain below the canonical destination. Clean partial extraction on failure.

Tests:
- Import ZIPs with Unix backslashes, '..', absolute, drive, and mixed-separator names and assert no outside file changes.
- Test file and directory entries plus fallback error cleanup.

Preventive controls:
- Shared safe archive extractor
- Canonical containment assertion
- Archive size and entry limits

<a id="finding-7"></a>

### [7] Agent source-map and symlink fallbacks escape project containment

| Field | Value |
| --- | --- |
| Severity | high |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | workspace path confinement bypass |
| CWE | CWE-22, CWE-59 |
| Affected lines | android/app/src/main/java/kr/compile/KrSourceMap.java:217-258, android/app/src/main/java/kr/compile/KrAgentCore.java:835-910, android/app/src/main/java/kr/compile/KrAgentCore.java:1101-1165, windows/src/kr/compile/KrAgentCore.java:755-830, windows/src/kr/compile/KrAgentCore.java:1013-1077, linux/src/kr/compile/KrAgentCore.java:755-830 |

#### Summary

Direct model-selected paths pass through safeFile, but source-map resolution and recursive basename fallback return Files without a final canonical-root check. A malicious project can redirect approval-free reads to text files outside the active project, which are then sent to the model endpoint.

#### Root Cause

Alternate path representations are trusted after direct safeFile validation fails: source_map originalFile retains parent traversal and recursive File APIs follow links, with no canonical containment check immediately before reading.

#### Validation

Both bypasses were traced to Files.readAllLines and then into conversation history. Direct paths remain protected, which narrows rather than defeats the finding.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- KrSourceMap.normalize removes prefixes but does not reject '..', and findSourceForGeneratedFile checks only File.isFile().
- read() substitutes the mapped File and reads it without safeFile.
- findFileInProject recursively uses isDirectory/listFiles/isFile, follows links, and returns the original File without canonical revalidation.
- Tool output is appended to model history and sent on the next request.

Counterevidence and remaining uncertainty:
- Direct agent paths are canonicalized and .git/.kragent are blocked.
- Reads are size-limited and binary extensions are filtered.
- Write and build actions require approval, but reads do not.

#### Dataflow

The canonical finding records the affected path at android/app/src/main/java/kr/compile/KrSourceMap.java:217-258, android/app/src/main/java/kr/compile/KrAgentCore.java:835-910, android/app/src/main/java/kr/compile/KrAgentCore.java:1101-1165, windows/src/kr/compile/KrAgentCore.java:755-830, windows/src/kr/compile/KrAgentCore.java:1013-1077, linux/src/kr/compile/KrAgentCore.java:755-830, but no expanded source-to-sink narrative was recorded.

- **Source:** A project-controlled KR_BUILD/source_map.json entry or filesystem symlink plus a model read action

- **Sink:** HTTP request body sent by askModel

- **Outcome:** Disclosure of readable files outside the active project and possible external overwrite through a symlinked parent during replace

Transformations:
- Alternate source resolution or recursive basename matching
- unvalidated File substitution
- text read appended to conversation

#### Reachability

The product automatically trusts the alternate resolver after safeFile rejection; native Git clone can preserve repository symlinks.

- **Attacker:** A malicious project author, potentially assisted by an untrusted configured model endpoint

- **Entry point:** A project-controlled KR_BUILD/source_map.json entry or filesystem symlink plus a model read action

- **Sink:** HTTP request body sent by askModel

- **Outcome:** Disclosure of readable files outside the active project and possible external overwrite through a symlinked parent during replace

Preconditions:
- The user opens a project containing the malicious mapping or link
- The agent is run and requests the alias

Existing controls:
- Direct agent paths are canonicalized and .git/.kragent are blocked.
- Reads are size-limited and binary extensions are filtered.
- Write and build actions require approval, but reads do not.

#### Severity

**High** — A malicious project plus an ordinary agent read can disclose user-readable credentials or source outside the project across Android, Windows, and Linux, without a read approval.

Reduce to medium if agent reads require explicit full-path approval or the endpoint is fully trusted; remove when every alternate resolver is canonicalized and symlink-safe at the final sink.

Impact assessment:
- **Level:** high
- **Rationale:** Credentials, private keys, configuration, and other projects' source can be exfiltrated to the model endpoint.

Likelihood assessment:
- **Level:** high
- **Rationale:** The input is a normal project artifact and agent reads need no additional approval.

#### Remediation

Route every mapped, searched, and diagnostic path through one final canonical containment function; reject absolute and parent paths, traverse with NOFOLLOW_LINKS, skip symlinks, and revalidate immediately before every read or write.

Tests:
- Create source maps with '..' and absolute paths and assert rejection.
- Create file and directory symlinks escaping the project and assert list/read/replace cannot follow them.
- Repeat on Android SAF and desktop roots.

Preventive controls:
- Single project-path resolver
- Link-safe filesystem traversal
- Approval bound to canonical paths

<a id="finding-8"></a>

### [8] Mycel dimensions overflow allocation sizes before initialization writes

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | integer overflow in native allocation sizing |
| CWE | CWE-190 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:1869-1908, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:9099, windows/components/cc-opencl/src/CC_OpenCL.c:1869-1908, linux/components/cc-opencl/src/CC_OpenCL.c:1869-1908 |

#### Summary

mycel_initialize multiplies attacker-controlled positive T_cap, C, and K without checked arithmetic, passes potentially wrapped byte counts to malloc, and writes pheromone entries before checking that allocation.

#### Root Cause

Element and byte counts are computed with unchecked multiplication, and the first allocation is used before the collective allocation-success check.

#### Validation

On 64-bit targets, large positive factors can wrap the byte multiplication to a small allocation while the initialization loop retains a much larger element count.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The public initializer checks only dimensions \> 0.
- pheromone_count and byte size use unchecked products.
- The pheromone loop precedes the allocation null check.
- Checkpoint loading also forwards file dimensions to the initializer.

Counterevidence and remaining uncertainty:
- Zero and negative dimensions are rejected.
- An initialized compute context is required.
- Practical callers usually choose much smaller dimensions.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:1869-1908, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:9099, windows/components/cc-opencl/src/CC_OpenCL.c:1869-1908, linux/components/cc-opencl/src/CC_OpenCL.c:1869-1908, but no expanded source-to-sink narrative was recorded.

- **Source:** subqg_init_mycel or load_mycel_state

- **Sink:** writes through st-\>pheromone before allocation validation

- **Outcome:** Heap overflow or NULL dereference

Transformations:
- positive dimensions multiplied in size_t
- byte count wraps or allocation fails
- initialization loop uses unchecked element count

#### Reachability

No checked multiplication or practical dimension limit intervenes.

- **Attacker:** A public-library caller or crafted checkpoint supplier controlling Mycel dimensions

- **Entry point:** subqg_init_mycel or load_mycel_state

- **Sink:** writes through st-\>pheromone before allocation validation

- **Outcome:** Heap overflow or NULL dereference

Preconditions:
- Large dimensions reach a supported initialized context

Existing controls:
- Zero and negative dimensions are rejected.
- An initialized compute context is required.
- Practical callers usually choose much smaller dimensions.

#### Severity

**Medium** — The path can corrupt heap memory or dereference NULL, but exploitation requires very large dimensions and sufficient initialization context.

Raise to high if a small platform-specific dimension tuple is shown to deterministically produce exploitable corruption; reduce to low if strict public dimension caps precede all calls.

Impact assessment:
- **Level:** high
- **Rationale:** Native memory corruption or deterministic process termination.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires extreme dimensions and platform-specific allocation behavior.

#### Remediation

Use checked multiplication for every element and byte count, enforce practical per-dimension and total-memory limits, and validate each allocation before any write.

Tests:
- Exercise overflow boundary tuples under ASan/UBSan.
- Fuzz checkpoint dimensions and assert early rejection before allocation.

Preventive controls:
- Checked arithmetic library
- Resource ceilings
- Immediate allocation checks

<a id="finding-9"></a>

### [9] Oversized qubit counts wrap state dimensions before CPU gate indexing

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | unchecked shift causing native bounds mismatch |
| CWE | CWE-1335 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:12704-12733, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:13789-13806, windows/components/cc-opencl/src/CC_OpenCL.c:13789-13806, linux/components/cc-opencl/src/CC_OpenCL.c:13789-13806 |

#### Summary

Quantum APIs compute (size_t)1 \<\< num_qubits without rejecting shifts at or above the size_t width, allocate using the resulting undefined/wrapped dimension, then compose gate indices from the original larger qubit count.

#### Root Cause

The public path validates positivity but not the machine-width/resource upper bound before shifting, and later validates targets against num_qubits rather than the actual allocated dimension.

#### Validation

On typical 64-bit builds num_qubits=64 invokes undefined shifting and can produce a tiny dimension while a target such as qubit 1 still passes and composes index 2.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The state dimension is computed with an unchecked shift in multiple functions.
- host_state is allocated from that dimension.
- Gate targets are caller-controlled and composed indices lack a final dimension check.
- A sibling API explicitly rejects shifts at the type width, showing the intended control.

Counterevidence and remaining uncertainty:
- Non-positive counts are rejected.
- The precise excessive-shift result is compiler-dependent.
- Practical quantum workloads normally use far smaller dimensions.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:12704-12733, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:13789-13806, windows/components/cc-opencl/src/CC_OpenCL.c:13789-13806, linux/components/cc-opencl/src/CC_OpenCL.c:13789-13806, but no expanded source-to-sink narrative was recorded.

- **Source:** quantum_apply_gate_sequence with excessive num_qubits

- **Sink:** host_state access at an index beyond the allocated dimension

- **Outcome:** Native heap out-of-bounds read/write

Transformations:
- unchecked power-of-two shift
- small/wrapped state allocation
- gate target composed from original count

#### Reachability

The exported call contains no width guard before allocation or CPU application.

- **Attacker:** A public-library caller controlling num_qubits and uploaded gate descriptors

- **Entry point:** quantum_apply_gate_sequence with excessive num_qubits

- **Sink:** host_state access at an index beyond the allocated dimension

- **Outcome:** Native heap out-of-bounds read/write

Preconditions:
- A compiler/target exhibits a corrupting result for the undefined shift

Existing controls:
- Non-positive counts are rejected.
- The precise excessive-shift result is compiler-dependent.
- Practical quantum workloads normally use far smaller dimensions.

#### Severity

**Medium** — The path can cause native heap corruption, but the exact result of excessive C shifting is compiler-dependent and requires an unusually large caller-supplied qubit count.

Raise to high on a supported build where a concrete num_qubits value deterministically yields a small allocation and reachable out-of-bounds write; remove after centralized shift and resource validation.

Impact assessment:
- **Level:** high
- **Rationale:** Potential heap corruption in the native process.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires abnormal dimensions and is compiler-dependent.

#### Remediation

Reject num_qubits at or above the size_t bit width and impose a practical memory cap; use a checked power-of-two helper and validate every composed index against state-\>dimension.

Tests:
- Test boundary counts around the machine width under ASan/UBSan.
- Property-test every gate-composed index against the allocated dimension.

Preventive controls:
- Central checked dimension helper
- Practical allocation ceilings
- Sanitizer tests

<a id="finding-10"></a>

### [10] Windows and Linux online-package opt-outs are ignored by build engines

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | ineffective security control |
| CWE | CWE-693 |
| Affected lines | windows/src/kr/compile/BuildManager.java:23, windows/src/kr/compile/CompilerEngine.java:27-35, windows/src/kr/compile/CompilerEngine.java:1027, windows/src/kr/compile/CompilerEngine.java:1216, linux/src/kr/compile/CompilerEngine.java:27-35, linux/src/kr/compile/CompilerEngine.java:1257 |

#### Summary

Both desktop UIs pass an unchecked-by-default onlineDependencies value into CompilerEngine, but the duplicated Windows and Linux engines assign the field and never read it; CMake, Cargo, and Go retain network-capable defaults.

#### Root Cause

The enforcement boolean is lost at the engine boundary; downstream tool invocations never consume it.

#### Validation

Repository searches find onlineDependencies in each desktop engine only at declaration, parameter, and assignment, while the network-capable build commands omit offline controls.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The checkbox defaults off and reaches CompilerEngine.
- No later read of the field occurs.
- CMake, Cargo, and Go launch without the corresponding offline settings used by Android.

Counterevidence and remaining uncertainty:
- A user must start a build.
- The UI label mentions vcpkg, which may narrow some expectations.
- Package-manager flags alone cannot sandbox arbitrary malicious CMake or Make logic.

#### Dataflow

The canonical finding records the affected path at windows/src/kr/compile/BuildManager.java:23, windows/src/kr/compile/CompilerEngine.java:27-35, windows/src/kr/compile/CompilerEngine.java:1027, windows/src/kr/compile/CompilerEngine.java:1216, linux/src/kr/compile/CompilerEngine.java:27-35, linux/src/kr/compile/CompilerEngine.java:1257, but no expanded source-to-sink narrative was recorded.

- **Source:** A CMake, Cargo, or Go project built while online packages is disabled

- **Sink:** Network-capable build tool and dependency build logic

- **Outcome:** Unexpected network access, dependency substitution, and execution during an ostensibly offline build

Transformations:
- UI value passed then discarded
- build tool launched with default network behavior
- remote dependency resolved

#### Reachability

The source demonstrates no enforcement branch after constructor assignment.

- **Attacker:** A project author or compromised dependency endpoint

- **Entry point:** A CMake, Cargo, or Go project built while online packages is disabled

- **Sink:** Network-capable build tool and dependency build logic

- **Outcome:** Unexpected network access, dependency substitution, and execution during an ostensibly offline build

Preconditions:
- The user starts a dependency-capable build with the control off

Existing controls:
- A user must start a build.
- The UI label mentions vcpkg, which may narrow some expectations.
- Package-manager flags alone cannot sandbox arbitrary malicious CMake or Make logic.

#### Severity

**Medium** — A project or later-compromised dependency can contact remote services and execute dependency build logic despite an explicit offline choice, though the user must start a build and malicious build scripts already carry substantial authority.

Reduce to low if the UI is explicitly relabeled as non-security-affecting; remove when cooperative package-manager flags and, where promised, OS network isolation enforce the choice.

Impact assessment:
- **Level:** medium
- **Rationale:** The explicit trust choice is bypassed and remote code can enter a privileged build.

Likelihood assessment:
- **Level:** high
- **Rationale:** Every affected desktop build backend retains its default network behavior.

#### Remediation

Consume the flag in both engines: enforce CMake disconnected mode, Cargo offline mode, GOPROXY=off, and no Git/vcpkg retrieval. Use an OS network sandbox if the control promises protection from hostile build scripts.

Tests:
- Run each backend against a test dependency server with the option off and assert zero connections.
- Verify the same projects succeed only after explicit opt-in.

Preventive controls:
- Central build-network policy
- Hermetic dependency cache tests
- Optional OS network sandbox

<a id="finding-11"></a>

### [11] Agent credentials and project context can be sent over cleartext HTTP

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | cleartext transmission of credentials and source |
| CWE | CWE-319 |
| Affected lines | windows/src/kr/compile/KrAgentCore.java:472-503, linux/src/kr/compile/KrAgentCore.java:472-503, android/app/src/main/java/kr/compile/KrAgentCore.java:247-255, android/app/src/main/java/kr/compile/KrAgentCore.java:552-583, android/app/src/main/AndroidManifest.xml:10 |

#### Summary

Windows and Linux accept arbitrary HttpURLConnection endpoints without HTTPS enforcement. Android enables cleartext and uses string-prefix checks that can misclassify public hosts, while all variants attach bearer credentials and project conversation content.

#### Root Cause

Transport policy is not tied to a parsed and authenticated endpoint: desktop has no scheme guard, Android uses textual prefixes, and credentials are not withheld on cleartext connections.

#### Validation

askModel attaches Authorization and serialized history to the configured URL. Defaults are HTTPS, but source controls do not prevent the unsafe configuration.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- Desktop validation checks only nonempty endpoint text.
- HttpURLConnection sends the bearer header and message body.
- Android permits broad 10.\*, 172.\*, and 192.168.\* textual prefixes and enables cleartext globally.

Counterevidence and remaining uncertainty:
- The default endpoint is HTTPS.
- Android rejects most obvious remote HTTP URLs.
- Tokens are held only in session memory.
- HTTP may be intended for local model servers.

#### Dataflow

The canonical finding records the affected path at windows/src/kr/compile/KrAgentCore.java:472-503, linux/src/kr/compile/KrAgentCore.java:472-503, android/app/src/main/java/kr/compile/KrAgentCore.java:247-255, android/app/src/main/java/kr/compile/KrAgentCore.java:552-583, android/app/src/main/AndroidManifest.xml:10, but no expanded source-to-sink narrative was recorded.

- **Source:** A configured cleartext model endpoint

- **Sink:** Remote network peer receiving HTTP

- **Outcome:** Credential/source disclosure and model-response tampering

Transformations:
- URL accepted
- bearer header and conversation serialized
- request transmitted without TLS

#### Reachability

No cryptographic transport requirement intervenes after configuration.

- **Attacker:** A network-adjacent or on-path attacker

- **Entry point:** A configured cleartext model endpoint

- **Sink:** Remote network peer receiving HTTP

- **Outcome:** Credential/source disclosure and model-response tampering

Preconditions:
- The user configures HTTP or a misleading Android URL passes the prefix check

Existing controls:
- The default endpoint is HTTPS.
- Android rejects most obvious remote HTTP URLs.
- Tokens are held only in session memory.
- HTTP may be intended for local model servers.

#### Severity

**Medium** — An on-path attacker can steal tokens and source or alter model commands, but exploitation requires a user-configured HTTP endpoint or an Android endpoint that passes the permissive check.

Raise to high if HTTP is the documented/default deployment path on shared networks; reduce to low if cleartext is limited to tokenless loopback by parsed resolved address.

Impact assessment:
- **Level:** high
- **Rationale:** Bearer tokens and proprietary project context can be stolen; active attackers can return deceptive actions.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires non-default endpoint configuration and network position.

#### Remediation

Require HTTPS whenever a token is present and for every non-loopback endpoint; parse and resolve the URI, restrict any tokenless exception to loopback, and disable global cleartext where possible.

Tests:
- Reject userinfo, DNS-prefix, IPv6, redirect, and public-host HTTP bypasses.
- Assert Authorization is never attached to cleartext requests.

Preventive controls:
- Central transport-policy validator
- TLS-only credential transport
- Network security configuration

<a id="finding-12"></a>

### [12] Project-controlled build-directory symlinks can delete files outside the project

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | symlink-following destructive filesystem operation |
| CWE | CWE-59 |
| Affected lines | windows/src/kr/compile/CompilerEngine.java:726-734, windows/src/kr/compile/CompilerEngine.java:822-824, linux/src/kr/compile/CompilerEngine.java:761-769, linux/src/kr/compile/CompilerEngine.java:857-859 |

#### Summary

Windows and Linux CMake-for-Android builds recursively delete KR_BUILD/cmake_build_android using File APIs that follow directory symlinks. A project can preseed that path as a link to an external directory.

#### Root Cause

Deletion tests File.isDirectory and recurses through listFiles without NOFOLLOW_LINKS, a real-path boundary, or ownership validation.

#### Validation

The output directory is the project's KR_BUILD tree and the fixed child is deleted before CMake configuration, so a preexisting link is followed before the IDE recreates the directory.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- deleteDirectory recursively traverses every isDirectory result.
- buildCMake constructs outputDir/cmake_build_android and immediately deletes it.
- No canonical comparison or symbolic-link rejection occurs.

Counterevidence and remaining uncertainty:
- The user must start a build.
- Symlink creation may require privileges or configuration on some Windows systems.
- The fixed path limits which project entry must be controlled.

#### Dataflow

The canonical finding records the affected path at windows/src/kr/compile/CompilerEngine.java:726-734, windows/src/kr/compile/CompilerEngine.java:822-824, linux/src/kr/compile/CompilerEngine.java:761-769, linux/src/kr/compile/CompilerEngine.java:857-859, but no expanded source-to-sink narrative was recorded.

- **Source:** A symlink at KR_BUILD/cmake_build_android

- **Sink:** File.delete on files and directories reached outside the project

- **Outcome:** Recursive deletion of external user-writable data

Transformations:
- Build selects the Android CMake target
- deleteDirectory classifies the link target as a directory
- recursive traversal enumerates target children

#### Reachability

The destructive call occurs before CMake and without validating the build directory's real path.

- **Attacker:** Author of a local or cloned symlink-capable project

- **Entry point:** A symlink at KR_BUILD/cmake_build_android

- **Sink:** File.delete on files and directories reached outside the project

- **Outcome:** Recursive deletion of external user-writable data

Preconditions:
- The project representation preserves the directory symlink
- The user starts the affected build

Existing controls:
- The user must start a build.
- Symlink creation may require privileges or configuration on some Windows systems.
- The fixed path limits which project entry must be controlled.

#### Severity

**Medium** — The sink can recursively delete user-writable external data, but exploitation requires a symlink-capable local project and the user to approve/start the affected build target.

Raise to high where imported projects routinely preserve symlinks and builds run automatically; remove when deletion is link-safe and build roots are freshly created outside attacker-controlled trees.

Impact assessment:
- **Level:** high
- **Rationale:** Potential loss of arbitrary user-writable files selected by the link target.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires a platform/project combination that preserves symlinks and a build action.

#### Remediation

Place disposable build directories in trusted IDE-owned storage or delete with a NOFOLLOW_LINKS walk that rejects every link and verifies each real path remains below a trusted build root.

Tests:
- Preseed file and directory symlinks at the build path and assert the external target remains unchanged.
- Run deletion-race and junction tests on Windows.

Preventive controls:
- Trusted build-root ownership
- NOFOLLOW_LINKS recursive deletion
- Canonical boundary checks

<a id="finding-13"></a>

### [13] Runner packaging follows project symlinks and bundles out-of-project files

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | symlink-following artifact data exposure |
| CWE | CWE-59 |
| Affected lines | android/app/src/main/java/kr/compile/RunnerPackager.java:123-139, windows/src/kr/compile/CompilerEngine.java:650-663, linux/src/kr/compile/CompilerEngine.java:685-698 |

#### Summary

Android, Windows, and Linux project packagers recursively classify and read File entries without rejecting symbolic links or checking their real path against the project root. Linked external files are copied into kr-project-data.zip and then into a generated APK.

#### Root Cause

Packagers derive an in-project archive name from the link path but open the link target through ordinary FileInputStream without final real-path containment.

#### Validation

A file symlink isFile and is read; a directory symlink isDirectory and recursively traversed. The runner extractor later treats the archive entry as ordinary project data.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- All three packagers recurse with listFiles/isDirectory.
- Files are opened with FileInputStream.
- No symbolic-link test or canonical project-root check occurs before reading.

Counterevidence and remaining uncertainty:
- Ignored build and metadata directory names reduce incidental packaging.
- The user must create a runner artifact.
- Disclosure generally occurs only when the artifact is shared or inspected by another party.

#### Dataflow

The canonical finding records the affected path at android/app/src/main/java/kr/compile/RunnerPackager.java:123-139, windows/src/kr/compile/CompilerEngine.java:650-663, linux/src/kr/compile/CompilerEngine.java:685-698, but no expanded source-to-sink narrative was recorded.

- **Source:** A file or directory symlink inside the project

- **Sink:** assets/kr-project-data.zip in the runner APK

- **Outcome:** Out-of-project files are disclosed with the generated artifact

Transformations:
- recursive packaging follows the target
- external bytes assigned the link-relative archive name
- archive embedded in generated APK

#### Reachability

No real-path boundary is checked before the external bytes are read.

- **Attacker:** A malicious project author

- **Entry point:** A file or directory symlink inside the project

- **Sink:** assets/kr-project-data.zip in the runner APK

- **Outcome:** Out-of-project files are disclosed with the generated artifact

Preconditions:
- The project representation preserves the symlink
- The user builds and shares or otherwise exposes the runner APK

Existing controls:
- Ignored build and metadata directory names reduce incidental packaging.
- The user must create a runner artifact.
- Disclosure generally occurs only when the artifact is shared or inspected by another party.

#### Severity

**Medium** — The flaw can disclose arbitrary user-readable files in a shareable runner artifact, but requires a symlink-bearing project and the user to build and distribute or inspect that APK.

Raise to high if generated APKs are automatically uploaded; remove when packaging uses NOFOLLOW_LINKS and canonical-root checks for every entry.

Impact assessment:
- **Level:** high
- **Rationale:** Readable secrets or unrelated source can be packaged and disclosed.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires preserved symlinks plus artifact exposure.

#### Remediation

Walk project resources without following links, reject every symbolic-link component, and require each real file path to remain below the canonical project root before reading.

Tests:
- Package projects with file, directory, junction, and chained symlinks and assert rejection.
- Inspect resulting ZIPs to ensure only canonical in-root files appear.

Preventive controls:
- Link-safe artifact inventory
- Canonical read boundary
- Artifact content manifest

<a id="finding-14"></a>

### [14] Agent mutation approvals omit most bytes that are subsequently committed

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | misleading security approval |
| CWE | CWE-451 |
| Affected lines | android/app/src/main/java/kr/compile/KrAgentCore.java:373-460, android/app/src/main/java/kr/compile/KrAgentCore.java:1199-1205, windows/src/kr/compile/KrAgentCore.java:315-394, linux/src/kr/compile/KrAgentCore.java:315-394 |

#### Summary

Write and replace dialogs show at most 1,500 characters of each model-controlled field, but writeAtomically can commit payloads approaching 1 MiB; replace also transforms and globally applies fields not represented as a final diff.

#### Root Cause

Approval is bound to a truncated textual preview of inputs rather than the complete canonical path and exact resulting file bytes.

#### Validation

preview truncates at 1,500 characters while network/action and write limits are much larger; replace can affect all occurrences after transformations.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The preview helper truncates to 1,500 characters.
- The commit path accepts substantially larger content.
- Dialogs return a single Boolean for the truncated detail.
- Replace shows raw fields rather than the final diff and affected count.

Counterevidence and remaining uncertainty:
- Every ordinary write, replace, and build has a blocking approval callback.
- Missing approval fails closed.
- Read-only tasks reject mutation actions.
- The truncation is visibly indicated.

#### Dataflow

The canonical finding records the affected path at android/app/src/main/java/kr/compile/KrAgentCore.java:373-460, android/app/src/main/java/kr/compile/KrAgentCore.java:1199-1205, windows/src/kr/compile/KrAgentCore.java:315-394, linux/src/kr/compile/KrAgentCore.java:315-394, but no expanded source-to-sink narrative was recorded.

- **Source:** A write or replace action containing a benign prefix and concealed suffix

- **Sink:** writeAtomically on the active project

- **Outcome:** User-authorized source or build configuration differs materially from what was reviewed

Transformations:
- payload preview truncated
- Boolean approval captured
- full payload transformed and committed

#### Reachability

The same action object supplies both the truncated preview and the complete committed payload.

- **Attacker:** An untrusted or compromised configured model endpoint

- **Entry point:** A write or replace action containing a benign prefix and concealed suffix

- **Sink:** writeAtomically on the active project

- **Outcome:** User-authorized source or build configuration differs materially from what was reviewed

Preconditions:
- The user approves the displayed truncated mutation

Existing controls:
- Every ordinary write, replace, and build has a blocking approval callback.
- Missing approval fails closed.
- Read-only tasks reject mutation actions.
- The truncation is visibly indicated.

#### Severity

**Medium** — An untrusted endpoint can conceal materially different code after a benign prefix, but the user still sees a truncation marker and must approve the mutation, with execution generally requiring another build/run action.

Raise to high if approved changes are automatically executed; reduce to low if approval is bound to a complete diff and cryptographic hashes but the UI has only a cosmetic omission.

Impact assessment:
- **Level:** high
- **Rationale:** Concealed source/build changes can later execute with user privileges.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires social engineering through a visible truncation and later execution.

#### Remediation

Compute final bytes first and approve a full scrollable diff bound to canonical path, old hash, new hash, and replacement count; verify the old hash immediately before commit.

Tests:
- Approve/reject payloads around the preview boundary and assert the full diff is reviewable.
- Verify approval hashes fail if content or target changes before commit.

Preventive controls:
- Hash-bound approvals
- Complete diff viewer
- Canonical target display

<a id="finding-15"></a>

### [15] Toolchain provisioning consumes executable packages without authenticated integrity

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | missing executable dependency authenticity |
| CWE | CWE-494 |
| Affected lines | windows/download-toolchain.ps1:19-27, android/scripts/package-toolchain.ps1:28-43, android/scripts/package-toolchain.ps1:74-88, android/scripts/package-toolchain.ps1:342-352, windows/src/kr/compile/ToolchainManager.java:67-91 |

#### Summary

The Windows toolchain script downloads and extracts a release archive without any digest or signature. Android's packaging script trusts an unauthenticated Termux package index and byte length rather than cryptographic package digests before republishing executable toolchains.

#### Root Cause

Executable supply-chain inputs are accepted based on HTTPS location, mutable repository metadata, byte length, or file presence instead of repository-pinned digest or trusted signature.

#### Validation

The Windows script contains no hash/signature step before Expand-Archive. The Android flow does not authenticate the index or verify its SHA256 field for each package.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- Invoke-WebRequest is followed by Expand-Archive and a readiness stamp.
- ToolchainManager accepts directory shape and compiler presence.
- Other repository provisioning scripts demonstrate SHA-256 verification patterns omitted here.

Counterevidence and remaining uncertainty:
- Default URLs use HTTPS and versioned paths.
- The Windows downloader is standalone rather than automatic startup.
- The final published Android toolchain downloader verifies its resulting ZIP hash, but that hash can bless a poisoned provisioning output.

#### Dataflow

The canonical finding records the affected path at windows/download-toolchain.ps1:19-27, android/scripts/package-toolchain.ps1:28-43, android/scripts/package-toolchain.ps1:74-88, android/scripts/package-toolchain.ps1:342-352, windows/src/kr/compile/ToolchainManager.java:67-91, but no expanded source-to-sink narrative was recorded.

- **Source:** A substituted toolchain ZIP or Termux package/index

- **Sink:** CompilerEngine executes the installed compiler and associated libraries

- **Outcome:** Developer-host code execution and compromised build outputs

Transformations:
- download accepted without independent authentication
- archive extracted
- presence/readiness checks pass

#### Reachability

The downloaded bytes become the exact toolchain path selected for normal builds.

- **Attacker:** An attacker controlling a referenced release asset, package index, publishing account, or defeated download trust path

- **Entry point:** A substituted toolchain ZIP or Termux package/index

- **Sink:** CompilerEngine executes the installed compiler and associated libraries

- **Outcome:** Developer-host code execution and compromised build outputs

Preconditions:
- A maintainer/developer runs the provisioning workflow during source compromise or delivery substitution

Existing controls:
- Default URLs use HTTPS and versioned paths.
- The Windows downloader is standalone rather than automatic startup.
- The final published Android toolchain downloader verifies its resulting ZIP hash, but that hash can bless a poisoned provisioning output.

#### Severity

**Medium** — A compromised release/package source can install compilers and libraries that later execute with developer or IDE privileges, but exploitation requires a provisioning run and upstream or delivery compromise.

Raise to high if these scripts are part of an automated trusted release pipeline without independent provenance; reduce to low if artifacts are verified by a separately protected signature before these scripts run.

Impact assessment:
- **Level:** high
- **Rationale:** Malicious compilers can steal source/signing material and poison releases.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires external source compromise plus a provisioning run; HTTPS alone narrows opportunistic tampering.

#### Remediation

Pin and verify SHA-256 or, preferably, signed provenance rooted in a separately protected key before extraction. Authenticate Termux release metadata and verify each package digest; stage and atomically install only verified contents.

Tests:
- Substitute wrong-digest Windows and Termux packages and assert no extraction or stamp.
- Verify signed metadata, version pinning, and staging rollback.

Preventive controls:
- Pinned hashes and signed provenance
- Verified staging directories
- Toolchain software bill of materials

<a id="finding-16"></a>

### [16] Android SAF agent workspace retains files from previously opened projects

| Field | Value |
| --- | --- |
| Severity | medium |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | cross-project residual data exposure |
| CWE | CWE-226 |
| Affected lines | android/app/src/main/java/kr/compile/KrAgentCore.java:267-286, android/app/src/main/java/kr/compile/DocumentTree.java:23-35, android/app/src/main/java/kr/compile/KrAgentCore.java:529-576 |

#### Summary

Every SAF-backed agent task reuses files/saf_agent_workspace and overlays the active tree without deleting entries absent from the new project. Stale files remain listable and readable by the model.

#### Root Cause

A fixed app-private cache is reused across project identities, and the importer creates/overwrites current entries but never removes previous-only entries.

#### Validation

Repository-wide references show no project-switch cleanup for saf_agent_workspace; a file unique to project A survives import of project B and remains in the agent root.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- The workspace path is constant.
- importProject recursively copies provider entries but has no deletion pass.
- Agent list/read operates over the reused root.

Counterevidence and remaining uncertainty:
- Same-name files are overwritten.
- Local filesystem projects do not use this cache.
- A separate compiler cache cleanup does not affect this workspace.

#### Dataflow

The canonical finding records the affected path at android/app/src/main/java/kr/compile/KrAgentCore.java:267-286, android/app/src/main/java/kr/compile/DocumentTree.java:23-35, android/app/src/main/java/kr/compile/KrAgentCore.java:529-576, but no expanded source-to-sink narrative was recorded.

- **Source:** A stale relative filename left by a prior SAF import

- **Sink:** askModel HTTP request

- **Outcome:** Disclosure of prior-project source or configuration

Transformations:
- new project overlaid into fixed cache
- model lists or requests stale path
- content appended to conversation

#### Reachability

No cleanup or per-URI namespace exists between imports.

- **Attacker:** An untrusted configured model endpoint after the user switches SAF projects

- **Entry point:** A stale relative filename left by a prior SAF import

- **Sink:** askModel HTTP request

- **Outcome:** Disclosure of prior-project source or configuration

Preconditions:
- Project A leaves a file absent from project B
- The user starts an agent task for project B

Existing controls:
- Same-name files are overwritten.
- Local filesystem projects do not use this cache.
- A separate compiler cache cleanup does not affect this workspace.

#### Severity

**Medium** — The flaw crosses project confidentiality boundaries, but requires a project switch and an agent task that names or discovers a stale file.

Raise to high if project switching is frequent with untrusted endpoints and stale names are enumerable; remove when workspaces are fresh or atomically cleared and URI-bound.

Impact assessment:
- **Level:** high
- **Rationale:** Confidential data from one project is disclosed while another project is active.

Likelihood assessment:
- **Level:** medium
- **Rationale:** Requires sequential project use and an agent request for residual content.

#### Remediation

Use a fresh uniquely named workspace per tree/run or securely clear the exact app-private cache before import; bind lifetime to the persisted tree URI and fail closed on cleanup errors.

Tests:
- Import A then B with disjoint files and assert no A path exists.
- Inject cleanup failures and assert agent startup aborts.

Preventive controls:
- Per-project cache namespaces
- Atomic workspace replacement
- Cleanup-on-switch tests

<a id="finding-17"></a>

### [17] CTC target length can overflow the dynamic-programming width

| Field | Value |
| --- | --- |
| Severity | low |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | signed integer overflow in native allocation sizing |
| CWE | CWE-190 |
| Affected lines | android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:16747, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:16781-16795, windows/components/cc-opencl/src/CC_OpenCL.c:16781-16795, linux/components/cc-opencl/src/CC_OpenCL.c:16781-16795 |

#### Summary

compute_ctc_loss_cpu computes signed int S = 2 \* L_b + 1 for caller-controlled large target lengths; overflow can make S non-positive, after which it is reset to one while the expansion loop still writes ext\[2\*l+1\].

#### Root Cause

A signed arithmetic expression derives the allocation width without overflow or resource checks, while the subsequent loop remains bounded by the original large length.

#### Validation

For sufficiently large L_b, S overflows and may be normalized to one; the first target element then writes ext\[1\] beyond that allocation.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- Lengths are sign-checked but not capped for arithmetic safety.
- S uses signed int multiplication/addition.
- The allocation uses S while the fill loop uses L_b.

Counterevidence and remaining uncertainty:
- The required length is extremely large.
- Signed overflow behavior is compiler-dependent.
- Supplying the corresponding input arrays may already exhaust resources.

#### Dataflow

The canonical finding records the affected path at android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:16747, android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c:16781-16795, windows/components/cc-opencl/src/CC_OpenCL.c:16781-16795, linux/components/cc-opencl/src/CC_OpenCL.c:16781-16795, but no expanded source-to-sink narrative was recorded.

- **Source:** compute_ctc_loss_cpu

- **Sink:** ext\[2\*l+1\] in the target-expansion loop

- **Outcome:** Native heap out-of-bounds write

Transformations:
- large L_b accepted
- 2\*L_b+1 overflows
- non-positive S reset to one

#### Reachability

The export is public, but practical resource prerequisites materially lower likelihood.

- **Attacker:** A public-library caller controlling CTC dimensions and target lengths

- **Entry point:** compute_ctc_loss_cpu

- **Sink:** ext\[2\*l+1\] in the target-expansion loop

- **Outcome:** Native heap out-of-bounds write

Preconditions:
- Very large dimension inputs reach the function on a compiler with the described overflow behavior

Existing controls:
- The required length is extremely large.
- Signed overflow behavior is compiler-dependent.
- Supplying the corresponding input arrays may already exhaust resources.

#### Severity

**Low** — The source reaches an out-of-bounds heap write, but it requires enormous dimensions, prior large input availability, and compiler-dependent signed-overflow behavior.

Raise to medium or high if a realistic bounded input can reach a corrupting allocation on a supported build; remove after checked width arithmetic and practical caps.

Impact assessment:
- **Level:** high
- **Rationale:** If reached, native heap corruption may crash the process or worse.

Likelihood assessment:
- **Level:** low
- **Rationale:** Extreme resource requirements and undefined signed arithmetic make practical exploitation uncertain.

#### Remediation

Reject L_b \> (INT_MAX-1)/2, compute all dimensions with checked size_t arithmetic, and impose practical caps on T, B, V, and target length.

Tests:
- Test arithmetic boundaries without allocating giant inputs via factored dimension helpers.
- Run UBSan and allocation-failure tests.

Preventive controls:
- Checked DP dimension helper
- Practical tensor limits
- UBSan CI

<a id="finding-18"></a>

### [18] Android enables online dependency retrieval before explicit opt-in

| Field | Value |
| --- | --- |
| Severity | low |
| Confidence | high |
| Confidence rationale | The source-to-sink path and affected control were independently traced in the frozen repository revision. |
| Category | insecure default configuration |
| CWE | CWE-1188 |
| Affected lines | android/app/src/main/java/kr/compile/BuildManager.java:34, android/app/src/main/java/kr/compile/MainActivity.java:163, android/app/src/main/java/kr/compile/CompilerEngine.java:210, android/app/src/main/java/kr/compile/CompilerEngine.java:1072, android/app/src/main/java/kr/compile/CompilerEngine.java:1101 |

#### Summary

BuildManager initializes onlineDependencies to true and MainActivity uses true when the allow_online preference is absent, so first-launch CMake, Cargo, and Go builds can access the network without a prior opt-in.

#### Root Cause

Both the runtime field and absent-preference fallback choose network-enabled behavior instead of requiring explicit consent.

#### Validation

The Boolean materially controls CMake disconnected mode, CARGO_NET_OFFLINE, and GOPROXY; the first-launch path selects true independently in two places.

Validation method: Independent static source review at the frozen Git revision, including caller, guard, transformation, sink, countercontrol, and prerequisite analysis.

- **Status:** validated
- **Disposition:** reportable

Evidence:
- BuildManager's field initializer is true.
- MainActivity reads a missing preference with true.
- CompilerEngine emits offline settings only when false.

Counterevidence and remaining uncertainty:
- The checkbox is visibly checked.
- No fetch happens until the user starts a build.
- Turning it off applies cooperative offline settings to the primary Android backends.
- Arbitrary build definitions still require a stronger sandbox.

#### Dataflow

The canonical finding records the affected path at android/app/src/main/java/kr/compile/BuildManager.java:34, android/app/src/main/java/kr/compile/MainActivity.java:163, android/app/src/main/java/kr/compile/CompilerEngine.java:210, android/app/src/main/java/kr/compile/CompilerEngine.java:1072, android/app/src/main/java/kr/compile/CompilerEngine.java:1101, but no expanded source-to-sink narrative was recorded.

- **Source:** A first-launch dependency-capable build

- **Sink:** CMake, Cargo, or Go dependency execution

- **Outcome:** Unexpected dependency retrieval and expanded supply-chain exposure

Transformations:
- missing preference interpreted as enabled
- build backend retains network access
- remote dependency content resolved

#### Reachability

The enabled default directly reaches backend environment and arguments.

- **Attacker:** A project author or dependency publisher

- **Entry point:** A first-launch dependency-capable build

- **Sink:** CMake, Cargo, or Go dependency execution

- **Outcome:** Unexpected dependency retrieval and expanded supply-chain exposure

Preconditions:
- The user starts a dependency-capable build without first disabling the visible option

Existing controls:
- The checkbox is visibly checked.
- No fetch happens until the user starts a build.
- Turning it off applies cooperative offline settings to the primary Android backends.
- Arbitrary build definitions still require a stronger sandbox.

#### Severity

**Low** — The default contradicts the repository's explicit opt-in objective and expands supply-chain exposure, but the checkbox visibly shows enabled and the user must choose a project and start a build.

Raise to medium if network retrieval is hidden or automatic; remove when the first-launch default is off and consent precedes the first network-enabled build.

Impact assessment:
- **Level:** medium
- **Rationale:** Remote dependency content can alter or execute within the build.

Likelihood assessment:
- **Level:** medium
- **Rationale:** The behavior is default but requires an explicit user build action and is visibly represented.

#### Remediation

Default both values to false, require an explicit persisted opt-in before the first network-enabled build, and describe hosts/lock integrity; use OS isolation if offline is intended against hostile scripts.

Tests:
- On a fresh profile, assert all dependency backends make zero network connections until opt-in.
- Verify preference migration preserves an explicit prior choice.

Preventive controls:
- Secure first-run defaults
- Consent-before-network test
- Dependency lock verification

## Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
| --- | --- | --- | --- |
| Android signing and runner identity | key management and update trust | Reported | Standard release flow, asset export, APK signing, fixed package identity, and installer mediation reviewed. |
| Project import and archive extraction | path traversal and resource import | Reported | GitHub ZIP fallback, native clone ingress, safe toolchain/runner extractors, and helper extraction were compared. |
| Agent workspace, paths, approvals, and transport | confidentiality and delegated authority | Reported | Android, Windows, and Linux agent implementations and dialogs were reviewed end to end. |
| Native adaptive-compute entry points | memory safety | Reported | Focused review established parser, state-loading, count, pointer, and dimension vulnerabilities in the three mirrored CC_OpenCL sources. |
| Compiler build and artifact packaging | filesystem and build trust | Reported | Build engines, runner project packaging, output deletion, and expected project build-script execution were reviewed. |
| Toolchain and dependency provisioning | software supply chain | Reported | Android, Windows, and Linux provisioning/download controls and selected CI configuration were compared. |
| Android exported components and runner bridge | component exposure | No issue found | Reviewed manifests, provider export state, runner activities, JNI paths, and safe runner ZIP extraction; no separate component-entry vulnerability was validated. |
| Web-service authorization and browser classes | server and browser attack classes | Not applicable | No database-backed multi-user service, browser-rendered application, HTTP parser, or XML service boundary was present. |
| Remaining repository inventory | residual whole-repository variance | Needs follow-up | 74 of 362 tracked paths were fully reviewed; additional large source, examples, documentation, and opaque artifacts remain outside complete semantic review. |

## Open Questions And Follow Up

- Was any externally distributed official Android IDE APK built through the same-key standard workflow?
  - Follow-up prompt: Compare a published APK signer and contents with release provenance; rotate the identity immediately if the private key asset was shipped.
- Do checked-in prebuilt CC_OpenCL and IDE binaries exactly correspond to the reviewed source?
  - Follow-up prompt: Reproduce builds or decompile and compare exported behavior and hashes.
- The three large mirrored CC_OpenCL.c files received focused high-risk review around exported parsers, serialization, memory handles, and dimension arithmetic, but were not exhaustively audited line by line.
  - Follow-up prompt: Review deferred unit large-native-source-remainder and close its stated proof gap. Paths: android/CC_OpenCl_Enterprise-main/src/CC_OpenCL.c, windows/components/cc-opencl/src/CC_OpenCL.c, linux/components/cc-opencl/src/CC_OpenCL.c.
- Checked-in binaries, JARs, and office/PDF documents were inventoried but not reverse engineered or decompiled in this standard source scan.
  - Follow-up prompt: Review deferred unit opaque-prebuilt-artifacts and close its stated proof gap. Paths: windows/dist/KR-Compiler-IDE.jar, linux/dist/KR-Compiler-IDE.jar, windows/components/cc-opencl/lib/windows-x64/CC_OpenCl.dll, linux/components/cc-opencl/lib/linux-x64/libCC_OpenCL.so, android/CC_OpenCl_Enterprise-main/docs/CC_OpenCl_Enterprise_Dokumentation.pdf, android/gradle/wrapper/gradle-wrapper.jar.
- The standard risk-prioritized pass fully reviewed 74 of 362 tracked paths; lower-risk examples, duplicated documentation, assets, and unselected implementation files remain for a deeper variance-reducing scan.
  - Follow-up prompt: Review deferred unit remaining-tracked-paths and close its stated proof gap. Paths: ..
