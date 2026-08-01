---
name: watchlater-workflow
description: Project specific rules and workflows for WatchLater. Use this when the user asks for a release, update, or has general requests.
---

# WatchLater Project Rules

## Release Process
- **DO NOT** build APKs manually using `./gradlew assembleDebug` or similar commands to provide to the user unless explicitly told.
- **DO NOT** copy APKs into the artifacts directory.
- **DO** bump the `versionCode` and `versionName` in `app/build.gradle.kts`.
- The user uses a CI/CD pipeline (GitHub Actions) for releases. After bumping the version, tell the user to commit and push to trigger the OTA update.

## User Preferences
- **Communication**: Caveman mode is active by default. Be brief, professional, and skip pleasantries.
- **Model Choice**: The user is extremely sensitive to model changes. Ensure you are using the exact model (e.g., gemini-2.5-flash) required for Edge Functions or scripts, as the Pro model causes quota limit errors (429).
    - **Quality**: The user demands "perfect" and "professional" output. No simple mistakes.

## Graphify Tool Rule
- **Mandatory**: Before any major architecture change or refactoring, you MUST review and update the `app_architecture_graph.md` artifact (or file if stored locally).
- **Purpose**: This graph uses Mermaid.js to visually map all connections (UI -> ViewModels -> Repositories -> Data Sources -> Network/DB). 
- **Usage**: Use this artifact to explicitly see every connection in the entire app before touching code to avoid breaking hidden dependencies.
