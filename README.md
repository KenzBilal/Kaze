# Kaze (WatchLater)

A professional-grade Android application for tracking movies and TV series, built with modern Android development practices (Jetpack Compose, Room, Coroutines, MVVM).

## Architecture Overview

- **UI:** Jetpack Compose (Material 3) with a custom, premium color palette (strict minimalist B&W/grey).
- **Architecture:** MVVM (Model-View-ViewModel) pattern.
- **Local Database:** Room Database (`WatchLaterDatabase`) - Source of truth for personal watchlist.
- **Remote Database (Social):** Supabase PostgreSQL - Handles user authentication, profiles, and follower relationships.
- **Network Layer:** Retrofit2 + Gson fetching data from OMDB API.
- **Dependency Injection:** Manual DI via `AppContainer`.
- **Image Loading:** Coil.
- **Asynchronous Work:** Kotlin Coroutines and Flow.

## Key Features

1. **Media Tracking:** Add movies, series, and anime. Track watched status and custom ratings out of 10.
2. **Social & Community:** 
   - Publicly searchable user profiles.
   - Follow/Unfollow users to see their watched lists and favorites.
   - Pinterest-style staggered grid for exploring other users' watchlists.
   - Global tracking of favorites (Movie, Series, Genre).
3. **OMDB API Integration:**
   - Automatically fetches high-quality posters, genres, and release years based on titles.
   - For TV series, it auto-fetches total seasons.
4. **Advanced Series Tracking:**
   - **Season caching:** Season data is cached locally for 30 days to heavily optimize OMDB free-tier API limits.
   - **Episode-level granularity:** Mark individual episodes as watched.
   - **Auto-advance logic:** Finishing an episode automatically advances to the next unwatched episode, jumping across seasons.
   - **Sequential enforcement:** Prevents marking later seasons as complete if earlier seasons are unfinished.
5. **Offline Support:** Full offline capability. API calls are only made once per season/item and cached to Room.
6. **Local Backup:** Export/Import entire database to/from JSON files securely.

## Over-The-Air (OTA) Auto-Updater

Kaze includes a custom, zero-dependency in-app auto-updater (`com.watchlater.updater.UpdateManager`) connected to a fully automated CI/CD pipeline.

### How the Updater Works:
1. **The Check:** On app launch, `HomeViewModel` checks a public GitHub Gist URL (`UPDATE_JSON_URL` injected via `build.gradle.kts`).
2. **The JSON:** The Gist contains the latest `versionCode`, `versionName`, and `apkUrl`.
3. **The Prompt:** If the Gist's `versionCode` > app's `versionCode`, the app shows an "Update Available" dialog.
4. **The Download:** Using Android's native `DownloadManager`, the APK is fetched to the Downloads folder.
5. **The Install:** A `FileProvider` triggers the native Android package installer.

### The CI/CD Release Pipeline:
You do not need to manually edit the Gist or build the APK. The entire release process is automated via GitHub Actions (`.github/workflows/release.yml`).

**To release a new version:**
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit and push your code to the `master` branch.
3. Tag the release and push the tag:
   ```bash
   git tag v1.3.0
   git push origin v1.3.0
   ```
4. **Automation takes over:**
   - The GitHub Action detects the `v*` tag.
   - It builds the release APK (`assembleRelease`).
   - It creates a GitHub Release and attaches the APK.
   - It securely authenticates using `GIST_TOKEN` (a repository secret) to edit the Gist JSON file.
   - The Gist is updated with the new `versionCode` and the direct GitHub Release APK download link.
   - Every installed app will immediately detect the update on next launch.

## API Keys & Environment Variables

The project requires an OMDB API key to function.
In your local `gradle.properties`, you must have:
```properties
omdb.api.key=YOUR_KEY_HERE
```
This is injected into the BuildConfig during compilation.

Supabase URL and Anon Key are injected via `build.gradle.kts` using environment variables or fallback values.

## ⚠️ AI Developer Project Rules

These rules **MUST** be followed by any AI agent working on this repository:

1. **Verify Deployments:** After every git push, you MUST monitor the deployment/build pipeline (GitHub Actions). Confirm it is successful. If it fails, fix the build immediately using professional, standard solutions—no temporary hacks. Repeat until successful.
2. **Maintain Documentation:** After every feature edit or architectural change, you MUST update this `README.md` file accordingly. This ensures project knowledge remains accurate regardless of which AI is working on it.
3. **Efficient Workflows:** Do NOT use live visual browser tools (like a web agent interacting with a Chrome instance) to watch pipelines or check status. It is slow and consumes excessive context. Instead, use efficient CLI tools like the GitHub CLI (`gh run list`, `gh run view`) or direct API calls.
4. **Professional Grade:** Never treat this repository as a "kid's project" or a toy app. It is a real, production-ready product. Ensure all code quality, error handling, and architecture meet professional engineering standards.
