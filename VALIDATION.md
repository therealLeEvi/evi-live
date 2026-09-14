# Validation gates

The candidate is not yet a Plugin Hub release. Do not infer approval from local tests.

| Gate | Evidence / status |
| --- | --- |
| Java 11 source compilation against RuneLite 1.12.38 | Passed direct compilation using the Java 11 standard library, source/target 11 |
| Synthetic GE transitions | Passed: baseline, partial/instant/completed/cancelled offers, repeat events, direction/counter changes, slot reuse, session reset |
| Pairing input | Passed: invalid/empty key rejection, BOM/whitespace/case normalization |
| Sidebar controls | Passed headless Swing test: masked input, save callback, clear after save |
| Economy namespace | Passed: ordinary-world continuity, separate special/unknown modes |
| Delivery behavior | Passed fake-transport tests: 401, disconnect, identical retry, stale generation, disable, overflow rebaseline |
| Standard Gradle build | Not passed locally: Windows compiler resource close AccessDeniedException; CI workflow prepared for Windows and Linux |
| Plugin Hub verification | Not run; requires submission and exact public commit |
| Live candidate lifecycle | Pending; original personal plugin worked, which does not validate the changed candidate |
| HTTP architecture policy | Awaiting maintainer clarification; see REVIEW-QUESTION.md |

## Live candidate acceptance procedure

Run this only with the user's own normal gameplay. No test should place, cancel or collect an in-game offer automatically.

1. Enable without a key: the sidebar stays available and explains pairing.
2. Pair with an invalid input, then a syntactically valid wrong key: input error and bridge-authentication error should be distinguishable.
3. Pair correctly and log in with existing offers: initial offers must remain incomplete baselines.
4. Observe normal new buy/sell activity, including partial fills, cancellation and collection; compare quantities and counters to the visible GE offers.
5. Stop and restart the bridge: queued packets should recover without duplicating completed records.
6. Disable and immediately re-enable the plugin: the previous sender must not consume new-session packets or change its sidebar status.
7. Log out, change worlds and switch accounts normally: fresh-session status must replace stale active offers; no false fully observed starts.
8. If using special game modes, verify those observations do not match ordinary-world trade pairs.
9. Restart RuneLite: the saved bridge key should load without re-entering it; no Jagex credential file should be read by plugin code.

Record date, candidate commit, RuneLite version, observed result, and any issue for each step. Do not attach credentials or raw personal logs publicly.
