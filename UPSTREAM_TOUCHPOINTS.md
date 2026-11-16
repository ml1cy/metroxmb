# Upstream touchpoints

Every edit this project makes to a file that exists upstream, so pulling new Metrolist
releases stays manageable. Project-only files (PLAN.md, assets-ps3/, the xmb and visualizer
packages) aren't listed.

| File | Change | Why |
|---|---|---|
| `CLAUDE.md` | Upstream symlink to `AGENTS.md` replaced with this project's own guide. `AGENTS.md` is untouched | Metrolist's agent rules are for their repo; this project needs its own. On upstream merges, keep ours |
| `.github/workflows/`, `.github/actions/`, `.github/scripts/`, `.github/tests/`, `.github/actionlint.yaml` | Deleted. Issue templates, PR template and `FUNDING.yml` are untouched | The owner removed GitHub Actions; upstream's CI needs their runners and secrets. On upstream merges, keep these deleted and delete any new files upstream adds there |
| `scripts/test_build_appimage.py`, `scripts/test_build_ios.py`, `scripts/test_desktop_smoke.py` | Removed the assertions that read workflow and action files; the build-script, Xcode-project and `gradle.properties` checks stay | The workflow files they inspected are deleted. On upstream merges, drop any new workflow assertions |
