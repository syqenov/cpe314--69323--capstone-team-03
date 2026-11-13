# PawTrack - Known Issues & Limitations

## Current Status
* **Test Count:** 75 tests passing (100% of suite).

## Known Limitations
* **Cat API Breed Matching:** TheCatAPI free tier endpoint returns a random cat image rather than matching the specific breed of the animal. This is a documented limitation of the external API provider.
* **Live API Automation:** Automated tests for external API endpoints (Dog CEO and TheCatAPI) currently parse saved JSON samples to prevent CI/CD network flakiness. Live endpoints are verified strictly via manual GUI testing.
* **Database File Location:** The application creates `pawtrack.db` in the local execution directory, which requires the host OS environment to have write permissions for that specific folder.
